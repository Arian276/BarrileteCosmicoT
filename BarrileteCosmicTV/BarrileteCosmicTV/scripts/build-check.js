#!/usr/bin/env node

console.log('🔧 BarrileteCosmico TV - Build Check');
console.log('=' .repeat(40));

// Verificar que el backend esté funcionando
try {
  require('child_process').execSync('curl -f http://localhost:3000/health', { timeout: 5000 });
  console.log('✅ Backend API funcionando');
} catch (error) {
  console.log('❌ Backend API no responde');
  process.exit(1);
}

console.log('✅ Build check completado');