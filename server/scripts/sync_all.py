#!/usr/bin/env python3
"""
sync_all.py — Sincronizacion TOTAL en un solo script
=====================================================
Corre esto en la PC que esta en la RED LOCAL de la planta (la que llega
a la lectora Hikvision por IP 10.141.1.230). El servidor en la nube
(Hugging Face) NO puede llegar a la lectora porque esta fuera de esa red.

Este script combina en un solo ciclo:

  1) EMPLEADOS: jala la lista COMPLETA de empleados de la lectora
     (ISAPI/AccessControl/UserInfo/Search) con sus FOTOS de rostro
     (ISAPI/Intelligent/FDLib/FDSearch) y los sube al servidor en la nube.
     Los nuevos se dan de alta automaticamente; los existentes se
     actualizan (nombre, foto, departamento).

  2) ASISTENCIA: jala TODOS los eventos de checada desde la ultima vez
     que corrio (o desde --since si se fuerza) y los sube al servidor.

  3) REPARACION: llama al endpoint /backfill-metadata del servidor para
     que cualquier checada que se haya guardado sin nombre (porque el
     empleado aun no existia en la ficha) se le cruce con la ficha ya
     actualizada y le aparezca el nombre. Tambien llama a /normalize
     para respetar el limite de 1 Check-in + 1 Check-out por dia.

Requisitos:
    pip install requests

Uso:
    python sync_all.py                           (una corrida y termina)
    python sync_all.py --loop                    (corre para siempre, cada 5 min)
    python sync_all.py --loop --interval 60      (cada 60 seg en vez de 5 min)
    python sync_all.py --since 2026-01-01T00:00:00  (fuerza fecha de inicio para
                                                     vaciar el backlog historico)
    python sync_all.py --sin-fotos               (mas rapido: omite fotos)
    python sync_all.py --debug                   (imprime respuestas crudas)

Para dejarlo corriendo siempre en Windows:
    - Tarea programada de Windows que arranque sync_all.py --loop al encender
    - O dejar una terminal/consola abierta con: python sync_all.py --loop
"""

import argparse
import base64
import json
import os
import sys
import time
from datetime import datetime, timedelta

import requests
from requests.auth import HTTPDigestAuth

# ======================= CONFIGURACION =======================
DEVICE_IP = "10.141.1.230"
DEVICE_USER = "admin"
DEVICE_PASS = "Branco2025"

# URLs del servidor en la nube (Hugging Face Space)
CLOUD_BASE = "https://d4r005-rhnaf-industrial.hf.space"
CLOUD_ATTENDANCE_URL = f"{CLOUD_BASE}/api/v1/asistencia/hikvision"
CLOUD_EMPLOYEE_SYNC_URL = f"{CLOUD_BASE}/api/v1/empleados/sync-device"
CLOUD_BACKFILL_URL = f"{CLOUD_BASE}/api/v1/asistencia/backfill-metadata"
CLOUD_NORMALIZE_URL = f"{CLOUD_BASE}/api/v1/asistencia/normalize"

# Offset de zona horaria (CDMX, fijo -06:00 desde 2022)
TZ_OFFSET = "-06:00"

# Paginacion ISAPI
BATCH_SIZE = 30
MAX_PAGES_ATTENDANCE = 50    # 1500 eventos max por corrida
MAX_PAGES_EMPLOYEES = 100   # 3000 usuarios max
MAX_PAGES_FACES = 100
FACE_LIB_ID = "1"

# Loop
DEFAULT_INTERVAL = 300       # 5 minutos
DEFAULT_LOOKBACK_DAYS = 7

# Archivo de estado (checkpoint de la ultima checada subida)
STATE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sync_state.json")
UPLOAD_CHUNK = 25            # empleados por request al servidor
# =============================================================


def log(msg: str):
    """Print con timestamp para que se vea ordenado en la consola."""
    ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{ts}] {msg}")


# -------------------- utilidades ISAPI --------------------

def with_tz(ts: str) -> str:
    """Le agrega el offset de zona horaria a un timestamp ISO8601 si no lo trae."""
    if not ts:
        return ts
    tail = ts[10:]
    if "+" in tail or "-" in tail or tail.endswith("Z"):
        return ts
    return ts + TZ_OFFSET


# -------------------- estado (checkpoint) --------------------

def load_state():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, "w" if False else "r", encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_state(state):
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=2)


# -------------------- PARTE 1: EMPLEADOS --------------------

def fetch_user_list(position: int = 0, debug: bool = False):
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
        log(f"  [EMP] UserInfo/Search status={resp.status_code} body={resp.text[:400]}")
    resp.raise_for_status()
    data = resp.json()
    if debug and position == 0:
        log("[DEBUG] Primer usuario crudo de la lectora:")
        try:
            print(json.dumps(data.get("UserInfoSearch", {}).get("UserInfo", [])[0], indent=2, ensure_ascii=False))
        except Exception:
            print(json.dumps(data, indent=2, ensure_ascii=False)[:800])
    return data


def fetch_all_users(debug: bool = False):
    users = []
    position = 0
    for _ in range(MAX_PAGES_EMPLOYEES):
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
    content_type = resp.headers.get("Content-Type", "")
    if "boundary=" not in content_type:
        if debug:
            log(f"  [FOTOS] Content-Type inesperado: {content_type}")
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
            photos_by_employee[pending_employee_no] = base64.b64encode(body_blob).decode("ascii")
            pending_employee_no = None

    return photos_by_employee


def fetch_all_face_photos(debug: bool = False):
    url = f"http://{DEVICE_IP}/ISAPI/Intelligent/FDLib/FDSearch?format=json"
    photos = {}
    position = 0
    for _ in range(MAX_PAGES_FACES):
        body = {
            "searchResultPosition": position,
            "maxResults": BATCH_SIZE,
            "faceLibType": "staticFD",
            "FDID": FACE_LIB_ID,
        }
        try:
            resp = requests.post(
                url, json=body, auth=HTTPDigestAuth(DEVICE_USER, DEVICE_PASS),
                timeout=20, stream=False,
            )
        except requests.RequestException as e:
            log(f"  [FOTOS] error de conexion: {e}")
            return photos

        if resp.status_code != 200:
            log(f"  [FOTOS] no se pudieron obtener (status={resp.status_code}). "
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
    total_creados = total_actualizados = total_fotos = 0
    for i in range(0, len(rows), UPLOAD_CHUNK):
        chunk = rows[i:i + UPLOAD_CHUNK]
        try:
            resp = requests.post(CLOUD_EMPLOYEE_SYNC_URL, json=chunk, timeout=30)
            resp.raise_for_status()
            result = resp.json()
            total_creados += result.get("creados", 0)
            total_actualizados += result.get("actualizados", 0)
            total_fotos += result.get("fotosGuardadas", 0)
        except requests.RequestException as e:
            log(f"  [EMP] error subiendo tanda {i // UPLOAD_CHUNK + 1}: {e}")
    return total_creados, total_actualizados, total_fotos


def sync_employees(sin_fotos: bool = False, debug: bool = False):
    """Jala empleados de la lectora y los sube a la nube. Devuelve (creados, actualizados, fotos)."""
    log("=== Sincronizando EMPLEADOS ===")
    try:
        users = fetch_all_users(debug=debug)
    except requests.RequestException as e:
        log(f"[EMP] No se pudo conectar a la lectora: {e}")
        return 0, 0, 0

    log(f"  {len(users)} empleados encontrados en la lectora.")

    photos = {}
    if not sin_fotos:
        log("  Consultando fotos de rostro ...")
        photos = fetch_all_face_photos(debug=debug)
        log(f"  {len(photos)} fotos encontradas.")

    rows = []
    for u in users:
        rows.append({
            "employeeNo": u["employeeNo"],
            "name": u["name"],
            "photoBase64": photos.get(u["employeeNo"]),
        })

    log(f"  Subiendo {len(rows)} empleados al servidor ...")
    creados, actualizados, fotos_guardadas = push_employees_to_cloud(rows)
    log(f"  Empleados -> Nuevos: {creados} | Actualizados: {actualizados} | Con foto: {fotos_guardadas}")
    return creados, actualizados, fotos_guardadas


# -------------------- PARTE 2: ASISTENCIA --------------------

def fetch_events(start_time: str, end_time: str, position: int = 0):
    url = f"http://{DEVICE_IP}/ISAPI/AccessControl/AcsEvent?format=json"
    body = {
        "AcsEventCond": {
            "searchID": "1",
            "searchResultPosition": position,
            "maxResults": BATCH_SIZE,
            "major": 0,
            "minor": 0,
            "startTime": with_tz(start_time),
            "endTime": with_tz(end_time),
        }
    }
    resp = requests.post(
        url, json=body, auth=HTTPDigestAuth(DEVICE_USER, DEVICE_PASS), timeout=15,
    )
    if resp.status_code == 400:
        log(f"  [ATT] 400 Bad Request. Body: {json.dumps(body)}")
        log(f"  [ATT] Respuesta: {resp.text[:500]}")
    resp.raise_for_status()
    return resp.json()


def push_attendance_to_cloud(event: dict) -> bool:
    employee_no = event.get("employeeNoString") or event.get("cardNo") or ""
    if not employee_no:
        return False

    payload = {
        "dateTime": event.get("time", datetime.now().isoformat()),
        "deviceID": event.get("deviceName", "LOCAL-SYNC"),
        "AccessControllerEvent": {
            "employeeNoString": employee_no,
            "currentVerifyMode": event.get("currentVerifyMode", "unknown"),
        },
    }

    try:
        resp = requests.post(CLOUD_ATTENDANCE_URL, json=payload, timeout=15)
        resp.raise_for_status()
        return True
    except requests.RequestException as e:
        log(f"  [ATT] error subiendo evento de {employee_no}: {e}")
        return False


def sync_attendance(force_since: str | None = None):
    """Jala eventos de asistencia desde la ultima vez y los sube. Devuelve (vistos, subidos)."""
    log("=== Sincronizando ASISTENCIA ===")
    state = load_state()

    if force_since:
        start_time = force_since
    elif "last_synced_time" in state:
        start_time = state["last_synced_time"]
    else:
        start_time = (datetime.now() - timedelta(days=DEFAULT_LOOKBACK_DAYS)).strftime("%Y-%m-%dT%H:%M:%S")

    end_time = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
    log(f"  Rango: {start_time} -> {end_time}")

    total_pushed = 0
    total_seen = 0
    position = 0
    latest_time_seen = start_time

    for page in range(MAX_PAGES_ATTENDANCE):
        try:
            data = fetch_events(start_time, end_time, position)
        except requests.RequestException as e:
            log(f"  [ATT] no se pudo consultar la lectora: {e}")
            break

        acs = data.get("AcsEvent", {})
        info_list = acs.get("InfoList", [])

        if not info_list:
            break

        for ev in info_list:
            total_seen += 1
            ev_time = ev.get("time", "")
            if ev_time > latest_time_seen:
                latest_time_seen = ev_time
            if push_attendance_to_cloud(ev):
                total_pushed += 1

        num_matches = acs.get("numOfMatches", len(info_list))
        position += num_matches
        status = acs.get("responseStatusStrg", "OK")
        if status != "MORE" or num_matches < BATCH_SIZE:
            break

    log(f"  Asistencia -> Vistos: {total_seen} | Subidos: {total_pushed}")

    if total_seen > 0:
        state["last_synced_time"] = latest_time_seen
    else:
        state.setdefault("last_synced_time", start_time)
    save_state(state)

    return total_seen, total_pushed


# -------------------- PARTE 3: REPARACION --------------------

def call_cloud_endpoint(url: str, label: str):
    """Llama un endpoint POST del servidor (backfill, normalize). No falla si no responde."""
    try:
        resp = requests.post(url, timeout=30)
        resp.raise_for_status()
        data = resp.json()
        log(f"  {label}: {data}")
    except requests.RequestException as e:
        log(f"  {label}: no se pudo ejecutar ({e})")


def repair_attendance():
    """Llama a backfill-metadata y normalize en el servidor para reparar checadas viejas."""
    log("=== Reparando checadas (backfill + normalize) ===")
    call_cloud_endpoint(CLOUD_BACKFILL_URL, "Backfill metadata")
    call_cloud_endpoint(CLOUD_NORMALIZE_URL, "Normalize limites diarios")


# -------------------- CICLO PRINCIPAL --------------------

def run_cycle(sin_fotos: bool = False, debug: bool = False, force_since: str | None = None):
    """Un ciclo completo: empleados -> asistencia -> reparacion."""
    log("------ INICIO DE CICLO ------")

    # 1) Primero empleados: si hay nuevos, se dan de alta con nombre y foto
    #    ANTES de que lleguen sus checadas, para que el nombre se cruce solo.
    sync_employees(sin_fotos=sin_fotos, debug=debug)

    # 2) Luego asistencia: jala checadas nuevas desde el ultimo checkpoint
    sync_attendance(force_since=force_since)

    # 3) Reparacion: cruza nombres de checadas viejas sin nombre con la ficha
    #    ya actualizada, y respeta el limite de 2 checadas por dia.
    repair_attendance()

    log("------ CICLO COMPLETADO ------")


def main():
    parser = argparse.ArgumentParser(
        description="Sincronizacion total RHNAF: empleados + asistencia + reparacion"
    )
    parser.add_argument("--loop", action="store_true", help="Corre para siempre en bucle")
    parser.add_argument("--interval", type=int, default=DEFAULT_INTERVAL,
                        help=f"Segundos entre ciclos en modo --loop (default {DEFAULT_INTERVAL})")
    parser.add_argument("--since", type=str, default=None,
                        help="Forzar fecha de inicio ISO8601 para asistencia (util para backlog)")
    parser.add_argument("--sin-fotos", action="store_true",
                        help="Omitir fotos de rostro (mas rapido)")
    parser.add_argument("--debug", action="store_true",
                        help="Imprime respuestas crudas de la lectora para diagnostico")
    args = parser.parse_args()

    if args.loop:
        log(f"Modo continuo: cada {args.interval}s. Ctrl+C para detener.")
        first = True
        while True:
            run_cycle(
                sin_fotos=args.sin_fotos,
                debug=args.debug,
                force_since=args.since if first else None,
            )
            first = False
            time.sleep(args.interval)
    else:
        run_cycle(
            sin_fotos=args.sin_fotos,
            debug=args.debug,
            force_since=args.since,
        )


if __name__ == "__main__":
    main()
