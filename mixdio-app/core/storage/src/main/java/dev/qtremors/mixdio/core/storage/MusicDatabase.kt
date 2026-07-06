package dev.qtremors.mixdio.core.storage

import android.content.Context
import androidx.room.*

@Dao
interface SongOverrideDao {
    @Query("SELECT * FROM song_overrides")
    suspend fun getAllOverrides(): List<SongOverride>

    @Query("SELECT * FROM song_overrides WHERE isFavorite = 1")
    suspend fun getFavorites(): List<SongOverride>

    @Query("SELECT * FROM song_overrides WHERE songId = :songId")
    suspend fun getOverrideForSong(songId: Long): SongOverride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: SongOverride)

    @Delete
    suspend fun deleteOverride(override: SongOverride)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylists(): List<Playlist>

    @Query("SELECT * FROM playlist_songs")
    suspend fun getAllPlaylistMappings(): List<PlaylistSong>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(playlistSong: PlaylistSong)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongsToPlaylist(playlistSongs: List<PlaylistSong>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId IN (:songIds)")
    suspend fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    suspend fun getSongIdsForPlaylist(playlistId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE songId = :songId")
    suspend fun getPlaylistCountForSong(songId: Long): Int
}

@Database(entities = [SongOverride::class, Playlist::class, PlaylistSong::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songOverrideDao(): SongOverrideDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "mixdio_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
