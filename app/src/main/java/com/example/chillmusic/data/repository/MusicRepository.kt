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
        Track("chill-001", "Midnight Runner", "Chill Beats Studio", "Night Run", 198000, "synth://chill-001", Track.Source.CATALOG, "CC0", null),
        Track("chill-002", "Urban Flow", "Lo-Fi Motion", "City Jogger", 224000, "synth://chill-002", Track.Source.CATALOG, "CC0", null),
        Track("chill-003", "Sunset Stride", "Ambient Pace", "Golden Hour", 186000, "synth://chill-003", Track.Source.CATALOG, "CC0", null),
        Track("chill-004", "Neon Pulse", "Synthwave Runner", "Electric Miles", 210000, "synth://chill-004", Track.Source.CATALOG, "CC0", null),
        Track("chill-005", "Morning Jog", "Nature Beats", "Dawn Patrol", 175000, "synth://chill-005", Track.Source.CATALOG, "CC0", null),
        Track("chill-006", "Deep Breath", "Zen Runners", "Mindful Miles", 240000, "synth://chill-006", Track.Source.CATALOG, "CC0", null),
        Track("chill-007", "Coastal Run", "Wave Tempo", "Ocean Pace", 192000, "synth://chill-007", Track.Source.CATALOG, "CC0", null),
        Track("chill-008", "Electric Dreams", "Digital Stride", "Future Pace", 215000, "synth://chill-008", Track.Source.CATALOG, "CC0", null)
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
            val fileName = getFileName(uri) ?: "Unknown Track"
            val id = "user-${System.currentTimeMillis()}"
            val internalFile = File(context.filesDir, "$id.mp3")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(internalFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Estimate duration or get real duration (simplified for now, setting to 0 or needing extraction)
            // Real implementation would use MediaMetadataRetriever
            val duration = getDuration(internalFile)

            val newTrack = Track(
                id = id,
                title = fileName.substringBeforeLast("."), // Simple title from filename
                artist = "Unknown Artist", // Placeholder
                album = null,
                duration = duration,
                url = internalFile.absolutePath,
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
        val file = File(track.url)
        if (file.exists()) {
            file.delete()
        }
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

    private fun getDuration(file: File): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
