package com.leapmotorguia.c10control;

import android.location.Location;
import android.os.SystemClock;

/** Cronometraje experimental únicamente con datos GPS, nunca señales inventadas del CAN. */
final class AccelerationTracker {
    private final VehicleStore store;
    private long launchedAt=-1, stable80At=-1, pullStartedAt=-1, lastTimestamp=-1;
    private boolean seenStationary, armed80;
    private double previousSpeed=-1;
    private String status100="Esperando coche parado (0–4 km/h)";
    private String status120="Mantén 80 ±1,5 km/h durante 3 s";
    AccelerationTracker(VehicleStore p){store=p;}
    void reset(){launchedAt=-1;stable80At=-1;pullStartedAt=-1;lastTimestamp=-1;
        seenStationary=false;armed80=false;previousSpeed=-1;
        status100="Esperando coche parado (0–4 km/h)";
        status120="Mantén 80 ±1,5 km/h durante 3 s";
    }
    void onLocation(Location l){
        if (!l.hasSpeed())return;
        long t=l.getElapsedRealtimeNanos()/1000000L;
        double v=l.getSpeed()*3.6d;
        if(t<=0|| t<=lastTimestamp||t>SystemClock.elapsedRealtime()+2000)return;
        if(lastTimestamp>0&&t-lastTimestamp>5000){reset();}
        lastTimestamp=t;
        if(v<0||v>240)return;
        if (android.os.Build.VERSION.SDK_INT>=26 && l.hasSpeedAccuracy() &&
                l.getSpeedAccuracyMetersPerSecond()*3.6 > 9) {
            status100="GPS con precisión insuficiente para cronometrar";
            status120="GPS con precisión insuficiente para cronometrar";
            previousSpeed=v;return;
        }
        if(v<=4 && launchedAt<0){seenStationary=true;status100="Preparado: salida detectada por GPS";}
        if(seenStationary && launchedAt<0 && previousSpeed>=0 && previousSpeed<=4 && v>4){
            launchedAt=t;status100="Cronometrando 0–100…";
        }
        if (launchedAt>=0 && v>=100){
            double seconds=(t-launchedAt)/1000d;
            if(seconds>0.5&&seconds<120){
                store.setString("accel_0_100",String.format(java.util.Locale.US,"%.2f s",seconds));
                status100="Última marca: "+store.getString("accel_0_100","—")+" · aproximada GPS";
            }
            launchedAt=-1;seenStationary=false;
        }
        if(v>=78.5 && v<=81.5){
            if(stable80At<0)stable80At=t;
            if(t-stable80At>=3000 && pullStartedAt<0){armed80=true;status120="Preparado 80–120: acelera en zona autorizada";}
        } else if(pullStartedAt<0){
            if (armed80 && v>81.5 && v<118){pullStartedAt=t;status120="Cronometrando 80–120…";}
            else {stable80At=-1;armed80=false;status120="Mantén 80 ±1,5 km/h durante 3 s";}
        }
        if(pullStartedAt>=0 && v>=120){
            double seconds=(t-pullStartedAt)/1000d;
            if(seconds>0.3&&seconds<120){
                store.setString("accel_80_120",String.format(java.util.Locale.US,"%.2f s",seconds));
                status120="Última marca: "+store.getString("accel_80_120","—")+" · aproximada GPS";
            }
            pullStartedAt=-1;armed80=false;stable80At=-1;
        }
        if(pullStartedAt>=0 && v<72){pullStartedAt=-1;stable80At=-1;armed80=false;}
        if(launchedAt>=0 && (t-launchedAt>60000 || v<2 && t-launchedAt>1500)){
            launchedAt=-1;seenStationary=true;status100="Salida cancelada; esperando nueva salida";
        }
        previousSpeed=v;
    }
    String s100(){return status100;}
    String s120(){return status120;}
    String best100(){return store.getString("accel_0_100","Sin medición");}
    String best120(){return store.getString("accel_80_120","Sin medición");}
}
