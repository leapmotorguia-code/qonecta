package com.leapmotorguia.c10nativa;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final int background = Color.rgb(14, 22, 30);
    private final int panel = Color.rgb(28, 42, 53);
    private final int accent = Color.rgb(98, 228, 183);
    private final int text = Color.rgb(237, 246, 245);
    private LinearLayout page;

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(dp(3), 1f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void spacer(LinearLayout parent, int height) {
        android.view.View view = new android.view.View(this);
        parent.addView(view, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.setBackground(shape(panel, 14));
        page.addView(card, new LinearLayout.LayoutParams(-1, -2));
        spacer(page, 14);
        return card;
    }

    private EditText number(String hint) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextColor(text);
        edit.setHintTextColor(Color.rgb(160, 179, 183));
        edit.setTextSize(19);
        edit.setHint(hint);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return edit;
    }

    private void costSection(String title, String consumptionHint, String priceHint) {
        LinearLayout box = card();
        box.addView(label(title, 23, text, true));
        spacer(box, 6);
        box.addView(label("Introduce los datos manualmente", 16, Color.LTGRAY, false));
        EditText consumption = number(consumptionHint);
        EditText price = number(priceHint);
        box.addView(consumption, new LinearLayout.LayoutParams(-1, dp(58)));
        box.addView(price, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView result = label("Sin calcular", 20, accent, true);
        Button button = new Button(this);
        button.setText("Calcular coste por 100 km");
        button.setTextSize(17);
        button.setAllCaps(false);
        box.addView(button, new LinearLayout.LayoutParams(-1, dp(55)));
        spacer(box, 9);
        box.addView(result);
        button.setOnClickListener(v -> {
            String computed = CostCalculator.calculate(consumption.getText().toString(), price.getText().toString());
            result.setText(computed == null ? "Introduce dos números válidos, sin negativos." : computed + " € / 100 km");
        });
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(18), dp(24), dp(24));
        scroll.addView(page, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        page.addView(label("C10 NATIVA", 30, accent, true));
        spacer(page, 4);
        page.addView(label("Versión experimental · 0.4 información", 18, text, false));
        spacer(page, 16);
        LinearLayout status = card();
        status.addView(label("La aplicación se ha abierto", 22, text, true));
        spacer(status, 7);
        status.addView(label("Calculadora y medición GPS experimental. No se conecta a los sistemas del coche ni activa cámaras.", 17, Color.LTGRAY, false));
        spacer(status, 10);
        status.addView(label("Android: " + Build.VERSION.RELEASE + "  ·  API " + Build.VERSION.SDK_INT + "\nModelo declarado: " + Build.MODEL, 16, text, false));
        boolean gps = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        spacer(status, 6);
        status.addView(label("GPS declarado por Android: " + (gps ? "sí" : "no detectado") + " (sin comprobar señal)", 16, text, false));
        spacer(status, 6);
        status.addView(label("La ubicación se solicita solo al iniciar la medición GPS. Sin conexión a internet ni servicios en segundo plano.", 16, accent, false));

        LinearLayout timing = card();
        timing.addView(label("Medición de prestaciones por GPS", 22, text, true));
        spacer(timing, 7);
        timing.addView(label("0–100 y 80–120 aproximados, con resultados locales. Necesita señal GPS y permiso de ubicación.", 17, Color.LTGRAY, false));
        Button openTiming = new Button(this);
        openTiming.setText("Abrir medición GPS");
        openTiming.setTextSize(18);
        openTiming.setAllCaps(false);
        timing.addView(openTiming, new LinearLayout.LayoutParams(-1, dp(55)));
        openTiming.setOnClickListener(v -> startActivity(new Intent(this, PerformanceActivity.class)));

        LinearLayout regen = card();
        regen.addView(label("Energía recuperada · estimación manual", 22, text, true));
        regen.addView(label("Introduce la potencia media regenerativa y el tiempo de frenada. Resultado aproximado, no es una lectura del coche.", 16, Color.LTGRAY, false));
        EditText power = number("Potencia media recuperada: kW");
        EditText seconds = number("Duración: segundos");
        regen.addView(power, new LinearLayout.LayoutParams(-1, dp(58)));
        regen.addView(seconds, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView energy = label("Sin calcular", 20, accent, true);
        Button calculate = new Button(this);
        calculate.setText("Calcular energía estimada");
        calculate.setAllCaps(false);
        regen.addView(calculate, new LinearLayout.LayoutParams(-1, dp(55)));
        regen.addView(energy);
        calculate.setOnClickListener(v -> {
            try {
                double kw = Double.parseDouble(power.getText().toString().replace(',', '.'));
                double sec = Double.parseDouble(seconds.getText().toString().replace(',', '.'));
                if (!Double.isFinite(kw) || !Double.isFinite(sec) || kw < 0 || sec < 0 || kw > 1000 || sec > 86400) throw new IllegalArgumentException();
                energy.setText(String.format(java.util.Locale.getDefault(), "Estimación: %.3f kWh (%.0f Wh)", kw * sec / 3600, kw * sec / 3.6));
            } catch (Exception ex) { energy.setText("Introduce valores válidos: 0–1000 kW y 0–86400 s."); }
        });
        LinearLayout live = card();
        live.addView(label("Datos del coche", 22, text, true));
        live.addView(label("Revoluciones del motor, encendido, potencia y regeneración real: no disponibles. Necesitan una conexión y datos compatibles comprobados para este C10.", 16, Color.LTGRAY, false));

        costSection("Coste eléctrico", "Consumo: kWh / 100 km", "Precio: € / kWh");
        costSection("Coste gasolina", "Consumo: litros / 100 km", "Precio: € / litro");
        LinearLayout next = card();
        next.addView(label("Funciones aún no disponibles", 21, text, true));
        spacer(next, 7);
        next.addView(label("Android Auto, CarPlay, datos reales del vehículo, cámaras, centinela y OBD no están incluidos.", 17, Color.LTGRAY, false));
        spacer(next, 7);
        next.addView(label("Realiza la configuración únicamente con el coche estacionado.", 16, accent, false));
    }
}
