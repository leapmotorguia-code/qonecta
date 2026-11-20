package es.c10.complemento.media
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
object MediaStreamingLauncher {
 val platforms=linkedMapOf("Google" to "https://www.google.com", "Netflix" to "https://www.netflix.com", "Prime Video" to "https://www.primevideo.com", "HBO / Max" to "https://www.max.com", "Pluto TV" to "https://pluto.tv", "Spotify" to "https://open.spotify.com")
 fun launch(activity:Activity,url:String) {
  AlertDialog.Builder(activity).setTitle("Uso estacionado").setMessage("Abre una aplicación o navegador externo. No garantiza reproducción ni ofrece acceso a una suscripción. Confirma con el vehículo estacionado.")
   .setNegativeButton("Cancelar",null).setPositiveButton("Abrir") { _,_->
    try { activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }
    catch(e:Exception){AlertDialog.Builder(activity).setMessage("No hay aplicación compatible para abrir este enlace.").setPositiveButton("Aceptar",null).show()}
   }.show()
 }
}
