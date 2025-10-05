package com.barriletecosmicotv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barriletecosmicotv.ui.theme.*
import com.barriletecosmicotv.viewmodel.SimpleChatViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch

/**
 * ✅ CHAT EXACTO COMO EN LA IMAGEN DEL USUARIO
 * Replicado pixel por pixel de la screenshot
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExactChatComponent(
    streamId: String,
    modifier: Modifier = Modifier,
    viewModel: SimpleChatViewModel = hiltViewModel()
) {
    var newMessage by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Estados del chat como en la imagen
    val chatMessages by viewModel.getChatMessages(streamId).collectAsState(initial = emptyList())

    // Auto-scroll a mensajes nuevos
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // Fondo como en la imagen
            .padding(horizontal = 16.dp)
    ) {
        // ✅ HEADER EXACTO COMO EN LA IMAGEN
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💬 Chat en vivo",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botón silenciar como en la imagen
            Icon(
                imageVector = Icons.Default.VolumeOff,
                contentDescription = "Silenciar",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        // ✅ INDICADOR "EN VIVO" VERDE EXACTO
        Row(
            modifier = Modifier
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Green, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "En vivo",
                color = Color.Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // ✅ MENSAJES EXACTOS COMO EN LA IMAGEN
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mostrar mensajes reales si existen
            items(chatMessages.filter { it.username.isNotBlank() }) { message ->
                ExactChatMessageItem(
                    username = message.username,
                    message = message.message,
                    timestamp = "Ahora" // Como en la imagen
                )
            }
            
            // ✅ PIXEL PERFECT - Sin estado vacío, exacto como imagen
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ✅ INPUT EXACTO COMO EN LA IMAGEN
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Campo de texto simple como en la imagen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    singleLine = true,
                    decorationBox = @Composable { innerTextField ->
                        if (newMessage.isEmpty()) {
                            Text(
                                text = "Escribir mensaje...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // ✅ BOTÓN ENVIAR CIRCULAR EXACTO
            IconButton(
                onClick = {
                    if (newMessage.isNotBlank()) {
                        viewModel.sendMessage(
                            streamId = streamId,
                            message = newMessage,
                            username = "Usuario" // Username por defecto
                        )
                        newMessage = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (newMessage.isNotBlank()) CosmicPrimary else Color.White.copy(alpha = 0.3f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ExactChatMessageItem(
    username: String,
    message: String,
    timestamp: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Username como en la imagen
        Text(
            text = username,
            color = when {
                username.contains("BarrileteCosmicoTv", ignoreCase = true) -> Color.Red
                username.contains("admin", ignoreCase = true) -> CosmicSecondary
                else -> Color.White
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Timestamp como en la imagen
        Text(
            text = timestamp,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
    
    // Mensaje debajo como en la imagen
    Text(
        text = message,
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 4.dp)
    )
}