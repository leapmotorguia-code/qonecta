package com.leapmotorguia.c10nativa;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Foreground-only paired ELM327 Bluetooth classic. Diagnostics only, no actuation or DTC erase. */
public final class ObdActivity extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private volatile BluetoothSocket socket;
    private Thread worker;
    private TextView status,values;
    private Spinner devices;
    private final List<BluetoothDevice> paired=new ArrayList<>();
    private volatile int generation;
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xffedf7f5);t.setPadding(12,10,12,10);return t;}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff0c1721);LinearLayout col=new LinearLayout(this);col.setOrientation(1);col.setPadding(24,18,24,24);scroll.addView(col);setContentView(scroll);ScreenInsets.apply(scroll);
        col.addView(text("OBD · lecturas estándar",26));
        col.addView(text("Necesitas un adaptador Bluetooth clásico ELM327 emparejado desde Ajustes de Android. No requiere internet. No es compatible con adaptadores BLE, Wi-Fi ni USB en esta prueba.",17));
        col.addView(text("Solo consulta RPM, velocidad, temperatura del refrigerante y carga calculada, si la ECU los admite. ATRV es tensión del adaptador/12 V. No mide batería de tracción ni energía regenerada y no cambia ajustes del coche.",17));
        devices=new Spinner(this);col.addView(devices);Button refresh=new Button(this);refresh.setText("Actualizar adaptadores emparejados");col.addView(refresh);refresh.setOnClickListener(v->refresh());
        Button start=new Button(this);start.setText("Conectar OBD");col.addView(start);start.setOnClickListener(v->connect());Button stop=new Button(this);stop.setText("Desconectar OBD");col.addView(stop);stop.setOnClickListener(v->{disconnect();status.setText("Desconectado. No hay datos en directo.");values.setText("Sin lecturas actuales.");});
        status=text("Sin conectar",19);values=text("Sin lecturas actuales.",21);col.addView(status);col.addView(values);refresh();
    }
    private BluetoothAdapter adapter(){BluetoothManager m=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);return m==null?null:m.getAdapter();}
    private boolean permission(){return Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private void refresh(){
        if(!permission()){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},61);return;}
        paired.clear();List<String> labels=new ArrayList<>();
        try{BluetoothAdapter a=adapter();if(a!=null&&a.isEnabled())for(BluetoothDevice d:a.getBondedDevices()){paired.add(d);String n=d.getName();labels.add(n==null?"Adaptador sin nombre":n);}}
        catch(SecurityException e){status.setText("Android no permite consultar Bluetooth.");}
        if(labels.isEmpty())labels.add("No hay adaptadores emparejados");devices.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
    }
    @Override public void onRequestPermissionsResult(int code,String[] p,int[] result){super.onRequestPermissionsResult(code,p,result);if(code==61&&permission())refresh();}
    private void connect(){
        if(running)return;if(!permission()){refresh();return;}if(paired.isEmpty()){status.setText("Empareja primero un adaptador ELM327.");return;}
        final BluetoothDevice device=paired.get(devices.getSelectedItemPosition());running=true;final int token=++generation;
        status.setText("Conectando…");values.setText("Sin lecturas actuales.");
        main.postDelayed(()->{if(running&&generation==token&&status.getText().toString().equals("Conectando…")){disconnect();status.setText("Tiempo de conexión agotado. Comprueba adaptador y emparejamiento.");}},12000);
        worker=new Thread(()->{
            BluetoothSocket connection=null;
            try{
                connection=device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                if(generation!=token||!running){connection.close();return;}
                socket=connection;connection.connect();
                if(!running||generation!=token)return;
                // AT commands configure the adapter; only Mode 01 PIDs are sent to the ECU.
                for(String c:new String[]{"ATZ","ATE0","ATL0","ATS0","ATH0","ATSP0"})command(connection,c);
                while(running&&generation==token){
                    Double rpm=ObdValues.decode("0C",command(connection,"010C"));Double speed=ObdValues.decode("0D",command(connection,"010D"));
                    Double temp=ObdValues.decode("05",command(connection,"0105"));Double load=ObdValues.decode("04",command(connection,"0104"));Double volts=ObdValues.adapterVolts(command(connection,"ATRV"));
                    String v="RPM: "+format(rpm,"rpm")+"\nVelocidad OBD: "+format(speed,"km/h")+"\nRefrigerante: "+format(temp,"°C")+"\nCarga calculada ECU: "+format(load,"%")+"\nTensión adaptador: "+format(volts,"V")+"\n\nMotor según respuesta RPM: "+(rpm==null?"no disponible":rpm>0?"RPM superiores a cero":"respuesta de 0 rpm")+"\nNo es una medida de potencia ni energía recuperada.";
                    main.post(()->{if(running&&generation==token){status.setText("Respuesta recibida · "+java.text.DateFormat.getTimeInstance().format(new Date()));values.setText(v);}});Thread.sleep(1500);
                }
            }catch(Exception e){main.post(()->{if(generation==token){status.setText("Conexión terminada: "+e.getClass().getSimpleName()+". No hay datos en directo.");values.setText("Sin lecturas actuales.");}});}
            finally{try{if(connection!=null)connection.close();}catch(IOException ignored){}if(generation==token){running=false;socket=null;}}
        },"c10-obd-read");worker.start();
    }
    private String format(Double v,String unit){return v==null?"No disponible":String.format(Locale.getDefault(),"%.1f %s",v,unit);}
    private String command(BluetoothSocket s,String query)throws Exception{
        if(!running||s==null)throw new IOException();InputStream in=s.getInputStream();
        while(in.available()>0)in.read();s.getOutputStream().write((query+"\r").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        StringBuilder out=new StringBuilder();long deadline=SystemClock.elapsedRealtime()+7000;
        while(running&&SystemClock.elapsedRealtime()<deadline){if(in.available()>0){int c=in.read();if(c<0)throw new EOFException();if(c=='>')return out.toString();out.append((char)c);if(out.length()>8192)throw new IOException("response too long");}else Thread.sleep(20);}
        throw new IOException("adapter timeout");
    }
    private void disconnect(){running=false;generation++;BluetoothSocket s=socket;socket=null;try{if(s!=null)s.close();}catch(IOException ignored){}if(worker!=null)worker.interrupt();worker=null;}
    @Override protected void onPause(){disconnect();if(status!=null){status.setText("Desconectado al salir de la pantalla.");values.setText("Sin lecturas actuales.");}super.onPause();}
}
