package com.example.chillmusic.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.chillmusic.data.model.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

class MusicRepository(private val context: Context) {

    companion object {
        private const val TAG = "MusicRepository"
        private const val USER_TRACKS_FILE = "user_tracks.json"
        private const val IMPORTED_TRACKS_DIR = "imported_tracks"
    }

    private val gson = Gson()

    suspend fun getUserTracks(): List<Track> = withContext(Dispatchers.IO) {
        val savedTracks = loadSavedTracks()
        val validTracks = savedTracks.filter(::isTrackAccessible)
        if (validTracks.size != savedTracks.size) {
            saveUserTracks(validTracks)
        }
        validTracks
    }

    suspend fun addUserTrack(uri: Uri): Track? = withContext(Dispatchers.IO) {
        try {
            tryTakePersistablePermission(uri)

            val fileName = getFileName(uri) ?: "Unknown Track"
            val importedFile = copyToManagedStorage(uri, fileName)
            val newTrack = Track(
                id = "user-${System.currentTimeMillis()}",
                title = fileName.substringBeforeLast("."),
                artist = "Local Device",
                album = null,
                duration = getDurationFromFile(importedFile),
                url = importedFile.absolutePath,
                source = Track.Source.USER
            )

            val currentTracks = getUserTracks().toMutableList()
            currentTracks.add(newTrack)
            saveUserTracks(currentTracks)
            newTrack
        } catch (error: Exception) {
            Log.e(TAG, "Failed to import track from $uri", error)
            null
        }
    }

    suspend fun removeUserTrack(track: Track) = withContext(Dispatchers.IO) {
        val currentTracks = getUserTracks().toMutableList()
        currentTracks.removeAll { it.id == track.id }
        deleteManagedTrack(track)
        saveUserTracks(currentTracks)
    }

    private fun loadSavedTracks(): List<Track> {
        val file = File(context.filesDir, USER_TRACKS_FILE)
        if (!file.exists()) return emptyList()

        return try {
            val json = file.readText()
            val type = object : TypeToken<List<Track>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (error: Exception) {
            Log.e(TAG, "Failed to read saved tracks", error)
            emptyList()
        }
    }

    private fun saveUserTracks(tracks: List<Track>) {
        File(context.filesDir, USER_TRACKS_FILE).writeText(gson.toJson(tracks))
    }

    private fun tryTakePersistablePermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // We copy the file into app storage right away, so playback does not depend on URI grants later.
        }
    }

    private fun copyToManagedStorage(sourceUri: Uri, displayName: String): File {
        val importDirectory = File(context.filesDir, IMPORTED_TRACKS_DIR).apply { mkdirs() }
        val extension = resolveExtension(sourceUri, displayName)
        val baseName = displayName.substringBeforeLast(".", "track")
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .ifBlank { "track" }
            .take(40)
        val destinationFile = File(
            importDirectory,
            "${System.currentTimeMillis()}-$baseName-${UUID.randomUUID().toString().take(8)}.$extension"
        )

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Could not open source stream")

        return destinationFile
    }

    private fun resolveExtension(sourceUri: Uri, displayName: String): String {
        val fromName = displayName.substringAfterLast('.', "").lowercase().takeIf { it.isNotBlank() }
        if (fromName != null) return fromName

        val mimeType = context.contentResolver.getType(sourceUri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.lowercase() ?: "mp3"
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = cursor.getString(index)
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

    private fun getDurationFromFile(file: File): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun isTrackAccessible(track: Track): Boolean {
        val file = resolveManagedFile(track) ?: return false
        return file.exists() && file.isFile
    }

    private fun deleteManagedTrack(track: Track) {
        val file = resolveManagedFile(track) ?: return
        if (file.exists()) {
            file.delete()
        }
    }

    private fun resolveManagedFile(track: Track): File? {
        val file = when {
            track.url.startsWith("file://") -> Uri.parse(track.url).path?.let(::File)
            track.url.contains("://") -> null
            else -> File(track.url)
        } ?: return null

        val importDirectory = File(context.filesDir, IMPORTED_TRACKS_DIR)
        return file.takeIf { candidate ->
            candidate.canonicalPath.startsWith(importDirectory.canonicalPath)
        }
    }
}
