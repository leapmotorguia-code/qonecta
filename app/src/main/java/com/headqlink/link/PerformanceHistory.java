package com.headqlink.link;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/** Historial local de resultados del GPS. No guarda coordenadas. */
final class PerformanceHistory {
    private static final String PREFS = "c10_performance";
    private static final String[] MODES = {"Sin especificar", "Eléctrico Sport", "Power Sport", "Otro modo"};
    private static final int MAX = 30;
    private PerformanceHistory() { }

    static synchronized void save(Context context, String result) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray old = new JSONArray(p.getString("results", "[]"));
            long now = System.currentTimeMillis();
            int mode = p.getInt("mode", 0);
            if (mode < 0 || mode >= MODES.length) mode = 0;
            JSONObject latest = old.length() == 0 ? null : old.optJSONObject(old.length() - 1);
            boolean update = latest != null && latest.optInt("mode", 0) == mode
                    && now >= latest.optLong("time", 0) && now - latest.optLong("time", 0) < 60000
                    && result.startsWith(latest.optString("result", "") + " · ");
            JSONArray next = new JSONArray();
            int limit = old.length() - (update ? 1 : 0);
            for (int i = Math.max(0, limit - MAX + 1); i < limit; i++) next.put(old.getJSONObject(i));
            JSONObject record = new JSONObject();
            record.put("time", now);
            record.put("mode", mode);
            record.put("result", result);
            next.put(record);
            p.edit().putString("results", next.toString()).apply();
        } catch (JSONException ignored) { /* No mostrar datos inventados si el historial está dañado. */ }
    }

    static void show(Activity activity) {
        SharedPreferences p = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int mode = p.getInt("mode", 0);
        if (mode < 0 || mode >= MODES.length) mode = 0;
        StringBuilder text = new StringBuilder("Modo elegido: ").append(MODES[mode])
                .append("\nResultados aproximados con el GPS del teléfono; no son datos OBD. Consulta con el coche aparcado.\n\n");
        try {
            JSONArray results = new JSONArray(p.getString("results", "[]"));
            if (results.length() == 0) text.append("Aún no hay pruebas guardadas.");
            DateFormat date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            for (int i = results.length() - 1; i >= 0; i--) {
                JSONObject item = results.getJSONObject(i);
                int m = item.optInt("mode", 0);
                if (m < 0 || m >= MODES.length) m = 0;
                text.append(date.format(new Date(item.optLong("time")))).append(" · ").append(MODES[m]).append("\n")
                        .append(item.optString("result")).append("\n\n");
            }
        } catch (JSONException ex) { text.append("No se ha podido leer el historial."); }
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Pruebas de prestaciones")
                .setMessage(text.toString())
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Elegir modo", (d, w) -> chooseMode(activity))
                .setNegativeButton("Borrar historial", (d, w) -> new MaterialAlertDialogBuilder(activity)
                        .setMessage("¿Borrar todas las pruebas guardadas en este móvil?")
                        .setPositiveButton("Borrar", (d2, w2) -> { p.edit().remove("results").apply(); show(activity); })
                        .setNegativeButton("Cancelar", (d2, w2) -> show(activity)).show())
                .show();
    }

    private static void chooseMode(Activity activity) {
        SharedPreferences p = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int current = p.getInt("mode", 0);
        if (current < 0 || current >= MODES.length) current = 0;
        new MaterialAlertDialogBuilder(activity).setTitle("Modo para la próxima prueba")
                .setSingleChoiceItems(MODES, current, (dialog, which) -> {
                    p.edit().putInt("mode", which).apply();
                    dialog.dismiss();
                    show(activity);
                }).setNegativeButton("Cancelar", (d, w) -> show(activity)).show();
    }
}
