package com.barriletecosmicotv.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.barriletecosmicotv.components.AnimatedSoccerButton
import com.barriletecosmicotv.components.NoResultsFound
import com.barriletecosmicotv.components.SearchBar
import com.barriletecosmicotv.components.SearchResultsHeader
import com.barriletecosmicotv.model.Category
import com.barriletecosmicotv.model.Stream
import com.barriletecosmicotv.ui.theme.CosmicPrimary
import com.barriletecosmicotv.ui.theme.CosmicSecondary
import com.barriletecosmicotv.ui.theme.LiveIndicator
import com.barriletecosmicotv.viewmodel.StreamViewModel
import com.barriletecosmicotv.viewmodel.UserViewModel
import com.barriletecosmicotv.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

// Solo usar canales del backend - sin datos hardcodeados
private val channelLogos = emptyMap<String, String>()

private fun getChannelLogo(channel: Stream): String {
    return channelLogos[channel.id] ?: channel.thumbnailUrl.ifEmpty {
        "https://via.placeholder.com/150x80/1976D2/FFFFFF?text=${channel.title.replace(" ", "+")}"
    }
}

// Helper: obtener usuario desde token "fake-token-for-<user>-<ts>"
private fun usernameFromToken(token: String?): String? {
    if (token.isNullOrBlank()) return null
    val re = Regex("^fake-token-for-(.+?)-\\d+$")
    return re.find(token.trim())?.groupValues?.getOrNull(1)
}

@Composable
fun HomeScreen(
    navController: NavController? = null,
    onStreamClick: ((String) -> Unit)? = null,
    viewModel: StreamViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val streams by viewModel.streams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("todas") } // persistir selección
    val categories by viewModel.categories.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ==== User VM states (para Cambiar contraseña) ====
    val currentPassword by userViewModel.currentPassword.collectAsState()
    val newPassword by userViewModel.newPassword.collectAsState()
    val confirmPassword by userViewModel.confirmPassword.collectAsState()
    val statusMessage by userViewModel.statusMessage.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar mensajes de resultado del cambio de contraseña
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            userViewModel.clearStatus()
        }
    }

    val filteredStreams = remember(streams, searchQuery, selectedCategory, favorites) {
        val sel = selectedCategory.trim().lowercase()
        val favoritesIds = favorites.toSet()

        var base = when {
            sel == "favorites" || sel == "favoritos" -> streams.filter { favoritesIds.contains(it.id) }
            sel == "todas" -> streams
            else -> streams.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        if (searchQuery.isNotEmpty()) {
            base = base.filter {
                it.title.contains(searchQuery, true) ||
                        it.description.contains(searchQuery, true) ||
                        it.category.contains(searchQuery, true)
            }
        }
        base
    }

    LaunchedEffect(Unit) {
        viewModel.loadStreams()
        viewModel.loadCategories()
        drawerState.close()
    }

    val cfg = LocalConfiguration.current
    val isTv = (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION
    val isWide = cfg.smallestScreenWidthDp >= 600 || cfg.screenWidthDp >= 600
    val useRailLayout = isTv || isWide

    val scale = if (isTv) 0.8f else 1f
    fun d(px: Int): Dp = (px.dp * scale)
    fun s(px: Int): TextUnit = (px.sp * scale)

    if (isLoading && streams.isEmpty()) {
        LoadingScreen()
        return
    }

    // Estado para mostrar el diálogo de Cambiar contraseña
    var showChangePassword by remember { mutableStateOf(false) }

    // Handler botón "Descargar última versión"
    fun handleDownloadLatest() {
        val mediafireUrl = "https://www.mediafire.com/folder/4q7eatf6ajvq0/BarrileteCosmico"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mediafireUrl))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            scope.launch { snackbarHostState.showSnackbar("No se pudo abrir el enlace.") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (useRailLayout) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CategoryRail(
                    categories = categories,
                    favoritesCount = favorites.size,
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it }
                )
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(d(1)),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Box(Modifier.weight(1f)) {
                    MainContent(
                        streams = filteredStreams,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        isRefreshing = isLoading,
                        onRefresh = {
                            viewModel.loadStreams()
                            viewModel.loadCategories()
                        },
                        onStreamClick = { id ->
                            onStreamClick?.invoke(id)
                            navController?.navigate("stream/$id")
                        },
                        isFavorite = { streamId -> favorites.contains(streamId) },
                        onToggleFavorite = { streamId -> viewModel.toggleFavorite(streamId) },
                        gridMinCell = d(180),               // más chicas
                        outerPadding = d(12),
                        itemSpacing = d(10),
                        headerPadding = d(12),
                        cardImageHeight = d(88),
                        titleSize = s(14),
                        chipTextSize = s(9),
                        liveBadgeTextSize = s(10),
                        headerActions = {
                            ArgentinaHeaderActions(
                                onDownloadLatest = { handleDownloadLatest() },
                                onChangePassword = { showChangePassword = true },
                                onLogout = { navController?.navigate("login") }
                            )
                        }
                    )
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                drawerContent = {
                    CategoryDrawerContent(
                        categories = categories,
                        favoritesCount = favorites.size,
                        selectedCategory = selectedCategory,
                        onCategoryChange = {
                            selectedCategory = it
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    MainContent(
                        streams = filteredStreams,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        isRefreshing = isLoading,
                        onRefresh = {
                            viewModel.loadStreams()
                            viewModel.loadCategories()
                        },
                        onStreamClick = { id ->
                            onStreamClick?.invoke(id)
                            navController?.navigate("stream/$id")
                        },
                        isFavorite = { streamId -> favorites.contains(streamId) },
                        onToggleFavorite = { streamId -> viewModel.toggleFavorite(streamId) },
                        gridMinCell = d(180),
                        outerPadding = d(12),
                        itemSpacing = d(10),
                        headerPadding = d(12),
                        cardImageHeight = d(88),
                        titleSize = s(14),
                        chipTextSize = s(9),
                        liveBadgeTextSize = s(10),
                        headerActions = {
                            ArgentinaHeaderActions(
                                onDownloadLatest = { handleDownloadLatest() },
                                onChangePassword = { showChangePassword = true },
                                onLogout = { navController?.navigate("login") }
                            )
                        }
                    )
                }
            }
        }

        // Diálogo Cambiar contraseña
        if (showChangePassword) {
            AlertDialog(
                onDismissRequest = {
                    showChangePassword = false
                    userViewModel.clearStatus()
                },
                icon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                title = { Text("Cambiar contraseña") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = userViewModel::onCurrentPasswordChange,
                            label = { Text("Contraseña actual") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = userViewModel::onNewPasswordChange,
                            label = { Text("Nueva contraseña") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = userViewModel::onConfirmPasswordChange,
                            label = { Text("Confirmar nueva") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newPassword.isNotBlank() && newPassword == confirmPassword) {
                                userViewModel.changePassword()
                                showChangePassword = false
                            }
                        }
                    ) { Text("Cambiar") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showChangePassword = false
                        userViewModel.clearStatus()
                    }) { Text("Cancelar") }
                }
            )
        }

        statusMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { userViewModel.clearStatus() },
                title = { Text("Cambio de contraseña") },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { userViewModel.clearStatus() }) { Text("OK") }
                }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "rotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = CosmicPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
            Text(
                text = "Cargando canales...",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
@Composable
private fun MainContent(
    streams: List<Stream>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onStreamClick: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit,
    gridMinCell: Dp = 180.dp,
    outerPadding: Dp = 12.dp,
    itemSpacing: Dp = 10.dp,
    headerPadding: Dp = 12.dp,
    cardImageHeight: Dp = 88.dp,
    titleSize: TextUnit = 14.sp,
    chipTextSize: TextUnit = 9.sp,
    liveBadgeTextSize: TextUnit = 10.sp,
    headerActions: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ArgentinaHeader(
            headerPadding = headerPadding,
            titleSize = if (titleSize.value < 16f) 22.sp else 24.sp,
            subtitleSize = if (titleSize.value < 16f) 11.sp else 12.sp,
            logoBoxSize = if (titleSize.value < 16f) 44.dp else 48.dp,
            logoIconSize = if (titleSize.value < 16f) 22.dp else 24.dp,
            trailingContent = headerActions
        )

        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            placeholder = "Buscar canales deportivos...",
            modifier = Modifier.padding(horizontal = headerPadding, vertical = 8.dp)
        )

        if (searchQuery.isNotEmpty()) {
            SearchResultsHeader(
                totalResults = streams.size,
                searchQuery = searchQuery
            )
        }

        if (isRefreshing && streams.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = headerPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = CosmicPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Actualizando canales...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (streams.isEmpty() && searchQuery.isNotEmpty()) {
            NoResultsFound(
                searchQuery = searchQuery,
                onClearSearch = { onSearchQueryChange("") }
            )
        } else if (streams.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No hay canales disponibles",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Desliza hacia abajo para actualizar",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(gridMinCell),
                contentPadding = PaddingValues(outerPadding),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                itemsIndexed(streams) { index, stream ->
                    ChannelCard(
                        stream = stream,
                        animationDelay = (index * 90).coerceAtMost(450),
                        onWatchClick = { onStreamClick(stream.id) },
                        isFavorite = isFavorite(stream.id),
                        onToggleFavorite = { onToggleFavorite(stream.id) },
                        imageHeight = cardImageHeight,
                        titleSize = titleSize,
                        chipTextSize = chipTextSize,
                        liveBadgeTextSize = liveBadgeTextSize,
                        cardInnerPadding = 12.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ArgentinaHeader(
    headerPadding: Dp = 12.dp,
    titleSize: TextUnit = 24.sp,
    subtitleSize: TextUnit = 12.sp,
    logoBoxSize: Dp = 48.dp,
    logoIconSize: Dp = 24.dp,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(headerPadding),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "logoSpin")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "logoRotation"
                )

                Box(
                    modifier = Modifier
                        .size(logoBoxSize)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CosmicPrimary, CosmicSecondary)
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(logoIconSize)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }

                Column {
                    Text(
                        text = "Barrilete Cósmico",
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        color = CosmicPrimary
                    )
                    Text(
                        text = "Fútbol argentino • Pasión • En vivo",
                        fontSize = subtitleSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                LiveBadge(textSize = 10.sp)
            }
        }
    }
}

/** Menú de acciones del header */
@Composable
private fun ArgentinaHeaderActions(
    onDownloadLatest: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Estado / deps para el contador
    var daysRemaining by remember { mutableStateOf<Int?>(null) }
    val appContext = LocalContext.current.applicationContext
    val sessionManager = remember { SessionManager(appContext) }
    val vm: UserViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()

    // Observa expiresAt en DataStore (cuando cambie, recalculamos)
    val expiresAtFlow = remember { sessionManager.getExpiresAt() }
    val expiresAt by expiresAtFlow.collectAsState(initial = null)

    // Helper: recalcular días (cálculo local a partir de expiresAt)
    suspend fun recalc() {
        val d = withContext(kotlinx.coroutines.Dispatchers.IO) { sessionManager.getDaysRemaining() }
        daysRemaining = if (d >= 0) d else null
    }

    // 0) Primer cálculo al montar
    LaunchedEffect(Unit) { recalc() }

    // 1) Recalcular cuando cambie expiresAt en DataStore
    LaunchedEffect(expiresAt) { recalc() }

    // 2) Ticker PERMANENTE cada 30s — llama backend + recalcula
    LaunchedEffect(true) {
        while (true) {
            withContext(Dispatchers.IO) { vm.refreshSubscription() } // 🔄 trae expiresAt fresco del backend
            recalc()
            kotlinx.coroutines.delay(30_000)
        }
    }

    // 3) Al abrir el menú, forzar refresh backend inmediato + recalc
    LaunchedEffect(expanded) {
        if (expanded) {
            withContext(Dispatchers.IO) { vm.refreshSubscription() }
            recalc()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        LiveBadge(textSize = 10.sp)
        Spacer(modifier = Modifier.width(8.dp))

        Box {
            IconButton(onClick = { expanded = !expanded }) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF00B4DB), Color(0xFF0083B0))),
                            CircleShape
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Perfil",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(26.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    scope.launch { recalc() }
                }
            ) {
                // Counter xx/30
                daysRemaining?.let { d ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Días restantes: $d/30",
                                fontWeight = FontWeight.Bold,
                                color = if (d <= 3) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = { /* informativo */ }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Descargar última versión") },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                    onClick = { expanded = false; onDownloadLatest() }
                )
                DropdownMenuItem(
                    text = { Text("Cambiar contraseña") },
                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                    onClick = { expanded = false; onChangePassword() }
                )
                DropdownMenuItem(
                    text = { Text("Cerrar sesión") },
                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                    onClick = { expanded = false; onLogout() }
                )
            }
        }
    }
}

@Composable
private fun LiveBadge(textSize: TextUnit = 10.sp) {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
        color = LiveIndicator,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NetworkCheck,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "VIVO",
                color = Color.White,
                fontSize = textSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChannelCard(
    stream: Stream,
    animationDelay: Int,
    onWatchClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    imageHeight: Dp = 88.dp,
    titleSize: TextUnit = 14.sp,
    chipTextSize: TextUnit = 9.sp,
    liveBadgeTextSize: TextUnit = 10.sp,
    cardInnerPadding: Dp = 12.dp
) {
    var isHovered by remember { mutableStateOf(false) }

    val animationSpec = tween<Float>(
        durationMillis = 600,
        delayMillis = animationDelay,
        easing = FastOutSlowInEasing
    )
    val alpha by animateFloatAsState(targetValue = 1f, animationSpec = animationSpec, label = "alpha")
    val translateY by animateFloatAsState(targetValue = 0f, animationSpec = animationSpec, label = "translateY")
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "hoverScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translateY * 50f
                this.scaleX = scale
                this.scaleY = scale
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(cardInnerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stream.title,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isHovered) CosmicPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stream.category,
                                fontSize = chipTextSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = CosmicSecondary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "HD",
                                fontSize = chipTextSize,
                                color = CosmicSecondary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
// Solo favorito (se eliminó el contador de visitas)
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (isFavorite) CosmicSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = getChannelLogo(stream),
                    contentDescription = stream.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if (isHovered) 1.08f else 1f
                            scaleY = if (isHovered) 1.08f else 1f
                        },
                    contentScale = ContentScale.Fit
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 8.dp, y = 8.dp),
                    color = Color(0xFFD32F2F),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "EN VIVO",
                        fontSize = liveBadgeTextSize,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedSoccerButton(
                streamId = stream.id,
                onWatchClick = onWatchClick
            )
        }
    }
}

@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    favoritesCount: Int = 0
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = if (selectedCategory == "todas") "Todas las Categorías"
    else categories.find { it.name == selectedCategory }?.displayName ?: selectedCategory

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = CosmicPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayName,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Expandir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Todas las Categorías",
                            color = if (selectedCategory == "todas") CosmicPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                onClick = {
                    onCategoryChange("todas")
                    expanded = false
                }
            )
            categories.forEach { category ->
                val isFav = category.name.lowercase() in setOf("favorites", "favoritos")
                val countToShow = if (isFav) favoritesCount else category.count
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = category.displayName,
                                color = if (selectedCategory == category.name) CosmicPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = countToShow.toString(),
                                color = CosmicSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    onClick = {
                        onCategoryChange(category.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryDrawerContent(
    categories: List<Category>,
    favoritesCount: Int,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.75f),
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = CosmicPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Categorías",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicPrimary
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    CategoryDrawerItem(
                        text = "Todas las Categorías",
                        count = categories.filter { it.name.lowercase() !in setOf("favorites","favoritos") }
                            .sumOf { it.count } + favoritesCount,
                        isSelected = selectedCategory == "todas",
                        onClick = { onCategoryChange("todas") }
                    )
                }

                items(categories) { category ->
                    val isFav = category.name.lowercase() in setOf("favorites", "favoritos")
                    CategoryDrawerItem(
                        text = category.displayName,
                        count = if (isFav) favoritesCount else category.count,
                        isSelected = selectedCategory == category.name,
                        onClick = { onCategoryChange(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "BarrileteCósmico TV",
                fontSize = 14.sp,
                color = CosmicPrimary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun CategoryRail(
    categories: List<Category>,
    favoritesCount: Int,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    railWidth: Dp = 88.dp
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .width(railWidth),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationRailItem(
            selected = selectedCategory == "todas",
            onClick = { onCategoryChange("todas") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Todas") },
            label = { Text("Todas") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        categories.forEach { cat ->
            val isFav = cat.name.lowercase() in setOf("favorites", "favoritos")
            NavigationRailItem(
                selected = selectedCategory == cat.name,
                onClick = { onCategoryChange(cat.name) },
                icon = { Icon(Icons.Default.Category, contentDescription = cat.displayName) },
                label = {
                    Text(if (isFav) "${cat.displayName} ($favoritesCount)" else cat.displayName)
                }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CategoryDrawerItem(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) CosmicPrimary.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) CosmicPrimary else MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = if (isSelected) CosmicPrimary else CosmicSecondary.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}