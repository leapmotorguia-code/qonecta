package es.c10.controlpro;
import org.junit.Test;import static org.junit.Assert.*;
public class MeasurementsTest {
 @Test public void costsAndEnergy(){assertEquals(3.0,Measurements.cost(15,.2),.00001);assertEquals(.25,Measurements.regenerated(30,30),.00001);}
 @Test public void invalidNumbers(){for(double d:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})try{Measurements.cost(d,1);fail();}catch(IllegalArgumentException expected){}}
 @Test public void interpolation(){Measurements.Timer t=new Measurements.Timer(3,100);t.update(0,0,5,true);t.update(1,10,5,true);for(int i=2;i<10;i++)t.update(i,i*10,5,true);assertEquals(9.7,t.update(10,100,5,true),.00001);}
 @Test public void recoveryRequiresCrossing(){Measurements.Timer t=new Measurements.Timer(80,120);assertTrue(Double.isNaN(t.update(1,90,5,true)));assertTrue(Double.isNaN(t.update(2,125,5,true)));t.reset();t.update(0,75,5,true);t.update(1,85,5,true);t.update(2,105,5,true);assertEquals(2.25,t.update(3,125,5,true),.00001);}
 @Test public void gapsAccuracyAndSpeedReset(){Measurements.Timer t=new Measurements.Timer(3,100);t.update(0,0,5,true);t.update(1,10,5,true);assertTrue(t.started());assertTrue(Double.isNaN(t.update(5,100,5,true)));assertFalse(t.started());t.update(6,0,5,true);t.update(7,10,5,true);t.update(8,60,50,true);assertFalse(t.started());t.update(9,0,5,true);t.update(10,10,5,true);t.update(11,50,5,false);assertFalse(t.started());}
}
