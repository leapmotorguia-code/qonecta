"""Install the exact release APKs on API30, navigate real panels, export diagnostics.
No test result here establishes OEM acceptance, media/network playback or vehicle access.
"""
import os,re,subprocess,time,xml.etree.ElementTree as ET,pathlib,json
PKG='com.leapmotorguia.c10rebuilt'
HELPER='com.leapmotorguia.c10installer'
def run(*args):
    p=subprocess.run(args,capture_output=True,text=True)
    if p.returncode: raise RuntimeError(str(args)+'\n'+p.stdout+p.stderr)
    return p.stdout
def ui():
    run('adb','shell','uiautomator','dump','/sdcard/c10-ui.xml')
    return run('adb','exec-out','cat','/sdcard/c10-ui.xml')
def tap(label):
    for attempt in range(16):
        tree=ET.fromstring(ui())
        for n in tree.iter('node'):
            if n.attrib.get('text','').casefold()==label.casefold() or n.attrib.get('content-desc')=='nav:'+label:
                nums=list(map(int,re.findall(r'\d+',n.attrib['bounds'])))
                x1,y1,x2,y2=nums
                if x2>x1 and y2>y1:
                    run('adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.8);return
        menus=['Inicio','Coche','Instrumentos','Eficiencia','Radio','Web','Fotos','Vídeos','TV','Herramientas','OBD','Ajustes','Diagnóstico','Integraciones']
        visible=[];nav_y=None;left=0;right=0
        for n in tree.iter('node'):
            desc=n.attrib.get('content-desc','')
            if desc.startswith('nav:'):
                x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']))
                if y2>y1 and x2>x1:
                    nav_y=(y1+y2)//2;left=min(left,x1);right=max(right,x2)
                    visible.append(menus.index(desc[4:]))
        if nav_y is None: raise AssertionError('Not found '+label+'\n'+ui())
        width=right-left
        xlo=int(left+width*.15);xhi=int(left+width*.85)
        backward=label in menus and menus.index(label)<min(visible)
        start,end=(xlo,xhi) if backward else (xhi,xlo)
        run('adb','shell','input','swipe',str(start),str(nav_y),str(end),str(nav_y),'300')
        time.sleep(.3)
    raise AssertionError('Not found '+label+'\n'+ui())
run('adb','shell','wm','size','1920x1080');run('adb','shell','wm','density','160')
run('adb','install','-r','deliverable/C10-ControlPlus-1.0.apk')
run('adb','install','-r','deliverable/C10-Instalador-1.0.apk')
for perm in ['android.permission.ACCESS_COARSE_LOCATION','android.permission.ACCESS_FINE_LOCATION','android.permission.READ_EXTERNAL_STORAGE']:
    run('adb','shell','pm','grant',PKG,perm)
run('adb','shell','am','start','-W','-n',PKG+'/com.headqlink.link.C10NativeActivity');time.sleep(2)
assert 'C10 ControlPlus' in ui()
for name in ['Coche','Instrumentos','Eficiencia','Radio','Diagnóstico','Integraciones','Ajustes']:
    tap(name);screen=ui();assert 'El panel no pudo abrirse' not in screen,screen
    assert run('adb','shell','pidof',PKG).strip()
for name in ['Fotos','Vídeos']:
    tap(name);tap('Estoy estacionado');assert 'El panel no pudo abrirse' not in ui()
tap('OBD');assert 'OBD · lecturas estándar' in ui();assert 'Sin lecturas actuales.' in ui()
run('adb','shell','input','keyevent','4');time.sleep(1)
tap('Herramientas');assert 'HERRAMIENTAS C10' in ui()
run('adb','shell','input','keyevent','4');time.sleep(1)
tap('Diagnóstico');screen=ui();assert 'API 30' in screen;assert 'SDK mínimo: 26 · objetivo: 35' in screen
assert 'Último error de panel: ninguno' in screen,screen
services=run('adb','shell','dumpsys','activity','services',PKG)
assert 'LinkService' not in services and 'AapService' not in services,services
run('adb','shell','am','force-stop',PKG)
run('adb','shell','am','start','-W','-n',HELPER+'/.MainActivity');time.sleep(1)
assert 'C10 Instalador diagnóstico' in ui()
tap('Actualizar resultado');assert 'Sin intentos.' in ui()
# Exercise the helper permission gate without disabling a device protection.
tap('Seleccionar APK');screen=ui();assert 'Android exige permitir instalaciones' in screen,screen
run('adb','shell','input','keyevent','4')
log=run('adb','logcat','-d','-s','AndroidRuntime:E')
assert 'FATAL EXCEPTION' not in log,log
pathlib.Path('deliverable/emulador-api30.txt').write_text('PASS: exact release APK installation; native Qonecta panels, tools, OBD empty state, diagnostic report and installer permission gate.\nNo real vehicle, actual OBD, account, camera, radio streaming or OEM acceptance tested.\n')
print('PASS: native app and standalone diagnostic installer on Android 11/API30. No vehicle claims.')
