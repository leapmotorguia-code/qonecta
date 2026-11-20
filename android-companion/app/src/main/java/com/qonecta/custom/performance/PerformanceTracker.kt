package es.c10.complemento.performance

data class AccelerationRunResult(val label: String, val durationSeconds: Double, val valid: Boolean = true)
/** Samples must have monotonic acquisition timestamps, not arrival times. */
class PerformanceTracker {
 private var previousSpeed: Double? = null
 private var previousTime = 0L
 private var launchStart: Long? = null
 private var recoveryStart: Long? = null
 private var stoppedSince: Long? = null
 private var armedFromStop = false
 var last0to100Seconds: Double? = null; private set
 var last80to120Seconds: Double? = null; private set
 fun cancelPending() { previousSpeed=null; launchStart=null; recoveryStart=null; stoppedSince=null; armedFromStop=false }
 fun resetRecords() { cancelPending(); last0to100Seconds=null; last80to120Seconds=null }
 fun onSpeedUpdateKmh(speedKmh: Double, timeNanos: Long): List<AccelerationRunResult> {
  if (!speedKmh.isFinite() || speedKmh<0 || speedKmh>350 || timeNanos<0) { cancelPending(); return emptyList() }
  val p=previousSpeed; val t=previousTime
  if (p!=null && timeNanos<=t) { cancelPending(); return emptyList() }
  if (p!=null && timeNanos-t>2_500_000_000L) cancelPending()
  val old=previousSpeed
  val out=mutableListOf<AccelerationRunResult>()
  if (speedKmh<=0.5) {
   if (stoppedSince==null) stoppedSince=timeNanos
   if (timeNanos-stoppedSince!!>=1_000_000_000L) armedFromStop=true
   launchStart=null
  } else stoppedSince=null
  if (old!=null) {
   fun crossing(v:Double)= t+((timeNanos-t)*(v-old)/(speedKmh-old)).toLong()
   if (armedFromStop && old<3 && speedKmh>=3) { launchStart=crossing(3.0); armedFromStop=false }
   if (old<80 && speedKmh>=80) recoveryStart=crossing(80.0)
   if (launchStart!=null && old<100 && speedKmh>=100) {
    val s=(crossing(100.0)-launchStart!!)/1e9
    if (s in 0.5..60.0) { last0to100Seconds=s; out.add(AccelerationRunResult("3–100 km/h",s)) }
    launchStart=null
   }
   if (recoveryStart!=null && old<120 && speedKmh>=120) {
    val s=(crossing(120.0)-recoveryStart!!)/1e9
    if (s in 0.5..60.0) { last80to120Seconds=s; out.add(AccelerationRunResult("80–120 km/h",s)) }
    recoveryStart=null
   }
  }
  if (speedKmh<75 || recoveryStart?.let { timeNanos-it>60_000_000_000L }==true) recoveryStart=null
  if (launchStart?.let { timeNanos-it>60_000_000_000L }==true) launchStart=null
  previousSpeed=speedKmh; previousTime=timeNanos
  return out
 }
}
