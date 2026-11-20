import subprocess,time,xml.etree.ElementTree as E,re,pathlib
P='es.c10.complemento'
def run(*a):
 p=subprocess.run(a,capture_output=True,text=True)
 if p.returncode: raise RuntimeError(str(a)+'\n'+p.stdout+p.stderr)
 return p.stdout
def ui():
 run('adb','shell','uiautomator','dump','/sdcard/lab-ui.xml');return run('adb','exec-out','cat','/sdcard/lab-ui.xml')
def tap(label):
 for i in range(16):
  s=ui();root=E.fromstring(s)
  for n in root.iter('node'):
   if n.attrib.get('text','').casefold()==label.casefold():
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']))
    if x2>x1 and y2>y1:run('adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.5);return
  container=next((n for n in root.iter('node') if n.attrib.get('scrollable')=='true'),root[0]);x1,y1,x2,y2=map(int,re.findall(r'\d+',container.attrib['bounds']));x=(x1+x2)//2;lo=int(y1+(y2-y1)*.2);hi=int(y1+(y2-y1)*.8);a,b=(hi,lo) if i<8 else(lo,hi);run('adb','shell','input','swipe',str(x),str(a),str(x),str(b),'200')
 raise AssertionError(label+'\n'+s)
def home():
 run('adb','shell','am','force-stop',P);run('adb','shell','am','start','-W','-n',P+'/.MainActivity');time.sleep(.6)
run('adb','install','-r','deliverable/C10-Complemento-1.3.apk');home();assert 'C10 COMPLEMENTO 1.3' in ui()
tap('Abrir UNIT original');assert 'No se encuentra UNIT' in ui();home();tap('Accesos disponibles');assert 'observaciones de esta aplicación' in ui();assert 'SIN FUENTE DE DATOS INTEGRADA' in ui();home();tap('Instrumentos GPS');tap('Ejecutar demostración');s=ui();assert 'DEMO' in s;assert '3–100' in s;assert '80–120' in s
home();tap('Diagnóstico y exportación');s=ui();assert 'API 30' in s;assert P in s;assert 'DEMO' in s
home();tap('Eficiencia manual');tap('Calcular costes separados');assert 'Introduce valores válidos' in ui();tap('Estimar energía');assert 'Introduce valores válidos' in ui()
home();tap('Vibración del dispositivo');tap('Detener vigilancia');assert 'Vigilancia detenida' in ui()
home();tap('Accesos multimedia')
for label in ['Google','Netflix','Prime Video','HBO / Max','Pluto TV','Spotify']:
 tap(label);assert 'Uso estacionado' in ui();tap('Cancelar')
assert run('adb','shell','pidof',P).strip()
pathlib.Path('deliverable/emulador-api30.txt').write_text('PASS: exact release APK installed and launched on Android11/API30. Demo crossing results, persisted history, diagnostics, invalid manual input, vibration stop, all six external-link parked dialogs. No actual GPS signal, streaming, vehicle telemetry or LeapOS installation tested.\n')
