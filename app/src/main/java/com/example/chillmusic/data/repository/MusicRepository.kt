package com.example.chillmusic.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.chillmusic.data.model.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MusicRepository(private val context: Context) {

    private val gson = Gson()
    private val USER_TRACKS_FILE = "user_tracks.json"

    val catalog: List<Track> = listOf(
        Track(
            id = "synth_1",
            title = "Pulse of the Road",
            artist = "ChillRun AI",
            duration = 180000,
            url = "synth://low_pulse",
            source = Track.Source.CATALOG
        ),
        Track(
            id = "synth_2",
            title = "Midnight Sprint",
            artist = "ChillRun AI",
            duration = 240000,
            url = "synth://fast_beat",
            source = Track.Source.CATALOG
        ),
        Track(
            id = "synth_3",
            title = "Zen Jog",
            artist = "ChillRun AI",
            duration = 300000,
            url = "synth://ambient_flow",
            source = Track.Source.CATALOG
        )
    )

    suspend fun getUserTracks(): List<Track> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, USER_TRACKS_FILE)
        if (!file.exists()) return@withContext emptyList()
        try {
            val json = file.readText()
            val type = object : TypeToken<List<Track>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addUserTrack(uri: Uri): Track? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            try {
                contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Not all URIs support persistable permissions, ignore if it fails
            }
            
            val fileName = getFileName(uri) ?: "Unknown Track"
            val id = "user-${System.currentTimeMillis()}"

            val duration = getDurationFromUri(uri)

            val newTrack = Track(
                id = id,
                title = fileName.substringBeforeLast("."), 
                artist = "Local Device",
                album = null,
                duration = duration,
                url = uri.toString(),
                source = Track.Source.USER
            )

            val currentTracks = getUserTracks().toMutableList()
            currentTracks.add(newTrack)
            saveUserTracks(currentTracks)
            newTrack
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun removeUserTrack(track: Track) = withContext(Dispatchers.IO) {
        val currentTracks = getUserTracks().toMutableList()
        currentTracks.removeAll { it.id == track.id }
        saveUserTracks(currentTracks)
    }

    private fun saveUserTracks(tracks: List<Track>) {
        val file = File(context.filesDir, USER_TRACKS_FILE)
        file.writeText(gson.toJson(tracks))
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if(index >= 0) result = cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun getDurationFromUri(uri: Uri): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
