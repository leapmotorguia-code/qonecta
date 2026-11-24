package com.leapmotorguia.c10control;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.os.Build;

import java.util.List;
import java.util.Locale;

/** Inventario de APIs Android disponibles sin tocar centralitas ni cámaras del vehículo. */
final class DeviceAudit {
    private DeviceAudit(){}
    static String report(Context c){
        StringBuilder b=new StringBuilder();
        b.append("C10 CONTROL PRO · INFORME DE ACCESOS\n");
        b.append("Marca Android: ").append(Build.MANUFACTURER).append("\n");
        b.append("Modelo Android: ").append(Build.MODEL).append("\n");
        b.append("SDK: ").append(Build.VERSION.SDK_INT).append("\n");
        b.append("Paquete: ").append(c.getPackageName()).append("\n");
        b.append("Fecha: ").append(new java.util.Date()).append("\n\n");
        boolean gps=c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
        boolean coarse=c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
        b.append("Ubicación precisa: ").append(gps?"AUTORIZADA":"NO AUTORIZADA").append("\n");
        b.append("Ubicación aproximada: ").append(coarse?"AUTORIZADA":"NO AUTORIZADA").append("\n");
        LocationManager lm=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
        if(lm!=null){
            try{b.append("GPS del dispositivo activo: ").append(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)?"SÍ":"NO").append("\n");}
            catch(Exception e){b.append("GPS: consulta denegada\n");}
        }
        PackageManager pm=c.getPackageManager();
        b.append("Función Bluetooth: ").append(pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)?"PRESENTE":"NO DETECTADA").append("\n");
        b.append("USB Host: ").append(pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)?"PRESENTE":"NO DETECTADO").append("\n");
        try {
            UsbManager usb=(UsbManager)c.getSystemService(Context.USB_SERVICE);
            java.util.Map<String,UsbDevice> devices=usb==null?null:usb.getDeviceList();
            b.append("USB visibles: ").append(devices==null?"No disponible":devices.size()).append("\n");
            if(devices!=null)for(UsbDevice d:devices.values()){
                b.append(String.format(Locale.US,"  %s VID %04X / PID %04X · permiso %s\n",
                        d.getDeviceName(),d.getVendorId(),d.getProductId(),usb.hasPermission(d)?"SÍ":"NO"));
            }
        }catch(Exception e){b.append("USB: no accesible ("+e.getClass().getSimpleName()+")\n");}
        try{
            SensorManager sm=(SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> sensors=sm==null?null:sm.getSensorList(Sensor.TYPE_ALL);
            b.append("Sensores Android: ").append(sensors==null?0:sensors.size()).append("\n");
            if(sensors!=null)for(Sensor s:sensors){b.append("  ").append(s.getName()).append(" (tipo ").append(s.getType()).append(")\n");}
        }catch(Exception e){b.append("Sensores Android: sin acceso\n");}
        try{
            CameraManager cameras=(CameraManager)c.getSystemService(Context.CAMERA_SERVICE);
            String[] ids=cameras==null?null:cameras.getCameraIdList();
            b.append("IDs de Camera2 Android: ").append(ids==null?"No disponible":java.util.Arrays.toString(ids)).append("\n");
        }catch(Exception e){b.append("Cámaras Camera2: sin acceso\n");}
        for(String[] it:new String[][]{
                {"Google Maps","com.google.android.apps.maps"},
                {"Spotify","com.spotify.music"},
                {"Netflix","com.netflix.mediaclient"},
                {"Max","com.wbd.stream"},
                {"Prime Video","com.amazon.avod.thirdpartyclient"},
                {"UNIT de referencia","com.tencent.qqmusiccar"}}){
            b.append(it[0]).append(": ").append(pm.getLaunchIntentForPackage(it[1])!=null?"INSTALADA Y LANZABLE":"NO LANZABLE").append("\n");
        }
        b.append("\nLIMITACIONES CONFIRMADAS DE ESTA APP:\n");
        b.append("Bus CAN / ECU / BMS: SIN INTERFAZ PROPIETARIA CONECTADA\n");
        b.append("Cámaras de aparcamiento: NO INTEGRADAS (Camera2 no equivale a cámaras 360º)\n");
        b.append("Datos de regeneración y generador REEV: NO LEÍDOS DEL COCHE\n");
        b.append("CarPlay y Android Auto nativos: NO IMPLEMENTADOS\n");
        b.append("Ninguna acción de este informe altera la configuración del vehículo.\n");
        return b.toString();
    }
}
