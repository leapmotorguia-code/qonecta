package es.c10.controlpro;
/** Pure calculations: all timestamps are monotonic seconds. No vehicle writes. */
public final class Measurements {
 public static double cost(double per100,double unitPrice){check(per100);check(unitPrice);return per100*unitPrice;}
 public static double regenerated(double kw,double seconds){check(kw);check(seconds);return kw*seconds/3600.0;}
 private static void check(double v){if(!Double.isFinite(v)||v<0)throw new IllegalArgumentException("Valor no válido");}
 public static final class Timer {
  private final double low,high; private double previousTime=Double.NaN,previousSpeed,start=Double.NaN;
  public Timer(double low,double high){if(low<3||high<=low)throw new IllegalArgumentException();this.low=low;this.high=high;}
  public void reset(){previousTime=Double.NaN;start=Double.NaN;}
  public double update(double t,double speed,double accuracy,boolean hasSpeed){
   if(!Double.isFinite(t)||!Double.isFinite(speed)||speed<0||!Double.isFinite(accuracy)||accuracy<0||accuracy>20||!hasSpeed){reset();return Double.NaN;}
   if(!Double.isNaN(previousTime)&&(t<=previousTime||t-previousTime>2.5)){reset();}
   double result=Double.NaN;
   if(!Double.isNaN(previousTime)){
    if(speed<low)start=Double.NaN;
    if(Double.isNaN(start)&&previousSpeed<low&&speed>=low&&speed>previousSpeed)start=cross(previousTime,previousSpeed,t,speed,low);
    if(!Double.isNaN(start)&&previousSpeed<high&&speed>=high&&speed>previousSpeed){result=cross(previousTime,previousSpeed,t,speed,high)-start;start=Double.NaN;}
   }
   previousTime=t;previousSpeed=speed;return result;
  }
  public boolean started(){return !Double.isNaN(start);}
  private double cross(double t0,double s0,double t1,double s1,double target){return t0+(target-s0)*(t1-t0)/(s1-s0);}
 }
}
