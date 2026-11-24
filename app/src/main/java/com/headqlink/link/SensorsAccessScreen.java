package com.headqlink.link;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.media.MediaDrm;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Auditoría NO invasiva del headunit y de la telemetría disponible.
 *
 * Nunca lee directamente el bus CAN, no utiliza comandos ECU, no abre cámaras
 * del vehículo ni solicita permisos privilegiados. La presencia de una
 * Camera2 ID significa "cámara Android", NO "cámara de aparcamiento".
 * Los datos de LeapStatus proceden únicamente de CarCloud / Leapmotor Mate.
 */
final class SensorsAccessScreen implements CarScreen {
    private static final long TICK_MS = 10_000L;
    private static final long CLOUD_FRESH_MS = 180_000L;
    private static final int BG_CARD = 0xFF132A38;
    private static final int BG_ROW = 0xFF112633;
    private static final int COLOR_GREEN = 0xFF64DFBC;
    private static final int COLOR_ORANGE = 0xFFF0C579;
    private static final int COLOR_MISSING = 0xFF92A7AF;
    private static final int COLOR_TEXT = 0xFFEAF7F8;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Entry> entries = new ArrayList<>();
    private Context context;
    private LinearLayout body;
    private ScrollView scroll;
    private boolean active;
    private String lastOrigin = "Ninguna fuente de vehículo conectada";
    private String lastAge = "Sin datos";

    private final Runnable refreshJob = new Runnable() {
        @Override public void run() {
            if (!active) return;
            refresh();
            handler.postDelayed(this, TICK_MS);
        }
    };

    static final class Entry {
        final String category;
        final String name;
        final String value;
        final String origin;
        final boolean available;
        Entry(String category, String name, String value, String origin, boolean available) {
            this.category = category; this.name = name; this.value = value;
            this.origin = origin; this.available = available;
        }
    }

    @Override public View create(Host host) {
        context = host.context();
        scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CarKit.BG);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(25, 20, 25, 32);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("SENSORES Y ACCESOS DEL C10", 34, COLOR_TEXT, true);
        content.addView(title);
        TextView subtitle = text("Auditoría en solo lectura · qué recibe realmente esta pantalla", 21, COLOR_MISSING, false);
        subtitle.setPadding(0, 4, 0, 14);
        content.addView(subtitle);
        TextView warning = text(
                "No se muestran datos inventados. La nube de Leapmotor puede enviar valores con retraso. " +
                "Detectar un sensor de Android no significa acceder al bus CAN ni a las cámaras 360º.", 20,
                COLOR_ORANGE, false);
        warning.setPadding(16, 12, 16, 12);
        warning.setBackground(round(0xFF3D3328, 18));
        content.addView(warning);

        addVehicleSelector(content);

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, 15, 0, 15);
        TextView update = pill("↻ ACTUALIZAR");
        update.setOnClickListener(v -> refresh());
        actions.addView(update, new LinearLayout.LayoutParams(0, 66, 1f));
        TextView export = pill("COPIAR INFORME");
        LinearLayout.LayoutParams exportLp = new LinearLayout.LayoutParams(0, 66, 1f);
        exportLp.leftMargin = 12;
        actions.addView(export, exportLp);
        export.setOnClickListener(v -> copyReport());
        content.addView(actions);

        body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        content.addView(body);
        active = true;
        refresh();
        handler.postDelayed(refreshJob, TICK_MS);
        return scroll;
    }

    /** Perfil de cálculo: NO modifica la configuración ni centralitas del coche. */
    private void addVehicleSelector(LinearLayout root) {
        TextView title = text("MODELO PARA ESTIMACIONES Y CONSUMOS", 24, COLOR_TEXT, true);
        title.setPadding(0, 18, 0, 5);
        root.addView(title);
        final TextView chosen = text("", 21, COLOR_GREEN, true);
        root.addView(chosen);
        final String[] names = {"C10 REEV", "C10 Life", "C10 Pro Max", "T03"};
        final String[] ids = {CarCloudStore.PROFILE_C10_REEV, CarCloudStore.PROFILE_C10_LIFE,
                CarCloudStore.PROFILE_C10_PROMAX, CarCloudStore.PROFILE_T03};
        final CarCloudStore prefs = new CarCloudStore(context);
        updateChosenProfile(chosen, prefs.profile());

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 10);
        for (int i = 0; i < names.length; i++) {
            final String id = ids[i];
            final String label = names[i];
            TextView button = pill(label);
            button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 19);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 66, 1f);
            if (i != 0) lp.leftMargin = 7;
            row.addView(button, lp);
            button.setOnClickListener(v -> {
                prefs.setProfile(id, 0);
                updateChosenProfile(chosen, id);
                Toast.makeText(context, "Perfil " + label + " guardado", Toast.LENGTH_SHORT).show();
                refresh();
            });
        }
        root.addView(row);
        TextView note = text("El perfil cambia la capacidad de batería y el modelo físico de los cálculos. " +
                "No cambia la configuración del coche ni verifica que los datos de fábrica sean correctos.",
                18, COLOR_MISSING, false);
        root.addView(note);
    }

    private void updateChosenProfile(TextView label, String profile) {
        String n = CarCloudStore.PROFILE_C10_REEV.equals(profile) ? "C10 REEV" :
                CarCloudStore.PROFILE_C10_PROMAX.equals(profile) ? "C10 Pro Max" :
                CarCloudStore.PROFILE_T03.equals(profile) ? "T03" :
                CarCloudStore.PROFILE_C10_LIFE.equals(profile) ? "C10 Life" :
                "Sin seleccionar (C10 Life por defecto)";
        label.setText("Perfil actual: " + n);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(context);
        t.setText(value);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private TextView pill(String title) {
        TextView t = text(title, 22, 0xFF071D27, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(0xFF49CBB9, 15));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    private static GradientDrawable round(int color, int radius) {
        GradientDrawable b = new GradientDrawable();
        b.setColor(color);
        b.setCornerRadius(radius);
        b.setStroke(1, 0xFF345563);
        return b;
    }

    private static String number(double value, String unit, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "—";
        return String.format(Locale.getDefault(), "%." + decimals + "f %s", value, unit);
    }

    private void put(String category, String name, String value, String origin, boolean available) {
        entries.add(new Entry(category, name, value, origin, available));
    }

    private static boolean has(Double value) {
        return value != null && !value.isNaN() && !value.isInfinite();
    }

    private void vehicle() {
        CarCloud.Snapshot snap;
        try { snap = CarCloud.car(); }
        catch (RuntimeException problem) {
            put("DATOS DEL COCHE", "Fuente Leapmotor", "No se pudo consultar",
                    problem.getClass().getSimpleName(), false);
            return;
        }
        if (snap == null || !snap.hasData() || snap.demo) {
            lastOrigin = "Nube Leapmotor / Mate no conectados";
            lastAge = snap != null && snap.demo ? "Demostración descartada" : "No recibidos";
            put("DATOS DEL COCHE", "Conexión al vehículo", "Sin lecturas confirmadas",
                    lastAge, false);
            addUnknownVehicle();
            return;
        }

        long age = snap.ageMs(System.currentTimeMillis());
        boolean recent = age >= 0 && age <= CLOUD_FRESH_MS;
        lastAge = age < 60_000 ? "Hace " + (age / 1000) + " s"
                : "Hace " + (age / 60_000) + " min";
        lastOrigin = "CarCloud / Leapmotor Mate (no es un OBD interno)";
        String source = lastOrigin + " · " + lastAge + (recent ? "" : " · lectura antigua");
        LeapStatus s = snap.status;

        put("DATOS DEL COCHE", "Estado de conexión", snap.state.name(),
                source, recent);
        putCloud("Batería de tracción", number(s.socBest(), "%", 1), source, recent, s.socBest());
        putCloud("Autonomía eléctrica", number(s.rangeKm, "km", 0), source, recent, s.rangeKm);
        putCloud("Tensión batería", number(s.batteryVoltage, "V", 1), source, recent, s.batteryVoltage);
        putCloud("Corriente batería", number(s.batteryCurrent, "A", 1), source, recent, s.batteryCurrent);
        putCloud("Temperatura mínima de batería", number(s.minBatteryTempC, "°C", 1), source, recent, s.minBatteryTempC);
        putCloud("Tiempo restante de carga", number(s.chargeRemainMin, "min", 0), source, recent, s.chargeRemainMin);
        put("DATOS DEL COCHE", "Carga CA", flag(s.acCharge), source, recent && s.acCharge != null);
        put("DATOS DEL COCHE", "Carga CC", flag(s.dcCharge), source, recent && s.dcCharge != null);
        putCloud("Cuentakilómetros", number(s.odometerKm, "km", 0), source, recent, s.odometerKm);
        putCloud("Velocidad comunicada", number(s.speedKmh, "km/h", 0), source, recent, s.speedKmh);
        putCloud("Temperatura interior", number(s.interiorTempC, "°C", 1), source, recent, s.interiorTempC);
        putCloud("Combustible REEV", number(s.fuelPct, "%", 1), source, recent, s.fuelPct);
        putCloud("Litros en depósito", number(s.fuelLiters, "L", 1), source, recent, s.fuelLiters);
        putCloud("Autonomía gasolina", number(s.fuelRangeKm, "km", 0), source, recent, s.fuelRangeKm);
        putCloud("Autonomía combinada", number(s.combinedRangeKm, "km", 0), source, recent, s.combinedRangeKm);
        put("DATOS DEL COCHE", "Cerraduras", flag(s.locked), source,
                recent && s.locked != null);
        String[] tyres = {"delantera izquierda", "delantera derecha", "trasera izquierda", "trasera derecha"};
        for (int i = 0; i < 4; i++) {
            double bar = s.tyreBar(i);
            putCloud("Neumático " + tyres[i], number(bar, "bar", 2), source, recent, bar);
            Boolean door = s.doorOpen(i);
            put("DATOS DEL COCHE", "Puerta " + tyres[i], flag(door), source,
                    recent && door != null);
        }
        put("DATOS DEL COCHE", "Maletero", flag(s.bootOpen), source,
                recent && s.bootOpen != null);
    }

    private void putCloud(String name, String value, String origin, boolean fresh, double raw) {
        put("DATOS DEL COCHE", name, Double.isNaN(raw) || Double.isInfinite(raw) ? "—" : value,
                origin, fresh && !Double.isNaN(raw) && !Double.isInfinite(raw));
    }

    private static String flag(Boolean value) {
        return value == null ? "—" : value ? "Sí" : "No";
    }

    private void addUnknownVehicle() {
        for (String name : new String[]{"Batería de tracción", "Autonomía", "Potencia / regeneración",
                "Nivel de combustible", "Presiones neumáticos", "Puertas", "Velocidad ECU",
                "Carga CA / CC"}) {
            put("DATOS DEL COCHE", name, "—", "Sin datos reales recibidos", false);
        }
    }

    private void androidCapabilities() {
        Context c = context;
        put("SISTEMA ANDROID", "Dispositivo", Build.MANUFACTURER + " " + Build.MODEL,
                "Información del sistema", true);
        put("SISTEMA ANDROID", "Versión Android", Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT,
                "Información del sistema", true);
        try {
            SensorManager manager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
            int count = manager == null ? 0 : manager.getSensorList(-1).size();
            put("SISTEMA ANDROID", "Sensores Android", count + " detectados",
                    "SensorManager; no son sensores del C10", count > 0);
        } catch (Exception ignored) {
            put("SISTEMA ANDROID", "Sensores Android", "No accesibles", "SensorManager", false);
        }
        try {
            UsbManager manager = (UsbManager) c.getSystemService(Context.USB_SERVICE);
            int count = manager == null ? 0 : manager.getDeviceList().size();
            put("SISTEMA ANDROID", "USB", count + " dispositivos visibles",
                    "UsbManager; no indica si Android Auto puede proyectarse", count > 0);
            if (manager != null) for (UsbDevice device : manager.getDeviceList().values()) {
                put("SISTEMA ANDROID", "USB · " + device.getDeviceName(),
                        String.format(Locale.US, "VID %04X / PID %04X", device.getVendorId(),
                                device.getProductId()),
                        "Inventario Android USB; sin abrir dispositivo", true);
            }
        } catch (Exception ignored) {
            put("SISTEMA ANDROID", "USB", "No accesible", "UsbManager", false);
        }
        try {
            CameraManager manager = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
            int count = manager == null ? 0 : manager.getCameraIdList().length;
            put("SISTEMA ANDROID", "Cámaras Camera2", count + " identificadores",
                    "Cámara Android; NO confirma acceso a cámaras de aparcamiento", count > 0);
        } catch (Exception ignored) {
            put("SISTEMA ANDROID", "Cámaras Camera2", "Sin inventario", "CameraManager", false);
        }

        if (Build.VERSION.SDK_INT >= 31) {
            boolean bt = c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
            put("SISTEMA ANDROID", "Permiso Bluetooth / dispositivos próximos",
                    bt ? "Concedido" : "Sin conceder", "BLUETOOTH_CONNECT", bt);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            boolean wifi = c.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED;
            put("SISTEMA ANDROID", "Permiso Wi-Fi próximo",
                    wifi ? "Concedido" : "Sin conceder",
                    "NEARBY_WIFI_DEVICES; no garantiza hotspot local", wifi);
        }
        boolean gps = c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        put("SISTEMA ANDROID", "Permiso ubicación precisa", gps ? "Concedido" : "Sin conceder",
                "Permisos de esta aplicación", gps);
        try {
            UUID widevine = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");
            if (MediaDrm.isCryptoSchemeSupported(widevine)) {
                MediaDrm drm = new MediaDrm(widevine);
                try {
                    String level = drm.getPropertyString("securityLevel");
                    put("SISTEMA ANDROID", "Widevine", level, "DRM Android, no prueba reproducción de Netflix/Max/Prime", true);
                } finally { drm.release(); }
            } else {
                put("SISTEMA ANDROID", "Widevine", "Sin soporte", "MediaDrm", false);
            }
        } catch (Exception ignored) {
            put("SISTEMA ANDROID", "Widevine", "No verificable", "MediaDrm", false);
        }

        for (String[] app : new String[][]{
                {"UNIT / Qonecta", "com.tencent.qqmusiccar"},
                {"Google Maps", "com.google.android.apps.maps"},
                {"Spotify", "com.spotify.music"},
                {"Netflix", "com.netflix.mediaclient"},
                {"Prime Video", "com.amazon.avod.thirdpartyclient"},
                {"Max / HBO", "com.wbd.stream"},
                {"AutoKit", "com.autokit"},
                {"Zlink", "com.zjinnova.zlink"}}) {
            try {
                boolean visible = c.getPackageManager().getLaunchIntentForPackage(app[1]) != null;
                put("APLICACIONES", app[0], visible ? "Visible / lanzable" : "No visible",
                        "PackageManager; visibilidad limitada por Android", visible);
            } catch (Exception ignored) {
                put("APLICACIONES", app[0], "No comprobable", "PackageManager", false);
            }
        }
    }

    private void unknownAccess() {
        put("ACCESOS PENDIENTES", "Bus CAN interno", "No confirmado", "Sin API autorizada", false);
        put("ACCESOS PENDIENTES", "OBD2 integrado", "No confirmado", "No se consulta la ECU sin interfaz", false);
        put("ACCESOS PENDIENTES", "Cámaras 360º del coche", "No confirmado", "Requiere API del fabricante", false);
        put("ACCESOS PENDIENTES", "Radares y ADAS", "No confirmado", "Requiere API del fabricante", false);
        put("ACCESOS PENDIENTES", "Potencia gastada / recuperada real", "No confirmada",
                "El modelo físico es estimación, no contador BMS", false);
        put("ACCESOS PENDIENTES", "Mandos remotos", "No probados", "No se envían comandos", false);
        put("ACCESOS PENDIENTES", "Receptor CarPlay", "Sin validación en el C10",
                "No deducible de un permiso Android", false);
    }

    private void refresh() {
        if (!active || body == null) return;
        entries.clear();
        vehicle();
        androidCapabilities();
        unknownAccess();
        int savedScroll = scroll == null ? 0 : scroll.getScrollY();
        body.removeAllViews();
        String category = "";
        int available = 0;
        for (Entry entry : entries) if (entry.available) available++;
        TextView count = text(
                "Accesos o valores detectados: " + available + " de " + entries.size()
                        + " · " + lastAge, 22, COLOR_GREEN, true);
        count.setPadding(0, 7, 0, 14);
        body.addView(count);
        for (Entry entry : entries) {
            if (!entry.category.equals(category)) {
                category = entry.category;
                TextView heading = text(category, 25, COLOR_GREEN, true);
                heading.setPadding(3, 24, 0, 12);
                body.addView(heading);
            }
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(16, 14, 16, 12);
            row.setBackground(round(BG_ROW, 15));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 8;
            body.addView(row, lp);
            TextView name = text(entry.name, 21, COLOR_TEXT, true);
            row.addView(name);
            int tint = entry.available ? COLOR_GREEN : COLOR_ORANGE;
            TextView value = text(entry.value, 23, tint, true);
            value.setPadding(0, 4, 0, 1);
            row.addView(value);
            TextView source = text(entry.origin, 17, COLOR_MISSING, false);
            row.addView(source);
        }
        if (scroll != null) scroll.post(() -> {
            if (active && scroll != null) scroll.scrollTo(0, savedScroll);
        });
    }

    private void copyReport() {
        try {
            JSONArray data = new JSONArray();
            for (Entry e : entries) {
                JSONObject j = new JSONObject();
                j.put("categoria", e.category);
                j.put("parametro", e.name);
                j.put("valor", e.value);
                j.put("origen", e.origin);
                j.put("datoReciente", e.available);
                data.put(j);
            }
            JSONObject report = new JSONObject();
            report.put("app", "UNIT / C10 Control · Auditoría de sensores");
            report.put("fecha", System.currentTimeMillis());
            report.put("fuenteVehiculo", lastOrigin);
            report.put("antiguedad", lastAge);
            report.put("lecturas", data);
            ClipboardManager clip = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clip == null) throw new IllegalStateException("Portapapeles ausente");
            clip.setPrimaryClip(ClipData.newPlainText("C10 sensores solo lectura", report.toString(2)));
            Toast.makeText(context, "Informe copiado; comprueba qué valores tienen origen real", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "No se pudo copiar el informe", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void destroy() {
        active = false;
        handler.removeCallbacks(refreshJob);
        entries.clear();
        body = null;
        scroll = null;
        context = null;
    }
}
