const fs = require('fs');
const path = require('path');
const bcrypt = require('bcryptjs');

const usersFile = path.join(process.cwd(), 'users.json');
const username = process.argv[2];
const newPassword = process.argv[3];

if (!username || !newPassword) {
  console.error("⚠️ Uso: node scripts/update-password.js <username> <nuevaContraseña>");
  process.exit(1);
}

if (!fs.existsSync(usersFile)) {
  console.error("❌ No existe users.json");
  process.exit(1);
}

const data = JSON.parse(fs.readFileSync(usersFile, 'utf8'));
const user = data.users.find(u => u.username === username);

if (!user) {
  console.error(`❌ Usuario '${username}' no encontrado`);
  process.exit(1);
}

// generar hash nuevo
user.passwordHash = bcrypt.hashSync(newPassword, 10);
delete user.password; // opcional: borrar campo password si existía

fs.writeFileSync(usersFile, JSON.stringify(data, null, 2));
console.log(`✅ Contraseña actualizada para usuario '${username}'`);