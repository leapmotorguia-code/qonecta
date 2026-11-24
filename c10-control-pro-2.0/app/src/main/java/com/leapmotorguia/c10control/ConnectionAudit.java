package com.leapmotorguia.c10control;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.*;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.view.Display;
import android.hardware.display.DisplayManager;
import android.webkit.WebView;
final class ConnectionAudit {
 static String report(Context c){
  StringBuilder b=new StringBuilder("Diagnóstico de conectividad · solo lectura\nAndroid API "+Build.VERSION.SDK_INT);
  PackageManager pm=c.getPackageManager();
  b.append("\nUSB host: ").append(pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST));
  try{UsbManager m=(UsbManager)c.getSystemService(Context.USB_SERVICE);b.append("\nUSB visibles: ").append(m.getDeviceList().size());for(UsbDevice d:m.getDeviceList().values())b.append("\nVID ").append(d.getVendorId()).append(" PID ").append(d.getProductId()).append(" · permiso ").append(m.hasPermission(d));}catch(Exception e){b.append("\nUSB no comprobado");}
  b.append("\nWi-Fi Direct: ").append(pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT));
  b.append("\nBluetooth: ").append(pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH));
  try{WifiManager w=(WifiManager)c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);b.append("\nWi-Fi activo: ").append(w.isWifiEnabled());}catch(Exception e){b.append("\nWi-Fi no comprobado");}
  b.append("\nUbicación: ").append(c.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED?"concedida":"no concedida");
  b.append("\nPermiso nearby: ").append(Build.VERSION.SDK_INT<31?"no aplicable a Android 11 o anterior":"revisar para futuro host");
  try{DisplayManager dm=(DisplayManager)c.getSystemService(Context.DISPLAY_SERVICE);for(Display d:dm.getDisplays())b.append("\nPantalla ").append(d.getDisplayId()).append(" · ").append(d.getMode().getPhysicalWidth()).append(" × ").append(d.getMode().getPhysicalHeight());}catch(Exception e){b.append("\nPantallas no comprobadas");}
  try{android.content.pm.PackageInfo p=WebView.getCurrentWebViewPackage();b.append("\nWebView: ").append(p==null?"no detectado":p.versionName);}catch(Exception e){b.append("\nWebView no comprobado");}
  for(String p:new String[]{"com.leapmotor.autosdk.server","com.tencent.qqmusiccar","com.headqlink.app","com.google.android.apps.maps","com.spotify.music"})try{pm.getPackageInfo(p,0);b.append("\n").append(p).append(": visible (no prueba acceso)");}catch(Exception e){b.append("\n").append(p).append(": no visible");}
  return b.append("\nNo se abre USB, no se inicia proyección, no se accede al CAN ni se escriben centralitas.").toString();
 }
}
