import requests
import json

url = "https://d4r005-rhnaf-industrial.hf.space/api/v1/asistencia/hikvision"
payload = {
    "dateTime": "2026-02-14T10:00:00",
    "deviceID": "TEST-PYTHON-FIX",
    "AccessControllerEvent": {
        "employeeNoString": "994455",
        "currentVerifyMode": "face"
    }
}

resp = requests.post(url, json=payload)
print(f"Status: {resp.status_code}")
print(f"Response: {resp.text}")
