package com.example.chillmusic.logic.audio

import com.example.chillmusic.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistCoordinatorTest {

    @Test
    fun select_moves_cursor_to_requested_track() {
        val tracks = listOf(track("a"), track("b"), track("c"))

        val snapshot = PlaylistCoordinator.select(tracks, track("c"))

        assertEquals(2, snapshot.currentIndex)
        assertEquals("c", snapshot.currentTrack?.id)
    }

    @Test
    fun sync_falls_back_to_first_track_when_current_is_missing() {
        val tracks = listOf(track("a"), track("b"))

        val snapshot = PlaylistCoordinator.sync(tracks, currentTrackId = "missing")

        assertEquals(0, snapshot.currentIndex)
        assertEquals("a", snapshot.currentTrack?.id)
    }

    @Test
    fun previous_wraps_to_last_track() {
        val tracks = listOf(track("a"), track("b"), track("c"))

        val snapshot = PlaylistCoordinator.previous(tracks, currentIndex = 0)

        assertEquals(2, snapshot.currentIndex)
        assertEquals("c", snapshot.currentTrack?.id)
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = "Artist",
        duration = 1_000L,
        url = "/tmp/$id.mp3",
        source = Track.Source.USER
    )
}
