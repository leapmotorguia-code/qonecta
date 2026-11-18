import os, re, subprocess, time, xml.etree.ElementTree as ET
PKG = 'com.leapmotorguia.c10prueba06'
COMPONENT = PKG + '/com.leapmotorguia.c10nativa.MainActivity'

def run(*args):
    p = subprocess.run(args, capture_output=True, text=True)
    if p.returncode:
        raise RuntimeError(str(args) + '\n' + p.stdout + p.stderr)
    return p.stdout

def ui():
    run('adb', 'shell', 'uiautomator', 'dump', '/sdcard/c10-smoke.xml')
    return run('adb', 'exec-out', 'cat', '/sdcard/c10-smoke.xml')

def tap(label):
    for _ in range(8):
        tree = ET.fromstring(ui())
        for node in tree.iter('node'):
            if node.attrib.get('text') == label:
                x1,y1,x2,y2 = map(int,re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',node.attrib['bounds']).groups())
                run('adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
                time.sleep(1)
                return
        w,h = map(int,re.search(r'(\d+)x(\d+)',run('adb','shell','wm','size')).groups())
        run('adb','shell','input','swipe',str(w//2),str(h*4//5),str(w//2),str(h//4),'300')
        time.sleep(.5)
    raise AssertionError('Botón no encontrado: '+label)

print(run('adb','install', 'c10-native/app/build/outputs/apk/release/app-release.apk'))
print(run('adb','shell','am','start','-W','-n',COMPONENT))
time.sleep(2)
assert run('adb','shell','pidof',PKG).strip()
screen = ui()
assert 'C10 PRUEBA 0.6' in screen and 'La aplicación se ha abierto' in screen,screen
# No location permission is requested on launch.
assert 'permissioncontroller' not in run('adb','shell','dumpsys','activity','activities').split('mResumedActivity')[-1][:500]
tap('Abrir medición GPS')
assert 'Medición por GPS' in ui()
tap('Preparar medición')
permission_ui=ui()
assert ('permissioncontroller' in permission_ui or 'packageinstaller' in permission_ui),permission_ui
# Dismiss without granting: denial must leave the app usable.
run('adb','shell','input','keyevent','4')
time.sleep(1)
assert run('adb','shell','pidof',PKG).strip()
run('adb','shell','am','force-stop',PKG)
run('adb','shell','pm','grant',PKG,'android.permission.ACCESS_COARSE_LOCATION')
run('adb','shell','pm','grant',PKG,'android.permission.ACCESS_FINE_LOCATION')
run('adb','shell','settings','put','secure','location_mode','3')
run('adb','shell','am','start','-W','-n',COMPONENT)
time.sleep(1)
tap('Abrir medición GPS')
tap('Preparar medición')
assert 'GPS:' in ui()
tap('Detener medición')
assert 'Medición detenida.' in ui()
assert run('adb','shell','pidof',PKG).strip()
print('PASS: install, launch, GPS screen, runtime permission request, cancel, grant, start and stop.')
print('Emulator API:', run('adb','shell','getprop','ro.build.version.sdk').strip())
print('No Play Protect or actual vehicle acceptance is simulated by this test.')
