#!/usr/bin/env python3
"""
attendance_sync.py
-------------------
Corre esta PC en la RED LOCAL de la planta (la que sí puede llegar a la
lectora Hikvision por IP, 10.141.1.230). El servidor en la nube (Hugging
Face) NO puede jalar los datos directo porque vive fuera de esa red.

Este script hace lo que el equipo no ha podido lograr solo (push por HTTPS
con TLS moderno): JALA (pull) los eventos de asistencia desde la lectora
via ISAPI, y los EMPUJA al servidor en la nube usando el HTTPS normal de
Windows/Python (que sí soporta certificados modernos sin problema).

Requisitos:
    pip install requests

Uso:
    python attendance_sync.py
    (corre una vez y termina - agenda con Task Scheduler cada 5 min, o
     usa el modo --loop para que se quede corriendo solo)

    python attendance_sync.py --loop        (corre para siempre, cada INTERVAL_SECONDS)
    python attendance_sync.py --since 2026-06-01T00:00:00   (fuerza fecha de inicio,
                                                               util para vaciar el
                                                               backlog acumulado)
"""

import argparse
import json
import os
import sys
import time
from datetime import datetime, timedelta

import requests
from requests.auth import HTTPDigestAuth

# ----------------------- CONFIGURACION -----------------------
DEVICE_IP = "10.141.1.230"
DEVICE_USER = "admin"
DEVICE_PASS = "Branco2025"          # <-- cambia si tu password es distinto

CLOUD_URL = "https://d4r005-rhnaf-industrial.hf.space/api/v1/asistencia/hikvision"

# Offset de zona horaria que exige la ISAPI de Hikvision en startTime/endTime
# (ISO8601 completo). Mexico City ya no tiene horario de verano desde 2022,
# se queda fijo en -06:00 todo el año.
TZ_OFFSET = "-06:00"

STATE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sync_state.json")
BATCH_SIZE = 30          # eventos por página (ISAPI típico soporta 30)
MAX_PAGES_PER_RUN = 30   # reducido para no saturar
INTERVAL_SECONDS = 300   # cada 5 min en modo --loop
DEFAULT_LOOKBACK_DAYS = 7  # si no hay estado previo, cuantos dias hacia atras jalar
# ---------------------------------------------------------------


def with_tz(ts: str) -> str:
    """Le agrega el offset de zona horaria a un timestamp ISO8601 si no lo trae ya.
    Hikvision RECHAZA (400 Bad Request) startTime/endTime que no incluyan el offset."""
    if not ts:
        return ts
    tail = ts[10:]  # todo despues de "YYYY-MM-DD", ahi es donde vendria el offset
    if "+" in tail or "-" in tail or tail.endswith("Z"):
        return ts  # ya trae offset
    return ts + TZ_OFFSET


def load_state():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_state(state):
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=2)


def fetch_events(start_time: str, end_time: str, position: int = 0, major: int = 5, minor: int = 0):
    """Un solo request de búsqueda de eventos a la ISAPI de la lectora.
    major=5 = eventos de Control de Acceso (checadas reales). major=0 = TODOS
    los tipos (incluye ruido de sistema: alarmas, aperturas de puerta, tamper, etc.
    que no tienen employeeNoString y por eso nunca se suben)."""
    url = f"http://{DEVICE_IP}/ISAPI/AccessControl/AcsEvent?format=json"
    body = {
        "AcsEventCond": {
            "searchID": "1",
            "searchResultPosition": position,
            "maxResults": BATCH_SIZE,
            "major": major,
            "minor": minor,
            "startTime": with_tz(start_time),
            "endTime": with_tz(end_time),
        }
    }
    resp = requests.post(
        url,
        json=body,
        auth=HTTPDigestAuth(DEVICE_USER, DEVICE_PASS),
        timeout=15,
    )
    if resp.status_code == 400:
        print(f"  [DEBUG] 400 Bad Request. Body enviado: {json.dumps(body)}")
        print(f"  [DEBUG] Respuesta de la lectora: {resp.text[:500]}")
    resp.raise_for_status()
    return resp.json()


def push_to_cloud(event: dict, skip_reasons: dict, skip_samples: dict) -> bool:
    """Manda un evento al servidor en la nube en el formato que ya espera."""
    employee_no = event.get("employeeNoString") or event.get("cardNo") or ""

    # Filtrado local de IDs invalidos (None, 0, vacios)
    id_clean = str(employee_no).strip().lower()
    if not employee_no or id_clean == "none" or id_clean == "null" or id_clean == "0":
        key = f"major={event.get('major')} minor={event.get('minor')}"
        skip_reasons[key] = skip_reasons.get(key, 0) + 1
        if key not in skip_samples:
            skip_samples[key] = event
        return False

    payload = {
        "dateTime": event.get("time", datetime.now().isoformat()),
        "deviceID": event.get("deviceName", "HIKVISIONWEB"),
        "AccessControllerEvent": {
            "employeeNoString": employee_no,
            "currentVerifyMode": event.get("currentVerifyMode", "unknown"),
        },
    }

    # Mecanismo de reintento suave para evitar errores 503 en Hugging Face
    max_retries = 3
    for attempt in range(max_retries):
        try:
            # Pausa obligatoria para no ametrallar el servidor gratuito
            time.sleep(0.15)

            resp = requests.post(CLOUD_URL, json=payload, timeout=20)
            if resp.status_code in [500, 502, 503, 504]:
                print(f"  [AVISO] Servidor ocupado (Intento {attempt+1}/{max_retries}). Esperando...")
                time.sleep(2 * (attempt + 1))
                continue

            resp.raise_for_status()
            return True
        except requests.RequestException as e:
            if attempt == max_retries - 1:
                print(f"  [ERROR] no se pudo subir empleado {employee_no} tras {max_retries} intentos: {e}")
                skip_reasons["push_failed"] = skip_reasons.get("push_failed", 0) + 1
                return False
            time.sleep(2)

    return False


def run_once(force_since: str | None = None, major: int = 5, minor: int = 0):
    state = load_state()

    if force_since:
        start_time = force_since
    elif "last_synced_time" in state:
        start_time = state["last_synced_time"]
    else:
        start_time = (datetime.now() - timedelta(days=DEFAULT_LOOKBACK_DAYS)).strftime(
            "%Y-%m-%dT%H:%M:%S"
        )

    end_time = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")

    print(f"Sincronizando eventos de {start_time} a {end_time} (major={major}, minor={minor}) ...")

    total_pushed = 0
    total_seen = 0
    position = 0
    latest_time_seen = start_time
    skip_reasons: dict = {}
    skip_samples: dict = {}

    for page in range(MAX_PAGES_PER_RUN):
        try:
            data = fetch_events(start_time, end_time, position, major=major, minor=minor)
        except requests.RequestException as e:
            print(f"[ERROR] no se pudo consultar la lectora: {e}")
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
            if push_to_cloud(ev, skip_reasons, skip_samples):
                total_pushed += 1

        num_matches = acs.get("numOfMatches", len(info_list))
        position += num_matches

        status = acs.get("responseStatusStrg", "OK")
        if status != "MORE" or num_matches < BATCH_SIZE:
            break

    print(f"Listo. Vistos: {total_seen} | Subidos a la nube: {total_pushed}")
    if skip_reasons:
        print("Eventos descartados por tipo (no traian employeeNo/cardNo):")
        for k, v in sorted(skip_reasons.items(), key=lambda x: -x[1]):
            print(f"  {k}: {v}")
        print("\nEjemplo de evento crudo por cada tipo descartado (para identificar que es):")
        for k, sample in skip_samples.items():
            print(f"  --- {k} ---")
            print(f"  {json.dumps(sample, ensure_ascii=False)[:600]}")

    # Avanza el checkpoint solo si vimos algo, para no repetir en la siguiente corrida
    if total_seen > 0:
        state["last_synced_time"] = latest_time_seen
    else:
        state.setdefault("last_synced_time", start_time)

    save_state(state)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--loop", action="store_true", help="Correr en bucle infinito")
    parser.add_argument("--since", type=str, default=None, help="Forzar fecha de inicio ISO8601")
    parser.add_argument(
        "--major", type=int, default=5,
        help="Tipo de evento ISAPI a buscar. 5 = Control de Acceso (checadas reales, default). "
             "0 = TODOS los tipos (incluye ruido de sistema sin employeeNo, util solo para diagnostico).",
    )
    parser.add_argument("--minor", type=int, default=0, help="Subtipo de evento ISAPI (default 0 = todos)")
    args = parser.parse_args()

    if args.loop:
        print(f"Modo continuo: cada {INTERVAL_SECONDS}s. Ctrl+C para detener.")
        while True:
            run_once(force_since=args.since, major=args.major, minor=args.minor)
            args.since = None  # solo se fuerza la primera vez
            time.sleep(INTERVAL_SECONDS)
    else:
        run_once(force_since=args.since, major=args.major, minor=args.minor)


if __name__ == "__main__":
    main()
