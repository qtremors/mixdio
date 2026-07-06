package dev.qtremors.melatune.feature.player.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.qtremors.melatune.core.storage.Playlist
import dev.qtremors.melatune.core.storage.Song
import dev.qtremors.melatune.core.ui.components.ExpressiveButton
import dev.qtremors.melatune.core.ui.components.ExpressiveButtonSize
import dev.qtremors.melatune.core.ui.components.ExpressiveButtonType
import dev.qtremors.melatune.core.ui.components.GroupPosition
import dev.qtremors.melatune.core.ui.components.getGroupShape
import dev.qtremors.melatune.core.ui.theme.FontAxes
import dev.qtremors.melatune.feature.player.components.MiniPlayer
import dev.qtremors.melatune.feature.player.components.SongRowItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class, androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun MainScreen(
    songs: List<Song>,
    playlists: List<Playlist>,
    playlistSongsMap: Map<Long, List<Song>>,
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    onSongSelect: (Song, List<Song>) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onPlaylistCreate: (String) -> Unit,
    onPlaylistDelete: (Playlist) -> Unit,
    onAddSongToPlaylist: (Playlist, Song) -> Unit,
    onMiniPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Playlists", "Albums", "Folders", "Favorites")

    var searchQuery by remember { mutableStateOf("") }
    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val tracksListState = rememberLazyListState()
    val scrollOffset = remember { derivedStateOf { tracksListState.firstVisibleItemScrollOffset } }
    val firstVisibleItem = remember { derivedStateOf { tracksListState.firstVisibleItemIndex } }
    
    val collapsedFraction by remember {
        derivedStateOf {
            if (firstVisibleItem.value > 0) 1f
            else (scrollOffset.value.toFloat() / 250f).coerceIn(0f, 1f)
        }
    }

    val titleWeight = lerp(950f, 600f, collapsedFraction)
    val titleWidth = lerp(85f, 110f, collapsedFraction)
    val titleRoundness = lerp(100f, 30f, collapsedFraction)

    val customFontFamily = remember(titleWeight, titleWidth, titleRoundness) {
        val axes = FontAxes(titleWeight, titleWidth, 32f, 0f, 0f, titleRoundness)
        androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(
                resId = dev.qtremors.melatune.core.ui.R.font.google_sans_flex_variable,
                variationSettings = axes.toVariationSettings()
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MELATUNE",
                        fontFamily = customFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 22.sp,
                        letterSpacing = 2.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    if (selectedTab == 0 && selectedPlaylistForDetail == null) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search tracks...", fontSize = 12.sp) },
                            modifier = Modifier
                                .width(190.dp)
                                .padding(end = 8.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp)) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentSong != null) {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick,
                        onClick = onMiniPlayerClick
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        modifier = Modifier
                            .animateContentSize()
                            .height(68.dp),
                        shape = RoundedCornerShape(100),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val icon = when (title) {
                                "Songs" -> Icons.Default.MusicNote
                                "Playlists" -> Icons.AutoMirrored.Filled.QueueMusic
                                "Albums" -> Icons.Default.Album
                                "Folders" -> Icons.Default.Folder
                                else -> Icons.Default.Favorite
                            }
                            val selected = selectedTab == index && selectedPlaylistForDetail == null
                            ShortNavigationBarItem(
                                selected = selected,
                                onClick = {
                                    selectedTab = index
                                    selectedPlaylistForDetail = null
                                },
                                icon = {
                                    val extraHeight by animateDpAsState(
                                        targetValue = if (selected) 8.dp else 0.dp,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "indicatorHeight"
                                    )
                                    Box(
                                        modifier = Modifier.height(24.dp + extraHeight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = {
                                    AnimatedVisibility(
                                        visible = selected,
                                        enter = fadeIn() + slideInHorizontally(
                                            initialOffsetX = { -10 },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) + expandHorizontally(expandFrom = Alignment.Start),
                                        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -10 }) + shrinkHorizontally(shrinkTowards = Alignment.Start)
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                                        )
                                    }
                                },
                                iconPosition = NavigationItemIconPosition.Start,
                                colors = ShortNavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                selectedPlaylistForDetail != null -> {
                    val playlist = selectedPlaylistForDetail!!
                    val songsInPlaylist = playlistSongsMap[playlist.id] ?: emptyList()
                    PlaylistDetailView(
                        playlist = playlist,
                        songs = songsInPlaylist,
                        onSongSelect = { song -> onSongSelect(song, songsInPlaylist) },
                        onFavoriteToggle = onFavoriteToggle,
                        onBack = { selectedPlaylistForDetail = null },
                        onDeletePlaylist = {
                            onPlaylistDelete(playlist)
                            selectedPlaylistForDetail = null
                        }
                    )
                }
                else -> {
                    when (selectedTab) {
                        0 -> {
                            if (filteredSongs.isEmpty()) {
                                EmptyStateView(message = "No songs found")
                            } else {
                                LazyColumn(
                                    state = tracksListState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(filteredSongs) { index, song ->
                                        val position = when {
                                            filteredSongs.size == 1 -> GroupPosition.Single
                                            index == 0 -> GroupPosition.Top
                                            index == filteredSongs.size - 1 -> GroupPosition.Bottom
                                            else -> GroupPosition.Middle
                                        }
                                        SongRowItem(
                                            song = song,
                                            onClick = { onSongSelect(song, filteredSongs) },
                                            onFavoriteClick = { onFavoriteToggle(song) },
                                            isCurrent = song.id == currentSong?.id,
                                            position = position
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                    ExpressiveButton(
                                        onClick = { showCreatePlaylistDialog = true },
                                        size = ExpressiveButtonSize.Medium,
                                        type = ExpressiveButtonType.Filled,
                                        text = "Create",
                                        icon = Icons.Default.Add
                                    )
                                }

                                if (playlists.isEmpty()) {
                                    EmptyStateView(message = "Create your first playlist")
                                } else {
                                    LazyColumn {
                                        itemsIndexed(playlists) { index, playlist ->
                                            val songCount = playlistSongsMap[playlist.id]?.size ?: 0
                                            val position = when {
                                                playlists.size == 1 -> GroupPosition.Single
                                                index == 0 -> GroupPosition.Top
                                                index == playlists.size - 1 -> GroupPosition.Bottom
                                                else -> GroupPosition.Middle
                                            }
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                                                    .clickable { selectedPlaylistForDetail = playlist },
                                                shape = getGroupShape(position),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(36.dp)
                                                        )
                                                        Spacer(Modifier.width(16.dp))
                                                        Column {
                                                            Text(playlist.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                            Text("$songCount tracks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            val albums = remember(songs) {
                                songs.groupBy { it.albumId }.values.map { it.first() }
                            }
                            if (albums.isEmpty()) {
                                EmptyStateView(message = "No albums discovered")
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    items(albums) { albumSong ->
                                        val albumSongs = songs.filter { it.albumId == albumSong.albumId }
                                        Card(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .clickable {
                                                    if (albumSongs.isNotEmpty()) {
                                                        onSongSelect(albumSongs.first(), albumSongs)
                                                    }
                                                },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = albumSong.albumArtUri,
                                                    contentDescription = albumSong.album,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = albumSong.album,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.basicMarquee()
                                                    )
                                                    Text(
                                                        text = albumSong.artist,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.basicMarquee()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            val folders = remember(songs) {
                                songs.groupBy { it.folderName }
                            }
                            if (folders.isEmpty()) {
                                EmptyStateView(message = "No folder groups found")
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    val folderList = folders.keys.toList()
                                    itemsIndexed(folderList) { index, folderName ->
                                        val folderSongs = folders[folderName] ?: emptyList()
                                        val position = when {
                                            folderList.size == 1 -> GroupPosition.Single
                                            index == 0 -> GroupPosition.Top
                                            index == folderList.size - 1 -> GroupPosition.Bottom
                                            else -> GroupPosition.Middle
                                        }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                                .clickable {
                                                    if (folderSongs.isNotEmpty()) {
                                                        onSongSelect(folderSongs.first(), folderSongs)
                                                    }
                                                },
                                            shape = getGroupShape(position),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(36.dp))
                                                Spacer(Modifier.width(16.dp))
                                                Column {
                                                    Text(folderName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    Text("${folderSongs.size} tracks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        4 -> {
                            val favoriteSongs = remember(songs) { songs.filter { it.isFavorite } }
                            if (favoriteSongs.isEmpty()) {
                                EmptyStateView(message = "No favorites starred yet")
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(favoriteSongs) { index, song ->
                                        val position = when {
                                            favoriteSongs.size == 1 -> GroupPosition.Single
                                            index == 0 -> GroupPosition.Top
                                            index == favoriteSongs.size - 1 -> GroupPosition.Bottom
                                            else -> GroupPosition.Middle
                                        }
                                        SongRowItem(
                                            song = song,
                                            onClick = { onSongSelect(song, favoriteSongs) },
                                            onFavoriteClick = { onFavoriteToggle(song) },
                                            isCurrent = song.id == currentSong?.id,
                                            position = position
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showCreatePlaylistDialog) {
                AlertDialog(
                    onDismissRequest = { showCreatePlaylistDialog = false },
                    title = { Text("New Playlist") },
                    text = {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("Enter playlist name...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    onPlaylistCreate(newPlaylistName)
                                    newPlaylistName = ""
                                    showCreatePlaylistDialog = false
                                }
                            }
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePlaylistDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    songs: List<Song>,
    onSongSelect: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onBack: () -> Unit,
    onDeletePlaylist: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("${songs.size} tracks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            IconButton(onClick = onDeletePlaylist) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.error)
            }
        }

        if (songs.isEmpty()) {
            EmptyStateView(message = "This playlist has no tracks")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(songs) { index, song ->
                    val position = when {
                        songs.size == 1 -> GroupPosition.Single
                        index == 0 -> GroupPosition.Top
                        index == songs.size - 1 -> GroupPosition.Bottom
                        else -> GroupPosition.Middle
                    }
                    SongRowItem(
                        song = song,
                        onClick = { onSongSelect(song) },
                        onFavoriteClick = { onFavoriteToggle(song) },
                        position = position
                    )
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
