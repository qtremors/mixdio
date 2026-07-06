package dev.qtremors.melatune

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.qtremors.melatune.core.storage.*
import dev.qtremors.melatune.core.ui.theme.MelatuneTheme
import dev.qtremors.melatune.core.ui.theme.GSFlexPreset
import dev.qtremors.melatune.core.ui.theme.GSFlexSettings
import dev.qtremors.melatune.feature.player.screens.MainScreen
import dev.qtremors.melatune.feature.player.screens.PlayerScreen
import dev.qtremors.melatune.player.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var mediaController: MediaController? = null
    private lateinit var musicProvider: MusicProvider
    private lateinit var database: MusicDatabase

    // State definitions
    private val songs = mutableStateListOf<Song>()
    private val playlists = mutableStateListOf<Playlist>()
    private val playlistSongsMap = mutableStateMapOf<Long, List<Song>>()

    private var currentSong by mutableStateOf<Song?>(null)
    private var isPlaying by mutableStateOf(false)
    private var progress by mutableFloatStateOf(0f)
    private var playbackPosition by mutableLongStateOf(0L)
    private var isShuffleEnabled by mutableStateOf(false)
    private var isRepeatEnabled by mutableStateOf(false)

    private var showPlayerScreen by mutableStateOf(false)
    private var progressPollJob: Job? = null

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (audioGranted) {
            loadMusicData()
        } else {
            Toast.makeText(this, "Storage access is required to scan offline songs.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        musicProvider = MusicProvider(this)
        database = MusicDatabase.getDatabase(this)

        checkPermissionsAndLoad()
        initializePlayerController()

        setContent {
            val flexSettings = remember { GSFlexSettings(GSFlexPreset.EXPRESSIVE) }
            MelatuneTheme(flexSettings = flexSettings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(
                            songs = songs,
                            playlists = playlists,
                            playlistSongsMap = playlistSongsMap,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            progress = progress,
                            onSongSelect = { song, queue -> playSong(song, queue) },
                            onPlayPauseClick = { togglePlayPause() },
                            onNextClick = { playNext() },
                            onFavoriteToggle = { song -> toggleFavorite(song) },
                            onPlaylistCreate = { name -> createPlaylist(name) },
                            onPlaylistDelete = { playlist -> deletePlaylist(playlist) },
                            onAddSongToPlaylist = { playlist, song -> addSongToPlaylist(playlist, song) },
                            onMiniPlayerClick = { showPlayerScreen = true }
                        )

                        // Full-screen Player transition overlay
                        AnimatedVisibility(
                            visible = showPlayerScreen,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            PlayerScreen(
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                progress = progress,
                                playbackPosition = playbackPosition,
                                isShuffleEnabled = isShuffleEnabled,
                                isRepeatEnabled = isRepeatEnabled,
                                onCollapseClick = { showPlayerScreen = false },
                                onPlayPauseClick = { togglePlayPause() },
                                onPreviousClick = { playPrevious() },
                                onNextClick = { playNext() },
                                onShuffleClick = { toggleShuffle() },
                                onRepeatClick = { toggleRepeat() },
                                onSeek = { targetPercent -> seekTo(targetPercent) },
                                onFavoriteClick = { currentSong?.let { toggleFavorite(it) } }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndLoad() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            loadMusicData()
        } else {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun loadMusicData() {
        lifecycleScope.launch {
            val cached = musicProvider.getCachedSongs()
            if (cached.isNotEmpty()) {
                songs.clear()
                songs.addAll(cached)
            }

            val fresh = musicProvider.syncSongs()
            songs.clear()
            songs.addAll(fresh)

            loadPlaylistsData()
        }
    }

    private suspend fun loadPlaylistsData() {
        val allPlaylists = database.playlistDao().getAllPlaylists()
        playlists.clear()
        playlists.addAll(allPlaylists)

        val mappings = database.playlistDao().getAllPlaylistMappings()
        val groupedMap = mappings.groupBy { it.playlistId }
        
        playlistSongsMap.clear()
        allPlaylists.forEach { pl ->
            val songIds = groupedMap[pl.id]?.map { it.songId } ?: emptyList()
            val plSongs = songs.filter { it.id in songIds }
            playlistSongsMap[pl.id] = plSongs
        }
    }

    private fun initializePlayerController() {
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                setupControllerListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupControllerListener() {
        val controller = mediaController ?: return
        
        updateStateFromController()

        controller.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentSongFromMediaItem(mediaItem)
            }

            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
                if (isPlayingChanged) {
                    startProgressPolling()
                } else {
                    stopProgressPolling()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateStateFromController()
            }
        })
    }

    private fun updateStateFromController() {
        val controller = mediaController ?: return
        isPlaying = controller.isPlaying
        isShuffleEnabled = controller.shuffleModeEnabled
        isRepeatEnabled = controller.repeatMode == Player.REPEAT_MODE_ONE
        updateCurrentSongFromMediaItem(controller.currentMediaItem)

        if (isPlaying) {
            startProgressPolling()
        } else {
            stopProgressPolling()
        }
    }

    private fun updateCurrentSongFromMediaItem(mediaItem: MediaItem?) {
        if (mediaItem == null) {
            currentSong = null
            return
        }
        val mediaId = mediaItem.mediaId.toLongOrNull() ?: return
        currentSong = songs.find { it.id == mediaId }
    }

    private fun playSong(song: Song, queue: List<Song>) {
        val controller = mediaController ?: return
        controller.stop()
        controller.clearMediaItems()

        val mediaItems = queue.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.uri)
                .build()
        }

        controller.addMediaItems(mediaItems)
        val songIndex = queue.indexOfFirst { it.id == song.id }
        if (songIndex >= 0) {
            controller.seekTo(songIndex, 0L)
        }
        controller.prepare()
        controller.play()
    }

    private fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.playbackState == Player.STATE_IDLE) {
            controller.prepare()
        }
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    private fun playNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    private fun playPrevious() {
        val controller = mediaController ?: return
        if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        }
    }

    private fun toggleShuffle() {
        val controller = mediaController ?: return
        val enabled = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = enabled
        isShuffleEnabled = enabled
    }

    private fun toggleRepeat() {
        val controller = mediaController ?: return
        val repeatMode = if (controller.repeatMode == Player.REPEAT_MODE_ONE) {
            Player.REPEAT_MODE_OFF
        } else {
            Player.REPEAT_MODE_ONE
        }
        controller.repeatMode = repeatMode
        isRepeatEnabled = repeatMode == Player.REPEAT_MODE_ONE
    }

    private fun seekTo(percent: Float) {
        val controller = mediaController ?: return
        val duration = controller.duration
        if (duration > 0) {
            val targetPosition = (duration * percent).toLong()
            controller.seekTo(targetPosition)
            playbackPosition = targetPosition
        }
    }

    private fun toggleFavorite(song: Song) {
        lifecycleScope.launch {
            val override = database.songOverrideDao().getOverrideForSong(song.id)
            val updatedOverride = override?.copy(isFavorite = !override.isFavorite)
                ?: SongOverride(songId = song.id, isFavorite = true)
            
            database.songOverrideDao().insertOverride(updatedOverride)

            val idx = songs.indexOfFirst { it.id == song.id }
            if (idx >= 0) {
                val updatedSong = songs[idx].copy(isFavorite = updatedOverride.isFavorite)
                songs[idx] = updatedSong
                musicProvider.updateSongInCache(updatedSong)
                if (currentSong?.id == song.id) {
                    currentSong = updatedSong
                }
            }

            loadPlaylistsData()
        }
    }

    private fun createPlaylist(name: String) {
        lifecycleScope.launch {
            database.playlistDao().insertPlaylist(Playlist(name = name))
            loadPlaylistsData()
        }
    }

    private fun deletePlaylist(playlist: Playlist) {
        lifecycleScope.launch {
            database.playlistDao().deletePlaylist(playlist)
            loadPlaylistsData()
        }
    }

    private fun addSongToPlaylist(playlist: Playlist, song: Song) {
        lifecycleScope.launch {
            database.playlistDao().addSongToPlaylist(PlaylistSong(playlistId = playlist.id, songId = song.id))
            loadPlaylistsData()
        }
    }

    private fun startProgressPolling() {
        progressPollJob?.cancel()
        progressPollJob = lifecycleScope.launch {
            while (true) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    playbackPosition = controller.currentPosition
                    val duration = controller.duration
                    progress = if (duration > 0) playbackPosition.toFloat() / duration else 0f
                }
                delay(200)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollJob?.cancel()
        progressPollJob = null
    }

    override fun onDestroy() {
        stopProgressPolling()
        super.onDestroy()
    }
}
