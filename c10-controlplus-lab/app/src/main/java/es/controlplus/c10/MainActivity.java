package es.controlplus.c10;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.List;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

/**
 * Aplicación acompañante independiente de pruebas: no modifica Qonecta ni el firmware.
 * Los enlaces externos son dirigidos a la aplicación oficial / navegador del dispositivo.
 * La geolocalización opcional solo procede de Android, no del bus CAN.
 */
public final class MainActivity extends Activity {
    private static final String TRUSTED_ORIGIN = "https://appassets.androidplatform.net";
    private static final int LOCATION_PERMISSION_REQUEST = 1901;
    private WebView web;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;

    public final class PlatformBridge {
        /** Informativo; no accede a cámaras de coche, bus CAN, radar ni batería HV. */
        @JavascriptInterface
        public String getCapabilities() {
            JSONObject result = new JSONObject();
            JSONObject android = new JSONObject();
            try {
                android.put("sdk", Build.VERSION.SDK_INT);
                android.put("manufacturer", Build.MANUFACTURER);
                android.put("model", Build.MODEL);
                PackageManager pm = getPackageManager();
                android.put("usbHost", pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST));
                android.put("bluetoothHardware", pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH));
                android.put("wifiHardware", pm.hasSystemFeature(PackageManager.FEATURE_WIFI));
                android.put("automotiveFeature", pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE));
                android.put("locationPermission", 
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ?
                    "Precisa concedida" : checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ?
                    "Aproximada concedida" : "Sin conceder");
                SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                List<Sensor> sensors = sensorManager == null ? null : sensorManager.getSensorList(Sensor.TYPE_ALL);
                android.put("sensorCount", sensors == null ? 0 : sensors.size());
                JSONArray sensorNames = new JSONArray();
                if (sensors != null) for (Sensor sensor : sensors) sensorNames.put(sensor.getName());
                android.put("sensorNames", sensorNames);
                try {
                    CameraManager cam = (CameraManager) getSystemService(CAMERA_SERVICE);
                    android.put("genericCameraCount", cam == null ? 0 : cam.getCameraIdList().length);
                } catch (Exception ex) { android.put("genericCameraCount", JSONObject.NULL); }
                try {
                    Class.forName("android.car.Car");
                    android.put("carClassPresent", true);
                } catch (Throwable e) { android.put("carClassPresent", false); }
                result.put("packageName", getPackageName());
                result.put("android", android);
                result.put("vehicle", new JSONObject()
                    .put("tractionBattery", "NO_VERIFICADO")
                    .put("powerAndRegeneration", "NO_VERIFICADO")
                    .put("vehicleCameras", "NO_VERIFICADO")
                    .put("radarAndParking", "NO_VERIFICADO")
                    .put("canObd", "SIN_CONEXION")
                    .put("adas", "SIN_ACCESO_VERIFICADO"));
                result.put("info", "Datos Android; los sistemas OEM no están conectados ni se controlan.");
            } catch (Exception ex) {
                try { result.put("error", ex.toString()); } catch (Exception ignored) {}
            }
            return result.toString();
        }
        @JavascriptInterface
        public void openService(String url) {
            if (url == null || !url.startsWith("https://")) return;
            // Solo los cuatro destinos oficiales declarados en el panel.
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (!("www.netflix.com".equals(host) || "pluto.tv".equals(host) ||
                  "www.primevideo.com".equals(host) || "www.hbomax.com".equals(host))) return;
            runOnUiThread(() -> openExternal(uri));
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new PlatformBridge(), "C10Native");
        web.setWebViewClient(new WebViewClient() {
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (TRUSTED_ORIGIN.equals(u.getScheme() + "://" + u.getHost())) return false;
                if ("https".equalsIgnoreCase(u.getScheme())) openExternal(u);
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {
                if (!TRUSTED_ORIGIN.equals(origin)) {
                    callback.invoke(origin, false, false);return;
                }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                         == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                         == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeoCallback = callback; pendingGeoOrigin = origin;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
                }
            }
        });
        setContentView(web);
        web.loadUrl(TRUSTED_ORIGIN + "/assets/index.html");
    }

    private void openExternal(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (Exception ex) {
            Toast.makeText(this,"No hay una aplicación compatible para abrir este enlace",
                Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && pendingGeoCallback != null) {
            boolean granted = false;
            for (int r : grantResults) if (r == PackageManager.PERMISSION_GRANTED) granted = true;
            pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
            pendingGeoCallback = null; pendingGeoOrigin = null;
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (web != null) { web.removeJavascriptInterface("C10Native"); web.destroy(); }
        super.onDestroy();
    }
}
