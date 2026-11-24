package com.headqlink.link;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Energía del trayecto separada en consumo, regeneración y neto.
 * Importante: sin contador BMS independiente, consumo y recuperación son
 * estimaciones del modelo. La nube solo sirve para ajustar el NETO.
 *
 * "Reiniciar balance" establece un punto cero en esta pantalla;
 * no borra viajes históricos ni modifica el vehículo.
 */
final class C10EnergyScreen implements CarScreen {
    private static final String PREF = "c10lab_energy_balance";
    private static final String K_START = "start";
    private static final String K_CONSUMED = "consumed";
    private static final String K_RECOVERED = "recovered";
    private static final String K_KM = "km";
    private static final int BG = 0xFF081C26;
    private static final int CARD = 0xFF132D3C;
    private static final int MINT = 0xFF78E6B6;
    private static final int AMBER = 0xFFF4C67B;
    private static final int WHITE = 0xFFE5F3F4;
    private static final int MUTED = 0xFFB4C7CF;

    private Context context;
    private CarSensors sensors;
    private TextView consumed, recovered, net, percent, average, detail, source;
    private boolean active;
    private double lastRawConsumed, lastRawRecovered, lastRawKm;
    private long lastStart;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final EnergyBreakdown model = new EnergyBreakdown();
    private final EnergyBreakdown shown = new EnergyBreakdown();
    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            if (!active) return;
            refresh();
            handler.postDelayed(this, 1_500L);
        }
    };

    @Override public View create(Host h) {
        context = h.context();
        sensors = CarSensors.start(context);
        active = true;
        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(context);
        content.setPadding(25, 22, 25, 30);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(text("BALANCE ENERGÉTICO · C10 LAB", 33, WHITE, true));
        TextView subtitle = text("Viaje en curso · consumo bruto, energía recuperada y saldo neto", 21, MUTED, false);
        subtitle.setPadding(0, 5, 0, 18);
        content.addView(subtitle);

        LinearLayout metrics = new LinearLayout(context);
        metrics.setOrientation(LinearLayout.VERTICAL);
        content.addView(metrics);

        consumed = metric(metrics, "ENERGÍA CONSUMIDA", AMBER);
        recovered = metric(metrics, "ENERGÍA RECUPERADA", MINT);
        net = metric(metrics, "SALDO NETO · CONSUMO − RECUPERACIÓN", WHITE);
        percent = metric(metrics, "PORCENTAJE RECUPERADO SOBRE EL GASTO", MINT);
        average = metric(metrics, "PROMEDIO NETO DEL TRAMO", WHITE);

        detail = text("Leyendo viaje...", 23, WHITE, false);
        detail.setPadding(0, 15, 0, 10);
        content.addView(detail);

        source = text("", 20, AMBER, false);
        source.setPadding(15, 13, 15, 13);
        source.setBackground(round(0xFF3A3226));
        content.addView(source);

        TextView reset = text("↺  REINICIAR ESTE BALANCE", 24, MINT, true);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(16, 22, 16, 22);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = 18;
        content.addView(reset, btnLp);
        reset.setOnClickListener(v -> {
            if (!active) return;
            SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            p.edit().putLong(K_START, lastStart)
                    .putLong(K_CONSUMED, Double.doubleToLongBits(lastRawConsumed))
                    .putLong(K_RECOVERED, Double.doubleToLongBits(lastRawRecovered))
                    .putLong(K_KM, Double.doubleToLongBits(lastRawKm)).apply();
            refresh();
            Toast.makeText(context, "Balance reiniciado; los viajes guardados no se borran", Toast.LENGTH_SHORT).show();
        });

        refresh();
        handler.postDelayed(refreshTask, 1_500L);
        return scroll;
    }

    private TextView metric(LinearLayout parent, String label, int color) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 11, 16, 11);
        box.setBackground(round(CARD));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin=8;
        parent.addView(box, lp);
        box.addView(text(label, 18, MUTED, true));
        TextView value = text("—", 34, color, true);
        value.setPadding(0, 3, 0, 0);
        box.addView(value);
        return value;
    }

    private void refresh() {
        if (!active || context == null || sensors == null) return;
        CarSensors.Snapshot s = sensors.snapshot();
        sensors.tripBreakdown(model);
        CarCloud.Snapshot cloud = CarCloud.car();
        CloudEnergy.Result realTrip = TripLog.liveReal(cloud);
        LiveEnergy live = LiveEnergy.of(s.tripKwh, s.tripKmEnergy,
                realTrip, TripLog.liveFuelUsedL(cloud));
        LiveEnergy.Fit fit = live.fit(model,
                live.real ? CarCloud.tripSplit(context, TripLog.liveStartMs()) : null,
                shown);

        double gross = shown.consumed(), regen = shown.recovered();
        long start = TripLog.liveStartMs();
        if (start <= 0) start = 0;
        lastStart = start;
        lastRawConsumed = Math.max(0, gross);
        lastRawRecovered = Math.max(0, regen);
        lastRawKm = Math.max(0, s.tripKmEnergy);

        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        long baseStart = p.getLong(K_START, Long.MIN_VALUE);
        double baseGross = Double.longBitsToDouble(p.getLong(K_CONSUMED,
                Double.doubleToLongBits(0)));
        double baseRegen = Double.longBitsToDouble(p.getLong(K_RECOVERED,
                Double.doubleToLongBits(0)));
        double baseKm = Double.longBitsToDouble(p.getLong(K_KM,
                Double.doubleToLongBits(0)));

        // Cambió el viaje, el valor retrocedió al reiniciar sensores, o el
        // punto cero corresponde a otro perfil: ignorar los offsets antiguos.
        if (baseStart != start || baseGross > gross + 0.01 ||
                baseRegen > regen + 0.01 || baseKm > s.tripKmEnergy + 0.05) {
            baseGross = baseRegen = baseKm = 0;
        }
        EnergyFlowSummary flow = EnergyFlowSummary.of(
                Math.max(0, gross - baseGross),
                Math.max(0, regen - baseRegen),
                Math.max(0, s.tripKmEnergy - baseKm),
                fit != LiveEnergy.Fit.MODEL);
        consumed.setText(flow.ready ? fmt(flow.consumedKwh, 3) + " kWh" : "—");
        recovered.setText(flow.ready ? fmt(flow.recoveredKwh, 3) + " kWh" : "—");
        net.setText(flow.ready ? fmt(flow.netKwh, 3) + " kWh" : "—");
        percent.setText(!Double.isNaN(flow.recoveryPercent)
                ? fmt(flow.recoveryPercent, 1) + " %" : "—");
        average.setText(!Double.isNaN(flow.netPer100)
                ? fmt(flow.netPer100, 2) + " kWh / 100 km" : "—");
        detail.setText("Distancia del tramo: " + fmt(flow.distanceKm, 2) + " km"
                + "    ·    Perfil: " + profileName(new CarCloudStore(context).profile()));
        source.setText(flow.provenance
                + "\nLa batería puede regenerar en una bajada, pero no se garantiza recuperar más que lo gastado."
                + "\nLa energía de gasolina del generador REEV se contabiliza por separado en la sección Coche.");
    }

    private static String profileName(String p) {
        if (CarCloudStore.PROFILE_C10_REEV.equals(p)) return "C10 REEV";
        if (CarCloudStore.PROFILE_C10_PROMAX.equals(p)) return "C10 Pro Max";
        if (CarCloudStore.PROFILE_T03.equals(p)) return "T03";
        return "C10 Life";
    }

    private static String fmt(double n, int digits) {
        return String.format(Locale.getDefault(), "%." + digits + "f", n);
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private static GradientDrawable round(int color) {
        GradientDrawable b = new GradientDrawable();
        b.setColor(color);
        b.setCornerRadius(16f);
        b.setStroke(1, 0xFF305663);
        return b;
    }

    @Override public void destroy() {
        active = false;
        handler.removeCallbacks(refreshTask);
        CarSensors.stop();
        sensors=null;
        context=null;
    }
}
