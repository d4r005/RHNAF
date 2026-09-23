#!/usr/bin/env python3
"""Registra en ehs_documents los archivos que ya existen en Google Drive.

Recorre recursivamente GOOGLE_DRIVE_FOLDER_ID, y por cada archivo inserta una
fila con el puntero "gdrive:<fileId>" en content_base64 (mismo convenio que
usa el backend Ktor al subir evidencias). Así los documentos ya presentes en
Drive aparecen en Evidencia Documental sin volver a subirlos ni llenar la base.

Convenciones de carpetas (iguales a las de la app):
  Evidencia Documental/<Categoria>/<Año o General>/<archivo>
  - categoria = primera subcarpeta (ej. Normativa, Otro); sin subcarpeta -> Otro
  - anio = subcarpeta numérica 1900..2100; "General" -> -1; sin año -> 0

Idempotente: si ya existe un registro con el mismo puntero drive, se omite.
Solo INSERT; nunca borra ni modifica registros existentes.

Variables requeridas:
  DATABASE_URL (postgres://...), GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET,
  GOOGLE_DRIVE_REFRESH_TOKEN, GOOGLE_DRIVE_FOLDER_ID
"""
import json
import os
import sys
import urllib.parse
import urllib.request
from datetime import datetime, timezone


def http_json(url, data=None, headers=None, method=None):
    req = urllib.request.Request(url, method=method,
                                 data=data.encode() if isinstance(data, str) else data,
                                 headers=headers or {})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def get_access_token():
    body = urllib.parse.urlencode({
        "client_id": os.environ["GOOGLE_CLIENT_ID"],
        "client_secret": os.environ["GOOGLE_CLIENT_SECRET"],
        "refresh_token": os.environ["GOOGLE_DRIVE_REFRESH_TOKEN"],
        "grant_type": "refresh_token",
    }).encode()
    req = urllib.request.Request("https://oauth2.googleapis.com/token", data=body,
                                 headers={"Content-Type": "application/x-www-form-urlencoded"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)["access_token"]


def list_children(token, folder_id):
    q = urllib.parse.quote(f"'{folder_id}' in parents and trashed = false")
    url = (f"https://www.googleapis.com/drive/v3/files?q={q}"
           f"&fields=files(id,name,mimeType,size,modifiedTime)&pageSize=200")
    req = urllib.request.Request(url, headers={"Authorization": "Bearer " + token})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r).get("files", [])


def walk(token, folder_id, path):
    for f in list_children(token, folder_id):
        name, mt = f["name"], f.get("modifiedTime", "")
        if f["mimeType"] == "application/vnd.google-apps.folder":
            yield from walk(token, f["id"], path + [name])
        else:
            yield f, path


def parse_year(folder_name):
    s = folder_name.strip()
    if s == "General":
        return -1
    if s.isdigit() and 1900 <= int(s) <= 2100:
        return int(s)
    return 0


def main():
    import psycopg2
    conn = psycopg2.connect(os.environ["DATABASE_URL"])
    token = get_access_token()
    inserted = skipped = failed = 0
    today = datetime.now(timezone.utc).strftime("%d/%m/%Y")
    for f, path in walk(token, os.environ["GOOGLE_DRIVE_FOLDER_ID"], []):
        drive_id = f["id"]
        try:
            categoria = path[0].strip()[:50] if path else "Otro"
            anio = parse_year(path[1]) if len(path) > 1 else 0
            modified = f.get("modifiedTime", "")
            fecha = ""
            if modified:
                try:
                    fecha = datetime.fromisoformat(modified.replace("Z", "+00:00")).strftime("%d/%m/%Y")
                except ValueError:
                    fecha = ""
            name = f["name"]
            titulo = name.rsplit(".", 1)[0][:300] if "." in name else name[:300]
            mime = f.get("mimeType", "application/octet-stream")[:100]
            size = int(f.get("size") or 0)
            cur = conn.cursor()
            cur.execute("SELECT 1 FROM ehs_documents WHERE content_base64 = %s LIMIT 1",
                        ("gdrive:" + drive_id,))
            if cur.fetchone():
                cur.close(); skipped += 1; continue
            cur.execute(
                """INSERT INTO ehs_documents
                   (categoria, titulo, fecha, anio, file_name, mime_type, file_size,
                    notas, uploaded_by, uploaded_date, module_type, module_record_id,
                    content_base64)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
                (categoria, titulo, fecha, anio, name[:300], mime, size,
                 "Importado desde Google Drive (" + "/".join(path) + ")",
                 "importacion-drive", today, "", 0, "gdrive:" + drive_id))
            cur.close()
            conn.commit()
            inserted += 1
            print(f"OK  {'/'.join(path)}/{name}")
        except Exception as e:  # noqa: BLE001 - seguir con el resto
            conn.rollback()
            failed += 1
            print(f"ERR {'/'.join(path)}/{f['name']}: {e}", file=sys.stderr)
    print(f"RESUMEN insertados={inserted} omitidos={skipped} fallidos={failed}")
    conn.close()
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
