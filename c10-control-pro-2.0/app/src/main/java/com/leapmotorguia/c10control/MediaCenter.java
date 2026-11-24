package com.leapmotorguia.c10control;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import android.text.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class MediaCenter {
 final MainActivity a; LinearLayout host,output; TextView status; VideoView video; WebView web; int generation; Channel current; boolean requested=false;
 static final class Channel { final String cat,name,stream,web,info; Channel(String[] x){cat=x[0];name=x[1];stream=x[2];web=x[3];info=x[4];} }
 final ArrayList<Channel> all=new ArrayList<>(),filtered=new ArrayList<>();
 MediaCenter(MainActivity a){this.a=a;try(BufferedReader r=new BufferedReader(new InputStreamReader(a.getAssets().open("tv-catalog.tsv"),StandardCharsets.UTF_8))){String s;while((s=r.readLine())!=null){String[] x=s.split("\\t",-1);if(x.length==5)all.add(new Channel(x));}}catch(IOException e){android.util.Log.e("CockpitMedia","Catalog",e);}}
 boolean busy(){return video!=null||web!=null;}
 boolean visible(){return host!=null;}
 void close(){stop();host=null;output=null;status=null;current=null;}
 void stop(){generation++;requested=false;if(video!=null){try{video.stopPlayback();}catch(Exception ignored){}video=null;}if(web!=null){try{web.onPause();web.stopLoading();web.destroy();}catch(Exception ignored){}web=null;}if(output!=null)output.removeAllViews();}
 void message(String s){if(status!=null)status.setText(s);}
 boolean allowed(){return a.videoAllowed()&&output!=null;}
 void show(LinearLayout l,String page){
 a.title(l,page);
 if(!a.videoAllowed()){a.label(l,"Vídeo y servicios web: estacionado en el coche. En el Samsung de prueba no se exige GPS.");a.btn(l,"Confirmar estacionamiento",a::parkedPrompt);return;}
 if(host!=null){a.label(l,"Ya hay un centro multimedia abierto en el otro panel.");return;}
 host=l;status=a.text("Selecciona un canal o servicio",15,a.fg);l.addView(status);
 LinearLayout bar=a.row();l.addView(bar);bar.addView(a.button("Detener",()->{stop();message("Reproducción detenida");}));bar.addView(a.button("Atrás web",()->{if(web!=null&&web.canGoBack())web.goBack();}));bar.addView(a.button("Web oficial",()->{if(current!=null&&!current.web.isEmpty())openWeb(current.web,current.name);else message("No hay página oficial seleccionada");}));
 LinearLayout area=a.row();area.setGravity(Gravity.TOP);l.addView(area,new LinearLayout.LayoutParams(-1,0,1));
 LinearLayout side=a.col();area.addView(side,new LinearLayout.LayoutParams(0,-1,38));
 output=a.col();area.addView(output,new LinearLayout.LayoutParams(0,-1,62));
 if(page.equals("TV / TDT"))catalog(side);else services(side,page);
 }
 void catalog(LinearLayout side){
 a.btn(side,"Pluto TV · directo y a la carta",()->{current=null;openWeb("https://pluto.tv/","Pluto TV");});
 a.label(side,all.size()+" canales · catálogo en abierto");
 final ArrayList<String> categories=new ArrayList<>();categories.add("Todos");for(Channel c:all)if(!categories.contains(c.cat))categories.add(c.cat);
 Spinner spinner=new Spinner(a);ArrayAdapter<String> ca=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,categories);ca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(ca);side.addView(spinner);
 EditText search=new EditText(a);search.setSingleLine(true);search.setHint("Buscar canal");search.setTextColor(a.fg);search.setHintTextColor(a.fg);side.addView(search);
 ListView list=new ListView(a);side.addView(list,new LinearLayout.LayoutParams(-1,0,1));
 ArrayAdapter<String> adapter=new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,new ArrayList<String>()){@Override public View getView(int p,View v,android.view.ViewGroup g){TextView t=(TextView)super.getView(p,v,g);t.setTextColor(a.fg);t.setTextSize(14);return t;}};list.setAdapter(adapter);
 Runnable filter=()->{String c=String.valueOf(spinner.getSelectedItem()),q=search.getText().toString().toLowerCase(Locale.ROOT).trim();filtered.clear();adapter.clear();for(Channel ch:all)if((c.equals("Todos")||ch.cat.equals(c))&&ch.name.toLowerCase(Locale.ROOT).contains(q)){filtered.add(ch);adapter.add(ch.name+(ch.stream.isEmpty()?" · web":" · directo"));}adapter.notifyDataSetChanged();};
 spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int i,long id){filter.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
 search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int f){}public void onTextChanged(CharSequence s,int st,int b,int c){filter.run();}public void afterTextChanged(Editable e){}});
 list.setOnItemClickListener((p,v,i,id)->{if(i>=filtered.size())return;current=filtered.get(i);if(current.stream.isEmpty())openWeb(current.web,current.name);else play(current);});filter.run();
 }
 void services(LinearLayout side,String page){
 ScrollView scroll=new ScrollView(a);side.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout items=a.col();scroll.addView(items);
 String[][] urls={{"Pluto TV","https://pluto.tv/"},{"RTVE Play","https://www.rtve.es/play/"},{"3Cat","https://www.3cat.cat/3cat/"},{"Atresplayer","https://www.atresplayer.com/directos/"},{"Mediaset Infinity","https://www.mediasetinfinity.es/"},{"YouTube","https://m.youtube.com/"},{"Netflix","https://www.netflix.com/"},{"Max","https://www.max.com/"},{"Prime Video","https://www.primevideo.com/"},{"Spotify web","https://open.spotify.com/"},{"YouTube Music","https://music.youtube.com/"},{"Google Maps web","https://maps.google.com/"},{"Waze mapa web","https://www.waze.com/live-map/"}};
 for(String[] x:urls)a.btn(items,x[0],()->{current=null;openWeb(x[1],x[0]);});
 if(page.equals("Web / YouTube")){EditText address=new EditText(a);address.setHint("Dirección HTTPS");address.setSingleLine(true);address.setTextColor(a.fg);items.addView(address);a.btn(items,"Abrir dentro de la app",()->{String u=address.getText().toString().trim();if(!u.startsWith("https://"))u="https://"+u;current=null;openWeb(u,"Navegador");});}
 a.label(items,"Servicios web sujetos a compatibilidad y cuentas. No se abre ninguna app externa.");
 }
 void play(Channel c){
 if(!allowed())return;stop();a.radio.stop();current=c;int token=++generation;requested=true;VideoView v=new VideoView(a);video=v;output.addView(v,new LinearLayout.LayoutParams(-1,-1));message("Conectando: "+c.name);
 try{v.setOnPreparedListener(mp->{if(generation!=token||video!=v||!allowed()){if(video==v)stop();return;}requested=false;v.start();message("En directo: "+c.name);});
 v.setOnErrorListener((mp,w,e)->{if(generation==token&&video==v){stop();message("No disponible: "+c.name+". Prueba Web oficial.");}return true;});
 v.setOnCompletionListener(mp->{if(generation==token&&video==v){stop();message("Emisión terminada: "+c.name);}});
 v.setVideoURI(Uri.parse(c.stream));v.start();a.handler.postDelayed(()->{if(generation==token&&video==v&&requested){stop();message("Sin respuesta: "+c.name+". Prueba Web oficial.");}},25000);
 }catch(Exception e){stop();message("No se pudo abrir "+c.name);}
 }
 void openWeb(String url,String name){
 if(!allowed()||url==null||!url.startsWith("https://")||Uri.parse(url).getHost()==null){message("Dirección no disponible");return;}
 stop();a.radio.stop();try{WebView w=new WebView(a);web=w;w.setBackgroundColor(Color.BLACK);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setMediaPlaybackRequiresUserGesture(true);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSupportMultipleWindows(false);s.setSafeBrowsingEnabled(true);
 w.setWebChromeClient(new WebChromeClient());w.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){boolean ok=allowed()&&"https".equalsIgnoreCase(r.getUrl().getScheme());if(!ok)message("Enlace externo o no permitido bloqueado");return !ok;}@Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())message("Web no disponible: "+name);}});w.setDownloadListener((u,ua,c,m,n)->message("Descarga externa no permitida"));output.addView(w,new LinearLayout.LayoutParams(-1,-1));message(name+" · web interna (puede requerir inicio de sesión)");w.loadUrl(url);
 }catch(Exception e){stop();message("WebView incompatible: "+name);}
 }
}
