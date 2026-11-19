package com.headqlink.link;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** New native shell; original CarScreen engines remain credited under AGPL-3.0. */
public final class C10NativeActivity extends Activity {
    private static final int LOCATION=91, MEDIA=92, EXPORT=93, FOLDER=94;
    private static final int BG=0xff0c1721, INK=0xffedf7f5, MINT=0xff67e3b7;
    private FrameLayout body;
    private LinearLayout root;
    private CarScreen current;
    private String section="Inicio", pending;
    private boolean paused;
    private int dp(int v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    private TextView text(String s,int size) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(INK);
        t.setPadding(dp(12),dp(8),dp(12),dp(8)); t.setTextIsSelectable(true); return t;
    }
    private void action(LinearLayout col,String name,Runnable task) {
        Button b=new Button(this); b.setText(name); b.setAllCaps(false); b.setMinHeight(dp(52));
        b.setOnClickListener(v->task.run()); col.addView(b,new LinearLayout.LayoutParams(-1,-2));
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); L.init(this); Str.init(this);
        Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            // Exception type and our call sites only: no credentials, URLs, VIN or device identifiers.
            StringBuilder trace=new StringBuilder(error.getClass().getName()+"\n");
            for(StackTraceElement e:error.getStackTrace()) if(e.getClassName().startsWith("com.headqlink")||e.getClassName().startsWith("com.leapmotorguia")) trace.append(e).append('\n');
            getSharedPreferences("c10_rebuild",MODE_PRIVATE).edit().putString("last_crash",trace.toString()).commit();
            if(previous!=null)previous.uncaughtException(thread,error);
        });
        if(!getSharedPreferences("c10_rebuild",MODE_PRIVATE).getBoolean("profile",false)) {
            new CarCloudStore(this).setProfile(CarCloudStore.PROFILE_C10_REEV,0);
            getSharedPreferences("c10_rebuild",MODE_PRIVATE).edit().putBoolean("profile",true).apply();
        }
        root=new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(BG);
        TextView title=text("C10 ControlPlus · 1.0 PRUEBA",22); title.setTextColor(MINT); root.addView(title);
        HorizontalScrollView bar=new HorizontalScrollView(this); LinearLayout row=new LinearLayout(this);
        for(String name:new String[]{"Inicio","Coche","Instrumentos","Eficiencia","Radio","Web","Fotos","Vídeos","TV","Herramientas","OBD","Ajustes","Diagnóstico","Integraciones"}) {
            Button b=new Button(this); b.setText(name); b.setAllCaps(false); b.setContentDescription("nav:"+name);
            b.setOnClickListener(v->select(name));row.addView(b,new LinearLayout.LayoutParams(-2,dp(54)));
        }
        bar.addView(row);root.addView(bar);body=new FrameLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        if(Build.VERSION.SDK_INT>=35)root.setOnApplyWindowInsetsListener((v,in)->{
            android.graphics.Insets safe=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
            v.setPadding(safe.left,safe.top,safe.right,safe.bottom);return in;
        });
        show("Inicio");
    }
    private LinearLayout page() {
        ScrollView sc=new ScrollView(this);LinearLayout p=new LinearLayout(this);p.setOrientation(1);p.setPadding(dp(16),dp(10),dp(16),dp(20));
        sc.addView(p);body.addView(sc,new FrameLayout.LayoutParams(-1,-1));return p;
    }
    private void clear() {
        if(current!=null)try{current.destroy();}catch(RuntimeException e){remember(e);}
        current=null;body.removeAllViews();
    }
    private void remember(Throwable e) {
        getSharedPreferences("c10_rebuild",MODE_PRIVATE).edit().putString("last_panel_error",section+": "+e.getClass().getSimpleName()).apply();
    }
    private void launch(Class<?> type) {
        try{startActivity(new Intent(this,type));}catch(RuntimeException e){remember(e);new AlertDialog.Builder(this).setMessage("Este componente no se pudo abrir: "+e.getClass().getSimpleName()).setPositiveButton("Cerrar",null).show();}
    }
    private void select(String name) {
        if(name.equals("Herramientas")){launch(com.leapmotorguia.c10nativa.MainActivity.class);return;}
        if(name.equals("OBD")){launch(com.leapmotorguia.c10nativa.ObdActivity.class);return;}
        if(Arrays.asList("Coche","Instrumentos","Eficiencia").contains(name)&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            pending=name;requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION);return;
        }
        if(Arrays.asList("Web","Fotos","Vídeos","TV").contains(name)) {
            new AlertDialog.Builder(this).setTitle("Uso estacionado").setMessage("La app no recibe una señal P validada del coche. Abre este contenido con el vehículo estacionado.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Estoy estacionado",(d,w)->media(name)).show();return;
        }
        show(name);
    }
    private void media(String name) {
        if(name.equals("Fotos")||name.equals("Vídeos")) {
            String perm=Build.VERSION.SDK_INT>=33?(name.equals("Fotos")?Manifest.permission.READ_MEDIA_IMAGES:Manifest.permission.READ_MEDIA_VIDEO):Manifest.permission.READ_EXTERNAL_STORAGE;
            if(checkSelfPermission(perm)!=PackageManager.PERMISSION_GRANTED){pending=name;requestPermissions(new String[]{perm},MEDIA);return;}
        }
        show(name);
    }
    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] results) {
        super.onRequestPermissionsResult(req,perms,results);
        if(req==LOCATION||req==MEDIA){String name=pending;pending=null;if(name!=null)show(name);}
    }
    private void show(String name) {
        clear();section=name;
        try {
            switch(name) {
                case "Coche":current=new CarHubScreen();break;
                case "Instrumentos":current=new InstrumentsScreen();break;
                case "Eficiencia":current=new EfficiencyScreen();break;
                case "Radio":current=new RadioScreen();break;
                case "Web":current=new WebScreen();break;
                case "Fotos":current=new GalleryScreen(false);break;
                case "Vídeos":current=new GalleryScreen(true);break;
                case "TV":current=new IptvScreen();break;
                case "Diagnóstico":diagnostics();return;
                case "Ajustes":settings();return;
                case "Integraciones":integrations();return;
                default:home();return;
            }
            CarStyle.setUiScale(1f,false);CarKit.narrow=host.width()<CarUi.NARROW_MIN_W;CarKit.pageH=host.height();
            try{body.addView(current.create(host),new FrameLayout.LayoutParams(-1,-1));}finally{CarKit.narrow=false;}
        } catch(RuntimeException|LinkageError error) {
            remember(error);clear();LinearLayout p=page();p.addView(text("El panel no pudo abrirse: "+error.getClass().getSimpleName()+". Consulta Diagnóstico.",20));action(p,"Volver al inicio",()->show("Inicio"));
        }
    }
    private void home() {
        LinearLayout p=page();p.addView(text("Tu panel nativo para el C10 REEV",26));
        p.addView(text("Conserva los paneles de coche, ruta, viajes, instrumentos, eficiencia, radio, navegador y multimedia de Qonecta+. Inicio y herramientas de diagnóstico reconstruidos para esta prueba.",18));
        p.addView(text("Los valores del modelo son estimaciones. Solo una fuente GPS, nube u OBD conectada puede aportar sus datos; no se sustituyen lecturas ausentes por datos inventados.",18));
        action(p,"Abrir coche y rutas",()->select("Coche"));action(p,"Prestaciones GPS y cálculos",()->select("Herramientas"));
        action(p,"OBD de solo lectura",()->select("OBD"));action(p,"Comprobar este dispositivo",()->show("Diagnóstico"));
        p.addView(text("La instalación en LeapOS no está validada. No es equivalente a todas las funciones de la app de teléfono ni incluye una integración nativa con CarPlay/Android Auto.",17));
        p.addView(text("Derivado de Qonecta+ / CharlysEV, HeadQLink / ryazrm y Open Headunit. Licencias y autores conservados. AGPL-3.0.",16));
    }
    private String report() {
        StringBuilder r=new StringBuilder("C10 ControlPlus 1.0 · INFORME SIN CREDENCIALES\n");
        r.append("Paquete: ").append(getPackageName()).append("\nAndroid: ").append(Build.VERSION.RELEASE).append(" · API ").append(Build.VERSION.SDK_INT);
        r.append("\nModelo: ").append(Build.MANUFACTURER).append(" / ").append(Build.MODEL).append("\nABI: ").append(Arrays.toString(Build.SUPPORTED_ABIS));
        r.append("\nSDK mínimo: ").append(getApplicationInfo().minSdkVersion).append(" · objetivo: ").append(getApplicationInfo().targetSdkVersion);
        r.append("\nEspacio libre: ").append(getFilesDir().getUsableSpace()/1048576).append(" MiB\n");
        for(String f:new String[]{"android.hardware.location.gps","android.hardware.bluetooth","android.hardware.camera.any"})r.append(f).append(": ").append(getPackageManager().hasSystemFeature(f)).append('\n');
        try{CameraManager cm=(CameraManager)getSystemService(CAMERA_SERVICE);r.append("Cámaras declaradas a Android: ").append(cm==null?"sin servicio":cm.getCameraIdList().length).append(" (no prueba acceso a cámaras ADAS/360)\n");}catch(Exception e){r.append("Cámaras: acceso a inventario no disponible\n");}
        try{SensorManager sm=(SensorManager)getSystemService(SENSOR_SERVICE);r.append("Sensores Android declarados: ").append(sm==null?0:sm.getSensorList(-1).size()).append(" (no son lecturas del vehículo)\n");}catch(Exception ignored){}
        r.append("GPS permitido: ").append(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED).append('\n');
        r.append("Último error de panel: ").append(getSharedPreferences("c10_rebuild",MODE_PRIVATE).getString("last_panel_error","ninguno")).append('\n');
        r.append("Último cierre capturado: ").append(getSharedPreferences("c10_rebuild",MODE_PRIVATE).getString("last_crash","ninguno")).append('\n');
        r.append("Este informe solo existe después de instalar. No lee logs privados del instalador ni prueba conexión real con el C10. No contiene VIN, serie, Bluetooth MAC ni cuenta.");
        return r.toString();
    }
    private void diagnostics() {
        LinearLayout p=page();p.addView(text(report(),17));action(p,"Guardar informe TXT",()->{
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"C10-informe-1.0.txt");
            try{startActivityForResult(i,EXPORT);}catch(RuntimeException e){new AlertDialog.Builder(this).setMessage("Este sistema no ofrece un selector para guardar archivos. Puedes fotografiar el informe.").setPositiveButton("Cerrar",null).show();}
        });
    }
    private void settings() {
        LinearLayout p=page();p.addView(text("Archivos y conexiones opcionales",24));
        action(p,"Elegir carpeta de vídeos / USB",()->{
            try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),FOLDER);}catch(RuntimeException e){p.addView(text("Selector de carpetas no disponible en este sistema.",17));}
        });
        p.addView(text("Los siguientes son los ajustes originales de Qonecta+. Sus integraciones necesitan sus propios requisitos y no se han probado en tu coche. No introduzcas credenciales por chat.",17));
        action(p,"Cuenta Leapmotor · integración experimental",()->launch(CarCloudActivity.class));
        action(p,"Home Assistant · integración opcional",()->launch(HomeAssistantActivity.class));
        action(p,"Ajustes de esta aplicación en Android",()->{
            try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(RuntimeException e){p.addView(text("Ajustes del sistema no disponibles.",17));}
        });
    }
    private void integrations() {
        LinearLayout p=page();p.addView(text("Qué podemos medir y qué sigue pendiente",24));
        p.addView(text("GPS: velocidad y prestaciones aproximadas, si hay proveedor y señal. OBD: RPM, velocidad y temperatura si el coche y un adaptador Bluetooth clásico ELM327 responden a los PID estándar. La tensión ATRV es la del adaptador/12 V, no la batería de tracción.",18));
        p.addView(text("Regeneración y costes: cálculo manual. No existe aún un PID de batería, potencia o regeneración del C10 validado en este proyecto.",18));
        p.addView(text("Cámara del parabrisas, distancia delantera, centinela y cámaras 360: no hay una API/proveedor confirmado. No se activan con botones simulados. Encontrar una cámara Android no demuestra acceso al sistema del coche.",18));
        p.addView(text("Aparcamiento autónomo, mover el coche desde el móvil, reprogramar potencia o regeneración y autorregular el motor: no implementados. La app no manda órdenes de dirección, freno o aceleración.",18));
        action(p,"Ver capacidades reales declaradas",()->show("Diagnóstico"));
    }
    @Override protected void onActivityResult(int req,int result,Intent data) {
        super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
        try {
            if(req==EXPORT){try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new java.io.IOException();out.write(report().getBytes(StandardCharsets.UTF_8));}Toast.makeText(this,"Informe guardado",Toast.LENGTH_LONG).show();}
            if(req==FOLDER){getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION);new Config(this).setVideoTree(data.getData().toString());Toast.makeText(this,"Carpeta guardada. Abre Vídeos.",Toast.LENGTH_LONG).show();}
        }catch(Exception e){new AlertDialog.Builder(this).setMessage("No se pudo guardar: "+e.getClass().getSimpleName()).setPositiveButton("Cerrar",null).show();}
    }
    private final CarScreen.Host host=new CarScreen.Host(){
        public android.content.Context context(){return C10NativeActivity.this;}
        public void setContent(View v){body.removeAllViews();body.addView(v,new FrameLayout.LayoutParams(-1,-1));}
        public int width(){return body.getWidth()>0?body.getWidth():getResources().getDisplayMetrics().widthPixels;}
        public int height(){return body.getHeight()>0?body.getHeight():getResources().getDisplayMetrics().heightPixels-dp(112);}
        public void post(Runnable r){body.post(r);}public void setFullscreen(boolean on){}public void setImmersive(boolean on){}
    };
    @Override protected void onStop(){paused=true;clear();RadioPlayer.stop();super.onStop();}
    @Override protected void onStart(){super.onStart();if(paused){paused=false;show("Inicio");}}
    @Override protected void onDestroy(){clear();super.onDestroy();}
    @Override public void onBackPressed(){if(!section.equals("Inicio"))show("Inicio");else super.onBackPressed();}
}
