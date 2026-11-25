package com.headqlink.link;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * C10 Plus: features in the existing CarHubScreen tab strip.
 *
 * This is deliberately a read-only diagnostic/visualization screen. Neither a
 * CarCloud response nor an Android phone sensor is an ECU/CAN diagnostic.
 * All integrated web services run inside this screen's own WebView.
 */
final class C10PlusScreen implements CarScreen {
    private static final int BG = 0xff08151f;
    private static final int PANEL = 0xff11283a;
    private static final int TEXT = 0xfff0f7fa;
    private static final int DIM = 0xffa5bdc9;
    private static final int ACCENT = 0xff00caa8;
    private static final String[] FEATURES = {
        "Maps", "Multimedia", "Consumos", "Gráficas", "Accesos C10",
        "Prestaciones", "Actualizaciones", "Modelo"
    };
    private static final String[] MEDIA_NAMES = {
        "Spotify", "YouTube", "Netflix", "Prime Video", "Max", "Pluto TV"
    };
    private static final String[] MEDIA_URLS = {
        "https://open.spotify.com", "https://www.youtube.com",
        "https://www.netflix.com", "https://www.primevideo.com",
        "https://www.max.com", "https://pluto.tv"
    };

    private Host host;
    private Context context;
    private SharedPreferences prefs;
    private LinearLayout root;
    private LinearLayout sides;
    private Spinner leftPicker, rightPicker;
    private Button toggle;
    private FrameLayout leftBox, rightBox;
    private Pane left, right;
    private boolean split;
    private boolean released;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<Double> realSocSamples = new ArrayList<>();
    private long lastSampleAt;
    private double latestRealSoc = Double.NaN;
    private String latestAge = "sin lectura";
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (released) return;
            try {
                CarCloud.Snapshot snap = CarCloud.car();
                long now = System.currentTimeMillis();
                // Never use demo, guessed, expired or invalid numbers as real data.
                if (snap != null && !snap.demo && snap.hasData()
                        && snap.ageMs(now) <= 120000) {
                    double soc = snap.soc(now, 120000);
                    if (Double.isFinite(soc) && soc >= 0 && soc <= 100) {
                        latestRealSoc = soc;
                        latestAge = (snap.ageMs(now) / 1000) + " s";
                        if (now - lastSampleAt >= 10000) {
                            realSocSamples.add(soc);
                            if (realSocSamples.size() > 90) realSocSamples.remove(0);
                            lastSampleAt = now;
                        }
                    } else clearCurrent();
                } else clearCurrent();
                if (left != null) left.refresh();
                if (right != null && split) right.refresh();
            } catch (RuntimeException ignored) {
                clearCurrent();
            }
            ui.postDelayed(this, 5000);
        }
    };

    private void clearCurrent() {
        latestRealSoc = Double.NaN;
        latestAge = "sin lectura reciente";
    }

    @Override public View create(Host h) {
        host = h;
        context = h.context();
        prefs = context.getSharedPreferences("c10plus_inside_unit", Context.MODE_PRIVATE);
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout top = row();
        TextView title = text("C10 PLUS  |  integrado en UNIT", 21, ACCENT);
        title.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        toggle = button("Dividir");
        toggle.setOnClickListener(v -> changeSplit(!split));
        top.addView(toggle, new LinearLayout.LayoutParams(dp(145), dp(46)));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(51)));

        LinearLayout picks = row();
        leftPicker = picker();
        rightPicker = picker();
        picks.addView(leftPicker, new LinearLayout.LayoutParams(0, dp(49), 1));
        picks.addView(rightPicker, new LinearLayout.LayoutParams(0, dp(49), 1));
        root.addView(picks);

        TextView note = text("REAL = nube Leapmotor reciente  |  No hay acceso ECU confirmado  |  Vídeo solo estacionado", 13, DIM);
        note.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(note, new LinearLayout.LayoutParams(-1, dp(28)));

        sides = row();
        leftBox = new FrameLayout(context);
        rightBox = new FrameLayout(context);
        sides.addView(leftBox, new LinearLayout.LayoutParams(0, -1, 1f));
        sides.addView(rightBox, new LinearLayout.LayoutParams(0, -1, 1f));
        root.addView(sides, new LinearLayout.LayoutParams(-1, 0, 1f));

        int i = clamp(prefs.getInt("left", 0));
        int j = clamp(prefs.getInt("right", 2));
        leftPicker.setSelection(i);
        rightPicker.setSelection(j);
        leftPicker.setOnItemSelectedListener(listener(0));
        rightPicker.setOnItemSelectedListener(listener(1));
        split = prefs.getBoolean("split", true);
        changeSplit(split);
        open(0, i);
        open(1, j);
        ui.post(refresh);
        return root;
    }

    private int clamp(int i) { return Math.max(0, Math.min(FEATURES.length - 1, i)); }
    private int dp(int n) { return (int) (n * context.getResources().getDisplayMetrics().density + 0.5f); }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setPadding(dp(7), dp(3), dp(7), dp(3));
        return l;
    }
    private TextView text(String s, int size, int color) {
        TextView v = new TextView(context);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setPadding(dp(9), dp(6), dp(9), dp(6));
        return v;
    }
    private Button button(String s) {
        Button b = new Button(context);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(14);
        return b;
    }
    private Spinner picker() {
        Spinner s = new Spinner(context);
        ArrayAdapter<String> a = new ArrayAdapter<>(context,
            android.R.layout.simple_spinner_item, FEATURES);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        s.setBackgroundColor(PANEL);
        s.setPadding(dp(8), dp(3), dp(8), dp(3));
        return s;
    }
    private AdapterView.OnItemSelectedListener listener(final int side) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View v, int index, long id) {
                if (!released) open(side, index);
            }
            @Override public void onNothingSelected(AdapterView<?> a) {}
        };
    }
    private void changeSplit(boolean active) {
        split = active;
        if (rightBox != null) rightBox.setVisibility(split ? View.VISIBLE : View.GONE);
        if (rightPicker != null) rightPicker.setVisibility(split ? View.VISIBLE : View.GONE);
        if (toggle != null) toggle.setText(split ? "Un panel" : "Dividir");
        if (prefs != null) prefs.edit().putBoolean("split", split).apply();
        if (right != null) right.active(split);
    }
    private void open(int side, int index) {
        if (released) return;
        index = clamp(index);
        FrameLayout target = side == 0 ? leftBox : rightBox;
        if (target == null) return;
        if (side == 0) { if (left != null) left.close(); left = null; }
        else { if (right != null) right.close(); right = null; }
        target.removeAllViews();
        Pane p = new Pane(index);
        target.addView(p.layout, new FrameLayout.LayoutParams(-1, -1));
        if (side == 0) left = p; else right = p;
        p.active(side == 0 || split);
        prefs.edit().putInt(side == 0 ? "left" : "right", index).apply();
    }

    private final class Pane {
        final int index;
        final LinearLayout layout;
        WebView web;
        TextView state;
        Graph graph;
        boolean disposed;

        Pane(int feature) {
            index = feature;
            layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setBackgroundColor(PANEL);
            layout.setPadding(dp(6), dp(3), dp(6), dp(3));
            layout.addView(text(FEATURES[feature].toUpperCase(Locale.ROOT), 18, ACCENT));
            try {
                if (feature == 0) maps();
                else if (feature == 1) media();
                else if (feature == 2) consumption();
                else if (feature == 3) graphs();
                else if (feature == 4) access();
                else if (feature == 5) performance();
                else if (feature == 6) updates();
                else model();
            } catch (Throwable e) {
                layout.addView(text("No disponible: " + e.getClass().getSimpleName(), 16, DIM));
            }
        }
        private void status(String label) {
            state = text(label, 15, DIM);
            layout.addView(state);
        }
        private void web() {
            web = new WebView(context);
            WebSettings s = web.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setAllowFileAccess(false);
            s.setAllowContentAccess(false);
            s.setMediaPlaybackRequiresUserGesture(true);
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            web.setWebChromeClient(new WebChromeClient());
            web.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView w, WebResourceRequest r) {
                    String scheme = r.getUrl().getScheme();
                    // Never delegate to an external app. Only safe web URLs stay inside WebView.
                    if ("https".equalsIgnoreCase(scheme)) return false;
                    if (state != null) state.setText("Enlace no compatible dentro de UNIT");
                    return true;
                }
                @Override public void onReceivedError(WebView w, WebResourceRequest r, WebResourceError e) {
                    if (r.isForMainFrame() && state != null) state.setText("No se pudo cargar el servicio en WebView");
                }
            });
            layout.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));
        }
        private void maps() {
            status("Google Maps web dentro de UNIT; navegación GPS/voz no verificada");
            LinearLayout line = row();
            EditText destination = new EditText(context);
            destination.setTextColor(TEXT);
            destination.setHintTextColor(DIM);
            destination.setSingleLine(true);
            destination.setHint("Buscar destino");
            line.addView(destination, new LinearLayout.LayoutParams(0, -2, 1f));
            Button go = button("Buscar");
            line.addView(go);
            layout.addView(line);
            web();
            go.setOnClickListener(v -> {
                String dest = destination.getText().toString().trim();
                if (!TextUtils.isEmpty(dest)) web.loadUrl("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(dest));
            });
            web.loadUrl("https://www.google.com/maps");
        }
        private void media() {
            status("Servicios en WebView. Algunos bloquean vídeo/login por DRM; sin redirección a otras apps");
            CheckBox stationary = new CheckBox(context);
            stationary.setText("Confirmo que el vehículo está estacionado");
            stationary.setTextColor(TEXT);
            layout.addView(stationary);
            LinearLayout line = row();
            Spinner choices = new Spinner(context);
            ArrayAdapter<String> a = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, MEDIA_NAMES);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            choices.setAdapter(a);
            line.addView(choices, new LinearLayout.LayoutParams(0, -2, 1f));
            Button go = button("Abrir aquí");
            go.setEnabled(false);
            stationary.setOnCheckedChangeListener((v, checked) -> go.setEnabled(checked));
            line.addView(go);
            layout.addView(line);
            web();
            go.setOnClickListener(v -> {
                if (!stationary.isChecked()) return;
                web.loadUrl(MEDIA_URLS[choices.getSelectedItemPosition()]);
                state.setText("Servicio web integrado; reproducción real no comprobada");
            });
        }
        private void consumption() {
            status("Sin datos ECU: no se inventan kWh ni litros");
            layout.addView(text("Batería (SoC, fuente nube):", 16, TEXT));
            layout.addView(text("Electricidad consumida: NO DISPONIBLE\nEnergía recuperada: NO DISPONIBLE\nGasolina consumida: NO DISPONIBLE\n\nNo se deduce la regeneración de una caída de batería.", 16, DIM));
        }
        private void graphs() {
            status("Gráfica de batería: solo puntos reales recientes, nunca DEMO");
            graph = new Graph(context);
            layout.addView(graph, new LinearLayout.LayoutParams(-1, 0, 1f));
        }
        private void access() {
            status("Diagnóstico de permisos y fuentes disponibles (solo lectura)");
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            int count = sm == null ? 0 : sm.getSensorList(-1).size();
            layout.addView(text("Sensores Android del dispositivo: " + count +
                "\nCámaras del vehículo 360º: NO VERIFICADO" +
                "\nCAN/OBD del Leapmotor: NO VERIFICADO" +
                "\nBatería Leapmotor: vía nube, si está conectada" +
                "\nGPS: del dispositivo Android; no demuestra acceso al vehículo", 16, DIM));
        }
        private void performance() {
            status("0–100 / 80–120: sin mediciones hasta validar velocidad real");
            layout.addView(text("Las aceleraciones deben medirse con datos reales y marcas de tiempo. No se muestran cifras de prueba como reales.", 16, DIM));
        }
        private void updates() {
            status("Actualizaciones internas: en preparación");
            layout.addView(text("Esta compilación de pruebas no publica todavía un canal de actualización firmado. Conserva la APK estable y actualiza manualmente.", 16, DIM));
        }
        private void model() {
            status("Elegir perfil visual. No modifica centralitas ni configuración del coche.");
            final String[] models = {"C10 REEV", "C10 BEV", "B10", "T03"};
            Spinner s = new Spinner(context);
            ArrayAdapter<String> a = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, models);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            s.setAdapter(a);
            s.setSelection(Math.max(0, Math.min(3, prefs.getInt("model", 0))));
            s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    prefs.edit().putInt("model", pos).apply();
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
            layout.addView(s);
        }
        void refresh() {
            if (disposed) return;
            if (state != null && (index == 2 || index == 3)) {
                state.setText(Double.isFinite(latestRealSoc)
                    ? String.format(Locale.ROOT, "REAL: SoC %.1f %%  ·  antigüedad %s (nube)", latestRealSoc, latestAge)
                    : "NO DISPONIBLE: no hay lectura reciente y fiable del coche");
            }
            if (graph != null) graph.invalidate();
        }
        void active(boolean visible) {
            if (web != null) {
                try { if (visible) web.onResume(); else web.onPause(); }
                catch (RuntimeException ignored) {}
            }
        }
        void close() {
            disposed = true;
            if (web != null) {
                try { web.stopLoading(); web.loadUrl("about:blank"); web.onPause(); }
                catch (RuntimeException ignored) {}
                layout.removeView(web);
                try { web.destroy(); } catch (RuntimeException ignored) {}
                web = null;
            }
        }
    }

    private final class Graph extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path line = new Path();
        Graph(Context c) { super(c); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            if (w < 100 || h < 80) return;
            paint.setColor(0xff1b4055);
            paint.setStrokeWidth(dp(1));
            for (int i = 0; i <= 4; i++) {
                float y = dp(20) + (h - dp(48)) * i / 4f;
                canvas.drawLine(dp(38), y, w - dp(10), y, paint);
            }
            if (realSocSamples.size() < 2) {
                paint.setColor(DIM);
                paint.setTextSize(dp(17));
                canvas.drawText("Sin suficientes muestras REALES", dp(35), h / 2, paint);
                return;
            }
            float yTop = dp(20), yBottom = h - dp(28);
            float left = dp(38), right = w - dp(12);
            line.reset();
            for (int i = 0; i < realSocSamples.size(); i++) {
                double soc = realSocSamples.get(i);
                float x = left + (right - left) * i / (realSocSamples.size() - 1f);
                float y = yBottom - (float)(soc / 100d) * (yBottom - yTop);
                if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
            }
            paint.setColor(ACCENT);
            paint.setStrokeWidth(dp(3));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(line, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    @Override public void destroy() {
        released = true;
        ui.removeCallbacks(refresh);
        if (left != null) left.close();
        if (right != null) right.close();
        left = null; right = null;
    }

    @Override public boolean keepOnThemeChange() { return true; }
}
