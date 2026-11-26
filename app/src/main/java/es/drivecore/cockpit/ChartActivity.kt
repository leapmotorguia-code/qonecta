package es.drivecore.cockpit
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

class ChartActivity:Activity(){
    private lateinit var history:HistoryStore
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        history=HistoryStore(this)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16);setBackgroundColor(Color.rgb(8,21,31))}
        val title=TextView(this).apply{text="Historial AAOS · últimas 30 muestras";setTextColor(Color.WHITE);textSize=18f}
        root.addView(Button(this).apply{text="Volver";setOnClickListener{finish()}})
        root.addView(title)
        val chart=LineChart(this)
        root.addView(chart,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        chart.description.isEnabled=false
        chart.axisRight.isEnabled=false
        chart.axisLeft.textColor=Color.WHITE
        chart.xAxis.textColor=Color.WHITE
        chart.legend.textColor=Color.WHITE
        chart.setNoDataText("No hay muestras con potencia confirmada")
        chart.setNoDataTextColor(Color.WHITE)
        history.recentRows({ result->
            val rows=result.asReversed()
            val datasets=ArrayList<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()
            if(rows.isNotEmpty()){
                val start=rows.first().utcMillis
                val x:(HistoryStore.Sample)->Float={((it.utcMillis-start)/1000.0).toFloat()}
                fun addSegments(name:String,color:Int,value:(HistoryStore.Sample)->Double?){
                    var entries=ArrayList<Entry>()
                    var prev:HistoryStore.Sample?=null
                    var first=true
                    fun flush(){if(entries.isNotEmpty()){val ds=LineDataSet(entries,if(first)name else "").apply{this.color=color;setCircleColor(color);setDrawValues(false);lineWidth=2f;circleRadius=2f};datasets.add(ds);first=false;entries=ArrayList()}}
                    for(s in rows){val p=prev;val v=value(s);if(p!=null&&(p.session!=s.session||s.utcMillis-p.utcMillis !in 1..25000))flush();if(v==null||!v.isFinite()){flush()}else entries.add(Entry(x(s),v.toFloat()));prev=s}
                    flush()
                }
                addSegments("Salida kW",Color.rgb(255,190,95)){it.outKw}
                addSegments("Entrada kW",Color.rgb(97,229,232)){it.inKw}
                chart.xAxis.valueFormatter=object:ValueFormatter(){override fun getFormattedValue(v:Float)=v.toInt().toString()+" s"}
            }
            if(datasets.isNotEmpty())chart.data=LineData(datasets)
            chart.invalidate()
        },{title.text=it})
    }
    override fun onDestroy(){history.close();super.onDestroy()}
}
