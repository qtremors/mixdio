package dev.qtremors.melatune.core.storage

import android.net.Uri

data class Song(
    val id: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uriString: String,
    val path: String,
    val dateAdded: Long = 0,
    val albumArtUriString: String? = null,
    val genre: String? = null,
    val folderName: String,
    val isFavorite: Boolean = false,
    val format: String = "",
    val bitrate: Int? = null
) {
    val uri: Uri get() = Uri.parse(uriString)
    val albumArtUri: Uri? get() = albumArtUriString?.let { Uri.parse(it) }
}
