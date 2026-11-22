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
        heading.setText("UNIT C10 Plus · Tu aplicación");
        heading.setTextColor(0xff04e2bd);
        heading.setTextSize(25);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(12,20,12,20);
        status.setText("Abriendo la barra original de UNIT...");
        root.addView(status);
        Button retry = new Button(this);
        retry.setText("Abrir UNIT C10 Plus");
        retry.setOnClickListener(v -> openNative());
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

    private void addVehicleSelector(LinearLayout col, boolean wizard) {
        TextView info=wizardText("Variantes reales del selector original UNIT:",17,0xffb9d9e7);
        col.addView(info);
        try {
            Class<?> profiles=Class.forName("es.c10.unitplus.integrated.VehicleProfiles");
            String[] names=(String[])profiles.getMethod("labels").invoke(null);
            int selected=(Integer)profiles.getMethod("index",Context.class).invoke(null,this);
            if(names==null || names.length==0)throw new IllegalStateException("sin perfiles");
            Spinner select=new Spinner(this);
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);
            select.setAdapter(adapter);
            if(selected>=0 && selected<names.length)select.setSelection(selected);
            col.addView(select);
            TextView chosen=wizardText("Perfil seleccionado: "+(selected>=0&&selected<names.length?names[selected]:"Sin identificar"),17,0xff00dfbe);
            col.addView(chosen);
            Button save=new Button(this);
            save.setText("Guardar modelo y versión");
            col.addView(save);
            save.setOnClickListener(v->{
                try{
                    int idx=select.getSelectedItemPosition();
                    profiles.getMethod("select",Context.class,int.class).invoke(null,this,idx);
                    chosen.setText("Guardado en UNIT: "+names[idx]);
                    Toast.makeText(this,"Modelo y versión guardados",Toast.LENGTH_SHORT).show();
                }catch(Throwable e){
                    chosen.setText("Error al guardar: "+e.getClass().getSimpleName());
                }
            });
        } catch(Throwable e) {
            col.addView(wizardText("No se ha podido cargar el selector de versiones de UNIT. "
                +"La selección anterior no se modificó. Detalle: "+e.getClass().getSimpleName(),15,0xfff0ac94));
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
        try {
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
            Toast.makeText(this, "Comprueba el nombre y acepta la actualización de Android", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "El instalador de Android no admite este archivo: "
                    + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
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

    private void attachNative(Dialog d, int attempt) {
        if (isFinishing() || !d.isShowing() || d.getWindow() == null) return;
        View view = findHub(d.getWindow().getDecorView());
        if (view instanceof ViewGroup) {
            try {
                bridge = C10Bridge.attach(this, (ViewGroup)view);
                status.setText("C10 Plus integrado en la barra original de UNIT");
            } catch (Throwable e) {
                status.setText("UNIT funciona, pero el acceso C10 Plus ha fallado: "+e.getClass().getSimpleName());
                getSharedPreferences("c10plus_beta_errors",MODE_PRIVATE).edit()
                    .putString("bridge",String.valueOf(e)).apply();
                Toast.makeText(this, "UNIT activo; error de integración: "+e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            }
        } else if (attempt < 12) {
            handler.postDelayed(() -> attachNative(d, attempt+1), 300);
        } else {
            status.setText("UNIT abierto, pero no encontré su barra de pestañas");
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
        private static final String[] TABS = {"Maps", "Multimedia", "Consumos", "Gráficas",
            "Accesos C10", "Prestaciones", "Actualizaciones", "Modelo"};
        private static final String[] MEDIA = {"Spotify","YouTube","Netflix","Prime Video","Max","Pluto TV"};
        private static final String[] URLS = {"https://open.spotify.com","https://www.youtube.com",
            "https://www.netflix.com","https://www.primevideo.com","https://www.max.com","https://pluto.tv"};
        private final Activity activity;
        private final FrameLayout body;
        private final LinearLayout nativeRow;
        private final Handler updateHandler = new Handler(Looper.getMainLooper());
        private final SharedPreferences prefs;
        private final ArrayList<TextView> extraButtons = new ArrayList<>();
        private final ArrayList<Double> realSoc = new ArrayList<>();
        private final Method nativeSelect;
        private final Object nativeHandler, nativeTick;
        private final ViewGroup hub;
        private LinearLayout custom;
        private FrameLayout leftSlot, rightSlot;
        private Spinner leftChoice, rightChoice;
        private Pane leftPane, rightPane;
        private boolean split, showing, closed;
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
                if (key.contains("game")||key.contains("karaoke")||title.contains("juego")||title.contains("karaoke"))
                    tv.setVisibility(View.GONE);
                else tv.setOnTouchListener((v,event)->{
                    if (event.getActionMasked()==MotionEvent.ACTION_DOWN && showing) stopCustom();
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
        }
        private void stopNativeTick() {
            if(nativeHandler instanceof Handler && nativeTick instanceof Runnable)
                ((Handler)nativeHandler).removeCallbacks((Runnable)nativeTick);
        }
        private void readReal() {
            latestSoc = gasoline = speed = powerKw = Double.NaN;
            try {
                Class<?> r=Class.forName("es.c10.unitplus.integrated.NativeSnapshot");
                Object d=r.getMethod("read").invoke(null);
                if ((Boolean)d.getClass().getMethod("usable").invoke(d)
                        && (Boolean)d.getClass().getField("authoritative").get(d)) {
                    latestSource=String.valueOf(d.getClass().getField("source").get(d));
                    latestSoc=number(d,"soc");
                    // fuelLitres is tank remaining, NEVER liters consumed.
                    gasoline=number(d,"fuelLitres");
                    speed=number(d,"speed");
                    powerKw=number(d,"power");
                    if(Double.isFinite(latestSoc)&&latestSoc>=0&&latestSoc<=100
                        &&System.currentTimeMillis()-lastSample>=7000) {
                        realSoc.add(latestSoc);
                        if(realSoc.size()>120)realSoc.remove(0);
                        lastSample=System.currentTimeMillis();
                    }
                } else latestSource="No disponible: sin lectura real reciente";
            } catch(Throwable e) {
                latestSource="Sin lectura: "+e.getClass().getSimpleName();
            }
            // Gross consumption and incoming battery energy are integrated by the
            // existing original C10 TripStore using real signed power samples.
            // The battery tank level is NOT a gross consumption meter.
            readTripTotals();
        }

        private void readTripTotals() {
            consumed=recovered=tripKm=measuredSeconds=Double.NaN;
            balancePartial=false;hasTripTotals=false;
            try {
                Class<?> storeClass=Class.forName("es.c10.unitplus.integrated.TripStore");
                Object store=storeClass.getMethod("get",Context.class).invoke(null,activity);
                Object session=storeClass.getField("current").get(store);
                Class<?> c=session.getClass();
                boolean measured=c.getField("measured").getBoolean(session);
                boolean legacy=c.getField("legacyEnergy").getBoolean(session);
                boolean hasEnergy=c.getField("hasEnergy").getBoolean(session);
                boolean hasReceived=c.getField("hasRecovered").getBoolean(session);
                balancePartial=c.getField("gap").getBoolean(session) ||
                    c.getField("energyIncomplete").getBoolean(session) ||
                    c.getField("recoveredIncomplete").getBoolean(session);
                tripKm=c.getField("hasDistance").getBoolean(session)?number(session,"km"):Double.NaN;
                measuredSeconds=number(session,"measuredSeconds");
                if(measured&&!legacy&&hasEnergy&&hasReceived) {
                    consumed=number(session,"kwh");
                    recovered=number(session,"recovered");
                    energyBalance=EnergyBalance.calculate(consumed,recovered,balancePartial);
                    hasTripTotals=energyBalance.available();
                    balanceSource=hasTripTotals?
                        "Potencia REAL integrada · "+(balancePartial?"cobertura parcial":"intervalos medidos") :
                        "Los acumulados no son válidos";
                } else {
                    energyBalance=EnergyBalance.calculate(Double.NaN,Double.NaN,balancePartial);
                    balanceSource=legacy?"Registro anterior sin origen verificable":
                        "Esperando muestras de potencia real";
                }
            } catch(Throwable e) {
                energyBalance=EnergyBalance.calculate(Double.NaN,Double.NaN,true);
                balanceSource="Datos de sesión no disponibles: "+e.getClass().getSimpleName();
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
        private void selectExtra(int tab) {
            stopCustom();
            showing = true;
            stopNativeTick();
            custom = new LinearLayout(activity);
            custom.setOrientation(LinearLayout.VERTICAL);
            custom.setBackgroundColor(0xff081520);
            LinearLayout header = row();
            TextView title = label("C10 PLUS   /   " + TABS[tab].toUpperCase(Locale.ROOT), 21, 0xff08e2c0);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            header.addView(title, new LinearLayout.LayoutParams(0, dip(53), 1));
            TextView real = label("DATOS REALES", 13, 0xff8cabbc);
            real.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(real);
            custom.addView(header);
            // Icon rail, available also inside UNIT's split-screen right-side panel.
            LinearLayout horizontal = row();
            android.widget.ScrollView railScroll = new android.widget.ScrollView(activity);
            railScroll.setFillViewport(false);
            railScroll.setVerticalScrollBarEnabled(false);
            LinearLayout rail = new LinearLayout(activity);
            rail.setOrientation(LinearLayout.VERTICAL);
            rail.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            rail.setPadding(dip(3), dip(7), dip(3), dip(7));
            rail.setBackgroundColor(0xff0b2331);
            final int[] icons = {
                android.R.drawable.ic_menu_mapmode,
                android.R.drawable.ic_media_play,
                android.R.drawable.ic_menu_info_details,
                android.R.drawable.ic_menu_sort_by_size,
                android.R.drawable.ic_menu_manage,
                android.R.drawable.ic_menu_compass,
                android.R.drawable.stat_sys_download_done,
                android.R.drawable.ic_menu_preferences
            };
            for (int i = 0; i < TABS.length; i++) {
                final int which = i;
                android.widget.ImageView circle = new android.widget.ImageView(activity);
                circle.setImageResource(icons[i]);
                circle.setColorFilter(which == tab ? 0xff08232a : 0xffb7deeb);
                int size = dip(49);
                circle.setPadding(dip(12), dip(12), dip(12), dip(12));
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                bg.setColor(which == tab ? 0xff22d7ba : 0xff213f50);
                bg.setStroke(dip(1), which == tab ? 0xff8bffe9 : 0xff466576);
                circle.setBackground(bg);
                circle.setContentDescription(TABS[i]);
                circle.setOnClickListener(v -> selectExtra(which));
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(size, size);
                cp.setMargins(dip(2), dip(5), dip(2), dip(6));
                rail.addView(circle, cp);
            }
            railScroll.addView(rail);
            horizontal.addView(railScroll, new LinearLayout.LayoutParams(dip(66), -1));
            Pane p = new Pane(tab);
            leftPane = p;
            horizontal.addView(p.layout, new LinearLayout.LayoutParams(0, -1, 1f));
            custom.addView(horizontal, new LinearLayout.LayoutParams(-1, 0, 1f));
            body.removeAllViews();
            body.addView(custom, new FrameLayout.LayoutParams(-1,-1));
            updateHandler.post(refresh);
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
            boolean disposed;
            Pane(int selected) {
                kind=selected;layout=new LinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(dip(5),dip(3),dip(5),dip(3));
                layout.setBackgroundColor(0xff102e3f);
                layout.addView(label(TABS[kind],19,0xff02e5c2));
                switch(kind) {
                    case 0:maps();break;
                    case 1:media();break;
                    case 2:consumption();break;
                    case 3:graph();break;
                    case 4:access();break;
                    case 5:performance();break;
                    case 6:updates();break;
                    case 7:model();break;
                }
            }
            void info(String s) {value=label(s,15,0xffcadfe7);layout.addView(value);}
            void web() {
                web=new WebView(activity);
                WebSettings ws=web.getSettings();
                ws.setJavaScriptEnabled(true);
                ws.setDomStorageEnabled(true);
                ws.setAllowFileAccess(false);
                ws.setAllowContentAccess(false);
                ws.setAllowFileAccessFromFileURLs(false);
                ws.setAllowUniversalAccessFromFileURLs(false);
                ws.setSupportMultipleWindows(false);
                ws.setJavaScriptCanOpenWindowsAutomatically(false);
                ws.setMediaPlaybackRequiresUserGesture(true);
                if(Build.VERSION.SDK_INT>=21)ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
                web.setWebChromeClient(new WebChromeClient() {
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
                        if(r.isForMainFrame()&&value!=null)value.setText("El sitio no se pudo cargar dentro de UNIT. Puede no admitir WebView.");
                    }
                });
                layout.addView(web,new LinearLayout.LayoutParams(-1,0,1));
            }
            void maps() {
                info("Maps dentro de UNIT (web; navegación y GPS pendientes de verificar)");
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
            void media() {
                info("Servicios dentro de UNIT, sin abrir navegador externo. Vídeo solo estacionado. DRM según el proveedor.");
                stationary=new android.widget.CheckBox(activity);
                stationary.setText("Confirmo que el coche está estacionado");
                stationary.setTextColor(0xffd9ebf6);
                stationary.setTextSize(15);
                layout.addView(stationary);
                LinearLayout chooser=row();
                Spinner providers=new Spinner(activity);
                ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_item,MEDIA);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                providers.setAdapter(adapter);
                chooser.addView(providers,new LinearLayout.LayoutParams(0,dip(48),1));
                Button open=button("Abrir dentro");
                chooser.addView(open);
                layout.addView(chooser);
                web();
                open.setEnabled(false);
                stationary.setOnCheckedChangeListener((v,checked)->open.setEnabled(checked));
                open.setOnClickListener(v->{
                    if(!stationary.isChecked())return;
                    if(Double.isFinite(speed)&&Math.abs(speed)>1.5){
                        if(value!=null)value.setText("Multimedia bloqueada: vehículo en movimiento");
                        return;
                    }
                    stoppedForMotion=false;
                    if(web!=null)web.loadUrl(URLS[providers.getSelectedItemPosition()]);
                });
            }
            void consumption() {
                info("DESDE EL ÚLTIMO REINICIO · Balance = gastado − entrante · combustible consumido no disponible");
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
            }
            void graph() {
                info("Batería: historial en tiempo real · sin simulaciones");
                graph=new Graph(activity);
                layout.addView(graph,new LinearLayout.LayoutParams(-1,0,1f));
            }
            void access() {
                info("Permisos Android ≠ acceso a cámaras/ECU del C10");
                SensorManager m=(SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
                int count=m==null?0:m.getSensorList(-1).size();
                layout.addView(label("Sensores Android: "+count+
                    "\nCámaras originales del C10: NO VERIFICADO\nBus CAN / OBD: NO VERIFICADO"+
                    "\nBatería y combustible: ver panel de consumo\nGPS: dispositivo Android",16,Color.WHITE));
            }
            void performance() {info("0–100 y 80–120 requieren GPS/OBD fiable. No hay tiempos inventados.");}
            void updates() {
                info("Actualización desde UNIT · sin borrar ajustes");
                layout.addView(label("1. Descarga el APK nuevo en este dispositivo.\n2. Pulsa «Elegir APK».\n3. Selecciona el archivo y confirma en el instalador Android.\n\nSolo se instalará encima si conserva el paquete y certificado de C10 Plus. No actualiza el software LeapOS.",16,Color.WHITE));
                Button b=button("Elegir APK e instalar");
                b.setOnClickListener(v->((C10NativeHubActivity)activity).chooseUpdateApk());
                layout.addView(b);
                layout.addView(label("Elige la nueva APK descargada para actualizar. La confirmación de seguridad es obligatoria.",14,0xffb5ced9));
            }
            void model() {
                info("Modelo y variante configurados en la fuente original de UNIT.");
                ((C10NativeHubActivity)activity).addVehicleSelector(layout,false);
                Button again=button("Volver a configuración inicial y permisos");
                again.setOnClickListener(v->((C10NativeHubActivity)activity).showSetup(0));
                layout.addView(again);
            }
            void refresh() {
                if(disposed)return;
                if(value!=null && kind==2) value.setText(
                    "Potencia: "+latestSource+"  ·  "+balanceSource+
                    (balancePartial?"  ·  DATOS PARCIALES":""));
                if(dashboard!=null)dashboard.invalidate();
                if(value!=null && kind==3)value.setText(
                    Double.isFinite(latestSoc)?"REAL · "+display(latestSoc,"%")+" · "+latestSource:
                    "NO DISPONIBLE: sin lectura de batería");
                if(graph!=null)graph.invalidate();
                if(kind==1&&web!=null&&Double.isFinite(speed)&&Math.abs(speed)>1.5&&!stoppedForMotion){
                    stoppedForMotion=true;
                    web.stopLoading();
                    web.loadUrl("about:blank");
                    web.onPause();
                    if(value!=null)value.setText("Multimedia detenida: el coche está en movimiento");
                    if(fullScreenWeb!=null)fullScreenWeb.dismiss();
                }
            }
            void active(boolean visible) {if(web!=null)try{if(visible)web.onResume();else web.onPause();}catch(Throwable ignored){}}
            void close() {
                disposed=true;
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
