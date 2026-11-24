package com.leapmotorguia.c10control;
import java.util.Locale;
public final class TripMeter {
 private boolean running; private long last=-1,validMs; private double lastSpeed,km,peak; private int samples;
 public void start(){running=true;last=-1;} public void pause(){running=false;last=-1;} public void gap(){last=-1;}
 public void reset(){pause();validMs=0;km=0;peak=0;samples=0;}
 public void sample(long t,double speed){if(!running||!Double.isFinite(speed)||speed<0||speed>300||t<0)return;if(last>=0&&t<=last)return;if(last>=0&&t-last<=3000){long d=t-last;validMs+=d;double a=lastSpeed<1?0:lastSpeed,b=speed<1?0:speed;km+=(a+b)*0.5*d/3600000.0;}last=t;lastSpeed=speed;peak=Math.max(peak,speed);samples++;}
 public String summary(){return String.format(Locale.US,"%s\nDistancia GPS: %.2f km\nTiempo con datos: %.1f min\nVelocidad media GPS: %s\nVelocidad máxima GPS: %s\nMuestras: %d",running?"Registrando":"En pausa",km,validMs/60000.0,validMs>0?String.format(Locale.US,"%.1f km/h",km*3600000/validMs):"—",samples>0?String.format(Locale.US,"%.1f km/h",peak):"—",samples);}
}