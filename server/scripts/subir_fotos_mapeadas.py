#!/usr/bin/env python3
"""
subir_fotos_mapeadas.py
------------------------
Usa el mapeo YA VERIFICADO (ID interno secuencial de la lectora -> employeeNo
real) que se genero con verificar_fotos.html, para descargar cada foto por su
INDICE (no por employeeNo, porque la URL LOCALS/pic/enrlFace usa el indice
secuencial interno de la lectora, no el employeeNo) y subirla al servidor
asociada al employeeNo correcto.

Corre esta PC en la RED LOCAL de la planta (llega a 10.141.1.230).

Requisitos:
    pip install requests

Uso:
    python subir_fotos_mapeadas.py            (descarga y sube las 122 fotos)
    python subir_fotos_mapeadas.py --debug     (imprime detalle de cada descarga)
    python subir_fotos_mapeadas.py --dry-run   (solo descarga, no sube nada,
                                                 guarda las fotos en ./fotos_verificadas/
                                                 para que las revises antes de subir)
"""

import argparse
import base64
import os
import sys

import requests
from requests.auth import HTTPDigestAuth

# ----------------------- CONFIGURACION -----------------------
DEVICE_IP = "10.141.1.230"
DEVICE_USER = "admin"
DEVICE_PASS = "Branco2025"

CLOUD_SYNC_URL = "https://d4r005-rhnaf-industrial.hf.space/api/v1/empleados/sync-device"
UPLOAD_CHUNK = 25
# ---------------------------------------------------------------

# Mapeo verificado: indice interno de la lectora (ID) -> (nombre, employeeNo)
# Generado desde verificar_fotos.html el 2026-08-13.
ID_TO_EMPLOYEE = {
    1: ("JOSE MEDRANO", "1341"),
    2: ("Elidet Gregorio", "1334"),
    3: ("ABRAHAM GONZALEZ", "459"),
    4: ("mariana gonzalez", "460"),
    5: ("AMADITA FERNANDEZ", "472"),
    6: ("SENUI LOPEZ", "478"),
    7: ("GABRIELA BARRAGAN", "479"),
    8: ("Oliveth Antonio", "498"),
    9: ("ANGEL RODRIGUEZ", "10010"),
    10: ("BENITO MORENO", "1447"),
    11: ("PAOLA LOPEZ", "1350"),
    12: ("CARLOS CANIZALES", "114"),
    13: ("Gregorio Cruz", "163"),
    14: ("JOSE MEDRANO", "341"),
    15: ("JOSEFINA ALVARADO", "343"),
    16: ("PERLA MORALES", "355"),
    17: ("Miguel Marin", "475"),
    18: ("Khac Nhu Pham", "272"),
    19: ("Yahir Ramirez Aguirre", "988"),
    20: ("Mario Hernandez Antonio", "997"),
    21: ("EVELIN ZAPATA", "1007"),
    22: ("CRISTINA HERNANDEZ", "1009"),
    23: ("ARNI SALAZAR", "10020"),
    24: ("GEMA CITLALY HERNANDEZ BORJON", "490"),
    25: ("LUCERO PALOMO", "1029"),
    26: ("MAURICIO ARMETA", "1046"),
    27: ("ASHLY GARCIA", "1042"),
    28: ("AMANDA HERRERA", "395"),
    29: ("MIGUEL DUARTE", "1047"),
    30: ("DIANA MONRROY", "1048"),
    31: ("YESENIA MARTINEZ", "1044"),
    32: ("ELIZABETH DEL ANGEL", "1049"),
    33: ("DAVID DIAZ", "1057"),
    34: ("Andrea Cortez", "10021"),
    35: ("SANDRA HERNANDEZ", "1060"),
    36: ("Jonathan Valenzuela", "10022"),
    37: ("ROBERTO SUAREZ ROMERO", "1064"),
    38: ("Yang Logan", "164"),
    39: ("Huynh Le Phuoc Thien", "165"),
    40: ("Nguyen Van Ngoc", "167"),
    41: ("Wang Zhixiang", "274"),
    42: ("Cen He", "364"),
    43: ("Wu Yurong", "502"),
    44: ("Nguyen Thanh Hong", "168"),
    45: ("Tran Thi Huyen", "508"),
    46: ("Fuming Yao", "509"),
    47: ("DONG BINBIN", "506"),
    48: ("DO THI PHUONG", "166"),
    49: ("Liliana Rosales", "10011"),
    50: ("Pan lihua", "504"),
    51: ("Wang Jie", "366"),
    52: ("Zhao Yun", "162"),
    53: ("Cao YanYun", "365"),
    54: ("Jinsong Zhao", "503"),
    55: ("Zhu Ping", "510"),
    56: ("DARIO ROBLES", "10008"),
    57: ("CYNTHIA SAUCEDA", "987"),
    58: ("MIGUEL ANGEL HERNANDEZ", "205"),
    59: ("CARLOS JOSE CANIZALEZ", "171"),
    60: ("SALVADOR VAZQUEZ", "378"),
    61: ("ADELAIDA HERNANDEZ", "220"),
    62: ("YARELY CORPUS", "417"),
    63: ("EDUARDO DIAZ", "221"),
    64: ("MARGARITA DE LEON HERRERA", "1077"),
    65: ("ERIK ACOSTA JUAREZ", "1085"),
    66: ("FLOR CORDOVA", "184"),
    67: ("LUZ MARIA ORTEGA RUIZ", "1090"),
    68: ("SANDRA PATRICIA MUÑOS SANDOVAL", "1092"),
    69: ("HEIDY NAOMY CAMPOS ROMAN", "1094"),
    70: ("Alejandro Hernandez", "20003"),
    71: ("SERVANDO ALONSO", "1105"),
    72: ("LUIS DE LEON MATA", "1104"),
    73: ("ADOLFO GONZALEZ", "1099"),
    74: ("MARIA DEL ROSARIO HUERAMO", "1098"),
    75: ("ANGEL ALFONSO SALAS", "1100"),
    76: ("NALLELY ABIGAIL OLIVARES", "1109"),
    77: ("LEIDY LOPEZ CRUZ", "1108"),
    78: ("YESENIA HUERTA", "1120"),
    79: ("MARIA SERRATO", "1121"),
    80: ("DIEGO ARMANDO YAÑEZ", "1123"),
    81: ("LUIS YAÑEZ", "1124"),
    82: ("SOFIA VAZQUEZ", "1126"),
    83: ("Jovani Alejandro Martinez", "1127"),
    84: ("Fransisca Santiago", "1110"),
    85: ("MARIBEL LOPEZ MARES", "1130"),
    86: ("FRANCISCO SUAREZ", "1129"),
    87: ("DAMARIS MORALES SILLAS", "1128"),
    88: ("JULIA BRAVO LEDEZMA", "1131"),
    89: ("ZABDIEL RODRIGUEZ VILLASEÑOR", "1132"),
    90: ("MIGUEL ANGEL CHAVEZ", "10023"),
    91: ("VENUSTIANO CRUZ", "1138"),
    92: ("FLOR MARTINEZ", "1136"),
    93: ("MARISOL GARCIA PALACIOS", "1133"),
    94: ("ISRAEL HERNANDEZ MORALES", "1134"),
    95: ("ADAN HERNANDEZ", "1135"),
    96: ("BLANCA BONILLA", "1139"),
    97: ("PEDRO REYNA", "1140"),
    98: ("SANDRA ELIA ROMERO", "1141"),
    99: ("CESAR PALACIOS ANAYA", "1142"),
    100: ("KEILA GONZALEZ", "1144"),
    101: ("MAYELA VILLALOBOS", "1146"),
    102: ("GILBERTO HERNANDEZ", "1145"),
    103: ("KARLA GARZA", "1143"),
    104: ("MARTHA HERNANDEZ", "1147"),
    105: ("JUAN VELA", "1149"),
    106: ("FERNANDO DUARTE", "1150"),
    107: ("FERNANDO GAEL", "1050"),
    108: ("ROSA MEDINA", "1155"),
    109: ("ELIZABETH HERNANDEZ", "1151"),
    110: ("MIRIAM ALVARADO", "1152"),
    111: ("JUANA RIOS", "1153"),
    112: ("JUAN SUAREZ", "1154"),
    113: ("OLIVA JIMENEZ MARTINEZ", "1156"),
    114: ("DARINTE MENDOZA", "1158"),
    115: ("JONATHAN MENDOZA GERONIMO", "1157"),
    116: ("MIGUEL ANGEL SAUCEDA SOSA", "1160"),
    117: ("DOMINGO HERNANDEZ VIDALES", "1161"),
    118: ("AMAIRANY ALVARADO COSTILLA", "1159"),
    119: ("MARIA DEL CARMEN HUERAMO", "1166"),
    120: ("KARI RAMIREZ JIMENEZ", "1165"),
    121: ("ALFREDO CERVIN", "1163"),
    122: ("EDGAR IVAN DIAZ MARTINEZ", "1164"),
}


def download_photo(idx: int, debug: bool = False):
    """Descarga la foto en el indice interno `idx` via LOCALS/pic/enrlFace."""
    padded = str(idx).zfill(10)
    url = f"http://{DEVICE_IP}/LOCALS/pic/enrlFace/0/{padded}.jpg@WEB0000"
    try:
        resp = requests.get(url, auth=HTTPDigestAuth(DEVICE_USER, DEVICE_PASS), timeout=10)
    except requests.RequestException as e:
        if debug:
            print(f"  [ID:{idx}] error de conexion: {e}")
        return None
    if resp.status_code == 200 and len(resp.content) > 100 and "image" in resp.headers.get("Content-Type", ""):
        return resp.content
    if debug:
        print(f"  [ID:{idx}] status={resp.status_code} ct={resp.headers.get('Content-Type','?')} len={len(resp.content)}")
    return None


def push_employees_to_cloud(rows: list):
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
    parser.add_argument("--debug", action="store_true", help="Imprime detalle de cada descarga")
    parser.add_argument("--dry-run", action="store_true",
                         help="Solo descarga y guarda en ./fotos_verificadas/, no sube nada")
    args = parser.parse_args()

    print(f"Descargando {len(ID_TO_EMPLOYEE)} fotos por indice interno de la lectora {DEVICE_IP} ...\n")

    if args.dry_run:
        os.makedirs("fotos_verificadas", exist_ok=True)

    rows = []
    ok = 0
    fail = 0
    for idx, (name, emp_no) in sorted(ID_TO_EMPLOYEE.items()):
        content = download_photo(idx, debug=args.debug)
        if content is None:
            fail += 1
            print(f"  ID:{idx} {name} (No:{emp_no}) -> SIN FOTO")
            continue
        ok += 1
        print(f"  ID:{idx} {name} (No:{emp_no}) -> OK ({len(content)} bytes)")

        if args.dry_run:
            safe_name = "".join(c if c.isalnum() else "_" for c in name)
            fname = f"fotos_verificadas/{emp_no}_{safe_name}.jpg"
            with open(fname, "wb") as f:
                f.write(content)
        else:
            rows.append({
                "employeeNo": emp_no,
                "name": name,
                "photoBase64": base64.b64encode(content).decode("ascii"),
            })

    print(f"\n=== DESCARGA ===")
    print(f"  OK: {ok}  |  Sin foto: {fail}")

    if args.dry_run:
        print(f"\nFotos guardadas en ./fotos_verificadas/ — revisalas antes de subir.")
        print(f"Cuando confirmes que estan bien, corre sin --dry-run para subirlas.")
        return

    if not rows:
        print("\nNo hay fotos para subir.")
        return

    print(f"\nSubiendo {len(rows)} fotos al servidor en la nube ...")
    creados, actualizados, fotos = push_employees_to_cloud(rows)
    print(f"\n=== RESULTADO ===")
    print(f"  Nuevos: {creados} | Actualizados: {actualizados} | Con foto: {fotos}")


if __name__ == "__main__":
    main()
