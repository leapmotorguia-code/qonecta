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
 * an available stationary UNIT reading, and stops
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
 final TextView status,count; final EditText search; final Spinner country, genre; final CheckBox onlyFavorites;
 final ArrayList<Entry> rows=new ArrayList<>();
 final ListView list; final ArrayAdapter<String> adapter; final FrameLayout playerSlot;
 final LinearLayout area, playerColumn; final FrameLayout picture;
 int ratio=0, size=70; boolean left=false; android.app.Dialog fullDialog; Button fullButton;
 final Runnable videoGuard=new Runnable(){public void run(){if(closed||!videoActive)return;if(!stationary.allowed()){stop();status.setText("Vídeo detenido: dispositivo o vehículo fuera del modo permitido.");return;}handler.postDelayed(this,1000);}};
 VideoView video; WebView browser; Runnable timeout; boolean closed,videoActive;
 MediaPanel(Activity a,LinearLayout root,String mode,Stationary stationary){this(a,root,mode,stationary,false);}
 MediaPanel(Activity a,LinearLayout root,String mode,Stationary stationary,boolean tablet){
   this.a=a;this.root=root;this.mode=mode;this.stationary=stationary;this.tablet=tablet;
   prefs=a.getSharedPreferences("c10plus_media_favorites",Context.MODE_PRIVATE);
   favorites=new HashSet<>(prefs.getStringSet("saved",Collections.emptySet()));
   radio=new RadioAudio(a);
   if(!mode.equals("tv"))load("radio");
   if(!mode.equals("radio"))load("tv");
   all.sort(Comparator.comparing((Entry e)->e.country,String.CASE_INSENSITIVE_ORDER).thenComparingInt(e->priority(e)));
   status=label("Selecciona una emisora o canal. Mantén pulsado para guardar o quitar favorito.");
   root.addView(status); LinearLayout controls=new LinearLayout(a);
   search=new EditText(a);search.setSingleLine(true);search.setHint("Buscar nombre o categoría");search.setTextColor(0xffe9f6fa);search.setHintTextColor(0xffb8d9e6);
   controls.addView(search,new LinearLayout.LayoutParams(0,-2,2));
   country=new Spinner(a);TreeSet<String> countries=new TreeSet<>();for(Entry e:all)countries.add(e.country);
   ArrayList<String> choices=new ArrayList<>();choices.add("Todos los países");choices.addAll(countries);
   ArrayAdapter<String> cs=spinnerAdapter(choices);cs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);country.setAdapter(cs);controls.addView(label("País"));controls.addView(country,new LinearLayout.LayoutParams(0,-2,2));
   onlyFavorites=new CheckBox(a);onlyFavorites.setText("★ Favoritos");onlyFavorites.setTextColor(0xffe9f6fa);onlyFavorites.setChecked(mode.equals("favorites"));controls.addView(onlyFavorites);root.addView(controls);
   genre=new Spinner(a);String[] genres={"Todas las categorías","Deportes","Cine","Series","Documentales","Noticias","Infantiles","Música","Generalistas","Otros"};
   ArrayAdapter<String> gs=spinnerAdapter(Arrays.asList(genres));gs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);genre.setAdapter(gs);if(!mode.equals("radio")){LinearLayout filters=new LinearLayout(a);filters.addView(label("Categoría"));filters.addView(genre,new LinearLayout.LayoutParams(0,-2,1));root.addView(filters);}
   if(!mode.equals("radio")){Button pluto=control("Pluto TV",()->openPluto());root.addView(pluto);}
   count=label("");root.addView(count);
   area=new LinearLayout(a);list=new ListView(a);
   adapter=new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,new ArrayList<String>()){
    @Override public boolean areAllItemsEnabled(){return false;}
    @Override public boolean isEnabled(int p){return p>=0&&p<rows.size()&&rows.get(p)!=null;}
    @Override public View getView(int p,View old,ViewGroup parent){TextView t=(TextView)super.getView(p,old,parent);boolean heading=!isEnabled(p);t.setTextColor(heading?0xff59f3d7:0xffe9f6fa);t.setTextSize(heading?18:16);t.setTypeface(null,heading?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);t.setBackgroundColor(heading?0xff153c50:android.graphics.Color.TRANSPARENT);return t;}
   };list.setAdapter(adapter);area.addView(list,new LinearLayout.LayoutParams(0,-1,1));
   playerColumn=new LinearLayout(a);playerColumn.setOrientation(LinearLayout.VERTICAL);playerColumn.setVisibility(View.GONE);
   playerSlot=new FrameLayout(a);playerSlot.setBackgroundColor(0xff000000);
   picture=new FrameLayout(a);playerSlot.addView(picture,new FrameLayout.LayoutParams(-1,-1,android.view.Gravity.CENTER));
   playerSlot.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fitPicture());
   playerColumn.addView(playerSlot,new LinearLayout.LayoutParams(-1,0,1));
   HorizontalScrollView scroll=new HorizontalScrollView(a);LinearLayout videoControls=new LinearLayout(a);
   fullButton=control("Pantalla completa",()->toggleFull());videoControls.addView(fullButton);
   videoControls.addView(control("16:9",()->{ratio=169;fitPicture();}));
   videoControls.addView(control("4:3",()->{ratio=43;fitPicture();}));
   videoControls.addView(control("Original",()->{ratio=0;fitPicture();}));
   videoControls.addView(control("50%",()->resize(50)));videoControls.addView(control("75%",()->resize(75)));videoControls.addView(control("100%",()->resize(100)));
   videoControls.addView(control("Cambiar lado",()->{left=!left;resize(size);}));
   scroll.addView(videoControls);playerColumn.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
   area.addView(playerColumn,new LinearLayout.LayoutParams(0,-1,1));root.addView(area,new LinearLayout.LayoutParams(-1,0,1));
   LinearLayout footer=new LinearLayout(a);Button stop=new Button(a);stop.setText("Detener reproducción");stop.setOnClickListener(v->stop());footer.addView(stop);
   Button help=new Button(a);help.setText("Guardar favorito");help.setOnClickListener(v->new AlertDialog.Builder(a).setMessage("Mantén pulsado cualquier canal o emisora de la lista para añadirlo o quitarlo de favoritos.").setPositiveButton("Aceptar",null).show());footer.addView(help);
   footer.addView(label("TDTChannels · emisiones públicas; algunas requieren web, país o cuenta."));root.addView(footer);
   list.setOnItemClickListener((par,v,i,id)->{if(i<rows.size()&&rows.get(i)!=null)select(rows.get(i));});
   list.setOnItemLongClickListener((par,v,i,id)->{if(i>=rows.size()||rows.get(i)==null)return true;Entry e=rows.get(i);boolean add=!favorites.contains(e.key);if(add)favorites.add(e.key);else favorites.remove(e.key);prefs.edit().putStringSet("saved",new HashSet<>(favorites)).apply();filter();Toast.makeText(a,add?"Favorito guardado":"Favorito eliminado",Toast.LENGTH_SHORT).show();return true;});
   onlyFavorites.setOnCheckedChangeListener((b,c)->filter());
   country.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int i,long id){filter();}public void onNothingSelected(AdapterView<?> p){}});
   genre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int i,long id){filter();}public void onNothingSelected(AdapterView<?> p){}});
   search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){}public void onTextChanged(CharSequence s,int st,int b,int c){filter();}public void afterTextChanged(Editable e){}});
   filter();
 }
 ArrayAdapter<String> spinnerAdapter(List<String> items){return new ArrayAdapter<String>(a,android.R.layout.simple_spinner_item,items){
 @Override public View getView(int p,View old,ViewGroup parent){TextView t=(TextView)super.getView(p,old,parent);t.setTextColor(0xffe9f6fa);t.setTextSize(16);t.setPadding(12,12,12,12);t.setBackgroundColor(0xff153c50);return t;}
 @Override public View getDropDownView(int p,View old,ViewGroup parent){TextView t=(TextView)super.getDropDownView(p,old,parent);t.setTextColor(0xffe9f6fa);t.setBackgroundColor(0xff102b3d);t.setPadding(16,14,16,14);return t;}
 };}
 Button control(String title,Runnable action){Button b=new Button(a);b.setAllCaps(false);b.setText(title);b.setTextSize(14);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(12,6,12,6);b.setSingleLine(true);b.setTextColor(0xffe9f6fa);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff24536a));b.setOnClickListener(v->action.run());return b;}
 int priority(Entry e){
   if(!e.kind.equals("tv"))return 1000;
   String n=e.name.toLowerCase(Locale.ROOT);String[] order={};
   switch(e.country){
    case "España":order=new String[]{"la 1","la 2","tv3","cuatro","telecinco","antena 3","lasexta","la séptima","mega","dmax","fdf","neox","nova","atreseries","be mad","divinity","energy","trece","24h","teledeporte","clan","boing"};break;
    case "Alemania":order=new String[]{"ard das erste","zdf","br bayerischer","ndr","wdr","sr saarland","hr hessenschau","tagesschau24","dw"};break;
    case "Portugal":order=new String[]{"rtp 1","rtp 2","rtp notícias","rtp internacional","rtp açores","rtp madeira","porto canal","cnn"};break;
    case "Italia":order=new String[]{"rai 1","rai 2","rai 3","la7","rai news"};break;
    case "Francia":order=new String[]{"tf1","france 2","france 3","tv5monde","france 24","franceinfo","bfm","cnews"};break;
    case "Argentina":order=new String[]{"canal 7","américa tv","el nueve","telefe","todo noticias","a24","c5n"};break;
    case "México":order=new String[]{"canal once","canal 6","n+ méxico","n+ foro","adn noticias"};break;
    case "Perú":order=new String[]{"tv perú","latina","panamericana","willax","canal ipe"};break;
    case "Colombia":order=new String[]{"señal colombia","noticias caracol","noticias rcn","citytv","teleantioquia"};break;
    case "Chile":order=new String[]{"tele13","meganoticias","24h"};break;
    case "Brasil":order=new String[]{"tv cultura","sbt interior","redetv!","rede massa","record news","cnn brasil"};break;
    case "Ecuador":order=new String[]{"ecuador tv","teleamazonas","oromar tv"};break;
    case "Paraguay":order=new String[]{"telefuturo","latele","unicanal","abc tv","noticias npy"};break;
    case "Costa Rica":order=new String[]{"teletica 7","trece","telediario"};break;
    case "Marruecos":order=new String[]{"snrt al aoula","snrt al maghribia","medi1 tv","snrt arrYadia".toLowerCase(Locale.ROOT),"snrt athaqafia"};break;
    case "Turquía":order=new String[]{"star tv","now tv","trt haber","trt world"};break;
    case "Estados Unidos":order=new String[]{"abc news","cbs news","nbc news","n+ univision","noticias telemundo","livenow from fox","bloomberg","nasa+"};break;
   }
   for(int i=0;i<order.length;i++){String k=order[i];if(n.equals(k)||(!e.country.equals("España")&&n.startsWith(k+" ")))return i;}
   return 1000;
 }
 void fitPicture(){int w=playerSlot.getWidth(),h=playerSlot.getHeight();if(w<=0||h<=0)return;int pw=w,ph=h;if(ratio!=0){float f=ratio==169?16f/9:4f/3;pw=Math.min(w,Math.round(h*f));ph=Math.min(h,Math.round(w/f));}
   FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)picture.getLayoutParams();if(p.width!=pw||p.height!=ph){p.width=pw;p.height=ph;p.gravity=android.view.Gravity.CENTER;picture.setLayoutParams(p);}}
 void resize(int pct){size=pct;if(fullDialog!=null)return;int index=left?0:1;if(area.indexOfChild(playerColumn)!=index){area.removeView(playerColumn);area.addView(playerColumn,index);}playerColumn.setLayoutParams(new LinearLayout.LayoutParams(0,-1,pct));list.setVisibility(pct==100?View.GONE:View.VISIBLE);list.setLayoutParams(new LinearLayout.LayoutParams(0,-1,100-pct));if(videoActive&&video!=null)video.start();}
 void toggleFull(){
   if(fullDialog!=null){fullDialog.dismiss();return;}
   if(!videoActive||!stationary.allowed())return;
   android.app.Dialog d=new android.app.Dialog(a,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
   LinearLayout page=new LinearLayout(a);page.setOrientation(LinearLayout.VERTICAL);
   page.setBackgroundColor(0xff000000);area.removeView(playerColumn);page.addView(playerColumn,new LinearLayout.LayoutParams(-1,0,1));
   fullDialog=d;fullButton.setText("Salir pantalla completa");d.setContentView(page);
   d.setOnDismissListener(x->{fullDialog=null;page.removeView(playerColumn);area.addView(playerColumn,left?0:area.getChildCount(),new LinearLayout.LayoutParams(0,-1,size));fullButton.setText("Pantalla completa");if(videoActive&&stationary.allowed()&&video!=null)video.start();});
   d.show();if(d.getWindow()!=null)d.getWindow().setLayout(-1,-1);if(video!=null)video.start();
 }
 void openPluto(){
   if(!stationary.allowed()){status.setText("Pluto TV: vídeo disponible con el vehículo estacionado.");return;}
   if(tablet){android.content.Intent launch=a.getPackageManager().getLaunchIntentForPackage("tv.pluto.android");if(launch!=null){stop();try{a.startActivity(launch);return;}catch(Exception ignored){}}}
   Entry e=new Entry("tv",new String[]{"Internacional","Streaming","Pluto TV","","https://pluto.tv/","Servicio oficial"});
   select(e);
 }
 TextView label(String s){TextView t=new TextView(a);t.setText(s);t.setTextColor(0xffe9f6fa);t.setPadding(8,5,8,5);t.setTextSize(14);return t;}
 void load(String kind){try(BufferedReader r=new BufferedReader(new InputStreamReader(a.getAssets().open(kind+"-catalog.tsv"),"UTF-8"))){String l;while((l=r.readLine())!=null){String[] f=l.split("\\t",-1);if(f.length==6&&!f[2].isEmpty())all.add(new Entry(kind,f));}}catch(Exception e){Toast.makeText(a,"Catálogo "+kind+" no disponible",Toast.LENGTH_LONG).show();}}
 String genreOf(Entry e){String c=e.category.toLowerCase(Locale.ROOT),n=e.name.toLowerCase(Locale.ROOT);if(c.contains("deport")||n.matches(".*\\b(sport|sports|futbol|fútbol|motor sport)\\b.*"))return "Deportes";if(n.matches(".*\\b(documental|documentales|documentary|discovery|nature|historia|history|national geographic|wildlife)\\b.*"))return "Documentales";if(n.equals("be mad")||n.matches(".*\\b(cine|cinema|movies|movie|películas|film|films)\\b.*"))return "Cine";if(n.equals("fdf")||n.equals("energy")||n.equals("divinity")||n.contains("atreseries")||n.matches(".*\\b(series|serie|sitcom)\\b.*"))return "Series";if(c.contains("informativ")||n.matches(".*\\b(news|noticias)\\b.*"))return "Noticias";if(c.contains("infantil")||n.matches(".*\\b(kids|junior|cartoon)\\b.*"))return "Infantiles";if(c.contains("musical")||n.matches(".*\\b(music|música|mtv)\\b.*"))return "Música";if(c.contains("nacional")||c.contains("autonóm"))return "Generalistas";return "Otros";}
 void filter(){if(closed)return;String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);String c=String.valueOf(country.getSelectedItem());visible.clear();rows.clear();adapter.clear();String lastGroup="";for(Entry e:all){if(onlyFavorites.isChecked()&&!favorites.contains(e.key))continue;if(country.getSelectedItemPosition()>0&&!e.country.equals(c))continue;if(!mode.equals("radio")&&genre.getSelectedItemPosition()>0&&(!e.kind.equals("tv")||!genreOf(e).equals(String.valueOf(genre.getSelectedItem()))))continue;if(!(e.name+" "+e.category+" "+e.country+" "+genreOf(e)).toLowerCase(Locale.ROOT).contains(q))continue;visible.add(e);String group=e.country;if(!mode.equals("radio")&&!group.equals(lastGroup)){rows.add(null);adapter.add("▾  "+group);lastGroup=group;}rows.add(e);adapter.add((favorites.contains(e.key)?"★ ":"☆ ")+(mode.equals("favorites")?(e.kind.equals("tv")?"TV · ":"Radio · "):"")+e.toString());}count.setText(visible.size()+" de "+all.size()+" · "+favorites.size()+" favoritos guardados");}
 boolean https(String u){try{return "https".equalsIgnoreCase(Uri.parse(u).getScheme())&&Uri.parse(u).getHost()!=null;}catch(Exception e){return false;}}
 void select(Entry e){
   stop();
   if(e.kind.equals("radio")){if(https(e.url)){radio.play(e.name,e.url);status.setText(radio.status());}else status.setText("Esta emisora no tiene un enlace de audio HTTPS compatible.");return;}
   if(!stationary.allowed()){status.setText(tablet?"Multimedia tablet no disponible en este momento.":"TV: se necesita una lectura UNIT de vehículo detenido. Si no está disponible, no se inicia vídeo.");return;}
   playVideo(e); // the same stationary gate is checked during preparation and playback
 }
 void playVideo(Entry e){
   stop();if(closed||!stationary.allowed())return;
   if(!https(e.url)&&!https(e.web)){status.setText("Canal sin enlace HTTPS compatible.");return;}
   videoActive=true;handler.removeCallbacks(videoGuard);handler.postDelayed(videoGuard,1000);playerColumn.setVisibility(View.VISIBLE);resize(size);status.setText("Conectando: "+e.name+" · "+e.note);
   if(!https(e.url)){openWeb(e);return;}
   try{VideoView v=new VideoView(a);video=v;picture.addView(v,new FrameLayout.LayoutParams(-1,-1,android.view.Gravity.CENTER));v.setOnPreparedListener(mp->{if(video!=v)return;cancelTimeout();if(!stationary.allowed()){stop();return;}v.start();status.setText("En directo: "+e.name);});v.setOnErrorListener((mp,w,x)->{if(video==v){stop();status.setText("Emisión no disponible: "+e.name);offerWeb(e);}return true;});v.setOnCompletionListener(mp->{if(video==v){stop();status.setText("Emisión finalizada: "+e.name);}});v.setVideoURI(Uri.parse(e.url));timeout=()->{if(video==v){stop();status.setText("Tiempo agotado: "+e.name);offerWeb(e);}};handler.postDelayed(timeout,25000);v.start();}catch(Exception ex){stop();status.setText("No se pudo reproducir "+e.name);offerWeb(e);}
 }
 void offerWeb(Entry e){if(!closed&&https(e.web)&&stationary.allowed())new AlertDialog.Builder(a).setMessage("¿Abrir la web oficial de "+e.name+"?").setPositiveButton("Abrir",(d,w)->{if(!closed&&stationary.allowed()){stop();videoActive=true;handler.removeCallbacks(videoGuard);handler.postDelayed(videoGuard,1000);playerColumn.setVisibility(View.VISIBLE);resize(size);openWeb(e);}}).setNegativeButton("Cancelar",null).show();}
 void openWeb(Entry e){try{browser=new WebView(a);browser.getSettings().setJavaScriptEnabled(true);browser.getSettings().setDomStorageEnabled(true);browser.getSettings().setAllowFileAccess(false);browser.getSettings().setAllowContentAccess(false);browser.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);browser.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !https(r.getUrl().toString());}});picture.addView(browser,new FrameLayout.LayoutParams(-1,-1,android.view.Gravity.CENTER));browser.loadUrl(e.web);status.setText("Web oficial: "+e.name+" · DRM según proveedor");}catch(Exception ex){stop();status.setText("Web no disponible: "+e.name);}}
 void cancelTimeout(){if(timeout!=null){handler.removeCallbacks(timeout);timeout=null;}}
 void refresh(){if(closed)return;if(videoActive){if(!stationary.allowed()){stop();status.setText(tablet?"TV detenida: dispositivo fuera del modo tablet.":"TV detenida: movimiento o lectura de velocidad no disponible.");}}else if(!radio.status().equals("Radio detenida"))status.setText(radio.status());}
 void stop(){list.setVisibility(View.VISIBLE);list.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1));cancelTimeout();handler.removeCallbacks(videoGuard);radio.stop();videoActive=false;if(video!=null){VideoView v=video;video=null;try{v.stopPlayback();}catch(Exception ignored){}}if(browser!=null){WebView w=browser;browser=null;try{w.stopLoading();w.loadUrl("about:blank");picture.removeView(w);w.destroy();}catch(Exception ignored){}}if(fullDialog!=null)fullDialog.dismiss();picture.removeAllViews();playerColumn.setVisibility(View.GONE);status.setText("Reproducción detenida");}
 void close(){closed=true;stop();}
}
