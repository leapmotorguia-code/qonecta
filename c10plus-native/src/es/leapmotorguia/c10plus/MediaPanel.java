package es.leapmotorguia.c10plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Local TDTChannels catalogue; favourites persist through signed app updates.
 * Only HTTPS streams; no DRM or geographic restriction bypass. Video requires
 * an available stationary UNIT reading and manual confirmation, and stops
 * on missing/moving data, leaving the page or activity pause. */
final class MediaPanel {
 interface Stationary { boolean allowed(); }
 static final class Entry {
   final String kind,country,category,name,url,web,note,key;
   Entry(String k,String[] f){kind=k;country=f[0];category=f[1];name=f[2];url=f[3];web=f[4];note=f[5];key=k+"|"+country+"|"+category+"|"+name;}
   public String toString(){return name+"  ·  "+country+"  ·  "+category;}
 }
 final Activity a; final LinearLayout root; final String mode; final Stationary stationary; final boolean tablet;
 final SharedPreferences prefs; final Set<String> favorites; final ArrayList<Entry> all=new ArrayList<>(), visible=new ArrayList<>();
 final RadioAudio radio; final Handler handler=new Handler(Looper.getMainLooper());
 final TextView status,count; final EditText search; final Spinner country; final CheckBox onlyFavorites;
 final ListView list; final ArrayAdapter<String> adapter; final FrameLayout playerSlot;
 VideoView video; WebView browser; Runnable timeout; boolean closed,videoActive;
 MediaPanel(Activity a,LinearLayout root,String mode,Stationary stationary){this(a,root,mode,stationary,false);}
 MediaPanel(Activity a,LinearLayout root,String mode,Stationary stationary,boolean tablet){
   this.a=a;this.root=root;this.mode=mode;this.stationary=stationary;this.tablet=tablet;
   prefs=a.getSharedPreferences("c10plus_media_favorites",Context.MODE_PRIVATE);
   favorites=new HashSet<>(prefs.getStringSet("saved",Collections.emptySet()));
   radio=new RadioAudio(a);
   if(!mode.equals("tv"))load("radio");
   if(!mode.equals("radio"))load("tv");
   status=label("Selecciona una emisora o canal. Mantén pulsado para guardar o quitar favorito.");
   root.addView(status); LinearLayout controls=new LinearLayout(a);
   search=new EditText(a);search.setSingleLine(true);search.setHint("Buscar nombre o categoría");search.setTextColor(0xffe9f6fa);search.setHintTextColor(0xffb8d9e6);
   controls.addView(search,new LinearLayout.LayoutParams(0,-2,2));
   country=new Spinner(a);TreeSet<String> countries=new TreeSet<>();for(Entry e:all)countries.add(e.country);
   ArrayList<String> choices=new ArrayList<>();choices.add("Todos los países");choices.addAll(countries);
   ArrayAdapter<String> cs=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,choices);cs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);country.setAdapter(cs);controls.addView(country,new LinearLayout.LayoutParams(0,-2,1));
   onlyFavorites=new CheckBox(a);onlyFavorites.setText("★ Favoritos");onlyFavorites.setTextColor(0xffe9f6fa);onlyFavorites.setChecked(mode.equals("favorites"));controls.addView(onlyFavorites);root.addView(controls);
   count=label("");root.addView(count);
   LinearLayout area=new LinearLayout(a);list=new ListView(a);
   adapter=new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,new ArrayList<String>()){
    @Override public View getView(int p,View old,ViewGroup parent){TextView t=(TextView)super.getView(p,old,parent);t.setTextColor(0xffe9f6fa);t.setTextSize(16);return t;}
   };list.setAdapter(adapter);area.addView(list,new LinearLayout.LayoutParams(0,-1,1));
   playerSlot=new FrameLayout(a);playerSlot.setBackgroundColor(0xff081520);playerSlot.setVisibility(View.GONE);area.addView(playerSlot,new LinearLayout.LayoutParams(0,-1,1));root.addView(area,new LinearLayout.LayoutParams(-1,0,1));
   LinearLayout footer=new LinearLayout(a);Button stop=new Button(a);stop.setText("Detener reproducción");stop.setOnClickListener(v->stop());footer.addView(stop);
   Button help=new Button(a);help.setText("Guardar favorito");help.setOnClickListener(v->new AlertDialog.Builder(a).setMessage("Mantén pulsado cualquier canal o emisora de la lista para añadirlo o quitarlo de favoritos.").setPositiveButton("Aceptar",null).show());footer.addView(help);
   footer.addView(label("TDTChannels · emisiones públicas; algunas requieren web, país o cuenta."));root.addView(footer);
   list.setOnItemClickListener((par,v,i,id)->{if(i<visible.size())select(visible.get(i));});
   list.setOnItemLongClickListener((par,v,i,id)->{if(i>=visible.size())return true;Entry e=visible.get(i);boolean add=!favorites.contains(e.key);if(add)favorites.add(e.key);else favorites.remove(e.key);prefs.edit().putStringSet("saved",new HashSet<>(favorites)).apply();filter();Toast.makeText(a,add?"Favorito guardado":"Favorito eliminado",Toast.LENGTH_SHORT).show();return true;});
   onlyFavorites.setOnCheckedChangeListener((b,c)->filter());
   country.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int i,long id){filter();}public void onNothingSelected(AdapterView<?> p){}});
   search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){}public void onTextChanged(CharSequence s,int st,int b,int c){filter();}public void afterTextChanged(Editable e){}});
   filter();
 }
 TextView label(String s){TextView t=new TextView(a);t.setText(s);t.setTextColor(0xffe9f6fa);t.setPadding(8,5,8,5);t.setTextSize(14);return t;}
 void load(String kind){try(BufferedReader r=new BufferedReader(new InputStreamReader(a.getAssets().open(kind+"-catalog.tsv"),"UTF-8"))){String l;while((l=r.readLine())!=null){String[] f=l.split("\\t",-1);if(f.length==6&&!f[2].isEmpty())all.add(new Entry(kind,f));}}catch(Exception e){Toast.makeText(a,"Catálogo "+kind+" no disponible",Toast.LENGTH_LONG).show();}}
 void filter(){if(closed)return;String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);String c=String.valueOf(country.getSelectedItem());visible.clear();adapter.clear();for(Entry e:all){if(onlyFavorites.isChecked()&&!favorites.contains(e.key))continue;if(country.getSelectedItemPosition()>0&&!e.country.equals(c))continue;if(!(e.name+" "+e.category+" "+e.country).toLowerCase(Locale.ROOT).contains(q))continue;visible.add(e);adapter.add((favorites.contains(e.key)?"★ ":"☆ ")+(mode.equals("favorites")?(e.kind.equals("tv")?"TV · ":"Radio · "):"")+e.toString());}count.setText(visible.size()+" de "+all.size()+" · "+favorites.size()+" favoritos guardados");}
 boolean https(String u){try{return "https".equalsIgnoreCase(Uri.parse(u).getScheme())&&Uri.parse(u).getHost()!=null;}catch(Exception e){return false;}}
 void select(Entry e){
   stop();
   if(e.kind.equals("radio")){if(https(e.url)){radio.play(e.name,e.url);status.setText(radio.status());}else status.setText("Esta emisora no tiene un enlace de audio HTTPS compatible.");return;}
   if(!stationary.allowed()){status.setText(tablet?"Multimedia tablet no disponible en este momento.":"TV: se necesita una lectura UNIT de vehículo detenido. Si no está disponible, no se inicia vídeo.");return;}
   if(tablet){playVideo(e);return;}
   new AlertDialog.Builder(a).setTitle(e.name).setMessage("Confirma que el coche está estacionado. Algunos canales requieren su web oficial o están limitados por país.").setPositiveButton("Estacionado",(d,w)->{if(!closed&&stationary.allowed())playVideo(e);}).setNegativeButton("Cancelar",null).show();
 }
 void playVideo(Entry e){
   stop();if(closed||!stationary.allowed())return;
   if(!https(e.url)&&!https(e.web)){status.setText("Canal sin enlace HTTPS compatible.");return;}
   videoActive=true;playerSlot.setVisibility(View.VISIBLE);status.setText("Conectando: "+e.name+" · "+e.note);
   if(!https(e.url)){openWeb(e);return;}
   try{VideoView v=new VideoView(a);video=v;playerSlot.addView(v,new FrameLayout.LayoutParams(-1,-1));v.setOnPreparedListener(mp->{if(video!=v)return;cancelTimeout();if(!stationary.allowed()){stop();return;}v.start();status.setText("En directo: "+e.name);});v.setOnErrorListener((mp,w,x)->{if(video==v){stop();status.setText("Emisión no disponible: "+e.name);offerWeb(e);}return true;});v.setOnCompletionListener(mp->{if(video==v){stop();status.setText("Emisión finalizada: "+e.name);}});v.setVideoURI(Uri.parse(e.url));timeout=()->{if(video==v){stop();status.setText("Tiempo agotado: "+e.name);offerWeb(e);}};handler.postDelayed(timeout,25000);v.start();}catch(Exception ex){stop();status.setText("No se pudo reproducir "+e.name);offerWeb(e);}
 }
 void offerWeb(Entry e){if(!closed&&https(e.web)&&stationary.allowed())new AlertDialog.Builder(a).setMessage("¿Abrir la web oficial de "+e.name+"?").setPositiveButton("Abrir",(d,w)->{if(!closed&&stationary.allowed()){stop();videoActive=true;playerSlot.setVisibility(View.VISIBLE);openWeb(e);}}).setNegativeButton("Cancelar",null).show();}
 void openWeb(Entry e){try{browser=new WebView(a);browser.getSettings().setJavaScriptEnabled(true);browser.getSettings().setDomStorageEnabled(true);browser.getSettings().setAllowFileAccess(false);browser.getSettings().setAllowContentAccess(false);browser.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);browser.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !https(r.getUrl().toString());}});playerSlot.addView(browser,new FrameLayout.LayoutParams(-1,-1));browser.loadUrl(e.web);status.setText("Web oficial: "+e.name+" · DRM según proveedor");}catch(Exception ex){stop();status.setText("Web no disponible: "+e.name);}}
 void cancelTimeout(){if(timeout!=null){handler.removeCallbacks(timeout);timeout=null;}}
 void refresh(){if(closed)return;if(videoActive){if(!stationary.allowed()){stop();status.setText(tablet?"TV detenida: dispositivo fuera del modo tablet.":"TV detenida: movimiento o lectura de velocidad no disponible.");}}else if(!radio.status().equals("Radio detenida"))status.setText(radio.status());}
 void stop(){cancelTimeout();radio.stop();videoActive=false;if(video!=null){VideoView v=video;video=null;try{v.stopPlayback();}catch(Exception ignored){}}if(browser!=null){WebView w=browser;browser=null;try{w.stopLoading();w.loadUrl("about:blank");playerSlot.removeView(w);w.destroy();}catch(Exception ignored){}}playerSlot.removeAllViews();playerSlot.setVisibility(View.GONE);status.setText("Reproducción detenida");}
 void close(){closed=true;stop();}
}
