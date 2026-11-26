package es.drivecore.cockpit
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import no.nordicsemi.android.ble.BleManager

/** Read-only GATT service discovery on an explicitly selected paired adapter.
 * No characteristic writes, no ELM protocol guessed. */
@SuppressLint("MissingPermission")
class BleProbe(context:Context,private val status:(String)->Unit):BleManager(context.applicationContext){
    override fun isRequiredServiceSupported(gatt:BluetoothGatt):Boolean {
        val services=gatt.services
        status("BLE GATT: "+services.size+" servicios\n"+services.take(12).joinToString("\n"){s->s.uuid.toString()+" · "+s.characteristics.size+" características"}+"\nProtocolo OBD BLE pendiente de identificar.")
        return services.isNotEmpty()
    }
    override fun onServicesInvalidated(){status("BLE desconectado / servicios invalidados")}
    fun inspect(device:BluetoothDevice){
        connect(device).timeout(15000).useAutoConnect(false).fail { _,code->status("BLE: error de conexión "+code) }.enqueue()
    }
}
