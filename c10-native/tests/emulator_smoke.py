import re
import subprocess
import time
import xml.etree.ElementTree as ET


def call(*args):
    return subprocess.run(args, check=True, capture_output=True, text=True).stdout


def ui():
    call('adb', 'shell', 'uiautomator', 'dump', '/sdcard/c10-test.xml')
    return call('adb', 'exec-out', 'cat', '/sdcard/c10-test.xml')


def tap_text(label):
    dims = re.search(r'(\d+)x(\d+)', call('adb', 'shell', 'wm', 'size'))
    width, height = map(int, dims.groups())
    xml = ''
    for _ in range(6):
        xml = ui()
        for node in ET.fromstring(xml).iter('node'):
            if node.attrib.get('text') == label:
                points = re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', node.attrib['bounds'])
                x1, y1, x2, y2 = map(int, points.groups())
                call('adb', 'shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
                time.sleep(1)
                return
        call('adb', 'shell', 'input', 'swipe', str(width // 2), str(height * 4 // 5), str(width // 2), str(height // 4), '350')
        time.sleep(1)
    raise AssertionError('No se encontró el botón: ' + label + '\n' + xml[-7000:])


call('adb', 'shell', 'pm', 'grant', 'com.leapmotorguia.c10gps', 'android.permission.ACCESS_FINE_LOCATION')
call('adb', 'shell', 'pm', 'grant', 'com.leapmotorguia.c10gps', 'android.permission.ACCESS_COARSE_LOCATION')
call('adb', 'shell', 'settings', 'put', 'secure', 'location_mode', '3')
tap_text('Abrir medición GPS')
call('adb', 'shell', 'dumpsys', 'activity', 'activities')
assert 'Medición por GPS' in ui(), 'La pantalla GPS no se abrió.'
tap_text('Preparar medición')
time.sleep(2)
for velocity in ['0', '0', '0', '0', '0', '6', '25', '60']:
    # Emulator console velocity is knots. These coordinates are synthetic.
    call('adb', 'emu', 'geo', 'fix', '2.12', '41.12', '30', '8', velocity)
    time.sleep(1.2)
found_result = False
found_history = False
dims = re.search(r'(\d+)x(\d+)', call('adb', 'shell', 'wm', 'size'))
width, height = map(int, dims.groups())
for _ in range(4):
    xml = ui()
    found_result |= bool(re.search(r'0–100: [0-9]+[.,][0-9]+ s', xml))
    found_history |= '0–100 km/h' in xml
    if found_result and found_history:
        break
    call('adb', 'shell', 'input', 'swipe', str(width // 2), str(height * 4 // 5), str(width // 2), str(height // 4), '350')
    time.sleep(1)
if not found_result or not found_history:
    print('TEXTOS VISIBLES:', [n.attrib.get('text') for n in ET.fromstring(xml).iter('node') if n.attrib.get('text')])
    print('DIAGNOSTICO DE UBICACION:', call('adb', 'shell', 'dumpsys', 'location')[-12000:])
assert found_result, 'No se obtuvo el 0–100 simulado.'
assert found_history, 'No se encontró el registro del resultado.'
print('Pantalla GPS y resultado simulado comprobados.')
