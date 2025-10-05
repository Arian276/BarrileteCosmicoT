const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const chokidar = require('chokidar');
const { v4: uuidv4 } = require('uuid');
const multer = require('multer');
const http = require('http');
const { Server } = require('socket.io');

// FCM opcional
let admin = null;
try { admin = require('firebase-admin'); } catch (e) { /* opcional */ }

const upload = multer();
const app = express();

app.use(cors());
app.use(express.json({ limit: '2mb' }));
app.use(express.urlencoded({ extended: true }));
app.use(express.text({ type: ['text/plain', 'text/*'] }));

// ===== Utils =====
const nowISO = () => new Date().toISOString();
const slugify = (s = '') =>
  s.toString().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');

const CHANNELS_DIR = path.join(process.cwd(), 'channels');

// ===== Usuarios fijos (con soporte opcional de bcrypt y users.json) =====
let bcrypt = null;
try { bcrypt = require('bcryptjs'); } catch (_) { /* opcional */ }

const usersFilePath = path.join(process.cwd(), 'users.json');

// Fallback: solo admin si no hay users.json
let DEFAULT_USERS = [
  { username: 'admin', password: '1234', name: 'Admin' }
];

function loadUsers() {
  try {
    if (fs.existsSync(usersFilePath)) {
      const json = JSON.parse(fs.readFileSync(usersFilePath, 'utf8'));
      // Formato esperado: { "users": [ { username, passwordHash? o password?, name? }, ... ] }
      if (json && Array.isArray(json.users)) return json.users;
    }
  } catch (e) {
    console.warn('⚠️ No se pudo leer users.json, usando usuarios por defecto:', e.message);
  }
  return DEFAULT_USERS;
}

async function checkPassword(userRec, plain) {
  // 1) si hay passwordHash y bcrypt disponible, usar bcrypt.compare
  if (userRec.passwordHash && bcrypt) {
    try { return await bcrypt.compare(String(plain), String(userRec.passwordHash)); }
    catch { /* fallback abajo */ }
  }
  // 2) si hay password en claro, comparar directo
  if (typeof userRec.password === 'string') {
    return String(userRec.password) === String(plain);
  }
  return false;
}

// ===== Parser M3U/M3U8 (tolerante) =====
function parseM3uContentFlexible(raw, defaultCategory = 'general', sourceName = 'general') {
  if (!raw || typeof raw !== 'string') return [];
  const text = raw.replace(/^\uFEFF/, '').replace(/\r\n/g, '\n');
  const lines = text.split('\n');
  const out = [];
  const urlRe = /^(https?:\/\/\S+)/i;
  const takeId = (title, host) => slugify(title) || slugify(host) || slugify(sourceName);

  for (let i = 0; i < lines.length; i++) {
    const L = (lines[i] || '').trim();
    if (!L) continue;

    // #EXTINF
    if (L.startsWith('#EXTINF')) {
      const comma = L.indexOf(',');
      const title = (comma >= 0 ? L.slice(comma + 1) : '').trim() || sourceName;
      const group = defaultCategory || sourceName;

      let j = i + 1, url = null;
      while (j < lines.length) {
        const cand = (lines[j] || '').trim();
        if (!cand || cand.startsWith('#')) { j++; continue; }
        const m = cand.match(urlRe);
        if (m) { url = m[1]; break; }
        const parts = cand.split(/[;,]/);
        if (parts.length >= 2 && urlRe.test(parts[1].trim())) { url = parts[1].trim().match(urlRe)[1]; break; }
        j++;
      }
      if (url) {
        let host = ''; try { host = new URL(url).hostname.replace(/^www\./, ''); } catch {}
        out.push({
          id: takeId(title, host),
          title, description: title, thumbnailUrl: '',
          streamUrl: url, category: slugify(group), categoryName: group,
          isLive: true, viewerCount: 0, country: 'AR', language: 'es', quality: 'HD',
          createdAt: nowISO(), updatedAt: nowISO(), _sourceFile: null,
        });
        i = j;
      }
      continue;
    }

    // "Nombre,URL" o "Nombre;URL"
    if (!L.startsWith('#') && (L.includes(',') || L.includes(';'))) {
      const sep = L.includes(',') ? ',' : ';';
      const parts = L.split(sep);
      if (parts.length >= 2 && urlRe.test(parts[1].trim())) {
        const title = parts[0].trim() || sourceName;
        const url = parts[1].trim().match(urlRe)[1];
        let host = ''; try { host = new URL(url).hostname.replace(/^www\./, ''); } catch {}
        out.push({
          id: takeId(title, host),
          title, description: title, thumbnailUrl: '',
          streamUrl: url, category: slugify(defaultCategory || sourceName),
          categoryName: defaultCategory || sourceName, isLive: true,
          viewerCount: 0, country: 'AR', language: 'es', quality: 'HD',
          createdAt: nowISO(), updatedAt: nowISO(), _sourceFile: null,
        });
        continue;
      }
    }

    // URL sola
    if (urlRe.test(L)) {
      const url = L.match(urlRe)[1];
      let host = ''; try { host = new URL(url).hostname.replace(/^www\./, ''); } catch {}
      const title = host || sourceName;
      out.push({
        id: takeId(title, host),
        title, description: title, thumbnailUrl: '',
        streamUrl: url, category: slugify(defaultCategory || sourceName),
        categoryName: defaultCategory || sourceName, isLive: true,
        viewerCount: 0, country: 'AR', language: 'es', quality: 'HD',
        createdAt: nowISO(), updatedAt: nowISO(), _sourceFile: null,
      });
      continue;
    }
  }
  return out;
}

// ===== Estado en memoria =====
let channels = [];
const chatDB   = new Map();              // streamId -> [ChatMessage]
const likesDB  = new Map();              // streamId -> { likes, likedBy:Set }
const viewersM = new Map();              // streamId -> Map(viewerId -> lastPingMs)
const VIEWER_TTL_MS = 60_000;

function ensureMaps(streamId) {
  if (!chatDB.has(streamId))  chatDB.set(streamId, []);
  if (!likesDB.has(streamId)) likesDB.set(streamId, { likes: 0, likedBy: new Set() });
  if (!viewersM.has(streamId)) viewersM.set(streamId, new Map());
}
function liveViewerCount(streamId) {
  const m = viewersM.get(streamId);
  if (!m) return 0;
  const now = Date.now();
  let n = 0;
  for (const ts of m.values()) if (now - ts <= VIEWER_TTL_MS) n++;
  return n;
}
setInterval(() => { // prune viewers
  const now = Date.now();
  for (const m of viewersM.values()) {
    for (const [vid, ts] of m.entries()) if (now - ts > VIEWER_TTL_MS) m.delete(vid);
  }
}, 15_000);

// ===== Carga de ./channels =====
function processFile(filePath) {
  try {
    const txt  = fs.readFileSync(filePath, 'utf8');
    const base = path.parse(filePath).name;
    const parsed = parseM3uContentFlexible(txt, base, base).map(ch => ({ ...ch, _sourceFile: filePath }));
    const cat = slugify(base);

    channels = channels.filter(ch => ch._sourceFile !== filePath && ch.category !== cat);
    channels = [...channels, ...parsed];
    parsed.forEach(s => ensureMaps(s.id));

    console.log(`✅ ${path.basename(filePath)}: ${parsed.length} canales. Total: ${channels.length}`);
  } catch (e) {
    console.error(`❌ Error en ${path.basename(filePath)}:`, e.message);
  }
}
function scanDir(dir) {
  if (!fs.existsSync(dir)) { console.warn(`⚠️ No existe ${dir}`); channels = []; return; }
  const files = fs.readdirSync(dir).filter(f => /\.m3u8?$/i.test(f));
  channels = [];
  files.forEach(f => processFile(path.join(dir, f)));
  console.log(`📺 Total tras escaneo: ${channels.length}`);
}
function watchDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  chokidar.watch(dir, {
    persistent: true, ignoreInitial: false, depth: 0,
    awaitWriteFinish: { stabilityThreshold: 300, pollInterval: 100 }
  })
  .on('add',    f => /\.m3u8?$/i.test(f) && processFile(f))
  .on('change', f => /\.m3u8?$/i.test(f) && processFile(f))
  .on('unlink', f => {
    const before = channels.length;
    channels = channels.filter(ch => ch._sourceFile !== f);
    console.log(`🗑️  ${path.basename(f)} eliminado: ${before - channels.length} canales. Total: ${channels.length}`);
  });
  console.log(`👀 Monitoreando: ${dir}`);
}
scanDir(CHANNELS_DIR);
watchDir(CHANNELS_DIR);

// ===== Helpers =====
const getStream = (id) => channels.find(s => s.id === id);
const byCategory = (c) => channels.filter(s => s.category === slugify(c));
const searchStreams = (q) => {
  const n = (q || '').toLowerCase();
  return channels.filter(s =>
    s.title.toLowerCase().includes(n) ||
    s.description.toLowerCase().includes(n) ||
    s.category.toLowerCase().includes(n)
  );
};
const buildCategories = (list) => {
  const counts = new Map(), names = new Map();
  for (const s of list) {
    counts.set(s.category, (counts.get(s.category) || 0) + 1);
    if (!names.has(s.category)) names.set(s.category, s.categoryName || s.category);
  }
  return [...counts.entries()].map(([name, count]) => ({ name, count, displayName: names.get(name) || name }));
};

// =========================================================
// ======================  API  (/api)  ====================
// =========================================================
const api = express.Router();

// Health / Reload
api.get('/health', (_req, res) => res.json({ ok: true, timestamp: nowISO(), streams: channels.length }));
api.post('/reload', (_req, res) => { scanDir(CHANNELS_DIR); res.json({ ok: true, streams: channels.length, reloadedAt: nowISO() }); });

// ===== AUTHENTICATION =====
api.post('/login', async (req, res) => {
  const { username, password } = req.body || {};

  if (!username || !password) {
    return res.status(400).json({ success: false, error: 'Faltan credenciales' });
  }

  const users = loadUsers();
  const user = users.find(u => String(u.username) === String(username));

  if (!user) {
    console.log(`Login fallido (no existe): ${username}`);
    return res.status(401).json({ success: false, error: 'Credenciales inválidas' });
  }

  const ok = await checkPassword(user, password);
  if (!ok) {
    console.log(`Login fallido (pass incorrecta): ${username}`);
    return res.status(401).json({ success: false, error: 'Credenciales inválidas' });
  }

  console.log(`Login exitoso para: ${username}`);
  return res.json({
    success: true,
    token: `fake-token-for-${username}-${Date.now()}`,
    user: { username: user.username, name: user.name || user.username }
  });
});

// >>> AÑADIDO: AUTH DE USUARIO PARA CERRAR SESIÓN SI FUE ELIMINADO <<<
// Extrae username desde el token "fake-token-for-<username>-<timestamp>"
function parseUsernameFromToken(token) {
  try {
    const t = String(token || '').trim();
    const m = t.match(/^fake-token-for-(.+?)-\d+$/);
    return m ? m[1] : null;
  } catch (_) { return null; }
}

function requireUserAuth(req, res, next) {
  const bearer = (req.headers.authorization || '').replace(/^Bearer\s+/i, '').trim();
  if (!bearer) return res.status(401).json({ error: 'NO_AUTH' });
  const username = parseUsernameFromToken(bearer);
  if (!username) return res.status(401).json({ error: 'INVALID_TOKEN' });

  const users = loadUsers();
  const user = users.find(u => String(u.username) === String(username));
  if (!user) return res.status(401).json({ error: 'SESSION_INVALID_OR_USER_DELETED' });

  req.user = { username: user.username, name: user.name || user.username };
  return next();
}
// <<< FIN AÑADIDO

// STREAMS (protegido para detonar 401 si el user fue borrado)
api.use('/streams', requireUserAuth);

api.get('/streams', (req, res) => {
  const { category } = req.query;
  res.json((category && category !== 'all') ? byCategory(category) : channels);
});
api.get('/streams/featured', (_req, res) => res.json(channels.slice(0, 10)));
api.get('/streams/:id', (req, res) => {
  const s = getStream(req.params.id);
  if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  const likesEntry = likesDB.get(s.id) || { likes: 0 };
  res.json({ ...s, viewerCount: liveViewerCount(s.id), likes: likesEntry.likes });
});
api.get('/streams/search', (req, res) => res.json(searchStreams(req.query.query || '')));
api.get('/streams/category/:category', (req, res) => res.json(byCategory(req.params.category)));
// VIEWERS (Join/Leave/Ping/Viewers) — resp como ApiService.kt
api.post('/streams/:id/join', (req, res) => {
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  const viewerId = (req.body && req.body.viewerId) ? String(req.body.viewerId) : uuidv4();
  ensureMaps(streamId);
  viewersM.get(streamId).set(viewerId, Date.now());
  io.to(streamId).emit('viewer-count-update', { streamId, viewerCount: liveViewerCount(streamId) });
  res.json({ viewerId, viewerCount: liveViewerCount(streamId), streamId });
});
api.post('/streams/:id/ping', (req, res) => {
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  const viewerId = (req.body && req.body.viewerId) ? String(req.body.viewerId) : null;
  if (viewerId) {
    ensureMaps(streamId);
    viewersM.get(streamId).set(viewerId, Date.now());
    io.to(streamId).emit('viewer-count-update', { streamId, viewerCount: liveViewerCount(streamId) });
  }
  res.json({ viewerCount: liveViewerCount(streamId), streamId });
});
api.post('/streams/:id/leave', (req, res) => {
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  const viewerId = (req.body && req.body.viewerId) ? String(req.body.viewerId) : null;
  if (viewerId && viewersM.get(streamId)) {
    viewersM.get(streamId).delete(viewerId);
    io.to(streamId).emit('viewer-count-update', { streamId, viewerCount: liveViewerCount(streamId) });
  }
  res.json({ viewerId: viewerId || '', viewerCount: liveViewerCount(streamId), streamId });
});
api.get('/streams/:id/viewers', (req, res) => {
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  res.json({ streamId, viewerCount: liveViewerCount(streamId), timestamp: nowISO() });
});

// LIKES
function ensureLike(streamId) { ensureMaps(streamId); return likesDB.get(streamId); }
api.post('/streams/:id/like', (req, res) => {
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  const userId = req.body?.userId;
  if (!userId) return res.status(400).json({ error: 'userId es requerido' });

  const entry = ensureLike(streamId);
  const has = entry.likedBy.has(userId);
  if (has) { entry.likedBy.delete(userId); entry.likes = Math.max(0, entry.likes - 1); }
  else     { entry.likedBy.add(userId);    entry.likes += 1; }

  io.to(streamId).emit('like-update', { streamId, likes: entry.likes });
  res.json({ streamId, likes: entry.likes, liked: entry.likedBy.has(userId), timestamp: nowISO() });
});
api.get('/streams/:id/likes', (req, res) => {
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  const userId = req.query?.userId;
  const entry = likesDB.get(streamId) || { likes: 0, likedBy: new Set() };
  res.json({ streamId, likes: entry.likes, liked: !!(userId && entry.likedBy.has(String(userId))), timestamp: nowISO() });
});
api.get('/streams/likes/summary', (_req, res) => {
  const summary = {};
  channels.forEach(s => {
    const e = likesDB.get(s.id) || { likes: 0 };
    summary[s.id] = { likes: e.likes, streamId: s.id };
  });
  res.json(summary);
});

// CHAT
api.post('/streams/:id/chat', (req, res) => {
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  const { username, message } = req.body || {};
  if (!username || !message) return res.status(400).json({ error: 'Username y message son requeridos' });

  ensureMaps(streamId);
  const msg = { id: uuidv4(), username: String(username), message: String(message), timestamp: Date.now(), colorHex: '#00BFFF' };
  chatDB.get(streamId).push(msg);
  io.to(streamId).emit('chat-message', { streamId, message: msg });
  res.json({ streamId, message: msg, timestamp: nowISO() });
});
api.get('/streams/:id/chat', (req, res) => {
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  const limit  = Math.max(1, Math.min(200, parseInt(req.query.limit  ?? '50', 10)));
  const offset = Math.max(0,                parseInt(req.query.offset ?? '0' , 10));
  ensureMaps(streamId);
  const arr = chatDB.get(streamId);
  res.json({ streamId, messages: arr.slice(offset, offset + limit), totalMessages: arr.length, offset, limit, timestamp: nowISO() });
});
api.delete('/streams/:id/chat', (req, res) => {
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.status(404).json({ error: 'Stream no encontrado' });
  chatDB.set(streamId, []);
  res.json({ streamId, message: 'Chat limpiado', timestamp: nowISO() });
});

// CATEGORIES / UI
api.get('/categories', (_req, res) => res.json(buildCategories(channels)));

let uiConfig = { theme: 'dark', featuredCategory: 'deportes', showViewerCount: true, updatedAt: nowISO() };
api.get('/ui-config', (_req, res) => res.json({ success: true, config: uiConfig, timestamp: nowISO() }));
api.put('/ui-config', (req, res) => { uiConfig = { ...uiConfig, ...(req.body || {}), updatedAt: nowISO() }; res.json({ success: true, config: uiConfig, timestamp: nowISO() }); });
api.get('/ui-config/:section', (req, res) => { const section = req.params.section; res.json({ success: true, section, data: { [section]: uiConfig[section] }, timestamp: nowISO() }); });
api.put('/ui-config/:section', (req, res) => { const section = req.params.section; uiConfig[section] = req.body?.value ?? req.body; uiConfig.updatedAt = nowISO(); res.json({ success: true, section, data: { [section]: uiConfig[section] }, timestamp: nowISO() }); });

// ===== FCM público =====
const devices = new Map(); // userId -> Set(tokens)
api.post('/fcm/register', async (req, res) => {   // <-- async + autosuscribe topic 'all'
  const { token, userId = 'anon', platform } = req.body || {};
  if (!token) return res.status(400).json({ error: 'Falta token' });
  if (!devices.has(userId)) devices.set(userId, new Set());
  devices.get(userId).add(token);

  // >>> AÑADIDO: auto-suscribir token al topic "all" si FCM está inicializado
  try {
    if (admin?.apps?.length) {
      await admin.messaging().subscribeToTopic([token], 'all');
    }
  } catch (_) {}
  // <<< FIN AÑADIDO

  return res.json({ ok: true, totalUsers: devices.size, totalTokens: Array.from(devices.values()).reduce((a,s)=>a+s.size,0) });
});
api.post('/fcm/unregister', (req, res) => {
  const { token } = req.body || {};
  if (token) {
    for (const [uid, set] of devices.entries()) {
      if (set.has(token)) { set.delete(token); if (set.size===0) devices.delete(uid); break; }
    }
  }
  return res.json({ ok: true, totalUsers: devices.size, totalTokens: Array.from(devices.values()).reduce((a,s)=>a+s.size,0) });
});
api.get('/fcm/stats', (_req, res) => {
  return res.json({ ok: true, totalUsers: devices.size, totalTokens: Array.from(devices.values()).reduce((a,s)=>a+s.size,0), timestamp: nowISO() });
});

// Montaje bajo /api
app.use('/api', api);

// Raíz y health
app.get('/', (_req, res) => res.json({ ok: true, message: 'BarrileteCosmico API', ts: nowISO() }));
app.get('/health', (_req, res) => res.json({ status: 'OK', timestamp: nowISO(), streams: channels.length }));

// ===== Admin protegido (/api/admin) =====
const adminRouter = express.Router();
function requireAuth(req, res, next) {
  const keyHeader = req.headers['x-api-key'] || '';
  const bearer = (req.headers.authorization || '').replace(/^Bearer\s+/i, '');
  const key = (keyHeader || bearer || '').trim();
  const conf = process.env.ADMIN_API_KEY || '';
  if (conf && key === conf) return next();
  return res.status(401).json({ error: 'Unauthorized' });
}

// Historial simple
const notificationHistory = [];

// Init FCM
(function initFCM() {
  try {
    if (!admin) { console.log('🔕 FCM no disponible (firebase-admin no instalado)'); return; }
    if (admin.apps?.length) return;
    const svcJson = process.env.FIREBASE_SERVICE_ACCOUNT;
    const svcPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
    if (svcJson) {
      const creds = JSON.parse(svcJson);
      admin.initializeApp({ credential: admin.credential.cert(creds) });
      console.log('🔔 FCM inicializado (JSON en env).');
    } else if (svcPath && fs.existsSync(svcPath)) {
      const creds = require(svcPath);
      admin.initializeApp({ credential: admin.credential.cert(creds) });
      console.log('🔔 FCM inicializado (ruta en env).');
    } else {
      console.log('🔕 FCM sin credenciales (modo simulación).');
    }
  } catch (e) {
    console.warn('🔕 FCM no inicializado:', e.message);
  }
})();

// helper
function chunk(arr, size) { const out=[]; for (let i=0;i<arr.length;i+=size) out.push(arr.slice(i,i+size)); return out; }

// Enviar notificación (tokens o audiencia 'all')
adminRouter.post('/notifications/send', async (req, res) => {
  try {
    const { title, message, type = 'general', audience = 'all', tokens = [] } = req.body || {};
    if (!title || !message) return res.status(400).json({ error: 'Título y mensaje son requeridos' });

    // elegir tokens
    let target = [];
    if (audience === 'all') {
      for (const set of devices.values()) target.push(...Array.from(set));
    } else if (Array.isArray(tokens) && tokens.length) {
      target = [...new Set(tokens.filter(Boolean))];
    }

    let sent = 0, failed = 0, mode = 'simulation';
    if (admin && admin.apps?.length && target.length) {
      for (const batch of chunk(target, 500)) {
        const resp = await admin.messaging().sendEachForMulticast({
          tokens: batch,
          notification: { title, body: message },
          data: { type: String(type), ts: nowISO() }
        });
        sent += resp.successCount; failed += resp.failureCount;
      }
      mode = 'firebase';
    } else if (target.length === 0) {
      mode = 'no_targets';
    }

    const notification = { id: Date.now(), title, message, type, audience, recipients: sent || target.length, sent_at: new Date(), status: sent>0? 'sent':'queued', fcm_result: { mode, sent, failed } };
    notificationHistory.unshift(notification);
    return res.json({ ok: true, notification });
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
});

// Test
adminRouter.post('/notifications/test', async (req, res) => {
  req.body = { title: '🔔 Prueba', message: 'Mensaje de prueba BarrileteCosmico', type: 'test', audience: 'all' };
  return adminRouter.handle(req, res);
});

// Historial
adminRouter.get('/notifications/history', (_req, res) => res.json({ ok: true, history: notificationHistory }));

// Listar devices
adminRouter.get('/devices', (_req, res) => {
  const items = [];
  for (const [userId, set] of devices.entries()) items.push({ userId, tokens: Array.from(set) });
  res.json({ ok: true, devices: items });
});

// >>> AÑADIDOS: ALIASES DE ENVÍO (NO REEMPLAZAN NADA) <<<

// Alias: enviar por TOPIC (compat)
adminRouter.post('/notify/topic', async (req, res) => {
  try {
    if (!admin?.apps?.length) return res.status(503).json({ error: 'FCM no inicializado' });
    const { topic = 'all', title = 'BarrileteCosmico', body = '', data = {} } = req.body || {};
    const clean = Object.fromEntries(Object.entries(data).map(([k,v]) => [String(k), String(v)]));
    const resp = await admin.messaging().send({
      topic,
      notification: { title, body },
      data: clean,
    });
    return res.json({ ok: true, messageId: resp });
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
});

// Alias: enviar por TOKENS (multicast, chunk 500)
adminRouter.post('/notify/tokens', async (req, res) => {
  try {
    if (!admin?.apps?.length) return res.status(503).json({ error: 'FCM no inicializado' });
    const { tokens = [], title = 'BarrileteCosmico', body = '', data = {} } = req.body || {};
    const uniq = [...new Set(tokens.filter(Boolean))];
    if (!uniq.length) return res.status(400).json({ error: 'Faltan tokens' });

    const clean = Object.fromEntries(Object.entries(data).map(([k,v]) => [String(k), String(v)]));
    const chunkArr = (a,n)=>a.reduce((r,_,i)=>(i%n?r[r.length-1].push(a[i]):r.push([a[i]]),r),[]);
    let success=0,failure=0;
    for (const batch of chunkArr(uniq, 500)) {
      const resp = await admin.messaging().sendEachForMulticast({
        tokens: batch,
        notification: { title, body },
        data: clean
      });
      success += resp.successCount; failure += resp.failureCount;
    }
    return res.json({ ok: true, success, failure });
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
});

// Broadcast a TODOS los tokens registrados en memoria (sin topic)
adminRouter.post('/notify/all', async (req, res) => {
  try {
    if (!admin?.apps?.length) return res.status(503).json({ error: 'FCM no inicializado' });
    const { title = 'BarrileteCosmico TV', body = '', data = {} } = req.body || {};
    const tokens = Array.from(devices.values()).flatMap(s => Array.from(s));
    if (!tokens.length) return res.json({ ok: true, success: 0, failure: 0, note: 'No hay tokens registrados' });

    const clean = Object.fromEntries(Object.entries(data).map(([k,v]) => [String(k), String(v)]));
    const chunkArr = (a,n)=>a.reduce((r,_,i)=>(i%n?r[r.length-1].push(a[i]):r.push([a[i]]),r),[]);
    let success=0,failure=0;
    for (const batch of chunkArr(tokens, 500)) {
      const resp = await admin.messaging().sendEachForMulticast({
        tokens: batch,
        notification: { title, body },
        data: clean
      });
      success += resp.successCount; failure += resp.failureCount;
    }
    return res.json({ ok: true, success, failure, totalTokens: tokens.length });
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
});
// <<< FIN AÑADIDOS >>>

app.use('/api/admin', requireAuth, adminRouter);

// ===== HTTP + Socket.io =====
const PORT = process.env.PORT || 5000; // En Replit usar process.env.PORT
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: '*', methods: ['GET','POST'], credentials: true } });

io.on('connection', (socket) => {
  socket.on('join-channel', ({ streamId, viewerId }) => {
    if (!streamId || !getStream(streamId)) return;
    socket.join(streamId);
    ensureMaps(streamId);
    const id = viewerId || socket.id;
    viewersM.get(streamId).set(String(id), Date.now());
    io.to(streamId).emit('viewer-count-update', { streamId, viewerCount: liveViewerCount(streamId) });
    socket.emit('joined-channel', { streamId, viewerId: id });
  });
  socket.on('leave-channel', ({ streamId, viewerId }) => {
    if (!streamId || !getStream(streamId)) return;
    socket.leave(streamId);
    const id = viewerId || socket.id;
    if (viewersM.get(streamId)) viewersM.get(streamId).delete(String(id));
    io.to(streamId).emit('viewer-count-update', { streamId, viewerCount: liveViewerCount(streamId) });
  });
  socket.on('ping', ({ streamId, viewerId }) => {
    if (!streamId || !getStream(streamId)) return;
    ensureMaps(streamId);
    const id = viewerId || socket.id;
    viewersM.get(streamId).set(String(id), Date.now());
    io.to(streamId).emit('viewer-count-update', { streamId, viewerCount: liveViewerCount(streamId) });
  });
});

server.listen(PORT, () => {
  console.log(`✅ API + WS escuchando en puerto ${PORT}`);
  console.log(`📂 Carpeta de playlists: ${CHANNELS_DIR}`);
});