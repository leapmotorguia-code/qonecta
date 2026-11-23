package es.leapmotorguia.c10plus;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import java.security.MessageDigest;
import java.util.Locale;

/** Local, read-only diagnostics. Never logs VIN, credentials or location. */
public final class StartupDiagnostics {
    private StartupDiagnostics(){}
    private static String hex(byte[] input){
        StringBuilder s=new StringBuilder();
        for(byte b:input)s.append(String.format(Locale.ROOT,"%02x",b&255));
        return s.toString();
    }
    private static String classStatus(String className) {
        try{
            Class.forName(className);
            return "Presente";
        }catch(Throwable x){return "Ausente o incompatible ("+x.getClass().getSimpleName()+")";}
    }
    private static String fieldStatus(String cls,String name) {
        try{
            Class.forName(cls).getDeclaredField(name);
            return "Presente";
        }catch(Throwable x){return "No disponible ("+x.getClass().getSimpleName()+")";}
    }
    public static String report(Context ctx,String lastBridgeError) {
        StringBuilder out=new StringBuilder("UNIT C10 Plus 4.4 · Diagnóstico de arranque\n");
        out.append("Solo lectura; sin acceso CAN, cámaras ni controles del coche.\n\n");
        try{
            PackageInfo info=ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),
                PackageManager.GET_SIGNING_CERTIFICATES);
            out.append("Paquete: ").append(info.packageName).append('\n');
            out.append("Versión: ").append(info.versionName).append(" (")
                .append(info.getLongVersionCode()).append(")\n");
            if(info.signingInfo!=null){
                Signature[] signers=info.signingInfo.getApkContentsSigners();
                if(signers!=null && signers.length==1){
                    String fp=hex(MessageDigest.getInstance("SHA-256")
                        .digest(signers[0].toByteArray()));
                    out.append("Huella de firma SHA-256: ").append(fp).append('\n');
                }
            }
        }catch(Throwable e){out.append("Identidad de APK: ").append(e.getClass().getSimpleName()).append('\n');}
        out.append("Android API: ").append(Build.VERSION.SDK_INT).append('\n');
        android.content.SharedPreferences lifecycle=
            ctx.getSharedPreferences("c10plus_lifecycle",Context.MODE_PRIVATE);
        java.text.SimpleDateFormat clock=new java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",Locale.getDefault());
        for(String action:new String[]{"last_resume","last_pause","last_stop"}) {
            long at=lifecycle.getLong(action,0);
            out.append("Actividad UNIT ").append(action).append(": ")
                .append(at>0?clock.format(new java.util.Date(at)):"Sin registro")
                .append('\n');
        }
        out.append("Diálogo de UNIT activo durante última pausa: ")
            .append(lifecycle.getBoolean("unit_dialog_showing",false))
            .append('\n');
        out.append("Un cambio de foco NO prueba que Android Auto/CarPlay se conectaron; "+
            "solo ayuda a encontrar el momento de la sustitución de pantalla.\n");

        String nativeHub="com.headqlink.unit.extended.ExtendedHubView";
        out.append("Vista original UNIT: ").append(classStatus(nativeHub)).append('\n');
        out.append("Contenedor original: ").append(fieldStatus(nativeHub,"bodyContainer")).append('\n');
        out.append("Barra de UNIT: ").append(fieldStatus(nativeHub,"tabButtons")).append('\n');
        out.append("Pestaña activa: ").append(fieldStatus(nativeHub,"activeTab")).append('\n');
        out.append("Módulo visual 4.4: ").append(classStatus(
            "es.leapmotorguia.c10plus.C10NativeHubActivity")).append('\n');
        out.append("Lector energía UNIT: ").append(classStatus(
            "es.c10.unitplus.integrated.NativeSnapshot")).append('\n');
        out.append("Historial de viajes: ").append(classStatus(
            "es.c10.unitplus.integrated.TripStore")).append('\n');
        out.append("Actualizador 4.4: ").append(classStatus(
            "es.leapmotorguia.c10plus.InAppUpdater")).append('\n');
        out.append("Último error de integración: ")
            .append(lastBridgeError==null || lastBridgeError.trim().isEmpty()?
                "No registrado":lastBridgeError.replaceAll("[\\r\\n]+"," ")).append('\n');
        out.append("\nCarPlay y Android Auto no quedan verificados por detectar las clases Android.\n");
        return out.toString();
    }
}