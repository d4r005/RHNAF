#!/usr/bin/env python3
"""
employee_sync.py
-------------------
Hermano de attendance_sync.py. Corre esta PC en la RED LOCAL de la planta
(la que sí llega a la lectora Hikvision por IP, 10.141.1.230).

Este script JALA (pull) de la lectora, via ISAPI:
  1) La lista COMPLETA de empleados/usuarios dados de alta en el equipo
     (ISAPI/AccessControl/UserInfo/Search) -> employeeNo, nombre.
  2) La FOTO de rostro de cada uno, si el equipo la expone
     (ISAPI/Intelligent/FDLib/FDSearch, respuesta multipart con la foto en JPEG).

Y lo EMPUJA (push) al servidor en la nube, endpoint nuevo:
    POST /api/v1/empleados/sync-device

Ese endpoint da de alta en el Módulo de Empleados a cualquier employeeNo que
todavía no exista en el sistema (por eso en Asistencia se veían checadas sin
nombre, ej. 1133, 1156, 1046: existen en la lectora pero no estaban en la
ficha de empleado) y guarda la foto para mostrarla ahi.

Requisitos:
    pip install requests

Uso:
    python employee_sync.py                 (una corrida, jala todo y sube)
    python employee_sync.py --sin-fotos     (mas rapido: solo nombres/IDs, sin fotos)
    python employee_sync.py --debug         (imprime el primer registro crudo de
                                              cada llamada ISAPI, util si tu firmware
                                              usa nombres de campo distintos)

NOTA SOBRE LA FOTO: el endpoint exacto para bajar fotos varía un poco según el
modelo/firmware de la lectora Hikvision. Este script intenta el endpoint mas
comun (ISAPI/Intelligent/FDLib/FDSearch). Si tu equipo no lo soporta, verás un
mensaje "[FOTOS] no se pudieron obtener" en la consola pero los EMPLEADOS
(nombre/ID/depto) se suben igual — corre con --debug y mándame la salida para
ajustar el endpoint de fotos a tu firmware exacto.
"""

import argparse
import json
import sys

import requests
from requests.auth import HTTPDigestAuth

# ----------------------- CONFIGURACION -----------------------
DEVICE_IP = "10.141.1.230"
DEVICE_USER = "admin"
DEVICE_PASS = "Branco2025"          # <-- cambia si tu password es distinto

CLOUD_SYNC_URL = "https://d4r005-rhnaf-industrial.hf.space/api/v1/empleados/sync-device"

BATCH_SIZE = 30           # usuarios por página (típico soportado por ISAPI)
MAX_PAGES = 100           # tope de páginas (=3000 usuarios) para no quedarse pegado
UPLOAD_CHUNK = 25         # cuantos empleados se mandan por request al servidor
FACE_LIB_ID = "1"         # ID de la biblioteca de rostros, casi siempre "1"
# ---------------------------------------------------------------


def fetch_user_list(position: int = 0, debug: bool = False):
    """Una página de la lista de usuarios/empleados dados de alta en el equipo."""
    url = f"http://{DEVICE_IP}/ISAPI/AccessControl/UserInfo/Search?format=json"
    body = {
        "UserInfoSearchCond": {
            "searchID": "1",
            "searchResultPosition": position,
            "maxResults": BATCH_SIZE,
        }
    }
    resp = requests.post(url, json=body, auth=HTTPDigestAuth(DEVICE_USER, DEVICE_PASS), timeout=15)
    if resp.status_code != 200:
        print(f"  [DEBUG] UserInfo/Search status={resp.status_code} body={resp.text[:400]}")
    resp.raise_for_status()
    data = resp.json()
    if debug and position == 0:
        print("[DEBUG] Primer usuario crudo de la lectora:")
        try:
            print(json.dumps(data.get("UserInfoSearch", {}).get("UserInfo", [])[0], indent=2, ensure_ascii=False))
        except Exception:
            print(json.dumps(data, indent=2, ensure_ascii=False)[:800])
    return data


def fetch_all_users(debug: bool = False):
    """Pagina hasta traer TODOS los empleados dados de alta en la lectora."""
    users = []
    position = 0
    for _ in range(MAX_PAGES):
        data = fetch_user_list(position, debug=debug)
        search = data.get("UserInfoSearch", {})
        info_list = search.get("UserInfo", [])
        if not info_list:
            break
        for u in info_list:
            employee_no = str(u.get("employeeNo", "")).strip()
            if not employee_no:
                continue
            users.append({
                "employeeNo": employee_no,
                "name": u.get("name", "") or "",
            })
        num_matches = search.get("numOfMatches", len(info_list))
        position += num_matches
        status = search.get("responseStatusStrg", "OK")
        if status != "MORE" or num_matches < BATCH_SIZE:
            break
    return users


def parse_multipart_faces(resp: requests.Response, debug: bool = False):
    """
    La búsqueda de rostros (FDLib/FDSearch) responde multipart/mixed: por cada
    registro manda una parte JSON (con el employeeNo/FPID) seguida de una parte
    binaria JPEG con la foto. Aqui separamos por el boundary y emparejamos
    JSON -> foto en el orden en que llegan.
    """
    content_type = resp.headers.get("Content-Type", "")
    if "boundary=" not in content_type:
        if debug:
            print(f"  [DEBUG-FOTOS] Content-Type inesperado: {content_type}")
        return {}

    boundary = content_type.split("boundary=")[-1].strip().strip('"')
    raw = resp.content
    parts = raw.split(("--" + boundary).encode())

    photos_by_employee = {}
    pending_employee_no = None

    for part in parts:
        if not part or part in (b"--", b"--\r\n"):
            continue
        if b"\r\n\r\n" not in part:
            continue
        header_blob, body_blob = part.split(b"\r\n\r\n", 1)
        header_text = header_blob.decode(errors="ignore")
        body_blob = body_blob.rstrip(b"\r\n")

        if "application/json" in header_text:
            try:
                meta = json.loads(body_blob.decode(errors="ignore"))
                face_info = meta.get("FaceInfo", meta)
                pending_employee_no = str(
                    face_info.get("employeeNo") or face_info.get("FPID") or ""
                ).strip() or None
            except Exception:
                pending_employee_no = None
        elif ("image/jpeg" in header_text or "image/jpg" in header_text) and pending_employee_no:
            import base64
            photos_by_employee[pending_employee_no] = base64.b64encode(body_blob).decode("ascii")
            pending_employee_no = None

    return photos_by_employee


def fetch_all_face_photos(debug: bool = False):
    """Trae TODAS las fotos de rostro registradas en la lectora, indexadas por employeeNo."""
    url = f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib/FDSearch?format=json"
    photos = {}
    position = 0
    for _ in range(MAX_PAGES):
        body = {
            "FDSearchCond": {
                "searchResultPosition": position,
                "maxResults": BATCH_SIZE,
                "faceLibType": "staticFD",
                "FDID": FACE_LIB_ID,
            }
        }
        try:
            resp = requests.post(
                url, json=body, auth=HTTPDigestAuth(DEVICE_USER, DEVICE_PASS),
                timeout=20, stream=False,
            )
        except requests.RequestException as e:
            print(f"  [FOTOS] error de conexion al pedir fotos: {e}")
            return photos

        if resp.status_code != 200:
            print(f"  [FOTOS] no se pudieron obtener (status={resp.status_code}). "
                  f"Los empleados se subiran sin foto. Respuesta: {resp.text[:300]}")
            return photos

        batch = parse_multipart_faces(resp, debug=debug)
        if not batch:
            break
        photos.update(batch)
        if len(batch) < BATCH_SIZE:
            break
        position += BATCH_SIZE

    return photos


def push_employees_to_cloud(rows: list):
    """Sube empleados (con o sin foto) en tandas al endpoint nuevo del servidor."""
    total_creados = total_actualizados = total_fotos = 0
    for i in range(0, len(rows), UPLOAD_CHUNK):
        chunk = rows[i:i + UPLOAD_CHUNK]
        try:
            resp = requests.post(CLOUD_SYNC_URL, json=chunk, timeout=30)
            resp.raise_for_status()
            result = resp.json()
            total_creados += result.get("creados", 0)
            total_actualizados += result.get("actualizados", 0)
            total_fotos += result.get("fotosGuardadas", 0)
            print(f"  Tanda {i // UPLOAD_CHUNK + 1}: {result.get('mensaje', result)}")
        except requests.RequestException as e:
            print(f"  [ERROR] no se pudo subir la tanda {i // UPLOAD_CHUNK + 1}: {e}")
    return total_creados, total_actualizados, total_fotos


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sin-fotos", action="store_true", help="Solo sube nombre/ID/depto, sin bajar fotos (mas rapido)")
    parser.add_argument("--debug", action="store_true", help="Imprime respuestas crudas para diagnosticar el formato del firmware")
    args = parser.parse_args()

    print(f"Consultando lista completa de empleados en la lectora {DEVICE_IP} ...")
    try:
        users = fetch_all_users(debug=args.debug)
    except requests.RequestException as e:
        print(f"[ERROR] no se pudo conectar a la lectora: {e}")
        sys.exit(1)

    print(f"  {len(users)} empleados encontrados en la lectora.")

    photos = {}
    if not args.sin_fotos:
        print("Consultando fotos de rostro registradas ...")
        photos = fetch_all_face_photos(debug=args.debug)
        print(f"  {len(photos)} fotos encontradas.")

    rows = []
    for u in users:
        rows.append({
            "employeeNo": u["employeeNo"],
            "name": u["name"],
            "photoBase64": photos.get(u["employeeNo"]),
        })

    print(f"Subiendo {len(rows)} empleados al servidor en la nube ...")
    creados, actualizados, fotos_guardadas = push_employees_to_cloud(rows)
    print(f"Listo. Nuevos: {creados} | Actualizados: {actualizados} | Con foto: {fotos_guardadas}")


if __name__ == "__main__":
    main()
