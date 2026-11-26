package es.drivecore.cockpit
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import java.util.concurrent.Executors

object NativeUsb {
    private val ready=runCatching{System.loadLibrary("drivecore_usb");true}.getOrDefault(false)
    private external fun descriptor(fd:Int):String
    fun inspect(fd:Int)=if(ready)descriptor(fd)else "LibUSB no disponible para esta arquitectura"
}
class UsbProbeActivity:Activity(){
    private lateinit var usb:UsbManager
    private lateinit var report:TextView
    private var selected:String?=null
    private var registered=false
    private val worker=Executors.newSingleThreadExecutor()
    private val action="es.drivecore.cockpit.USB_DIAGNOSTIC_PERMISSION"
    private val receiver=object:BroadcastReceiver(){
        @Suppress("DEPRECATION")
        override fun onReceive(c:Context?,i:Intent?){
            if(i?.action!=action)return
            val d=if(Build.VERSION.SDK_INT>=33)i.getParcelableExtra(UsbManager.EXTRA_DEVICE,UsbDevice::class.java) else i.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            if(d!=null&&selected==d.deviceName&&i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false)&&usb.hasPermission(d))read(d)else report.text="Permiso USB no concedido"
        }
    }
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        usb=getSystemService(UsbManager::class.java)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16)}
        root.addView(Button(this).apply{text="Volver";setOnClickListener{finish()}})
        report=TextView(this).apply{text="Selecciona un dispositivo de prueba con el coche estacionado. Desconecta primero Android Auto o CarPlay. No se reclaman interfaces USB."}
        root.addView(report)
        val scroll=ScrollView(this);val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        try{
            if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,IntentFilter(action),Context.RECEIVER_NOT_EXPORTED)else { @Suppress("DEPRECATION") registerReceiver(receiver,IntentFilter(action)) }
            registered=true
        }catch(e:Exception){report.text="No se pudo preparar permiso USB: "+e.javaClass.simpleName;return}
        for(d in usb.deviceList.values)list.addView(Button(this).apply{text="%04X:%04X · %d interfaces".format(d.vendorId,d.productId,d.interfaceCount);setOnClickListener{
            selected=d.deviceName
            if(usb.hasPermission(d))read(d)else{
                val p=PendingIntent.getBroadcast(this@UsbProbeActivity,17,Intent(action).setPackage(packageName),PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=31)PendingIntent.FLAG_MUTABLE else 0)
                runCatching{usb.requestPermission(d,p)}.onFailure{report.text="Permiso USB no disponible"}
            }
        }})
        if(usb.deviceList.isEmpty())list.addView(TextView(this).apply{text="Sin dispositivos USB"})
    }
    private fun read(d:UsbDevice){
        report.text="Leyendo descriptor autorizado…"
        worker.execute{
            val result=runCatching{val c=usb.openDevice(d)?:error("No se pudo abrir");try{NativeUsb.inspect(c.fileDescriptor)}finally{c.close()}}.getOrElse{"LibUSB: "+it.javaClass.simpleName}
            runOnUiThread{if(!isDestroyed)report.text=result}
        }
    }
    override fun onDestroy(){if(registered)runCatching{unregisterReceiver(receiver)};worker.shutdown();super.onDestroy()}
}
