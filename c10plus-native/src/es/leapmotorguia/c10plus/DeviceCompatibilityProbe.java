package es.leapmotorguia.c10plus;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.media.MediaDrm;
import android.os.Build;
import android.webkit.WebView;
import java.util.UUID;

/**
 * Only capability/permission inspection. NO CAN probing, network scanning,
 * pairing, remote calls, opening cameras or driving-control APIs.
 */
public final class DeviceCompatibilityProbe {
    private DeviceCompatibilityProbe() {}
    private static final int PRESENT=VehicleAccessAudit.DEVICE;
    private static final int PENDING=VehicleAccessAudit.UNKNOWN;
    private static final int PERMISSION=VehicleAccessAudit.PERMISSION;
    private static final int INFO=VehicleAccessAudit.INFO;
    private static void add(java.util.List<VehicleAccessAudit.Item> list,String title,int status,String evidence) {
        list.add(new VehicleAccessAudit.Item(title,status,evidence));
    }
    private static String pkgVersion(PackageManager p,String pkg) {
        try{
            PackageInfo i=p.getPackageInfo(pkg,0);
            return i.versionName==null?"Instalada (sin versión)":i.versionName;
        }catch(Throwable e){return null;}
    }
    public static void append(Context c,VehicleAccessAudit.Report r) {
        PackageManager pm=c.getPackageManager();
        // The system WebView is critical to ALL our HTML5 websites.
        try {
            PackageInfo web=Build.VERSION.SDK_INT>=26?WebView.getCurrentWebViewPackage():null;
            add(r.android,"Motor WebView / navegador integrado",
                web!=null?PRESENT:PENDING,
                web==null?"El proveedor WebView no se pudo identificar. Si falta, las webs no funcionarán.":
                "Proveedor: "+web.packageName+" · versión "+web.versionName+". "+
                "Esto confirma el motor, no que Netflix ni Google Maps sean compatibles.");
        }catch(Throwable e){
            add(r.android,"Motor WebView / navegador integrado",PENDING,
                "No se pudo consultar: "+e.getClass().getSimpleName());
        }
        // DRM capability is separate from approval by any individual streaming platform.
        try {
            UUID widevine=UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");
            if(!MediaDrm.isCryptoSchemeSupported(widevine)){
                add(r.android,"DRM Widevine",PENDING,
                    "No hay compatibilidad Widevine anunciada. Netflix/Max/Prime Video pueden no reproducirse.");
            }else{
                MediaDrm drm=new MediaDrm(widevine);
                try{
                    String level=drm.getPropertyString("securityLevel");
                    add(r.android,"DRM Widevine",PRESENT,
                        "Admite Widevine · seguridad "+level+
                        ". No confirma que Netflix, Max o Prime Video autoricen este dispositivo.");
                } finally {drm.release();}
            }
        }catch(Throwable e){
            add(r.android,"DRM Widevine",PENDING,
                "No se pudo comprobar compatibilidad DRM: "+e.getClass().getSimpleName());
        }
        String[][] apps={
            {"Google Maps","com.google.android.apps.maps"},
            {"Spotify","com.spotify.music"},
            {"Netflix","com.netflix.mediaclient"},
            {"Prime Video","com.amazon.avod.thirdpartyclient","com.amazon.avod"},
            {"Max / HBO","com.wbd.stream","com.hbo.hbonow","com.hbo.hbomax"},
            {"Pluto TV","tv.pluto.android","tv.pluto.android.tv"}
        };
        for(String[] app:apps) {
            String version=null,packageName="";
            for(int i=1;i<app.length;i++){
                String found=pkgVersion(pm,app[i]);
                if(found!=null){version=found;packageName=app[i];break;}
            }
            add(r.android,"Aplicación Android: "+app[0],version==null?PENDING:PRESENT,
                version==null?"No detectada por el gestor de paquetes. Puede no estar instalada "+
                    "o la versión usar otro paquete. Un enlace web no equivale a una aplicación.":
                    "Aplicación Android detectada: "+packageName+" ("+version+"). "+
                    "Detectarla NO confirma que pueda ejecutarse o reproducir contenidos.");
        }
        boolean play=pkgVersion(pm,"com.google.android.gms")!=null;
        add(r.android,"Servicios de Google Play",play?PRESENT:PENDING,
            play?"Paquete de Google Play Services presente, compatibilidad pendiente de probar.":
            "No se ha detectado Google Play Services. Algunas aplicaciones Google lo necesitan.");
        // USB: only count devices currently enumerated; don't collect identifiers or secrets.
        try{
            UsbManager usb=(UsbManager)c.getSystemService(Context.USB_SERVICE);
            int count=usb==null?-1:usb.getDeviceList().size();
            add(r.projection,"Dispositivos USB detectados por Android",count>=0?PRESENT:PENDING,
                count<0?"El servicio USB no es accesible.":
                count+" dispositivos visibles al servicio USB. Esto NO identifica "+
                "una sesión Android Auto/CarPlay ni demuestra que USB esté sin conflictos.");
        }catch(Throwable e){
            add(r.projection,"Dispositivos USB detectados por Android",PENDING,
                "No se pudieron enumerar: "+e.getClass().getSimpleName());
        }
        // Check effective platform grants, not merely requested manifest permissions.
        String[][] perms={
            {"Car API: velocidad","android.car.permission.CAR_SPEED"},
            {"Car API: energía","android.car.permission.CAR_ENERGY"},
            {"Car API: información","android.car.permission.CAR_INFO"},
            {"Car API: motor detallado","android.car.permission.CAR_ENGINE_DETAILED"},
            {"Leapmotor MediaService","com.leapmotor.media.MediaService.permission.ACCESS_SERVICE"}
        };
        for(String[] p:perms){
            boolean granted=Build.VERSION.SDK_INT<23 ||
                c.checkSelfPermission(p[1])==PackageManager.PERMISSION_GRANTED;
            add(r.future,p[0],granted?PRESENT:PERMISSION,
                granted?"Permiso Android concedido; faltan API documentada, proveedor y lectura real.":
                "Sin permiso efectivo. La declaración en el APK no concede acceso al coche.");
        }
        boolean carFramework=false;
        try{Class.forName("android.car.Car");carFramework=true;}catch(Throwable ignored){}
        add(r.future,"Framework Android Car",carFramework?PRESENT:PENDING,
            carFramework?"La clase Car existe; disponibilidad de servicios/valores por comprobar.":
                "No se detectó la API android.car.Car. Puede ser un sistema LeapOS no compatible.");
        // Existing cloud adapters are optional and must not be mistaken for live access.
        for(String[] item:new String[][]{
            {"CarCloud (datos por cuenta Leapmotor)","com.headqlink.link.CarCloud"},
            {"CarCloudSession (sesión cloud)","com.headqlink.link.CarCloudSession"},
            {"LeapStatus (batería, TPMS y carga)","com.headqlink.link.LeapStatus"}
        }){
            boolean exists=false;
            try{Class.forName(item[1]);exists=true;}catch(Throwable ignored){}
            add(r.future,item[0],exists?INFO:PENDING,
                exists?"Módulo de Qonecta presente. Requiere una sesión autorizada y datos "+
                    "recientes; este diagnóstico NO abre sesión ni descarga información.":
                "No se ha encontrado este módulo en la APK 0.8 instalada.");
        }
        add(r.future,"Fuente de datos CAN del vehículo",PENDING,
            "El indicador «CAN C10 EN VIVO» de UNIT debe contrastarse con el diagnóstico "+
            "y sus valores reales. No se ha realizado escaneo de buses CAN ni comandos OBD.");
    }
}
