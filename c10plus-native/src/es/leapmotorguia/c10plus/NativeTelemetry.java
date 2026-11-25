package es.leapmotorguia.c10plus;
import java.lang.reflect.Method;

/** Read only the already present UNIT snapshot. No new vehicle commands or privileged calls. */
final class NativeTelemetry {
    double evRange=Double.NaN, fuelPercent=Double.NaN, fuelRange=Double.NaN, exterior=Double.NaN, cabin=Double.NaN, avg=Double.NaN, instant=Double.NaN;
    double nativeDistance=Double.NaN,nativeEnergy=Double.NaN,nativeIncoming=Double.NaN;
    boolean available;
    static double bounded(double n,double lo,double hi){return Double.isFinite(n)&&n>=lo&&n<=hi?n:Double.NaN;}
    static Object call(Object o,String name)throws Exception{return o.getClass().getMethod(name).invoke(o);}
    static double num(Object o,String name){try{return ((Number)call(o,name)).doubleValue();}catch(Throwable e){return Double.NaN;}}
    static double field(Object o,String n){try{return ((Number)o.getClass().getField(n).get(o)).doubleValue();}catch(Throwable e){return Double.NaN;}}
    static NativeTelemetry read(Object point){
        NativeTelemetry t=new NativeTelemetry();
        try{
            if(!Boolean.TRUE.equals(call(point,"usable"))||!point.getClass().getField("connected").getBoolean(point)||point.getClass().getField("demo").getBoolean(point))return t;
            long age=point.getClass().getField("ageMs").getLong(point);
            if(age<0||age>5000)return t;
            t.nativeDistance=bounded(field(point,"nativeDistance"),0,100000);
            t.nativeEnergy=bounded(field(point,"nativeEnergy"),0,100000);
            t.nativeIncoming=bounded(field(point,"nativeRecovered"),0,100000);
            // Zero/default values cannot be distinguished from unavailable without independent evidence.
            if(!(t.nativeEnergy>0||t.nativeIncoming>0)){t.nativeEnergy=t.nativeIncoming=Double.NaN;}
            if(!(t.nativeDistance>0))t.nativeDistance=Double.NaN;
            t.fuelPercent=bounded(field(point,"fuelPct"),0.01,100);
            Class<?> cls=Class.forName("com.headqlink.unit.extended.LeapVehicleDataSource");
            Object source=cls.getField("INSTANCE").get(null);
            Method snapshot=cls.getMethod("snapshot",long.class,boolean.class);
            Object s=snapshot.invoke(source,System.currentTimeMillis(),false);
            if(!Boolean.TRUE.equals(call(s,"getConnected"))||Boolean.TRUE.equals(call(s,"isDemo")))return t;
            long now=System.currentTimeMillis(), last=((Number)call(s,"getLastUpdateMs")).longValue();
            if(last<=0||now-last<0||now-last>5000)return t;
            t.available=true;
            t.evRange=bounded(num(s,"getEvRangeKm"),0.01,1500);
            t.fuelRange=bounded(num(s,"getFuelRangeKm"),0.01,2000);
            t.exterior=bounded(num(s,"getTempExteriorC"),-50,65);
            t.cabin=bounded(num(s,"getTempLeftC"),15,35);
            t.avg=bounded(num(s,"getAvgConsumptionKwh100"),0.01,100);
            t.instant=bounded(num(s,"getInstantConsumptionKwh100"),-100,150);
            // UNIT may estimate these values or apply local overrides; never present them as verified ECU measurements.
        }catch(Throwable ignored){}
        return t;
    }
}
