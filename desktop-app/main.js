const { app, BrowserWindow, Menu, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const http = require('http');

const ERP_URL = 'https://d4r005-rhnaf-industrial.hf.space';

const DEFAULT_CONFIG = {
  deviceIp: '10.141.1.230',
  deviceUser: 'admin',
  devicePass: 'Branco2025',
  cloudUrl: 'https://d4r005-rhnaf-industrial.hf.space/api/v1/asistencia/hikvision'
};

function getConfigPath() { return path.join(app.getPath('userData'), 'config.json'); }
function getStatePath() { return path.join(app.getPath('userData'), 'sync_state.json'); }

function loadConfig() {
  try {
    const p = getConfigPath();
    if (fs.existsSync(p)) return { ...DEFAULT_CONFIG, ...JSON.parse(fs.readFileSync(p, 'utf-8')) };
  } catch (e) { /* ignore, fall back to defaults */ }
  return { ...DEFAULT_CONFIG };
}
function saveConfig(cfg) {
  fs.mkdirSync(path.dirname(getConfigPath()), { recursive: true });
  fs.writeFileSync(getConfigPath(), JSON.stringify(cfg, null, 2));
}
function loadState() {
  try {
    const p = getStatePath();
    if (fs.existsSync(p)) return JSON.parse(fs.readFileSync(p, 'utf-8'));
  } catch (e) { /* ignore */ }
  return {};
}
function saveState(state) {
  fs.mkdirSync(path.dirname(getStatePath()), { recursive: true });
  fs.writeFileSync(getStatePath(), JSON.stringify(state, null, 2));
}

// ------------------------------------------------------------------
// HTTP Digest Auth (Hikvision ISAPI usa Digest, no Basic) -
// reimplementacion en Node de lo que hacia requests.auth.HTTPDigestAuth
// en el script de Python attendance_sync.py
// ------------------------------------------------------------------
function md5(str) { return crypto.createHash('md5').update(str).digest('hex'); }

function parseAuthHeader(header) {
  const result = {};
  const re = /(\w+)=(?:"([^"]*)"|([^,]*))/g;
  let m;
  while ((m = re.exec(header)) !== null) result[m[1]] = m[2] !== undefined ? m[2] : m[3];
  return result;
}

function httpRequestOnce({ host, port, path: p, method, headers, body }) {
  return new Promise((resolve, reject) => {
    const req = http.request({ host, port, path: p, method, headers }, (res) => {
      let data = '';
      res.on('data', (c) => { data += c; });
      res.on('end', () => resolve({ statusCode: res.statusCode, headers: res.headers, body: data }));
    });
    req.on('error', reject);
    req.setTimeout(15000, () => req.destroy(new Error('Tiempo de espera agotado conectando a la lectora')));
    if (body) req.write(body);
    req.end();
  });
}

async function digestRequest({ host, port = 80, path: p, method = 'POST', username, password, jsonBody }) {
  const bodyStr = jsonBody ? JSON.stringify(jsonBody) : '';
  const baseHeaders = { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(bodyStr) };

  const first = await httpRequestOnce({ host, port, path: p, method, headers: baseHeaders, body: bodyStr });
  if (first.statusCode !== 401 || !first.headers['www-authenticate']) return first;

  const params = parseAuthHeader(first.headers['www-authenticate']);
  const nc = '00000001';
  const cnonce = crypto.randomBytes(8).toString('hex');
  const ha1 = md5(`${username}:${params.realm}:${password}`);
  const ha2 = md5(`${method}:${p}`);

  let authorizationHeader;
  if (params.qop) {
    const response = md5(`${ha1}:${params.nonce}:${nc}:${cnonce}:${params.qop}:${ha2}`);
    authorizationHeader =
      `Digest username="${username}", realm="${params.realm}", nonce="${params.nonce}", uri="${p}", ` +
      `qop=${params.qop}, nc=${nc}, cnonce="${cnonce}", response="${response}"` +
      (params.opaque ? `, opaque="${params.opaque}"` : '');
  } else {
    const response = md5(`${ha1}:${params.nonce}:${ha2}`);
    authorizationHeader =
      `Digest username="${username}", realm="${params.realm}", nonce="${params.nonce}", uri="${p}", response="${response}"` +
      (params.opaque ? `, opaque="${params.opaque}"` : '');
  }

  return httpRequestOnce({
    host, port, path: p, method,
    headers: { ...baseHeaders, Authorization: authorizationHeader },
    body: bodyStr
  });
}

// ------------------------------------------------------------------
// Logica de sincronizacion (puerto 1:1 de attendance_sync.py)
// ------------------------------------------------------------------
const TZ_OFFSET = '-06:00'; // Mexico City no tiene horario de verano desde 2022

function withTz(ts) {
  if (!ts) return ts;
  const tail = ts.slice(10);
  if (tail.includes('+') || tail.includes('-') || tail.endsWith('Z')) return ts;
  return ts + TZ_OFFSET;
}

function isoLocalNoMs(d) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function sleep(ms) { return new Promise((r) => setTimeout(r, ms)); }

async function fetchEvents(cfg, startTime, endTime, position, major = 5, minor = 0) {
  const p = '/ISAPI/AccessControl/AcsEvent?format=json';
  const res = await digestRequest({
    host: cfg.deviceIp,
    path: p,
    method: 'POST',
    username: cfg.deviceUser,
    password: cfg.devicePass,
    jsonBody: {
      AcsEventCond: {
        searchID: '1',
        searchResultPosition: position,
        maxResults: 30,
        major,
        minor,
        startTime: withTz(startTime),
        endTime: withTz(endTime)
      }
    }
  });
  if (res.statusCode !== 200) {
    throw new Error(`Lectora respondio ${res.statusCode}: ${(res.body || '').slice(0, 200)}`);
  }
  return JSON.parse(res.body);
}

async function pushToCloud(cfg, event) {
  const employeeNo = event.employeeNoString || event.cardNo || '';
  const idClean = String(employeeNo).trim().toLowerCase();
  if (!employeeNo || ['none', 'null', '0', ''].includes(idClean)) return { pushed: false };

  const payload = {
    dateTime: event.time || new Date().toISOString(),
    deviceID: event.deviceName || 'HIKVISIONWEB',
    AccessControllerEvent: {
      employeeNoString: employeeNo,
      currentVerifyMode: event.currentVerifyMode || 'unknown'
    }
  };

  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const resp = await fetch(cfg.cloudUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if ([500, 502, 503, 504].includes(resp.status)) {
        await sleep(2000 * (attempt + 1));
        continue;
      }
      return { pushed: resp.ok };
    } catch (e) {
      if (attempt === 2) return { pushed: false, error: e.message };
      await sleep(2000);
    }
  }
  return { pushed: false };
}

async function runSync(cfg, onProgress) {
  const state = loadState();
  const startTime = state.last_synced_time || isoLocalNoMs(new Date(Date.now() - 7 * 24 * 3600 * 1000));
  const endTime = isoLocalNoMs(new Date());

  let totalPushed = 0;
  let totalSeen = 0;
  let position = 0;
  let latestTimeSeen = startTime;

  onProgress?.(`Sincronizando de ${startTime} a ${endTime}...`);

  for (let page = 0; page < 30; page++) {
    let data;
    try {
      data = await fetchEvents(cfg, startTime, endTime, position, 5, 0);
    } catch (e) {
      onProgress?.(`Error consultando la lectora: ${e.message}`);
      return { totalSeen, totalPushed, error: e.message };
    }

    const acs = data.AcsEvent || {};
    const infoList = acs.InfoList || [];
    if (!infoList.length) break;

    for (const ev of infoList) {
      totalSeen++;
      if (ev.time && ev.time > latestTimeSeen) latestTimeSeen = ev.time;
      const r = await pushToCloud(cfg, ev);
      if (r.pushed) totalPushed++;
      await sleep(150);
    }
    onProgress?.(`Pagina ${page + 1}: vistos ${totalSeen}, subidos ${totalPushed}...`);

    const numMatches = acs.numOfMatches || infoList.length;
    position += numMatches;
    if (acs.responseStatusStrg !== 'MORE' || numMatches < 30) break;
  }

  if (totalSeen > 0) state.last_synced_time = latestTimeSeen;
  saveState(state);
  return { totalSeen, totalPushed };
}

// ------------------------------------------------------------------
// Ventanas de Electron
// ------------------------------------------------------------------
let mainWindow;
let settingsWindow;

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });
  mainWindow.loadURL(ERP_URL);
  mainWindow.once('ready-to-show', () => mainWindow.show());
  Menu.setApplicationMenu(buildMenu());
}

function buildMenu() {
  return Menu.buildFromTemplate([
    {
      label: 'RH NAF ERP',
      submenu: [
        { label: 'Sincronizar Historico Local', click: () => triggerSync() },
        { label: 'Configurar Lectora...', click: () => openSettingsWindow() },
        { type: 'separator' },
        { label: 'Recargar', click: () => mainWindow.reload() },
        { type: 'separator' },
        { role: 'quit', label: 'Salir' }
      ]
    },
    {
      label: 'Ver',
      submenu: [
        { role: 'reload' },
        { role: 'toggleDevTools' },
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { role: 'togglefullscreen' }
      ]
    }
  ]);
}

function triggerSync() {
  if (!mainWindow) return;
  mainWindow.webContents.send('sync-progress', 'Iniciando sincronizacion...');
  runSync(loadConfig(), (msg) => mainWindow.webContents.send('sync-progress', msg)).then((result) => {
    const msg = result.error
      ? `Error: ${result.error}`
      : `Listo. Vistos: ${result.totalSeen} | Subidos: ${result.totalPushed}`;
    mainWindow.webContents.send('sync-progress', msg);
  });
}

function openSettingsWindow() {
  if (settingsWindow) { settingsWindow.focus(); return; }
  settingsWindow = new BrowserWindow({
    width: 480,
    height: 460,
    parent: mainWindow,
    modal: true,
    resizable: false,
    webPreferences: {
      preload: path.join(__dirname, 'settings-preload.js'),
      contextIsolation: true
    }
  });
  settingsWindow.setMenuBarVisibility(false);
  settingsWindow.loadFile(path.join(__dirname, 'settings.html'));
  settingsWindow.on('closed', () => { settingsWindow = null; });
}

ipcMain.handle('sync-local', async () => {
  return runSync(loadConfig(), (msg) => mainWindow?.webContents.send('sync-progress', msg));
});
ipcMain.handle('open-settings', () => openSettingsWindow());
ipcMain.handle('get-settings', () => loadConfig());
ipcMain.handle('save-settings', (event, cfg) => { saveConfig(cfg); return true; });

app.whenReady().then(createMainWindow);
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
