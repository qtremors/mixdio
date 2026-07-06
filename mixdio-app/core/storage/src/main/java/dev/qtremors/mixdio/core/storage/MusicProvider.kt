package dev.qtremors.mixdio.core.storage

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UriTypeAdapter : TypeAdapter<Uri>() {
    override fun write(out: JsonWriter, value: Uri?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    override fun read(reader: JsonReader): Uri? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return reader.nextString().toUri()
    }
}

class MusicProvider(private val context: Context) {
    private val database = MusicDatabase.getDatabase(context)
    private val cacheFile = File(context.filesDir, "mixdio_songs_cache.json")
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .disableHtmlEscaping()
        .create()

    fun getCachedSongs(): List<Song> {
        if (!cacheFile.exists()) return emptyList()
        return try {
            val json = cacheFile.readText()
            val type = object : TypeToken<List<Song>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveToCache(songs: List<Song>) {
        try {
            val json = gson.toJson(songs)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSongInCache(updatedSong: Song) {
        val songs = getCachedSongs().toMutableList()
        val idx = songs.indexOfFirst { it.id == updatedSong.id }
        if (idx >= 0) {
            songs[idx] = updatedSong
            saveToCache(songs)
        } else {
            songs.add(updatedSong)
            saveToCache(songs)
        }
    }

    fun hasReadPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun syncSongs(): List<Song> = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) return@withContext emptyList()
        val songList = mutableListOf<Song>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add(MediaStore.Audio.Media.GENRE)
            projectionList.add(MediaStore.Audio.Media.BITRATE)
        }
        val projection = projectionList.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val overrides = database.songOverrideDao().getAllOverrides().associateBy { it.songId }

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            } else -1
            val bitrateColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                var title = cursor.getString(titleColumn) ?: "Unknown Title"
                var artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                var album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val data = cursor.getString(dataColumn) ?: ""
                val dateAdded = cursor.getLong(dateAddedColumn)
                var genre = if (genreColumn != -1) cursor.getString(genreColumn) else null
                
                if (artist == "<unknown>") artist = "Unknown Artist"
                if (album == "<unknown>") album = "Unknown Album"

                val override = overrides[id]
                var coverUrl: String? = null
                var isFavorite = false

                if (override != null) {
                    override.title?.let { if (it.isNotBlank()) title = it }
                    override.artist?.let { if (it.isNotBlank()) artist = it }
                    override.album?.let { if (it.isNotBlank()) album = it }
                    override.genre?.let { if (it.isNotBlank()) genre = it }
                    coverUrl = override.coverUri
                    isFavorite = override.isFavorite
                }

                val folderName = data.substringBeforeLast("/").substringAfterLast("/")
                val extension = data.substringAfterLast(".").lowercase()
                val format = when (extension) {
                    "mp3" -> "MP3"
                    "flac" -> "FLAC"
                    "wav" -> "WAV"
                    "aac", "m4a" -> "AAC"
                    "ogg" -> "OGG"
                    "opus" -> "OPUS"
                    "wma" -> "WMA"
                    "alac" -> "ALAC"
                    else -> extension.uppercase()
                }
                val file = File(data)

                val mediaStoreBitrate = if (bitrateColumn != -1) {
                    val value = cursor.getInt(bitrateColumn)
                    if (value > 0) value else null
                } else null

                val calculatedBitrate = if (duration > 0 && file.exists()) {
                    ((file.length() * 8) / (duration / 1000f)).toInt()
                } else null

                val bitrate = mediaStoreBitrate ?: calculatedBitrate

                val contentUri: Uri = ContentUris.withAppendedId(collection, id)

                val albumArtUri = try {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )
                } catch (_: Exception) { null }

                songList.add(
                    Song(
                        id = id,
                        albumId = albumId,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uriString = contentUri.toString(),
                        path = data,
                        dateAdded = dateAdded,
                        albumArtUriString = coverUrl ?: albumArtUri?.toString(),
                        genre = genre,
                        folderName = folderName,
                        isFavorite = isFavorite,
                        format = format,
                        bitrate = bitrate
                    )
                )
            }
        }
        saveToCache(songList)
        songList
    }

    suspend fun refreshLibrary(): List<Song> = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) return@withContext emptyList()

        val pathsToScan = mutableSetOf<String>()
        getCachedSongs().forEach { pathsToScan.add(it.path) }

        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (musicDir.exists()) pathsToScan.add(musicDir.absolutePath)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) pathsToScan.add(downloadsDir.absolutePath)

        if (pathsToScan.isNotEmpty()) {
            val latch = CountDownLatch(1)
            MediaScannerConnection.scanFile(
                context,
                pathsToScan.toTypedArray(),
                null
            ) { _, _ -> latch.countDown() }
            latch.await(10, TimeUnit.SECONDS)
        }

        syncSongs()
    }
}
