package es.leapmotorguia.c10plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.ScrollView;
import android.app.AlertDialog;
import java.util.List;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.GeolocationPermissions;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * NATIVE user APK bootstrap. Calls the ORIGINAL UNIT ExtendedHubView.Dialog factory
 * already contained in the user's stable APK, then appends C10 Plus tabs to its
 * ORIGINAL tab row. It does not import or replace the CharlysEV launcher.
 */
public final class C10NativeHubActivity extends Activity {
    private Dialog nativeDialog;
    private C10Bridge bridge;
    private TextView status;
    private boolean opening;
    private boolean initSetup;
    private boolean hadVisibleUnitOnPause;
    private static final String SETUP_PREFS = "c10plus_native_onboarding";
    private static final int REQUEST_APK = 7311;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(24,24,24,24);
        root.setBackgroundColor(0xff07141e);
        TextView heading = new TextView(this);
        heading.setText("UNIT C10 Plus 4.5 · Integración y actualizaciones");
        heading.setTextColor(0xff04e2bd);
        heading.setTextSize(25);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(12,20,12,20);
        status.setText("Versión 4.5 · Iniciando la integración con UNIT...");
        root.addView(status);
        Button retry = new Button(this);
        retry.setText("Recuperar nuestras pantallas / Abrir UNIT");
        retry.setOnClickListener(v -> restoreNative());
        root.addView(retry);
        Button original = new Button(this);
        original.setText("UNIT original (rescate)");
        original.setOnClickListener(v -> {
            try {
                Intent i = new Intent();
                i.setClassName(getPackageName(), "com.andrerinas.openheadunit.main.MainActivity");
                startActivity(i);
            } catch (Throwable e) {
                status.setText("No se pudo abrir UNIT original: "+e.getClass().getSimpleName());
            }
        });
        root.addView(original);
        Button diagnostic=new Button(this);
        diagnostic.setText("Diagnóstico de integración 4.5");
        diagnostic.setOnClickListener(v->showStartupDiagnostic());
        root.addView(diagnostic);
        setContentView(root);
        // Upgrades from 3.x also see the onboarding once; users may reopen it any time.
        if (!getSharedPreferences(SETUP_PREFS,MODE_PRIVATE).getBoolean("finished",false)) {
            handler.postDelayed(() -> showSetup(0), 200);
        } else {
            handler.postDelayed(this::openNative, 200);
        }
    }

    /**
     * Onboarding uses the application's existing UNIT vehicle profile store.
     * Permissions are requested one group at a time, only after a user tap.
     * It deliberately never requests privileged ECU, CAMERA or SMS permissions.
     */
    private LinearLayout wizardColumn() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(28,20,28,20);
        l.setBackgroundColor(0xff0c2130);
        return l;
    }
    private TextView wizardText(String text, int size, int color) {
        TextView v=new TextView(this);
        v.setText(text);v.setTextColor(color);v.setTextSize(size);
        v.setPadding(8,12,8,12);
        return v;
    }
    private void showSetup(int step) {
        if (isFinishing()) return;
        if (step > 3) {
            getSharedPreferences(SETUP_PREFS, MODE_PRIVATE).edit()
                .putBoolean("finished",true).apply();
            openNative();
            return;
        }
        LinearLayout content=wizardColumn();
        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(false);
        sc.addView(content);
        final int screen=step;
        String title;
        if(screen==0) {
            title="1/4 · Bienvenido a UNIT C10 Plus";
            content.addView(wizardText("Tu aplicación original UNIT, con nuestras herramientas integradas.\n\n"
                +"Selecciona la variante exacta del vehículo. Los datos técnicos de la ficha no son mediciones reales.\n\n"
                +"Los permisos de Android se pedirán solo para las funciones que los necesiten. "
                +"Puedes repetir esta configuración desde la pestaña «Modelo».",17,0xfff3faff));
        } else if (screen==1) {
            title="2/4 · Modelo y versión del coche";
            addVehicleSelector(content,true);
        } else if (screen==2) {
            title="3/4 · Permisos para conectar y medir";
            content.addView(wizardText("Sin estos permisos, algunas funciones de GPS, Bluetooth y hotspot pueden no funcionar. "
                +"Solo se solicitarán permisos normales disponibles para esta aplicación.\n\n"
                +"GPS = posición del móvil/sistema, no acceso confirmado a las cámaras o centralitas del C10.",16,0xffe2f0f8));
            setupPermissionButton(content,"Ubicación precisa / GPS",
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION});
            if(Build.VERSION.SDK_INT>=31) setupPermissionButton(content,"Bluetooth / dispositivos cercanos",
                new String[]{"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"});
            if(Build.VERSION.SDK_INT>=33){
                setupPermissionButton(content,"Wi-Fi cercano",new String[]{"android.permission.NEARBY_WIFI_DEVICES"});
                setupPermissionButton(content,"Notificaciones",new String[]{"android.permission.POST_NOTIFICATIONS"});
            }
        } else {
            title="4/4 · Comprobación";
            String[] names={"android.permission.ACCESS_FINE_LOCATION",
                "android.permission.BLUETOOTH_CONNECT","android.permission.NEARBY_WIFI_DEVICES",
                "android.permission.POST_NOTIFICATIONS"};
            StringBuilder lines=new StringBuilder("Estado de permisos:\n");
            for(String name:names) {
                if (name.equals("android.permission.BLUETOOTH_CONNECT")&&Build.VERSION.SDK_INT<31)continue;
                if ((name.equals("android.permission.NEARBY_WIFI_DEVICES")||
                     name.equals("android.permission.POST_NOTIFICATIONS"))&&Build.VERSION.SDK_INT<33)continue;
                boolean granted=checkSelfPermission(name)==PackageManager.PERMISSION_GRANTED;
                lines.append(name.substring(name.lastIndexOf('.')+1)).append(": ")
                    .append(granted?"Concedido":"Pendiente").append("\n");
            }
            content.addView(wizardText(lines.toString(),16,0xffd0e5ed));
            content.addView(wizardText("El perfil se guarda en la configuración original de UNIT. "
                +"Esto no instala servicios de coche, no activa accesos ECU ni modifica LeapOS.",15,0xffa7c1d1));
        }
        final AlertDialog[] current={null};
        AlertDialog dlg=new AlertDialog.Builder(this)
            .setTitle(title).setView(sc)
            .setPositiveButton(step==3?"Terminar":"Siguiente",(d,w)->showSetup(screen+1))
            .setNegativeButton(step==0?"Omitir":"Atrás",(d,w)->{
                if(screen==0)showSetup(4);else showSetup(screen-1);
            })
            .create();
        dlg.setCanceledOnTouchOutside(false);
        dlg.show();
    }
    private void setupPermissionButton(LinearLayout col, String title,String[] candidates) {
        Button b=new Button(this);
        b.setText(title+" · Solicitar");
        col.addView(b);
        b.setOnClickListener(v -> {
            java.util.ArrayList<String> needed=new java.util.ArrayList<>();
            for(String perm:candidates){
                try{
                    if(getPackageManager().getPermissionInfo(perm,0)==null)continue;
                    if(checkSelfPermission(perm)!=PackageManager.PERMISSION_GRANTED)needed.add(perm);
                } catch(Exception ignored){}
            }
            if(!needed.isEmpty()) {
                // Android silently ignores permissions not declared by the installed APK;
                // the user can still complete setup without them.
                requestPermissions(needed.toArray(new String[0]), 6811);
            } else Toast.makeText(this,"No hay permisos pendientes para este apartado",Toast.LENGTH_SHORT).show();
        });
    }

    /** The 0.8 build has no VehicleProfiles class. Keep its original internals
     * unchanged and store an explicit LOCAL display profile in our own settings.
     */
    private void addVehicleSelector(LinearLayout col, boolean wizard) {
        try {
            Class<?> profiles=Class.forName("es.c10.unitplus.integrated.VehicleProfiles");
            String[] names=(String[])profiles.getMethod("labels").invoke(null);
            int selected=(Integer)profiles.getMethod("index",Context.class).invoke(null,this);
            if(names==null||names.length==0)throw new IllegalStateException("sin perfiles");
            col.addView(wizardText("Selector nativo de perfiles de UNIT:",17,0xffb9d9e7));
            Spinner choose=new Spinner(this);
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);
            choose.setAdapter(adapter);
            if(selected>=0&&selected<names.length)choose.setSelection(selected);
            col.addView(choose);
            TextView result=wizardText("Modelo actual: "+names[Math.max(0,Math.min(selected,names.length-1))],17,0xff00dfbe);
            col.addView(result);
            Button save=new Button(this);
            save.setText("Guardar modelo en UNIT");
            col.addView(save);
            save.setOnClickListener(v->{
                try{
                    int index=choose.getSelectedItemPosition();
                    profiles.getMethod("select",Context.class,int.class).invoke(null,this,index);
                    result.setText("Guardado en UNIT: "+names[index]);
                }catch(Throwable error){result.setText("No se pudo guardar: "+error.getClass().getSimpleName());}
            });
        }catch(Throwable absent){
            col.addView(wizardText(
                "UNIT 0.8 no incluye el selector moderno de variantes. "+
                "Puedes guardar un perfil local sin alterar ni reprogramar el vehículo.",16,0xffb9d9e7));
            final String[] models={
                "C10 REEV","C10 BEV","C10 BEV AWD","B10 BEV","B10 REEV",
                "T03 BEV","B05 BEV","B03X BEV","B03 BEV","Otro / desconocido"};
            SharedPreferences prefs=getSharedPreferences("c10plus_native",MODE_PRIVATE);
            int selected=prefs.getInt("legacy_model_index",0);
            selected=Math.max(0,Math.min(models.length-1,selected));
            Spinner choose=new Spinner(this);
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,models);
            choose.setAdapter(adapter);
            choose.setSelection(selected);
            col.addView(choose);
            TextView result=wizardText("Perfil local: "+models[selected],16,0xff06dab8);
            col.addView(result);
            Button save=new Button(this);
            save.setText("Guardar perfil local");
            col.addView(save);
            save.setOnClickListener(v->{
                int index=choose.getSelectedItemPosition();
                prefs.edit().putInt("legacy_model_index",index)
                    .putString("legacy_model_label",models[index]).apply();
                result.setText("Guardado solo en C10 Plus: "+models[index]);
                Toast.makeText(this,"Perfil de visualización guardado",Toast.LENGTH_SHORT).show();
            });
        }
    }

    /** Opens the Android document chooser from inside UNIT, without external web links. */
    void chooseUpdateApk() {
        try {
            Intent select = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            select.addCategory(Intent.CATEGORY_OPENABLE);
            select.setType("*/*");
            select.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(select, REQUEST_APK);
        } catch (Exception e) {
            Toast.makeText(this, "Selector de actualizaciones no disponible", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != REQUEST_APK || result != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        // Never assume an arbitrary document is an APK: Android's package installer
        // verifies the package identity and signing certificate before allowing update.
        boolean opened=false;
        String reason="No hay instalador Android disponible";
        for(String action:new String[]{Intent.ACTION_VIEW,Intent.ACTION_INSTALL_PACKAGE}) {
            try {
                Intent install=new Intent(action);
                install.setDataAndType(uri,"application/vnd.android.package-archive");
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(install);
                opened=true;
                Toast.makeText(this,"Confirma la instalación si LeapOS la autoriza",Toast.LENGTH_LONG).show();
                break;
            } catch (Exception e) {
                reason=e.getClass().getSimpleName()+" en "+action;
            }
        }
        if(!opened) Toast.makeText(this,"No se puede abrir el instalador: "+reason,
            Toast.LENGTH_LONG).show();
    }

    /** Manual repair when the projection or system has replaced our UNIT dialog.
     * Does not force itself above Android Auto or CarPlay; runs only after
     * the user has returned to this Activity and explicitly tapped restore.
     */
    private void restoreNative() {
        try { if (bridge != null) bridge.dispose(); } catch(Throwable ignored){}
        bridge = null;
        try { if (nativeDialog != null) nativeDialog.dismiss(); } catch(Throwable ignored){}
        nativeDialog = null;
        getSharedPreferences("c10plus_lifecycle",MODE_PRIVATE).edit()
            .putLong("last_restore",System.currentTimeMillis()).apply();
        status.setText("Restableciendo las pestañas originales y C10 Plus...");
        handler.postDelayed(this::openNative,160);
    }

    private void openNative() {
        if (opening || (nativeDialog != null && nativeDialog.isShowing())) return;
        opening = true;
        try {
            Class<?> cls = Class.forName("com.headqlink.unit.extended.ExtendedHubView");
            Field field = cls.getDeclaredField("Companion");
            field.setAccessible(true);
            Object companion = field.get(null);
            Class<?> compClass = companion.getClass();
            Method show = null;
            for (Method m : compClass.getDeclaredMethods()) {
                if (m.getName().equals("showDialog$default") && m.getParameterTypes().length == 6) {
                    show = m; break;
                }
            }
            Dialog dialog;
            if (show != null) {
                show.setAccessible(true);
                dialog = (Dialog)show.invoke(null, companion, this, null, false, 6, null);
            } else {
                Method direct = null;
                for (Method m : compClass.getDeclaredMethods()) {
                    if (m.getName().equals("showDialog") && m.getParameterTypes().length == 3) {
                        direct = m; break;
                    }
                }
                if (direct == null) throw new NoSuchMethodException("UNIT showDialog");
                direct.setAccessible(true);
                Object[] enumValues = direct.getParameterTypes()[1].getEnumConstants();
                dialog = (Dialog)direct.invoke(companion, this, enumValues != null && enumValues.length > 0 ? enumValues[0] : null, false);
            }
            if (dialog == null) throw new IllegalStateException("UNIT no devolvió su diálogo");
            nativeDialog = dialog;
            if (!dialog.isShowing()) dialog.show();
            handler.postDelayed(() -> attachNative(dialog, 0), 200);
            status.setText("UNIT original abierto");
        } catch (Throwable e) {
            Throwable why = e.getCause() == null ? e : e.getCause();
            status.setText("No se pudo abrir el menú original: "+why.getClass().getSimpleName() +
                (why.getMessage() == null ? "" : " · " + why.getMessage()));
            getSharedPreferences("c10plus_beta_errors", MODE_PRIVATE).edit()
                .putString("native_open", String.valueOf(why)).apply();
        } finally {
            opening = false;
        }
    }

    /** Keep a simple, privacy-safe lifecycle trace to identify Android Auto
     * switching away from our own Activity. Does not claim which app took focus.
     */
    @Override protected void onPause() {
        hadVisibleUnitOnPause = nativeDialog!=null && nativeDialog.isShowing();
        getSharedPreferences("c10plus_lifecycle",MODE_PRIVATE).edit()
            .putLong("last_pause",System.currentTimeMillis())
            .putBoolean("unit_dialog_showing", hadVisibleUnitOnPause).apply();
        if(bridge!=null)bridge.pauseCatalog();
        super.onPause();
    }
    @Override protected void onStop() {
        getSharedPreferences("c10plus_lifecycle",MODE_PRIVATE).edit()
            .putLong("last_stop",System.currentTimeMillis()).apply();
        super.onStop();
    }
    @Override protected void onResume() {
        super.onResume();
        getSharedPreferences("c10plus_lifecycle",MODE_PRIVATE).edit()
            .putLong("last_resume",System.currentTimeMillis()).apply();
        // Android may destroy only the floating UNIT dialog when projection
        // takes over. Restore it after this Activity returns to the foreground.
        // Never request foreground or draw over the projection itself.
        if (hadVisibleUnitOnPause && (nativeDialog==null || !nativeDialog.isShowing())) {
            hadVisibleUnitOnPause=false;
            handler.postDelayed(()->{
                if(!isFinishing() && !opening && (nativeDialog==null || !nativeDialog.isShowing()))
                    restoreNative();
            },300);
        }
    }

    private void showStartupDiagnostic() {
        String error=getSharedPreferences("c10plus_beta_errors",MODE_PRIVATE)
            .getString("bridge","");
        String report=StartupDiagnostics.report(this,error);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);
        TextView text=new TextView(this);
        text.setText(report);text.setTextSize(15);text.setTextColor(Color.WHITE);
        text.setPadding(24,16,24,16);
        scroll.addView(text);
        new AlertDialog.Builder(this).setTitle("C10 Plus 4.5 · Comprobación")
            .setView(scroll)
            .setPositiveButton("Copiar informe",(dialog,which)->{
                android.content.ClipboardManager cb=(android.content.ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
                if(cb!=null) cb.setPrimaryClip(android.content.ClipData.newPlainText(
                    "Diagnóstico de inicio 4.5",report));
                Toast.makeText(this,"Informe copiado",Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cerrar",null).show();
    }

    private void attachNative(Dialog d, int attempt) {
        if (isFinishing() || !d.isShowing() || d.getWindow() == null) return;
        View view = findHub(d.getWindow().getDecorView());
        if (view instanceof ViewGroup) {
            try {
                bridge = C10Bridge.attach(this, (ViewGroup)view);
                status.setText("4.5 · Mejoras integradas en UNIT");
                Toast.makeText(this,"UNIT C10 Plus 4.5 conectado",Toast.LENGTH_LONG).show();
            } catch (Throwable e) {
                status.setText("UNIT funciona, pero el acceso C10 Plus ha fallado: "+e.getClass().getSimpleName());
                getSharedPreferences("c10plus_beta_errors",MODE_PRIVATE).edit()
                    .putString("bridge",String.valueOf(e)).apply();
                new AlertDialog.Builder(this)
                    .setTitle("C10 Plus 4.5 no pudo enlazar con UNIT")
                    .setMessage("La aplicación original sigue disponible. Pulsa «Diagnóstico» para obtener el motivo y corregirlo sin desinstalar.")
                    .setPositiveButton("Diagnóstico",(dialog,which)->showStartupDiagnostic())
                    .setNegativeButton("Continuar con UNIT",null).show();
            }
        } else if (attempt < 12) {
            handler.postDelayed(() -> attachNative(d, attempt+1), 300);
        } else {
            status.setText("No se localizó el menú de UNIT para integrar nuestras funciones.");
            getSharedPreferences("c10plus_beta_errors",MODE_PRIVATE).edit()
                .putString("bridge","Vista original ExtendedHubView no encontrada tras 12 intentos").apply();
            new AlertDialog.Builder(this).setTitle("Integración pendiente")
                .setMessage("UNIT se ha abierto, pero nuestra barra no ha podido conectarse. Puedes copiar un diagnóstico.")
                .setPositiveButton("Diagnóstico",(dialog,which)->showStartupDiagnostic())
                .setNegativeButton("Cerrar",null).show();
        }
    }

    private View findHub(View node) {
        if (node == null) return null;
        if ("com.headqlink.unit.extended.ExtendedHubView".equals(node.getClass().getName())) return node;
        if (node instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup)node;
            for (int i=0;i<vg.getChildCount();i++) {
                View found = findHub(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (bridge != null) bridge.dispose();
        if (nativeDialog != null && nativeDialog.isShowing()) nativeDialog.dismiss();
        super.onDestroy();
    }

    /** Attach only to UNIT's own visible tab row and body, not a second activity. */
    private static final class C10Bridge {
        private static final String[] TABS = {"Maps", "Consumos", "Gráficas",
            "Accesos C10", "Actualizaciones", "Modelo", "Cockpit", "Radio+", "Prestaciones", "TDT+", "Favoritos"};
        // Media providers live ONLY in UNIT's original RADIO and TV screens.
        private static final String[] RADIO_NAMES = {"Spotify"};
        private static final String[] RADIO_URLS = {"https://open.spotify.com"};
        private static final String[] TV_NAMES = {"Netflix","Prime Video","Max (HBO)","Pluto TV","YouTube"};
        private static final String[] TV_URLS = {"https://www.netflix.com",
            "https://www.primevideo.com","https://www.max.com","https://pluto.tv",
            "https://www.youtube.com"};
        private final Activity activity;
        private final FrameLayout body;
        private final LinearLayout nativeRow;
        private final Handler updateHandler = new Handler(Looper.getMainLooper());
        private final SharedPreferences prefs;
        private final ArrayList<TextView> extraButtons = new ArrayList<>();
        private final ArrayList<Double> realSoc = new ArrayList<>();
        private final ArrayList<SpeedGraph.Point> cockpitHistory=new ArrayList<>();
        private RadioAudio cockpitRadio;
        private Performance cockpitPerformance;
        private String cockpitResult="Sin prueba activa";
        private final Method nativeSelect;
        private final Field nativeActiveTab;
        private String observedNativeRole="";
        private View nativeBodyContent;
        private WebView savedTvWebView;
        private ViewGroup savedTvParent;
        private ViewGroup.LayoutParams savedTvParams;
        private FrameLayout tvPlayerWrapper;
        private FrameLayout nativeTvPlayerSlot;
        private android.widget.VideoView pausedTvVideo;
        private boolean tvIsEmbedded;
        private int savedTvIndex=-1;
        private final Runnable nativeMediaTabMonitor=new Runnable() {
            @Override public void run() {
                if(closed)return;
                try{
                    Object active=nativeActiveTab.get(hub);
                    String tab=active==null?"":String.valueOf(active).toUpperCase(Locale.ROOT);
                    String role=tab.equals("TV")?"tv":tab.equals("RADIO")?"radio":"";
                    // UNIT's left-side rail and top tabs both call selectTab().
                    // Watch its real activeTab, not touch events on one nav bar.
                    if(!showing && !role.isEmpty() && body.getChildCount()>0) {
                        View current=firstNativeBodyChild();
                        boolean changed=!role.equals(observedNativeRole) ||
                            current!=nativeBodyContent ||
                            (nativeMediaBar==null || nativeMediaBar.getParent()!=body);
                        if(changed) {
                            if(!role.equals(observedNativeRole))closeNativeMediaWeb();
                            if(nativeMediaOverlay!=null && nativeMediaOverlay.getParent()==null)
                                closeNativeMediaWeb();
                            observedNativeRole=role;
                            nativeBodyContent=current;
                            showNativeMediaBar(role);
                        }
                    } else if(!role.isEmpty()) {
                        observedNativeRole=role;
                    } else if(!observedNativeRole.isEmpty()) {
                        observedNativeRole="";
                        closeNativeMediaWeb();
                        clearNativeMediaBar();
                    }
                }catch(Throwable ignored){
                    // Never break UNIT navigation if a future build changes its state.
                }
                updateHandler.postDelayed(this,500);
            }
        };
        private View firstNativeBodyChild(){
            for(int i=0;i<body.getChildCount();i++){
                View v=body.getChildAt(i);
                if(v!=nativeMediaBar && v!=nativeMediaOverlay)return v;
            }
            return null;
        }
        private final Object nativeHandler, nativeTick;
        private final ViewGroup hub;
        private LinearLayout custom;
        private FrameLayout leftSlot, rightSlot;
        private Spinner leftChoice, rightChoice;
        private Pane leftPane, rightPane;
        private boolean split, showing, closed;
        private View nativeMediaBar, nativeMediaOverlay;
        private WebView nativeMediaBrowser;
        private android.app.Dialog nativeVideoFullscreen;
        private int nativeSelectionGeneration;
        private final Runnable nativeMediaSafetyTick = new Runnable() {
            @Override public void run() {
                if(nativeMediaOverlay == null || nativeMediaBrowser == null || closed) return;
                readReal();
                if(Double.isFinite(speed) && Math.abs(speed) > 1.5d) {
                    closeNativeMediaWeb();
                    Toast.makeText(activity,"Vídeo detenido: vehículo en movimiento",Toast.LENGTH_LONG).show();
                } else updateHandler.postDelayed(this,2000);
            }
        };
        private long lastSample;
        private double latestSoc = Double.NaN;
        private String latestSource = "Sin telemetría confirmada";
        private double consumed = Double.NaN, recovered = Double.NaN, gasoline = Double.NaN;
        private double speed = Double.NaN, powerKw = Double.NaN, tripKm = Double.NaN;
        private double measuredSeconds = Double.NaN;
        private boolean balancePartial, hasTripTotals;
        private String balanceSource = "Sin potencia real integrada";
        private EnergyBalance.Result energyBalance = EnergyBalance.calculate(Double.NaN,Double.NaN,false);
        private final Runnable refresh = new Runnable() {
            @Override public void run() {
                if (closed || !showing) return;
                stopNativeTick();
                readReal();
                if (leftPane != null) leftPane.refresh();
                if (rightPane != null) rightPane.refresh();
                updateHandler.postDelayed(this, 1200);
            }
        };
        static C10Bridge attach(Activity a,ViewGroup hub) throws Exception {
            return new C10Bridge(a,hub);
        }
        private C10Bridge(Activity a,ViewGroup h) throws Exception {
            activity=a;hub=h; prefs=a.getSharedPreferences("c10plus_native", MODE_PRIVATE);
            Field b=h.getClass().getDeclaredField("bodyContainer");b.setAccessible(true);
            body=(FrameLayout)b.get(h);
            Field buttons=h.getClass().getDeclaredField("tabButtons");buttons.setAccessible(true);
            Map<?,?> tabs=(Map<?,?>)buttons.get(h);
            if(tabs == null || tabs.isEmpty()) throw new IllegalStateException("UNIT sin pestañas");
            TextView sample=(TextView)tabs.values().iterator().next();
            nativeRow=(LinearLayout)sample.getParent();
            Class<?> enumType=Class.forName("com.headqlink.unit.extended.ExtendedHubView$Tab");
            nativeSelect=h.getClass().getMethod("selectTab", enumType);
            nativeActiveTab=h.getClass().getDeclaredField("activeTab");
            nativeActiveTab.setAccessible(true);
            Object hd=null, tk=null;
            try {Field f=h.getClass().getDeclaredField("uiHandler");f.setAccessible(true);hd=f.get(h);}catch(Throwable ignored){}
            try {Field f=h.getClass().getDeclaredField("tickRunnable");f.setAccessible(true);tk=f.get(h);}catch(Throwable ignored){}
            nativeHandler=hd;nativeTick=tk;
            // Preserve original handlers; only use a non-consuming touch listener to
            // clear a C10 Plus page before UNIT changes to its native tab.
            for(Map.Entry<?,?> e:tabs.entrySet()) {
                TextView tv=(TextView)e.getValue();
                String key=String.valueOf(e.getKey()).toLowerCase(Locale.ROOT);
                String title=tv.getText().toString().toLowerCase(Locale.ROOT);
                if(key.contains("game")||key.contains("karaoke")||title.contains("juego")||title.contains("karaoke")) {
                    tv.setVisibility(View.GONE);
                    continue;
                }
                final String role=(key.contains("radio")||title.equals("radio"))?"radio":
                    (key.contains("tv")||title.equals("tv")||title.contains("televisi"))?"tv":"";
                tv.setOnTouchListener((v,event)->{
                    if(event.getActionMasked()==MotionEvent.ACTION_DOWN) {
                        ++nativeSelectionGeneration;
                        stopCustom();
                        closeNativeMediaWeb();
                        clearNativeMediaBar();
                        if(!role.isEmpty()) {
                            final int marker=nativeSelectionGeneration;
                            // UNIT's original OnClick owns selection. We append AFTER it renders.
                            updateHandler.postDelayed(()->{
                                if(!closed && !showing && marker==nativeSelectionGeneration)
                                    showNativeMediaBar(role);
                            },200);
                        }
                    }
                    return false;
                });
            }
            for(int i=0;i<TABS.length;i++) {
                final int tab=i;
                TextView v=new TextView(a);
                v.setText(TABS[i]);v.setTextColor(Color.WHITE);
                v.setGravity(Gravity.CENTER);
                v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, sample.getTextSize());
                v.setPadding(sample.getPaddingLeft(),sample.getPaddingTop(),sample.getPaddingRight(),sample.getPaddingBottom());
                GradientDrawable bg=new GradientDrawable();
                bg.setColor(0xff123b45);bg.setCornerRadius(16);bg.setStroke(1,0xff1ca992);
                v.setBackground(bg);
                ViewGroup.LayoutParams source=sample.getLayoutParams();
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,source.height);
                lp.leftMargin=7;
                nativeRow.addView(v,lp);
                extraButtons.add(v);
                v.setOnClickListener(w->selectExtra(tab));
            }
            updateHandler.postDelayed(nativeMediaTabMonitor,450);
        }
        /**
         * Inside UNIT's own RADIO / TV body: append only a compact service strip.
         * Original radio stations and IPTV lists remain mounted underneath.
         */
        private void showNativeMediaBar(String section) {
            clearNativeMediaBar();
            if(showing || closed || body.getChildCount()==0)return;
            final boolean radio="radio".equals(section);
            final String[] names=radio?RADIO_NAMES:TV_NAMES;
            final String[] urls=radio?RADIO_URLS:TV_URLS;
            LinearLayout strip=new LinearLayout(activity);
            strip.setOrientation(LinearLayout.HORIZONTAL);
            strip.setGravity(Gravity.CENTER_VERTICAL);
            strip.setPadding(dip(9),dip(4),dip(9),dip(4));
            strip.setBackgroundColor(0xff0b2838);
            TextView lbl=label(radio?"RADIO · Spotify":"TV · Streaming",14,0xff00e1bd);
            lbl.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            strip.addView(lbl);
            android.widget.HorizontalScrollView scroll=new android.widget.HorizontalScrollView(activity);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout buttons=new LinearLayout(activity);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            for(int i=0;i<names.length;i++) {
                final int n=i;
                Button b=button(names[i]);
                b.setTextSize(13f);
                b.setPadding(dip(7),0,dip(7),0);
                b.setMinimumHeight(0);
                b.setMinHeight(0);
                b.setOnClickListener(v->askToOpenNativeMedia(section,names[n],urls[n],!radio));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dip(40));
                lp.leftMargin=dip(4);
                buttons.addView(b,lp);
            }
            scroll.addView(buttons);
            strip.addView(scroll,new LinearLayout.LayoutParams(0,dip(44),1f));
            nativeMediaBar=strip;
            FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(-1,dip(52),Gravity.BOTTOM);
            body.addView(strip,params);
        }

        private void clearNativeMediaBar(){
            if(nativeMediaBar!=null) {
                if(nativeMediaBar.getParent() instanceof ViewGroup)
                    ((ViewGroup)nativeMediaBar.getParent()).removeView(nativeMediaBar);
                nativeMediaBar=null;
            }
        }

        private void askToOpenNativeMedia(String section,String title,String url,boolean video) {
            readReal();
            if(video && Double.isFinite(speed) && Math.abs(speed)>1.5d) {
                Toast.makeText(activity,"No se puede mostrar vídeo en movimiento",Toast.LENGTH_LONG).show();
                return;
            }
            // Streaming sites are embedded; user confirmation is required for video.
            if(video) {
                new AlertDialog.Builder(activity)
                    .setTitle(title+" en TV")
                    .setMessage("Abre el servicio web dentro de UNIT. Confirma que el coche está estacionado. El proveedor puede bloquear vídeo o acceso por DRM.")
                    .setPositiveButton("Estoy estacionado",(d,w)->openNativeMedia(section,title,url,true))
                    .setNegativeButton("Cancelar",null).show();
            } else openNativeMedia(section,title,url,false);
        }

        private void openNativeMedia(String section,String title,String url,boolean video) {
            closeNativeMediaWeb();
            if(showing || closed)return;
            // The TV service must occupy the EXISTING player rectangle (RTVE etc.),
            // not cover the channel grid and the rest of UNIT.
            FrameLayout tvSlot=null;
            if(video) {
                tvSlot=findNativeTvPlayerSlot(body);
                if(tvSlot==null) {
                    Toast.makeText(activity,
                        "Reproductor de TV no detectado. No se cambiará la pantalla original.",
                        Toast.LENGTH_LONG).show();
                    return;
                }
                nativeTvPlayerSlot=tvSlot;
                pauseOriginalTvPlayer(tvSlot);
            }
            LinearLayout content=new LinearLayout(activity);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setBackgroundColor(0xff071b2a);
            LinearLayout header=row();
            TextView heading=label(("radio".equals(section)?"RADIO":"TV")+"  /  "+title,
                19,0xff1de1bd);
            heading.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            header.addView(heading,new LinearLayout.LayoutParams(0,dip(48),1f));
            Button back=button("Volver a "+("radio".equals(section)?"Radio":"TV"));
            back.setOnClickListener(v->closeNativeMediaWeb());
            header.addView(back,new LinearLayout.LayoutParams(-2,dip(48)));
            content.addView(header);
            TextView warning=label("Dentro de UNIT · servicio web · disponibilidad según proveedor",13,0xffa7c2d0);
            content.addView(warning);
            WebView browser=new WebView(activity);
            WebSettings settings=browser.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
            settings.setSupportMultipleWindows(false);
            settings.setMediaPlaybackRequiresUserGesture(true);
            if(Build.VERSION.SDK_INT>=21)
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            browser.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req) {
                    String scheme=req.getUrl().getScheme();
                    if("https".equalsIgnoreCase(scheme) || "about".equalsIgnoreCase(scheme))return false;
                    warning.setText("Enlace externo bloqueado; sigues dentro de UNIT.");
                    return true;
                }
                @Override public boolean shouldOverrideUrlLoading(WebView v,String link) {
                    return !(link.startsWith("https://")||link.startsWith("about:"));
                }
                @Override public void onReceivedError(WebView v,WebResourceRequest req,android.webkit.WebResourceError err) {
                    if(req.isForMainFrame())
                        warning.setText("No se pudo cargar el sitio dentro de UNIT. Puede rechazar WebView.");
                }
            });
            browser.setWebChromeClient(new WebChromeClient() {
                private CustomViewCallback activeCb;
                @Override public void onShowCustomView(View view,CustomViewCallback cb){
                    if(video){
                        // Provider fullscreen must not escape the small TV rectangle.
                        cb.onCustomViewHidden();
                        return;
                    }
                    if(nativeVideoFullscreen!=null){cb.onCustomViewHidden();return;}
                    activeCb=cb;
                    android.app.Dialog dlg=new android.app.Dialog(activity,
                        android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                    dlg.setContentView(view);
                    dlg.setOnDismissListener(d->{
                        if(activeCb!=null)activeCb.onCustomViewHidden();
                        activeCb=null;
                        nativeVideoFullscreen=null;
                    });
                    nativeVideoFullscreen=dlg;dlg.show();
                }
                @Override public void onHideCustomView() {
                    if(nativeVideoFullscreen!=null)nativeVideoFullscreen.dismiss();
                }
            });
            content.addView(browser,new LinearLayout.LayoutParams(-1,0,1f));
            nativeMediaBrowser=browser;
            nativeMediaOverlay=content;
            if(video && tvSlot!=null) {
                tvIsEmbedded=true;
                tvSlot.addView(content,new FrameLayout.LayoutParams(-1,-1));
            } else {
                tvIsEmbedded=false;
                body.addView(content,new FrameLayout.LayoutParams(-1,-1));
            }
            browser.loadUrl(url);
            updateHandler.removeCallbacks(nativeMediaSafetyTick);
            if(video)updateHandler.postDelayed(nativeMediaSafetyTick,1700);
        }

        private void closeNativeMediaWeb() {
            updateHandler.removeCallbacks(nativeMediaSafetyTick);
            if(nativeVideoFullscreen!=null) {
                try{nativeVideoFullscreen.dismiss();}catch(RuntimeException ignored){}
                nativeVideoFullscreen=null;
            }
            if(nativeMediaBrowser!=null) {
                try {
                    nativeMediaBrowser.stopLoading();
                    nativeMediaBrowser.loadUrl("about:blank");
                    nativeMediaBrowser.onPause();
                    if(nativeMediaBrowser.getParent() instanceof ViewGroup)
                        ((ViewGroup)nativeMediaBrowser.getParent()).removeView(nativeMediaBrowser);
                    nativeMediaBrowser.destroy();
                }catch(RuntimeException ignored){}
                nativeMediaBrowser=null;
            }
            if(nativeMediaOverlay!=null) {
                if(nativeMediaOverlay.getParent() instanceof ViewGroup)
                    ((ViewGroup)nativeMediaOverlay.getParent()).removeView(nativeMediaOverlay);
                nativeMediaOverlay=null;
            }
            if(savedTvWebView!=null){
                try{savedTvWebView.onResume();}catch(Throwable ignored){}
                savedTvWebView=null;
            }
            if(pausedTvVideo!=null){
                // Original channel playback is restored by choosing a channel in UNIT.
                pausedTvVideo=null;
            }
            nativeTvPlayerSlot=null;
            tvIsEmbedded=false;
        }

        /** Locate the real television player host without changing UNIT's channel grid. */
        private FrameLayout findNativeTvPlayerSlot(View root) {
            View player=findNativePlayerCandidate(root);
            if(player==null)return null;
            android.view.ViewParent parent=player.getParent();
            while(parent instanceof View && parent!=body){
                View v=(View)parent;
                if(v instanceof FrameLayout &&
                   v.getWidth()>=dip(140) && v.getHeight()>=dip(75) &&
                   v.getHeight()<body.getHeight()*.92f){
                    return (FrameLayout)v;
                }
                parent=v.getParent();
            }
            return null;
        }
        private View findNativePlayerCandidate(View root) {
            if(root==null || root==nativeMediaBar || root==nativeMediaOverlay)return null;
            if(root instanceof WebView && root!=nativeMediaBrowser){
                String name=root.getClass().getName();
                if(name.contains("buildTvView") || root.getWidth()>=dip(160))
                    return root;
            }
            if(root instanceof android.widget.VideoView ||
               root instanceof android.view.TextureView ||
               root instanceof android.view.SurfaceView) return root;
            if(root instanceof ViewGroup){
                ViewGroup g=(ViewGroup)root;
                for(int i=0;i<g.getChildCount();i++){
                    View found=findNativePlayerCandidate(g.getChildAt(i));
                    if(found!=null)return found;
                }
            }
            return null;
        }
        private void pauseOriginalTvPlayer(View root) {
            if(root instanceof WebView && root!=nativeMediaBrowser) {
                savedTvWebView=(WebView)root;
                try{savedTvWebView.onPause();}catch(Throwable ignored){}
            }
            if(root instanceof android.widget.VideoView) {
                pausedTvVideo=(android.widget.VideoView)root;
                try{pausedTvVideo.pause();}catch(Throwable ignored){}
            }
            if(root instanceof ViewGroup) {
                ViewGroup g=(ViewGroup)root;
                for(int i=0;i<g.getChildCount();i++)
                    pauseOriginalTvPlayer(g.getChildAt(i));
            }
        }

        private void stopNativeTick() {
            if(nativeHandler instanceof Handler && nativeTick instanceof Runnable)
                ((Handler)nativeHandler).removeCallbacks((Runnable)nativeTick);
        }
        private static boolean flag(Object o,String name,boolean fallback) {
            try {return o.getClass().getField(name).getBoolean(o);}
            catch(Throwable e){return fallback;}
        }
        private static long numberLong(Object o,String name,long fallback){
            try {return ((Number)o.getClass().getField(name).get(o)).longValue();}
            catch(Throwable e){return fallback;}
        }
        /** 0.8 has no authoritative/source fields. Require fresh, connected,
         * non-demo data before showing its old vehicle snapshot as a live reading.
         */
        private void readReal() {
            latestSoc=gasoline=speed=powerKw=Double.NaN;
            try {
                Class<?> r=Class.forName("es.c10.unitplus.integrated.NativeSnapshot");
                Object d=r.getMethod("read").invoke(null);
                boolean usable=Boolean.TRUE.equals(d.getClass().getMethod("usable").invoke(d));
                boolean fresh=numberLong(d,"ageMs",Long.MAX_VALUE)>=0 &&
                    numberLong(d,"ageMs",Long.MAX_VALUE)<60000L;
                boolean connected=flag(d,"connected",false);
                boolean demo=flag(d,"demo",true);
                boolean legacy=!hasField(d,"authoritative");
                boolean trusted=legacy?(usable&&fresh&&connected&&!demo):
                    (usable&&flag(d,"authoritative",false));
                if(trusted){
                    latestSource=legacy?"UNIT 0.8: lectura conectada reciente · origen sin certificación":
                        String.valueOf(d.getClass().getField("source").get(d));
                    latestSoc=number(d,"soc");
                    speed=number(d,"speed");
                    powerKw=number(d,"power");
                    gasoline=number(d,"fuelLitres"); // 0.8 does not expose fuelLitres.
                    if(Double.isFinite(latestSoc)&&latestSoc>=0&&latestSoc<=100
                        &&System.currentTimeMillis()-lastSample>=7000) {
                        realSoc.add(latestSoc);
                        if(realSoc.size()>120)realSoc.remove(0);
                        lastSample=System.currentTimeMillis();
                    }
                }else latestSource="Sin muestra reciente conectada y verificable";
            }catch(Throwable e){latestSource="Lectura no disponible: "+e.getClass().getSimpleName();}
            readTripTotals();
            long now=android.os.SystemClock.elapsedRealtime();
            if(Double.isFinite(speed)&&speed>=0&&speed<=350)SpeedGraph.add(cockpitHistory,now,(float)speed);
            if(cockpitPerformance!=null && Double.isFinite(speed)){
                cockpitPerformance.sample(now,speed);cockpitResult=cockpitPerformance.status();
                if(cockpitPerformance.result!=null)cockpitPerformance=null;
            }
        }
        private static boolean hasField(Object obj,String name){
            try{obj.getClass().getField(name);return true;}
            catch(Throwable ignored){return false;}
        }
        private void readTripTotals() {
            consumed=recovered=tripKm=measuredSeconds=Double.NaN;
            balancePartial=false;hasTripTotals=false;
            try{
                Class<?> storeClass=Class.forName("es.c10.unitplus.integrated.TripStore");
                Object store=storeClass.getMethod("get",Context.class).invoke(null,activity);
                Object session=storeClass.getField("current").get(store);
                boolean modern=hasField(session,"measured");
                boolean complete=flag(session,"hasEnergy",false) &&
                    (modern?flag(session,"hasRecovered",false):flag(session,"baseline",false));
                boolean recorded=modern?flag(session,"measured",false):true;
                boolean legacyInvalid=modern?flag(session,"legacyEnergy",true):false;
                balancePartial=!modern || flag(session,"gap",false) ||
                    flag(session,"energyIncomplete",false) ||
                    flag(session,"recoveredIncomplete",false);
                tripKm=modern&&!flag(session,"hasDistance",false)?
                    Double.NaN:number(session,"km");
                measuredSeconds=number(session,"measuredSeconds");
                if(complete&&recorded&&!legacyInvalid){
                    double spent=number(session,"kwh");
                    double incoming=number(session,"recovered");
                    energyBalance=EnergyBalance.calculate(spent,incoming,balancePartial);
                    hasTripTotals=energyBalance.available();
                    if(hasTripTotals){
                        consumed=spent;recovered=incoming;
                        balanceSource=modern?"Lecturas del acumulador UNIT":
                            "UNIT 0.8 · balance legado ORIENTATIVO; origen sin confirmar";
                    }else balanceSource="Contadores disponibles, pero sin lectura numérica fiable";
                }else{
                    energyBalance=EnergyBalance.calculate(Double.NaN,Double.NaN,balancePartial);
                    balanceSource=modern?"Esperando muestras medidas":"UNIT 0.8: falta acumulado fiable";
                }
            }catch(Throwable error){
                energyBalance=EnergyBalance.calculate(Double.NaN,Double.NaN,true);
                balanceSource="Registro no accesible: "+error.getClass().getSimpleName();
            }
        }
        private void tripAction(boolean save) {
            try {
                Class<?> storeClass=Class.forName("es.c10.unitplus.integrated.TripStore");
                Object store=storeClass.getMethod("get",Context.class).invoke(null,activity);
                if(save){
                    Object ok=storeClass.getMethod("saveAndReset").invoke(store);
                    if(!Boolean.TRUE.equals(ok)) {
                        Toast.makeText(activity,"Aún no hay datos medidos para guardar",Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(activity,"Medición guardada. Nueva medición iniciada.",Toast.LENGTH_LONG).show();
                } else {
                    storeClass.getMethod("resetCurrent").invoke(store);
                    Toast.makeText(activity,"Contadores de medición reiniciados",Toast.LENGTH_SHORT).show();
                }
                realSoc.clear();
                lastSample=0;
                readReal();
                if(leftPane!=null)leftPane.refresh();
            } catch(Throwable err) {
                Toast.makeText(activity,"No se pudo cambiar la medición: "+
                    err.getClass().getSimpleName(),Toast.LENGTH_LONG).show();
            }
        }
        /** Copy ONLY credible vehicle readings, never VIN, login or position. */
        private void copyEnergySession() {
            readReal();
            StringBuilder b=new StringBuilder("UNIT Leapmotor Plus · resumen eléctrico\n");
            b.append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault())
                .format(new java.util.Date())).append("\n");
            b.append("Perfil configurado: ").append(
                CompatibilityTestLog.configuredProfile(activity)).append("\n");
            b.append("Origen: ").append(latestSource).append("\n");
            b.append("Potencia: ").append(display(powerKw,"kW")).append("\n");
            b.append("Batería: ").append(display(latestSoc,"%")).append("\n");
            b.append("Distancia de sesión: ").append(display(tripKm,"km")).append("\n");
            b.append("Energía gastada: ").append(display(consumed,"kWh")).append("\n");
            b.append("Energía entrante: ").append(display(recovered,"kWh")).append("\n");
            if(energyBalance.available()) {
                b.append("Balance neto de batería (gastado - entrante): ")
                    .append(display(energyBalance.netKwh,"kWh")).append("\n");
                b.append("Estado: ").append(energyBalance.status==EnergyBalance.Status.GAIN?
                    "GANANCIA DE ENERGÍA EN BATERÍA":
                    energyBalance.status==EnergyBalance.Status.LOSS?
                    "PÉRDIDA NETA DE ENERGÍA":"EQUILIBRIO").append("\n");
                b.append("Cobertura: ").append(energyBalance.partial?
                    "PARCIAL; intervalo incompleto":"INTERVALOS MEDIDOS").append("\n");
            } else b.append("Balance: NO DISPONIBLE (datos insuficientes)\n");
            b.append("Advertencia: energía entrante puede incluir generación REEV o carga, no solo frenada.\n");
            android.content.ClipboardManager cm=
                (android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if(cm!=null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText(
                    "Resumen energético medido",b.toString()));
                Toast.makeText(activity,"Resumen copiado. Puedes pegarlo en el chat.",
                    Toast.LENGTH_LONG).show();
            }
        }
        private static double number(Object obj,String field) {
            try{Object val=obj.getClass().getField(field).get(obj);
                double v=((Number)val).doubleValue();return Double.isFinite(v)?v:Double.NaN;
            }catch(Throwable ignored){return Double.NaN;}
        }
        private String display(double v,String suffix) {
            return Double.isFinite(v) ? String.format(Locale.ROOT,"%.2f %s",v,suffix) : "NO DISPONIBLE";
        }
        private int dip(int value) {return (int)(value*activity.getResources().getDisplayMetrics().density+0.5f);}
        private TextView label(String value,int sp,int color) {
            TextView t=new TextView(activity);t.setText(value);t.setTextSize(sp);t.setTextColor(color);
            t.setPadding(dip(10),dip(6),dip(10),dip(6));return t;
        }
        private LinearLayout row() {
            LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.HORIZONTAL);return l;
        }
        private Button button(String text) {
            Button b=new Button(activity);b.setText(text);b.setAllCaps(false);return b;
        }
        private Spinner choice(int selected) {
            Spinner s=new Spinner(activity);
            ArrayAdapter<String> arr=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_item,TABS);
            arr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            s.setAdapter(arr);s.setSelection(Math.max(0,Math.min(TABS.length-1,selected)));return s;
        }
        /**
         * One native UNIT tab = one feature. Split-screen is controlled by UNIT's
         * original rail (AA on one side, this view on the other). No duplicate
         * selectors and no cross-category content.
         */
        /** Each enhancement is opened in UNIT's own tab row. No second icon rail. */
        private void selectExtra(int tab) {
            ++nativeSelectionGeneration;
            closeNativeMediaWeb();
            clearNativeMediaBar();
            stopCustom();
            showing=true;
            stopNativeTick();
            custom=new LinearLayout(activity);
            custom.setOrientation(LinearLayout.VERTICAL);
            custom.setBackgroundColor(0xff081520);
            LinearLayout header=row();
            TextView title=label("C10 PLUS  ·  "+TABS[tab].toUpperCase(Locale.ROOT),21,0xff08e2c0);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            header.addView(title,new LinearLayout.LayoutParams(0,dip(48),1));
            TextView real=label("EN UNIT",12,0xff88bcc9);
            real.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(real);
            custom.addView(header);
            Pane pane=new Pane(tab);
            leftPane=pane;
            custom.addView(pane.layout,new LinearLayout.LayoutParams(-1,0,1f));
            body.removeAllViews();
            body.addView(custom,new FrameLayout.LayoutParams(-1,-1));
            updateHandler.post(refresh);
        }

        void pauseCatalog(){if(leftPane!=null && leftPane.media!=null)leftPane.media.stop();if(rightPane!=null && rightPane.media!=null)rightPane.media.stop();}
        private boolean catalogStationary(){
            if(activity.isFinishing()||!showing||closed)return false;
            try{
                Object d=Class.forName("es.c10.unitplus.integrated.NativeSnapshot").getMethod("read").invoke(null);
                long age=numberLong(d,"ageMs",Long.MAX_VALUE);
                double v=number(d,"speed");
                return age>=0 && age<=3000L && flag(d,"connected",false) && !flag(d,"demo",true)
                    && Boolean.TRUE.equals(d.getClass().getMethod("usable").invoke(d))
                    && Double.isFinite(v) && v>=0 && v<0.5;
            }catch(Throwable ignored){return false;}
        }
        private void stopCustom() {
            boolean previouslyShowing=showing;
            showing=false;updateHandler.removeCallbacks(refresh);
            if(leftPane!=null)leftPane.close();if(rightPane!=null)rightPane.close();
            leftPane=rightPane=null;custom=null;
            // Re-enable UNIT's original live refresh after the user returns to
            // its native Ruta, Viajes, Radio etc. Our custom pages pause it.
            if(previouslyShowing && !closed && nativeHandler instanceof Handler && nativeTick instanceof Runnable) {
                Handler h=(Handler)nativeHandler;
                h.removeCallbacks((Runnable)nativeTick);
                h.postDelayed((Runnable)nativeTick,550);
            }
        }
        void dispose() {
            closed=true;stopCustom();
            if(cockpitRadio!=null){cockpitRadio.stop();cockpitRadio=null;}
            cockpitPerformance=null;
            closeNativeMediaWeb();
            clearNativeMediaBar();
            updateHandler.removeCallbacks(nativeMediaSafetyTick);
            updateHandler.removeCallbacks(nativeMediaTabMonitor);
            for(TextView tab:extraButtons)if(tab.getParent()==nativeRow)nativeRow.removeView(tab);
            extraButtons.clear();
        }
        private final class Pane {
            final int kind;
            final LinearLayout layout;
            TextView value;
            WebView web;
            android.app.Dialog fullScreenWeb;
            android.webkit.WebChromeClient.CustomViewCallback fullScreenCallback;
            android.widget.CheckBox stationary;
            boolean stoppedForMotion;
            Graph graph;
            MetricDashboard dashboard;
            SpeedGraph cockpitGauge,cockpitGraph;
            TextView cockpitInfo;
            InAppUpdater updater;
            MediaPanel media;
            VehicleAccessAudit.Report accessReport;
            LinearLayout accessList;
            boolean disposed;
            Pane(int selected) {
                kind=selected;layout=new LinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(dip(5),dip(3),dip(5),dip(3));
                layout.setBackgroundColor(0xff102e3f);
                layout.addView(label(TABS[kind],19,0xff02e5c2));
                switch(kind) {
                    case 0:maps();break;
                    case 1:consumption();break;
                    case 2:graph();break;
                    case 3:access();break;
                    case 4:updates();break;
                    case 5:model();break;
                    case 6:cockpit();break;
                    case 7:radioPlus();break;
                    case 8:performancePlus();break;
                    case 9:catalog("tv");break;
                    case 10:catalog("favorites");break;
                }
            }
            void cockpit(){
                cockpitInfo=label("Datos de UNIT · sin simulación",14,0xffcadfe7);layout.addView(cockpitInfo);
                LinearLayout tiles=row();
                cockpitGauge=new SpeedGraph(activity,0xff091925,0xffeaf7fd,0xff23d3aa,true);
                cockpitGraph=new SpeedGraph(activity,0xff091925,0xffeaf7fd,0xff23d3aa,false);
                tiles.addView(cockpitGauge,new LinearLayout.LayoutParams(0,-1,1f));tiles.addView(cockpitGraph,new LinearLayout.LayoutParams(0,-1,1f));
                layout.addView(tiles,new LinearLayout.LayoutParams(-1,0,1f));
                dashboard=new MetricDashboard(activity);layout.addView(dashboard,new LinearLayout.LayoutParams(-1,0,1f));
            }
            void radioPlus(){catalog("radio");}
            void catalog(String mode){
                media=new MediaPanel(activity,layout,mode,()->catalogStationary());
            }
            void performancePlus(){
                cockpitInfo=label(cockpitResult,16,0xffcadfe7);layout.addView(cockpitInfo);
                layout.addView(label("Solo circuito cerrado. Lecturas UNIT aproximadas, no cronometraje homologado.",14,0xffcadfe7));
                for(int h:new int[]{50,100,120,140}){final int high=h;Button b=button("0–"+h+" km/h");b.setOnClickListener(v->armPerformance(0,high));layout.addView(b);}
                Button b=button("80–120 km/h · 3 s estables");b.setOnClickListener(v->armPerformance(80,120));layout.addView(b);
                Button cancel=button("Cancelar");cancel.setOnClickListener(v->{cockpitPerformance=null;cockpitResult="Cancelada";});layout.addView(cancel);
            }
            void armPerformance(int low,int high){new AlertDialog.Builder(activity).setMessage("¿Prueba en circuito cerrado y seguro?").setPositiveButton("Preparar",(d,w)->{cockpitPerformance=new Performance(low,high);cockpitResult="Esperando velocidad inicial";}).setNegativeButton("Cancelar",null).show();}
            void info(String s) {value=label(s,15,0xffcadfe7);layout.addView(value);}
            void web() {
                web=new WebView(activity);
                WebSettings ws=web.getSettings();
                ws.setJavaScriptEnabled(true);
                ws.setDomStorageEnabled(true);
                ws.setGeolocationEnabled(true);
                ws.setUseWideViewPort(true);
                ws.setLoadWithOverviewMode(true);
                ws.setAllowFileAccess(false);
                ws.setAllowContentAccess(false);
                ws.setAllowFileAccessFromFileURLs(false);
                ws.setAllowUniversalAccessFromFileURLs(false);
                ws.setSupportMultipleWindows(false);
                ws.setJavaScriptCanOpenWindowsAutomatically(false);
                ws.setMediaPlaybackRequiresUserGesture(true);
                if(Build.VERSION.SDK_INT>=21)ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
                web.setWebChromeClient(new WebChromeClient() {
                    @Override public void onGeolocationPermissionsShowPrompt(String origin,
                        GeolocationPermissions.Callback callback) {
                        Uri uri=Uri.parse(origin);
                        String host=uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);
                        boolean trusted="https".equalsIgnoreCase(uri.getScheme()) &&
                            ("www.google.com".equals(host)||"google.com".equals(host)||
                             "maps.google.com".equals(host));
                        if(!trusted) {
                            callback.invoke(origin,false,false);
                            if(value!=null)value.setText("Ubicación web denegada para un sitio no autorizado.");
                            return;
                        }
                        boolean permitted=activity.checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
                            activity.checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
                        if(!permitted) {
                            callback.invoke(origin,false,false);
                            if(value!=null)value.setText(
                                "Google Maps necesita permiso de ubicación Android. Entra en Modelo → Configuración inicial y permisos, autorízalo y vuelve a Maps.");
                            return;
                        }
                        new AlertDialog.Builder(activity)
                            .setTitle("Ubicación para Google Maps")
                            .setMessage("¿Permites que la página de Google Maps utilice la ubicación del dispositivo Android? No equivale al GPS interno del coche.")
                            .setPositiveButton("Permitir",(dialog,which)->callback.invoke(origin,true,false))
                            .setNegativeButton("No permitir",(dialog,which)->callback.invoke(origin,false,false))
                            .show();
                    }
                    @Override public void onShowCustomView(View view,CustomViewCallback cb) {
                        if(fullScreenWeb!=null) { cb.onCustomViewHidden();return; }
                        fullScreenCallback=cb;
                        fullScreenWeb=new Dialog(activity,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                        fullScreenWeb.setContentView(view);
                        fullScreenWeb.setOnDismissListener(d->{
                            if(fullScreenCallback!=null)fullScreenCallback.onCustomViewHidden();
                            fullScreenCallback=null;fullScreenWeb=null;
                        });
                        fullScreenWeb.show();
                    }
                    @Override public void onHideCustomView() {
                        if(fullScreenWeb!=null)fullScreenWeb.dismiss();
                    }
                });
                web.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest request) {
                        // Deep links, intents and external app redirects are deliberately refused.
                        String scheme=request.getUrl().getScheme();
                        if("https".equalsIgnoreCase(scheme)||"about".equalsIgnoreCase(scheme)) return false;
                        if(value!=null)value.setText("Enlace externo bloqueado: permanece dentro de UNIT");
                        return true;
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView w,String url) {
                        return !url.startsWith("https://")&&!url.startsWith("about:");
                    }
                    @Override public void onReceivedError(WebView w,WebResourceRequest r,android.webkit.WebResourceError error) {
                        if(r.isForMainFrame()&&value!=null)value.setText(
                            "Google Maps no se pudo cargar dentro de UNIT: "+
                            error.getErrorCode()+" · "+error.getDescription()+
                            ". Comprueba en Accesos C10 el motor WebView.");
                    }
                    @Override public void onReceivedHttpError(WebView w,WebResourceRequest r,
                            android.webkit.WebResourceResponse response) {
                        if(r.isForMainFrame() && response.getStatusCode()>=400 && value!=null)
                            value.setText("El servidor rechazó Google Maps web: HTTP "+
                                response.getStatusCode()+
                                ". La navegación puede requerir la aplicación Google Maps del móvil.");
                    }
                });
                layout.addView(web,new LinearLayout.LayoutParams(-1,0,1));
            }
            void maps() {
                info("Google Maps WEB dentro de UNIT. El mapa y la navegación pueden requerir un WebView compatible y permisos; Google Maps de Android Auto es otra aplicación.");
                LinearLayout search=row();
                EditText input=new EditText(activity);input.setTextColor(Color.WHITE);
                input.setSingleLine(true);input.setHint("Buscar lugar");
                search.addView(input,new LinearLayout.LayoutParams(0,-2,1));
                Button go=button("Buscar");search.addView(go);
                Button navigate=button("Ruta");search.addView(navigate);
                Button clear=button("Borrar");search.addView(clear);
                layout.addView(search);
                web();
                // Searching is not starting a trip. Directions are only requested explicitly.
                go.setOnClickListener(v->{
                    String q=input.getText().toString().trim();
                    if(!q.isEmpty())web.loadUrl("https://www.google.com/maps/search/?api=1&query="+Uri.encode(q));
                });
                navigate.setOnClickListener(v->{
                    String q=input.getText().toString().trim();
                    if(!q.isEmpty())web.loadUrl("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(q));
                });
                clear.setOnClickListener(v->{
                    input.setText("");
                    web.stopLoading();
                    web.loadUrl("https://www.google.com/maps");
                    web.clearHistory();
                    if(value!=null)value.setText("Búsqueda limpia. Ninguna nueva ruta iniciada.");
                });
                web.loadUrl("https://www.google.com/maps");
            }
            void consumption() {
                info("UNIT 0.8 · Balance orientativo si hay contadores; no confundir con regeneración exclusiva");
                dashboard=new MetricDashboard(activity);
                layout.addView(dashboard,new LinearLayout.LayoutParams(-1,0,1f));
                LinearLayout actions=row();
                Button restart=button("Reiniciar medición");
                restart.setOnClickListener(v->new AlertDialog.Builder(activity)
                    .setTitle("Reiniciar contadores")
                    .setMessage("Borrar los totales de la medición actual. El historial guardado se conserva.")
                    .setPositiveButton("Reiniciar",(d,w)->tripAction(false))
                    .setNegativeButton("Cancelar",null).show());
                actions.addView(restart,new LinearLayout.LayoutParams(0,dip(48),1));
                Button save=button("Guardar y reiniciar");
                save.setOnClickListener(v->tripAction(true));
                actions.addView(save,new LinearLayout.LayoutParams(0,dip(48),1));
                layout.addView(actions);
                Button summary=button("Copiar resumen de energía");
                summary.setOnClickListener(v->copyEnergySession());
                layout.addView(summary,new LinearLayout.LayoutParams(-1,dip(45)));
            }
            void graph() {
                info("Batería: historial en tiempo real · sin simulaciones");
                graph=new Graph(activity);
                layout.addView(graph,new LinearLayout.LayoutParams(-1,0,1f));
            }
            void access() {
                info("Diagnóstico solo lectura: comprobar qué detecta UNIT sin modificar el coche");
                LinearLayout tools=row();
                Button scan=button("Volver a comprobar");
                Button copy=button("Copiar informe");
                tools.addView(scan,new LinearLayout.LayoutParams(0,dip(48),1f));
                tools.addView(copy,new LinearLayout.LayoutParams(0,dip(48),1f));
                layout.addView(tools);
                Button diag=button("Diagnóstico del inicio y firma 4.5");
                diag.setOnClickListener(v->((C10NativeHubActivity)activity).showStartupDiagnostic());
                layout.addView(diag,new LinearLayout.LayoutParams(-1,dip(46)));
                Button tests=button("Registrar prueba de Android Auto / CarPlay");
                tests.setOnClickListener(v->showCompatibilityTests());
                layout.addView(tests,new LinearLayout.LayoutParams(-1,dip(49)));
                android.widget.ScrollView scroll=new android.widget.ScrollView(activity);
                scroll.setFillViewport(false);
                scroll.setVerticalScrollBarEnabled(true);
                accessList=new LinearLayout(activity);
                accessList.setOrientation(LinearLayout.VERTICAL);
                accessList.setPadding(dip(7),dip(8),dip(7),dip(16));
                scroll.addView(accessList);
                layout.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
                scan.setOnClickListener(v->updateAccessReport());
                copy.setOnClickListener(v->{
                    if(accessReport==null)updateAccessReport();
                    if(accessReport==null)return;
                    android.content.ClipboardManager clipboard=
                        (android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if(clipboard!=null) {
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                            "Diagnóstico UNIT Leapmotor Plus",
                                accessReport.asText()+"\n\n"+
                                CompatibilityTestLog.report(activity)));
                        Toast.makeText(activity,
                            "Informe copiado. Puedes pegarlo en este chat para revisarlo.",
                            Toast.LENGTH_LONG).show();
                    }
                });
                updateAccessReport();
            }
            void updateAccessReport() {
                if(disposed || accessList==null)return;
                accessReport=VehicleAccessAudit.scan(activity);
                accessList.removeAllViews();
                TextView header=label("ÚLTIMA COMPROBACIÓN  ·  "+
                    new java.text.SimpleDateFormat("dd/MM HH:mm:ss",Locale.getDefault())
                        .format(new java.util.Date(accessReport.checkedAt)),14,0xffb5d2de);
                accessList.addView(header);
                accessList.addView(label(
                    "Lectura real ≠ capacidad Android ≠ acceso al coche. "+
                    "No se intenta conectar a centralitas ni activar cámaras.",14,0xffd7e7ef));
                addAuditGroup("DATOS REALES DEL VEHÍCULO",accessReport.vehicle);
                addAuditGroup("HARDWARE Y PERMISOS ANDROID",accessReport.android);
                addAuditGroup("ANDROID AUTO Y CARPLAY",accessReport.projection);
                addAuditGroup("POSIBILIDADES POR INVESTIGAR",accessReport.future);
            }
            void addAuditGroup(String title,java.util.List<VehicleAccessAudit.Item> entries) {
                TextView heading=label(title,17,0xff12e3c0);
                heading.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                accessList.addView(heading);
                for(VehicleAccessAudit.Item entry:entries) {
                    LinearLayout card=new LinearLayout(activity);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dip(10),dip(6),dip(10),dip(8));
                    GradientDrawable bg=new GradientDrawable();
                    bg.setColor(0xff163546);
                    bg.setCornerRadius(dip(9));
                    card.setBackground(bg);
                    TextView name=label(entry.title,16,Color.WHITE);
                    name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    card.addView(name);
                    int statusColor=entry.state==VehicleAccessAudit.LIVE?0xff28e2af:
                        entry.state==VehicleAccessAudit.DEVICE?0xff79ceff:
                        entry.state==VehicleAccessAudit.PERMISSION?0xffffd279:0xffbac7d2;
                    card.addView(label(entry.status(),12,statusColor));
                    card.addView(label(entry.detail,14,0xffd0e1e8));
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
                    lp.bottomMargin=dip(7);
                    accessList.addView(card,lp);
                }
            }
            /** These observations are manually entered by the owner, not detected by CAN. */
            void showCompatibilityTests() {
                android.widget.ScrollView scroll=new android.widget.ScrollView(activity);
                LinearLayout holder=new LinearLayout(activity);
                holder.setPadding(dip(18),dip(10),dip(18),dip(10));
                holder.setOrientation(LinearLayout.VERTICAL);
                scroll.addView(holder);
                holder.addView(label("Registra las pruebas con el vehículo estacionado. "+
                    "Los resultados son tuyos; la app NO los interpreta como detección automática.",15,0xffd0e8f1));
                final int[] saved=CompatibilityTestLog.states(activity);
                final Spinner[] picks=new Spinner[CompatibilityTestLog.TESTS.length];
                for(int i=0;i<picks.length;i++) {
                    holder.addView(label(CompatibilityTestLog.TESTS[i],16,Color.WHITE));
                    Spinner choice=new Spinner(activity);
                    ArrayAdapter<String> items=new ArrayAdapter<>(activity,
                        android.R.layout.simple_spinner_item,CompatibilityTestLog.STATES);
                    items.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    choice.setAdapter(items);
                    choice.setSelection(saved[i]);
                    holder.addView(choice);
                    picks[i]=choice;
                }
                holder.addView(label("Observaciones (opcional, hasta 500 caracteres)",14,0xff9ec7d7));
                EditText notes=new EditText(activity);
                notes.setText(CompatibilityTestLog.notes(activity));
                notes.setHint("Ej.: CarPlay se ve, pero no hay sonido; TV y mapa bien...");
                notes.setMinLines(2);
                notes.setMaxLines(4);
                notes.setTextColor(Color.WHITE);
                notes.setHintTextColor(0xffa5bfca);
                holder.addView(notes);
                AlertDialog dialog=new AlertDialog.Builder(activity)
                    .setTitle("Pruebas de compatibilidad")
                    .setView(scroll)
                    .setPositiveButton("Guardar",(d,w)->{
                        int[] states=new int[picks.length];
                        for(int i=0;i<picks.length;i++)states[i]=picks[i].getSelectedItemPosition();
                        CompatibilityTestLog.save(activity,states,
                            notes.getText().toString(),
                            CompatibilityTestLog.configuredProfile(activity));
                        Toast.makeText(activity,
                            "Pruebas guardadas. Aparecerán al copiar el informe.",
                            Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancelar",null)
                    .create();
                dialog.show();
                if(dialog.getWindow()!=null)
                    dialog.getWindow().setLayout(
                        Math.min(activity.getResources().getDisplayMetrics().widthPixels-dip(30),dip(680)),
                        Math.min(activity.getResources().getDisplayMetrics().heightPixels-dip(80),dip(660)));
            }

            void updates() {
                info("NOVEDAD · Actualizar directamente desde UNIT sin abrir el navegador.");
                updater=new InAppUpdater(activity,layout);
                Button local=button("Instalar un APK descargado manualmente");
                local.setOnClickListener(v->((C10NativeHubActivity)activity).chooseUpdateApk());
                layout.addView(local,new LinearLayout.LayoutParams(-1,dip(46)));
                layout.addView(label("La descarga automática solo utiliza versiones publicadas en "+
                    "nuestro canal GitHub, firmadas con este mismo certificado. "+
                    "Android siempre pedirá autorización para instalarlas. "+
                    "Si el coche bloquea la instalación, no se fuerza ningún permiso.",14,0xffacced9));
            }
            void model() {
                info("Perfil seleccionado en UNIT · no implica identificación automática del vehículo.");
                ((C10NativeHubActivity)activity).addVehicleSelector(layout,false);
                TextView hint=label("",15,0xffb8d9e6);
                layout.addView(hint);
                Runnable show=()->{
                    String selected=CompatibilityTestLog.configuredProfile(activity);
                    VehicleProfileHints.Hint profile=VehicleProfileHints.interpret(selected);
                    hint.setText("Perfil UNIT: "+selected+
                        "\nFamilia: "+profile.model+"  ·  Tipo: "+profile.friendlyDrivetrain()+
                        "\n"+profile.guidance()+
                        "\nEl acceso real a funciones se confirma en Accesos C10.");
                };
                show.run();
                Button check=button("Actualizar ficha del modelo");
                check.setOnClickListener(v->show.run());
                layout.addView(check,new LinearLayout.LayoutParams(-1,dip(45)));
                Button again=button("Configuración inicial y permisos");
                again.setOnClickListener(v->((C10NativeHubActivity)activity).showSetup(0));
                layout.addView(again);
            }
            void refresh() {
                if(disposed)return;
                if(kind==6){long now=android.os.SystemClock.elapsedRealtime();Float v=Double.isFinite(speed)&&speed>=0?(float)speed:null;cockpitGauge.update(cockpitHistory,v,now);cockpitGraph.update(cockpitHistory,v,now);cockpitInfo.setText(latestSource);}
                if(media!=null)media.refresh();
                if(kind==8)cockpitInfo.setText(cockpitResult);
                if(value!=null && kind==1) value.setText(
                    "Potencia: "+latestSource+"  ·  "+balanceSource+
                    (balancePartial?"  ·  DATOS PARCIALES":""));
                if(dashboard!=null)dashboard.invalidate();
                if(value!=null && kind==2)value.setText(
                    Double.isFinite(latestSoc)?"REAL · "+display(latestSoc,"%")+" · "+latestSource:
                    "NO DISPONIBLE: sin lectura de batería");
                if(graph!=null)graph.invalidate();
            }
            void active(boolean visible) {if(web!=null)try{if(visible)web.onResume();else web.onPause();}catch(Throwable ignored){}}
            void close() {
                disposed=true;
                if(media!=null){media.close();media=null;}
                if(updater!=null){updater.close();updater=null;}
                if(fullScreenWeb!=null) {
                    try{fullScreenWeb.dismiss();}catch(Throwable ignored){}
                    fullScreenWeb=null;
                }
                if(web!=null) {
                    try{web.stopLoading();web.loadUrl("about:blank");web.onPause();layout.removeView(web);web.destroy();}
                    catch(Throwable ignored){}
                    web=null;
                }
            }
        }
        /**
         * Professional native Canvas dashboard modeled after UNIT's card
         * style and the C10's instant-power display. No chart is populated
         * with demo readings.
         */
        private final class MetricDashboard extends View {
            final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            final android.graphics.RectF oval=new android.graphics.RectF();
            MetricDashboard(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
            void words(Canvas c,String text,float x,float y,float size,int color,boolean bold,float maxWidth) {
                p.setColor(color);p.setStyle(Paint.Style.FILL);
                p.setTypeface(bold?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.DEFAULT);
                p.setTextSize(size);
                while(p.measureText(text)>maxWidth && p.getTextSize()>10f)p.setTextSize(p.getTextSize()*.91f);
                c.drawText(text,x,y,p);
            }
            void card(Canvas c,float l,float t,float r,float b,int color) {
                p.setColor(color);p.setStyle(Paint.Style.FILL);
                c.drawRoundRect(l,t,r,b,16,16,p);
            }
            @Override protected void onDraw(Canvas cv){
                float w=getWidth(),h=getHeight();
                if(w<260||h<170)return;
                final float k=Math.max(.55f,Math.min(1.45f,Math.min(w/940f,h/515f)));
                final float pad=12*k;
                final int BLUE=0xff57bdff,GREEN=0xff23d3aa,ORANGE=0xfff3a05b,YELLOW=0xffffd16d,
                    MUTE=0xff98b7c5,WHITE=0xffeaf7fd;
                p.setStyle(Paint.Style.FILL);p.setColor(0xff091925);
                cv.drawColor(0xff091925);
                float topHeight=h*.54f;
                float gaugeRight=w*.42f;
                card(cv,pad,pad,gaugeRight-pad,topHeight-pad,0xff142f41);
                // Instant-power track from OEM: charge left, discharge right.
                float meterY=topHeight*.15f;
                float cx=(gaugeRight)/2f;
                float trackLeft=pad*2,trackRight=gaugeRight-pad*2;
                p.setStrokeWidth(5*k);p.setColor(0xff3c5663);
                cv.drawLine(trackLeft,meterY,trackRight,meterY,p);
                p.setColor(0xffc2d1d7);
                cv.drawCircle(cx,meterY,4*k,p);
                if(Double.isFinite(powerKw)){
                    float extent=Math.min(1f,(float)Math.abs(powerKw)/150f);
                    p.setStrokeWidth(8*k);
                    p.setColor(powerKw>=0?ORANGE:GREEN);
                    cv.drawLine(cx,meterY,cx+(powerKw>=0?1:-1)*(trackRight-cx)*extent,meterY,p);
                }
                words(cv,"CARGA",trackLeft,meterY-13*k,14*k,MUTE,false,cx-trackLeft);
                words(cv,"GASTO",cx+12*k,meterY-13*k,14*k,MUTE,false,trackRight-cx-12*k);
                words(cv,Double.isFinite(powerKw)?
                    String.format(Locale.ROOT,"%.1f kW",Math.abs(powerKw)):"— kW",
                    trackLeft,meterY+33*k,24*k,Double.isFinite(powerKw)?
                    (powerKw>=0?ORANGE:GREEN):WHITE,true,trackRight-trackLeft);
                // Thick circular battery indicator.
                float r=Math.min((gaugeRight-3*pad)*.27f,topHeight*.245f);
                float gy=topHeight*.68f;
                oval.set(cx-r,gy-r,cx+r,gy+r);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(17*k);
                p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xff2b4a5b);
                cv.drawArc(oval,130,280,false,p);
                if(Double.isFinite(latestSoc)&&latestSoc>=0&&latestSoc<=100){
                    p.setColor(BLUE);cv.drawArc(oval,130,(float)(280*latestSoc/100f),false,p);
                }
                p.setStyle(Paint.Style.FILL);
                String battery=Double.isFinite(latestSoc)?
                    String.format(Locale.ROOT,"%.0f%%",latestSoc):"—";
                words(cv,battery,cx-r*.54f,gy+12*k,37*k,WHITE,true,r*1.28f);
                words(cv,"BATERÍA",cx-r*.67f,gy+33*k,14*k,MUTE,false,r*1.6f);

                float rightLeft=gaugeRight+pad,rightRight=w-pad;
                float rh=(topHeight-3*pad)/2;
                double[] metrics={consumed,recovered,gasoline,tripKm};
                String[] names={"GASTADO · BATERÍA","ENTRADA · BATERÍA","COMBUSTIBLE RESTANTE","DISTANCIA MEDIDA"};
                String[] units={" kWh"," kWh"," L"," km"};
                int[] colors={ORANGE,GREEN,YELLOW,BLUE};
                for(int i=0;i<4;i++){
                    int col=i%2,row=i/2;
                    float cw=(rightRight-rightLeft-pad)/2;
                    float x=rightLeft+col*(cw+pad),y=pad+row*(rh+pad);
                    card(cv,x,y,x+cw,y+rh,0xff153546);
                    p.setColor(colors[i]);cv.drawRoundRect(x+7*k,y+7*k,x+12*k,y+rh-7*k,3,3,p);
                    words(cv,names[i],x+20*k,y+27*k,16*k,MUTE,false,cw-29*k);
                    String number=Double.isFinite(metrics[i])?
                        String.format(Locale.ROOT,"%.2f",metrics[i])+units[i]:"—";
                    words(cv,number,x+20*k,y+rh-19*k,30*k,
                        Double.isFinite(metrics[i])?colors[i]:MUTE,true,cw-30*k);
                }
                // Comparative charge/discharge bars and net energy status.
                float lowerY=topHeight+pad, lowerB=h-pad;
                card(cv,pad,lowerY,w-pad,lowerB,0xff132d3d);
                float barX=pad+21*k,barR=w*.66f;
                float total=lowerB-lowerY;
                float labelY=lowerY+26*k;
                words(cv,"BALANCE ENERGÉTICO MEDIDO",barX,labelY,19*k,WHITE,true,barR-barX);
                double max=hasTripTotals?Math.max(consumed,recovered):Double.NaN;
                float barW=Math.max(20,barR-barX);
                for(int i=0;i<2;i++){
                    float y=lowerY+total*(i==0?.42f:.68f);
                    words(cv,i==0?"Gasto":"Entrada",barX,y-8*k,15*k,MUTE,false,barW*.35f);
                    float yy=y+2*k;float left=barX+barW*.32f;
                    float right=barR-11*k;
                    card(cv,left,yy,right,yy+15*k,0xff2a4556);
                    if(Double.isFinite(max)&&max>0&&hasTripTotals){
                        float ratio=(float)Math.min(1,(i==0?consumed:recovered)/max);
                        p.setColor(i==0?ORANGE:GREEN);
                        cv.drawRoundRect(left,yy,left+(right-left)*ratio,yy+15*k,7,7,p);
                    }
                }
                float bx=w*.68f, bw=w-bx-pad;
                if(energyBalance.available()){
                    boolean gain=energyBalance.status==EnergyBalance.Status.GAIN;
                    boolean neutral=energyBalance.status==EnergyBalance.Status.EQUAL;
                    int color=neutral?BLUE:(gain?GREEN:ORANGE);
                    words(cv,gain?"GANANCIA NETA":neutral?"EN EQUILIBRIO":"GASTO NETO",
                        bx,lowerY+39*k,21*k,color,true,bw);
                    words(cv,String.format(Locale.ROOT,"%+.3f kWh",-energyBalance.netKwh),
                        bx,lowerY+80*k,30*k,WHITE,true,bw);
                    words(cv,energyBalance.partial?"INTERVALOS PARCIALES":"DATOS MEDIDOS",
                        bx,lowerY+105*k,14*k,MUTE,false,bw);
                    if(Double.isFinite(energyBalance.recoveryPct))
                        words(cv,"Entrada / gasto: "+String.format(Locale.ROOT,"%.0f%%",energyBalance.recoveryPct),
                            bx,lowerY+126*k,14*k,GREEN,false,bw);
                } else {
                    words(cv,"SIN DATOS",bx,lowerY+44*k,25*k,MUTE,true,bw);
                    words(cv,"Necesita potencia real",bx,lowerY+71*k,16*k,MUTE,false,bw);
                }
                words(cv,"Entrada incluye cualquier carga de la batería; no solo frenada regenerativa.",
                    barX,lowerB-10*k,13*k,MUTE,false,w-2*pad-16*k);
            }
        }
        /**
         * Visual history + consumption/recharge comparison in one native page.
         * Empty charts remain empty when no measured samples are available.
         */
        private final class Graph extends View {
            final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            final Path curve=new Path();
            Graph(Context c){super(c);}
            void print(Canvas c,String txt,float x,float y,float px,int color,float width){
                p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                p.setTextSize(px);
                while(p.measureText(txt)>width&&p.getTextSize()>11)p.setTextSize(p.getTextSize()*.9f);
                p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawText(txt,x,y,p);
            }
            @Override protected void onDraw(Canvas cv){
                super.onDraw(cv);
                float w=getWidth(),h=getHeight();
                if(w<230||h<175)return;
                float k=Math.max(.62f,Math.min(1.3f,Math.min(w/850f,h/460f)));
                final int GREEN=0xff2ce6be,BLUE=0xff54c5ff,ORANGE=0xfff5a261,MUTE=0xff97b5c3,WHITE=0xffecf8fc;
                cv.drawColor(0xff091a28);
                p.setStyle(Paint.Style.FILL);p.setColor(0xff133044);
                cv.drawRoundRect(10*k,10*k,w-10*k,h*.64f,18*k,18*k,p);
                float l=49*k,r=w-29*k,t=76*k,b=h*.64f-25*k;
                print(cv,"HISTORIAL DE BATERÍA · SoC",l,40*k,23*k,WHITE,r-l);
                print(cv,"Datos reales recibidos desde UNIT",l,61*k,14*k,MUTE,r-l);
                p.setColor(0xff314b5a);p.setStrokeWidth(1.5f*k);
                for(int i=0;i<=4;i++){
                    float y=t+(b-t)*i/4f;
                    cv.drawLine(l,y,r,y,p);
                    print(cv,""+(100-i*25)+"%",8*k,y+4*k,12*k,MUTE,38*k);
                }
                if(realSoc.size()>=2){
                    curve.reset();
                    for(int i=0;i<realSoc.size();i++){
                        float x=l+(r-l)*i/(realSoc.size()-1f);
                        float y=b-(b-t)*(realSoc.get(i).floatValue()/100f);
                        if(i==0)curve.moveTo(x,y);else curve.lineTo(x,y);
                    }
                    p.setColor(BLUE);p.setStrokeWidth(4*k);p.setStyle(Paint.Style.STROKE);
                    p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
                    cv.drawPath(curve,p);
                    float yy=b-(b-t)*(realSoc.get(realSoc.size()-1).floatValue()/100f);
                    p.setStyle(Paint.Style.FILL);cv.drawCircle(r,yy,7*k,p);
                    print(cv,display(realSoc.get(realSoc.size()-1),"%"),l,b+19*k,15*k,BLUE,150*k);
                }else{
                    print(cv,"Sin muestras reales suficientes",l,t+(b-t)*.57f,19*k,MUTE,r-l);
                }
                float cardTop=h*.66f,cardBottom=h-10*k;
                p.setColor(0xff123040);
                cv.drawRoundRect(10*k,cardTop,w-10*k,cardBottom,18*k,18*k,p);
                float labelX=27*k,maxW=Math.max(100,w*.56f);
                print(cv,"GASTADO vs. ENERGÍA ENTRANTE",labelX,cardTop+28*k,20*k,WHITE,maxW);
                float barLeft=labelX+91*k,barRight=w*.69f;
                float max=hasTripTotals?(float)Math.max(consumed,recovered):0;
                for(int i=0;i<2;i++){
                    float y=cardTop+44*k+i*(cardBottom-cardTop-65*k)/2;
                    print(cv,i==0?"Gasto":"Entrada",labelX,y+12*k,14*k,MUTE,85*k);
                    p.setColor(0xff254b5e);p.setStyle(Paint.Style.FILL);
                    cv.drawRoundRect(barLeft,y,barRight,y+17*k,7*k,7*k,p);
                    if(hasTripTotals&&max>0){
                        float ratio=(float)((i==0?consumed:recovered)/max);
                        p.setColor(i==0?ORANGE:GREEN);
                        cv.drawRoundRect(barLeft,y,barLeft+(barRight-barLeft)*ratio,y+17*k,7*k,7*k,p);
                    }
                }
                float right=w*.72f;
                if(energyBalance.available()){
                    int col=energyBalance.status==EnergyBalance.Status.GAIN?GREEN:
                        energyBalance.status==EnergyBalance.Status.EQUAL?BLUE:ORANGE;
                    print(cv,energyBalance.status==EnergyBalance.Status.GAIN?"GANANCIA":
                        energyBalance.status==EnergyBalance.Status.EQUAL?"EQUILIBRIO":"PÉRDIDA",
                        right,cardTop+39*k,21*k,col,w-right-24*k);
                    print(cv,String.format(Locale.ROOT,"%+.2f kWh",-energyBalance.netKwh),
                        right,cardTop+81*k,24*k,WHITE,w-right-24*k);
                    print(cv,energyBalance.partial?"PARCIAL":"REAL",
                        right,cardTop+109*k,14*k,MUTE,w-right-24*k);
                }else{
                    print(cv,"SIN BALANCE",right,cardTop+56*k,19*k,MUTE,w-right-24*k);
                }
            }
        }

    }
}
