package com.headqlink.link;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * C10 native host for the actual Qonecta+ CarScreen implementations.
 * No VirtualDisplay, projection, LinkService, AA server or demo data.
 * Derived project: AGPL-3.0; original authors and licences remain in LICENSE/NOTICE.
 */
public final class C10NativeActivity extends Activity {
    private LinearLayout root;
    private FrameLayout body;
    private CarScreen current;
    private String section = "Inicio";
    private String pendingSection;
    private boolean stopped;

    private int dp(int px) {
        return Math.round(px * getResources().getDisplayMetrics().density);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        L.init(this);
        // This build is for the user's C10 REEV. Persist its profile once, without
        // credentials or a cloud request; do not silently use the 69.9 kWh EV.
        android.content.SharedPreferences nativePrefs = getSharedPreferences("c10_native",MODE_PRIVATE);
        if (!nativePrefs.getBoolean("reev_profile_initialized",false)) {
            new CarCloudStore(this).setProfile(CarCloudStore.PROFILE_C10_REEV,0);
            nativePrefs.edit().putBoolean("reev_profile_initialized",true).apply();
        }
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CarKit.BG);
        TextView title = text("C10 Nativa Plus · 0.9 ALTERNATIVA", 22);
        title.setPadding(dp(14),dp(10),dp(14),dp(6));
        root.addView(title);
        HorizontalScrollView bar = new HorizontalScrollView(this);
        LinearLayout menu = new LinearLayout(this);
        String[] sections = {"Inicio","Diagnóstico","Coche","Instrumentos","Eficiencia","Radio","Web","Fotos","Vídeos","TV","Herramientas GPS"};
        for (String name : sections) {
            Button b = new Button(this);
            b.setText(name);
            b.setAllCaps(false);
            b.setOnClickListener(v -> select(name));
            menu.addView(b,new LinearLayout.LayoutParams(-2,dp(52)));
        }
        bar.addView(menu);
        root.addView(bar);
        body = new FrameLayout(this);
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((v,insets) -> {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(safe.left,safe.top,safe.right,safe.bottom);
                return insets;
            });
            root.requestApplyInsets();
        }
        // Start with a truthful status page, not simulated telemetry or automatically playing media.
        show("Inicio");
    }

    private TextView text(String value,int size) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(CarKit.TEXT);
        t.setPadding(dp(12),dp(8),dp(12),dp(8));
        return t;
    }

    private void clear() {
        if (current != null) {
            try { current.destroy(); } catch (RuntimeException e) { L.e("C10 native panel cleanup",e); }
            current = null;
        }
        body.removeAllViews();
    }

    private void select(String name) {
        if ("Herramientas GPS".equals(name)) {
            startActivity(new Intent(this, com.leapmotorguia.c10nativa.MainActivity.class));
            return;
        }
        if (("Coche".equals(name) || "Instrumentos".equals(name) || "Eficiencia".equals(name))
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingSection = name;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION},91);
            return;
        }
        if ("Vídeos".equals(name) || "TV".equals(name) || "Web".equals(name) || "Fotos".equals(name)) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Contenido para uso estacionado")
                .setMessage("Esta alfa no recibe la posición P ni el estado de conducción del coche. Abre este contenido solo con el vehículo estacionado.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Estoy estacionado",(d,w) -> show(name)).show();
            return;
        }
        show(name);
    }

    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if (request == 91) {
            String name = pendingSection;
            pendingSection = null;
            // Panels must remain usable without permission and label missing GPS themselves.
            if (name != null) show(name);
        }
    }

    private void show(String name) {
        clear();
        section = name;
        try {
            switch (name) {
                case "Diagnóstico": diagnostics(); return;
                case "Coche": current = new CarHubScreen(); break;
                case "Instrumentos": current = new InstrumentsScreen(); break;
                case "Eficiencia": current = new EfficiencyScreen(); break;
                case "Radio": current = new RadioScreen(); break;
                case "Web": current = new WebScreen(); break;
                case "Fotos": current = new GalleryScreen(false); break;
                case "Vídeos": current = new GalleryScreen(true); break;
                case "TV": current = new IptvScreen(); break;
                default: status(); return;
            }
            CarStyle.setUiScale(1f,false);
            CarKit.narrow = host.width() < CarUi.NARROW_MIN_W;
            CarKit.pageH = host.height();
            try { body.addView(current.create(host),new FrameLayout.LayoutParams(-1,-1)); }
            finally { CarKit.narrow = false; }
        } catch (RuntimeException | LinkageError e) {
            L.e("C10 native panel "+name,e);
            clear();
            body.addView(text("No se ha podido abrir "+name+".\n"+
                e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage())+
                "\nEsta función necesita adaptación; no está verificada.",18));
        }
    }


    private void diagnostics() {
        StringBuilder report = new StringBuilder();
        report.append("DIAGNÓSTICO C10 0.9\n");
        report.append("Android: ").append(Build.VERSION.RELEASE).append(" · API ").append(Build.VERSION.SDK_INT).append("\n");
        report.append("Fabricante: ").append(Build.MANUFACTURER).append("\nModelo: ").append(Build.MODEL).append("\n");
        report.append("Arquitecturas: ").append(java.util.Arrays.toString(Build.SUPPORTED_ABIS)).append("\n");
        report.append("Paquete: ").append(getPackageName()).append("\n");
        report.append("Android mínimo / objetivo: ").append(getApplicationInfo().minSdkVersion).append(" / ").append(getApplicationInfo().targetSdkVersion).append("\n");
        report.append("Espacio disponible para la app: ").append(getFilesDir().getUsableSpace()/1024/1024).append(" MiB\n");
        PackageManager pm = getPackageManager();
        for (String feature : new String[]{"android.hardware.location","android.hardware.location.gps",
                "android.hardware.location.network","android.hardware.touchscreen","android.hardware.faketouch",
                "android.hardware.usb.host","android.hardware.type.automotive"}) {
            report.append(feature).append(": ").append(pm.hasSystemFeature(feature) ? "sí" : "no").append("\n");
        }
        report.append("Permiso GPS: ").append(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ? "concedido" : "sin conceder").append("\n");
        try {
            String installer = pm.getInstallSourceInfo(getPackageName()).getInstallingPackageName();
            report.append("Instalador de esta app: ").append(installer == null ? "no informado" : installer).append("\n");
        } catch (Exception e) {
            report.append("Instalador de esta app: no disponible\n");
        }
        report.append("\nEsta pantalla solo funciona después de instalar esta app. No puede leer el motivo de rechazo de otro APK ni los registros privados del instalador. No demuestra acceso a cámaras o datos del vehículo.\n");
        report.append("\nNo incluye VIN, número de serie ni datos de tu cuenta. Puedes fotografiar esta pantalla para revisar la compatibilidad.");
        ScrollView sc = new ScrollView(this);
        TextView result = text(report.toString(),18);
        result.setTextIsSelectable(true);
        sc.addView(result);
        body.addView(sc,new FrameLayout.LayoutParams(-1,-1));
    }

    private void status() {
        ScrollView sc = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        sc.addView(col);
        col.addView(text("Paneles originales de Qonecta+ en una actividad nativa",24));
        col.addView(text("Alfa 0.9 de compatibilidad para el coche. No es una versión definitiva ni garantiza la instalación en LeapOS.",18));
        col.addView(text("Perfil de estimación: C10 REEV. Los parámetros del modelo no son mediciones del coche.",18));
        col.addView(text("Android "+Build.VERSION.RELEASE+" · API "+Build.VERSION.SDK_INT+
            "\nModelo: "+Build.MODEL,18));
        col.addView(text("Coche, instrumentos, eficiencia, radio, web, fotos, vídeos y TV reutilizan sus implementaciones de Qonecta+. Algunas dependen de internet, archivos o servicios no disponibles en el coche.",18));
        col.addView(text("Sin cuenta configurada, los datos reales del vehículo no están disponibles. Potencia, consumo y regeneración calculados por el modelo son estimaciones, no lecturas OBD.",18));
        col.addView(text("No incluye receptor Android Auto/CarPlay nativo, acceso a cámaras, centinela, aparcamiento remoto, control del motor ni ajustes de potencia o frenada.",18));
        col.addView(text("Sin modo demostración. Sin arranque automático ni cambios en firmware. GPS y datos disponibles deben comprobarse en este vehículo.",18));
        col.addView(text("Proyecto derivado de Qonecta+ (CharlysEV), HeadQLink (ryazrm) y Open Headunit. AGPL-3.0; créditos y licencias conservados.",16));
        body.addView(sc,new FrameLayout.LayoutParams(-1,-1));
    }

    private final CarScreen.Host host = new CarScreen.Host() {
        public android.content.Context context() { return C10NativeActivity.this; }
        public void setContent(View v) { body.removeAllViews(); body.addView(v,new FrameLayout.LayoutParams(-1,-1)); }
        public int width() { return body.getWidth()>0 ? body.getWidth() : getResources().getDisplayMetrics().widthPixels; }
        public int height() { return body.getHeight()>0 ? body.getHeight() : Math.max(240,getResources().getDisplayMetrics().heightPixels-dp(112)); }
        public void post(Runnable r) { body.post(r); }
        // Keep native navigation and the vehicle's system UI accessible.
        public void setFullscreen(boolean on) { }
        public void setImmersive(boolean on) { }
    };

    @Override protected void onStop() {
        stopped = true;
        clear();
        RadioPlayer.stop();
        super.onStop();
    }

    @Override protected void onStart() {
        super.onStart();
        if (stopped) { stopped=false; show("Inicio"); }
    }

    @Override protected void onDestroy() {
        clear();
        super.onDestroy();
    }
}
