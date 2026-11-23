package es.leapmotorguia.c10studio;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.usb.UsbManager;
import android.location.*;
import android.media.MediaDrm;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import android.widget.*;
import android.view.*;
import org.json.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import java.security.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final String VERSION="1.0.0", PKG="es.leapmotorguia.c10studio";
    private static final int VERSION_CODE=100, PICK_APK=80, REQ_LOCATION=81, REQ_BLUETOOTH=82;
    private static final String UPDATE_URL="https://raw.githubusercontent.com/leapmotorguia-code/qonecta/feature/c10-studio-independiente-v1/updates/c10-studio.json";
    private final Handler ui=new Handler(Looper.getMainLooper());
    private WebView browser;
    private SharedPreferences prefs;
    private LocationManager locationManager;
    private LocationListener listener;
    private BluetoothSocket obdSocket;
    private Thread obdWorker;
    private final AtomicBoolean obdRunning=new AtomicBoolean(false);
    private String obdStatus="Desconectado", obdName="", obdLastError="";
    private volatile String obdTelemetry="{}";
    private volatile JSONObject onlineUpdate;
    private long pendingDownload=-1;
    private File updateFile;
    private boolean tracking=false;
    private double lastLat=Double.NaN,lastLon=Double.NaN,tripDistanceKm=0.0,gpsSpeed=Double.NaN;
    private float gpsAccuracy=Float.NaN;
    private long tripSince=0, lastFix=0;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("studio_v1",MODE_PRIVATE);
        tripDistanceKm=prefs.getFloat("tripKm",0f);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        browser=new WebView(this);
        browser.setBackgroundColor(0xff06131c);
        WebSettings settings=browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        browser.setWebChromeClient(new WebChromeClient());
        browser.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                Uri uri=r.getUrl();
                if("file".equals(uri.getScheme()) && "android_asset".equals(uri.getHost()))return false;
                openExternal(uri.toString());
                return true;
            }
        });
        browser.addJavascriptInterface(new Bridge(),"C10Native");
        setContentView(browser);
        browser.loadUrl("file:///android_asset/index.html");
        locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
        registerGps();
        ui.postDelayed(new Runnable(){@Override public void run(){
            if(isFinishing())return;
            emit("tick",getStatus().toString());
            ui.postDelayed(this,1800);
        }},2200);
    }
    private void emit(String event,String json){
        if(browser==null)return;
        ui.post(()->{if(browser!=null)browser.evaluateJavascript(
            "window.C10&&window.C10.receive("+JSONObject.quote(event)+","+
            (json==null?"null":json)+")",null);
        });
    }
    private static JSONObject obj(Object... fields){
        JSONObject j=new JSONObject();
        for(int i=0;i+1<fields.length;i+=2)try{j.put(String.valueOf(fields[i]),fields[i+1]);}
        catch(JSONException ignored){}
        return j;
    }
    private void feedback(String message){emit("toast",obj("text",message).toString());}
    private void openExternal(String address){
        try{
            Uri uri=Uri.parse(address);
            if(!"https".equalsIgnoreCase(uri.getScheme()) && !"geo".equalsIgnoreCase(uri.getScheme())) {
                feedback("Enlace no admitido");return;
            }
            Intent in=new Intent(Intent.ACTION_VIEW,uri);
            startActivity(in);
        }catch(Exception x){feedback("No hay aplicación compatible para abrir el enlace");}
    }
    private boolean granted(String p){return Build.VERSION.SDK_INT<23 || checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;}
    private void registerGps(){
        if(locationManager==null || !granted(Manifest.permission.ACCESS_FINE_LOCATION))return;
        listener=new LocationListener(){
            @Override public void onLocationChanged(Location loc){
                long now=System.currentTimeMillis();
                gpsSpeed=loc.hasSpeed()?loc.getSpeed()*3.6d:Double.NaN;
                gpsAccuracy=loc.hasAccuracy()?loc.getAccuracy():Float.NaN;
                lastFix=now;
                if(tracking){
                    if(!Double.isNaN(lastLat)){
                        float[] out=new float[1];
                        Location.distanceBetween(lastLat,lastLon,loc.getLatitude(),loc.getLongitude(),out);
                        if(out[0]>=2 && out[0]<2000 && now-tripSince<86400000L)
                            tripDistanceKm+=out[0]/1000d;
                    }
                    lastLat=loc.getLatitude();lastLon=loc.getLongitude();
                    prefs.edit().putFloat("tripKm",(float)tripDistanceKm).apply();
                }
            }
            @Override public void onProviderEnabled(String provider){}
            @Override public void onProviderDisabled(String provider){}
            @Override public void onStatusChanged(String provider,int status,Bundle extras){}
        };
        try{locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000,1,listener);}
        catch(Exception ignored){}
    }
    private JSONObject getStatus(){
        return obj("version",VERSION,"packageName",getPackageName(),
            "sdk",Build.VERSION.SDK_INT,"device",Build.MANUFACTURER+" "+Build.MODEL,
            "model",prefs.getString("model","C10 REEV"),
            "odometerMode","GPS propio, no ECU",
            "gpsSpeed",Double.isNaN(gpsSpeed)?JSONObject.NULL:Math.round(gpsSpeed*10d)/10d,
            "gpsAccuracy",Float.isNaN(gpsAccuracy)?JSONObject.NULL:Math.round(gpsAccuracy),
            "gpsAgeMs",lastFix==0?JSONObject.NULL:System.currentTimeMillis()-lastFix,
            "tripKm",Math.round(tripDistanceKm*100d)/100d,
            "tracking",tracking,"obdState",obdStatus,"obdName",obdName,
            "obdError",obdLastError,"obd",parseObject(obdTelemetry),
            "favoriteApps",prefs.getString("favorites","[]"),
            "chargeSoc",JSONObject.NULL,"vehicleFuelLiters",JSONObject.NULL,
            "regenKwh",JSONObject.NULL,"powerKw",JSONObject.NULL);
    }
    private static JSONObject parseObject(String src){
        try{return new JSONObject(src);}catch(Exception e){return new JSONObject();}
    }
    private JSONArray launchableApps(){
        JSONArray arr=new JSONArray();
        try{
            Intent intent=new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            PackageManager pm=getPackageManager();
            List<ResolveInfo> list=pm.queryIntentActivities(intent,0);
            Set<String> seen=new HashSet<>();
            for(ResolveInfo item:list){
                String p=item.activityInfo.packageName;
                if(p.equals(PKG) || !seen.add(p))continue;
                arr.put(obj("pkg",p,"name",String.valueOf(item.loadLabel(pm))));
            }
        }catch(Exception e){}
        return arr;
    }
    private boolean launch(String pkg){
        if(pkg==null || !pkg.matches("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+"))return false;
        try{
            Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
            if(i==null)return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        }catch(Exception e){return false;}
    }
    private JSONArray pairedObd(){
        JSONArray list=new JSONArray();
        if(Build.VERSION.SDK_INT>=31 && !granted(Manifest.permission.BLUETOOTH_CONNECT))
            return list;
        try{
            BluetoothAdapter ad=BluetoothAdapter.getDefaultAdapter();
            if(ad==null)return list;
            for(BluetoothDevice d:ad.getBondedDevices()){
                list.put(obj("name",d.getName()==null?"Dispositivo Bluetooth":d.getName(),
                    "address",d.getAddress()));
            }
        }catch(Exception ignored){}
        return list;
    }
    private JSONObject diagnose(){
        JSONObject j=obj("time",System.currentTimeMillis(),
            "android",Build.VERSION.RELEASE,"api",Build.VERSION.SDK_INT,
            "hardware",Build.MANUFACTURER+" "+Build.MODEL,
            "gpsPermission",granted(Manifest.permission.ACCESS_FINE_LOCATION),
            "bluetoothPermission",Build.VERSION.SDK_INT<31||granted(Manifest.permission.BLUETOOTH_CONNECT),
            "obd",obdStatus,"obdConnected",obdRunning.get(),
            "gpsFix",lastFix>0,"gpsSource","Android (NO GPS del C10 certificado)",
            "canBus","Sin interfaz CAN interna confirmada",
            "vehicleCameras","Sin acceso a cámaras del vehículo confirmado",
            "tpms","Sin acceso confirmado",
            "doors","Sin acceso confirmado",
            "carplay","No incluye receptor CarPlay certificado; requiere solución externa",
            "androidAuto","Receptor externo necesario; no se certifica sesión de proyección");
        try{
            UsbManager u=(UsbManager)getSystemService(USB_SERVICE);
            j.put("usbVisible",u==null?JSONObject.NULL:u.getDeviceList().size());
        }catch(Exception e){}
        try{
            CameraManager m=(CameraManager)getSystemService(CAMERA_SERVICE);
            j.put("androidCameraCount",m==null?JSONObject.NULL:m.getCameraIdList().length);
        }catch(Exception e){}
        try{
            SensorManager sm=(SensorManager)getSystemService(SENSOR_SERVICE);
            j.put("androidSensors",sm==null?JSONObject.NULL:sm.getSensorList(-1).size());
        }catch(Exception e){}
        try{
            UUID drm=UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");
            if(MediaDrm.isCryptoSchemeSupported(drm)){
                MediaDrm m=new MediaDrm(drm);
                try{j.put("widevine",m.getPropertyString("securityLevel"));}finally{m.release();}
            }else j.put("widevine","No detectado");
        }catch(Exception e){try{j.put("widevine","No comprobable");}catch(Exception ignored){}}
        try{
            ConnectivityManager c=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo n=c==null?null:c.getActiveNetworkInfo();
            j.put("online",n!=null&&n.isConnected());
        }catch(Exception e){}
        for(String[] p:new String[][]{
            {"maps","com.google.android.apps.maps"},{"spotify","com.spotify.music"},
            {"netflix","com.netflix.mediaclient"},{"prime","com.amazon.avod.thirdpartyclient"},
            {"max","com.wbd.stream"},{"unit","com.tencent.qqmusiccar"},
            {"autokit","com.autokit"},{"zlink","com.zjinnova.zlink"}
        })try{j.put("app_"+p[0],getPackageManager().getLaunchIntentForPackage(p[1])!=null);}
        catch(Exception e){}
        try{j.put("pairedBluetooth",pairedObd().length());}catch(JSONException ignored){}
        return j;
    }
    private static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private String command(InputStream in,OutputStream out,String cmd) throws IOException{
        out.write((cmd+"\r").getBytes("US-ASCII"));out.flush();
        ByteArrayOutputStream buf=new ByteArrayOutputStream();
        long until=SystemClock.elapsedRealtime()+2200;
        while(SystemClock.elapsedRealtime()<until && obdRunning.get()){
            int available=in.available();
            if(available>0){
                int c=in.read();
                if(c<0)throw new IOException("Desconectado");
                if(c=='>')break;
                if(buf.size()<800)buf.write(c);
            }else try{Thread.sleep(25);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
        }
        return buf.toString("US-ASCII").toUpperCase(Locale.ROOT).replaceAll("[\\r\\n ]+","");
    }
    private Double pid(String hex,String p,int count){
        Matcher m=Pattern.compile("41"+p+"([0-9A-F]{"+(count*2)+"})").matcher(hex);
        if(!m.find())return null;
        try{
            String s=m.group(1);int a=Integer.parseInt(s.substring(0,2),16);
            if(p.equals("0D"))return (double)a;
            if(p.equals("0C"))return (a*256+Integer.parseInt(s.substring(2,4),16))/4d;
            if(p.equals("05"))return (double)a-40;
            if(p.equals("2F"))return a*100d/255d;
            if(p.equals("5E"))return (a*256+Integer.parseInt(s.substring(2,4),16))/20d;
            return (double)a;
        }catch(Exception e){return null;}
    }
    private synchronized void stopObd(){
        obdRunning.set(false);
        if(obdSocket!=null)try{obdSocket.close();}catch(Exception ignored){}
        obdSocket=null;
        obdStatus="Desconectado";
        obdTelemetry="{}";
    }
    private void connectObd(String address){
        if(!address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")){feedback("Dirección Bluetooth inválida");return;}
        if(Build.VERSION.SDK_INT>=31 && !granted(Manifest.permission.BLUETOOTH_CONNECT)){
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQ_BLUETOOTH);return;
        }
        stopObd();
        obdRunning.set(true);
        obdStatus="Conectando";obdName=address;obdLastError="";
        obdWorker=new Thread(()->{
            BluetoothSocket socket=null;
            try{
                BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
                if(adapter==null)throw new IOException("Bluetooth no disponible");
                BluetoothDevice device=adapter.getRemoteDevice(address);
                socket=device.createRfcommSocketToServiceRecord(SPP);
                synchronized(MainActivity.this){obdSocket=socket;}
                socket.connect();
                InputStream input=socket.getInputStream();
                OutputStream output=socket.getOutputStream();
                obdName=device.getName()==null?address:device.getName();
                command(input,output,"ATZ");
                command(input,output,"ATE0");command(input,output,"ATL0");
                command(input,output,"ATS0");command(input,output,"ATH0");
                command(input,output,"ATSP0");
                obdStatus="Conectado (ELM327)";
                while(obdRunning.get()){
                    JSONObject j=new JSONObject();
                    for(String[] p:new String[][]{{"0D","speed", "1"},{"0C","rpm","2"},
                        {"05","coolantC","1"},{"2F","fuelPct","1"},{"5E","fuelLph","2"}}){
                        String raw=command(input,output,"01"+p[0]);
                        Double v=pid(raw,p[0],Integer.parseInt(p[2]));
                        if(v!=null)j.put(p[1],Math.round(v*100d)/100d);
                    }
                    j.put("source","OBD2 ELM327 externo · PID estándar; disponibilidad según ECU");
                    obdTelemetry=j.toString();
                    emit("obd",j.toString());
                    try{Thread.sleep(1300);}catch(InterruptedException e){break;}
                }
            }catch(Exception e){
                obdLastError=e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage());
                obdStatus="Error de conexión";
                emit("obdError",obj("text",obdLastError).toString());
            }finally{
                obdRunning.set(false);
                if(socket!=null)try{socket.close();}catch(Exception ignored){}
            }
        },"studio-obd-readonly");
        obdWorker.setDaemon(true);obdWorker.start();
    }
    private void chooseApk(){
        try{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/vnd.android.package-archive");
            startActivityForResult(i,PICK_APK);
        }catch(Exception e){
            try{
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
                startActivityForResult(i,PICK_APK);
            }catch(Exception e2){feedback("El coche no dispone de selector de archivos");}
        }
    }
    @Override protected void onActivityResult(int r,int result,Intent data){
        super.onActivityResult(r,result,data);
        if(r==PICK_APK && result==RESULT_OK && data!=null && data.getData()!=null){
            Uri uri=data.getData();
            try{
                Intent i=new Intent(Intent.ACTION_INSTALL_PACKAGE);
                i.setData(uri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(i);
            }catch(Exception e){
                try{
                    Intent i=new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri,"application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(i);
                }catch(Exception ignored){feedback("LeapOS no ofrece un instalador de APK accesible");}
            }
        }
    }
    private void checkUpdates(){
        new Thread(()->{
            try{
                URL url=new URL(UPDATE_URL);
                HttpsURLConnection c=(HttpsURLConnection)url.openConnection();
                c.setConnectTimeout(9000);c.setReadTimeout(9000);
                c.setRequestProperty("User-Agent","C10-Studio/1");
                if(c.getResponseCode()!=200)throw new IOException("Canal no publicado");
                ByteArrayOutputStream b=new ByteArrayOutputStream();
                InputStream in=c.getInputStream();byte[] chunk=new byte[4096];int n;
                while((n=in.read(chunk))!=-1){if(b.size()+n>12000)throw new IOException("Respuesta demasiado grande");b.write(chunk,0,n);}
                in.close();c.disconnect();
                JSONObject m=new JSONObject(b.toString("UTF-8"));
                if(!PKG.equals(m.optString("packageName")))throw new IOException("Paquete no coincide");
                int version=m.optInt("versionCode",0);
                onlineUpdate=m;
                if(version<=VERSION_CODE){emit("update",obj("status","current","message","C10 Studio está al día").toString());return;}
                String link=m.optString("apkUrl","");
                if(!link.startsWith("https://github.com/leapmotorguia-code/qonecta/releases/download/"))
                    throw new IOException("Origen no autorizado");
                if(!m.optString("sha256").matches("(?i)[0-9a-f]{64}"))
                    throw new IOException("Hash SHA-256 no disponible");
                emit("update",obj("status","available","version",m.optString("versionName"),
                    "notes",m.optString("notes","Versión publicada")).toString());
            }catch(Exception e){emit("update",obj("status","error","message",
                "No se puede consultar el canal: "+String.valueOf(e.getMessage())).toString());}
        },"studio-update-check").start();
    }
    private void downloadUpdate(){
        JSONObject m=onlineUpdate;
        if(m==null || m.optInt("versionCode")<=VERSION_CODE){feedback("No hay actualización nueva publicada");return;}
        try{
            String link=m.getString("apkUrl");
            File folder=getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if(folder==null)throw new IOException("Carpeta de descargas inaccesible");
            String name="C10-Studio-"+m.getInt("versionCode")+".apk";
            updateFile=new File(folder,name);
            if(updateFile.exists()&&!updateFile.delete())throw new IOException("Archivo anterior bloqueado");
            DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            if(dm==null)throw new IOException("Descargador Android no disponible");
            DownloadManager.Request req=new DownloadManager.Request(Uri.parse(link));
            req.setTitle("C10 Studio "+m.optString("versionName"));
            req.setMimeType("application/vnd.android.package-archive");
            req.setDestinationInExternalFilesDir(this,Environment.DIRECTORY_DOWNLOADS,name);
            pendingDownload=dm.enqueue(req);
            emit("update",obj("status","downloading","message","Descargando APK oficial").toString());
            ui.postDelayed(new Runnable(){@Override public void run(){monitorDownload(this);}},1500);
        }catch(Exception e){feedback("No se pudo descargar: "+e.getMessage());}
    }
    private void monitorDownload(Runnable again){
        if(pendingDownload<=0)return;
        DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        try(Cursor c=dm.query(new DownloadManager.Query().setFilterById(pendingDownload))){
            if(c==null || !c.moveToFirst())throw new IOException("Descarga no localizada");
            int st=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if(st==DownloadManager.STATUS_SUCCESSFUL){
                verifyAndInstall();return;
            }
            if(st==DownloadManager.STATUS_FAILED){feedback("La descarga ha fallado");return;}
            ui.postDelayed(again,1400);
        }catch(Exception e){feedback("Descarga: "+e.getMessage());}
    }
    private void verifyAndInstall(){
        new Thread(()->{
            try{
                if(updateFile==null||!updateFile.isFile()||updateFile.length()>150*1024*1024||updateFile.length()<10000)
                    throw new SecurityException("Tamaño no válido");
                MessageDigest md=MessageDigest.getInstance("SHA-256");
                try(FileInputStream input=new FileInputStream(updateFile)){
                    byte[] buf=new byte[65536];int n;while((n=input.read(buf))!=-1)md.update(buf,0,n);
                }
                StringBuilder hex=new StringBuilder();for(byte b:md.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));
                if(onlineUpdate==null||!hex.toString().equalsIgnoreCase(onlineUpdate.optString("sha256")))
                    throw new SecurityException("SHA-256 no coincide");
                PackageManager pm=getPackageManager();
                PackageInfo file=pm.getPackageArchiveInfo(updateFile.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
                PackageInfo installed=pm.getPackageInfo(PKG,PackageManager.GET_SIGNING_CERTIFICATES);
                if(file==null||!PKG.equals(file.packageName)||file.getLongVersionCode()<=VERSION_CODE)
                    throw new SecurityException("Paquete/versión no válidos");
                if(file.signingInfo==null||installed.signingInfo==null)
                    throw new SecurityException("No hay certificado verificable");
                android.content.pm.Signature[] a=file.signingInfo.getApkContentsSigners(),b=installed.signingInfo.getApkContentsSigners();
                if(a.length!=1||b.length!=1||!Arrays.equals(a[0].toByteArray(),b[0].toByteArray()))
                    throw new SecurityException("Firma diferente");
                ui.post(()->{
                    try{
                        DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
                        Uri uri=dm.getUriForDownloadedFile(pendingDownload);
                        if(uri==null)throw new IOException("URI ausente");
                        Intent i=new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        i.setData(uri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try{startActivity(i);}
                        catch(Exception ex){
                            Intent fallback=new Intent(Intent.ACTION_VIEW);
                            fallback.setDataAndType(uri,"application/vnd.android.package-archive");
                            fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(fallback);
                        }
                    }catch(Exception e){feedback("LeapOS impide abrir el instalador oficial: "+e.getClass().getSimpleName());}
                });
            }catch(Exception e){emit("update",obj("status","error","message",
                "Actualización rechazada: "+String.valueOf(e.getMessage())).toString());}
        },"studio-verify-apk").start();
    }
    @Override public void onRequestPermissionsResult(int req,String[] names,int[] grants){
        super.onRequestPermissionsResult(req,names,grants);
        if(req==REQ_LOCATION)registerGps();
        emit("tick",getStatus().toString());
    }
    @Override protected void onDestroy(){
        stopObd();
        if(locationManager!=null&&listener!=null)try{locationManager.removeUpdates(listener);}catch(Exception ignored){}
        if(browser!=null){browser.removeJavascriptInterface("C10Native");browser.destroy();browser=null;}
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    public class Bridge {
        @JavascriptInterface public String status(){return getStatus().toString();}
        @JavascriptInterface public String apps(){return launchableApps().toString();}
        @JavascriptInterface public String report(){return diagnose().toString();}
        @JavascriptInterface public String bluetoothDevices(){return pairedObd().toString();}
        @JavascriptInterface public void openUrl(String url){ui.post(()->openExternal(url));}
        @JavascriptInterface public void launchApp(String pkg){ui.post(()->{
            if(!launch(pkg))feedback("Aplicación no instalada o bloqueada por LeapOS");
        });}
        @JavascriptInterface public void connectObd(String mac){ui.post(()->connectObd(mac));}
        @JavascriptInterface public void disconnectObd(){ui.post(()->stopObd());}
        @JavascriptInterface public void permissionGps(){ui.post(()->{
            if(granted(Manifest.permission.ACCESS_FINE_LOCATION))registerGps();
            else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION);
        });}
        @JavascriptInterface public void permissionBluetooth(){ui.post(()->{
            if(Build.VERSION.SDK_INT>=31&&!granted(Manifest.permission.BLUETOOTH_CONNECT))
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQ_BLUETOOTH);
        });}
        @JavascriptInterface public void setModel(String model){
            if(model!=null && model.length()<40)prefs.edit().putString("model",model).apply();
        }
        @JavascriptInterface public void setFavorites(String json){
            try{JSONArray arr=new JSONArray(json);if(arr.length()>60)return;
                prefs.edit().putString("favorites",arr.toString()).apply();
            }catch(Exception ignored){}
        }
        @JavascriptInterface public void track(String action){ui.post(()->{
            if("reset".equals(action)){tracking=false;tripDistanceKm=0;lastLat=Double.NaN;tripSince=0;
                prefs.edit().putFloat("tripKm",0f).apply();
            }else if("start".equals(action)){tracking=true;lastLat=Double.NaN;tripSince=System.currentTimeMillis();}
            else if("stop".equals(action))tracking=false;
            emit("tick",getStatus().toString());
        });}
        @JavascriptInterface public void checkUpdates(){ui.post(()->checkUpdates());}
        @JavascriptInterface public void installUpdate(){ui.post(()->downloadUpdate());}
        @JavascriptInterface public void chooseApk(){ui.post(()->chooseApk());}
        @JavascriptInterface public void showToast(String message){ui.post(()->feedback(message==null?"":message));}
    }
}
