package es.drivecore.cockpit
import android.app.Activity
import android.os.Bundle
import android.widget.*
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

/** Map viewer, not a routing/navigation engine. User controls camera. */
class MapActivity:Activity(){
    private var mapView:MapView?=null
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        root.addView(Button(this).apply{text="Volver a instrumentación · Mapa sin rutas";setOnClickListener{finish()}})
        setContentView(root)
        try{
            MapLibre.getInstance(this)
            val view=MapView(this)
            mapView=view
            root.addView(view,LinearLayout.LayoutParams(-1,0,1f))
            view.onCreate(state)
            view.getMapAsync { map ->
                map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                if(state==null)map.cameraPosition=CameraPosition.Builder().target(LatLng(40.0,-3.0)).zoom(5.0).build()
            }
        }catch(e:Exception){root.addView(TextView(this).apply{text="Mapa no disponible: "+e.javaClass.simpleName})}
        catch(e:LinkageError){root.addView(TextView(this).apply{text="Motor gráfico no compatible"})}
    }
    override fun onStart(){super.onStart();mapView?.onStart()}
    override fun onResume(){super.onResume();mapView?.onResume()}
    override fun onPause(){mapView?.onPause();super.onPause()}
    override fun onStop(){mapView?.onStop();super.onStop()}
    override fun onDestroy(){mapView?.onDestroy();mapView=null;super.onDestroy()}
    override fun onLowMemory(){super.onLowMemory();mapView?.onLowMemory()}
    override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);mapView?.onSaveInstanceState(out)}
}
