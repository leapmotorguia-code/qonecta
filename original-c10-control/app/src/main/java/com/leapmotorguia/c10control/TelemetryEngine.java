package com.leapmotorguia.c10control;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

/** GPS nativo de Android: no se conecta al bus CAN del vehículo. */
final class TelemetryEngine implements LocationListener {
    interface Changed { void onChanged(); }
    private final Context context;
    private final VehicleStore prefs;
    private final AccelerationTracker accel;
    private final LocationManager manager;
    private final Changed changed;
    private Location latest;
    private boolean running;
    private String state="Sin permiso GPS";
    TelemetryEngine(Context c,Changed ch){context=c;prefs=new VehicleStore(c);accel=new AccelerationTracker(prefs);
        manager=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);changed=ch;}
    boolean permitted(){return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
            ||context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    void start(){
        if(running)return;
        if(!permitted()){state="Permiso de ubicación pendiente"; changed.onChanged();return;}
        if(manager==null){state="No hay LocationManager en este Android";changed.onChanged();return;}
        try{
            boolean any=false;
            if(manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,500,0,this);any=true;
            }
            if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000,0,this);any=true;
            }
            running=any;
            state=any?"Buscando señal GPS…":"Activa la ubicación del sistema";
        }catch(SecurityException e){state="Permiso GPS rechazado";}
        catch(Exception e){state="GPS no disponible: "+e.getClass().getSimpleName();}
        changed.onChanged();
    }
    void stop(){if(manager!=null){try{manager.removeUpdates(this);}catch(Exception ignored){}}running=false;}
    @Override public void onLocationChanged(Location l){
        if(l==null || l.getElapsedRealtimeNanos()<=0)return;
        long age=SystemClock.elapsedRealtime()-l.getElapsedRealtimeNanos()/1000000L;
        if(age<0||age>5000)return;
        if(latest==null||l.getElapsedRealtimeNanos()>=latest.getElapsedRealtimeNanos())latest=l;
        state="Ubicación Android · "+l.getProvider();
        accel.onLocation(l);changed.onChanged();
    }
    @Override public void onProviderDisabled(String provider){state="Proveedor "+provider+" desactivado";changed.onChanged();}
    @Override public void onProviderEnabled(String provider){if(!running)start();}
    @Override public void onStatusChanged(String provider,int status,Bundle extras){}
    String status(){return state;}
    Location fresh(){
        if(latest==null)return null;
        long age=SystemClock.elapsedRealtime()-latest.getElapsedRealtimeNanos()/1000000L;
        return age>=0&&age<4000?latest:null;
    }
    double speedKmh(){Location l=fresh();return l!=null&&l.hasSpeed()?l.getSpeed()*3.6:-1;}
    boolean parked(){double v=speedKmh();return v>=0&&v<2;}
    AccelerationTracker accel(){return accel;}
}
