package es.leapmotorguia.c10plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.hardware.usb.*;
import android.os.*;
import android.provider.Settings;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.Locale;

/** One explicit launch coordinator. Receiver protocols remain in the original engines.
 * No USB claim, no privileged setting writes, no background activity launches. */
final class C10Connect {
    private static final String AA="com.andrerinas.openheadunit.main.MainActivity";
    private static final String CP="com.shilapi.xcertplay.LeapCarPlayActivity";
    private static final String PREF="c10plus_connect";
    private final Context app;
    private long lastLaunch=-1;
    private String pending="";
    C10Connect(Context c){app=c.getApplicationContext();}
    static String engine(String name){
        if(name.equals(AA)||name.equals("com.andrerinas.openheadunit.aap.AapProjectionActivity"))return "Android Auto";
        if(name.equals(CP)||name.equals("com.shilapi.xcertplay.CarPlayHostActivity"))return "CarPlay";
        return "";
    }
    private SharedPreferences prefs(){return app.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    void observed(Activity a){
        String e=engine(a.getClass().getName());if(e.isEmpty())return;
        prefs().edit().putString("observed",e).putLong("observedAt",System.currentTimeMillis()).apply();
        if(!pending.isEmpty()&&!pending.equals(e))prefs().edit().putString("event","Otro receptor abrió su interfaz: "+e).apply();
    }
    void returned(){pending="";lastLaunch=-1;}
    private void record(String event){prefs().edit().putString("event",event).putLong("eventAt",System.currentTimeMillis()).apply();}
    private void launch(Activity a,String e){
        if(a.isFinishing()||a.isDestroyed())return;
        String current=engine(a.getClass().getName());
        if(!current.isEmpty()&&!current.equals(e)){
            new AlertDialog.Builder(a).setTitle("Cambiar receptor").setMessage("Cierra primero la sesión anterior desde su interfaz. C10 Connect no fuerza el cierre de la conexión activa.").setPositiveButton("Aceptar",null).show();return;
        }
        long now=SystemClock.elapsedRealtime();
        if(lastLaunch>=0&&now-lastLaunch<8000){Toast.makeText(a,"Ya hay un intento de apertura. Espera o vuelve al inicio.",Toast.LENGTH_SHORT).show();return;}
        pending=e;lastLaunch=now;
        try{
            Intent i=new Intent();i.setClassName(a.getPackageName(),e.equals("Android Auto")?AA:CP);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            a.startActivity(i);
            prefs().edit().putString("preferred",e).apply();
            record("Apertura solicitada: "+e+"; conexión no confirmada");
        }catch(Exception err){pending="";lastLaunch=-1;record("Error de apertura: "+err.getClass().getSimpleName());Toast.makeText(a,"Receptor no disponible",Toast.LENGTH_LONG).show();}
    }
    private void settings(Activity a,String action){
        try{a.startActivity(new Intent(action));record("Ajustes abiertos: "+action);}catch(Exception e){record("Ajustes no disponibles: "+action);Toast.makeText(a,"Ajustes no disponibles en este equipo",Toast.LENGTH_LONG).show();}
    }
    String report(){
        SharedPreferences p=prefs();StringBuilder s=new StringBuilder("C10 Connect 5.8 · diagnóstico\n");
        s.append("Preferencia: ").append(p.getString("preferred","Sin selección")).append('\n');
        s.append("Última interfaz observada: ").append(p.getString("observed","Ninguna")).append(" (no confirma conexión)\n");
        s.append("Último evento: ").append(p.getString("event","Sin intento")).append('\n');
        try{
            UsbManager u=(UsbManager)app.getSystemService(Context.USB_SERVICE);
            if(u!=null){s.append("USB visibles: ").append(u.getDeviceList().size()).append('\n');
                for(UsbDevice d:u.getDeviceList().values())s.append(String.format(Locale.ROOT,"VID %04X PID %04X · interfaces %d · autorización %s\n",d.getVendorId(),d.getProductId(),d.getInterfaceCount(),u.hasPermission(d)?"sí":"no"));
                UsbAccessory[] ar=u.getAccessoryList();s.append("Accesorios USB: ").append(ar==null?0:ar.length).append('\n');}
        }catch(Exception e){s.append("USB: lectura no disponible\n");}
        try{android.bluetooth.BluetoothManager b=(android.bluetooth.BluetoothManager)app.getSystemService(Context.BLUETOOTH_SERVICE);
            android.bluetooth.BluetoothAdapter ad=b==null?null:b.getAdapter();s.append("Bluetooth: ").append(ad==null?"No visible":ad.isEnabled()?"Activado":"Desactivado").append('\n');
        }catch(Exception e){s.append("Bluetooth: estado no autorizado\n");}
        try{android.net.wifi.WifiManager w=(android.net.wifi.WifiManager)app.getSystemService(Context.WIFI_SERVICE);s.append("Wi-Fi: ").append(w==null?"No visible":w.isWifiEnabled()?"Activado":"Desactivado").append('\n');}catch(Exception e){s.append("Wi-Fi: estado no disponible\n");}
        return s.toString();
    }
    private Button button(Activity a,String text,Runnable r){
        Button b=new Button(a);b.setAllCaps(false);b.setText(text);b.setTextColor(0xffeaf7ff);b.setTextSize(16);
        GradientDrawable g=new GradientDrawable();g.setColor(0xff123b45);g.setCornerRadius(16);g.setStroke(1,0xff27d9b9);b.setBackground(g);b.setOnClickListener(v->r.run());return b;
    }
    void show(Activity a,Runnable home){
        if(a.isFinishing()||a.isDestroyed())return;
        LinearLayout page=new LinearLayout(a);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(20,14,20,14);page.setBackgroundColor(0xff081b29);
        TextView title=new TextView(a);title.setText("C10 CONNECT · TELÉFONO");title.setTextSize(24);title.setTextColor(0xff27e3c1);page.addView(title);
        TextView info=new TextView(a);info.setTextColor(0xffd0e7ef);info.setText("Elige un receptor. USB o inalámbrico se configura en su motor. C10 Connect coordina nuestras aperturas, no reemplaza todavía los protocolos.");page.addView(info);
        AlertDialog dialog=new AlertDialog.Builder(a).setView(page).setNegativeButton("Cerrar",null).create();
        page.addView(button(a,"Android Auto · receptor",()->{dialog.dismiss();launch(a,"Android Auto");}));
        page.addView(button(a,"Apple CarPlay · receptor",()->{dialog.dismiss();launch(a,"CarPlay");}));
        LinearLayout wireless=new LinearLayout(a);
        wireless.addView(button(a,"Bluetooth",()->{dialog.dismiss();settings(a,Settings.ACTION_BLUETOOTH_SETTINGS);}),new LinearLayout.LayoutParams(0,-2,1));
        wireless.addView(button(a,"Wi-Fi",()->{dialog.dismiss();settings(a,Settings.ACTION_WIFI_SETTINGS);}),new LinearLayout.LayoutParams(0,-2,1));page.addView(wireless);
        TextView data=new TextView(a);data.setTextColor(0xffc1dce7);data.setTextSize(13);data.setText(report());
        ScrollView sc=new ScrollView(a);sc.addView(data);page.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        page.addView(button(a,"Actualizar diagnóstico",()->data.setText(report())));
        page.addView(button(a,"Copiar diagnóstico",()->{android.content.ClipboardManager c=(android.content.ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);if(c!=null)c.setPrimaryClip(ClipData.newPlainText("C10 Connect",report()));}));
        page.addView(button(a,"Volver a nuestro cockpit",()->{dialog.dismiss();home.run();}));
        dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-1);
    }
}
