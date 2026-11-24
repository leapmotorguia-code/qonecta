package com.leapmotorguia.c10control;
import android.content.*;
import android.os.*;
import java.util.*;
/** Passive reception only; no requests, heartbeats, CAN writes or permissions bypass. */
final class VehicleSignals {
 private static final String ACTION="leapmotor.cantoapp.update";
 private final Context context; private boolean registered; private String error=""; private long count,last;
 private final TreeMap<String,String> data=new TreeMap<>();
 VehicleSignals(Context c){context=c;}
 private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
  if(!ACTION.equals(i.getAction()))return;count++;last=SystemClock.elapsedRealtime();
  try{Bundle b=i.getExtras();if(b==null)return;for(String key:b.keySet()){
    if(!key.matches("[0-9]{4,12}")||data.size()>=250&&!data.containsKey(key))continue;
    Object o=b.get(key);if(o instanceof Number||o instanceof Boolean){String v=String.valueOf(o);if(v.length()<=32)data.put(key,v);}
  }}catch(RuntimeException e){error="Mensaje no interpretable";}
 }};
 void start(){if(registered)return;try{IntentFilter f=new IntentFilter(ACTION);if(Build.VERSION.SDK_INT>=33)context.registerReceiver(receiver,f,Context.RECEIVER_EXPORTED);else context.registerReceiver(receiver,f);registered=true;error="";}catch(Exception e){error=e.getClass().getSimpleName();}}
 void stop(){if(registered){try{context.unregisterReceiver(receiver);}catch(Exception ignored){}registered=false;}}
 void reset(){count=0;last=0;data.clear();}
 String report(){StringBuilder s=new StringBuilder("Observación LeapOS (solo lectura, no autenticada)\nEstado: ").append(registered?"Escuchando":"En pausa").append("\nMensajes recibidos: ").append(count).append("\nIDs numéricos observados: ").append(data.size());
 if(last>0)s.append("\nÚltimo mensaje hace ").append((SystemClock.elapsedRealtime()-last)/1000).append(" s");if(!error.isEmpty())s.append("\nError: ").append(error);
 if(count==0)s.append("\nTodavía no se ha recibido ningún mensaje; no demuestra que CAN sea inaccesible.");
 for(Map.Entry<String,String> e:data.entrySet())s.append("\n").append(e.getKey()).append(" = ").append(e.getValue());
 return s.toString();}
}