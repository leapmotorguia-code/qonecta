package com.qonecta.custom.sentry
import kotlin.math.sqrt
data class SentryEvent(val timestampMillis:Long,val deltaG:Float)
class VehicleSentryMonitor {
 var isArmed=false;private set
 private var previous:FloatArray?=null
 private var lastEvent:Long?=null
 fun arm(){isArmed=true;previous=null;lastEvent=null}
 fun disarm(){isArmed=false;previous=null}
 fun evaluate(timeMillis:Long,x:Float,y:Float,z:Float):SentryEvent? {
  if (!isArmed || !x.isFinite() || !y.isFinite() || !z.isFinite()) return null
  val p=previous;previous=floatArrayOf(x,y,z)
  if(p==null)return null
  val delta=sqrt((x-p[0])*(x-p[0])+(y-p[1])*(y-p[1])+(z-p[2])*(z-p[2]))
  if(delta<0.35f || lastEvent?.let{timeMillis-it<5000}==true)return null
  lastEvent=timeMillis;return SentryEvent(timeMillis,delta)
 }
}
