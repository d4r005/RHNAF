# Plan para Generar Ejecutable Windows (.exe)

Este plan detalla los pasos técnicos para convertir el código fuente de la carpeta `desktop-app` en un archivo ejecutable distribuible para Windows.

## Análisis Técnico

La aplicación de escritorio utiliza **Electron**. Para generar el `.exe`, necesitamos:
1.  Asegurar que todas las dependencias de Node.js estén instaladas.
2.  Configurar los blancos de compilación (targets) para incluir un instalador (`nsis`) o un ejecutable `portable`.
3.  Ejecutar el proceso de empaquetado mediante `electron-builder`.

## Proposed Changes

### [Desktop App Component]

#### [MODIFY] [package.json](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/desktop-app/package.json)
*   Actualizar la sección `build.win.target` para incluir `nsis` (generará un instalador `RH NAF ERP Setup.exe`).
*   Opcionalmente añadir `portable` para un ejecutable único que no requiere instalación.

#### [RUN] Instalación de dependencias
*   Ejecutar `npm install` dentro de la carpeta `desktop-app`.

#### [RUN] Compilación final
*   Ejecutar `npm run dist` para generar los binarios.

## Verification Plan

### Manual Verification
1.  **Carpeta Release**: Verificar que aparezca una nueva carpeta llamada `release` dentro de `desktop-app`.
2.  **Prueba del Ejecutable**: Ejecutar el archivo `.exe` generado y confirmar que abre el ERP correctamente y que los menús de sincronización funcionan.
