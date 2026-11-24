package com.leapmotorguia.c10control;
public final class Performance {
 public final int low, high; private double last=-1; private long lt=-1, stable=-1; private double start=-1; private boolean armed=false; public Double result;
 public Performance(int low,int high){this.low=low;this.high=high;}
 public void sample(long t,double v){
  if(!Double.isFinite(v)||v<0||t<0)return;
  if(lt>=0 && (t<=lt || t-lt>2500)){if(t<=lt)return;stable=-1;armed=false;start=-1;last=-1;}
  if(result!=null)return;
  if(low==0){if(v<=1){armed=true;start=-1;} if(armed && start<0 && last>=0 && last<=1 && v>1)start=cross(lt,last,t,v,1);}
  else if(start<0){if(Math.abs(v-low)<=1.5){if(stable<0)stable=t;if(t-stable>=3000)armed=true;}else if(v<low-1.5){stable=-1;armed=false;}else if(v>low+1.5){if(armed && last>=0 && last<=low+1.5)start=cross(lt,last,t,v,low+1.5);else{stable=-1;armed=false;}}}
  if(start>=0 && low>0 && v<low-1.5){start=-1;armed=false;stable=-1;}
  if(start>=0 && last>=0 && last<high && v>=high){result=(cross(lt,last,t,v,high)-start)/1000.0;}
  last=v;lt=t;
 }
 private static double cross(long t1,double v1,long t2,double v2,double target){return v2==v1?t2:t1+(t2-t1)*Math.max(0,Math.min(1,(target-v1)/(v2-v1)));}
 public String status(){return result!=null?String.format(java.util.Locale.US,"%.2f s (GPS aproximado)",result):start>=0?"Midiendo":armed?"Preparado":"Esperando velocidad inicial";}
}