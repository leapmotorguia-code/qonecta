"""Rebuild the 0.8 combined project with an original three-panel layout inspired by 6.4.
Use the complete, pinned corresponding source from the successful 0.8 CI artifact."""
import pathlib,sys,re,xml.etree.ElementTree as ET
root=pathlib.Path(sys.argv[1]); p=root/"receiver-combined"
def edit(rel,fn):
    q=p/rel;s=q.read_text();v=fn(s)
    if s==v:raise ValueError("Patch not applied: "+rel)
    q.write_text(v)
def one(s,a,b):
    if s.count(a)!=1:raise ValueError("Expected one: "+a[:150]+" ("+str(s.count(a))+")")
    return s.replace(a,b,1)
def gradle(s):
    s=one(s,"versionCode = 8","versionCode = 9")
    s=one(s,'versionName = "0.8-headunit-experimental"','versionName = "6.4-fusion-0.8"')
    s=one(s,"targetSdk = 35","targetSdk = 35\n        ndk { abiFilters += listOf(\"arm64-v8a\", \"armeabi-v7a\") }")
    s=one(s,"    packaging {","    packaging {\n        jniLibs { useLegacyPackaging = true }")
    s=one(s,"// debugging setup","isDebuggable = false\n            isMinifyEnabled = false")
    return s
edit("app/build.gradle.kts",gradle)
def manifest(s):
    return one(s,'android:label="DriveCore Native"','android:label="DriveCore Fusion"')
edit("app/src/main/AndroidManifest.xml",manifest)
d="cockpit/src/main/java/es/drivecore/cockpit/"
def tele(s):
    s=one(s,"val speedAtNs: Long? = null","val speedAtNs: Long? = null,\n    val exteriorC: Double? = null, val oemVideoAllowed: Boolean = false")
    s=one(s,'            energy.sample(kw,', '''            val exterior = num("ENV_OUTSIDE_TEMPERATURE")?.takeIf { it in -60.0..70.0 }
            val oemAllows = runCatching {
                val ux = clazz.getMethod("getCarManager", String::class.java).invoke(c, "uxrestriction") ?: error("No UX manager")
                val restrictions = ux.javaClass.getMethod("getCurrentCarUxRestrictions").invoke(ux) ?: error("No UX data")
                val requires = restrictions.javaClass.getMethod("isRequiresDistractionOptimization").invoke(restrictions) as Boolean
                val flags = (restrictions.javaClass.getMethod("getActiveRestrictions").invoke(restrictions) as Number).toInt()
                !requires && flags == 0
            }.getOrDefault(false)
            data = data.copy(exteriorC=exterior,oemVideoAllowed=oemAllows,diagnostics=diagnostics.toMap())
            energy.sample(kw,''')
    return s
edit(d+"TelemetryProvider.kt",tele)
A="{http://schemas.android.com/apk/res/android}"
ET.register_namespace("android",A[1:-1])
path=p/"cockpit/src/main/AndroidManifest.xml"
doc=ET.parse(path);r=doc.getroot();ET.SubElement(r,"uses-permission",{A+"name":"android.car.permission.CAR_EXTERIOR_ENVIRONMENT"})
queries=ET.SubElement(r,"queries")
for pkg in ("com.google.android.apps.maps","com.waze","com.spotify.music","com.google.android.apps.youtube.music"):
    ET.SubElement(queries,"package",{A+"name":pkg})
doc.write(path,encoding="utf-8",xml_declaration=True)
def main(s):
    s=s.replace("Native 0.8","Fusion 6.4 / 0.8").replace("C10 REEV · 0.8","C10 REEV · Fusion")
    s=one(s,"    private val metrics=",'''    private lateinit var entertainment: LinearLayout
    private lateinit var mediaStatus: TextView
    private var video: android.webkit.WebView? = null
    private var mediaMode = "Radio"
    private var firstOdo: Double? = null
    private var latestDistance: Double? = null
    private var peakOut: Double? = null
    private var peakIn: Double? = null
    private var peakSpeed: Double? = null
    private val metrics=''')
    s=s.replace('private val bg=Color.rgb(8,21,31); private val panel=Color.rgb(16,38,50)','private val bg=Color.rgb(4,22,34); private val panel=Color.rgb(7,48,69)')
    s=s.replace('private val cyan=Color.rgb(97,229,232); private val amber=Color.rgb(255,196,112)','private val cyan=Color.rgb(65,246,240); private val amber=Color.rgb(233,250,70)')
    s=s.replace('cornerRadius=dp(12)','cornerRadius=dp(7)')
    s=s.replace('textSize=14f;setTextColor(white);background=shape(Color.rgb(23,54,67));minHeight=dp(48)','textSize=12f;setTextColor(white);background=shape(Color.rgb(7,72,101),Color.rgb(27,162,196));minHeight=dp(38);minimumHeight=dp(38);setPadding(dp(4),dp(2),dp(4),dp(2))')
    s=s.replace('v.background=shape(Color.rgb(10,29,40));v.setPadding(dp(12),dp(9),dp(12),dp(9))','v.background=shape(Color.rgb(8,55,80),Color.rgb(29,168,195));v.setPadding(dp(7),dp(6),dp(7),dp(6))').replace('label("—",24f,color,true)','label("—",19f,color,true)')
    i=s.index("    override fun onCreate(");j=s.index("    private fun buildRight()",i)
    s=s[:i]+'''    private fun pickMode(name:String){mode=name;buildRight();render()}
    private fun columnPanel(parent:LinearLayout,weight:Float):LinearLayout {
        val sc=ScrollView(this).apply{isFillViewport=true;isVerticalScrollBarEnabled=false;background=shape(panel,Color.rgb(30,161,189))}
        val c=col().apply{setPadding(dp(7),dp(7),dp(7),dp(7))}
        sc.addView(c);parent.addView(sc,LinearLayout.LayoutParams(0,-1,weight));return c
    }
    private fun controls(parent:LinearLayout,vararg pairs:Pair<String,()->Unit>){
        val r=row();pairs.forEachIndexed{i,p->if(i>0)gap(r,4);r.addView(button(p.first,p.second),LinearLayout.LayoutParams(0,dp(39),1f))};parent.addView(r);gap(parent,4)
    }
    private fun openApp(pkg:String,name:String){
        val i=packageManager.getLaunchIntentForPackage(pkg)
        if(i==null)Toast.makeText(this,"$name no instalado o no accesible",Toast.LENGTH_LONG).show()
        else runCatching{startActivity(i)}.onFailure{Toast.makeText(this,"$name no disponible",Toast.LENGTH_LONG).show()}
    }
    private fun projection(){
        AlertDialog.Builder(this).setTitle("Android Auto · prueba estacionado").setMessage("Desconecta la proyección anterior. No cambies el receptor por defecto ni actives root/ADB. CarPlay no incluido.").setPositiveButton("Abrir"){_,_->
            radio.stop();stopVideo()
            runCatching{startActivity(Intent().setClassName(packageName,"com.andrerinas.openheadunit.main.MainActivity"))}.onFailure{Toast.makeText(this,"Receptor no disponible",Toast.LENGTH_LONG).show()}
        }.setNegativeButton("Cancelar",null).show()
    }
    override fun onCreate(state:Bundle?){
        super.onCreate(state);history=HistoryStore(this);usb=getSystemService(UsbManager::class.java)
        radio=RadioEngine(this){radioStatus=it;if(::mediaStatus.isInitialized&&mediaMode=="Radio")mediaStatus.text=it;if(::detail.isInitialized&&mode=="Radio")detail.text=it}
        mode=state?.getString("mode")?:"Energía"
        if(mode !in listOf("Energía","Gráficas","Estado","Conexiones","Radio","Historial","Diagnóstico","Prestaciones"))mode="Energía"
        val root=col().apply{setBackgroundColor(bg);setPadding(dp(5),dp(5),dp(5),dp(5))}
        val header=row();header.addView(label("DRIVECORE FUSION 6.4",14f,cyan,true),LinearLayout.LayoutParams(0,-2,1f))
        status=label("Comprobando AAOS…",11f,muted);header.addView(status);root.addView(header);gap(root,5)
        controls(root,"Inicio" to {pickMode("Energía")},"Viajes" to {pickMode("Historial")},"Teléfono" to {pickMode("Conexiones")},"Ajustes" to {pickMode("Diagnóstico")},"Vistas" to {pickMode("Estado")})
        val body=row().apply{gravity=Gravity.TOP};root.addView(body,LinearLayout.LayoutParams(-1,0,1f))
        val left=columnPanel(body,26f);gap(body,4)
        left.addView(label("NAVEGACIÓN",12f,cyan,true));gap(left,4)
        controls(left,"Android Auto" to {projection()},"CarPlay" to {Toast.makeText(this,"Receptor CarPlay no integrado",Toast.LENGTH_LONG).show()})
        controls(left,"USB / Wi-Fi / Bluetooth" to {pickMode("Conexiones")})
        controls(left,"Google Maps" to {openApp("com.google.android.apps.maps","Google Maps")},"Waze" to {openApp("com.waze","Waze")})
        controls(left,"Mapa integrado · MapLibre" to {startActivity(Intent(this,MapActivity::class.java))})
        controls(left,"OBD2 · lectura" to {chooseObd()},"BLE" to {chooseBle()})
        controls(left,"LibUSB · diagnóstico" to {startActivity(Intent(this,UsbProbeActivity::class.java))})
        left.addView(label("Proyección manual. MapLibre es un visor de mapas, no navegación guiada. Apps externas solo si están instaladas.",11f))
        val center=columnPanel(body,48f);gap(body,4)
        center.addView(label("TELEMETRÍA DIGITAL",12f,cyan,true));gap(center,4)
        val top=row();val a=col();val b=col()
        fun single(c:LinearLayout,title:String,key:String,color:Int){val r=row();card(r,title,key,color);c.addView(r);gap(c,4)}
        single(a,"POTENCIA NETA","power",amber);single(a,"BATERÍA","soc",amber)
        single(b,"COMBUSTIBLE","fuel",white);single(b,"DISTANCIA SESIÓN","distance",cyan)
        top.addView(a,LinearLayout.LayoutParams(0,-2,1f));gap(top,4)
        val visual=col();gauge=Gauge();visual.addView(gauge,LinearLayout.LayoutParams(-1,dp(104)))
        val car=ImageView(this).apply{setImageResource(R.drawable.car_silhouette);contentDescription="Silueta ilustrativa";scaleType=ImageView.ScaleType.FIT_CENTER};visual.addView(car,LinearLayout.LayoutParams(-1,dp(32)))
        top.addView(visual,LinearLayout.LayoutParams(0,-2,1.35f));gap(top,4);top.addView(b,LinearLayout.LayoutParams(0,-2,1f));center.addView(top);gap(center,4)
        val r1=row();card(r1,"ENERGÍA SALIENTE","sessionOut",amber);gap(r1,4);card(r1,"ENERGÍA ENTRANTE","sessionIn",cyan);gap(r1,4);card(r1,"BALANCE NETO","net",amber);center.addView(r1);gap(center,4)
        val r2=row();card(r2,"CONSUMO NETO","average",amber);gap(r2,4);card(r2,"EXTERIOR","ext",white);gap(r2,4);card(r2,"AUTONOMÍA","range",cyan);center.addView(r2);gap(center,4)
        val r3=row();card(r3,"DESCARGA","out",amber);gap(r3,4);card(r3,"ENTRADA","in",cyan);gap(r3,4);card(r3,"ODÓMETRO","odo");center.addView(r3);gap(center,4)
        val nav=row()
        for((name,title) in listOf("Energía" to "Consumos","Gráficas" to "Gráficas","Estado" to "Accesos","Prestaciones" to "Prestaciones")){
            val bt=button(title){pickMode(name)};tabs[name]=bt;nav.addView(bt,LinearLayout.LayoutParams(0,dp(39),1f));gap(nav,3)
        }
        center.addView(nav);gap(center,4)
        right=col();center.addView(right,LinearLayout.LayoutParams(-1,dp(250)))
        entertainment=columnPanel(body,26f);buildMedia()
        setContentView(root);buildRight();handleUsb(intent)
    }
    private fun videoAllowed():Boolean {
        val t=telemetry?.latest()?:return false
        return ParkedVideoGate.permits(t,t.oemVideoAllowed)
    }
    private fun stopVideo(){
        video?.let{v->v.stopLoading();v.loadUrl("about:blank");(v.parent as? android.view.ViewGroup)?.removeView(v);v.destroy()}
        video=null
    }
    private fun buildMedia(){
        stopVideo();entertainment.removeAllViews()
        entertainment.addView(label("ENTRETENIMIENTO",12f,cyan,true));gap(entertainment,4)
        controls(entertainment,"TDT" to {mediaMode="TDT";buildMedia()},"Radio" to {mediaMode="Radio";buildMedia()},"Pluto TV" to {mediaMode="Pluto";buildMedia()})
        controls(entertainment,"Favoritos" to {mediaMode="Favoritos";buildMedia()},"Apps" to {mediaMode="Apps";buildMedia()})
        mediaStatus=label(if(mediaMode=="Radio")radioStatus else "Selecciona contenido",12f,white);entertainment.addView(mediaStatus);gap(entertainment,6)
        when(mediaMode){
            "Radio","Favoritos"->{
                entertainment.addView(label(radio.decoderReport(),11f))
                val prefs=getSharedPreferences("fusion_media",MODE_PRIVATE)
                val url=EditText(this).apply{hint="https://… audio";setSingleLine(true);setTextColor(white);setTextSize(12f);setHintTextColor(muted);setText(prefs.getString("audio",""))}
                entertainment.addView(url)
                controls(entertainment,"Reproducir" to {
                    val u=url.text.toString().trim()
                    if(Uri.parse(u).scheme=="https"&&!Uri.parse(u).host.isNullOrBlank()){prefs.edit().putString("audio",u).apply();radio.play(u)}else mediaStatus.text="Introduce URL HTTPS de audio"
                },"Parar" to {radio.stop();radioStatus="Radio detenida";mediaStatus.text=radioStatus})
                controls(entertainment,"Guardar favorito" to {
                    val u=url.text.toString().trim();if(Uri.parse(u).scheme=="https"&&!Uri.parse(u).host.isNullOrBlank()){
                        val fav=prefs.getStringSet("favorites",emptySet())!!.toMutableSet();if(fav.size<50){fav.add(u);prefs.edit().putStringSet("favorites",fav).apply();mediaStatus.text="Favorito guardado"}
                    }
                })
                if(mediaMode=="Favoritos")for(u in prefs.getStringSet("favorites",emptySet())!!.sorted())entertainment.addView(button(Uri.parse(u).host?:u){radio.play(u)})
            }
            "TDT"->{
                entertainment.addView(label("Emisiones oficiales · solo estacionado",11f))
                for((n,u) in listOf("La 1" to "https://www.rtve.es/play/videos/directo/la-1/","La 2" to "https://www.rtve.es/play/videos/directo/la-2/","Canal 24h" to "https://www.rtve.es/play/videos/directo/canal-24h/"))entertainment.addView(button(n){openVideo(u)})
            }
            "Pluto"->entertainment.addView(button("Abrir Pluto TV oficial"){openVideo("https://pluto.tv/es/live-tv")})
            "Apps"->{
                entertainment.addView(button("Spotify"){openApp("com.spotify.music","Spotify")})
                entertainment.addView(button("YouTube Music"){openApp("com.google.android.apps.youtube.music","YouTube Music")})
                entertainment.addView(button("Netflix · web"){openVideo("https://www.netflix.com/")})
                entertainment.addView(button("Prime Video · web"){openVideo("https://www.primevideo.com/")})
                entertainment.addView(button("Max · web"){openVideo("https://www.max.com/")})
                entertainment.addView(label("Las webs pueden exigir DRM o aplicación oficial; compatibilidad no confirmada.",11f))
            }
        }
        if(mediaMode in listOf("TDT","Pluto","Apps"))entertainment.addView(label("Vídeo deshabilitado si no hay confirmación continua de marcha P, freno, velocidad cero y restricciones OEM.",11f,amber))
    }
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private fun openVideo(url:String){
        stopVideo();radio.stop()
        if(!videoAllowed()){mediaStatus.text="Vídeo no autorizado: falta confirmación de estacionamiento/OEM";return}
        val host=Uri.parse(url).host?:return
        val base=host.removePrefix("www.")
        val w=android.webkit.WebView(this);video=w
        w.settings.javaScriptEnabled=true;w.settings.domStorageEnabled=true;w.settings.allowFileAccess=false;w.settings.allowContentAccess=false
        w.settings.setSupportMultipleWindows(false);w.settings.mediaPlaybackRequiresUserGesture=true
        w.settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        w.webViewClient=object:android.webkit.WebViewClient(){
            override fun shouldOverrideUrlLoading(view:android.webkit.WebView,request:android.webkit.WebResourceRequest):Boolean{
                val h=request.url.host.orEmpty()
                return !videoAllowed()||request.url.scheme!="https"||!(h==base||h.endsWith("."+base))
            }
        }
        entertainment.addView(button("Cerrar vídeo"){buildMedia()})
        entertainment.addView(w,LinearLayout.LayoutParams(-1,dp(230)));w.loadUrl(url)
        mediaStatus.text="Contenido oficial · sujeto a compatibilidad web y DRM"
    }
'''+s[j:]
    s=one(s,'if(mode=="Energía"){\n            graphContainer=col();chart=PowerChart();graphContainer.addView(chart,LinearLayout.LayoutParams(-1,dp(160)));contents.addView(graphContainer);gap(contents)\n            val rr=row();card(rr,"SALIDA MEDIDA","sessionOut",amber);gap(rr);card(rr,"ENTRADA MEDIDA","sessionIn",cyan);contents.addView(rr);gap(contents);contents.addView(detail);gap(contents)', '''if(mode=="Energía"||mode=="Gráficas"){
            if(mode=="Gráficas"){graphContainer=col();chart=PowerChart();graphContainer.addView(chart,LinearLayout.LayoutParams(-1,dp(120)));contents.addView(graphContainer);gap(contents)}
            contents.addView(detail);gap(contents)
            if(mode=="Gráficas")contents.addView(button("Historial MPAndroidChart"){startActivity(Intent(this,ChartActivity::class.java))})''')
    s=s.replace('graphPoints.clear();lastGraphNs=null;render()','graphPoints.clear();lastGraphNs=null;firstOdo=null;latestDistance=null;peakOut=null;peakIn=null;peakSpeed=null;render()')
    s=one(s,'"Estado"->{contents.addView(button("Modelo ilustrativo 3D · Filament")','"Prestaciones"->{contents.addView(label("Máximos observados durante la sesión. No es una prueba de aceleración.",12f))}\n                "Estado"->{contents.addView(button("Modelo ilustrativo 3D · Filament")')
    s=s.replace('"android.car.permission.CAR_INFO").filter','"android.car.permission.CAR_INFO","android.car.permission.CAR_EXTERIOR_ENVIRONMENT").filter')
    start=s.index('        if(mode=="Energía"){metric("sessionOut"');end=s.index('        else detail.text=when(mode)',start)
    s=s[:start]+'''        if(video!=null&&!videoAllowed()){stopVideo();if(::mediaStatus.isInitialized)mediaStatus.text="Vídeo detenido: estacionamiento/OEM no confirmado"}
        t.odometerKm?.let{odo->if(firstOdo==null)firstOdo=odo;firstOdo?.let{start->if(odo>=start)latestDistance=odo-start}}
        t.speedKmh?.let{peakSpeed=max(peakSpeed?:it,it)};t.dischargeKw?.let{peakOut=max(peakOut?:it,it)};t.incomingKw?.let{peakIn=max(peakIn?:it,it)}
        val hasEnergy=totals.intervals>0
        metric("sessionOut",fmt(totals.outKwh.takeIf{hasEnergy},"kWh",3));metric("sessionIn",fmt(totals.inKwh.takeIf{hasEnergy},"kWh",3));metric("net",fmt(totals.netKwh.takeIf{hasEnergy},"kWh",3))
        metric("power",fmt(if(t.dischargeKw!=null&&t.incomingKw!=null)t.dischargeKw-t.incomingKw else null,"kW"))
        metric("distance",fmt(latestDistance,"km"));metric("ext",fmt(t.exteriorC,"°C"))
        // Do not present a trip average from incomplete foreground sampling.
        metric("average","—")
        val ns=t.powerAtNs
        if(ns!=null&&ns!=lastGraphNs&&t.incomingKw!=null&&t.dischargeKw!=null){graphPoints.add(ns to (t.incomingKw-t.dischargeKw));while(graphPoints.size>120)graphPoints.removeAt(0);lastGraphNs=ns}
        if(mode=="Energía"||mode=="Gráficas"){
            detail.text="Salida: "+fmt(totals.outKwh.takeIf{hasEnergy},"kWh",3)+" | Entrada: "+fmt(totals.inKwh.takeIf{hasEnergy},"kWh",3)+"\\\\nBalance: "+fmt(totals.netKwh.takeIf{hasEnergy},"kWh",3)+"\\\\nEntrada no equivale a regeneración. Promedio de viaje no confirmado. Muestras parciales AAOS."
            if(mode=="Gráficas"&&::chart.isInitialized)chart.invalidate()
        }
'''.replace("\\\\n","\\n")+s[end:]
    s=one(s,'else detail.text=when(mode){"Estado"->','else detail.text=when(mode){"Prestaciones"->"Máxima velocidad observada: "+fmt(peakSpeed,"km/h",0)+"\\\\nPico de descarga: "+fmt(peakOut,"kW")+"\\\\nPico de entrada: "+fmt(peakIn,"kW");"Estado"->'.replace("\\\\n","\\n"))
    s=s.replace("override fun onPause(){ui.removeCallbacks(refresh);","override fun onPause(){ui.removeCallbacks(refresh);stopVideo();").replace("override fun onDestroy(){ui.removeCallbacks(refresh);","override fun onDestroy(){ui.removeCallbacks(refresh);stopVideo();")
    return s
edit(d+"MainActivity.kt",main)
print("Fusion layout and ARM packaging applied.")
