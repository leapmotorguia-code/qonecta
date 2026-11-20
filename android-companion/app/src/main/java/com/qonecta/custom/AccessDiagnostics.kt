package es.c10.complemento

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.location.LocationManager

/** Public, read-only API observations only. Never binds OEM services or invokes car controls. */
class AccessDiagnostics(private val context:Context) {
 fun inspect():String=buildString {
  append("ACCESOS · observaciones de esta aplicación\n")
  append("UNIT original: ")
  try {
   val pi=context.packageManager.getPackageInfo("com.tencent.qqmusiccar",0)
   append("VISIBLE · ${pi.versionName} · versión ${pi.longVersionCode}\n")
  }catch(e:Exception){append("NO VISIBLE / NO INSTALADA; no se infiere bloqueo\n")}
  append("Abrir UNIT no comparte su firma, permisos o datos con esta app.\n")
  val lm=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
  append("Ubicación precisa: ${if(granted(Manifest.permission.ACCESS_FINE_LOCATION)) "PERMISO CONCEDIDO; señal no comprobada aquí" else "PERMISO NO CONCEDIDO"}\n")
  append("Proveedor GPS: ")
  try {append(if(lm.allProviders.contains(LocationManager.GPS_PROVIDER)) "VISIBLE; activado=${lm.isProviderEnabled(LocationManager.GPS_PROVIDER)}\n" else "NO DISPONIBLE EN API PÚBLICA\n")}catch(e:Exception){append("NO COMPROBABLE · ${e.javaClass.simpleName}\n")}
  val sm=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  val sensors=listOf("Acelerómetro" to Sensor.TYPE_ACCELEROMETER,"Giroscopio" to Sensor.TYPE_GYROSCOPE,"Luz" to Sensor.TYPE_LIGHT,"Proximidad" to Sensor.TYPE_PROXIMITY)
  for((name,type)in sensors)append("$name: ${if(sm.getDefaultSensor(type)!=null) "VISIBLE; lectura funcional no acreditada aquí" else "NO DISPONIBLE EN API PÚBLICA"}\n")
  append("Proximidad Android no equivale a distancia delantera del coche.\n")
  append("Cámaras Android: ")
  try {val cameras=(context.getSystemService(Context.CAMERA_SERVICE) as CameraManager).cameraIdList;append("${cameras.size} expuestas a Camera2; ninguna abierta. No acredita cámaras 360 o ADAS.\n")}
  catch(e:Exception){append("NO COMPROBABLE · ${e.javaClass.simpleName}\n")}
  val perms=listOf("com.leapmotor.autosdk.server.USE_SERVICE","com.leapmotor.media.MediaService.permission.ACCESS_SERVICE","com.leapmotor.carcontrol.CAR_CONTROL_SERVICE")
  for(perm in perms)append("$perm: ${if(granted(perm)) "CONCEDIDO; servicio NO PROBADO" else "NO CONCEDIDO a esta app"}\n")
  for(pkg in listOf("com.leapmotor.autosdk.server","com.leapmotor.media","com.leapmotor.carcontrol")){
   append("Componente $pkg: ")
   try {val pi=context.packageManager.getPackageInfo(pkg,PackageManager.GET_SERVICES or PackageManager.GET_PROVIDERS);val count=(pi.services?.count{it.exported}?:0)+(pi.providers?.count{it.exported}?:0);append("VISIBLE; $count servicios/proveedores exportados. NO se ha intentado acceder.\n")}
   catch(e:PackageManager.NameNotFoundException){append("NO VISIBLE / NO INSTALADO bajo este nombre\n")}
   catch(e:Exception){append("NO COMPROBABLE · ${e.javaClass.simpleName}\n")}
  }
  append("RPM, potencia de tracción, batería HV, generador y regeneración real: SIN FUENTE DE DATOS INTEGRADA.\n")
  append("Puertas, bloqueo, cámaras de aparcamiento/ADAS y centinela del coche: NO COMPROBADOS / SIN INTEGRACIÓN.\n")
  append("No se han enviado órdenes ni leído CAN, VIN, cuentas, claves o coordenadas.\n")
 }
 private fun granted(permission:String)=context.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED
}
