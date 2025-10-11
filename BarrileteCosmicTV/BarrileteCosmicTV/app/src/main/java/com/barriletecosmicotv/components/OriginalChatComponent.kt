package com.barriletecosmicotv.components

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
 * CHAT ORIGINAL DE STREAMSCREEN - Simple y funcional como estaba antes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginalChatComponent(
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
            containerColor = Color.Black.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header simple del chat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = CosmicPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Chat en vivo",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    // Contador de mensajes
                    if (chatMessages.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${chatMessages.size})",
                            color = CosmicMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // Área de mensajes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = CosmicMuted.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "¡Sé el primero en comentar!",
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = chatMessages.filter { it.username.isNotBlank() },
                            key = { it.id }
                        ) { message ->
                            OriginalChatMessageItem(message = message)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Área de entrada de mensajes original
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = currentMessage,
                    onValueChange = { currentMessage = it },
                    placeholder = {
                        Text(
                            text = if (savedUsername.isNotBlank()) "Escribe un mensaje..." else "Ingresa tu nombre primero",
                            color = CosmicMuted.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    },
                    enabled = savedUsername.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicMuted.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        disabledTextColor = CosmicMuted.copy(alpha = 0.5f),
                        cursorColor = CosmicPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 2
                )
                
                // Botón enviar original
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
                        .size(48.dp)
                        .background(
                            if (currentMessage.isNotBlank()) CosmicPrimary else CosmicMuted.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar mensaje",
                        tint = if (currentMessage.isNotBlank()) Color.White else CosmicMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    
    // Diálogo para pedir username
    if (showUsernameDialog) {
        OriginalUsernameDialog(
            onUsernameSet = { username ->
                viewModel.saveUsername(username)
                showUsernameDialog = false
            },
            onDismiss = { showUsernameDialog = false }
        )
    }
}

@Composable
private fun OriginalChatMessageItem(message: SimpleChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Username con color especial
            Text(
                text = "${message.username}:",
                color = when {
                    message.username.contains("BarrileteCosmicoTv", ignoreCase = true) -> Color.Red
                    message.username.contains("admin", ignoreCase = true) -> CosmicSecondary
                    else -> CosmicPrimary
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(end = 6.dp)
            )
            
            // Mensaje
            Text(
                text = message.message,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OriginalUsernameDialog(
    onUsernameSet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Únete al chat",
                color = Color.White,
                fontSize = 16.sp
            )
        },
        text = {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = {
                    Text(
                        text = "Tu nombre",
                        color = CosmicMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicMuted.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                    cursorColor = CosmicPrimary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (username.isNotBlank()) {
                        onUsernameSet(username.trim())
                    }
                },
                enabled = username.isNotBlank()
            ) {
                Text(
                    text = "Entrar",
                    color = CosmicPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = CosmicMuted
                )
            }
        },
        containerColor = Color.Black.copy(alpha = 0.95f)
    )
}