package es.drivecore.cockpit
import android.app.Activity
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import android.widget.*
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Original simplified geometric vehicle, not an exact model or sensor visualization. */
class Vehicle3dActivity:Activity(){
    private var viewer:ModelViewer?=null
    private var active=false
    private val frame=object:Choreographer.FrameCallback{
        override fun doFrame(t:Long){if(!active)return;viewer?.render(t);Choreographer.getInstance().postFrameCallback(this)}
    }
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        root.addView(Button(this).apply{text="Volver · modelo 3D ilustrativo (sin sensores)";setOnClickListener{finish()}})
        setContentView(root)
        try{
            Utils.init()
            val surface=SurfaceView(this)
            root.addView(surface,LinearLayout.LayoutParams(-1,0,1f))
            val v=ModelViewer(surface)
            viewer=v
            surface.setOnTouchListener(v)
            val mesh=ByteBuffer.allocateDirect(168).order(ByteOrder.LITTLE_ENDIAN)
            val vertices=floatArrayOf(-.5f,-.5f,-.5f,.5f,-.5f,-.5f,.5f,.5f,-.5f,-.5f,.5f,-.5f,-.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f,.5f,-.5f,.5f,.5f)
            vertices.forEach{mesh.putFloat(it)}
            intArrayOf(0,2,1,0,3,2,4,5,6,4,6,7,0,4,7,0,7,3,1,2,6,1,6,5,3,7,6,3,6,2,0,1,5,0,5,4).forEach{mesh.putShort(it.toShort())}
            mesh.rewind()
            val json="""{"asset":{"version":"2.0"},"extensionsUsed":["KHR_materials_unlit"],"scene":0,"scenes":[{"nodes":[0,1,2,3,4,5]}],"nodes":[{"mesh":0,"scale":[1.8,0.65,4.4],"translation":[0,0.6,0]},{"mesh":1,"scale":[1.55,0.8,2.65],"translation":[0,1.25,-0.18]},{"mesh":2,"scale":[0.4,0.66,0.8],"translation":[-0.92,0.32,1.3]},{"mesh":2,"scale":[0.4,0.66,0.8],"translation":[0.92,0.32,1.3]},{"mesh":2,"scale":[0.4,0.66,0.8],"translation":[-0.92,0.32,-1.3]},{"mesh":2,"scale":[0.4,0.66,0.8],"translation":[0.92,0.32,-1.3]}],"buffers":[{"uri":"vehicle.bin","byteLength":168}],"bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":96,"target":34962},{"buffer":0,"byteOffset":96,"byteLength":72,"target":34963}],"accessors":[{"bufferView":0,"componentType":5126,"count":8,"type":"VEC3","min":[-0.5,-0.5,-0.5],"max":[0.5,0.5,0.5]},{"bufferView":1,"componentType":5123,"count":36,"type":"SCALAR"}],"materials":[{"pbrMetallicRoughness":{"baseColorFactor":[0.02,0.5,0.59,1]},"extensions":{"KHR_materials_unlit":{}}},{"pbrMetallicRoughness":{"baseColorFactor":[0.14,0.25,0.3,1]},"extensions":{"KHR_materials_unlit":{}}},{"pbrMetallicRoughness":{"baseColorFactor":[0.06,0.07,0.08,1]},"extensions":{"KHR_materials_unlit":{}}}],"meshes":[{"primitives":[{"attributes":{"POSITION":0},"indices":1,"material":0}]},{"primitives":[{"attributes":{"POSITION":0},"indices":1,"material":1}]},{"primitives":[{"attributes":{"POSITION":0},"indices":1,"material":2}]}]}"""
            v.loadModelGltf(ByteBuffer.wrap(json.toByteArray(Charsets.UTF_8))){uri->if(uri=="vehicle.bin")mesh else null}
            v.transformToUnitCube()
        }catch(e:Exception){root.addView(TextView(this).apply{text="3D no disponible: "+e.javaClass.simpleName})}
        catch(e:LinkageError){root.addView(TextView(this).apply{text="Biblioteca gráfica no compatible"})}
    }
    override fun onResume(){super.onResume();active=true;Choreographer.getInstance().postFrameCallback(frame)}
    override fun onPause(){active=false;Choreographer.getInstance().removeFrameCallback(frame);super.onPause()}
    override fun onDestroy(){active=false;Choreographer.getInstance().removeFrameCallback(frame);viewer=null;super.onDestroy()}
}
