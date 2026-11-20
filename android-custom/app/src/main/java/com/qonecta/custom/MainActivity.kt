package com.qonecta.custom

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.*
import android.location.*
import android.os.*
import android.graphics.Color
import android.view.View
import android.widget.*
import com.qonecta.custom.performance.PerformanceTracker
import com.qonecta.custom.efficiency.TripEnergyBalanceTracker
import com.qonecta.custom.sentry.VehicleSentryMonitor
import com.qonecta.custom.media.MediaStreamingLauncher
import java.util.Locale

class MainActivity : Activity(), SensorEventListener {
 private lateinit var body:LinearLayout
 private val tracker=PerformanceTracker()
 private val sentry=VehicleSentryMonitor()
 private lateinit var sm:SensorManager
 private lateinit var lm:LocationManager
 private var gps=false
 private var live:TextView?=null
 private var timings:TextView?=null
 private var status:TextView?=null
 private var page=""
 private val prefs by lazy { getSharedPreferences("lab12",MODE_PRIVATE) }
 private val listener=object:LocationListener {
  override fun onLocationChanged(l:Location) {
   if(!gps)return
   val age=SystemClock.elapsedRealtimeNanos()-l.elapsedRealtimeNanos
   if(!l.hasSpeed() || !l.hasAccuracy() || l.accuracy>20 || age<0 || age>2_500_000_000L || !l.hasSpeedAccuracy() || l.speedAccuracyMetersPerSecond>2) {
    tracker.cancelPending();live?.text="GPS sin precisión suficiente; prueba cancelada";return
   }
   val speed=l.speed.toDouble()*3.6
   live?.text=String.format(Locale.getDefault(),"GPS %.1f km/h · precisión ±%.1f km/h",speed,l.speedAccuracyMetersPerSecond*3.6)
   val results=tracker.onSpeedUpdateKmh(speed,l.elapsedRealtimeNanos)
   for (result in results) appendHistory("GPS aproximado ${result.label}: ${"%.2f".format(result.durationSeconds)} s")
   if(results.isNotEmpty()) timings?.text=prefs.getString("history","")
  }
  override fun onProviderDisabled(provider:String){tracker.cancelPending();live?.text="GPS desactivado"}
  override fun onProviderEnabled(provider:String){}
  @Deprecated("legacy") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?){}
 }
 override fun onCreate(b:Bundle?) {
  super.onCreate(b);sm=getSystemService(SENSOR_SERVICE) as SensorManager;lm=getSystemService(LOCATION_SERVICE) as LocationManager;home()
 }
 private fun panel(name:String) {
  stopSensors();page=name;live=null;timings=null;status=null
  val scroll=ScrollView(this);body=LinearLayout(this);body.orientation=LinearLayout.VERTICAL;body.setPadding(24,24,24,36);body.setBackgroundColor(Color.rgb(13,21,32));scroll.addView(body);setContentView(scroll)
  text(name,24f)
  if(name!="C10 LABORATORIO 1.2") button("Volver al inicio"){home()}
 }
 private fun text(s:String,size:Float=18f):TextView {
  val v=TextView(this);v.text=s;v.textSize=size;v.setTextColor(Color.WHITE);v.setPadding(0,12,0,12);body.addView(v);return v
 }
 private fun button(s:String,action:()->Unit){val b=Button(this);b.text=s;body.addView(b,LinearLayout.LayoutParams(-1,-2));b.setOnClickListener{action()}}
 private fun field(hint:String):EditText {val e=EditText(this);e.hint=hint;e.setTextColor(Color.WHITE);e.setHintTextColor(Color.LTGRAY);e.inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;body.addView(e);return e}
 private fun number(e:EditText):Double?=e.text.toString().replace(',','.').toDoubleOrNull()?.takeIf{it.isFinite() && it>=0 && it<=100000}
 private fun home(){panel("C10 LABORATORIO 1.2");text("Prueba independiente basada en el proyecto que has enviado. No incluye Qonecta Unit ni Android Auto/CarPlay. Sin conexión a telemetría, puertas o cámaras del coche.");button("Instrumentos GPS"){instruments()};button("Eficiencia manual"){efficiency()};button("Accesos multimedia"){media()};button("Vibración del dispositivo"){vibration()};button("Diagnóstico y exportación"){diagnostics()}}
 private fun instruments(){panel("Instrumentos GPS");text("3–100 NO equivale a 0–100. Necesita buena señal GPS y parada previa de un segundo. 80–120 detecta cruces de velocidad. Mediciones aproximadas; uso en recinto cerrado autorizado.");live=text("GPS detenido");button("Iniciar GPS"){startGps()};button("Detener GPS"){stopSensors();live?.text="GPS detenido"};button("Ejecutar demostración"){stopSensors();demo()};text("Historial local · GPS y DEMO separados");timings=text(prefs.getString("history","")?:"");button("Borrar historial"){prefs.edit().remove("history").apply();timings?.text="Sin resultados"}}
 private fun startGps(){
  if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),7);return}
  try {
   if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){live?.text="Activa Ubicación/GPS en los ajustes del dispositivo";return}
   tracker.cancelPending();lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,200L,0f,listener);gps=true;live?.text="Esperando señal GPS precisa…"
  }catch(e:Exception){live?.text="GPS no disponible: ${e.javaClass.simpleName}"}
 }
 override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,grants:IntArray){super.onRequestPermissionsResult(code,permissions,grants);if(code==7 && page=="Instrumentos GPS"){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) startGps() else live?.text="Ubicación precisa no autorizada"}}
 private fun demo(){val t=PerformanceTracker();val out=mutableListOf<String>();val samples=listOf(0.0,0.0,10.0,30.0,50.0,70.0,90.0,110.0,130.0);samples.forEachIndexed{i,v->t.onSpeedUpdateKmh(v,i*1_000_000_000L).forEach{out.add("DEMO ${it.label}: ${"%.2f".format(it.durationSeconds)} s")}};out.forEach{appendHistory(it)};live?.text="DEMO · datos simulados, sin movimiento del coche";timings?.text=prefs.getString("history","")}
 private fun appendHistory(s:String){val previous=prefs.getString("history","")?:"";prefs.edit().putString("history",(s+"\n"+previous).lineSequence().take(40).joinToString("\n")).apply()}
 private fun efficiency(){panel("Eficiencia manual");text("Datos reales de batería, motor y generador: SIN DATOS. El coche no está conectado. Cargar batería no equivale necesariamente a regenerar al frenar.");val kwh=field("Consumo eléctrico · kWh/100 km");val price=field("Precio electricidad · €/kWh");val liters=field("Consumo gasolina · L/100 km");val petrol=field("Precio gasolina · €/L");val result=text("Sin calcular");button("Calcular costes separados"){val a=number(kwh);val b=number(price);val c=number(liters);val d=number(petrol);result.text=if(a==null||b==null||c==null||d==null)"Introduce valores válidos" else "MANUAL · Electricidad ${"%.2f".format(a*b)} €/100 km\nGasolina ${"%.2f".format(c*d)} €/100 km\nNo calcula un trayecto mixto."};val power=field("Potencia media recuperada · kW");val seconds=field("Duración · segundos");val recovery=text("Estimación pendiente");button("Estimar energía"){val p=number(power);val s=number(seconds);recovery.text=if(p==null||s==null)"Introduce valores válidos" else "ESTIMADA · ${"%.4f".format(p*s/3600)} kWh; no es una lectura del coche"};button("Demostrar balance energético"){val e=TripEnergyBalanceTracker();for(i in 0..60)e.onTelemetryTick(-10.0,15.0,i*1_000_000_000L);val snap=e.snapshot();text("DEMO · batería cargada ${"%.3f".format(snap.chargedKwh)} kWh; generador ${"%.3f".format(snap.generatorKwh)} kWh. No atribuimos esta carga a frenadas.")}}
 private fun media(){panel("Accesos multimedia");text("Abre páginas en una aplicación externa disponible. No es un reproductor integrado y no garantiza compatibilidad DRM. Solo con el vehículo estacionado.");for((name,url)in MediaStreamingLauncher.platforms)button(name){MediaStreamingLauncher.launch(this,url)}}
 private fun vibration(){panel("Vibración del dispositivo");text("Prueba experimental en primer plano. Sin cámaras, puertas ni vigilancia con el coche apagado. Se detiene al salir de la app. Un sensor del teléfono mide el teléfono, no demuestra un impacto en el coche.");val sensor=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);status=text(if(sensor==null)"Acelerómetro no disponible" else "Sensor disponible; desarmado");button("Iniciar prueba de vibración"){if(sensor==null){status?.text="Acelerómetro no disponible"}else{if(sm.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL)){sentry.arm();status?.text="Prueba de vibración activa en primer plano"}else status?.text="No se pudo iniciar el sensor"}};button("Detener vigilancia"){stopSensors();status?.text="Vigilancia detenida"};text(prefs.getString("events","Sin eventos")?:"")}
 override fun onSensorChanged(e:SensorEvent){val event=sentry.evaluate(e.timestamp/1_000_000,e.values[0]/SensorManager.GRAVITY_EARTH,e.values[1]/SensorManager.GRAVITY_EARTH,e.values[2]/SensorManager.GRAVITY_EARTH)?:return;val entry="Vibración ${"%.2f".format(event.deltaG)} G · ${java.util.Date()}";prefs.edit().putString("events",(entry+"\n"+prefs.getString("events","")).lineSequence().take(50).joinToString("\n")).apply();status?.text=entry}
 override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
 private fun report()="C10 Laboratorio 1.2\nPaquete: $packageName\nAndroid ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}\nABI ${Build.SUPPORTED_ABIS.joinToString()}\nGPS ${lm.allProviders.contains(LocationManager.GPS_PROVIDER)}\nAcelerómetro ${sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!=null}\nUbicación precisa ${checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED}\nTelemetría C10: no conectada\nSin VIN, cuentas ni coordenadas.\n"+(prefs.getString("history","")?:"")
 private fun diagnostics(){panel("Diagnóstico y exportación");text(report());button("Guardar informe"){try{startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"C10-Laboratorio-Diagnostico.txt"),9)}catch(e:Exception){text("No hay selector de archivos disponible")}}}
 @Deprecated("legacy") override fun onActivityResult(request:Int,result:Int,data:Intent?){super.onActivityResult(request,result,data);if(request==9&&result==RESULT_OK){try{data?.data?.let{uri->contentResolver.openOutputStream(uri)?.use{it.write(report().toByteArray())};text("Informe guardado")}}catch(e:Exception){text("No se pudo guardar el informe")}}}
 private fun stopSensors(){if(gps){lm.removeUpdates(listener);gps=false};tracker.cancelPending();sm.unregisterListener(this);sentry.disarm()}
 override fun onPause(){stopSensors();live?.text="GPS detenido al salir";status?.text="Vigilancia detenida al salir";super.onPause()}
 @Deprecated("legacy") override fun onBackPressed(){home()}
}
