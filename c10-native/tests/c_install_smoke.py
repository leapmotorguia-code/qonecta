import subprocess
import time

APK = "c10-native/app/build/outputs/apk/release/app-release.apk"
PKG = "com.tencent.qqmusiccar"

def adb(*args, check=True):
    r = subprocess.run(["adb", *args], capture_output=True, text=True, timeout=90)
    if check and r.returncode:
        raise RuntimeError(r.stdout + r.stderr)
    return r

r = adb("install", APK)
assert "Success" in r.stdout, r.stdout
r = adb("shell", "am", "start", "-W", "-n",
        PKG + "/com.leapmotorguia.c10nativa.MainActivity")
assert "Status: ok" in r.stdout, r.stdout
assert adb("shell", "pidof", PKG).stdout.strip()
adb("shell", "uiautomator", "dump", "/sdcard/c10-ui.xml")
ui = adb("shell", "cat", "/sdcard/c10-ui.xml").stdout
assert "C10 PRUEBA C" in ui, ui
print("Instalación y apertura en AOSP Android 11: OK")

adb("uninstall", PKG)
r = adb("install", "/tmp/c10-other-signer.apk")
assert "Success" in r.stdout, r.stdout
r = adb("install", "-r", APK, check=False)
out = r.stdout + r.stderr
assert "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in out, out
print("Actualizar un paquete con firma distinta fue rechazado: OK")
adb("uninstall", PKG)
