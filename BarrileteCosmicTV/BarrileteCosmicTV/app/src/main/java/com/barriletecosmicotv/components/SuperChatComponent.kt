package com.barriletecosmicotv.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barriletecosmicotv.data.SimpleChatMessage
import com.barriletecosmicotv.ui.theme.*
import com.barriletecosmicotv.viewmodel.SimpleChatViewModel
import kotlinx.coroutines.launch

/**
 * 🚀 CHAT SÚPER BONITO CON MUCHA ONDA - Rediseño total con animaciones y gradientes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperChatComponent(
    streamId: String,
    modifier: Modifier = Modifier,
    viewModel: SimpleChatViewModel = hiltViewModel(),
    isCompact: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Estados del chat original
    val chatMessages by viewModel.getChatMessages(streamId).collectAsState(initial = emptyList())
    val savedUsername by viewModel.username.collectAsState(initial = "")
    var currentMessage by rememberSaveable { mutableStateOf("") }
    var showUsernameDialog by rememberSaveable { mutableStateOf(false) }
    
    // Auto-scroll a mensajes nuevos
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    // Solicitar username si no existe
    LaunchedEffect(savedUsername) {
        if (savedUsername.isBlank()) {
            showUsernameDialog = true
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        // 🌟 FONDO CON GRADIENTE SÚPER BONITO
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.95f),
                            CosmicPrimary.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.98f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 🎨 HEADER SÚPER ESTILIZADO CON GRADIENTE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    CosmicPrimary.copy(alpha = 0.3f),
                                    CosmicSecondary.copy(alpha = 0.2f),
                                    CosmicPrimary.copy(alpha = 0.3f)
                                )
                            ),
                            RoundedCornerShape(15.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 🔥 ICONO ANIMADO
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(
                                                CosmicPrimary,
                                                CosmicSecondary
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Chat",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "💬 CHAT EN VIVO",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (chatMessages.isNotEmpty()) {
                                    Text(
                                        text = "🔥 ${chatMessages.size} mensajes activos",
                                        color = CosmicSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        // 🌟 INDICADOR DE ESTADO LIVE
                        Box(
                            modifier = Modifier
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Red,
                                            Color(0xFFFF6B6B)
                                        )
                                    ),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🔴 LIVE",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 🌈 ÁREA DE MENSAJES SÚPER ESTILIZADA
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    CosmicPrimary.copy(alpha = 0.05f),
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            ),
                            RoundedCornerShape(15.dp)
                        )
                        .padding(12.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 🎭 ICONO ANIMADO VACÍO
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            androidx.compose.ui.graphics.Brush.radialGradient(
                                                colors = listOf(
                                                    CosmicPrimary.copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEmotions,
                                        contentDescription = null,
                                        tint = CosmicPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "🚀 ¡Sé el primero en comentar!",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "💬 Comparte tu emoción",
                                    color = CosmicMuted,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = chatMessages.filter { it.username.isNotBlank() },
                                key = { it.id }
                            ) { message ->
                                SuperStyledChatMessageItem(message = message)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 🎮 ÁREA DE INPUT SÚPER ESTILIZADA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    CosmicPrimary.copy(alpha = 0.1f),
                                    CosmicSecondary.copy(alpha = 0.1f),
                                    CosmicPrimary.copy(alpha = 0.1f)
                                )
                            ),
                            RoundedCornerShape(25.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = currentMessage,
                            onValueChange = { currentMessage = it },
                            placeholder = {
                                Text(
                                    text = if (savedUsername.isNotBlank()) "💬 Escribe algo épico..." else "👤 Ingresa tu nombre primero",
                                    color = CosmicMuted.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            },
                            enabled = savedUsername.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicPrimary,
                                unfocusedBorderColor = CosmicPrimary.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                                disabledTextColor = CosmicMuted.copy(alpha = 0.5f),
                                cursorColor = CosmicSecondary,
                                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(25.dp),
                            maxLines = 3
                        )
                        
                        // 🚀 BOTÓN SÚPER ESTILIZADO
                        IconButton(
                            onClick = {
                                if (currentMessage.isNotBlank() && savedUsername.isNotBlank()) {
                                    viewModel.sendMessage(
                                        streamId = streamId,
                                        message = currentMessage,
                                        username = savedUsername
                                    )
                                    currentMessage = ""
                                    
                                    // Scroll al final
                                    scope.launch {
                                        if (chatMessages.isNotEmpty()) {
                                            listState.animateScrollToItem(chatMessages.size)
                                        }
                                    }
                                }
                            },
                            enabled = currentMessage.isNotBlank() && savedUsername.isNotBlank(),
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    if (currentMessage.isNotBlank()) {
                                        androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(
                                                CosmicSecondary,
                                                CosmicPrimary
                                            )
                                        )
                                    } else {
                                        androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(
                                                CosmicMuted.copy(alpha = 0.3f),
                                                CosmicMuted.copy(alpha = 0.1f)
                                            )
                                        )
                                    },
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Rocket,
                                contentDescription = "Enviar mensaje",
                                tint = if (currentMessage.isNotBlank()) Color.White else CosmicMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo de username súper estilizado
    if (showUsernameDialog) {
        SuperUsernameDialog(
            onUsernameSelected = { username ->
                viewModel.saveUsername(username)
                showUsernameDialog = false
            },
            onDismiss = { showUsernameDialog = false }
        )
    }
}

@Composable
fun SuperStyledChatMessageItem(message: SimpleChatMessage) {
    val isAdmin = message.username.equals("BarrileteCosmicoTv", ignoreCase = true)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 🎨 AVATAR SÚPER ESTILIZADO
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            if (isAdmin) Color.Red else CosmicPrimary,
                            if (isAdmin) Color(0xFFFF6B6B) else CosmicSecondary
                        )
                    ),
                    CircleShape
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAdmin) "👑" else message.username.take(1).uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 💬 BURBUJA DE MENSAJE SÚPER ESTILIZADA
        Box(
            modifier = Modifier
                .background(
                    if (isAdmin) {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red.copy(alpha = 0.3f),
                                Color(0xFFFF6B6B).copy(alpha = 0.2f)
                            )
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                CosmicPrimary.copy(alpha = 0.2f),
                                CosmicSecondary.copy(alpha = 0.1f)
                            )
                        )
                    },
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 12.dp,
                        bottomEnd = 12.dp,
                        bottomStart = 12.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = if (isAdmin) "👑 ${message.username}" else "🔥 ${message.username}",
                    color = if (isAdmin) Color.Red else CosmicPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperUsernameDialog(
    onUsernameSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black.copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = CosmicPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🚀 ¡Únete al chat!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Ingresa tu nombre para chatear con otros fanáticos:",
                    color = CosmicMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = {
                        Text(
                            text = "👤 Tu nombre...",
                            color = CosmicMuted
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicMuted.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CosmicPrimary
                    ),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        onUsernameSelected(username.trim())
                    }
                },
                enabled = username.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicPrimary
                ),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Rocket,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("¡Entrar!")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = CosmicMuted
                )
            ) {
                Text("Cancelar")
            }
        }
    )
}