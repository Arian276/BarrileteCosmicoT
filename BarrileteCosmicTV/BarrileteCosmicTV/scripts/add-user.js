// scripts/add-user.js
const fs = require('fs');
const path = require('path');
const bcrypt = require('bcryptjs');

const usersFile = path.join(process.cwd(), 'users.json');

function load() {
  if (!fs.existsSync(usersFile)) return { users: [] };
  return JSON.parse(fs.readFileSync(usersFile, 'utf8'));
}

async function addUser(username, password, name = '') {
  const data = load();
  if (data.users.some(u => u.username === username)) {
    console.error('Usuario ya existe');
    process.exit(1);
  }
  const salt = await bcrypt.genSalt(10);
  const hash = await bcrypt.hash(password, salt);
  data.users.push({ username, passwordHash: hash, name });
  fs.writeFileSync(usersFile, JSON.stringify(data, null, 2));
  console.log('Usuario creado:', username);
}

const [,, username, password, name] = process.argv;
if (!username || !password) {
  console.log('Uso: node scripts/add-user.js <username> <password> [name]');
  process.exit(1);
}

addUser(username, password, name).catch(e => { console.error(e); process.exit(1); });