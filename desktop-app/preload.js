const { ipcRenderer } = require('electron');

function injectFloatingWidget() {
  if (document.getElementById('rhnaf-sync-widget')) return;
  if (!document.body) return;

  const container = document.createElement('div');
  container.id = 'rhnaf-sync-widget';
  container.style.cssText = `
    position: fixed; bottom: 20px; right: 20px; z-index: 2147483000;
    display: flex; flex-direction: column; align-items: flex-end;
    font-family: -apple-system, "Segoe UI", Roboto, sans-serif;
  `;

  const status = document.createElement('div');
  status.id = 'rhnaf-sync-status';
  status.style.cssText = `
    background: #0f172a; color: #e2e8f0; font-size: 12px; padding: 8px 14px;
    border-radius: 8px; margin-bottom: 8px; max-width: 340px; display: none;
    box-shadow: 0 4px 14px rgba(0,0,0,0.3); line-height: 1.4;
  `;

  const btn = document.createElement('button');
  btn.id = 'rhnaf-sync-btn';
  btn.textContent = '\uD83D\uDD04 Sincronizar Historico Local';
  btn.style.cssText = `
    background: #2563eb; color: white; border: none; padding: 13px 22px;
    border-radius: 999px; font-weight: bold; font-size: 13px; cursor: pointer;
    box-shadow: 0 4px 16px rgba(37,99,235,0.45);
  `;
  btn.addEventListener('mouseenter', () => { btn.style.background = '#1d4ed8'; });
  btn.addEventListener('mouseleave', () => { btn.style.background = '#2563eb'; });

  btn.addEventListener('click', async () => {
    btn.disabled = true;
    btn.style.opacity = '0.7';
    btn.textContent = '\u23F3 Sincronizando...';
    status.style.display = 'block';
    status.textContent = 'Conectando con la lectora en la red local (LAN)...';
    try {
      const result = await ipcRenderer.invoke('sync-local');
      if (result && result.error) {
        status.textContent = `\u274C ${result.error}`;
      } else {
        status.textContent = `\u2705 Listo. Vistos: ${result.totalSeen} | Subidos a la nube: ${result.totalPushed}`;
      }
    } catch (e) {
      status.textContent = `\u274C Error: ${e.message}`;
    } finally {
      btn.disabled = false;
      btn.style.opacity = '1';
      btn.textContent = '\uD83D\uDD04 Sincronizar Historico Local';
      setTimeout(() => { status.style.display = 'none'; }, 9000);
    }
  });

  container.appendChild(status);
  container.appendChild(btn);
  document.body.appendChild(container);
}

ipcRenderer.on('sync-progress', (event, message) => {
  const status = document.getElementById('rhnaf-sync-status');
  if (status) { status.style.display = 'block'; status.textContent = message; }
});

window.addEventListener('DOMContentLoaded', injectFloatingWidget);
// Reintento por si la SPA reemplaza el DOM despues de cargar
setInterval(() => { if (document.body && !document.getElementById('rhnaf-sync-widget')) injectFloatingWidget(); }, 2000);
