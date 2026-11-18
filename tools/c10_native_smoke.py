import re,subprocess,time,xml.etree.ElementTree as ET

PKG='com.leapmotorguia.c10nativeplus'

def run(*args):
    p=subprocess.run(args,capture_output=True,text=True)
    if p.returncode: raise RuntimeError(str(args)+'\n'+p.stdout+p.stderr)
    return p.stdout

def ui():
    run('adb','shell','uiautomator','dump','/sdcard/c10-native.xml')
    return run('adb','exec-out','cat','/sdcard/c10-native.xml')

def tap(label):
    for _ in range(12):
        for n in ET.fromstring(ui()).iter('node'):
            if n.attrib.get('text')==label:
                x1,y1,x2,y2=map(int,re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',n.attrib['bounds']).groups())
                if x2>x1 and y2>y1:
                    run('adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
                    time.sleep(1)
                    return
        w,h=map(int,re.search(r'(\d+)x(\d+)',run('adb','shell','wm','size')).groups())
        # Find the actual native toolbar; a fixed y can swipe the panel instead.
        nav_y = None
        for n in ET.fromstring(ui()).iter('node'):
            if n.attrib.get('class')=='android.widget.Button' and n.attrib.get('text') in ['Inicio','Coche','Instrumentos','Eficiencia','Radio','Web','Fotos','Vídeos','TV','Herramientas GPS']:
                x1,y1,x2,y2=map(int,re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',n.attrib['bounds']).groups())
                if x2>x1 and y2>y1:
                    nav_y=(y1+y2)//2
                    break
        assert nav_y is not None,ui()
        run('adb','shell','input','swipe',str(w*4//5),str(nav_y),str(w//4),str(nav_y),'300')
        time.sleep(.4)
    raise AssertionError('No se encontró '+label+'\\n'+ui())

run('adb','shell','wm','size','1920x1080')
run('adb','shell','wm','density','160')
run('adb','install','/tmp/native.apk')
run('adb','shell','pm','grant',PKG,'android.permission.ACCESS_COARSE_LOCATION')
run('adb','shell','pm','grant',PKG,'android.permission.ACCESS_FINE_LOCATION')
run('adb','shell','am','start','-W','-n',PKG+'/com.headqlink.link.C10NativeActivity')
time.sleep(2)
assert 'C10 Nativa Plus' in ui()
assert 'Paneles originales de Qonecta+' in ui()
for name in ['Instrumentos','Eficiencia','Coche','Radio']:
    tap(name)
    screen=ui()
    assert 'No se ha podido abrir' not in screen,screen
    assert run('adb','shell','pidof',PKG).strip()
tap('Herramientas GPS')
assert 'HERRAMIENTAS C10' in ui()
run('adb','shell','input','keyevent','4')
time.sleep(1)
assert 'C10 Nativa Plus' in ui()
services=run('adb','shell','dumpsys','activity','services',PKG)
assert 'com.headqlink.link.LinkService' not in services,services
assert 'com.andrerinas.openheadunit.aap.AapService' not in services,services
print('PASS native host: actual Qonecta panels, own tools, foreground lifecycle; no projection service.')
print('Only emulator checks; no vehicle acceptance, cloud data, media streaming or camera access verified.')
