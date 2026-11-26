package es.drivecore.cockpit

import android.content.Context
import android.os.SystemClock
import java.io.Closeable
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import kotlin.math.abs

/** AAOS public CarPropertyManager, accessed reflectively because android.car is optional.
 * No hidden OEM interfaces, no vehicle writes. All reads on a worker thread. */
data class Reading<T>(val value: T?, val ageMs: Long?, val reason: String = "")
data class CarTelemetry(
    val speedKmh: Double? = null, val odometerKm: Double? = null,
    val batteryPct: Double? = null, val batteryKwh: Double? = null,
    val dischargeKw: Double? = null, val incomingKw: Double? = null,
    val fuelLitres: Double? = null, val rangeCombinedKm: Double? = null,
    val rangeEvKm: Double? = null, val rangeFuelKm: Double? = null,
    val gearPark: Boolean? = null, val brake: Boolean? = null,
    val sampledAtMs: Long = 0L, val diagnostics: Map<String,String> = emptyMap(),
    val powerAtNs: Long? = null, val gearAtNs: Long? = null, val brakeAtNs: Long? = null, val speedAtNs: Long? = null
)
interface TelemetryProvider : Closeable { fun latest(): CarTelemetry }
class AaosTelemetryProvider(context: Context) : TelemetryProvider {
    private val ctx = context.applicationContext
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val closeOnce = java.util.concurrent.atomic.AtomicBoolean(false)
    private val energy = EnergyAccumulator()
    @Volatile var totals = EnergyTotals()
        private set
    fun resetEnergy() { if (!closed) runCatching { scope.launch { if (!closed) { energy.reset(); totals = energy.totals() } } } }
    private var car: Any? = null
    @Volatile private var data = CarTelemetry()
    @Volatile private var closed = false
    private val polling = scope.launch { while (isActive && !closed) { poll(); delay(1000) } }
    override fun latest() = data
    private fun poll() {
        val diagnostics = linkedMapOf<String,String>()
        val timestamps = mutableMapOf<String, Long>()
        try {
            val clazz = Class.forName("android.car.Car")
            if (car == null) car = clazz.getMethod("createCar", Context::class.java).invoke(null, ctx)
            val c = car ?: error("Car no disponible")
            val pm = clazz.getMethod("getCarManager", String::class.java).invoke(c, "property") ?: error("CarPropertyManager no disponible")
            val ids = Class.forName("android.car.VehiclePropertyIds")
            fun read(name: String, freshMs: Long? = null): Any? {
                return try {
                    val id = ids.getField(name).getInt(null)
                    val p = pm.javaClass.getMethod("getProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).invoke(pm,id,0)
                        ?: error("Sin valor")
                    val status = (p.javaClass.getMethod("getStatus").invoke(p) as Number).toInt()
                    if (status != 0) error("Estado no disponible: $status")
                    val ts = (p.javaClass.getMethod("getTimestamp").invoke(p) as Number).toLong()
                    val age = (SystemClock.elapsedRealtimeNanos() - ts) / 1_000_000
                    if (ts <= 0 || age < 0) error("Fecha inválida")
                    timestamps[name] = ts
                    if (freshMs != null && (age < 0 || age > freshMs)) error("Lectura antigua: $age ms")
                    diagnostics[name] = "AAOS; edad ${age} ms"
                    p.javaClass.getMethod("getValue").invoke(p)
                } catch (e: Exception) {
                    diagnostics[name] = (if(e is InvocationTargetException) e.targetException else e).javaClass.simpleName + ": no disponible"
                    null
                }
            }
            fun num(name: String, freshMs: Long? = null) = (read(name,freshMs) as? Number)?.toDouble()?.takeIf { it.isFinite() }
            val speed = num("PERF_VEHICLE_SPEED", 2000)?.takeIf { abs(it) <= 100 }?.let { abs(it)*3.6 }
            val odo = num("PERF_ODOMETER")?.takeIf { it >= 0 }
            val wh = num("EV_BATTERY_LEVEL")?.takeIf { it >= 0 }
            val cap = num("EV_CURRENT_BATTERY_CAPACITY")?.takeIf { it > 0 }
            val pct = if (wh != null && cap != null && wh <= cap) 100*wh/cap else null
            val kw = num("EV_BATTERY_INSTANTANEOUS_CHARGE_RATE", 2000)?.div(1_000_000)?.takeIf { abs(it) <= 1000 }
            val fuel = num("FUEL_LEVEL")?.takeIf { it >= 0 }?.div(1000)
            val range = num("RANGE_REMAINING")?.takeIf { it >= 0 }?.div(1000)
            val gear = read("GEAR_SELECTION", 2000) as? Number
            val parkId = runCatching { Class.forName("android.car.VehicleGear").getField("GEAR_PARK").getInt(null) }.getOrNull()
            val parked = if (gear != null && parkId != null) gear.toInt() == parkId else null
            val brake = read("PARKING_BRAKE_ON", 2000) as? Boolean
            data = CarTelemetry(speed,odo,pct,wh?.div(1000),kw?.let { (-it).coerceAtLeast(0.0) },kw?.coerceAtLeast(0.0),fuel,range,null,null,parked,brake,SystemClock.elapsedRealtime(),diagnostics.toMap(), timestamps["EV_BATTERY_INSTANTANEOUS_CHARGE_RATE"], timestamps["GEAR_SELECTION"], timestamps["PARKING_BRAKE_ON"], timestamps["PERF_VEHICLE_SPEED"])
            energy.sample(kw, if (kw != null) timestamps["EV_BATTERY_INSTANTANEOUS_CHARGE_RATE"] else null)
            totals = energy.totals()
        } catch (e: Exception) {
            data = CarTelemetry(diagnostics = mapOf("AAOS" to "No disponible / sin permiso: ${e.javaClass.simpleName}"))
            energy.sample(null, null)
            disconnect()
        } catch (e: LinkageError) { data = CarTelemetry(diagnostics = mapOf("AAOS" to "Biblioteca no disponible")); energy.sample(null,null); disconnect() }
    }
    private fun disconnect() { car?.let { runCatching { it.javaClass.getMethod("disconnect").invoke(it) } }; car = null }
    override fun close() { if (!closeOnce.compareAndSet(false,true)) return; closed = true; polling.cancel(); scope.launch { try { disconnect() } finally { scope.cancel(); dispatcher.close() } } }
}
/** Additional deny-only policy. Never relax the manufacturer's existing restriction.
 * No cloud, GPS or OBD readings can grant permission to display video. */
object ParkedVideoGate {
    fun permits(t: CarTelemetry, oemAllows: Boolean): Boolean = oemAllows &&
        t.sampledAtMs > 0 && SystemClock.elapsedRealtime() - t.sampledAtMs in 0L..1500L &&
        t.speedKmh != null && abs(t.speedKmh) < 0.1 && t.gearPark == true && t.brake == true &&
        listOf(t.speedAtNs,t.gearAtNs,t.brakeAtNs).all { it != null && SystemClock.elapsedRealtimeNanos()-it in 0L..1_500_000_000L }
}
