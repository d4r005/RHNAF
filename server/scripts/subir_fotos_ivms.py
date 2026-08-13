#!/usr/bin/env python3
"""
subir_fotos_ivms.py — Busca fotos de empleados exportadas desde iVMS-4200
y las sube al servidor RHNAF.

COMO USAR:
1. En iVMS-4200: Access Control > Person Management > selecciona todos > Export
   - Si te pregunta formato, elige Excel/CSV + fotos
   - Guarda todo en una carpeta, ej: C:\\Users\\dtruj\\Desktop\\fotos_empleados

2. Si iVMS-4200 no tiene Export, busca en su carpeta de datos:
   - C:\\Users\\dtruj\\Documents\\iVMS-4200
   - C:\\ProgramData\\Hikvision\\iVMS-4200

3. Corre este script apuntando a la carpeta con las fotos:
   python subir_fotos_ivms.py "C:\\Users\\dtruj\\Desktop\\fotos_empleados"

4. El script busca archivos .jpg/.png/.bmp, intenta matchear el nombre del
   archivo con el employeeNo, y sube cada foto al servidor.

Si las fotos tienen nombres como "1341.jpg" o "JOSE_MEDRANO_1341.jpg",
el script extrae el employeeNo y lo asocia.
"""
import sys
import os
import re
import base64
import json
import requests

CLOUD_SYNC_URL = "https://d4r005-rhnaf-industrial.hf.space/api/v1/empleados/sync-device"

def extract_employee_no(filename):
    """Extrae el employeeNo del nombre del archivo.
    Busca el primer grupo de digitos en el nombre."""
    name = os.path.splitext(filename)[0]
    # Buscar numeros en el nombre (1-5 digitos)
    match = re.search(r'\b(\d{1,6})\b', name)
    if match:
        return match.group(1)
    return None

def find_image_files(folder):
    """Busca recursivamente todos los archivos de imagen en la carpeta."""
    extensions = {'.jpg', '.jpeg', '.png', '.bmp'}
    images = []
    for root, dirs, files in os.walk(folder):
        for f in files:
            ext = os.path.splitext(f)[1].lower()
            if ext in extensions:
                images.append(os.path.join(root, f))
    return images

def upload_photo(employee_no, photo_path):
    """Sube una foto para un employeeNo al servidor."""
    with open(photo_path, 'rb') as f:
        photo_b64 = base64.b64encode(f.read()).decode('ascii')
    
    payload = [{
        "employeeNo": employee_no,
        "name": "",
        "photoBase64": photo_b64,
    }]
    
    try:
        resp = requests.post(CLOUD_SYNC_URL, json=payload, timeout=30)
        resp.raise_for_status()
        result = resp.json()
        return result.get("fotosGuardadas", 0) > 0
    except Exception as e:
        print(f"  ERROR subiendo {employeeNo}: {e}")
        return False

def main():
    if len(sys.argv) < 2:
        # Modo scan: buscar en carpetas tipicas de iVMS-4200
        print("=== MODO SCAN: Buscando en carpetas de iVMS-4200 ===\n")
        search_dirs = [
            os.path.expanduser("~/Documents/iVMS-4200"),
            os.path.expanduser("~/Documents/iVMS-4200 2.0"),
            os.path.expanduser("~/Documents/iVMS-4200 3.0"),
            "C:\\ProgramData\\Hikvision\\iVMS-4200",
            "C:\\Program Files (x86)\\Hikvision\\iVMS-4200",
            "C:\\Program Files\\Hikvision\\iVMS-4200",
        ]
        all_images = []
        for d in search_dirs:
            if os.path.exists(d):
                print(f"  Escaneando: {d}")
                imgs = find_image_files(d)
                if imgs:
                    print(f"    -> {len(imgs)} imagenes encontradas!")
                    all_images.extend(imgs)
                else:
                    print(f"    -> sin imagenes")
            else:
                print(f"  No existe: {d}")
        
        if not all_images:
            print("\nNo se encontraron imagenes en carpetas de iVMS-4200.")
            print("\nPara usar manualmente:")
            print("  1. Exporta las fotos desde iVMS-4200 a una carpeta")
            print("  2. Corre: python subir_fotos_ivms.py \"ruta\\a\\la\\carpeta\"")
            return
        
        print(f"\nTotal: {len(all_images)} imagenes encontradas.")
        folder = os.path.dirname(all_images[0])
    else:
        folder = sys.argv[1]
        if not os.path.exists(folder):
            print(f"La carpeta no existe: {folder}")
            return
        print(f"=== Buscando fotos en: {folder} ===\n")
        all_images = find_image_files(folder)
    
    if not all_images:
        print("No se encontraron imagenes en la carpeta.")
        return
    
    print(f"\n{len(all_images)} imagenes encontradas:\n")
    
    matched = 0
    unmatched = []
    uploaded = 0
    
    for img_path in all_images:
        filename = os.path.basename(img_path)
        emp_no = extract_employee_no(filename)
        if emp_no:
            matched += 1
            print(f"  {filename} -> employeeNo={emp_no}")
            if upload_photo(emp_no, img_path):
                uploaded += 1
                print(f"    *** SUBIDA OK ***")
            else:
                print(f"    (no se guardo la foto)")
        else:
            unmatched.append(filename)
    
    print(f"\n=== RESUMEN ===")
    print(f"  Total imagenes: {len(all_images)}")
    print(f"  Con employeeNo: {matched}")
    print(f"  Subidas OK: {uploaded}")
    print(f"  Sin match: {len(unmatched)}")
    if unmatched:
        print(f"\n  Archivos sin employeeNo detectable:")
        for f in unmatched[:10]:
            print(f"    - {f}")
        if len(unmatched) > 10:
            print(f"    ... y {len(unmatched) - 10} mas")

if __name__ == "__main__":
    main()
