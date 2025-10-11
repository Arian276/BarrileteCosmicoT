// server.js (completo, corregido y con gate de suscripción para ocultar canales si daysRemaining <= 0)

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
      if (json && Array.isArray(json.users)) return json.users;
    }
  } catch (e) {
    console.warn('No se pudo leer users.json, usando usuarios por defecto:', e.message);
  }
  return DEFAULT_USERS;
}

async function checkPassword(userRec, plain) {
  if (userRec.passwordHash && bcrypt) {
    try { return await bcrypt.compare(String(plain), String(userRec.passwordHash)); }
    catch { /* ignore */ }
  }
  if (typeof userRec.password === 'string') {
    return String(userRec.password) === String(plain);
  }
  return false;
}

// ===== util de suscripción: días restantes =====
function calcDaysRemaining(expiresAt) {
  if (!expiresAt) return -1;
  const exp = Date.parse(expiresAt);
  if (Number.isNaN(exp)) return -1;
  const diff = exp - Date.now();
  const d = Math.ceil(diff / (24 * 60 * 60 * 1000));
  return d < 0 ? 0 : d;
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
        let host = ''; try { host = new URL(url).hostname.replace(/^www\./, ''); } catch { /* ignore */ }
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

    if (!L.startsWith('#') && (L.includes(',') || L.includes(';'))) {
      const sep = L.includes(',') ? ',' : ';';
      const parts = L.split(sep);
      if (parts.length >= 2 && urlRe.test(parts[1].trim())) {
        const title = parts[0].trim() || sourceName;
        const url = parts[1].trim().match(urlRe)[1];
        let host = ''; try { host = new URL(url).hostname.replace(/^www\./, ''); } catch { /* ignore */ }
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

    if (urlRe.test(L)) {
      const url = L.match(urlRe)[1];
      let host = ''; try { host = new URL(url).hostname.replace(/^www\./, ''); } catch { /* ignore */ }
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
const chatDB   = new Map();
const likesDB  = new Map();
const viewersM = new Map();
const VIEWER_TTL_MS = 60_000;

const userSessions = new Map();

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
setInterval(() => {
  const now = Date.now();
  for (const m of viewersM.values()) {
    for (const [vid, ts] of m.entries()) if (now - ts > VIEWER_TTL_MS) m.delete(vid);
  }
  for (const [username, sessions] of userSessions.entries()) {
    for (const [key, ts] of sessions.entries()) {
      if (now - ts > VIEWER_TTL_MS) sessions.delete(key);
    }
    if (sessions.size === 0) userSessions.delete(username);
  }
}, 15_000);

// ===== Persistir activeSessions a users.json (cada ~7s, escritura atómica) =====
function persistActiveSessionsToUsersJson() {
  try {
    if (!fs.existsSync(usersFilePath)) return; // si no hay archivo, no persistimos
    const raw = fs.readFileSync(usersFilePath, 'utf8');
    const json = JSON.parse(raw);
    if (!json || !Array.isArray(json.users)) return;

    const updated = {
      ...json,
      users: json.users.map(u => {
        const username = String(u.username ?? '');
        const sessions = userSessions.get(username);
        const activeSessions = sessions ? sessions.size : 0;
        return { ...u, activeSessions };
      })
    };

    const tmpPath = usersFilePath + '.tmp';
    fs.writeFileSync(tmpPath, JSON.stringify(updated, null, 2), 'utf8');
    fs.renameSync(tmpPath, usersFilePath);
  } catch (e) {
    console.warn('No se pudo persistir activeSessions en users.json:', e.message);
  }
}
setInterval(persistActiveSessionsToUsersJson, 7000);

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

    console.log(`${path.basename(filePath)}: ${parsed.length} canales. Total: ${channels.length}`);
  } catch (e) {
    console.error(`Error en ${path.basename(filePath)}:`, e.message);
  }
}
function scanDir(dir) {
  if (!fs.existsSync(dir)) { console.warn(`No existe ${dir}`); channels = []; return; }
  const files = fs.readdirSync(dir).filter(f => /\.m3u8?$/i.test(f));
  channels = [];
  files.forEach(f => processFile(path.join(dir, f)));
  console.log(`Total tras escaneo: ${channels.length}`);
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
    console.log(`${path.basename(f)} eliminado: ${before - channels.length} canales. Total: ${channels.length}`);
  });
  console.log(`Monitoreando: ${dir}`);
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

// ===== API (/api) =====
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

  // === Expiración top-level ===
  // 1) Si users.json trae expiresAt, usamos esa. 2) Si no, simulamos +3 días.
  const simulatedExp = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString();
  const expiresAt = user.expiresAt || simulatedExp;
  const daysRemaining = calcDaysRemaining(expiresAt);

  return res.json({
    success: true,
    token: `fake-token-for-${username}-${Date.now()}`,
    user: {
      username: user.username,
      name: user.name || user.username,
      registeredAt: user.registeredAt || null
    },
    // top-level para la app
    expiresAt,
    daysRemaining
  });
});

// ===== Cambio de contraseña (robusto: usa bcrypt y persiste en users.json) =====
api.post('/change-password', async (req, res) => {
  try {
    // Acepta username del token (si viene) o del body (retro-compat)
    const bearer = (req.headers.authorization || '').replace(/^Bearer\s+/i, '').trim();
    const userFromToken = bearer ? parseUsernameFromToken(bearer) : null;

    const { username: bodyUser, currentPassword, newPassword } = req.body || {};
    const username = userFromToken || bodyUser;

    if (!username || !currentPassword || !newPassword) {
      return res.status(400).json({ error: 'Faltan campos requeridos' });
    }
    if (String(newPassword).length < 4) {
      return res.status(400).json({ error: 'La nueva contraseña es demasiado corta' });
    }

    // Leer users.json (mismo path que usa tu login)
    if (!fs.existsSync(usersFilePath)) {
      return res.status(500).json({ error: 'No se encontró users.json' });
    }

    const raw = fs.readFileSync(usersFilePath, 'utf8');
    const data = JSON.parse(raw);
    if (!data || !Array.isArray(data.users)) {
      return res.status(500).json({ error: 'Formato inválido en users.json' });
    }

    const users = data.users;
    const idx = users.findIndex(u => String(u.username) === String(username));
    if (idx === -1) return res.status(404).json({ error: 'Usuario no encontrado' });

    const target = users[idx];

    // 🚫 BLOQUEO PARA USUARIOS DE PRUEBA (name === "Prueba", case-insensitive)
    if (String(target.name || '').toLowerCase() === 'prueba') {
      return res.status(403).json({ error: 'Las cuentas de prueba no pueden cambiar la contraseña.' });
    }

    // Validar contraseña actual (hash con bcrypt o password plano)
    let isValid = false;
    if (target.passwordHash && bcrypt) {
      try {
        isValid = await bcrypt.compare(String(currentPassword), String(target.passwordHash));
      } catch {
        isValid = false;
      }
    } else if (typeof target.password === 'string') {
      isValid = (String(target.password) === String(currentPassword));
    }

    if (!isValid) {
      return res.status(401).json({ error: 'Contraseña actual incorrecta' });
    }

    // Evitar poner la misma contraseña
    if (target.passwordHash && bcrypt) {
      const same = await bcrypt.compare(String(newPassword), String(target.passwordHash));
      if (same) return res.status(409).json({ error: 'La nueva contraseña no puede ser igual a la actual' });
    } else if (typeof target.password === 'string') {
      if (String(target.password) === String(newPassword)) {
        return res.status(409).json({ error: 'La nueva contraseña no puede ser igual a la actual' });
      }
    }

    // Actualizar credencial
    if (bcrypt) {
      const hash = await bcrypt.hash(String(newPassword), 10);
      delete target.password;
      target.passwordHash = hash;
    } else {
      // Si no hay bcrypt instalado, guarda en plano (tu login ya soporta ambos)
      delete target.passwordHash;
      target.password = String(newPassword);
    }

    // Escritura atómica para evitar pisadas por otros intervalos
    const tmp = usersFilePath + '.tmp';
    fs.writeFileSync(tmp, JSON.stringify({ users }, null, 2), 'utf8');
    fs.renameSync(tmp, usersFilePath);

    // Confirmación mínima
    return res.json({ ok: true, message: 'Contraseña actualizada correctamente' });
  } catch (err) {
    console.error('change-password error:', err);
    return res.status(500).json({ error: 'Error interno del servidor' });
  }
});

// ===== AUTH helper =====
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

// ===== Gate de suscripción: si está vencida, bloquea/filtra streams =====
function getUserExpiry(username) {
  const users = loadUsers();
  const user = users.find(u => String(u.username) === String(username));
  return user?.expiresAt || null;
}
function isExpired(username) {
  const exp = getUserExpiry(username);
  const d = calcDaysRemaining(exp);
  return d === 0 || d === -1; // 0 o inválido => expirado/no habilitado
}

// ===== STREAMS (protegido) =====
api.use('/streams', requireUserAuth);

// Listado general: si expiró, devolver []
api.get('/streams', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json([]); // sin canales para cuentas vencidas
  }
  const { category } = req.query;
  return res.json((category && category !== 'all') ? byCategory(category) : channels);
});

api.get('/streams/featured', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json([]); // sin destacados
  }
  return res.json(channels.slice(0, 10));
});

api.get('/streams/:id', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: false, error: 'SUBSCRIPTION_EXPIRED', streamId: req.params.id });
  }
  const s = getStream(req.params.id);
  if (!s) return res.json({ active: false, error: 'Stream no encontrado', streamId: req.params.id });
  const likesEntry = likesDB.get(s.id) || { likes: 0 };
  res.json({ active: true, ...s, viewerCount: liveViewerCount(s.id), likes: likesEntry.likes });
});

api.get('/streams/search', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json([]); // sin resultados
  }
  return res.json(searchStreams(req.query.query || ''));
});

api.get('/streams/category/:category', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json([]); // sin resultados
  }
  return res.json(byCategory(req.params.category));
});

api.post('/streams/:id/join', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: false, streamId: req.params.id, error: 'SUBSCRIPTION_EXPIRED' });
  }
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.json({ active: false, streamId, error: 'Stream no encontrado' });

  const viewerId = (req.body && req.body.viewerId) ? String(req.body.viewerId) : uuidv4();
  ensureMaps(streamId);
  viewersM.get(streamId).set(viewerId, Date.now());

  // track de sesión por usuario
  const username = req.user?.username;
  if (username) {
    if (!userSessions.has(username)) userSessions.set(username, new Map());
    const sessionKey = `${streamId}:${viewerId}`;
    userSessions.get(username).set(sessionKey, Date.now());
  }

  io.to(streamId).emit('viewer-count-update', { streamId, viewerCount: liveViewerCount(streamId) });
  res.json({ active: true, viewerId, viewerCount: liveViewerCount(streamId), streamId });
});

api.post('/streams/:id/ping', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: false, streamId: req.params.id, error: 'SUBSCRIPTION_EXPIRED' });
  }
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.json({ active: false, streamId, error: 'Stream no encontrado' });

  const viewerId = (req.body && req.body.viewerId) ? String(req.body.viewerId) : null;
  if (viewerId) {
    ensureMaps(streamId);
    viewersM.get(streamId).set(viewerId, Date.now());

    // refresco de sesión por usuario
    const username = req.user?.username;
    if (username) {
      if (!userSessions.has(username)) userSessions.set(username, new Map());
      const sessionKey = `${streamId}:${viewerId}`;
      userSessions.get(username).set(sessionKey, Date.now());
    }
  }
  res.json({ active: true, viewerCount: liveViewerCount(streamId), streamId });
});

api.post('/streams/:id/leave', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: true, viewerId: '', viewerCount: 0, streamId: req.params.id });
  }
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.json({ active: false, streamId, error: 'Stream no encontrado' });

  const viewerId = (req.body && req.body.viewerId) ? String(req.body.viewerId) : null;
  if (viewerId && viewersM.get(streamId)) {
    viewersM.get(streamId).delete(viewerId);
  }

  // baja de sesión por usuario
  const username = req.user?.username;
  if (username && userSessions.has(username) && viewerId) {
    const sessionKey = `${streamId}:${viewerId}`;
    userSessions.get(username).delete(sessionKey);
    if (userSessions.get(username).size === 0) userSessions.delete(username);
  }

  io.to(streamId).emit('viewer-count-update', { streamId, viewerCount: liveViewerCount(streamId) });
  res.json({ active: true, viewerId: viewerId || '', viewerCount: liveViewerCount(streamId), streamId });
});

api.get('/streams/:id/viewers', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: true, streamId: req.params.id, viewerCount: 0, timestamp: nowISO() });
  }
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.json({ active: false, streamId, viewerCount: 0, error: 'Stream no encontrado' });
  res.json({ active: true, streamId, viewerCount: liveViewerCount(s.id), timestamp: nowISO() });
});

// ===== LIKES =====
function ensureLike(streamId) { ensureMaps(streamId); return likesDB.get(streamId); }
api.post('/streams/:id/like', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: false, streamId: req.params.id, likes: 0, liked: false, error: 'SUBSCRIPTION_EXPIRED' });
  }
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.json({ active: false, streamId, error: 'Stream no encontrado' });
  const userId = req.body?.userId;
  if (!userId) return res.status(400).json({ error: 'userId es requerido' });

  const entry = ensureLike(streamId);
  const has = entry.likedBy.has(userId);
  if (has) { entry.likedBy.delete(userId); entry.likes = Math.max(0, entry.likes - 1); }
  else     { entry.likedBy.add(userId);    entry.likes += 1; }

  io.to(streamId).emit('like-update', { streamId, likes: entry.likes });
  res.json({ active: true, streamId, likes: entry.likes, liked: entry.likedBy.has(userId), timestamp: nowISO() });
});
api.get('/streams/:id/likes', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: false, streamId: req.params.id, likes: 0, liked: false, error: 'SUBSCRIPTION_EXPIRED' });
  }
  const streamId = req.params.id;
  const s = getStream(streamId);
  if (!s) return res.json({ active: false, streamId, likes: 0, liked: false, error: 'Stream no encontrado' });
  const userId = req.query?.userId;
  const entry = likesDB.get(streamId) || { likes: 0, likedBy: new Set() };
  res.json({ active: true, streamId, likes: entry.likes, liked: !!(userId && entry.likedBy.has(String(userId))), timestamp: nowISO() });
});

// ===== CHAT =====
api.post('/streams/:id/chat', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: false, streamId: req.params.id, error: 'SUBSCRIPTION_EXPIRED' });
  }
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.json({ active: false, streamId, error: 'Stream no encontrado' });
  const { username, message } = req.body || {};
  if (!username || !message) return res.status(400).json({ error: 'Username y message son requeridos' });

  ensureMaps(streamId);
  const msg = { id: uuidv4(), username: String(username), message: String(message), timestamp: Date.now(), colorHex: '#00BFFF' };
  chatDB.get(streamId).push(msg);
  io.to(streamId).emit('chat-message', { streamId, message: msg });
  res.json({ active: true, streamId, message: msg, timestamp: nowISO() });
});
api.get('/streams/:id/chat', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: true, streamId: req.params.id, messages: [], totalMessages: 0, timestamp: nowISO() });
  }
  const streamId = req.params.id;
  const s = getStream(streamId);
  if (!s) return res.json({ active: false, streamId, messages: [], totalMessages: 0, error: 'Stream no encontrado' });
  const limit  = Math.max(1, Math.min(200, parseInt(req.query.limit  ?? '50', 10)));
  const offset = Math.max(0,                parseInt(req.query.offset ?? '0' , 10));
  ensureMaps(streamId);
  const arr = chatDB.get(streamId);
  res.json({ active: true, streamId, messages: arr.slice(offset, offset + limit), totalMessages: arr.length, offset, limit, timestamp: nowISO() });
});
api.delete('/streams/:id/chat', (req, res) => {
  if (isExpired(req.user.username)) {
    return res.json({ active: false, streamId: req.params.id, error: 'SUBSCRIPTION_EXPIRED' });
  }
  const streamId = req.params.id;
  const s = getStream(streamId); if (!s) return res.json({ active: false, streamId, error: 'Stream no encontrado' });
  chatDB.set(streamId, []);
  res.json({ active: true, streamId, message: 'Chat limpiado', timestamp: nowISO() });
});

// ===== CATEGORIES / UI =====
api.get('/categories', (req, res) => {
  // Categorías vacías si expiró (para que UI no muestre conteos)
  if (req.headers.authorization) {
    const username = parseUsernameFromToken((req.headers.authorization || '').replace(/^Bearer\s+/i, '').trim());
    if (username && isExpired(username)) return res.json([]);
  }
  return res.json(buildCategories(channels));
});

let uiConfig = { theme: 'dark', featuredCategory: 'deportes', showViewerCount: true, updatedAt: nowISO() };
api.get('/ui-config', (_req, res) => res.json({ success: true, config: uiConfig, timestamp: nowISO() }));
api.put('/ui-config', (req, res) => { uiConfig = { ...uiConfig, ...(req.body || {}), updatedAt: nowISO() }; res.json({ success: true, config: uiConfig, timestamp: nowISO() }); });
api.get('/ui-config/:section', (req, res) => { const section = req.params.section; res.json({ success: true, section, data: { [section]: uiConfig[section] }, timestamp: nowISO() }); });
api.put('/ui-config/:section', (req, res) => { const section = req.params.section; uiConfig[section] = req.body?.value ?? req.body; uiConfig.updatedAt = nowISO(); res.json({ success: true, section, data: { [section]: uiConfig[section] }, timestamp: nowISO() }); });

// ===== Endpoint del propio usuario (opcional, útil para probar) =====
api.get('/me/sessions/count', requireUserAuth, (req, res) => {
  const username = req.user.username;
  const sessions = userSessions.get(username);
  return res.json({ username, activeSessions: sessions ? sessions.size : 0, timestamp: nowISO() });
});

// ===== Endpoint de suscripción =====
api.get('/me/subscription', requireUserAuth, (req, res) => {
  const username = req.user.username;
  const users = loadUsers();
  const user = users.find(u => String(u.username) === String(username));
  const exp = user?.expiresAt || null;
  return res.json({
    ok: true,
    username,
    expiresAt: exp,
    daysRemaining: calcDaysRemaining(exp),
    timestamp: nowISO()
  });
});

// ===== FCM público =====
const devices = new Map(); // userId -> Set(tokens)
api.post('/fcm/register', async (req, res) => {
  const { token, userId = 'anon', platform } = req.body || {};
  if (!token) return res.status(400).json({ error: 'Falta token' });
  if (!devices.has(userId)) devices.set(userId, new Set());
  devices.get(userId).add(token);

  try {
    if (admin?.apps?.length) {
      await admin.messaging().subscribeToTopic([token], 'all');
    }
  } catch (_) {}

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

(function initFCM() {
  try {
    if (!admin) { console.log('FCM no disponible (firebase-admin no instalado)'); return; }
    if (admin.apps?.length) return;
    const svcJson = process.env.FIREBASE_SERVICE_ACCOUNT;
    const svcPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
    if (svcJson) {
      const creds = JSON.parse(svcJson);
      admin.initializeApp({ credential: admin.credential.cert(creds) });
      console.log('FCM inicializado (JSON en env).');
    } else if (svcPath && fs.existsSync(svcPath)) {
      const creds = require(svcPath);
      admin.initializeApp({ credential: admin.credential.cert(creds) });
      console.log('FCM inicializado (ruta en env).');
    } else {
      console.log('FCM sin credenciales (modo simulación).');
    }
  } catch (e) {
    console.warn('FCM no inicializado:', e.message);
  }
})();

function chunk(arr, size) { const out=[]; for (let i=0;i<arr.length;i+=size) out.push(arr.slice(i,i+size)); return out; }

adminRouter.post('/notifications/send', async (req, res) => {
  try {
    const { title, message, type = 'general', audience = 'all', tokens = [] } = req.body || {};
    if (!title || !message) return res.status(400).json({ error: 'Título y mensaje son requeridos' });

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

adminRouter.post('/notifications/test', async (req, res) => {
  req.body = { title: 'Prueba', message: 'Mensaje de prueba BarrileteCosmico', type: 'test', audience: 'all' };
  return adminRouter.handle(req, res);
});

adminRouter.get('/notifications/history', (_req, res) => res.json({ ok: true, history: notificationHistory }));

adminRouter.get('/devices', (_req, res) => {
  const items = [];
  for (const [userId, set] of devices.entries()) items.push({ userId, tokens: Array.from(set) });
  res.json({ ok: true, devices: items });
});

adminRouter.get('/users/active-sessions', (_req, res) => {
  const out = [];
  let total = 0;
  for (const [username, sessions] of userSessions.entries()) {
    out.push({ username, activeSessions: sessions.size });
    total += sessions.size;
  }
  res.json({ ok: true, users: out, totalSessions: total, timestamp: nowISO() });
});
adminRouter.get('/users/:username/active-sessions', (req, res) => {
  const { username } = req.params;
  const sessions = userSessions.get(username);
  if (!sessions) return res.json({ ok: true, username, activeSessions: 0, sessions: [] });
  const list = Array.from(sessions.keys()).map(k => {
    const [streamId, viewerId] = k.split(':');
    return { streamId, viewerId, lastPingMs: sessions.get(k) };
  });
  res.json({ ok: true, username, activeSessions: sessions.size, sessions: list });
});

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
  console.log(`API + WS escuchando en puerto ${PORT}`);
  console.log(`Carpeta de playlists: ${CHANNELS_DIR}`);
});