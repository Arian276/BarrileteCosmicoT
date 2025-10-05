const fs = require('fs');
const path = require('path');

const usersFile = path.join(process.cwd(), 'users.json');
const usernameToRemove = process.argv[2];

if (!usernameToRemove) {
  console.error("⚠️ Uso: node scripts/remove-user.js <username>");
  process.exit(1);
}

if (!fs.existsSync(usersFile)) {
  console.error("❌ No existe users.json");
  process.exit(1);
}

const data = JSON.parse(fs.readFileSync(usersFile, 'utf8'));
data.users = data.users.filter(u => u.username !== usernameToRemove);

fs.writeFileSync(usersFile, JSON.stringify(data, null, 2));
console.log(`✅ Usuario '${usernameToRemove}' eliminado`);