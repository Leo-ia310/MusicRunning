package com.example.chillmusic.logic.audio

import com.example.chillmusic.data.model.Track

data class PlaylistSnapshot(
    val tracks: List<Track>,
    val currentIndex: Int
) {
    val currentTrack: Track?
        get() = tracks.getOrNull(currentIndex)
}

object PlaylistCoordinator {

    fun sync(tracks: List<Track>, currentTrackId: String?): PlaylistSnapshot {
        if (tracks.isEmpty()) return PlaylistSnapshot(emptyList(), -1)
        val index = currentTrackId?.let { id -> tracks.indexOfFirst { it.id == id } } ?: -1
        return PlaylistSnapshot(tracks, if (index >= 0) index else 0)
    }

    fun select(tracks: List<Track>, track: Track): PlaylistSnapshot {
        if (tracks.isEmpty()) return PlaylistSnapshot(listOf(track), 0)
        val index = tracks.indexOfFirst { it.id == track.id }
        return if (index >= 0) {
            PlaylistSnapshot(tracks, index)
        } else {
            PlaylistSnapshot(tracks + track, tracks.size)
        }
    }

    fun next(tracks: List<Track>, currentIndex: Int): PlaylistSnapshot {
        if (tracks.isEmpty()) return PlaylistSnapshot(emptyList(), -1)
        val normalizedIndex = currentIndex.takeIf { it in tracks.indices } ?: 0
        val nextIndex = (normalizedIndex + 1) % tracks.size
        return PlaylistSnapshot(tracks, nextIndex)
    }

    fun previous(tracks: List<Track>, currentIndex: Int): PlaylistSnapshot {
        if (tracks.isEmpty()) return PlaylistSnapshot(emptyList(), -1)
        val normalizedIndex = currentIndex.takeIf { it in tracks.indices } ?: 0
        val previousIndex = if (normalizedIndex == 0) tracks.lastIndex else normalizedIndex - 1
        return PlaylistSnapshot(tracks, previousIndex)
    }
}
