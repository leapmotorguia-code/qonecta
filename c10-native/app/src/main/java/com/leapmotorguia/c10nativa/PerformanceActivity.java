package com.leapmotorguia.c10nativa;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Visible, foreground-only GPS estimates. No coordinates or routes are persisted. */
public final class PerformanceActivity extends Activity implements LocationListener {
    private static final int GPS_REQUEST = 73;
    private static final int BG = Color.rgb(14,22,30), TEXT = Color.rgb(237,246,245), GREEN = Color.rgb(98,228,183);
    private LocationManager locationManager;
    private PerformanceTimer timer;
    private PerformanceTimer.Mode mode;
    private Spinner measurement, drivingMode;
    private TextView gpsStatus, instructions, result, history;
    private Button start, stop;
    private boolean tracking, recorded, awaitingPermission;
    private long lastFixNanos;

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView text(String s, int size, boolean bold) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(TEXT);
        v.setLineSpacing(dp(3), 1f); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v;
    }
    private void add(LinearLayout parent, android.view.View child, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(margin); parent.addView(child, p);
    }
    private Spinner spinner(String[] items) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(adapter); return s;
    }
    private Button button(String title) { Button b = new Button(this); b.setText(title); b.setAllCaps(false); b.setTextSize(18); return b; }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout column = new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(24), dp(18), dp(24), dp(24)); scroll.addView(column); setContentView(scroll);
        TextView title = text("Medición por GPS", 28, true); title.setTextColor(GREEN); add(column, title, 8);
        add(column, text("Estimación experimental. Úsala solo en un circuito autorizado y prepara todo antes de moverte. No manipules la pantalla al conducir.", 17, false), 12);
        add(column, text("Tipo de prueba", 18, true), 4);
        measurement = spinner(new String[]{"Desde parado: 0–50, 0–100, 0–120, 0–130 y 0–140", "Recuperación: 80–100, 80–120, 80–130 y 80–140"}); add(column, measurement, 8);
        add(column, text("Modo elegido manualmente", 18, true), 4);
        drivingMode = spinner(new String[]{"Eléctrico Sport", "Power Sport", "Otro modo"}); add(column, drivingMode, 8);
        start = button("Preparar medición"); stop = button("Detener medición"); stop.setEnabled(false); add(column, start, 6); add(column, stop, 12);
        gpsStatus = text("GPS: sin iniciar", 18, true); gpsStatus.setTextColor(GREEN); add(column, gpsStatus, 8);
        instructions = text("Pulsa Preparar. La app pedirá ubicación y buscará señal GPS.", 17, false); add(column, instructions, 8);
        result = text("Aún no hay resultados.", 20, true); add(column, result, 16);
        add(column, text("Historial de resultados principales", 21, true), 6);
        history = text("", 16, false); add(column, history, 8);
        Button erase = button("Borrar historial"); add(column, erase, 12);
        erase.setOnClickListener(v -> new android.app.AlertDialog.Builder(this).setTitle("¿Borrar historial?")
            .setMessage("Se eliminarán los resultados guardados en esta aplicación.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Borrar", (d, w) -> {
                getPreferences(MODE_PRIVATE).edit().remove("history").apply(); refreshHistory();
            }).show());
        add(column, text("La salida desde parado se detecta al superar 3 km/h; el cruce de velocidades se estima entre muestras. El resultado puede diferir de la medida real, especialmente con GPS lento o mala señal.", 16, false), 10);
        add(column, text("La app no modifica el vehículo. Guarda únicamente tiempos, fecha y el modo elegido; no guarda coordenadas ni rutas.", 16, false), 4);
        start.setOnClickListener(v -> askForGps()); stop.setOnClickListener(v -> stopGps("Medición detenida.")); refreshHistory();
    }

    private void askForGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            awaitingPermission = true;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, GPS_REQUEST);
        } else beginGps();
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == GPS_REQUEST && awaitingPermission) {
            awaitingPermission = false;
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) beginGps();
            else gpsStatus.setText("GPS: permiso de ubicación precisa no concedido.");
        }
    }

    private void beginGps() {
        if (tracking) return;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager == null || locationManager.getProvider(LocationManager.GPS_PROVIDER) == null) {
                gpsStatus.setText("GPS: este sistema no ofrece un proveedor GPS a la aplicación."); return;
            }
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsStatus.setText("GPS: desactivado o no disponible en este sistema."); return;
            }
            mode = measurement.getSelectedItemPosition() == 0 ? PerformanceTimer.Mode.FROM_STOP : PerformanceTimer.Mode.RECOVERY;
            timer = new PerformanceTimer(mode); recorded = false; lastFixNanos = 0;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200L, 0f, this, Looper.getMainLooper());
            tracking = true; start.setEnabled(false); stop.setEnabled(true); measurement.setEnabled(false); drivingMode.setEnabled(false);
            gpsStatus.setText("GPS: buscando señal precisa…"); instructions.setText("Mantén la aplicación abierta. Espera señal GPS."); result.setText("Aún no hay resultados.");
        } catch (SecurityException | IllegalArgumentException e) { gpsStatus.setText("GPS: el sistema no permite iniciar la ubicación."); }
    }

    private void stopGps(String message) {
        if (locationManager != null && tracking) {
            try { locationManager.removeUpdates(this); } catch (SecurityException ignored) { }
        }
        tracking = false; start.setEnabled(true); stop.setEnabled(false); measurement.setEnabled(true); drivingMode.setEnabled(true);
        gpsStatus.setText(message);
    }
    @Override protected void onPause() {
        super.onPause();
        if (tracking) stopGps("GPS detenido al salir de la pantalla. Vuelve a preparar la medición.");
    }
    @Override public void onProviderDisabled(String provider) { if (tracking) stopGps("GPS desactivado durante la medición."); }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override public void onLocationChanged(Location loc) {
        if (!tracking || timer == null) return;
        long ns = loc.getElapsedRealtimeNanos();
        if (ns == lastFixNanos) return;
        lastFixNanos = ns;
        long ageNs = SystemClock.elapsedRealtimeNanos() - ns;
        boolean speedOk = loc.hasSpeed() && loc.getSpeed() >= 0 && !Float.isNaN(loc.getSpeed());
        boolean posOk = loc.hasAccuracy() && loc.getAccuracy() <= 25f;
        boolean speedAccuracyOk = Build.VERSION.SDK_INT < 26 || !loc.hasSpeedAccuracy() || loc.getSpeedAccuracyMetersPerSecond() <= 1.5f;
        if (ns <= 0 || ageNs > 3_000_000_000L || ageNs < -1_000_000_000L || !speedOk || !posOk || !speedAccuracyOk) {
            timer.reset(); gpsStatus.setText("GPS insuficiente o señal antigua: esperando una lectura válida.");
            instructions.setText("La medición se reinició para evitar un resultado engañoso."); result.setText("Aún no hay resultados en esta medición."); return;
        }
        double kmh = loc.getSpeed() * 3.6;
        if (!timer.accept(ns / 1_000_000L, kmh)) {
            gpsStatus.setText("GPS interrumpido: vuelve a estabilizar la velocidad."); result.setText("Medición reiniciada."); return;
        }
        String accuracy = String.format(Locale.getDefault(), "GPS: %.0f km/h · precisión de posición ±%.0f m", kmh, loc.getAccuracy());
        if (Build.VERSION.SDK_INT >= 26 && !loc.hasSpeedAccuracy()) accuracy += " · precisión de velocidad no informada";
        gpsStatus.setText(accuracy);
        switch (timer.getState()) {
            case WAITING: instructions.setText(mode == PerformanceTimer.Mode.FROM_STOP ? "Espera parado 3 segundos con GPS válido." : "Mantén aproximadamente 80 km/h durante 3 segundos."); break;
            case ARMED: instructions.setText(mode == PerformanceTimer.Mode.FROM_STOP ? "Preparado: el tiempo comienza al superar 3 km/h." : "Preparado: necesita detectar el paso por 80 km/h al acelerar."); break;
            case TIMING: instructions.setText("Midiendo… resultados estimados a partir de la señal GPS."); break;
            case COMPLETE: instructions.setText("Medición finalizada."); break;
        }
        String prefix = mode == PerformanceTimer.Mode.FROM_STOP ? "0" : "80";
        int[] targets = mode == PerformanceTimer.Mode.FROM_STOP ? new int[]{50,100,120,130,140} : new int[]{100,120,130,140};
        StringBuilder out = new StringBuilder();
        for (int target : targets) {
            Long ms = timer.elapsedAt(target);
            out.append(prefix).append("–").append(target).append(": ").append(ms == null ? "—" : String.format(Locale.getDefault(), "%.2f s", ms / 1000.0)).append('\n');
        }
        result.setText(out.toString());
        int primary = mode == PerformanceTimer.Mode.FROM_STOP ? 100 : 120;
        Long elapsed = timer.elapsedAt(primary);
        if (!recorded && elapsed != null) {
            recorded = true;
            saveHistory(prefix + "–" + primary, elapsed, drivingMode.getSelectedItem().toString());
        }
        if (timer.getState() == PerformanceTimer.State.COMPLETE) stopGps("Medición finalizada. GPS detenido.");
    }

    private void saveHistory(String test, long ms, String chosenMode) {
        try {
            JSONArray values = new JSONArray(getPreferences(MODE_PRIVATE).getString("history", "[]"));
            JSONArray next = new JSONArray();
            JSONObject row = new JSONObject(); row.put("time", System.currentTimeMillis()); row.put("test", test); row.put("ms", ms); row.put("mode", chosenMode); next.put(row);
            for (int i = 0; i < values.length() && i < 29; i++) next.put(values.get(i));
            getPreferences(MODE_PRIVATE).edit().putString("history", next.toString()).apply(); refreshHistory();
        } catch (JSONException ignored) { history.setText("No se pudo guardar el historial."); }
    }
    private void refreshHistory() {
        try {
            JSONArray rows = new JSONArray(getPreferences(MODE_PRIVATE).getString("history", "[]"));
            if (rows.length() == 0) { history.setText("Todavía no hay resultados guardados."); return; }
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject r = rows.getJSONObject(i);
                out.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(r.getLong("time"))))
                    .append(" · ").append(r.getString("test")).append(" km/h · ")
                    .append(String.format(Locale.getDefault(), "%.2f s", r.getLong("ms") / 1000.0))
                    .append(" · ").append(r.getString("mode")).append('\n');
            }
            history.setText(out.toString());
        } catch (JSONException ignored) { history.setText("No se pudo leer el historial."); }
    }
}
