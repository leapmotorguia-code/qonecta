package com.leapmotorguia.c10control;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * C10 Control Pro 1.0 — proyecto nuevo, sin dependencias de UNIT/Charly.
 * Ninguna función escribe en centralitas del vehículo ni usa APIs privadas.
 */
public final class MainActivity extends Activity {
    private static final String[] PAGES={"Inicio","Coche","Energía","Prestaciones","Sensores","Aplicaciones","Navegador","Actualizaciones"};
    private static final int REQUEST_GPS=1201,REQUEST_APK=1202;
    private static final String UPDATE_JSON="https://raw.githubusercontent.com/leapmotorguia-code/qonecta/feature/c10-control-original-v1/original-c10-control/update.json";
    private final Handler timer=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){@Override public void run(){refreshLive();timer.postDelayed(this,1000);}};
    private VehicleStore store;
    private TelemetryEngine telemetry;
    private LinearLayout body,nav;
    private TextView liveSpeed,liveGps,perfStatus100,perfStatus120;
    private TextView footer;
    private String page="Inicio";

    @Override public void onCreate(Bundle state){super.onCreate(state);
        store=new VehicleStore(this);
        telemetry=new TelemetryEngine(this,this::refreshLive);
        drawFrame();show("Inicio");timer.post(tick);
    }
    @Override protected void onResume(){super.onResume();if(telemetry!=null)telemetry.start();}
    @Override protected void onPause(){if(telemetry!=null)telemetry.stop();super.onPause();}
    @Override protected void onDestroy(){timer.removeCallbacks(tick);if(telemetry!=null)telemetry.stop();super.onDestroy();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);drawFrame();show(page);}
    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] results){
        super.onRequestPermissionsResult(req,perms,results);
        if(req==REQUEST_GPS){telemetry.start();show(page);}
    }

    private void drawFrame(){
        LinearLayout root=Ui.col(this);root.setBackgroundColor(Ui.BG);
        LinearLayout brand=Ui.row(this);brand.setPadding(Ui.dp(this,19),Ui.dp(this,10),Ui.dp(this,19),Ui.dp(this,8));
        LinearLayout name=Ui.col(this);
        name.addView(Ui.text(this,"C10  /  CONTROL PRO",24,Ui.TEXT,true));
        name.addView(Ui.text(this,"LEAPMOTOR GUÍA  ·  APP ORIGINAL V1.0",12,Ui.MINT,false));
        brand.addView(name,new LinearLayout.LayoutParams(0,-2,1f));
        TextView version=Ui.text(this,"PROTOTIPO  •  SIN ACCESO CAN",12,Ui.YELLOW,true);
        brand.addView(version);root.addView(brand);
        HorizontalScrollView strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);
        strip.setBackgroundColor(Ui.NAV);
        nav=Ui.row(this);nav.setPadding(Ui.dp(this,10),Ui.dp(this,6),Ui.dp(this,10),Ui.dp(this,6));
        strip.addView(nav,new HorizontalScrollView.LayoutParams(-2,-2));
        root.addView(strip);
        body=Ui.col(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1f));
        footer=Ui.text(this,"C10 Control Pro · Proyecto independiente · Pruebas con vehículo estacionado",12,Ui.DIM,false);
        footer.setBackgroundColor(Ui.NAV);
        footer.setPadding(Ui.dp(this,18),Ui.dp(this,7),Ui.dp(this,12),Ui.dp(this,7));
        root.addView(footer);
        setContentView(root);
    }

    private void show(String target){
        page=target;liveSpeed=null;liveGps=null;perfStatus100=null;perfStatus120=null;
        nav.removeAllViews();
        for(String n:PAGES){
            Button b=Ui.button(this,n,false);
            if(n.equals(target)){
                b.setTextColor(0xff071d20);b.setBackground(Ui.bg(Ui.MINT,Ui.dp(this,10),0));
            }
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,Ui.dp(this,47));p.rightMargin=Ui.dp(this,8);
            nav.addView(b,p);b.setOnClickListener(v->show(n));
        }
        ScrollView scroll=Ui.page(this,body);
        LinearLayout c=Ui.content(scroll);
        if("Inicio".equals(target))home(c);
        else if("Coche".equals(target))vehicle(c);
        else if("Energía".equals(target))energy(c);
        else if("Prestaciones".equals(target))performance(c);
        else if("Sensores".equals(target))diagnostics(c);
        else if("Aplicaciones".equals(target))apps(c);
        else if("Navegador".equals(target))browser(c);
        else if("Actualizaciones".equals(target))updates(c);
        refreshLive();
    }

    private void addButton(LinearLayout c,String text,boolean primary,View.OnClickListener cb){
        Button b=Ui.button(this,text,primary);
        LinearLayout.LayoutParams p=Ui.match(Ui.dp(this,52));p.bottomMargin=Ui.dp(this,10);
        c.addView(b,p);b.setOnClickListener(cb);
    }
    private void line(LinearLayout box,String label,String value){Ui.textRow(this,box,label,value,Ui.TEXT);}
    private void gap(LinearLayout l){Ui.smallGap(this,l);}
    private void home(LinearLayout c){
        c.addView(Ui.title(this,"Tu C10, todo en un mismo lugar"));
        c.addView(Ui.caption(this,"Nueva aplicación independiente para pruebas reales en LeapOS. Ninguna pantalla modifica el coche."));
        LinearLayout overview=Ui.card(this,c,"ESTADO EN DIRECTO");
        liveSpeed=Ui.text(this,"— km/h",43,Ui.TEXT,true);overview.addView(liveSpeed);
        liveGps=Ui.text(this,"Comprobando GPS Android…",15,Ui.DIM,false);overview.addView(liveGps);
        line(overview,"Vehículo seleccionado",store.modelName());
        line(overview,"Batería útil de referencia",EnergyLedger.fmt(store.usable(),"kWh"));
        addButton(overview,"Activar GPS / permitir ubicación",false,v->askGps());
        LinearLayout p=Ui.card(this,c,"PANTALLAS DE ESTA VERSIÓN");
        Ui.textRow(this,p,"Coche","Perfiles y datos de referencia",Ui.MINT);
        Ui.textRow(this,p,"Energía","SOC, kWh, regeneración y combustible",Ui.MINT);
        Ui.textRow(this,p,"Prestaciones","0–100 y 80–120 por GPS",Ui.MINT);
        Ui.textRow(this,p,"Sensores","Auditoría real de accesos Android",Ui.MINT);
        Ui.textRow(this,p,"Aplicaciones","Google Maps, Spotify y más",Ui.MINT);
        Ui.textRow(this,p,"Navegador","WebView Android integrado",Ui.MINT);
        c.addView(Ui.note(this,"Android Auto, CarPlay, cámaras 360° y telemetría CAN/BMS todavía NO están integrados. " +
                "Se mantienen así identificados para evitar resultados inventados."));
    }

    private void vehicle(LinearLayout c){
        c.addView(Ui.title(this,"Seleccionar vehículo"));
        c.addView(Ui.caption(this,"Perfil local para cálculos. No altera ni programa ningún componente del Leapmotor."));
        LinearLayout box=Ui.card(this,c,"MODELO DE COCHE");
        Spinner selector=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,VehicleStore.NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selector.setAdapter(adapter);selector.setSelection(store.model());
        box.addView(selector,new LinearLayout.LayoutParams(-1,Ui.dp(this,54)));
        addButton(box,"Guardar modelo seleccionado",true,v->{
            store.model(selector.getSelectedItemPosition());store.resetUsable();toast("Modelo: "+store.modelName());show("Coche");
        });
        line(box,"Modelo actual",store.modelName());
        line(box,"Batería nominal (referencia)",EnergyLedger.fmt(store.nominal(),"kWh"));
        line(box,"Batería útil para cálculos",EnergyLedger.fmt(store.usable(),"kWh"));
        line(box,"Autonomía extendida",store.isReev()?"Sí, REEV":"No / no modelada");
        LinearLayout cap=Ui.card(this,c,"CAPACIDAD ÚTIL AJUSTABLE");
        EditText amount=Ui.number(this,"kWh útiles");
        Ui.field(this,cap,"Personaliza la batería útil para los cálculos",amount,
                String.format(Locale.US,"%.1f",store.usable()));
        addButton(cap,"Guardar capacidad útil",false,v->{
            double x=EnergyLedger.parse(amount.getText().toString());
            if(!(x>=5&&x<=150)){toast("Introduce un valor entre 5 y 150 kWh");return;}
            store.usable(x);toast("Capacidad guardada");show("Coche");
        });
        c.addView(Ui.note(this,"Las capacidades son valores de referencia del modelo, no lecturas del BMS. " +
                "El perfil no proporciona acceso remoto, cámaras ni control del vehículo."));
    }

    private void energy(LinearLayout c){
        c.addView(Ui.title(this,"Balance de energía y consumos"));
        c.addView(Ui.caption(this,"Registro propio: valores escritos por ti. Sin acceso al contador de regeneración del BMS."));
        LinearLayout inputs=Ui.card(this,c,"DATOS DE UN TRAMO");
        EditText s0=Ui.number(this,"Ej.: 90");
        EditText s1=Ui.number(this,"Ej.: 52");
        EditText km=Ui.number(this,"Ej.: 102.5");
        EditText gross=Ui.number(this,"Solo si tienes un dato fiable");
        EditText regen=Ui.number(this,"Solo si tienes un dato fiable");
        EditText fuel=Ui.number(this,"Ej.: 3.5");
        Ui.field(this,inputs,"Batería al inicio (%)",s0,store.getString("energy_s0",""));
        Ui.field(this,inputs,"Batería al final (%)",s1,store.getString("energy_s1",""));
        Ui.field(this,inputs,"Distancia del tramo (km)",km,store.getString("energy_km",""));
        Ui.field(this,inputs,"Energía bruta gastada (kWh) · opcional",gross,store.getString("energy_gross",""));
        Ui.field(this,inputs,"Energía recuperada (kWh) · opcional",regen,store.getString("energy_regen",""));
        Ui.field(this,inputs,"Combustible utilizado (L) · opcional",fuel,store.getString("energy_fuel",""));
        addButton(inputs,"Calcular y guardar tramo",true,v->{
            store.setString("energy_s0",s0.getText().toString());
            store.setString("energy_s1",s1.getText().toString());
            store.setString("energy_km",km.getText().toString());
            store.setString("energy_gross",gross.getText().toString());
            store.setString("energy_regen",regen.getText().toString());
            store.setString("energy_fuel",fuel.getText().toString());
            show("Energía");
        });
        EnergyLedger r=new EnergyLedger(
                EnergyLedger.parse(store.getString("energy_s0","")),
                EnergyLedger.parse(store.getString("energy_s1","")),
                EnergyLedger.parse(store.getString("energy_km","")),
                EnergyLedger.parse(store.getString("energy_gross","")),
                EnergyLedger.parse(store.getString("energy_regen","")),
                EnergyLedger.parse(store.getString("energy_fuel","")),store.usable());
        LinearLayout results=Ui.card(this,c,"RESULTADOS · SOLO DATOS INTRODUCIDOS");
        line(results,"Energía neta de batería por diferencia SOC",r.socValid()?EnergyLedger.fmt(r.batteryNet(),"kWh"):"—");
        line(results,"Consumo neto SOC / distancia",EnergyLedger.fmt(r.netPer100(),"kWh/100 km"));
        line(results,"Energía bruta gastada declarada",r.manualValid()?EnergyLedger.fmt(r.grossKwh,"kWh"):"—");
        line(results,"Energía regenerada declarada",r.manualValid()?EnergyLedger.fmt(r.recoveredKwh,"kWh"):"—");
        line(results,"Balance bruto menos regeneración",r.manualValid()?EnergyLedger.fmt(r.manualNet(),"kWh"):"—");
        line(results,"Recuperada / gastada declarada",EnergyLedger.fmt(r.recoverPercent(),"%"));
        line(results,"Consumo gasolina",EnergyLedger.fmt(r.fuelPer100(),"L/100 km"));
        if(r.socValid() && r.batteryNet()>0 && r.fuelL==0){
            line(results,"Autonomía proyectada al mismo ritmo",
                    EnergyLedger.fmt(store.usable()/r.batteryNet()*r.km,"km"));
        }
        addButton(results,"Borrar los datos del tramo",false,v->{
            for(String key:new String[]{"energy_s0","energy_s1","energy_km","energy_gross","energy_regen","energy_fuel"})store.setString(key,"");
            toast("Tramo borrado");show("Energía");
        });
        c.addView(Ui.note(this,"ATENCIÓN: el descenso de porcentaje de batería NO mide por separado energía consumida y regenerada. " +
                "Si el generador REEV ha funcionado, el SOC tampoco representa toda la energía usada para mover el coche. " +
                "No se inventan cifras cuando no hay lecturas."));
    }

    private void performance(LinearLayout c){
        c.addView(Ui.title(this,"Pruebas de prestaciones"));
        c.addView(Ui.caption(this,"Medición automática orientativa mediante velocidad GPS. Requiere buena cobertura."));
        LinearLayout speed=Ui.card(this,c,"VELOCIDAD GPS EN DIRECTO");
        liveSpeed=Ui.text(this,"— km/h",46,Ui.TEXT,true);speed.addView(liveSpeed);
        liveGps=Ui.text(this,"Esperando ubicación…",15,Ui.DIM,false);speed.addView(liveGps);
        addButton(speed,"Permitir ubicación GPS",false,v->askGps());
        LinearLayout zero=Ui.card(this,c,"ACELERACIÓN 0–100 KM/H");
        perfStatus100=Ui.text(this,telemetry.accel().s100(),19,Ui.TEXT,true);zero.addView(perfStatus100);
        line(zero,"Última prueba guardada",telemetry.accel().best100());
        line(zero,"Inicio","Tras estar parado a 0–4 km/h");
        LinearLayout eighty=Ui.card(this,c,"RECUPERACIÓN 80–120 KM/H");
        perfStatus120=Ui.text(this,telemetry.accel().s120(),19,Ui.TEXT,true);eighty.addView(perfStatus120);
        line(eighty,"Última prueba guardada",telemetry.accel().best120());
        line(eighty,"Preparación","Mantener 80 ±1,5 km/h durante 3 s");
        addButton(eighty,"Rearmar detección automática",false,v->{telemetry.accel().reset();refreshLive();});
        c.addView(Ui.note(this,"Estas mediciones NO son homologadas. La precisión depende del GPS y de su frecuencia de actualización. " +
                "Solo deben realizarse legalmente, en circuito o entorno cerrado y sin manipular la pantalla al conducir."));
    }

    private void diagnostics(LinearLayout c){
        c.addView(Ui.title(this,"Sensores y accesos disponibles"));
        c.addView(Ui.caption(this,"Inventario real y de solo lectura del sistema Android donde se instala la APK."));
        LinearLayout actions=Ui.card(this,c,"AUDITORÍA DEL DISPOSITIVO");
        final TextView report=Ui.text(this,"",14,Ui.TEXT,false);
        report.setTextIsSelectable(true);
        Runnable reload=()->report.setText(DeviceAudit.report(this));
        addButton(actions,"↻  Actualizar informe",false,v->reload.run());
        addButton(actions,"Copiar informe al portapapeles",true,v->{
            ClipboardManager cb=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if(cb!=null){cb.setPrimaryClip(ClipData.newPlainText("Informe C10 Control Pro",report.getText()));toast("Informe copiado");}
        });
        LinearLayout data=Ui.card(this,c,null);data.addView(report);
        c.addView(Ui.note(this,"Un identificador Camera2 detectado es una cámara de Android, no necesariamente la cámara delantera, " +
                "trasera o 360º de aparcamiento. Los permisos de Android no conceden automáticamente acceso a CAN/BMS."));
        reload.run();
    }

    private static final String[][] APPS={
        {"Google Maps","com.google.android.apps.maps","https://maps.google.com/","N"},
        {"Spotify","com.spotify.music","https://open.spotify.com/","N"},
        {"YouTube","com.google.android.youtube","https://www.youtube.com/","V"},
        {"Netflix","com.netflix.mediaclient","https://www.netflix.com/","V"},
        {"Max / HBO","com.wbd.stream","https://www.max.com/","V"},
        {"Prime Video","com.amazon.avod.thirdpartyclient","https://www.primevideo.com/","V"},
        {"Pluto TV","tv.pluto.android","https://pluto.tv/","V"}
    };
    private void apps(LinearLayout c){
        c.addView(Ui.title(this,"Aplicaciones y multimedia"));
        c.addView(Ui.caption(this,"Abre las apps Android instaladas. Si no existen, intenta abrir sus páginas web oficiales."));
        for(String[] a:APPS){
            boolean available=getPackageManager().getLaunchIntentForPackage(a[1])!=null;
            LinearLayout box=Ui.card(this,c,a[0]);
            line(box,"Aplicación instalada",available?"Sí, lanzable":"No detectada");
            if(a[3].equals("V"))box.addView(Ui.text(this,"Vídeo solo con el vehículo estacionado",14,Ui.YELLOW,false));
            addButton(box,available?"Abrir aplicación":"Abrir página oficial",false,v->openApp(a));
        }
        LinearLayout projection=Ui.card(this,c,"ANDROID AUTO / APPLE CARPLAY");
        projection.addView(Ui.text(this,"Esta aplicación no contiene receptores propios de Android Auto o CarPlay. "+
                "Podemos contrastar su funcionamiento con UNIT, pero no lo presentamos como integrado.",15,Ui.DIM,false));
        addButton(projection,"Abrir UNIT existente (solo comparación)",false,v->{
            Intent i=getPackageManager().getLaunchIntentForPackage("com.tencent.qqmusiccar");
            if(i==null){toast("UNIT no está instalada o no es visible");return;}
            try{startActivity(i);}catch(Exception e){toast("UNIT no se pudo abrir");}
        });
        c.addView(Ui.note(this,"Netflix, Max, Prime Video y otras plataformas pueden requerir DRM/Widevine, " +
                "certificación y compatibilidad de LeapOS. Mostrar un acceso no garantiza reproducción."));
    }
    private void openApp(String[] app){
        if("V".equals(app[3])){
            if(telemetry.speedKmh()>3){toast("Vídeo bloqueado: velocidad GPS indica movimiento");return;}
            new AlertDialog.Builder(this).setTitle("Vídeo solo estacionado")
                    .setMessage("Confirma que el coche está estacionado. Si no hay GPS disponible, " +
                            "la aplicación no puede comprobarlo. No uses vídeo conduciendo.")
                    .setNegativeButton("Cancelar",null)
                    .setPositiveButton("Estoy estacionado",(d,w)->openAppNow(app))
                    .show();
        }else openAppNow(app);
    }
    private void openAppNow(String[] app){
        try{
            Intent intent=getPackageManager().getLaunchIntentForPackage(app[1]);
            if(intent==null)intent=new Intent(Intent.ACTION_VIEW,Uri.parse(app[2]));
            startActivity(intent);
        }catch(Exception e){toast("LeapOS no permite abrir este destino: "+e.getClass().getSimpleName());}
    }
    private void browser(LinearLayout c){
        c.addView(Ui.title(this,"Navegador propio"));
        c.addView(Ui.caption(this,"Navegador web de la pantalla Android. Las páginas usan el motor WebView disponible."));
        LinearLayout box=Ui.card(this,c,"ABRIR NAVEGACIÓN");
        addButton(box,"Iniciar navegador",true,v->openBrowser("https://www.google.com/"));
        addButton(box,"Abrir Google Maps web",false,v->openBrowser("https://maps.google.com/"));
        addButton(box,"Abrir Leapmotor Guía",false,v->openBrowser("https://leapguia.impresor.chatgpt.site/"));
        c.addView(Ui.note(this,"El navegador no sustituye al sistema completo de aplicaciones Android. " +
                "El vídeo protegido puede no funcionar en WebView aunque lo haga en una aplicación nativa."));
    }
    private void openBrowser(String url){Intent i=new Intent(this,BrowserActivity.class);i.putExtra("url",url);startActivity(i);}
    private void updates(LinearLayout c){
        c.addView(Ui.title(this,"Actualizaciones del sistema"));
        c.addView(Ui.caption(this,"Actualizaciones de C10 Control Pro; no modifica firmware del coche ni LeapOS."));
        LinearLayout box=Ui.card(this,c,"VERSIÓN ACTUAL");
        line(box,"Aplicación","C10 Control Pro");
        line(box,"Versión","1.0.0-original");
        line(box,"Paquete",getPackageName());
        TextView state=Ui.text(this,"No se ha consultado el manifiesto de versiones",15,Ui.DIM,false);
        box.addView(state);
        addButton(box,"Comprobar versión publicada",false,v->checkForUpdates(state));
        addButton(box,"Instalar APK de actualización descargada",true,v->chooseApk());
        c.addView(Ui.note(this,"La instalación se realiza mediante el instalador de Android y requiere tu autorización. " +
                "Una actualización futura deberá tener exactamente este paquete y una firma compatible. " +
                "No se instala software del coche de forma automática."));
    }
    private void checkForUpdates(TextView state){
        state.setText("Comprobando conexión…");
        new Thread(()->{
            String result;
            HttpURLConnection conn=null;
            try{
                conn=(HttpURLConnection)new URL(UPDATE_JSON).openConnection();
                conn.setConnectTimeout(7000);conn.setReadTimeout(7000);conn.setRequestProperty("Accept","application/json");
                if(conn.getResponseCode()!=200)throw new Exception("HTTP "+conn.getResponseCode());
                StringBuilder b=new StringBuilder();
                try(BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()))){
                    String line;while((line=br.readLine())!=null && b.length()<8192)b.append(line);
                }
                JSONObject json=new JSONObject(b.toString());
                int remote=json.optInt("versionCode",0);
                String version=json.optString("versionName","desconocida");
                String link=json.optString("apkUrl","");
                if(remote>1 && link.startsWith("https://")){
                    final String availableMessage="Hay una versión nueva "+version+". Abre el enlace de descarga oficial.";
                    final String url=link;
                    runOnUiThread(()->{
                        state.setText(availableMessage);new AlertDialog.Builder(this).setTitle("Versión disponible")
                            .setMessage("Versión "+version+". Se descargará desde el enlace del proyecto. " +
                                    "Después selecciona la APK desde esta pantalla para instalarla.")
                            .setNegativeButton("Cancelar",null)
                            .setPositiveButton("Abrir descarga",(d,w)->{
                                try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
                                catch(Exception e){toast("No se puede abrir la descarga");}
                            }).show();
                    });
                    return;
                }
                result=remote>1?"Versión futura sin APK publicada":"Esta es la versión publicada más reciente ("+version+")";
            }catch(Exception ex){result="Sin comprobación disponible: "+ex.getMessage();}
            finally{if(conn!=null)conn.disconnect();}
            final String message=result;
            runOnUiThread(()->{if(!isFinishing())state.setText(message);});
        },"c10-update-check").start();
    }
    private void chooseApk(){
        Intent select=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        select.setType("*/*");select.addCategory(Intent.CATEGORY_OPENABLE);
        try{startActivityForResult(select,REQUEST_APK);}catch(Exception e){toast("No hay selector de archivos disponible");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request!=REQUEST_APK||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try{
            if(Build.VERSION.SDK_INT>=26 && !getPackageManager().canRequestPackageInstalls()){
                toast("Autoriza instalar apps desconocidas para C10 Control Pro y repite la selección");
                Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:"+getPackageName()));startActivity(settings);return;
            }
            Intent install=new Intent(Intent.ACTION_INSTALL_PACKAGE);
            install.setDataAndType(uri,"application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
        }catch(Exception e){toast("Instalador Android no disponible: "+e.getClass().getSimpleName());}
    }
    private void askGps(){
        if(telemetry.permitted()){telemetry.start();toast("Se ha solicitado ubicación al sistema");return;}
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_GPS);
    }
    private void refreshLive(){
        if(telemetry==null)return;
        if(liveSpeed!=null){
            double v=telemetry.speedKmh();liveSpeed.setText(v>=0?String.format(Locale.getDefault(),"%.1f km/h",v):"— km/h");
        }
        if(liveGps!=null){
            Location l=telemetry.fresh();
            liveGps.setText(l==null?telemetry.status():"GPS Android: "+l.getProvider()+" · precisión posicional "+
                    String.format(Locale.getDefault(),"%.0f m",l.getAccuracy()));
        }
        if(perfStatus100!=null)perfStatus100.setText(telemetry.accel().s100());
        if(perfStatus120!=null)perfStatus120.setText(telemetry.accel().s120());
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
