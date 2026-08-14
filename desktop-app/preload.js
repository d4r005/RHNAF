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
    border-radius: 8px; margin-bottom: 8px; max-width: 360px; display: none;
    box-shadow: 0 4px 14px rgba(0,0,0,0.3); line-height: 1.4;
  `;

  const btnRow = document.createElement('div');
  btnRow.style.cssText = 'display: flex; gap: 8px; align-items: center;';

  const btnFull = document.createElement('button');
  btnFull.id = 'rhnaf-sync-btn';
  btnFull.textContent = '\uD83D\uDD04 Sync Todo';
  btnFull.style.cssText = `
    background: #2563eb; color: white; border: none; padding: 13px 22px;
    border-radius: 999px; font-weight: bold; font-size: 13px; cursor: pointer;
    box-shadow: 0 4px 16px rgba(37,99,235,0.45);
  `;
  btnFull.addEventListener('mouseenter', () => { btnFull.style.background = '#1d4ed8'; });
  btnFull.addEventListener('mouseleave', () => { btnFull.style.background = '#2563eb'; });

  const btnEmp = document.createElement('button');
  btnEmp.id = 'rhnaf-sync-emp-btn';
  btnEmp.textContent = '\uD83D\uDD04 Sync Checadas';
  btnEmp.style.cssText = `
    background: #059669; color: white; border: none; padding: 13px 16px;
    border-radius: 999px; font-weight: bold; font-size: 13px; cursor: pointer;
    box-shadow: 0 4px 16px rgba(5,150,105,0.4);
  `;
  btnEmp.addEventListener('mouseenter', () => { btnEmp.style.background = '#047857'; });
  btnEmp.addEventListener('mouseleave', () => { btnEmp.style.background = '#059669'; });

  async function runSync(channel, busyText, doneTextFn) {
    const btn = channel === 'sync-full' ? btnFull : btnEmp;
    btn.disabled = true;
    btn.style.opacity = '0.7';
    btn.textContent = busyText;
    status.style.display = 'block';
    status.textContent = 'Conectando con la lectora en la red local (LAN)...';
    try {
      const result = await ipcRenderer.invoke(channel);
      if (result && result.error) {
        status.textContent = `\u274C ${result.error}`;
      } else {
        status.textContent = doneTextFn(result);
      }
    } catch (e) {
      status.textContent = `\u274C Error: ${e.message}`;
    } finally {
      btn.disabled = false;
      btn.style.opacity = '1';
      btnFull.textContent = '\uD83D\uDD04 Sync Todo';
      btnEmp.textContent = '\uD83D\uDD04 Sync Checadas';
      setTimeout(() => { status.style.display = 'none'; }, 10000);
    }
  }

  btnFull.addEventListener('click', () => {
    runSync('sync-full', '\u23F3 Sincronizando...', (r) =>
      `\u2705 Sincronizacion completa. Checadas: ${r.totalSeen} vistas, ${r.totalPushed} subidas.`);
  });

  btnEmp.addEventListener('click', () => {
    runSync('sync-local', '\u23F3 Checadas...', (r) =>
      `\u2705 Checadas: ${r.totalSeen} vistas, ${r.totalPushed} subidas.`);
  });

  btnRow.appendChild(btnEmp);
  btnRow.appendChild(btnFull);
  container.appendChild(status);
  container.appendChild(btnRow);
  document.body.appendChild(container);
}

ipcRenderer.on('sync-progress', (event, message) => {
  const status = document.getElementById('rhnaf-sync-status');
  if (status) { status.style.display = 'block'; status.textContent = message; }
});

window.addEventListener('DOMContentLoaded', injectFloatingWidget);
setInterval(() => { if (document.body && !document.getElementById('rhnaf-sync-widget')) injectFloatingWidget(); }, 2000);
