package com.headqlink.link;

import android.app.Activity;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

final class ReevCostDialog {
    private ReevCostDialog() { }
    static void show(Activity activity) {
        float density = activity.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(24 * density);
        box.setPadding(pad, 0, pad, 0);
        TextView note = new TextView(activity);
        note.setText("Las cifras rellenadas son ejemplos: cámbialas por tus datos. Para un viaje mixto, evita contar dos veces la electricidad generada con gasolina y compara con una batería inicial y final parecidas.");
        box.addView(note);
        EditText kwh = field(activity, box, "Electricidad de red (kWh/100 km)", "18");
        EditText litres = field(activity, box, "Gasolina (litros/100 km)", "0");
        EditText kwhPrice = field(activity, box, "Precio electricidad (€/kWh)", "0.20");
        EditText litrePrice = field(activity, box, "Precio gasolina (€/litro)", "1.60");
        EditText distance = field(activity, box, "Distancia del viaje (km)", "100");
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(box);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Calculadora de coste REEV").setView(scroll)
                .setPositiveButton("Calcular", null).setNegativeButton("Cerrar", null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double a = parse(kwh), b = parse(litres), c = parse(kwhPrice), d = parse(litrePrice), e = parse(distance);
            double total = ReevCost.total(a, b, c, d, e);
            if (Double.isNaN(total) || e <= 0) {
                new MaterialAlertDialogBuilder(activity).setMessage("Revisa los números. La distancia debe ser mayor que cero.").setPositiveButton("Entendido", null).show();
                return;
            }
            String result = String.format(Locale.getDefault(), "Coste aproximado: %.2f € por %.1f km\nElectricidad: %.2f €\nGasolina: %.2f €\n\nCalculado con los datos introducidos; no mide el coche.",
                    total, e, a * c * e / 100, b * d * e / 100);
            new MaterialAlertDialogBuilder(activity).setTitle("Resultado estimado").setMessage(result).setPositiveButton("Cerrar", null).show();
        }));
        dialog.show();
    }
    private static EditText field(Activity a, LinearLayout box, String label, String example) {
        EditText field = new EditText(a);
        field.setHint(label);
        field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        field.setText(example);
        box.addView(field, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return field;
    }
    private static double parse(EditText field) {
        try { return Double.parseDouble(field.getText().toString().trim().replace(',', '.')); }
        catch (NumberFormatException ex) { return Double.NaN; }
    }
}
