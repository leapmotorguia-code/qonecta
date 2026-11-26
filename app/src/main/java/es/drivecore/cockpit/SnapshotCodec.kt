package es.drivecore.cockpit
import com.google.protobuf.CodedOutputStream
import java.io.ByteArrayOutputStream
object SnapshotCodec {
    fun encode(t:CarTelemetry):ByteArray {
        val bytes=ByteArrayOutputStream()
        val out=CodedOutputStream.newInstance(bytes)
        out.writeUInt32(1,1)
        out.writeUInt64(2,t.sampledAtMs)
        listOf(t.speedKmh,t.batteryPct,t.batteryKwh,t.dischargeKw,t.incomingKw,t.fuelLitres,t.rangeCombinedKm,t.odometerKm).forEachIndexed { i,v -> if(v!=null&&v.isFinite())out.writeDouble(i+3,v) }
        t.gearPark?.let {out.writeBool(11,it)}
        t.brake?.let {out.writeBool(12,it)}
        out.flush()
        return bytes.toByteArray()
    }
}
