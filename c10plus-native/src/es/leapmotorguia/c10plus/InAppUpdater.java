package es.leapmotorguia.c10plus;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/**
 * Manual, opt-in updater for the user's own UNIT C10 Plus builds.
 * Only accepts our public update-channel manifest and SAME PACKAGE / SIGNER.
 * Does NOT silently install, contact car APIs, bypass Android policy or use
 * system privileges. The APK must be published to GitHub Releases beforehand.
 */
public final class InAppUpdater {
    private static final String MANIFEST_URL =
        "https://raw.githubusercontent.com/leapmotorguia-code/qonecta/main/updates/unit-c10-plus.json";
    private static final String RELEASE_PREFIX =
        "https://github.com/leapmotorguia-code/qonecta/releases/download/";
    private static final String PACKAGE = "com.tencent.qqmusiccar";
    private static final String SIGNER =
        "6861d639e6f428e2d29e24501e8bb05e58de606f927ef24f5ebe8a5952da56f6";
    private static final int MAX_MANIFEST = 16384;
    private static final long MAX_APK_BYTES = 230L*1024*1024;
    private static final String PREF = "c10plus_inapp_update_v1";

    private final Activity activity;
    private final LinearLayout host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences preferences;
    private final DownloadManager downloads;
    private TextView status, details;
    private Button check, fetch, install;
    private JSONObject candidate;
    private long downloadId = -1;
    private File destination;
    private volatile boolean busy, destroyed, verifying, validated;
    private final Runnable poller = new Runnable(){
        @Override public void run(){
            if(destroyed)return;
            if(downloadId>0)checkDownload();
            handler.postDelayed(this,1600);
        }
    };
    public InAppUpdater(Activity a, LinearLayout target) {
        activity=a;host=target;
        preferences=a.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        downloads=(DownloadManager)a.getSystemService(Context.DOWNLOAD_SERVICE);
        build();
        handler.post(poller);
        recoverPriorDownload();
    }
    private TextView label(String s,int size,int color) {
        TextView t=new TextView(activity);
        t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setPadding(dp(12),dp(8),dp(12),dp(8));
        return t;
    }
    private int dp(int n){return (int)(n*activity.getResources().getDisplayMetrics().density+0.5f);}
    private Button button(String name) {
        Button b=new Button(activity);b.setAllCaps(false);b.setText(name);return b;
    }
    private void build() {
        host.addView(label("ACTUALIZACIONES DE UNIT · DESDE EL COCHE",19,0xff14e4bb));
        status=label("Versión instalada: "+versionText(),16,0xffe9f6fa);
        host.addView(status);
        details=label("El sistema buscará solo versiones publicadas de nuestro proyecto. "+
            "No instala archivos sin tu consentimiento. Las próximas versiones deben "+
            "estar firmadas con el mismo certificado que esta APK.",14,0xffafc8d3);
        host.addView(details);
        check=button("Comprobar nuevas versiones");
        check.setOnClickListener(v->checkManifest());
        host.addView(check,new LinearLayout.LayoutParams(-1,dp(49)));
        fetch=button("Descargar actualización aquí");
        fetch.setEnabled(false);fetch.setVisibility(View.GONE);
        fetch.setOnClickListener(v->downloadUpdate());
        host.addView(fetch,new LinearLayout.LayoutParams(-1,dp(49)));
        install=button("Instalar APK descargada y verificada");
        install.setEnabled(false);install.setVisibility(View.GONE);
        install.setOnClickListener(v->launchInstaller());
        host.addView(install,new LinearLayout.LayoutParams(-1,dp(49)));
        host.addView(label(
            "1. Comprobar versión.\n"+
            "2. Descargar desde el servidor oficial de nuestras APK.\n"+
            "3. Verificar SHA-256, paquete y certificado.\n"+
            "4. Autorizar la instalación en Android.\n\n"+
            "Si el coche bloquea instaladores de origen desconocido, "+
            "se mostrará el ajuste oficial. No se modifica LeapOS.",14,0xffa9c0cc));
    }
    private String versionText(){
        try{
            PackageInfo p=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);
            return p.versionName+" ("+p.getLongVersionCode()+")";
        }catch(Throwable e){return "desconocida";}
    }
    private int localVersion(){
        try{
            return (int)Math.min(Integer.MAX_VALUE,activity.getPackageManager()
                .getPackageInfo(activity.getPackageName(),0).getLongVersionCode());
        }catch(Throwable e){return 0;}
    }
    private void message(String s) {
        if(destroyed)return;
        status.setText(s);
    }
    private void showFailure(String s){
        if(destroyed)return;
        busy=false;check.setEnabled(true);
        message(s);
        details.setText("No se ha instalado ningún archivo. Puedes volver a comprobar.");
    }
    private static String hex(byte[] bytes) {
        StringBuilder s=new StringBuilder(bytes.length*2);
        for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));
        return s.toString();
    }
    private static String readManifest() throws Exception {
        URL url=new URL(MANIFEST_URL);
        if(!"https".equalsIgnoreCase(url.getProtocol()) ||
            !"raw.githubusercontent.com".equalsIgnoreCase(url.getHost()))
            throw new SecurityException("origen de actualizaciones incorrecto");
        HttpsURLConnection c=(HttpsURLConnection)url.openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(12000);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("User-Agent","UNIT-C10-Plus-Updater/1.0");
        try {
            if(c.getResponseCode()!=200)throw new Exception("servidor respondió "+c.getResponseCode());
            try(InputStream in=c.getInputStream();ByteArrayOutputStream result=new ByteArrayOutputStream()){
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1){
                    if(result.size()+n>MAX_MANIFEST)throw new SecurityException("manifiesto demasiado grande");
                    result.write(buffer,0,n);
                }
                return result.toString("UTF-8");
            }
        } finally {c.disconnect();}
    }
    private static boolean validManifest(JSONObject m,boolean requiresApk) {
        if(m.optInt("schema")!=1 || !"stable".equals(m.optString("channel")) ||
           !PACKAGE.equals(m.optString("packageName")) ||
           !SIGNER.equalsIgnoreCase(m.optString("signerSha256")))return false;
        if(!requiresApk)return true;
        String url=m.optString("apkUrl");
        String digest=m.optString("sha256");
        return url.startsWith(RELEASE_PREFIX) && !url.contains("..") &&
            digest.matches("[0-9a-fA-F]{64}") && m.optInt("versionCode")>0;
    }
    public void checkManifest(){
        if(busy || destroyed)return;
        busy=true;validated=false;candidate=null;
        check.setEnabled(false);fetch.setVisibility(View.GONE);install.setVisibility(View.GONE);
        message("Comprobando el canal oficial...");
        new Thread(()->{
            try{
                JSONObject m=new JSONObject(readManifest());
                if(!validManifest(m,false))throw new SecurityException("manifest no válido");
                boolean published=m.optBoolean("published",false);
                int v=m.optInt("versionCode",0);
                if(published&&!validManifest(m,true))
                    throw new SecurityException("archivo o firma de actualización no válidos");
                activity.runOnUiThread(()->{
                    if(destroyed)return;
                    busy=false;check.setEnabled(true);
                    if(!published || v<=localVersion()){
                        message("Sin actualización nueva publicada.");
                        details.setText("Versión instalada: "+versionText()+
                            "\nCanal de actualizaciones disponible; no hay una versión superior publicada.");
                    }else{
                        candidate=m;
                        message("Nueva versión: "+m.optString("versionName")+" ("+v+")");
                        details.setText(m.optString("notes","Versión disponible para descargar."));
                        fetch.setEnabled(true);fetch.setVisibility(View.VISIBLE);
                    }
                });
            }catch(Exception e){
                activity.runOnUiThread(()->showFailure(
                    "No se pudo comprobar: "+e.getClass().getSimpleName()+
                    (e.getMessage()==null?"":" · "+e.getMessage())));
            }
        },"c10plus-update-manifest").start();
    }
    private void recoverPriorDownload() {
        long id=preferences.getLong("downloadId",-1);
        String cached=preferences.getString("candidate","");
        String filename=preferences.getString("filename","");
        if(id<=0 || cached.isEmpty() || filename.isEmpty())return;
        try{
            JSONObject m=new JSONObject(cached);
            if(!validManifest(m,true) || m.optInt("versionCode")<=localVersion())return;
            if(!filename.matches("UNIT-C10-Plus-[0-9]+\\.apk"))return;
            File dir=activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if(dir==null)return;
            candidate=m;
            downloadId=id;
            destination=new File(dir,filename);
            message("Recuperando estado de descarga anterior...");
            checkDownload();
        }catch(Exception ignored){}
    }
    private void downloadUpdate(){
        if(destroyed||busy||candidate==null||downloads==null)return;
        if(!validManifest(candidate,true)||candidate.optInt("versionCode")<=localVersion()){
            showFailure("Actualización no válida");return;
        }
        try{
            int v=candidate.optInt("versionCode");
            String name="UNIT-C10-Plus-"+v+".apk";
            File dir=activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if(dir==null)throw new Exception("carpeta de descargas no accesible");
            destination=new File(dir,name);
            if(destination.exists() && !destination.delete())
                throw new Exception("no se puede sustituir la descarga antigua");
            DownloadManager.Request req=new DownloadManager.Request(Uri.parse(candidate.getString("apkUrl")));
            req.setTitle("UNIT C10 Plus "+candidate.optString("versionName"));
            req.setDescription("Actualización de UNIT firmada");
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(false);
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            req.setMimeType("application/vnd.android.package-archive");
            req.setDestinationInExternalFilesDir(activity,Environment.DIRECTORY_DOWNLOADS,name);
            downloadId=downloads.enqueue(req);
            preferences.edit().putLong("downloadId",downloadId)
                .putString("candidate",candidate.toString())
                .putString("filename",name).apply();
            busy=true;validated=false;
            fetch.setEnabled(false);
            message("Descarga iniciada dentro de UNIT...");
        }catch(Exception e){showFailure("No se pudo descargar: "+e.getMessage());}
    }
    private void checkDownload(){
        if(downloads==null||downloadId<=0||destroyed)return;
        try(Cursor c=downloads.query(new DownloadManager.Query().setFilterById(downloadId))){
            if(c==null || !c.moveToFirst())return;
            int statusCode=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if(statusCode==DownloadManager.STATUS_SUCCESSFUL){
                if(!verifying&&!validated)verifyFile();
            }else if(statusCode==DownloadManager.STATUS_FAILED){
                downloadId=-1;busy=false;check.setEnabled(true);
                message("Descarga fallida. Comprueba la conexión y vuelve a intentar.");
            }else{
                long bytes=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long total=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                message(total>0?"Descargando actualización: "+(bytes*100L/total)+"%":
                    "Descargando actualización...");
            }
        }catch(Exception e){message("Esperando información de la descarga...");}
    }
    private void verifyFile(){
        if(verifying||destination==null||candidate==null)return;
        verifying=true;busy=true;
        message("Descarga terminada. Verificando firma y contenido...");
        final JSONObject m=candidate;
        final File f=destination;
        new Thread(()->{
            String problem=null;
            try {
                if(!f.isFile()||f.length()<1024||f.length()>MAX_APK_BYTES)
                    throw new SecurityException("tamaño de APK no válido");
                MessageDigest sha=MessageDigest.getInstance("SHA-256");
                try(InputStream in=new FileInputStream(f)){
                    byte[] buf=new byte[65536];int n;
                    while((n=in.read(buf))!=-1)sha.update(buf,0,n);
                }
                if(!hex(sha.digest()).equalsIgnoreCase(m.getString("sha256")))
                    throw new SecurityException("SHA-256 no coincide; descarga rechazada");
                PackageManager pm=activity.getPackageManager();
                PackageInfo archive=pm.getPackageArchiveInfo(
                    f.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
                if(archive==null || !PACKAGE.equals(archive.packageName) ||
                    archive.getLongVersionCode()!=m.optLong("versionCode"))
                    throw new SecurityException("paquete o versión distintos");
                if(archive.signingInfo==null)throw new SecurityException("APK sin certificado");
                Signature[] signatures=archive.signingInfo.getApkContentsSigners();
                if(signatures==null||signatures.length!=1)throw new SecurityException("número de firmantes inesperado");
                MessageDigest signerHash=MessageDigest.getInstance("SHA-256");
                String fingerprint=hex(signerHash.digest(signatures[0].toByteArray()));
                if(!SIGNER.equalsIgnoreCase(fingerprint))
                    throw new SecurityException("firma ajena al proyecto");
                if(archive.getLongVersionCode()<=localVersion())
                    throw new SecurityException("no es versión más reciente");
            }catch(Exception e){problem=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
            final String outcome=problem;
            activity.runOnUiThread(()->{
                verifying=false;busy=false;
                if(destroyed)return;
                check.setEnabled(true);
                if(outcome==null){
                    validated=true;
                    message("APK descargada y VERIFICADA: "+m.optString("versionName"));
                    details.setText("Integridad SHA-256, paquete y certificado correctos. "+
                        "Pulsa instalar para abrir la confirmación oficial de Android.");
                    install.setEnabled(true);install.setVisibility(View.VISIBLE);
                }else{
                    validated=false;
                    install.setVisibility(View.GONE);
                    message("Archivo rechazado: "+outcome);
                }
            });
        },"c10plus-update-verification").start();
    }
    private void launchInstaller(){
        if(destroyed||!validated||downloadId<=0||destination==null)return;
        if(Build.VERSION.SDK_INT>=26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            message("Android exige autorización para instalar APK de esta aplicación.");
            try{
                Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:"+activity.getPackageName()));
                activity.startActivity(settings);
                details.setText("Autoriza la instalación para UNIT, vuelve aquí y pulsa Instalar de nuevo.");
            }catch(Exception e){message("No se puede abrir el permiso del instalador: "+e.getClass().getSimpleName());}
            return;
        }
        try{
            Uri fileUri=downloads.getUriForDownloadedFile(downloadId);
            if(fileUri==null)throw new Exception("Android no facilitó la URI de la descarga");
            Intent intent=new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri,"application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            message("Confirma la actualización en el instalador de Android.");
        }catch(Exception e){
            message("El instalador del coche no acepta la descarga: "+
                (e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));
        }
    }
    public void close(){
        destroyed=true;
        handler.removeCallbacks(poller);
    }
}