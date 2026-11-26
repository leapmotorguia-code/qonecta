import pathlib,sys
p=pathlib.Path(sys.argv[1])/"receiver-combined"
def edit(rel,fn):
    q=p/rel; s=q.read_text(); v=fn(s)
    if s==v: raise RuntimeError("No change "+rel)
    q.write_text(v)
def one(s,a,b):
    if s.count(a)!=1: raise RuntimeError("Expected one "+a[:100]+" found "+str(s.count(a)))
    return s.replace(a,b,1)
d="cockpit/src/main/java/es/drivecore/cockpit/"
edit("app/build.gradle.kts",lambda s:one(one(s,"versionCode = 9","versionCode = 10"),'versionName = "6.4-fusion-0.8"','versionName = "6.4-fusion-premium-0.9"'))
def main(s):
    s=one(s,'    private lateinit var entertainment: LinearLayout','''    private var dashboardMap: org.maplibre.android.maps.MapView? = null
    private var mapStarted=false
    private var mapResumed=false
    private var mapVisible=false
    private lateinit var entertainment: LinearLayout''')
    s=one(s,'private val bg=Color.rgb(4,22,34); private val panel=Color.rgb(7,48,69)','private val bg=Color.rgb(3,17,27); private val panel=Color.rgb(5,30,44)')
    s=one(s,'private fun shape(color:Int,stroke:Int=0):GradientDrawable=GradientDrawable().apply{setColor(color);cornerRadius=dp(7).toFloat();if(stroke!=0)setStroke(dp(1),stroke)}','''private fun shape(color:Int,stroke:Int=0):GradientDrawable=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(color,Color.rgb((Color.red(color)*0.62).toInt(),(Color.green(color)*0.72).toInt(),(Color.blue(color)*0.78).toInt()))).apply{cornerRadius=dp(10).toFloat();if(stroke!=0)setStroke(dp(1),stroke)}
    private fun section(s:String)=label(s,14f,cyan,true).apply{letterSpacing=.06f}
    private fun heightDp()=(resources.displayMetrics.heightPixels/resources.displayMetrics.density).toInt()
    private fun compact()=heightDp()<620''')
    s=one(s,'textSize=12f;setTextColor(white);background=shape(Color.rgb(7,72,101),Color.rgb(27,162,196));minHeight=dp(38);minimumHeight=dp(38);','textSize=13f;setTextColor(white);background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x5541f6f0),shape(Color.rgb(8,56,78),Color.rgb(27,162,196)),null);minHeight=dp(44);minimumHeight=dp(44);')
    a=s.index("    private fun card("); b=s.index("    private fun pickMode(",a)
    s=s[:a]+'''    private fun card(parent:LinearLayout,title:String,key:String,color:Int=white){
        val box=row();box.background=shape(Color.rgb(8,43,62),Color.rgb(26,143,168));box.setPadding(dp(7),dp(6),dp(7),dp(6))
        val stripe=View(this).apply{background=shape(color)}
        box.addView(stripe,LinearLayout.LayoutParams(dp(4),dp(if(compact())36 else 48)));gap(box,7)
        val v=col();val caption=label(title,if(compact())10f else 12f,white,true);caption.maxLines=2;v.addView(caption)
        val t=label("—",if(compact())21f else 25f,color,true);t.maxLines=1;t.setAutoSizeTextTypeUniformWithConfiguration(12,if(compact())21 else 25,1,android.util.TypedValue.COMPLEX_UNIT_SP)
        metrics[key]=t;v.addView(t,LinearLayout.LayoutParams(-1,dp(if(compact())29 else 34)))
        box.addView(v,LinearLayout.LayoutParams(0,-2,1f));parent.addView(box,LinearLayout.LayoutParams(0,-2,1f))
    }
'''+s[b:]
    s=s.replace('dp(39)','dp(44)')
    s=one(s,'val header=row();header.addView(label("DRIVECORE FUSION 6.4",14f,cyan,true),LinearLayout.LayoutParams(0,-2,1f))','''val header=row();header.addView(label("DRIVECORE  /  FUSION PREMIUM",18f,white,true),LinearLayout.LayoutParams(0,-2,1f))
        header.addView(label("C10 REEV   ·   ",12f,cyan,true))''')
    s=one(s,'left.addView(label("NAVEGACIÓN",12f,cyan,true));gap(left,4)','left.addView(section("NAVEGACIÓN"));gap(left,5)')
    s=one(s,'center.addView(label("TELEMETRÍA DIGITAL",12f,cyan,true));gap(center,4)','center.addView(section("TELEMETRÍA DIGITAL"));gap(center,5)')
    s=one(s,'controls(left,"Mapa integrado · MapLibre" to {startActivity(Intent(this,MapActivity::class.java))})','''val mapCard=col().apply{background=shape(Color.rgb(8,35,51),Color.rgb(28,137,164));setPadding(dp(4),dp(4),dp(4),dp(4))}
        mapCard.addView(label("MAPA INTEGRADO · MAPLIBRE",11f,cyan,true))
        val mapFrame=FrameLayout(this);mapCard.addView(mapFrame,LinearLayout.LayoutParams(-1,dp(if(compact())165 else 225)))
        val placeholder=Button(this).apply{text="Cargar mapa";isAllCaps=false;setTextColor(white);background=shape(Color.rgb(7,43,62),Color.rgb(24,103,124));setOnClickListener{loadDashboardMap(mapFrame)}}
        mapFrame.addView(placeholder,FrameLayout.LayoutParams(-1,-1))
        left.addView(mapCard);gap(left,5)
        controls(left,"Ampliar mapa" to {startActivity(Intent(this,MapActivity::class.java))})
        left.addView(label("Visor de mapas · sin ruta ni posición confirmadas",11f,muted))''')
    s=one(s,'val visual=col();gauge=Gauge();visual.addView(gauge,LinearLayout.LayoutParams(-1,dp(104)))','val visual=col();gauge=Gauge();visual.addView(gauge,LinearLayout.LayoutParams(-1,dp(if(compact())128 else 180)))')
    s=one(s,'val car=ImageView(this).apply{setImageResource(R.drawable.car_silhouette);contentDescription="Silueta ilustrativa";scaleType=ImageView.ScaleType.FIT_CENTER};visual.addView(car,LinearLayout.LayoutParams(-1,dp(32)))','''val car=ImageView(this).apply{setImageResource(R.drawable.premium_vehicle);contentDescription="Representación ilustrativa del vehículo";scaleType=ImageView.ScaleType.FIT_CENTER;setOnClickListener{startActivity(Intent(this@MainActivity,Vehicle3dActivity::class.java))}};visual.addView(car,LinearLayout.LayoutParams(-1,dp(if(compact())46 else 58)))''')
    s=one(s,'center.addView(r3);gap(center,4)','''// Keep these values in the diagnostic/detail view instead of crowding the cockpit.
        // Their existing metric TextViews remain available for render updates.
        gap(center,4)''')
    s=one(s,'right=col();center.addView(right,LinearLayout.LayoutParams(-1,dp(250)))','right=col();center.addView(right,LinearLayout.LayoutParams(-1,dp(if(compact())225 else 290)))')
    s=one(s,'entertainment.addView(label("ENTRETENIMIENTO",12f,cyan,true));gap(entertainment,4)','entertainment.addView(section("ENTRETENIMIENTO"));gap(entertainment,5)')
    s=one(s,'entertainment.addView(label(radio.decoderReport(),11f))','''val artwork=RadioArtwork()
                entertainment.addView(artwork,LinearLayout.LayoutParams(-1,dp(if(compact())112 else 150)))
                gap(entertainment,5)
                entertainment.addView(label(radio.decoderReport(),11f))''')
    s=one(s,'"Reproducir" to {','"▶  Reproducir" to {')
    s=one(s,'},"Parar" to {radio.stop();radioStatus="Radio detenida";mediaStatus.text=radioStatus})','},"■  Parar" to {radio.stop();radioStatus="Radio detenida";mediaStatus.text=radioStatus})')
    s=one(s,'if(mode=="Gráficas"){graphContainer=col();chart=PowerChart();graphContainer.addView(chart,LinearLayout.LayoutParams(-1,dp(120)));contents.addView(graphContainer);gap(contents)}','graphContainer=col();chart=PowerChart();graphContainer.addView(chart,LinearLayout.LayoutParams(-1,dp(if(compact())115 else 155)));contents.addView(graphContainer);gap(contents,4)')
    s=one(s,'if(mode=="Gráficas"&&::chart.isInitialized)chart.invalidate()','if(::chart.isInitialized)chart.invalidate()')
    s=one(s,'private fun videoAllowed():Boolean {','''private fun loadDashboardMap(frame:FrameLayout){
        if(dashboardMap!=null)return
        try{
            org.maplibre.android.MapLibre.getInstance(this)
            val mv=org.maplibre.android.maps.MapView(this)
            dashboardMap=mv;frame.removeAllViews();frame.addView(mv,FrameLayout.LayoutParams(-1,-1))
            mv.onCreate(null);mv.onStart();mapStarted=true;mv.onResume();mapResumed=true
            mv.getMapAsync { map->
                if(dashboardMap!==mv)return@getMapAsync
                map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                map.cameraPosition=org.maplibre.android.camera.CameraPosition.Builder().target(org.maplibre.android.geometry.LatLng(40.0,-3.0)).zoom(5.0).build()
            }
        }catch(e:Exception){closeDashboardMap();frame.removeAllViews();frame.addView(label("Mapa no disponible · "+e.javaClass.simpleName))}
        catch(e:LinkageError){closeDashboardMap();frame.removeAllViews();frame.addView(label("Motor de mapa no compatible"))}
    }
    private fun closeDashboardMap(){
        val mv=dashboardMap;dashboardMap=null
        if(mapResumed)runCatching{mv?.onPause()};mapResumed=false
        if(mapStarted)runCatching{mv?.onStop()};mapStarted=false
        runCatching{mv?.onDestroy()}
    }
    override fun onStart(){super.onStart();dashboardMap?.let{if(!mapStarted){it.onStart();mapStarted=true}}}
    override fun onStop(){dashboardMap?.let{if(mapStarted){it.onStop();mapStarted=false}};super.onStop()}
    override fun onLowMemory(){super.onLowMemory();dashboardMap?.onLowMemory()}
    private fun videoAllowed():Boolean {''')
    s=one(s,'override fun onResume(){super.onResume();','override fun onResume(){super.onResume();dashboardMap?.let{if(!mapResumed){it.onResume();mapResumed=true}};')
    s=one(s,'override fun onPause(){ui.removeCallbacks(refresh);','override fun onPause(){dashboardMap?.let{if(mapResumed){it.onPause();mapResumed=false}};ui.removeCallbacks(refresh);')
    s=one(s,'override fun onDestroy(){ui.removeCallbacks(refresh);','override fun onDestroy(){closeDashboardMap();ui.removeCallbacks(refresh);')
    # New Canvas visuals. No fictitious samples or route lines.
    a=s.index("    inner class Gauge:"); s=s[:a]+'''    inner class Gauge:View(this@MainActivity){
        var speed:Double?=null
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c:Canvas){
            val w=width.toFloat();val h=height.toFloat()
            val r=min(w*.41f,h*.43f);if(r<4)return
            val x=w/2;val y=h*.55f;val oval=RectF(x-r,y-r,x+r,y+r)
            p.style=Paint.Style.FILL;p.shader=RadialGradient(x,y,r*1.2f,intArrayOf(0xff103d4a.toInt(),0xff071c2b.toInt(),0xff051725.toInt()),null,Shader.TileMode.CLAMP);c.drawCircle(x,y,r*1.08f,p);p.shader=null
            p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.ROUND;p.strokeWidth=r*.11f
            p.color=0xff214958.toInt();c.drawArc(oval,145f,250f,false,p)
            p.shader=SweepGradient(x,y,intArrayOf(0xff29b8ff.toInt(),0xff28f4e7.toInt(),0xffc9f542.toInt(),0xff29b8ff.toInt()),null)
            p.alpha=155;c.drawArc(oval,145f,250f,false,p);p.alpha=255;p.shader=null
            speed?.takeIf{it.isFinite()&&it>=0}?.let{
                p.color=cyan;p.strokeWidth=r*.13f;c.drawArc(oval,145f,(it.coerceAtMost(200.0)/200*250).toFloat(),false,p)
            }
            p.strokeCap=Paint.Cap.BUTT
            for(i in 0..40){
                val a=Math.toRadians(145.0+i*250.0/40)
                val major=i%4==0
                val a1=r*(if(major).79f else .86f);val a2=r*.91f
                p.color=if(major)white else muted;p.strokeWidth=if(major)dp(2).toFloat() else dp(1).toFloat()
                c.drawLine(x+cos(a).toFloat()*a1,y+sin(a).toFloat()*a1,x+cos(a).toFloat()*a2,y+sin(a).toFloat()*a2,p)
                if(i%8==0){p.style=Paint.Style.FILL;p.textSize=r*.15f;p.textAlign=Paint.Align.CENTER;c.drawText((i*5).toString(),x+cos(a).toFloat()*r*.64f,y+sin(a).toFloat()*r*.64f+r*.05f,p);p.style=Paint.Style.STROKE}
            }
            p.style=Paint.Style.FILL;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD
            p.color=white;p.textSize=r*.63f;c.drawText(speed?.roundToInt()?.toString()?:"—",x,y+r*.17f,p)
            p.color=cyan;p.textSize=r*.17f;c.drawText("km/h",x,y+r*.43f,p)
            p.color=muted;p.typeface=Typeface.DEFAULT;p.textSize=r*.11f;c.drawText(if(speed==null)"SIN LECTURA" else "AAOS",x,y+r*.62f,p)
        }
    }
    inner class RadioArtwork:View(this@MainActivity){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c:Canvas){
            val w=width.toFloat();val h=height.toFloat();if(w<1||h<1)return
            p.style=Paint.Style.FILL;p.shader=LinearGradient(0f,0f,w,h,intArrayOf(0xff31165d.toInt(),0xff0b294c.toInt(),0xff005270.toInt()),null,Shader.TileMode.CLAMP);c.drawRoundRect(0f,0f,w,h,dp(10).toFloat(),dp(10).toFloat(),p);p.shader=null
            val x=w/2;val y=h*.38f;val r=min(w,h)*.17f
            p.color=cyan;p.style=Paint.Style.STROKE;p.strokeWidth=dp(2).toFloat()
            for(i in 1..3){val a=r*i/3;c.drawArc(x-a,y-a,x+a,y+a,205f,130f,false,p);c.drawArc(x-a,y-a,x+a,y+a,25f,130f,false,p)}
            p.style=Paint.Style.FILL;p.color=white;c.drawCircle(x,y,r*.12f,p)
            p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=min(dp(20).toFloat(),w*.08f);c.drawText("RADIO ONLINE",x,h*.79f,p)
        }
    }
    inner class PowerChart:View(this@MainActivity){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c:Canvas){
            val w=width.toFloat();val h=height.toFloat();if(w<dp(80)||h<dp(70))return
            p.style=Paint.Style.FILL;p.color=0xff071c2b.toInt();c.drawRoundRect(0f,0f,w,h,dp(8).toFloat(),dp(8).toFloat(),p)
            val l=dp(34).toFloat();val rr=w-dp(8);val top=dp(28).toFloat();val bottom=h-dp(18);val mid=(top+bottom)/2
            val scale=max(10.0,graphPoints.maxOfOrNull{abs(it.second)}?:0.0)
            p.textAlign=Paint.Align.LEFT;p.color=white;p.textSize=dp(11).toFloat();c.drawText("FLUJO DE ENERGÍA · kW",dp(8).toFloat(),dp(15).toFloat(),p)
            for(i in 0..4){val yy=top+(bottom-top)*i/4;p.color=0xff244655.toInt();p.strokeWidth=1f;c.drawLine(l,yy,rr,yy,p);p.color=muted;p.textSize=dp(9).toFloat();c.drawText((scale*(1-i/2.0)).roundToInt().toString(),dp(3).toFloat(),yy+dp(3),p)}
            if(graphPoints.size<2){p.color=muted;p.textAlign=Paint.Align.CENTER;p.textSize=dp(12).toFloat();c.drawText("Esperando muestras reales AAOS",(l+rr)/2,mid-dp(6),p);return}
            val start=graphPoints.first().first;val duration=max(1L,graphPoints.last().first-start)
            fun xx(n:Long)=l+(rr-l)*(n-start).toFloat()/duration
            fun yy(v:Double)=mid+(v/scale*((bottom-top)/2)).toFloat()
            for(i in 1 until graphPoints.size){
                val a=graphPoints[i-1];val b=graphPoints[i];if(b.first-a.first !in 1L..3_000_000_000L)continue
                val x1=xx(a.first);val x2=xx(b.first);val y1=yy(a.second);val y2=yy(b.second)
                fun seg(xa:Float,ya:Float,xb:Float,yb:Float,color:Int){
                    val path=Path().apply{moveTo(xa,mid);lineTo(xa,ya);lineTo(xb,yb);lineTo(xb,mid);close()}
                    p.color=color;p.alpha=35;p.style=Paint.Style.FILL;c.drawPath(path,p);p.alpha=255;p.strokeWidth=dp(2).toFloat();c.drawLine(xa,ya,xb,yb,p)
                }
                if(a.second*b.second<0){val f=(abs(a.second)/(abs(a.second)+abs(b.second))).toFloat();val xc=x1+(x2-x1)*f;seg(x1,y1,xc,mid,if(a.second>0)cyan else amber);seg(xc,mid,x2,y2,if(b.second>0)cyan else amber)}
                else seg(x1,y1,x2,y2,if(a.second+b.second>0)cyan else amber)
            }
            p.textSize=dp(9).toFloat();p.textAlign=Paint.Align.LEFT;p.color=amber;c.drawText("Salida ↑",l,h-dp(5),p);p.color=cyan;p.textAlign=Paint.Align.RIGHT;c.drawText("Entrada ↓ · Ahora",rr,h-dp(5),p)
        }
    }
}
'''
    return s
edit(d+"MainActivity.kt",main)
vehicle='''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="280dp" android:height="90dp" android:viewportWidth="280" android:viewportHeight="90">
<path android:fillColor="#133E4E" android:pathData="M12,74 Q140,88 270,74 Q140,65 12,74"/>
<path android:fillColor="#2D6077" android:strokeColor="#79CDDA" android:strokeWidth="1.5" android:pathData="M17,62 L21,42 Q25,34 43,30 L80,25 Q95,7 119,7 L193,7 Q218,9 239,27 L258,33 Q270,40 270,54 L265,68 L17,68 Z"/>
<path android:fillColor="#102B3F" android:pathData="M75,28 Q92,11 119,11 L192,11 Q213,13 232,28 L75,28 Z"/>
<path android:fillColor="#477B92" android:pathData="M83,25 L108,12 L126,12 L126,25 Z M133,12 L162,12 L162,25 L133,25 Z M168,12 L194,12 Q211,15 224,25 L168,25 Z"/>
<path android:fillColor="#5F96AC" android:pathData="M26,42 Q63,35 89,33 L235,33 L262,40 L248,47 L28,47 Z"/>
<path android:fillColor="#214B60" android:pathData="M21,50 L257,50 L265,59 L260,67 L20,67 Z"/>
<path android:strokeColor="#7BAABD" android:strokeWidth="1" android:pathData="M89,34 L91,62 M165,34 L164,62 M210,35 L211,60 M108,40 L120,40 M179,40 L191,40"/>
<path android:fillColor="#D9F7FF" android:pathData="M23,39 L45,36 L52,39 L37,43 L23,43 Z"/>
<path android:fillColor="#FF3549" android:pathData="M235,36 L264,38 L268,41 L268,45 L236,42 Z"/>
<path android:fillColor="#0A1823" android:pathData="M39,65 A21,21 0,1 1,81 65 A21,21 0,1 1,39 65 M208,65 A21,21 0,1 1,250 65 A21,21 0,1 1,208 65"/>
<path android:fillColor="#96B3C3" android:strokeColor="#C8E6EF" android:strokeWidth="1" android:pathData="M45,65 A15,15 0,1 1,75 65 A15,15 0,1 1,45 65 M214,65 A15,15 0,1 1,244 65 A15,15 0,1 1,214 65"/>
<path android:fillColor="#20394A" android:pathData="M50,65 A10,10 0,1 1,70 65 A10,10 0,1 1,50 65 M219,65 A10,10 0,1 1,239 65 A10,10 0,1 1,219 65"/>
<path android:strokeColor="#9BE5EF" android:strokeWidth="2" android:pathData="M60,52 L60,78 M47,65 L73,65 M51,56 L69,74 M51,74 L69,56 M229,52 L229,78 M216,65 L242,65 M220,56 L238,74 M220,74 L238,56"/>
<path android:strokeColor="#43E6DE" android:strokeWidth="2" android:pathData="M84,72 L205,72"/>
</vector>'''
(p/"cockpit/src/main/res/drawable/premium_vehicle.xml").write_text(vehicle)
print("Premium native cockpit patch applied")
