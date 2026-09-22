#!/usr/bin/env python3
"""Transfer public tables from old Supabase to new Postgres, excluding ehs_documents.

Requires SOURCE_SUPABASE_SERVICE_ROLE_KEY and TARGET_DATABASE_URL. Never reads,
creates, or copies public.ehs_documents. Run --source-only for a read-only check;
run --apply from a trusted runner after the destination schema has been created.
"""
import argparse
import json
import os
import re
import sys
import urllib.parse
import urllib.request

SOURCE_URL = "https://vrqxemvsizitimvvqttd.supabase.co/rest/v1/"
EXCLUDED = {"ehs_documents"}
DESTINATION_REF = "bonwrvlgkkkuvskezqzh"
PAGE_SIZE = 250
NAME = re.compile(r"^[a-z][a-z0-9_]*$")


def request_json(path, key):
    req = urllib.request.Request(
        urllib.parse.urljoin(SOURCE_URL, path),
        headers={
            "apikey": key,
            "Authorization": "Bearer " + key,
            "Accept": "application/openapi+json" if path == "" else "application/json",
        },
    )
    with urllib.request.urlopen(req, timeout=45) as response:
        return json.load(response)


def source_schema(key):
    openapi = request_json("", key)
    definitions = openapi["definitions"]
    tables = sorted(
        path[1:] for path in openapi["paths"]
        if path.startswith("/") and NAME.fullmatch(path[1:]) and path[1:] not in EXCLUDED
    )
    if not tables or "ehs_documents" in tables:
        raise RuntimeError("Invalid source table list")
    for table in tables:
        if table not in definitions:
            raise RuntimeError("Missing schema for " + table)
        properties = definitions[table]["properties"]
        pk = [name for name, meta in properties.items() if "Primary Key.<pk/>" in meta.get("description", "")]
        if len(pk) != 1:
            raise RuntimeError(f"Expected exactly one primary key in {table}: {pk}")
    return tables, definitions


def read_page(table, pk, offset, key):
    query = urllib.parse.urlencode({
        "select": "*", "order": f"{pk}.asc", "limit": PAGE_SIZE, "offset": offset,
    })
    return request_json(f"{table}?{query}", key)


def check_destination(conn, tables, definitions):
    with conn.cursor() as cur:
        cur.execute("""
            SELECT table_name, column_name FROM information_schema.columns
            WHERE table_schema = 'public'
        """)
        existing = {}
        for table, column in cur.fetchall():
            existing.setdefault(table, set()).add(column)
        if "ehs_documents" in existing:
            raise RuntimeError("Destination unexpectedly contains public.ehs_documents")
        missing_tables = sorted(set(tables) - set(existing))
        missing_columns = {
            table: sorted(set(definitions[table]["properties"]) - existing.get(table, set()))
            for table in tables if table in existing
        }
        missing_columns = {table: cols for table, cols in missing_columns.items() if cols}
        if missing_tables or missing_columns:
            raise RuntimeError(f"Missing destination tables: {missing_tables}; missing columns: {missing_columns}")
        cur.execute("""
            SELECT t.relname, a.attname FROM pg_index i
            JOIN pg_class t ON t.oid = i.indrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(i.indkey)
            WHERE n.nspname = 'public' AND i.indisprimary
        """)
        keys = {}
        for table, column in cur.fetchall():
            keys.setdefault(table, set()).add(column)
        for table in tables:
            source_pk = {name for name, meta in definitions[table]["properties"].items()
                         if "Primary Key.<pk/>" in meta.get("description", "")}
            if source_pk != keys.get(table):
                raise RuntimeError(f"Primary key mismatch in {table}")


def migrate(conn, tables, definitions, key):
    from psycopg2 import sql
    from psycopg2.extras import execute_values, Json

    moved = {}
    with conn:
        with conn.cursor() as cur:
            for table in tables:
                pk = next(name for name, meta in definitions[table]["properties"].items()
                          if "Primary Key.<pk/>" in meta.get("description", ""))
                columns = list(definitions[table]["properties"])
                update_cols = [name for name in columns if name != pk]
                table_ident = sql.Identifier("public", table)
                statement = sql.SQL("INSERT INTO {} ({}) VALUES %s ON CONFLICT ({}) DO UPDATE SET {}").format(
                    table_ident,
                    sql.SQL(", ").join(map(sql.Identifier, columns)),
                    sql.Identifier(pk),
                    sql.SQL(", ").join(sql.SQL("{}=EXCLUDED.{}").format(sql.Identifier(c), sql.Identifier(c))
                                      for c in update_cols),
                )
                offset = 0
                while True:
                    rows = read_page(table, pk, offset, key)
                    if not isinstance(rows, list):
                        raise RuntimeError("Unexpected API response for " + table)
                    if rows:
                        values = [tuple(Json(row[col]) if isinstance(row[col], (dict, list)) else row[col]
                                        for col in columns) for row in rows]
                        execute_values(cur, statement.as_string(conn), values, page_size=PAGE_SIZE)
                    offset += len(rows)
                    if len(rows) < PAGE_SIZE:
                        break
                moved[table] = offset
                # Preserve IDs from the old database; advance the serial for future inserts.
                if definitions[table]["properties"][pk].get("type") == "integer":
                    cur.execute("SELECT pg_get_serial_sequence(%s, %s)", (f'public."{table}"', pk))
                    sequence = cur.fetchone()[0]
                    if sequence:
                        cur.execute(sql.SQL("SELECT MAX({}) FROM {}").format(sql.Identifier(pk), table_ident))
                        maximum = cur.fetchone()[0]
                        if maximum is not None:
                            cur.execute("SELECT setval(%s::regclass, %s, true)", (sequence, maximum))
                print(f"{table}: {offset} records", flush=True)
    return moved


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--source-only", action="store_true")
    group.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    key = os.environ["SOURCE_SUPABASE_SERVICE_ROLE_KEY"]
    tables, definitions = source_schema(key)
    print(f"Source: {len(tables)} public tables; public.ehs_documents EXCLUDED", flush=True)
    if args.source_only:
        return
    import psycopg2
    from urllib.parse import urlsplit
    uri = os.environ["TARGET_DATABASE_URL"]
    parsed = urlsplit(uri)
    if parsed.scheme not in ("postgres", "postgresql") or not parsed.password:
        raise RuntimeError("TARGET_DATABASE_URL must be a complete PostgreSQL URI")
    if DESTINATION_REF not in (parsed.username or "") and DESTINATION_REF not in (parsed.hostname or ""):
        raise RuntimeError("Database URL does not match destination Supabase project")
    conn = psycopg2.connect(uri, connect_timeout=20, sslmode="require")
    try:
        check_destination(conn, tables, definitions)
        result = migrate(conn, tables, definitions, key)
        print(f"Migration committed: {sum(result.values())} records, {len(result)} tables; ehs_documents omitted")
    finally:
        conn.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        # Driver exceptions may embed connection URLs. Never log exception text.
        print(f"Migration failed ({type(exc).__name__}). No partial data transaction committed.", file=sys.stderr)
        sys.exit(1)
