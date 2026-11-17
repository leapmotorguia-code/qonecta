package com.leapmotorguia.c10nativa;

import android.app.Activity;
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
        page.addView(label("Primera prueba de pantalla · versión 0.1", 18, text, false));
        spacer(page, 16);
        LinearLayout status = card();
        status.addView(label("La aplicación se ha abierto", 22, text, true));
        spacer(status, 7);
        status.addView(label("Esta prueba solo muestra información de Android y calcula costes manuales. No se conecta al coche ni activa cámaras.", 17, Color.LTGRAY, false));
        spacer(status, 10);
        status.addView(label("Android: " + Build.VERSION.RELEASE + "  ·  API " + Build.VERSION.SDK_INT + "\nModelo declarado: " + Build.MODEL, 16, text, false));
        boolean gps = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        spacer(status, 6);
        status.addView(label("GPS declarado por Android: " + (gps ? "sí" : "no detectado") + " (sin comprobar señal)", 16, text, false));
        spacer(status, 6);
        status.addView(label("Sin permisos, conexión a internet ni servicios en segundo plano.", 16, accent, false));

        costSection("Coste eléctrico", "Consumo: kWh / 100 km", "Precio: € / kWh");
        costSection("Coste gasolina", "Consumo: litros / 100 km", "Precio: € / litro");
        LinearLayout next = card();
        next.addView(label("Funciones aún no disponibles", 21, text, true));
        spacer(next, 7);
        next.addView(label("Android Auto, CarPlay, datos reales, mediciones GPS, cámaras, centinela y OBD no están incluidos en esta prueba.", 17, Color.LTGRAY, false));
        spacer(next, 7);
        next.addView(label("Realiza la configuración únicamente con el coche estacionado.", 16, accent, false));
    }
}
