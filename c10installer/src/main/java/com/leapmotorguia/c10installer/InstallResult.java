package com.leapmotorguia.c10installer;
import android.content.*;
import android.content.pm.PackageInstaller;
public final class InstallResult extends BroadcastReceiver {
    public static String name(int status){
        switch(status){
            case PackageInstaller.STATUS_SUCCESS:return "SUCCESS: instalada";
            case PackageInstaller.STATUS_FAILURE_BLOCKED:return "BLOCKED: instalación bloqueada por sistema, política o verificación";
            case PackageInstaller.STATUS_FAILURE_ABORTED:return "ABORTED: operación cancelada";
            case PackageInstaller.STATUS_FAILURE_INVALID:return "INVALID: paquete no válido";
            case PackageInstaller.STATUS_FAILURE_CONFLICT:return "CONFLICT: conflicto con un paquete existente";
            case PackageInstaller.STATUS_FAILURE_STORAGE:return "STORAGE: problema de almacenamiento";
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:return "INCOMPATIBLE: requisitos incompatibles";
            default:return "Estado Android "+status;
        }
    }
    @Override public void onReceive(Context c,Intent intent){
        int status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE);
        if(status==PackageInstaller.STATUS_PENDING_USER_ACTION){
            c.getSharedPreferences("install",0).edit().putString("report","Android solicita confirmación. Completa o cancela el diálogo del sistema.").apply();
            Intent confirm=intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if(confirm!=null)try{c.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException e){c.getSharedPreferences("install",0).edit().putString("report","No se pudo mostrar la confirmación: "+e.getClass().getSimpleName()).apply();}
            return;
        }
        String raw=intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        String report=name(status)+"\nCódigo: "+status+"\nDetalle devuelto por Android: "+(raw==null?"no informado":raw)+"\n\nEste resultado pertenece a este dispositivo. Instalar en Samsung no demuestra que LeapOS lo permita.";
        c.getSharedPreferences("install",0).edit().putString("report",report).apply();
    }
}
