package com.barriletecosmicotv.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriletecosmicotv.ui.theme.*

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    placeholder: String = "Buscar canales deportivos...",
    modifier: Modifier = Modifier
) {
    // SOLO ajustar en TV/Box; en celular queda igual
    val cfg = LocalConfiguration.current
    val isTv = (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val scale = if (isTv) 0.8f else 1f
    fun d(px: Int) = (px.dp * scale)
    fun s(px: Int) = (px.sp * scale)

    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Colores animados basados en el foco
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) CosmicPrimary else Color.White.copy(alpha = 0.3f),
        animationSpec = tween(300),
        label = "border_color"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused)
            Color.White.copy(alpha = 0.15f)
        else
            Color.White.copy(alpha = 0.1f),
        animationSpec = tween(300),
        label = "background_color"
    )

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = s(14)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = if (isFocused) CosmicPrimary else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(d(20))
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onSearchQueryChange("")
                        keyboardController?.hide()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar búsqueda",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(d(18))
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { keyboardController?.hide() }
        ),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            cursorColor = CosmicPrimary,
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor
        ),
        shape = RoundedCornerShape(d(25)),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState -> isFocused = focusState.isFocused }
    )
}

@Composable
fun SearchResultsHeader(
    totalResults: Int,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    // SOLO ajustar en TV/Box; en celular queda igual
    val cfg = LocalConfiguration.current
    val isTv = (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val scale = if (isTv) 0.8f else 1f
    fun d(px: Int) = (px.dp * scale)
    fun s(px: Int) = (px.sp * scale)

    if (searchQuery.isNotEmpty()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = d(16), vertical = d(8)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Resultados de búsqueda",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = s(14),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (totalResults == 0) {
                        "No se encontraron canales para \"$searchQuery\""
                    } else {
                        "$totalResults canal${if (totalResults != 1) "es" else ""} encontrado${if (totalResults != 1) "s" else ""}"
                    },
                    color = CosmicPrimary,
                    fontSize = s(12)
                )
            }

            if (totalResults > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicSecondary.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(d(12))
                ) {
                    Text(
                        text = totalResults.toString(),
                        color = Color.White,
                        fontSize = s(12),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = d(8), vertical = d(4))
                    )
                }
            }
        }
    }
}

@Composable
fun NoResultsFound(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    // SOLO ajustar en TV/Box; en celular queda igual
    val cfg = LocalConfiguration.current
    val isTv = (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val scale = if (isTv) 0.8f else 1f
    fun d(px: Int) = (px.dp * scale)
    fun s(px: Int) = (px.sp * scale)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(d(32)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d(16))
    ) {
        // Icono de búsqueda vacía
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Sin resultados",
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(d(64))
        )

        Text(
            text = "No se encontraron canales",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = s(18),
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "No hay canales que coincidan con \"$searchQuery\".\nIntenta con otro término de búsqueda.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = s(14),
            lineHeight = s(20)
        )

        Button(
            onClick = onClearSearch,
            colors = ButtonDefaults.buttonColors(
                containerColor = CosmicPrimary.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(d(20))
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.size(d(16))
            )
            Spacer(modifier = Modifier.width(d(8)))
            Text(
                text = "Limpiar búsqueda",
                fontSize = s(14),
                fontWeight = FontWeight.Medium
            )
        }
    }
}