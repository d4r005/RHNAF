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

STATE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sync_state.json")
BATCH_SIZE = 30          # eventos por página (ISAPI típico soporta 30)
MAX_PAGES_PER_RUN = 50   # tope de páginas por corrida (=1500 eventos) para no tardar horas
INTERVAL_SECONDS = 300   # cada 5 min en modo --loop
DEFAULT_LOOKBACK_DAYS = 7  # si no hay estado previo, cuantos dias hacia atras jalar
# ---------------------------------------------------------------


def load_state():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_state(state):
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=2)


def fetch_events(start_time: str, end_time: str, position: int = 0):
    """Un solo request de búsqueda de eventos a la ISAPI de la lectora."""
    url = f"http://{DEVICE_IP}/ISAPI/AccessControl/AcsEvent?format=json"
    body = {
        "AcsEventCond": {
            "searchID": "1",
            "searchResultPosition": position,
            "maxResults": BATCH_SIZE,
            "major": 0,
            "minor": 0,
            "startTime": start_time,
            "endTime": end_time,
        }
    }
    resp = requests.post(
        url,
        json=body,
        auth=HTTPDigestAuth(DEVICE_USER, DEVICE_PASS),
        timeout=15,
    )
    resp.raise_for_status()
    return resp.json()


def push_to_cloud(event: dict) -> bool:
    """Manda un evento al servidor en la nube en el formato que ya espera."""
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
        resp = requests.post(CLOUD_URL, json=payload, timeout=15)
        resp.raise_for_status()
        return True
    except requests.RequestException as e:
        print(f"  [ERROR] no se pudo subir evento de empleado {employee_no}: {e}")
        return False


def run_once(force_since: str | None = None):
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

    print(f"Sincronizando eventos de {start_time} a {end_time} ...")

    total_pushed = 0
    total_seen = 0
    position = 0
    latest_time_seen = start_time

    for page in range(MAX_PAGES_PER_RUN):
        try:
            data = fetch_events(start_time, end_time, position)
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
            if push_to_cloud(ev):
                total_pushed += 1

        num_matches = acs.get("numOfMatches", len(info_list))
        position += num_matches

        status = acs.get("responseStatusStrg", "OK")
        if status != "MORE" or num_matches < BATCH_SIZE:
            break

    print(f"Listo. Vistos: {total_seen} | Subidos a la nube: {total_pushed}")

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
    args = parser.parse_args()

    if args.loop:
        print(f"Modo continuo: cada {INTERVAL_SECONDS}s. Ctrl+C para detener.")
        while True:
            run_once(force_since=args.since)
            args.since = None  # solo se fuerza la primera vez
            time.sleep(INTERVAL_SECONDS)
    else:
        run_once(force_since=args.since)


if __name__ == "__main__":
    main()
