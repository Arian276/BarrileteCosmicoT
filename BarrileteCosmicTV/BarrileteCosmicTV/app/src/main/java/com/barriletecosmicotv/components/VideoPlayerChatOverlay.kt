package com.barriletecosmicotv.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barriletecosmicotv.data.SimpleChatMessage
import com.barriletecosmicotv.ui.theme.*
import com.barriletecosmicotv.viewmodel.SimpleChatViewModel
import kotlinx.coroutines.launch

/**
 * CHAT OVERLAY PARA VIDEO PLAYER
 * Discreto, minimalista, no interrumpe la vista del video
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerChatOverlay(
    streamId: String,
    modifier: Modifier = Modifier,
    viewModel: SimpleChatViewModel = hiltViewModel(),
    onToggle: ((Boolean) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Estados del chat
    val chatMessages by viewModel.getChatMessages(streamId).collectAsState(initial = emptyList())
    val savedUsername by viewModel.username.collectAsState(initial = "")
    var currentMessage by rememberSaveable { mutableStateOf("") }
    var showUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    
    // Auto-scroll a mensajes nuevos
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty() && isExpanded) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    // Solicitar username si no existe
    LaunchedEffect(savedUsername) {
        if (savedUsername.isBlank()) {
            showUsernameDialog = true
        }
    }
    
    // CHAT COMPACTO Y DISCRETO
    Box(modifier = modifier) {
        Column {
            // HEADER minimalista - Solo para abrir/cerrar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        isExpanded = !isExpanded
                        onToggle?.invoke(isExpanded)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = CosmicPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chat (${chatMessages.size})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // CONTENIDO DEL CHAT - Solo cuando está expandido
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically() + fadeIn(tween(300)),
                exit = slideOutVertically() + fadeOut(tween(200))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp) // Compacto para no tapar video
                    ) {
                        // MENSAJES - Área compacta
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            if (chatMessages.isEmpty()) {
                                // Estado vacío minimalista
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Escribe el primer mensaje",
                                        color = CosmicMuted.copy(alpha = 0.5f),
                                        fontSize = 10.sp
                                    )
                                }
                            } else {
                                // Lista de mensajes súper compacta
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    items(
                                        items = chatMessages.filter { it.username.isNotBlank() },
                                        key = { it.id }
                                    ) { message ->
                                        VideoPlayerChatMessage(message = message)
                                    }
                                }
                            }
                        }
                        
                        Divider(
                            color = CosmicMuted.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                        
                        // INPUT super compacto
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentMessage,
                                onValueChange = { currentMessage = it },
                                placeholder = {
                                    Text(
                                        text = if (savedUsername.isNotBlank()) "Mensaje..." else "Usuario?",
                                        color = CosmicMuted.copy(alpha = 0.4f),
                                        fontSize = 10.sp
                                    )
                                },
                                enabled = savedUsername.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicPrimary.copy(alpha = 0.6f),
                                    unfocusedBorderColor = CosmicMuted.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                    cursorColor = CosmicPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 1,
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Botón enviar mini
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentMessage.isNotBlank()) CosmicPrimary.copy(alpha = 0.8f)
                                        else CosmicMuted.copy(alpha = 0.3f)
                                    )
                                    .clickable(enabled = currentMessage.isNotBlank() && savedUsername.isNotBlank()) {
                                        if (currentMessage.isNotBlank() && savedUsername.isNotBlank()) {
                                            viewModel.sendMessage(
                                                streamId = streamId,
                                                message = currentMessage,
                                                username = savedUsername
                                            )
                                            currentMessage = ""
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo para pedir username
    if (showUsernameDialog) {
        VideoPlayerUsernameDialog(
            onUsernameSet = { username ->
                viewModel.saveUsername(username)
                showUsernameDialog = false
            },
            onDismiss = { showUsernameDialog = false }
        )
    }
}

@Composable
private fun VideoPlayerChatMessage(message: SimpleChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Username compacto
        Text(
            text = "${message.username}:",
            color = when {
                message.username.contains("BarrileteCosmicoTv", ignoreCase = true) -> Color.Red
                message.username.contains("admin", ignoreCase = true) -> CosmicSecondary
                else -> CosmicPrimary
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 3.dp)
        )
        
        // Mensaje compacto
        Text(
            text = message.message,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 9.sp,
            lineHeight = 11.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VideoPlayerUsernameDialog(
    onUsernameSet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chat en video",
                color = Color.White,
                fontSize = 14.sp
            )
        },
        text = {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = {
                    Text(
                        text = "Tu nombre",
                        color = CosmicMuted,
                        fontSize = 12.sp
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
                    text = "OK",
                    color = CosmicPrimary,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = CosmicMuted,
                    fontSize = 12.sp
                )
            }
        },
        containerColor = Color.Black.copy(alpha = 0.95f)
    )
}