# Walkthrough: Sincronización Real-Time bajo demanda (Opción A)

He implementado el "Puente de Comunicación" que permite que el botón **"Get Events from Device"** de la página web active realmente la sincronización en tu computadora de la planta.

## Cambios Realizados

### 1. Sistema de Tareas Remotas
*   **Servidor**: Se creó una nueva tabla `system_tasks` para gestionar órdenes de trabajo.
*   **API**: Se añadieron endpoints para solicitar tareas, consultarlas (polling) y actualizarlas.

### 2. Script de Python Inteligente
*   El script `attendance_sync.py` ahora funciona en un modo dual:
    *   **Automático**: Sincroniza cada 5 minutos.
    *   **Bajo Demanda**: Revisa el servidor cada 5 segundos buscando órdenes que tú envíes desde la web.
*   Al detectar una orden, el script se comunica con la lectora Hikvision local e informa del progreso a la nube.

### 3. Interfaz Web Interactiva
*   El botón **"Get Events from Device"** ahora envía una orden real.
*   Muestra el estado en tiempo real: `PENDING`, `BUSY` (Ocupado procesando) y `DONE`.
*   Avisa con una alerta cuando la sincronización ha terminado exitosamente.

## Cómo usarlo

1.  **En la Planta**: Mantén abierto el script de Python en modo bucle:
    ```powershell
    python attendance_sync.py --loop
    ```
2.  **En la Web**: Presiona el botón **"Get Events from Device"**.
3.  **Observa la magia**: Verás que la consola de Python en tu PC detecta la orden inmediatamente, lee la lectora y los datos aparecen en tu pantalla web en segundos.

> [!TIP]
> Este método es el más robusto ya que no requiere abrir puertos en el router de la planta ni usar túneles VPN complejos.
