package com.example.chillmusic.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.chillmusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    // In a real app, we would copy assets to internal storage or Play Asset Delivery.
    // For this prototype, we'll simulate "App Music" with a list of dummy or raw resource references if possible,
    // but the user requirement says "App Music" (Free of copyright). 
    // I will assume we might have raw resources or just simulate it for now as "Built-in".
    // Since I cannot upload MP3s, I will generate logic to read from `res/raw` if they existed, 
    // or just return a placeholder list for the "Chill" library.

    suspend fun getAppMusic(): List<Song> = withContext(Dispatchers.IO) {
        // Mock data for "Chill Music" (Built-in)
        listOf(
            Song(
                id = "1",
                title = "Chill Run Track 1",
                artist = "Chill Music App (COMA)",
                uri = Uri.EMPTY, // Placeholder, normally: Uri.parse("android.resource://com.example.chillmusic/raw/track1")
                isAppMusic = true
            ),
            Song(
                id = "2",
                title = "Morning Jog",
                artist = "Chill Music App (COMA)",
                uri = Uri.EMPTY,
                isAppMusic = true
            )
        )
    }

    suspend fun getUserMusic(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Select only music
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                songs.add(
                    Song(
                        id = id.toString(),
                        title = title,
                        artist = artist ?: "Unknown",
                        uri = contentUri,
                        albumArtUri = albumArtUri,
                        isAppMusic = false
                    )
                )
            }
        }
        songs
    }
}
