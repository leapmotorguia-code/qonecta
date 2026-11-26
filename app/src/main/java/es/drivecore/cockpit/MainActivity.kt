package es.drivecore.cockpit

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.os.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.*
import android.net.Uri
import android.view.*
import android.widget.*
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity() {
    private val bg=Color.rgb(8,21,31); private val panel=Color.rgb(16,38,50)
    private val cyan=Color.rgb(97,229,232); private val amber=Color.rgb(255,196,112)
    private val white=Color.rgb(230,244,249); private val muted=Color.rgb(154,183,196)
    private lateinit var usb: UsbManager
    private var telemetry:AaosTelemetryProvider?=null
    private var last=CarTelemetry(); private var totals=EnergyTotals()
    private val ui=Handler(Looper.getMainLooper()); private var mode="Energía"
    private var usbEvent="Sin evento USB"; private var radioStatus="Radio detenida"
    private lateinit var radio: RadioEngine
    private val obd = ObdClient()
    private lateinit var history: HistoryStore
    private var recording = false
    private var sessionId = 0L
    private var lastSaved = 0L
    private var bleProbe: BleProbe? = null
    private var bleStatus = "BLE sin conexión"
    private var historyStatus = "Historial desactivado"
    private lateinit var status: TextView; private lateinit var detail: TextView
    private lateinit var right: LinearLayout; private lateinit var gauge: Gauge
    private lateinit var chart: PowerChart
    private lateinit var graphContainer: LinearLayout
    private val metrics=linkedMapOf<String,TextView>(); private val tabs=linkedMapOf<String,Button>()
    private var lastGraphNs:Long?=null
    private val graphPoints=ArrayList<Pair<Long,Double>>()
    private val refresh=object:Runnable { override fun run(){ render(); ui.postDelayed(this,500) } }
    private fun dp(n:Int)=(n*resources.displayMetrics.density+0.5f).toInt()
    private fun shape(color:Int,stroke:Int=0):GradientDrawable=GradientDrawable().apply{setColor(color);cornerRadius=dp(12).toFloat();if(stroke!=0)setStroke(dp(1),stroke)}
    private fun label(s:String,size:Float=14f,color:Int=muted,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(color);if(bold)setTypeface(typeface,Typeface.BOLD)}
    private fun col()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
    private fun row()=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    private fun gap(parent:LinearLayout,n:Int=8){parent.addView(View(this),if(parent.orientation==LinearLayout.VERTICAL)LinearLayout.LayoutParams(1,dp(n)) else LinearLayout.LayoutParams(dp(n),1))}
    private fun button(s:String,action:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=14f;setTextColor(white);background=shape(Color.rgb(23,54,67));minHeight=dp(48);setOnClickListener{action()}}
    private fun card(parent:LinearLayout,title:String,key:String,color:Int=white){val v=col();v.background=shape(Color.rgb(10,29,40));v.setPadding(dp(12),dp(9),dp(12),dp(9));v.addView(label(title,11f,muted,true));val t=label("—",24f,color,true);metrics[key]=t;v.addView(t);parent.addView(v,LinearLayout.LayoutParams(0, -2,1f));}
    override fun onCreate(state:Bundle?){
        super.onCreate(state);history=HistoryStore(this);usb=getSystemService(UsbManager::class.java);radio=RadioEngine(this){radioStatus=it;if(::detail.isInitialized&&mode=="Radio")detail.text=it}
        mode=state?.getString("mode") ?: "Energía";if(mode !in listOf("Energía","Estado","Conexiones","Radio","Historial","Diagnóstico"))mode="Energía"
        val root=col().apply{setBackgroundColor(bg);setPadding(dp(14),dp(12),dp(14),dp(12))}
        val header=row();val brand=col();brand.addView(label("DRIVECORE  /  NATIVE",22f,cyan,true));brand.addView(label("C10 REEV · 0.5  |  AAOS + OBD2",12f));header.addView(brand,LinearLayout.LayoutParams(0,-2,1f));status=label("Comprobando…",13f,muted);header.addView(status);root.addView(header);gap(root,10)
        val body=row();body.gravity=Gravity.TOP;root.addView(body,LinearLayout.LayoutParams(-1,0,1f))
        val leftScroll=ScrollView(this).apply{isFillViewport=true;isVerticalScrollBarEnabled=false;background=shape(panel)}
        val left=col().apply{setPadding(dp(12),dp(12),dp(12),dp(12))};leftScroll.addView(left);body.addView(leftScroll,LinearLayout.LayoutParams(0,-1,42f));gap(body,12)
        left.addView(label("INSTRUMENTACIÓN",13f,cyan,true));val car=ImageView(this).apply{setImageResource(R.drawable.car_silhouette);contentDescription="Silueta ilustrativa";scaleType=ImageView.ScaleType.FIT_CENTER};left.addView(car,LinearLayout.LayoutParams(-1,dp(58)))
        gauge=Gauge();left.addView(gauge,LinearLayout.LayoutParams(-1,dp(145)))
        val r1=row();card(r1,"BATERÍA","soc",cyan);gap(r1);card(r1,"COMBUSTIBLE","fuel",amber);left.addView(r1);gap(left)
        val r2=row();card(r2,"DESCARGA","out",amber);gap(r2);card(r2,"ENTRADA","in",cyan);left.addView(r2);gap(left)
        val r3=row();card(r3,"AUTONOMÍA INFORMADA","range");gap(r3);card(r3,"ODÓMETRO","odo");left.addView(r3)
        right=col().apply{background=shape(panel);setPadding(dp(14),dp(12),dp(14),dp(12))};body.addView(right,LinearLayout.LayoutParams(0,-1,58f))
        gap(root,10);val nav=row();for(name in listOf("Energía","Estado","Conexiones","Radio","Historial","Diagnóstico")){val b=button(name){mode=name;buildRight();render()};tabs[name]=b;nav.addView(b,LinearLayout.LayoutParams(0,dp(50),1f));if(name!="Diagnóstico")gap(nav,8)};root.addView(nav)
        setContentView(root);buildRight();handleUsb(intent)
    }
    private fun buildRight(){
        right.removeAllViews();tabs.forEach{(n,b)->b.background=shape(if(n==mode)Color.rgb(26,84,94) else Color.rgb(23,54,67),if(n==mode)cyan else 0)}
        right.addView(label(mode.uppercase(Locale.ROOT),19f,cyan,true));gap(right,8)
        detail=label("—",14f,white);val scroll=ScrollView(this).apply{isFillViewport=false};val contents=col();scroll.addView(contents);right.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        if(mode=="Energía"){
            graphContainer=col();chart=PowerChart();graphContainer.addView(chart,LinearLayout.LayoutParams(-1,dp(160)));contents.addView(graphContainer);gap(contents)
            val rr=row();card(rr,"SALIDA MEDIDA","sessionOut",amber);gap(rr);card(rr,"ENTRADA MEDIDA","sessionIn",cyan);contents.addView(rr);gap(contents);contents.addView(detail);gap(contents)
            contents.addView(button("Reiniciar contador de sesión"){telemetry?.resetEnergy();totals=EnergyTotals();graphPoints.clear();lastGraphNs=null;render()})
        } else {
            contents.addView(detail);gap(contents)
            when(mode){
                "Historial"->{
                    contents.addView(button("Iniciar registro de muestras AAOS"){recording=true;sessionId=System.currentTimeMillis();lastSaved=0;historyStatus="Registrando sesión";render()});gap(contents)
                    contents.addView(button("Finalizar registro"){recording=false;historyStatus="Registro detenido";render()});gap(contents)
                    contents.addView(button("Ver últimas muestras guardadas"){history.list{ rows -> if(!isFinishing) AlertDialog.Builder(this).setTitle("Últimas muestras · Room").setMessage(rows).setPositiveButton("Cerrar",null).show() }});gap(contents)
                    contents.addView(button("Exportar última lectura · Protobuf"){exportProto()});gap(contents)
                    contents.addView(button("Borrar historial local"){AlertDialog.Builder(this).setMessage("¿Borrar todas las muestras guardadas?").setPositiveButton("Borrar"){_,_->recording=false;history.clear{historyStatus=it}}.setNegativeButton("Cancelar",null).show()})
                }
                "Radio"->{
                    contents.addView(label("Radio HTTPS · Media3 / HLS",13f));val url=EditText(this).apply{hint="https://… (audio)";setSingleLine(true);setTextColor(white);setHintTextColor(muted);inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI;setText(getPreferences(MODE_PRIVATE).getString("radioUrl",""))};contents.addView(url)
                    val r=row();r.addView(button("Reproducir"){val s=url.text.toString().trim();if(runCatching{Uri.parse(s).scheme=="https"&&!Uri.parse(s).host.isNullOrBlank()}.getOrDefault(false)){getPreferences(MODE_PRIVATE).edit().putString("radioUrl",s).apply();radio.play(s)}else{radioStatus="Introduce una URL HTTPS de audio válida";render()}},LinearLayout.LayoutParams(0,-2,1f));gap(r);r.addView(button("Detener"){radio.stop();radioStatus="Radio detenida";render()},LinearLayout.LayoutParams(0,-2,1f));contents.addView(r);contents.addView(label("TDT de vídeo y proyección: no implementadas en esta base.",13f));
                }
                "Estado"->{contents.addView(button("Abrir mapa MapLibre"){runCatching{startActivity(Intent(this,MapActivity::class.java))}.onFailure{Toast.makeText(this,"Mapa no disponible",Toast.LENGTH_LONG).show()}})}
                "Conexiones"->{contents.addView(button("Diagnóstico BLE (emparejado)"){chooseBle()});gap(contents);contents.addView(button("Cerrar conexión BLE"){bleProbe?.close();bleProbe=null;bleStatus="BLE desconectado";render()});gap(contents);contents.addView(button("Conectar OBD2 Bluetooth clásico"){chooseObd()});gap(contents);contents.addView(button("Desconectar OBD2"){obd.disconnect();render()});gap(contents);contents.addView(button("Ajustes Wi-Fi"){settings(android.provider.Settings.ACTION_WIFI_SETTINGS)});gap(contents);contents.addView(button("Ajustes Bluetooth"){settings(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)});gap(contents);contents.addView(button("Permiso Bluetooth (Android 12+)"){if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),9)})}
                "Diagnóstico"->{contents.addView(button("Solicitar permisos del vehículo"){askCarPermissions()});gap(contents);contents.addView(button("Compartir informe de diagnóstico"){val i=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,diagnostic());runCatching{startActivity(Intent.createChooser(i,"Compartir diagnóstico"))}})}
            }
        }
    }
    private fun chooseBle(){
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),9);return}
        val devices=runCatching{getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices?.filter{it.type==android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE||it.type==android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL}.orEmpty()}.getOrDefault(emptyList())
        if(devices.isEmpty()){Toast.makeText(this,"Empareja un adaptador BLE compatible en ajustes. No todos admiten emparejamiento.",Toast.LENGTH_LONG).show();return}
        val names=devices.map{runCatching{it.name?:it.address}.getOrDefault("BLE")}.toTypedArray()
        AlertDialog.Builder(this).setTitle("Selecciona tu adaptador BLE").setItems(names){_,i->
            bleProbe?.close();bleProbe=null;bleStatus="Conectando BLE..."
            val probe=BleProbe(this){s->ui.post{bleStatus=s}}
            bleProbe=probe;probe.inspect(devices[i])
        }.setNegativeButton("Cancelar",null).show()
    }
    private fun exportProto(){
        val t=telemetry?.latest()?:CarTelemetry()
        if(t.sampledAtMs<=0||SystemClock.elapsedRealtime()-t.sampledAtMs !in 0L..2500L){Toast.makeText(this,"Sin lectura AAOS actual",Toast.LENGTH_LONG).show();return}
        val encoded=SnapshotCodec.encode(t)
        val payload="DriveCore TelemetrySnapshot v1 (Protobuf, Base64)\n"+android.util.Base64.encodeToString(encoded,android.util.Base64.NO_WRAP)
        runCatching{startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,payload),"Compartir lectura"))}
    }
    private fun chooseObd(){
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),9);return}
        val devices=runCatching{getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices?.toList().orEmpty()}.getOrDefault(emptyList())
        if(devices.isEmpty()){Toast.makeText(this,"Empareja primero el adaptador OBD2 SPP en ajustes Bluetooth",Toast.LENGTH_LONG).show();return}
        val names=devices.map{runCatching{it.name ?: it.address}.getOrDefault("Dispositivo Bluetooth")}.toTypedArray()
        AlertDialog.Builder(this).setTitle("Selecciona SOLO tu adaptador OBD2 ELM327 SPP").setItems(names){_,i->
            AlertDialog.Builder(this).setMessage("Conectar a ${names[i]} y consultar únicamente PIDs estándar de lectura?").setPositiveButton("Conectar"){_,_->obd.connect(devices[i]);render()}.setNegativeButton("Cancelar",null).show()
        }.setNegativeButton("Cancelar",null).show()
    }
    private fun obdReport()="OBD2 SPP: ${obd.status()}\nVelocidad OBD: ${fmt(obd.read("010D"),"km/h",0)} · combustible OBD: ${fmt(obd.read("012F"),"%",0)}\nMotor: ${fmt(obd.read("010C"),"rpm",0)} · refrigerante: ${fmt(obd.read("0105"),"°C",0)}\nTemperatura exterior OBD: ${fmt(obd.read("0146"),"°C",0)}\nSolo PID estándar; no batería HV ni CAN propietario."
    private fun settings(action:String){runCatching{startActivity(Intent(action))}.onFailure{Toast.makeText(this,"Ajuste no disponible",Toast.LENGTH_SHORT).show()}}
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);handleUsb(intent)}
    override fun onSaveInstanceState(out:Bundle){out.putString("mode",mode);super.onSaveInstanceState(out)}
    @Suppress("DEPRECATION") private fun handleUsb(i:Intent?){if(i?.action==UsbManager.ACTION_USB_DEVICE_ATTACHED){val d=i.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE);if(d!=null)usbEvent="USB ${d.vendorId}:${d.productId} · permiso ${if(usb.hasPermission(d))"sí" else "no"}"}}
    private fun askCarPermissions(){val requested=arrayOf("android.car.permission.CAR_SPEED","android.car.permission.CAR_ENERGY","android.car.permission.CAR_MILEAGE_3P","android.car.permission.CAR_MILEAGE","android.car.permission.CAR_POWERTRAIN","android.car.permission.CAR_INFO").filter{p->runCatching{(packageManager.getPermissionInfo(p,0).protectionLevel and 15)==1&&checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED}.getOrDefault(false)};if(requested.isNotEmpty())requestPermissions(requested.toTypedArray(),8)else Toast.makeText(this,"No hay más permisos de usuario solicitables",Toast.LENGTH_LONG).show()}
    private fun fmt(v:Double?,unit:String,dec:Int=1)=v?.let{String.format(Locale("es","ES"),"%.${dec}f",it)+" "+unit}?:"—"
    private fun metric(k:String,v:String){metrics[k]?.text=v}
    private fun usbReport()=runCatching{usb.deviceList.values.joinToString("\n"){d->"USB %04X:%04X · %d interfaces · permiso %s".format(d.vendorId,d.productId,d.interfaceCount,if(usb.hasPermission(d))"sí" else "no")}.ifBlank{"No se detectan dispositivos USB host"}}.getOrDefault("USB no accesible")
    private fun diagnostic():String {val codecs=runCatching{MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter{!it.isEncoder}.flatMap{it.supportedTypes.toList()}.filter{it=="video/avc"||it=="video/hevc"}.distinct().joinToString()}.getOrDefault("No disponible");val audio=runCatching{getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_OUTPUTS).joinToString{it.type.toString()}}.getOrDefault("No disponible");return "DriveCore Native 0.5 · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nUSB host: ${packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)} · ${usbReport()}\nCódecs: $codecs\nTipos de salida de audio: $audio\n${obdReport()}\n\n"+last.diagnostics.entries.joinToString("\n"){"${it.key}: ${it.value}"}.ifBlank{"Esperando servicio AAOS"}}
    private fun render(){val p=telemetry;last=p?.latest()?:CarTelemetry();val valid=last.sampledAtMs>0&&SystemClock.elapsedRealtime()-last.sampledAtMs in 0L..2500L;val t=if(valid)last else CarTelemetry();if(p!=null)totals=p.totals
        val now=SystemClock.elapsedRealtime();if(recording&&valid&&now-lastSaved>=10000L&&(t.speedKmh!=null||t.batteryPct!=null||t.fuelLitres!=null||t.incomingKw!=null)){lastSaved=now;history.save(HistoryStore.Sample(sessionId,System.currentTimeMillis(),t.speedKmh,t.batteryPct,t.fuelLitres,t.dischargeKw,t.incomingKw)){historyStatus=it}}
        gauge.speed=t.speedKmh;gauge.invalidate();metric("soc",fmt(t.batteryPct,"%",0));metric("fuel",fmt(t.fuelLitres,"L"));metric("out",fmt(t.dischargeKw,"kW"));metric("in",fmt(t.incomingKw,"kW"));metric("range",fmt(t.rangeCombinedKm,"km",0));metric("odo",fmt(t.odometerKm,"km",0));status.text=if(t.speedKmh!=null||t.batteryKwh!=null)"AAOS · lectura disponible" else "AAOS · sin datos confirmados"
        if(mode=="Energía"){metric("sessionOut",fmt(totals.outKwh.takeIf{totals.intervals>0},"kWh",3));metric("sessionIn",fmt(totals.inKwh.takeIf{totals.intervals>0},"kWh",3));detail.text="Balance salida − entrada: ${fmt(totals.netKwh.takeIf{totals.intervals>0},"kWh",3)}\nEntrada no equivale a frenada regenerativa. Incluye posibles aportes del generador o carga. Contador parcial de esta sesión de la aplicación.";val ns=t.powerAtNs;if(ns!=null&&ns!=lastGraphNs&&t.incomingKw!=null&&t.dischargeKw!=null){chart.add(ns,t.incomingKw-t.dischargeKw);lastGraphNs=ns};chart.invalidate()}
        else detail.text=when(mode){"Estado"->"Energía almacenada: ${fmt(t.batteryKwh,"kWh")}\nMarcha P: ${t.gearPark?.let{if(it)"Sí" else "No"}?:"—"}\nFreno estacionamiento: ${t.brake?.let{if(it)"Sí" else "No"}?:"—"}\nAutonomía EV y gasolina por separado: no expuestas por este lector.\nDatos de solo lectura; — = no disponible.";"Conexiones"->"$usbEvent\n${usbReport()}\n\n${obdReport()}\n\n$bleStatus\n\nUSB detectado no implica sesión de proyección.\nAndroid Auto y CarPlay: receptor pendiente. No se reclama ninguna interfaz USB.";"Radio"->radioStatus;"Historial"->"$historyStatus\nMuestras AAOS reales cada 10 s mientras la pantalla está activa. No es un cálculo de trayecto completo. Datos locales, sin GPS ni subida automática.";else->diagnostic()}
    }
    override fun onResume(){super.onResume();if(telemetry==null)telemetry=AaosTelemetryProvider(this);ui.removeCallbacks(refresh);ui.post(refresh)}
    override fun onPause(){ui.removeCallbacks(refresh);bleProbe?.close();bleProbe=null;bleStatus="BLE cerrado al salir";radio.stop();radioStatus="Radio detenida al salir";super.onPause()}
    override fun onDestroy(){ui.removeCallbacks(refresh);radio.stop();bleProbe?.close();history.close();obd.close();telemetry?.close();telemetry=null;super.onDestroy()}
    inner class Gauge:View(this@MainActivity){var speed:Double?=null;private val paint=Paint(3);override fun onDraw(c:Canvas){val w=width.toFloat();val h=height.toFloat();val r=min(w*.38f,h*.54f);val cx=w/2;val cy=h*.58f;val a=RectF(cx-r,cy-r,cx+r,cy+r);paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(9).toFloat();paint.strokeCap=Paint.Cap.ROUND;paint.color=Color.rgb(40,70,82);c.drawArc(a,150f,240f,false,paint);speed?.let{paint.color=cyan;c.drawArc(a,150f,(it/200*240).toFloat().coerceIn(0f,240f),false,paint)};paint.style=Paint.Style.FILL;paint.textAlign=Paint.Align.CENTER;paint.typeface=Typeface.DEFAULT_BOLD;paint.textSize=dp(40).toFloat();paint.color=white;c.drawText(speed?.roundToInt()?.toString()?:"—",cx,cy+dp(5),paint);paint.textSize=dp(13).toFloat();paint.color=muted;c.drawText("km/h · AAOS",cx,cy+dp(27),paint)}}
    inner class PowerChart:View(this@MainActivity){val points=graphPoints;private val p=Paint(3);fun add(ns:Long,v:Double){points.add(ns to v);while(points.size>100)points.removeAt(0)};override fun onDraw(c:Canvas){val w=width.toFloat();val h=height.toFloat();c.drawRoundRect(0f,0f,w,h,dp(10).toFloat(),dp(10).toFloat(),p.apply{color=Color.rgb(10,29,40);style=Paint.Style.FILL});p.color=muted;p.textSize=dp(12).toFloat();c.drawText("POTENCIA DE BATERÍA · kW",dp(10).toFloat(),dp(18).toFloat(),p);val mid=h*.56f;p.color=Color.rgb(61,88,100);c.drawLine(dp(10).toFloat(),mid,w-dp(10),mid,p);if(points.size<2){p.color=muted;c.drawText("Esperando muestras reales",dp(10).toFloat(),mid-dp(12),p);return};val scale=max(10.0,points.maxOf{abs(it.second)});val start=points.first().first;val duration=max(1L,points.last().first-start)
fun xx(n:Long):Float=dp(10)+(w-dp(20))*(n-start).toFloat()/duration
fun yy(v:Double):Float=mid-(v/scale*(h*.33)).toFloat()
p.strokeWidth=dp(2).toFloat();for(i in 1 until points.size){val a=points[i-1];val b=points[i];if(b.first-a.first>3_000_000_000L)continue;p.color=if(b.second>=0)cyan else amber;c.drawLine(xx(a.first),yy(a.second),xx(b.first),yy(b.second),p)};p.color=muted;c.drawText("±${scale.roundToInt()} kW",dp(10).toFloat(),h-dp(9),p)}}
}
