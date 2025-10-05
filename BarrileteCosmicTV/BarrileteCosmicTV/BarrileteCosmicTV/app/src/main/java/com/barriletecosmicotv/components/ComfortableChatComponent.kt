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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barriletecosmicotv.data.SimpleChatMessage
import com.barriletecosmicotv.ui.theme.*
import com.barriletecosmicotv.viewmodel.SimpleChatViewModel
import kotlinx.coroutines.launch

/**
 * CHAT SÚPER CÓMODO - Se abre/cierra fácil, diseño limpio
 * Funciona perfectamente dentro del video player
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfortableChatComponent(
    streamId: String,
    modifier: Modifier = Modifier,
    viewModel: SimpleChatViewModel = hiltViewModel(),
    isCompact: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Estados del chat
    val chatMessages by viewModel.getChatMessages(streamId).collectAsState(initial = emptyList())
    val savedUsername by viewModel.username.collectAsState(initial = "")
    var currentMessage by rememberSaveable { mutableStateOf("") }
    var showUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(!isCompact) }
    
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
    
    Box(modifier = modifier) {
        Column {
            // HEADER - Siempre visible y clickeable para abrir/cerrar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        isExpanded = !isExpanded
                        onToggle?.invoke(isExpanded)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = CosmicPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chat (${chatMessages.size})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // CONTENIDO DEL CHAT - Solo cuando está expandido
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp) // Altura fija cómoda
                    ) {
                        // MENSAJES
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            if (chatMessages.isEmpty()) {
                                // Estado vacío
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "¡Sé el primero en comentar!",
                                        color = CosmicMuted.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                // Lista de mensajes
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    items(
                                        items = chatMessages.filter { it.username.isNotBlank() },
                                        key = { it.id }
                                    ) { message ->
                                        CompactChatMessage(message = message)
                                    }
                                }
                            }
                        }
                        
                        Divider(
                            color = CosmicMuted.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                        
                        // INPUT DE MENSAJE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentMessage,
                                onValueChange = { currentMessage = it },
                                placeholder = {
                                    Text(
                                        text = if (savedUsername.isNotBlank()) "Escribe..." else "Nombre primero",
                                        color = CosmicMuted.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                },
                                enabled = savedUsername.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicPrimary,
                                    unfocusedBorderColor = CosmicMuted.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                    cursorColor = CosmicPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 1,
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Botón enviar
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentMessage.isNotBlank()) CosmicPrimary 
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
                                    tint = if (currentMessage.isNotBlank()) Color.White else CosmicMuted,
                                    modifier = Modifier.size(14.dp)
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
        UsernameDialog(
            onUsernameSet = { username ->
                viewModel.saveUsername(username)
                showUsernameDialog = false
            },
            onDismiss = { showUsernameDialog = false }
        )
    }
}

@Composable
private fun CompactChatMessage(message: SimpleChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
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
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 4.dp)
        )
        
        // Mensaje
        Text(
            text = message.message,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UsernameDialog(
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