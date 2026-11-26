"""Assemble an experimental combined APK from pinned upstream and local cockpit.
No binary patching, no root/ADB commands, no default USB/launcher takeover."""
import pathlib, re, sys, shutil, xml.etree.ElementTree as ET
p=pathlib.Path(sys.argv[1])
def edit(rel, fn):
    path=p/rel
    old=path.read_text()
    new=fn(old)
    if new==old: raise RuntimeError(f"Expected edit did not match: {rel}")
    path.write_text(new)
def replace_once(s,old,new):
    if s.count(old)!=1: raise RuntimeError(f"Expected one match for {old!r}, got {s.count(old)}")
    return s.replace(old,new,1)

# Keep upstream application and lifecycle; own instrument panel remains a separate Gradle namespace.
edit("settings.gradle.kts",lambda s:s.replace('include(":app", ":contract")','include(":app", ":contract", ":cockpit")',1))
def cockpit_gradle(s):
    s=re.sub(r"id 'com.android.application' version '[^']+'","id 'com.android.library'",s,count=1)
    s=re.sub(r"id 'org.jetbrains.kotlin.android' version '[^']+'","id 'org.jetbrains.kotlin.android'",s,count=1)
    s=re.sub(r'^\s*(applicationId|versionCode|versionName)\s+[^\n]*\n','\n',s,flags=re.M)
    s=s.replace("com.google.protobuf:protobuf-javalite:4.29.3","com.google.protobuf:protobuf-java:3.25.5")
    return s
edit("cockpit/build.gradle",cockpit_gradle)
def app_gradle(s):
    s=replace_once(s,'applicationId = "com.andrerinas.headunitrevived"','applicationId = "es.drivecore.cockpit"')
    s=replace_once(s,"minSdk = 16\n        targetSdk = 36","minSdk = 28\n        targetSdk = 35")
    s=replace_once(s,"versionCode = 125","versionCode = 8")
    s=replace_once(s,'versionName = "4.1.0-beta2"','versionName = "0.8-headunit-experimental"')
    s=replace_once(s,'implementation(project(":contract"))','implementation(project(":contract"))\n    implementation(project(":cockpit"))')
    return s
edit("app/build.gradle.kts",app_gradle)
with (p/"gradle.properties").open("a") as f:
    f.write("\nandroid.enableJetifier=true\n")
# Avoid duplicated Protobuf implementations.
with (p/"app/build.gradle.kts").open("a") as f:
    f.write('\nconfigurations.configureEach { exclude(group = "com.google.protobuf", module = "protobuf-javalite") }\n')
A="{http://schemas.android.com/apk/res/android}"
T="{http://schemas.android.com/tools}"
ET.register_namespace("android",A[1:-1])
ET.register_namespace("tools",T[1:-1])
# Merge app manifests without taking over the system launcher, USB attach or OEM services.
own=p/"cockpit/src/main/AndroidManifest.xml"
doc=ET.parse(own)
app=doc.getroot().find("application")
for key in list(app.attrib): del app.attrib[key]
for activity in app.findall("activity"):
    activity.set(A+"theme","@style/DriveTheme")
doc.write(own,encoding="utf-8",xml_declaration=True)
up=p/"app/src/main/AndroidManifest.xml"
doc=ET.parse(up);root=doc.getroot();app=root.find("application")
app.set(A+"label","DriveCore Native")
app.set(A+"allowBackup","false")
app.set(A+"usesCleartextTraffic","false")
blocked_perms={"android.permission.ACCESS_SUPERUSER","android.permission.WRITE_SETTINGS","android.permission.READ_LOGS","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.RECEIVE_BOOT_COMPLETED","android.permission.SYSTEM_ALERT_WINDOW","android.permission.USE_FULL_SCREEN_INTENT"}
for child in list(root):
    if child.tag=="uses-permission" and child.get(A+"name") in blocked_perms: root.remove(child)
    elif child.tag=="permission" and child.get(A+"name")=="com.andrerinas.openheadunit.permission.NAVIGATION_UPDATE": child.set(A+"name","es.drivecore.cockpit.permission.NAVIGATION_UPDATE")
disabled={".app.BootCompleteReceiver",".app.AutoStartReceiver",".app.WifiAutoStartReceiver",".app.ScreenOffAdminReceiver",".automation.AutomationReceiver",".main.AutomationActivity",".app.RemoteControlReceiver",".connection.carkey.CarKeyBroadcastReceiver",".main.FloatingButtonService","rikka.shizuku.ShizukuProvider",".CarLauncherAlias"}
for item in list(app):
    name=item.get(A+"name","")
    if item.tag in ("activity","activity-alias") and name!="":
        item.set(A+"exported","false")
        for child in list(item):
            if child.tag=="intent-filter" or (child.tag=="meta-data" and child.get(A+"name") in ("android.app.shortcuts","android.hardware.usb.action.USB_DEVICE_ATTACHED")): item.remove(child)
    if name in disabled:
        item.set(A+"enabled","false")
        item.set(A+"exported","false")
        for child in list(item):
            if child.tag=="intent-filter":item.remove(child)
    if name==".main.MainActivity":
        item.set(A+"taskAffinity","${applicationId}.projection")
    if name=="rikka.shizuku.ShizukuProvider":
        item.set(T+"replace","android:enabled,android:exported")
doc.write(up,encoding="utf-8",xml_declaration=True)
# Make root, shell and OEM application disabling unavailable in this build.
utils=p/"app/src/main/java/com/andrerinas/openheadunit/utils"
(utils/"SUExecutor.kt").write_text('''package com.andrerinas.openheadunit.utils
/** Restricted integration build: no shell, root, or Shizuku execution. */
class SUExecutor {
    fun register() {}
    fun unregister() {}
    fun checkPermissionOnBoot() {}
    fun checkPermission(): Boolean = false
    fun setProp(key: String, value: String): Boolean = false
    fun execShell(cmd: String, asRootUser: Boolean = false): Int = -1
}
''')
edit("app/src/main/java/com/andrerinas/openheadunit/utils/LibsuAvailability.kt",lambda s:s.replace("= sdk >= MIN_SDK","= false"))
(utils/"adb/AdbManager.kt").write_text('''package com.andrerinas.openheadunit.utils.adb
import android.content.Context
/** No self-ADB shell connection in this integration. */
object AdbManager {
    suspend fun isAdbPortOpen(port: Int = 5555, timeoutMs: Int = 1000): Boolean = false
    suspend fun exec(context: Context, command: String, port: Int = 5555): Pair<Int,String> =
        Pair(-1, "Privileged operations are disabled in DriveCore")
}
''')
def restrict_settings(s):
    for key in ("autoKillOemApps","autoStartOnUsb","autoStartOnBoot","autoStartOnBootInBackground","autoStartOnScreenOn","autoStartOnWifi","usbConnectOnPlugIn","allowExternalConfiguration"):
        pattern=r"(    var "+key+r": Boolean\n)        get\(\) =[^\n]*\n        set\(value\)[^\n]*"
        s,n=re.subn(pattern,r"\1        get() = false\n        set(value) {}",s,count=1)
        if n!=1:raise RuntimeError("Unexpected Settings definition: "+key)
    return s
edit("app/src/main/java/com/andrerinas/openheadunit/utils/Settings.kt",restrict_settings)
for tree in (p/"app/src/main", p/"contract/src/main"):
    for f in tree.rglob("*"):
        if f.suffix in (".kt",".java",".xml") and f.is_file():
            s=f.read_text()
            v=s.replace("com.andrerinas.openheadunit.permission.NAVIGATION_UPDATE","es.drivecore.cockpit.permission.NAVIGATION_UPDATE")
            if v!=s:f.write_text(v)
# Link only the source-built libusb and FFmpeg audio libs; avoid ABI collisions.
# Upstream video uses Android hardware MediaCodec; its optional FFmpeg video fallback is disabled.
up_lib=p/"app/src/main/jniLibs"
cp_lib=p/"cockpit/src/main/jniLibs"
for abi in ("arm64-v8a","armeabi-v7a","x86","x86_64"):
    dest=up_lib/abi
    dest.mkdir(parents=True,exist_ok=True)
    for name in ("libavcodec.so","libavutil.so","libswscale.so","libswresample.so","libusb1.0.so"):
        (dest/name).unlink(missing_ok=True)
    shutil.move(str(cp_lib/abi/"libusb1.0.so"),str(dest/"libusb1.0.so"))
edit("app/src/main/cpp/CMakeLists.txt",lambda s:s.replace("set(HUR_FFMPEG_READY TRUE)","set(HUR_FFMPEG_READY FALSE)",1))
# User-initiated actual in-process projection activity, not an external app launcher.
main="cockpit/src/main/java/es/drivecore/cockpit/MainActivity.kt"
def add_receiver(s):
    s=s.replace("Native 0.7","Native 0.8").replace("C10 REEV · 0.7","C10 REEV · 0.8")
    s=replace_once(s,'"Conexiones"->{contents.addView(button("Diagnóstico LibUSB manual")','''"Conexiones"->{contents.addView(button("Android Auto · receptor integrado (prueba)"){
                        AlertDialog.Builder(this).setTitle("Prueba de proyección con el vehículo estacionado").setMessage("Cierra la sesión de proyección anterior. No cambies el receptor USB por defecto ni actives permisos root/ADB. CarPlay no está incluido.").setPositiveButton("Abrir receptor"){_,_->
                            radio.stop()
                            runCatching{startActivity(Intent().setClassName(packageName,"com.andrerinas.openheadunit.main.MainActivity"))}.onFailure{Toast.makeText(this,"Receptor no disponible: "+it.javaClass.simpleName,Toast.LENGTH_LONG).show()}
                        }.setNegativeButton("Cancelar",null).show()
                    });gap(contents);contents.addView(button("Diagnóstico LibUSB manual")''')
    s=s.replace("Android Auto y CarPlay: receptor pendiente. No se reclama ninguna interfaz USB.","Android Auto: módulo Open Headunit experimental; apertura manual. CarPlay no integrado. No se reclama ninguna interfaz USB automáticamente.")
    s=s.replace("TDT de vídeo y proyección: no implementadas en esta base.","TDT de vídeo no implementada. Android Auto experimental en Conexiones.")
    return s
edit(main,add_receiver)
# Include license and attribution in the app and corresponding source.
assets=p/"app/src/main/assets/drivecore-licenses"
assets.mkdir(parents=True,exist_ok=True)
shutil.copy2(p/"LICENSE",assets/"Open-Headunit-AGPL-3.0.txt")
shutil.copy2(p/"COPYRIGHT_MICHAEL_REID_GPLv3AFFERO.txt",assets/"Michael-Reid-Copyright.txt")
print("Merged pinned source. No root, default USB assignment, or boot startup. Device validation still required.")
