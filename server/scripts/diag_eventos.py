#!/usr/bin/env python3
"""
diag_eventos.py — Diagnostico de fotos en eventos de checada
Busca si la lectora guarda foto capturada en cada evento de checada.

Uso:
    python diag_eventos.py
"""
import sys
import json
import base64
import requests
from requests.auth import HTTPDigestAuth

DEVICE_IP = "10.141.1.230"
DEVICE_USER = "admin"
DEVICE_PASS = "Branco2025"
AUTH = HTTPDigestAuth(DEVICE_USER, DEVICE_PASS)

def main():
    print("\n=== DIAGNOSTICO DE FOTOS EN EVENTOS ===\n")

    # 1. Capabilities de AcsEvent
    print("--- 1. AcsEvent capabilities ---")
    try:
        resp = requests.get(f"http://{DEVICE_IP}/ISAPI/AccessControl/AcsEvent/capabilities?format=json", auth=AUTH, timeout=10)
        print(f"  status={resp.status_code}")
        if resp.status_code == 200:
            data = resp.json()
            print(f"  {json.dumps(data, indent=2, ensure_ascii=False)[:1000]}")
    except Exception as e:
        print(f"  ERROR: {e}")

    # 2. Buscar eventos recientes y ver si tienen pictureURL
    print("\n--- 2. Eventos recientes (buscando pictureURL) ---")
    try:
        body = {
            "AcsEventCond": {
                "searchID": "1",
                "searchResultPosition": 0,
                "maxResults": 3,
                "major": 0,
                "minor": 0,
                "startTime": "2026-08-13T00:00:00-06:00",
                "endTime": "2026-08-13T23:59:59-06:00",
            }
        }
        resp = requests.post(f"http://{DEVICE_IP}/ISAPI/AccessControl/AcsEvent?format=json", json=body, auth=AUTH, timeout=15)
        print(f"  status={resp.status_code}")
        if resp.status_code == 200:
            data = resp.json()
            acs = data.get("AcsEvent", {})
            info_list = acs.get("InfoList", [])
            print(f"  {len(info_list)} eventos encontrados")
            for i, ev in enumerate(info_list[:3]):
                print(f"\n  --- Evento {i+1} ---")
                print(f"  {json.dumps(ev, indent=2, ensure_ascii=False)[:800]}")
                # Buscar campos de imagen
                for k, v in ev.items():
                    if any(x in k.lower() for x in ["pic", "image", "face", "url", "photo", "capture"]):
                        print(f"  *** CAMPO DE IMAGEN: {k} = {str(v)[:300]}")
    except Exception as e:
        print(f"  ERROR: {e}")

    # 3. Probar endpoints de picture de eventos
    print("\n--- 3. Endpoints de EventPic ---")
    for url in [
        f"http://{DEVICE_IP}/ISAPI/AccessControl/AcsEventPic?format=json",
        f"http://{DEVICE_IP}/ISAPI/AccessControl/EventPic/1",
        f"http://{DEVICE_IP}/ISAPI/AccessControl/AcsEvent/1/picture",
        f"http://{DEVICE_IP}/ISAPI/Streaming/channels/1/picture",
        f"http://{DEVICE_IP}/ISAPI/Streaming/channels/1/picture?format=json",
    ]:
        try:
            resp = requests.get(url, auth=AUTH, timeout=10)
            ct = resp.headers.get("Content-Type", "")
            print(f"  GET {url.split('/ISAPI')[-1]} -> status={resp.status_code} ct={ct} size={len(resp.content)}")
            if resp.status_code == 200 and "image" in ct:
                print(f"    *** IMAGEN! {len(resp.content)} bytes ***")
                with open(f"diag_event_test.jpg", "wb") as f:
                    f.write(resp.content)
        except Exception as e:
            print(f"  ERROR: {e}")

    # 4. Buscar eventos con needEventPic
    print("\n--- 4. Eventos con needEventPic=true ---")
    try:
        body2 = {
            "AcsEventCond": {
                "searchID": "1",
                "searchResultPosition": 0,
                "maxResults": 3,
                "major": 0,
                "minor": 0,
                "startTime": "2026-08-13T00:00:00-06:00",
                "endTime": "2026-08-13T23:59:59-06:00",
                "needEventPic": True,
            }
        }
        resp = requests.post(f"http://{DEVICE_IP}/ISAPI/AccessControl/AcsEvent?format=json", json=body2, auth=AUTH, timeout=15)
        print(f"  status={resp.status_code}")
        if resp.status_code == 200:
            ct = resp.headers.get("Content-Type", "")
            print(f"  Content-Type={ct} size={len(resp.content)}")
            if "multipart" in ct:
                print(f"  *** MULTIPART! Guardando... ***")
                with open("diag_event_multipart.bin", "wb") as f:
                    f.write(resp.content)
            elif "json" in ct:
                data = resp.json()
                acs = data.get("AcsEvent", {})
                info_list = acs.get("InfoList", [])
                for i, ev in enumerate(info_list[:3]):
                    print(f"\n  Evento {i+1} con needEventPic:")
                    print(f"  {json.dumps(ev, indent=2, ensure_ascii=False)[:1000]}")
    except Exception as e:
        print(f"  ERROR: {e}")

    # 5. Snapshot endpoint (camara en vivo)
    print("\n--- 5. Snapshot de camara ---")
    for ch in [1, 101, 201]:
        try:
            resp = requests.get(f"http://{DEVICE_IP}/ISAPI/Streaming/channels/{ch}/picture", auth=AUTH, timeout=10)
            ct = resp.headers.get("Content-Type", "")
            print(f"  GET /Streaming/channels/{ch}/picture -> status={resp.status_code} ct={ct} size={len(resp.content)}")
            if resp.status_code == 200 and "image" in ct:
                print(f"    *** IMAGEN! Guardando como diag_snapshot_{ch}.jpg ***")
                with open(f"diag_snapshot_{ch}.jpg", "wb") as f:
                    f.write(resp.content)
        except Exception as e:
            print(f"  ERROR: {e}")

    # 6. Probar AcsEvent con picEnable
    print("\n--- 6. Eventos con picEnable ---")
    try:
        body3 = {
            "AcsEventCond": {
                "searchID": "1",
                "searchResultPosition": 0,
                "maxResults": 3,
                "major": 0,
                "minor": 0,
                "startTime": "2026-08-13T00:00:00-06:00",
                "endTime": "2026-08-13T23:59:59-06:00",
                "picEnable": True,
            }
        }
        resp = requests.post(f"http://{DEVICE_IP}/ISAPI/AccessControl/AcsEvent?format=json", json=body3, auth=AUTH, timeout=20)
        print(f"  status={resp.status_code}")
        ct = resp.headers.get("Content-Type", "")
        print(f"  Content-Type={ct} size={len(resp.content)}")
        if "multipart" in ct:
            print(f"  *** MULTIPART! Guardando... ***")
            with open("diag_event_pic.bin", "wb") as f:
                f.write(resp.content)
            # Tambien intentar parsear
            boundary = ct.split("boundary=")[-1].strip().strip('"')
            parts = resp.content.split(("--" + boundary).encode())
            print(f"  Partes multipart: {len([p for p in parts if p and p not in (b'--', b'--\\r\\n')])}")
        elif "json" in ct:
            data = resp.json()
            print(f"  {json.dumps(data, indent=2, ensure_ascii=False)[:1000]}")
    except Exception as e:
        print(f"  ERROR: {e}")

    print("\n=== DIAGNOSTICO COMPLETADO ===")

if __name__ == "__main__":
    main()
