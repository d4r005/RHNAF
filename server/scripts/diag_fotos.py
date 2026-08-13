#!/usr/bin/env python3
"""
diag_fotos.py — Diagnostico de endpoints de fotos para lectora Hikvision
Corre esto en la PC de la planta. Prueba muchos endpoints ISAPI diferentes
en un solo empleado para descubrir cual funciona para bajar la foto de rostro.

Uso:
    python diag_fotos.py                    (usa employeeNo 1341 por default)
    python diag_fotos.py 1001                (usa employeeNo 1001)
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

def try_get(url, label):
    try:
        resp = requests.get(url, auth=AUTH, timeout=10)
        ct = resp.headers.get("Content-Type", "")
        size = len(resp.content)
        print(f"  [{label}] GET {url.split('/ISAPI')[-1]}")
        print(f"    -> status={resp.status_code} Content-Type={ct} size={size}")
        if resp.status_code == 200:
            if "image" in ct:
                print(f"    *** IMAGEN JPEG! {size} bytes ***")
                # Guardar la imagen para verificacion
                with open(f"diag_test_{label}.jpg", "wb") as f:
                    f.write(resp.content)
                print(f"    *** Guardada como diag_test_{label}.jpg ***")
                return True
            elif "multipart" in ct:
                print(f"    *** MULTIPART! Guardando... ***")
                with open(f"diag_test_{label}.bin", "wb") as f:
                    f.write(resp.content)
                print(f"    *** Guardada como diag_test_{label}.bin ***")
                return True
            elif "json" in ct:
                data = resp.json()
                # Buscar campos de foto
                print(f"    JSON keys: {list(data.keys())[:10]}")
                print(f"    JSON (primeros 500 chars): {json.dumps(data, ensure_ascii=False)[:500]}")
                # Buscar faceURL, faceData, etc
                for k, v in data.items():
                    if any(x in k.lower() for x in ["face", "url", "pic", "image", "data"]):
                        print(f"    *** Campo relevante: {k}={str(v)[:200]}")
                return False
            else:
                print(f"    Body (primeros 200 bytes): {resp.content[:200]}")
                return False
        else:
            if resp.text:
                print(f"    Body: {resp.text[:300]}")
            return False
    except Exception as e:
        print(f"  [{label}] ERROR: {e}")
        return False

def try_post(url, body, label):
    try:
        resp = requests.post(url, json=body, auth=AUTH, timeout=10)
        ct = resp.headers.get("Content-Type", "")
        size = len(resp.content)
        print(f"  [{label}] POST {url.split('/ISAPI')[-1]}")
        print(f"    body={json.dumps(body, ensure_ascii=False)[:200]}")
        print(f"    -> status={resp.status_code} Content-Type={ct} size={size}")
        if resp.status_code == 200:
            if "image" in ct:
                print(f"    *** IMAGEN JPEG! {size} bytes ***")
                with open(f"diag_test_{label}.jpg", "wb") as f:
                    f.write(resp.content)
                return True
            elif "multipart" in ct:
                print(f"    *** MULTIPART! Guardando... ***")
                with open(f"diag_test_{label}.bin", "wb") as f:
                    f.write(resp.content)
                return True
            elif "json" in ct:
                data = resp.json()
                print(f"    JSON keys: {list(data.keys())[:10]}")
                print(f"    JSON (primeros 500 chars): {json.dumps(data, ensure_ascii=False)[:500]}")
                return False
            else:
                print(f"    Body (primeros 200 bytes): {resp.content[:200]}")
                return False
        else:
            if resp.text:
                print(f"    Body: {resp.text[:300]}")
            return False
    except Exception as e:
        print(f"  [{label}] ERROR: {e}")
        return False

def main():
    emp_no = sys.argv[1] if len(sys.argv) > 1 else "1341"
    print(f"\n=== DIAGNOSTICO DE FOTOS PARA EMPLEADO {emp_no} ===\n")
    print(f"Lectora: {DEVICE_IP}\n")

    # 1. Listar librerias de rostros disponibles
    print("--- 1. Listar librerias FDLib ---")
    try_get(f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib?format=json", "lib_list")
    try_get(f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib/1?format=json", "lib_1")
    try_get(f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib/2?format=json", "lib_2")

    # 2. Listar registros en cada libreria
    print("\n--- 2. Listar registros FDDataRecord ---")
    for fdid in [1, 2]:
        try_get(f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib/{fdid}/FDDataRecord?format=json", f"records_{fdid}")
        try_get(f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib/{fdid}/FDDataRecord/{emp_no}?format=json", f"record_{fdid}_{emp_no}")

    # 3. faceDataPicture con diferentes FDIDs
    print("\n--- 3. faceDataPicture ---")
    for fdid in [1, 2]:
        try_get(f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib/{fdid}/faceDataPicture/{emp_no}", f"facepic_{fdid}")

    # 4. Endpoints de AccessControl
    print("\n--- 4. AccessControl face endpoints ---")
    try_get(f"http://{DEVICE_IP}/ISAPI/AccessControl/UserInfo/{emp_no}/faceImage", "ac_faceimg")
    try_get(f"http://{DEVICE_IP}/ISAPI/AccessControl/UserInfo/{emp_no}/faceImage?format=json", "ac_faceimg_json")
    try_get(f"http://{DEVICE_IP}/ISAPI/AccessControl/UserFaceData/{emp_no}", "ac_facedata")
    try_get(f"http://{DEVICE_IP}/ISAPI/AccessControl/UserFaceData/{emp_no}?format=json", "ac_facedata_json")
    try_get(f"http://{DEVICE_IP}/ISAPI/AccessControl/FaceData/{emp_no}", "ac_facedata2")
    try_get(f"http://{DEVICE_IP}/ISAPI/AccessControl/FaceData/{emp_no}?format=json", "ac_facedata2_json")

    # 5. POST busquedas
    print("\n--- 5. POST busquedas ---")
    try_post(
        f"http://{DEVICE_IP}/ISAPI/AccessControl/UserFaceData/Search?format=json",
        {"UserFaceDataSearchCond": {"searchID": "1", "searchResultPosition": 0, "maxResults": 5, "employeeNo": [emp_no]}},
        "post_facedata_search"
    )
    try_post(
        f"http://{DEVICE_IP}/ISAPI/AccessControl/UserFaceDataSearch?format=json",
        {"UserFaceDataSearchCond": {"searchID": "1", "searchResultPosition": 0, "maxResults": 5, "employeeNo": [emp_no]}},
        "post_facedatasearch"
    )
    try_post(
        f"http://{DEVICE_IP}/ISAPI/AccessControl/FaceData/Search?format=json",
        {"FaceDataSearchCond": {"searchID": "1", "searchResultPosition": 0, "maxResults": 5, "employeeNo": [emp_no]}},
        "post_face_search"
    )

    # 6. UserInfo completo con posibles campos de cara
    print("\n--- 6. UserInfo completo ---")
    try_post(
        f"http://{DEVICE_IP}/ISAPI/AccessControl/UserInfo/Search?format=json",
        {"UserInfoSearchCond": {"searchID": "1", "searchResultPosition": 0, "maxResults": 1, "employeeNo": [emp_no]}},
        "post_userinfo_emp"
    )

    # 7. Endpoints alternativos
    print("\n--- 7. Endpoints alternativos ---")
    try_get(f"http://{DEVICE_IP}/ISAPI/AccessControl/UserInfo/{emp_no}/faceDataRecord?format=json", "ac_facedatarecord")
    try_get(f"http://{DEVICE_IP}/ISAPI/AccessControl/UserInfo/{emp_no}?format=json", "ac_userinfo_single")
    try_get(f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib/1/faceDataRecord/{emp_no}?format=json", "fd_facedatarecord")

    # 8. capabilities completo
    print("\n--- 8. Capabilities completas ---")
    try_get(f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib/capabilities?format=json", "cap_full")
    try_get(f"http://{DEVICE_IP}/ISAPI/AccessControl/UserInfo/capabilities?format=json", "cap_userinfo")

    print(f"\n=== DIAGNOSTICO COMPLETADO ===")
    print(f"Revisa arriba cualquier linea con *** que indique que se encontro imagen o dato.")
    print(f"Si se guardo algun archivo diag_test_*.jpg, abrelo para ver si es la foto.")

if __name__ == "__main__":
    main()
