package es.c10.complemento.efficiency
/** Battery charging is not classified as braking regeneration. Generator is a separate channel. */
data class TripEnergySummary(val dischargedKwh:Double,val chargedKwh:Double,val generatorKwh:Double,val netBatteryKwh:Double)
class TripEnergyBalanceTracker {
 private var prevTime:Long?=null
 private var prevBattery=0.0; private var prevGenerator=0.0
 private var discharged=0.0; private var charged=0.0; private var generated=0.0
 fun onTelemetryTick(batteryPowerKw:Double,generatorPowerKw:Double,timeNanos:Long):TripEnergySummary {
  if (!batteryPowerKw.isFinite() || !generatorPowerKw.isFinite() || generatorPowerKw<0 || timeNanos<0) { prevTime=null; return snapshot() }
  val t=prevTime
  if (t!=null && timeNanos>t && timeNanos-t<=5_000_000_000L) {
   val h=(timeNanos-t)/3.6e12
   if (prevBattery*batteryPowerKw<0) {
    val f=kotlin.math.abs(prevBattery)/(kotlin.math.abs(prevBattery)+kotlin.math.abs(batteryPowerKw))
    add(prevBattery*h*f/2);add(batteryPowerKw*h*(1-f)/2)
   } else add((prevBattery+batteryPowerKw)*h/2)
   generated+=(prevGenerator+generatorPowerKw)*h/2
  }
  prevTime=timeNanos;prevBattery=batteryPowerKw;prevGenerator=generatorPowerKw
  return snapshot()
 }
 private fun add(kwh:Double) { if(kwh>=0) discharged+=kwh else charged-=kwh }
 fun snapshot()=TripEnergySummary(discharged,charged,generated,discharged-charged)
 fun resetTrip() {prevTime=null; discharged=0.0;charged=0.0;generated=0.0}
}
