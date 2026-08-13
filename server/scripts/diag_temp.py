import requests
import json
from requests.auth import HTTPDigestAuth

AUTH = HTTPDigestAuth('admin', 'Branco2025')
BASE = 'http://10.141.1.230'

print('--- Snapshot de camara ---')
for ch in [1, 101, 201]:
    try:
        r = requests.get(f'{BASE}/ISAPI/Streaming/channels/{ch}/picture', auth=AUTH, timeout=10)
        ct = r.headers.get('Content-Type', '')
        print(f'  canal {ch}: status={r.status_code} ct={ct} size={len(r.content)}')
        if r.status_code == 200 and 'image' in ct:
            open(f'diag_snap_{ch}.jpg', 'wb').write(r.content)
            print(f'  *** GUARDADO diag_snap_{ch}.jpg ***')
    except Exception as e:
        print(f'  canal {ch}: ERROR {e}')

print('--- Device Info ---')
try:
    r = requests.get(f'{BASE}/ISAPI/System/deviceInfo?format=json', auth=AUTH, timeout=10)
    print(f'  status={r.status_code}')
    if r.status_code == 200:
        d = r.json()
        info = d.get('DeviceInfo', {})
        print(f'  deviceName: {info.get("deviceName", "?")}')
        print(f'  deviceType: {info.get("deviceType", "?")}')
        print(f'  model: {info.get("model", "?")}')
        print(f'  firmwareVersion: {info.get("firmwareVersion", "?")}')
except Exception as e:
    print(f'  ERROR: {e}')

print('--- Endpoints alternativos ---')
paths = [
    '/ISAPI/AccessControl/UserInfo/1341/faceData',
    '/ISAPI/AccessControl/faceImage/1341',
    '/ISAPI/AccessControl/capture/1341',
    '/ISAPI/Intelligent/FDLib/1/faceImage/1341',
    '/ISAPI/AccessControl/FaceCapture/1341',
    '/ISAPI/AccessControl/CaptureResult?employeeNo=1341',
]
for path in paths:
    try:
        r = requests.get(f'{BASE}{path}', auth=AUTH, timeout=10)
        ct = r.headers.get('Content-Type', '')
        print(f'  {path}: status={r.status_code} ct={ct} size={len(r.content)}')
        if r.status_code == 200 and 'image' in ct:
            open('diag_alt.jpg', 'wb').write(r.content)
            print('  *** GUARDADO diag_alt.jpg ***')
    except Exception as e:
        print(f'  {path}: ERROR {e}')

print('--- Eventos con picEnable ---')
try:
    body = {'AcsEventCond': {'searchID': '1', 'searchResultPosition': 0, 'maxResults': 3, 'major': 0, 'minor': 0, 'startTime': '2026-08-13T00:00:00-06:00', 'endTime': '2026-08-13T23:59:59-06:00', 'picEnable': True}}
    r = requests.post(f'{BASE}/ISAPI/AccessControl/AcsEvent?format=json', json=body, auth=AUTH, timeout=20)
    ct = r.headers.get('Content-Type', '')
    print(f'  status={r.status_code} ct={ct} size={len(r.content)}')
    if 'multipart' in ct:
        open('diag_event_pic.bin', 'wb').write(r.content)
        print('  *** MULTIPART GUARDADO diag_event_pic.bin ***')
    elif 'json' in ct and r.status_code == 200:
        d = r.json()
        events = d.get('AcsEvent', {}).get('InfoList', [])
        for i, e in enumerate(events[:3]):
            print(f'  Evento {i+1}: {json.dumps(e, ensure_ascii=False)[:500]}')
            for k in e:
                if any(x in k.lower() for x in ['pic', 'image', 'face', 'url', 'photo']):
                    print(f'    *** CAMPO: {k}={str(e[k])[:200]}')
except Exception as e:
    print(f'  ERROR: {e}')

print('--- DIAGNOSTICO COMPLETADO ---')
