#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

console.log('📱 BarrileteCosmico TV - Validación Android');
console.log('=' .repeat(50));

// Validar estructura básica
const androidPath = 'BarrileteCosmicTV';
console.log(`🔍 Validando proyecto en: ${androidPath}/`);

if (!fs.existsSync(androidPath)) {
  console.log('❌ Directorio del proyecto Android no encontrado');
  process.exit(1);
}

// Contar archivos
const kotlinFiles = require('child_process').execSync(
  `find ${androidPath}/app/src -name "*.kt" | wc -l`, 
  {encoding: 'utf8'}
).trim();

const xmlFiles = require('child_process').execSync(
  `find ${androidPath}/app/src -name "*.xml" | wc -l`, 
  {encoding: 'utf8'}
).trim();

console.log(`📁 Archivos Kotlin: ${kotlinFiles}`);
console.log(`📁 Archivos XML: ${xmlFiles}`);

// Validar archivos clave
const keyFiles = [
  'app/build.gradle',
  'app/src/main/AndroidManifest.xml',
  'app/src/main/java/com/barriletecosmicotv/MainActivity.kt',
  'app/src/main/java/com/barriletecosmicotv/components/VideoPlayer.kt'
];

console.log('\n🔍 Validando archivos clave:');
keyFiles.forEach(file => {
  const exists = fs.existsSync(path.join(androidPath, file));
  console.log(`${exists ? '✅' : '❌'} ${file}`);
});

console.log('\n✅ Validación completada');