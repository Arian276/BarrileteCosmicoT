package com.barriletecosmicotv.screens

import androidx.compose.ui.unit.TextUnit
import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.barriletecosmicotv.components.AnimatedSoccerButton
import com.barriletecosmicotv.components.ViewerCount
import com.barriletecosmicotv.components.SearchBar
import com.barriletecosmicotv.components.SearchResultsHeader
import com.barriletecosmicotv.components.NoResultsFound
import com.barriletecosmicotv.model.Stream
import com.barriletecosmicotv.model.Category
import com.barriletecosmicotv.ui.theme.*
import com.barriletecosmicotv.viewmodel.StreamViewModel

// Solo usar canales del backend - sin datos hardcodeados
private val channelLogos = emptyMap<String, String>()

private fun getChannelLogo(channel: Stream): String {
    return channelLogos[channel.id] ?: channel.thumbnailUrl.ifEmpty {
        "https://via.placeholder.com/150x80/1976D2/FFFFFF?text=${channel.title.replace(" ", "+")}"
    }
}

@Composable
fun HomeScreen(
    navController: NavController? = null,
    onStreamClick: ((String) -> Unit)? = null,
    viewModel: StreamViewModel = hiltViewModel()
) {
    val streams by viewModel.streams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("todas") }
    val categories by viewModel.categories.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    if (useRailLayout) {
        Row(Modifier.fillMaxSize()) {
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
                    gridMinCell = d(280),
                    outerPadding = d(16),
                    itemSpacing = d(16),
                    headerPadding = d(16),
                    cardImageHeight = d(100),
                    titleSize = s(16),
                    chipTextSize = s(10),
                    liveBadgeTextSize = s(11)
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
            Box(modifier = Modifier.fillMaxSize()) {
                MainContent(
                    streams = filteredStreams,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }, // <-- CORRECCIÓN
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
                    onToggleFavorite = { streamId -> viewModel.toggleFavorite(streamId) }
                )
            }
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
    onToggleFavorite: (String) -> Unit, // <-- CORRECCIÓN: debe recibir streamId
    gridMinCell: Dp = 280.dp,
    outerPadding: Dp = 16.dp,
    itemSpacing: Dp = 16.dp,
    headerPadding: Dp = 16.dp,
    cardImageHeight: Dp = 100.dp,
    titleSize: TextUnit = 16.sp,
    chipTextSize: TextUnit = 10.sp,
    liveBadgeTextSize: TextUnit = 11.sp
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
            logoIconSize = if (titleSize.value < 16f) 22.dp else 24.dp
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
                        animationDelay = (index * 100).coerceAtMost(500),
                        onWatchClick = { onStreamClick(stream.id) },
                        isFavorite = isFavorite(stream.id),
                        onToggleFavorite = { onToggleFavorite(stream.id) },
                        imageHeight = cardImageHeight,
                        titleSize = titleSize,
                        chipTextSize = chipTextSize,
                        liveBadgeTextSize = liveBadgeTextSize,
                        cardInnerPadding = 16.dp
                    )
                }
            }
        }
    }
}
@Composable
private fun ArgentinaHeader(
    headerPadding: Dp = 16.dp,
    titleSize: TextUnit = 24.sp,
    subtitleSize: TextUnit = 12.sp,
    logoBoxSize: Dp = 48.dp,
    logoIconSize: Dp = 24.dp
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveBadge(textSize = 10.sp)
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
        modifier = Modifier
            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
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
    imageHeight: Dp = 100.dp,
    titleSize: TextUnit = 16.sp,
    chipTextSize: TextUnit = 10.sp,
    liveBadgeTextSize: TextUnit = 11.sp,
    cardInnerPadding: Dp = 16.dp
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
                        color = if (isHovered) CosmicPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
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
                            color = CosmicSecondary.copy(alpha = 0.1f),
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ViewerCount(streamId = stream.id)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                            tint = if (isFavorite) CosmicSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            scaleX = if (isHovered) 1.1f else 1f
                            scaleY = if (isHovered) 1.1f else 1f
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

            Spacer(modifier = Modifier.height(16.dp))

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

            // <- REEMPLAZADO: ahora es scrollable usando LazyColumn
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
                        count = categories.filter { it.name.lowercase() !in setOf("favorites","favoritos") }.sumOf { it.count } + favoritesCount,
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