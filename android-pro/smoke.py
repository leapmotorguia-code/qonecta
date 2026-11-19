import subprocess,time,xml.etree.ElementTree as E,re,pathlib
P='es.c10.controlpro'
def run(*a):
 p=subprocess.run(a,capture_output=True,text=True)
 if p.returncode: raise RuntimeError(str(a)+'\n'+p.stdout+p.stderr)
 return p.stdout
def ui():
 run('adb','shell','uiautomator','dump','/sdcard/pro-ui.xml');return run('adb','exec-out','cat','/sdcard/pro-ui.xml')
def tap(label):
 for i in range(12):
  s=ui()
  for n in E.fromstring(s).iter('node'):
   if n.attrib.get('text','').casefold()==label.casefold():
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']));
    if x2>x1 and y2>y1:run('adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.6);return
  root=E.fromstring(s); container=next((n for n in root.iter('node') if n.attrib.get('scrollable')=='true'),root[0]); x1,y1,x2,y2=map(int,re.findall(r'\d+',container.attrib['bounds'])); x=(x1+x2)//2; low=int(y1+(y2-y1)*.2);high=int(y1+(y2-y1)*.8);a,b=(high,low) if i<6 else (low,high);run('adb','shell','input','swipe',str(x),str(a),str(x),str(b),'250');time.sleep(.3)
 raise AssertionError('Missing '+label+'\n'+s)
run('adb','install','-r','deliverable/C10-Control-Pro-0.2.apk')
run('adb','shell','am','start','-W','-n',P+'/.MainActivity');time.sleep(2)
assert 'C10 CONTROL PRO' in ui()
tap('Diagnóstico');assert 'API 30' in ui();assert P in ui()
tap('Volver al inicio');tap('Costes y regeneración estimada');tap('Calcular costes separados');assert 'Introduce valores válidos' in ui()
tap('Estimar energía');assert 'Introduce valores válidos' in ui()
run('adb','shell','am','force-stop',P);run('adb','shell','am','start','-W','-n',P+'/.MainActivity');time.sleep(1)
tap('Velocidad y mediciones GPS');assert '3–100 NO equivale a 0–100' in ui();tap('Detener medición');assert 'GPS detenido' in ui()
tap('Volver al inicio');tap('Accesos multimedia');s=ui()
for x in ['Netflix','HBO / Max','Pluto TV','Google','Amazon Prime Video']:tap(x);assert 'Uso estacionado' in ui();tap('Cancelar')
assert run('adb','shell','pidof',P).strip()
run('adb','shell','am','force-stop',P)
pathlib.Path('deliverable/emulador-api30.txt').write_text('PASS: exact APK installed and launched on Android11/API30; diagnostics; invalid manual inputs; GPS screen and stop; Netflix, HBO/Max, Pluto TV, Google, Amazon Prime links show parked confirmation. No streaming, GPS reception or real LeapOS installation tested.\n')
