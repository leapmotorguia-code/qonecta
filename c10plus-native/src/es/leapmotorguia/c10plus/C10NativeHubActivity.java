package es.leapmotorguia.c10plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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
        handler.postDelayed(this::openNative, 200);
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
        private double speed = Double.NaN;
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
            latestSoc = consumed = recovered = gasoline = speed = Double.NaN;
            try {
                Class<?> r=Class.forName("es.c10.unitplus.integrated.NativeSnapshot");
                Object d=r.getMethod("read").invoke(null);
                if (!(Boolean)d.getClass().getMethod("usable").invoke(d)) {
                    latestSource="No disponible: sin lectura real reciente";return;
                }
                latestSource=String.valueOf(d.getClass().getField("source").get(d));
                latestSoc=number(d,"soc");
                consumed=number(d,"nativeEnergy");
                recovered=number(d,"nativeRecovered");
                gasoline=number(d,"fuelLitres");
                speed=number(d,"speed");
                if(Double.isFinite(latestSoc) && latestSoc >= 0 && latestSoc <= 100
                    && System.currentTimeMillis()-lastSample >= 7000) {
                    realSoc.add(latestSoc);
                    if(realSoc.size()>90)realSoc.remove(0);
                    lastSample=System.currentTimeMillis();
                }
            } catch(Throwable e){latestSource="No disponible: "+e.getClass().getSimpleName();}
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
        private void selectExtra(int tab) {
            stopCustom();
            showing=true;
            stopNativeTick();
            custom=new LinearLayout(activity);custom.setOrientation(LinearLayout.VERTICAL);
            custom.setBackgroundColor(0xff091925);
            LinearLayout bar=row();
            bar.addView(label("C10 PLUS · original UNIT",20,0xff00e2bc),new LinearLayout.LayoutParams(0,dip(49),1));
            Button splitButton=button("Dividir");
            bar.addView(splitButton);custom.addView(bar);
            LinearLayout controls=row();
            leftChoice=choice(tab);rightChoice=choice(prefs.getInt("right",2));
            controls.addView(leftChoice,new LinearLayout.LayoutParams(0,dip(50),1));
            controls.addView(rightChoice,new LinearLayout.LayoutParams(0,dip(50),1));
            custom.addView(controls);
            TextView hint=label("REAL = lectura verificada · Datos inexistentes = NO DISPONIBLE · vídeo solo estacionado",13,0xffbad1df);
            custom.addView(hint);
            LinearLayout halves=row();
            leftSlot=new FrameLayout(activity);rightSlot=new FrameLayout(activity);
            halves.addView(leftSlot,new LinearLayout.LayoutParams(0,-1,1));
            halves.addView(rightSlot,new LinearLayout.LayoutParams(0,-1,1));
            custom.addView(halves,new LinearLayout.LayoutParams(-1,0,1));
            split=prefs.getBoolean("split",true);
            splitButton.setOnClickListener(v->setSplit(!split));
            body.removeAllViews();body.addView(custom,new FrameLayout.LayoutParams(-1,-1));
            leftChoice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id) {
                    if(showing)swap(0,position);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
            });
            rightChoice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id) {
                    if(showing)swap(1,position);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
            });
            swap(0,tab);
            swap(1,prefs.getInt("right",2));
            setSplit(split);
            updateHandler.post(refresh);
        }
        private void setSplit(boolean active) {
            split=active;prefs.edit().putBoolean("split",active).apply();
            if(rightSlot != null)rightSlot.setVisibility(split?View.VISIBLE:View.GONE);
            if(rightChoice != null)rightChoice.setVisibility(split?View.VISIBLE:View.GONE);
            if(rightPane != null)rightPane.active(split);
        }
        private void swap(int side,int type) {
            if(!showing)return;
            type=Math.max(0,Math.min(type,TABS.length-1));
            FrameLayout slot=side==0?leftSlot:rightSlot;
            Pane old=side==0?leftPane:rightPane;
            if(old!=null)old.close();
            if(slot==null)return;
            slot.removeAllViews();
            Pane p=new Pane(type);slot.addView(p.layout,new FrameLayout.LayoutParams(-1,-1));
            if(side==0)leftPane=p;else rightPane=p;
            p.active(side==0||split);
            prefs.edit().putInt(side==0?"left":"right",type).apply();
        }
        private void stopCustom() {
            showing=false;updateHandler.removeCallbacks(refresh);
            if(leftPane!=null)leftPane.close();if(rightPane!=null)rightPane.close();
            leftPane=rightPane=null;custom=null;
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
                WebSettings ws=web.getSettings();ws.setJavaScriptEnabled(true);
                ws.setDomStorageEnabled(true);ws.setAllowFileAccess(false);
                ws.setAllowContentAccess(false);ws.setMediaPlaybackRequiresUserGesture(true);
                web.setWebChromeClient(new WebChromeClient());
                web.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView w, WebResourceRequest request) {
                        return !"https".equalsIgnoreCase(request.getUrl().getScheme());
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
                info("Web integrado; algunos servicios bloquean DRM y acceso");
                LinearLayout bar=row();
                Spinner providers=new Spinner(activity);
                ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_item,MEDIA);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                providers.setAdapter(adapter);
                bar.addView(providers,new LinearLayout.LayoutParams(0,-2,1));
                Button go=button("Abrir aquí");bar.addView(go);
                layout.addView(bar);
                web();
                go.setOnClickListener(v->{
                    // No open-other-app fallback. WebView stays inside UNIT.
                    if(Double.isFinite(speed) && Math.abs(speed) > 1.5) {
                        if(value!=null)value.setText("Multimedia bloqueada mientras se conduce");
                        return;
                    }
                    web.loadUrl(URLS[providers.getSelectedItemPosition()]);
                });
            }
            void consumption() {
                info("DATOS REALES · Si no hay lectura, se muestra —");
                dashboard=new MetricDashboard(activity);
                layout.addView(dashboard,new LinearLayout.LayoutParams(-1,0,1f));
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
                layout.addView(label("Prueba 3.1 → 3.2: abre aquí el APK 3.2 descargado. La confirmación de seguridad es obligatoria.",14,0xffb5ced9));
            }
            void model() {
                info("Perfil de visualización; no reprograma el coche");
                String[] profiles={"C10 REEV","C10 BEV","B10","T03"};
                Spinner s=new Spinner(activity);
                ArrayAdapter<String> a=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_item,profiles);
                a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                s.setAdapter(a);s.setSelection(Math.max(0,Math.min(3,prefs.getInt("model",0))));
                s.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id) {
                        prefs.edit().putInt("model",pos).apply();
                    }
                    @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
                });
                layout.addView(s);
            }
            void refresh() {
                if(disposed)return;
                if(value!=null && kind==2) value.setText(
                    "Fuente: "+latestSource+"\nConsumo y recuperación: solo si el coche los publica");
                if(dashboard!=null)dashboard.invalidate();
                if(value!=null && kind==3)value.setText(
                    Double.isFinite(latestSoc)?"REAL · "+display(latestSoc,"%")+" · "+latestSource:
                    "NO DISPONIBLE: sin lectura de batería");
                if(graph!=null)graph.invalidate();
                if(dashboard!=null)dashboard.invalidate();
            }
            void active(boolean visible) {if(web!=null)try{if(visible)web.onResume();else web.onPause();}catch(Throwable ignored){}}
            void close() {
                disposed=true;
                if(web!=null) {
                    try{web.stopLoading();web.loadUrl("about:blank");web.onPause();layout.removeView(web);web.destroy();}
                    catch(Throwable ignored){}
                    web=null;
                }
            }
        }
        /** Clear no-data cards: deliberate dashes, never synthetic sample values. */
        private final class MetricDashboard extends View {
            final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            MetricDashboard(Context c) {super(c);}
            @Override protected void onDraw(Canvas cv) {
                super.onDraw(cv);
                float w=getWidth(),h=getHeight();
                if(w<160||h<100)return;
                float gap=12f, top=6f, cardW=(w-gap*3)/2f, cardH=(h-gap*3)/2f;
                String[] names={"BATERÍA","CONSUMIDO","RECUPERADO","GASOLINA"};
                double[] vals={latestSoc,consumed,recovered,gasoline};
                String[] units={"%","kWh","kWh","L"};
                int[] colors={0xff42b6f6,0xfff29e55,0xff30d8a5,0xffe6ca66};
                for(int i=0;i<4;i++) {
                    int row=i/2,col=i%2;
                    float x=gap+col*(cardW+gap),y=top+row*(cardH+gap);
                    p.setStyle(Paint.Style.FILL);p.setColor(0xff193747);
                    cv.drawRoundRect(x,y,x+cardW,y+cardH,14,14,p);
                    p.setColor(colors[i]);
                    cv.drawRoundRect(x+10,y+12,x+15,y+cardH-12,3,3,p);
                    p.setTextSize(Math.max(15f,Math.min(22f,cardH*.18f)));
                    cv.drawText(names[i],x+25,y+Math.max(30,cardH*.33f),p);
                    p.setColor(0xfff1f9fa);
                    p.setTextSize(Math.max(18f,Math.min(38f,cardH*.30f)));
                    String v=Double.isFinite(vals[i])?
                        String.format(Locale.ROOT,"%.1f %s",vals[i],units[i]):"—";
                    cv.drawText(v,x+25,y+Math.max(61,cardH*.69f),p);
                    if(Double.isFinite(vals[i]) && i==0) {
                        p.setColor(0xff183b4b);cv.drawRoundRect(x+22,y+cardH-15,x+cardW-14,y+cardH-8,4,4,p);
                        p.setColor(colors[i]);cv.drawRoundRect(x+22,y+cardH-15,x+22+(float)((cardW-36)*Math.max(0,Math.min(1,vals[i]/100d))),y+cardH-8,4,4,p);
                    }
                }
            }
        }
        private final class Graph extends View {
            final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            final Path path=new Path();
            Graph(Context c){super(c);}
            @Override protected void onDraw(Canvas c) {
                float w=getWidth(),h=getHeight();
                if(w<=100||h<=100)return;
                p.setColor(0xff264453);p.setStrokeWidth(2);
                for(int i=0;i<=4;i++) {
                    float y=35+(h-70)*i/4f;
                    c.drawLine(45,y,w-20,y,p);
                }
                if(realSoc.size()<2) {
                    p.setTextSize(24);p.setColor(0xffccdce4);
                    c.drawText("Sin suficientes datos REALES",40,h/2f,p);return;
                }
                path.reset();
                for(int i=0;i<realSoc.size();i++) {
                    float x=45+(w-65)*i/(realSoc.size()-1f);
                    float y=h-35-(h-70)*(realSoc.get(i).floatValue()/100f);
                    if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
                }
                p.setColor(0xff00e6bf);p.setStrokeWidth(4);p.setStyle(Paint.Style.STROKE);
                c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
            }
        }
    }
}
