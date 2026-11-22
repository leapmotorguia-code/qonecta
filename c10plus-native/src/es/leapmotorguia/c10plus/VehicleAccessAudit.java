package es.leapmotorguia.c10plus;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Read-only capability inventory. Never pairs, opens car cameras, sends CAN/
 * OBD commands, changes car settings, contacts Leapmotor cloud or probes IPs.
 * It separates a *vehicle-sourced live sample* from Android device hardware.
 */
public final class VehicleAccessAudit {
    public static final int LIVE = 0;
    public static final int DEVICE = 1;
    public static final int PERMISSION = 2;
    public static final int UNKNOWN = 3;
    public static final int INFO = 4;

    public static final class Item {
        public final String title;
        public final int state;
        public final String detail;
        public Item(String title,int state,String detail) {
            this.title=title;this.state=state;this.detail=detail;
        }
        public String status() {
            switch(state) {
                case LIVE:return "LECTURA REAL";
                case DEVICE:return "DISPONIBLE EN ANDROID";
                case PERMISSION:return "REQUIERE PERMISO";
                case INFO:return "COMPROBACIÓN MANUAL";
                default:return "ACCESO NO CONFIRMADO";
            }
        }
    }
    public static final class Report {
        public final long checkedAt;
        public final List<Item> vehicle=new ArrayList<>();
        public final List<Item> android=new ArrayList<>();
        public final List<Item> future=new ArrayList<>();
        public final List<Item> projection=new ArrayList<>();
        public Report(long time){checkedAt=time;}
        private static void append(StringBuilder b,String group,List<Item> rows) {
            b.append("\n").append(group).append("\n");
            for(Item e:rows) {
                b.append("• ").append(e.title).append(" [").append(e.status()).append("]\n");
                b.append("  ").append(e.detail).append("\n");
            }
        }
        public String asText() {
            StringBuilder b=new StringBuilder();
            b.append("UNIT C10 Plus · Auditoría de accesos (solo lectura)\n");
            b.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault())
                .format(new Date(checkedAt)));
            b.append("\nLos sensores/cámaras Android NO implican acceso a sensores/cámaras del coche.\n");
            append(b,"1. DATOS DE VEHÍCULO",vehicle);
            append(b,"2. CAPACIDADES DEL DISPOSITIVO ANDROID",android);
            append(b,"3. CONEXIONES Y PROYECCIÓN",projection);
            append(b,"4. ACCESOS QUE REQUIEREN INTEGRACIÓN",future);
            b.append("\nSin escaneo activo de la red, sin control del coche y sin modificación de LeapOS.");
            return b.toString();
        }
    }
    private VehicleAccessAudit(){}

    private static double num(Object d,String name) {
        try {
            Object v=d.getClass().getField(name).get(d);
            if(v instanceof Number) {
                double n=((Number)v).doubleValue();
                return Double.isFinite(n)?n:Double.NaN;
            }
        } catch(Throwable ignored){}
        return Double.NaN;
    }

    /** Entries added only after the original UNIT reports credible, usable data. */
    private static void inspectProfile(Context ctx,Report r) {
        String configured=CompatibilityTestLog.configuredProfile(ctx);
        VehicleProfileHints.Hint hint=VehicleProfileHints.interpret(configured);
        r.vehicle.add(new Item("Perfil seleccionado en UNIT",INFO,
            "Etiqueta: "+configured+"; familia orientativa: "+hint.model+
            "; tipo: "+hint.friendlyDrivetrain()+". Esto no identifica automáticamente el coche."));
        r.vehicle.add(new Item("Alcance de compatibilidad",INFO,
            hint.guidance()+" La disponibilidad depende de la instalación en el sistema del coche y las señales autorizadas."));
    }

    private static void inspectSnapshot(Report r) {
        boolean good=false;
        String evidence="No se ha recibido una muestra válida de UNIT.";
        Object snapshot=null;
        try {
            Class<?> c=Class.forName("es.c10.unitplus.integrated.NativeSnapshot");
            snapshot=c.getMethod("read").invoke(null);
            good=Boolean.TRUE.equals(snapshot.getClass().getMethod("usable").invoke(snapshot))
                && snapshot.getClass().getField("authoritative").getBoolean(snapshot);
            if(good) {
                Object src=snapshot.getClass().getField("source").get(snapshot);
                evidence="Última lectura validada por UNIT · fuente: "+String.valueOf(src);
            } else evidence="El lector existe, pero no informa de una muestra autorizada y reciente.";
        } catch(Throwable x){
            evidence="Lectura no accesible desde esta versión de UNIT ("+
                x.getClass().getSimpleName()+").";
        }
        r.vehicle.add(new Item("Enlace de datos del coche",good?LIVE:UNKNOWN,evidence));
        String[] names={"Batería (SoC)","Potencia eléctrica","Velocidad","Combustible restante"};
        String[] fields={"soc","power","speed","fuelLitres"};
        String[] units={"%","kW","km/h","L"};
        for(int i=0;i<names.length;i++){
            double n=good?num(snapshot,fields[i]):Double.NaN;
            if(i==0 && Double.isFinite(n) && (n<0 || n>100))n=Double.NaN;
            int st=Double.isFinite(n)?LIVE:UNKNOWN;
            String detail=Double.isFinite(n)?
                String.format(Locale.getDefault(),"Dato real disponible: %.1f %s.",n,units[i]):
                "Sin dato real verificado en UNIT.";
            if(i==3)detail+=" Es combustible restante, NO litros consumidos.";
            r.vehicle.add(new Item(names[i],st,detail));
        }
    }

    private static void inspectTrips(Context ctx,Report r) {
        try {
            Class<?> cls=Class.forName("es.c10.unitplus.integrated.TripStore");
            Object store=cls.getMethod("get",Context.class).invoke(null,ctx);
            Object session=cls.getField("current").get(store);
            Class<?> fields=session.getClass();
            boolean measured=fields.getField("measured").getBoolean(session);
            boolean old=fields.getField("legacyEnergy").getBoolean(session);
            boolean e=fields.getField("hasEnergy").getBoolean(session);
            boolean regen=fields.getField("hasRecovered").getBoolean(session);
            boolean valid=measured&&!old&&e&&regen
                && Double.isFinite(num(session,"kwh"))
                && Double.isFinite(num(session,"recovered"));
            r.vehicle.add(new Item("Contadores energía de viaje",valid?LIVE:UNKNOWN,
                valid?"Gasto y entrada eléctrica disponibles en el acumulador real de UNIT.":
                "UNIT tiene un registro de viajes, pero falta una medición completa y fiable."));
        }catch(Throwable x){
            r.vehicle.add(new Item("Contadores energía de viaje",UNKNOWN,
                "No se pudo leer la sesión ("+x.getClass().getSimpleName()+")."));
        }
    }

    private static boolean allowed(Context c,String permission) {
        return Build.VERSION.SDK_INT<23 ||
            c.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED;
    }
    private static boolean declared(Context c,String name){
        try {
            String[] p=c.getPackageManager().getPackageInfo(
                c.getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions;
            if(p!=null)for(String a:p)if(name.equals(a))return true;
        }catch(Throwable ignored){}
        return false;
    }

    private static void inspectAndroid(Context c,Report r) {
        try{
            SensorManager m=(SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> sensors=m==null?new ArrayList<Sensor>():m.getSensorList(Sensor.TYPE_ALL);
            int n=sensors.size();
            StringBuilder examples=new StringBuilder();
            for(int i=0;i<Math.min(4,n);i++){
                if(i>0)examples.append(", ");
                examples.append(sensors.get(i).getName());
            }
            r.android.add(new Item("Sensores de este Android",n>0?DEVICE:UNKNOWN,
                n+" sensores del dispositivo Android detectados"+
                (n>0?" (ej.: "+examples+").":".")+
                " No son necesariamente sensores del Leapmotor."));
        }catch(Throwable x){
            r.android.add(new Item("Sensores de este Android",UNKNOWN,"Consulta bloqueada: "+
                x.getClass().getSimpleName()));
        }

        try{
            CameraManager m=(CameraManager)c.getSystemService(Context.CAMERA_SERVICE);
            String[] ids=m==null?new String[0]:m.getCameraIdList();
            r.android.add(new Item("Cámaras expuestas por Android",ids.length>0?DEVICE:UNKNOWN,
                ids.length+" identificadores de cámara disponibles a nivel Android. "+
                "NO demuestra acceso a cámaras 360º o de aparcamiento del C10."));
        }catch(Throwable x){
            r.android.add(new Item("Cámaras expuestas por Android",UNKNOWN,
                "El servicio de cámara no permite enumerarlas ("+x.getClass().getSimpleName()+")."));
        }

        boolean locationPerm=allowed(c,Manifest.permission.ACCESS_FINE_LOCATION)
            ||allowed(c,Manifest.permission.ACCESS_COARSE_LOCATION);
        try{
            LocationManager m=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
            boolean on=m!=null && (
                m.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                m.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
            int state=!locationPerm?PERMISSION:on?DEVICE:UNKNOWN;
            r.android.add(new Item("GPS / ubicación Android",state,
                !locationPerm?"Permiso de ubicación pendiente.":
                on?"Proveedor de ubicación habilitado; no se ha leído ni enviado tu posición.":
                "El proveedor de ubicación está deshabilitado o no es accesible."));
        }catch(Throwable x){
            r.android.add(new Item("GPS / ubicación Android",UNKNOWN,
                "No se pudo comprobar el servicio ("+x.getClass().getSimpleName()+")."));
        }
        try{
            BluetoothManager m=(BluetoothManager)c.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter=m==null?null:m.getAdapter();
            boolean grant=Build.VERSION.SDK_INT<31 ||
                allowed(c,"android.permission.BLUETOOTH_CONNECT");
            int state=!grant?PERMISSION:adapter!=null?DEVICE:UNKNOWN;
            String desc=!grant?"Permiso de dispositivos cercanos pendiente.":
                adapter!=null?"Hardware Bluetooth detectado; no verifica conexión al C10.":
                "No se detecta adaptador Bluetooth.";
            r.android.add(new Item("Bluetooth Android",state,desc));
        }catch(Throwable x){
            r.android.add(new Item("Bluetooth Android",UNKNOWN,"Consulta no autorizada: "+
                x.getClass().getSimpleName()));
        }
        try{
            WifiManager m=(WifiManager)c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            boolean hasWifi=m!=null;
            r.android.add(new Item("Wi-Fi / hotspot del dispositivo",
                hasWifi?DEVICE:UNKNOWN,hasWifi?
                "Servicio Wi-Fi del dispositivo disponible; no confirma enlace local con el coche.":
                "Servicio Wi-Fi no accesible."));
        }catch(Throwable x){
            r.android.add(new Item("Wi-Fi / hotspot del dispositivo",UNKNOWN,
                "Consulta no disponible: "+x.getClass().getSimpleName()));
        }
        if(Build.VERSION.SDK_INT>=33 && !allowed(c,"android.permission.NEARBY_WIFI_DEVICES")){
            r.android.add(new Item("Permiso Wi-Fi cercano",
                declared(c,"android.permission.NEARBY_WIFI_DEVICES")?PERMISSION:UNKNOWN,
                declared(c,"android.permission.NEARBY_WIFI_DEVICES")?
                "Declara el permiso, pero aún no está concedido.":
                "La APK no declara permiso Wi-Fi cercano."));
        }
        r.android.add(new Item("Sistema operativo de UNIT",INFO,
            "Android API "+Build.VERSION.SDK_INT+". Tipo de hardware y compatibilidad de proyección pendientes de prueba física."));
        r.android.add(new Item("Acceso a cámaras del C10",UNKNOWN,
            "Enumerar cámaras Android NO habilita cámaras del vehículo. "+
            "Se requiere interfaz documentada y autorización específica."));
    }
    private static void inspectConnectivity(Report r){
        r.projection.add(new Item("Pruebas de conectividad registradas",INFO,
            "El informe de pruebas manuales se incluye al pulsar «Copiar informe». No equivale a detección automática."));
        r.projection.add(new Item("Android Auto",INFO,
            "Conecta Android al C10 y verifica vídeo, audio, tacto, reconexión y pantalla dividida. "+
            "La presencia de servicios Android no acredita por sí sola una sesión proyectada."));
        r.projection.add(new Item("Apple CarPlay",INFO,
            "Esta APK Android no implementa ni detecta una sesión CarPlay. "+
            "Para el iPhone hace falta el puente/servicio CarPlay compatible y una prueba real."));
        r.projection.add(new Item("Qonecta Companion",UNKNOWN,
            "No se confirma una sesión con el complemento del móvil solo por mostrar rutas o botones."));
    }
    private static void inspectPending(Report r){
        r.future.add(new Item("Bus CAN / OBD2",UNKNOWN,
            "No se ha verificado una interfaz autorizada de lectura CAN/OBD ni PID compatibles. "+
            "Bluetooth o USB detectado no equivalen a acceso a la centralita."));
        r.future.add(new Item("Cámaras 360º / delanteras / traseras",UNKNOWN,
            "No hay API de cámara del C10 verificada."));
        r.future.add(new Item("Batería: celdas, temperatura, carga DC",UNKNOWN,
            "No se ha comprobado telemetría de celdas, temperatura, corriente o cargador."));
        r.future.add(new Item("Climatizador, puertas, ventanas y techo",UNKNOWN,
            "No existe un canal de control remoto autenticado y validado desde esta APK."));
        r.future.add(new Item("ADAS, radar y ultrasonidos",UNKNOWN,
            "Sin acceso demostrado a datos de sensores de asistencia a la conducción."));
    }
    public static Report scan(Context c) {
        Report result=new Report(System.currentTimeMillis());
        inspectProfile(c,result);
        inspectSnapshot(result);
        inspectTrips(c,result);
        inspectAndroid(c,result);
        inspectConnectivity(result);
        inspectPending(result);
        return result;
    }
}
