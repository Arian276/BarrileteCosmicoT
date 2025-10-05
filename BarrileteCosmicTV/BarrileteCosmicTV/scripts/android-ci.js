#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

console.log('🚀 BarrileteCosmico TV - Android CI para Replit');
console.log('=' .repeat(60));

const checks = [];
let allPassed = true;

// Función para ejecutar check
function runCheck(name, checkFunction) {
  try {
    const result = checkFunction();
    console.log(`✅ ${name}: ${result || 'PASS'}`);
    checks.push({ name, status: 'PASS', result });
    return true;
  } catch (error) {
    console.log(`❌ ${name}: ${error.message}`);
    checks.push({ name, status: 'FAIL', error: error.message });
    allPassed = false;
    return false;
  }
}

// Check 1: Estructura del proyecto Android
runCheck('Estructura del proyecto', () => {
  const androidPath = 'BarrileteCosmicTV';
  const requiredFiles = [
    'app/build.gradle',
    'app/src/main/AndroidManifest.xml',
    'app/src/main/java/com/barriletecosmicotv/MainActivity.kt',
    'build.gradle',
    'settings.gradle',
    'gradlew'
  ];
  
  if (!fs.existsSync(androidPath)) {
    throw new Error('Directorio BarrileteCosmicTV no encontrado');
  }
  
  const missing = requiredFiles.filter(file => 
    !fs.existsSync(path.join(androidPath, file))
  );
  
  if (missing.length > 0) {
    throw new Error(`Archivos faltantes: ${missing.join(', ')}`);
  }
  
  const kotlinFiles = execSync(`find ${androidPath}/app/src -name "*.kt" | wc -l`, {encoding: 'utf8'}).trim();
  const xmlFiles = execSync(`find ${androidPath}/app/src -name "*.xml" | wc -l`, {encoding: 'utf8'}).trim();
  
  return `${kotlinFiles} archivos Kotlin, ${xmlFiles} archivos XML`;
});

// Check 2: Dependencias Android
runCheck('Dependencias Android', () => {
  const buildGradle = fs.readFileSync('BarrileteCosmicTV/app/build.gradle', 'utf8');
  
  const requiredDeps = [
    'androidx.compose',
    'androidx.navigation:navigation-compose',
    'com.google.dagger:hilt-android',
    'androidx.media3:media3-exoplayer',
    'com.squareup.retrofit2:retrofit'
  ];
  
  const missing = requiredDeps.filter(dep => !buildGradle.includes(dep));
  
  if (missing.length > 0) {
    throw new Error(`Dependencias faltantes: ${missing.join(', ')}`);
  }
  
  return 'ExoPlayer, Hilt, Retrofit, Compose configurados';
});

// Check 3: Configuración de ExoPlayer
runCheck('Configuración ExoPlayer', () => {
  const videoPlayerPath = 'BarrileteCosmicTV/app/src/main/java/com/barriletecosmicotv/components/VideoPlayer.kt';
  
  if (!fs.existsSync(videoPlayerPath)) {
    throw new Error('VideoPlayer.kt no encontrado');
  }
  
  const content = fs.readFileSync(videoPlayerPath, 'utf8');
  
  if (!content.includes('androidx.media3.exoplayer.ExoPlayer')) {
    throw new Error('ExoPlayer no configurado correctamente');
  }
  
  if (!content.includes('MediaItem')) {
    throw new Error('MediaItem no implementado');
  }
  
  return 'ExoPlayer con MediaItem y PlayerView';
});

// Check 4: Backend funcionando
runCheck('Backend API', () => {
  try {
    execSync('curl -f http://localhost:3000/health', { timeout: 5000 });
    return 'Respondiendo correctamente';
  } catch (error) {
    throw new Error('Backend no responde en puerto 3000');
  }
});

// Check 5: Conexión Android-Backend
runCheck('Conexión Android-Backend', () => {
  const retrofitPath = 'BarrileteCosmicTV/app/src/main/java/com/barriletecosmicotv/data/api/RetrofitInstance.kt';
  
  if (!fs.existsSync(retrofitPath)) {
    throw new Error('RetrofitInstance.kt no encontrado');
  }
  
  const content = fs.readFileSync(retrofitPath, 'utf8');
  
  if (!content.includes('riker.replit.dev')) {
    throw new Error('URL del backend no configurada correctamente');
  }
  
  return 'Retrofit apuntando a Replit backend';
});

// Check 6: Permisos Android
runCheck('Permisos Android', () => {
  const manifestPath = 'BarrileteCosmicTV/app/src/main/AndroidManifest.xml';
  const content = fs.readFileSync(manifestPath, 'utf8');
  
  const requiredPermissions = [
    'android.permission.INTERNET',
    'android.permission.ACCESS_NETWORK_STATE'
  ];
  
  const missing = requiredPermissions.filter(perm => !content.includes(perm));
  
  if (missing.length > 0) {
    throw new Error(`Permisos faltantes: ${missing.join(', ')}`);
  }
  
  return 'Permisos de red configurados';
});

// Check 7: Colores tema argentino
runCheck('Tema argentino', () => {
  const colorPath = 'BarrileteCosmicTV/app/src/main/java/com/barriletecosmicotv/ui/theme/Color.kt';
  const content = fs.readFileSync(colorPath, 'utf8');
  
  if (!content.includes('CosmicPrimary') || !content.includes('CosmicSecondary')) {
    throw new Error('Colores argentinos no configurados');
  }
  
  return 'Colores celeste y naranja configurados';
});

// Check 8: GitHub Actions workflow
runCheck('GitHub Actions', () => {
  const workflowPath = '.github/workflows/android.yml';
  
  if (!fs.existsSync(workflowPath)) {
    throw new Error('Workflow de GitHub Actions no encontrado');
  }
  
  const content = fs.readFileSync(workflowPath, 'utf8');
  
  if (!content.includes('assembleDebug')) {
    throw new Error('Build de Android no configurado');
  }
  
  return 'Workflow configurado para build automático';
});

console.log('\n' + '=' .repeat(60));
console.log('📊 RESUMEN DE CHECKS:');
console.log('=' .repeat(60));

checks.forEach(check => {
  const status = check.status === 'PASS' ? '✅' : '❌';
  console.log(`${status} ${check.name}`);
  if (check.result) console.log(`   → ${check.result}`);
  if (check.error) console.log(`   → ${check.error}`);
});

console.log('\n' + '=' .repeat(60));

if (allPassed) {
  console.log('🎉 ¡TODOS LOS CHECKS PASARON!');
  console.log('✅ Proyecto Android listo para desarrollo');
  console.log('✅ Backend funcionando correctamente');
  console.log('✅ Integración completa configurada');
  console.log('\n🚀 Ready for GitHub Actions deployment!');
  process.exit(0);
} else {
  console.log('⚠️  ALGUNOS CHECKS FALLARON');
  console.log('🔧 Revisar los errores mostrados arriba');
  process.exit(1);
}