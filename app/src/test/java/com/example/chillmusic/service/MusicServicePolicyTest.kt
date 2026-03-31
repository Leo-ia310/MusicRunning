package com.example.chillmusic.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicServicePolicyTest {

    @Test
    fun stops_when_idle_and_motion_mode_is_disabled() {
        assertTrue(
            MusicServicePolicy.shouldStopOnTaskRemoved(
                isPlaying = false,
                motionModeEnabled = false
            )
        )
    }

    @Test
    fun stays_alive_while_playing_or_tracking_motion() {
        assertFalse(
            MusicServicePolicy.shouldStopOnTaskRemoved(
                isPlaying = true,
                motionModeEnabled = false
            )
        )
        assertFalse(
            MusicServicePolicy.shouldStopOnTaskRemoved(
                isPlaying = false,
                motionModeEnabled = true
            )
        )
    }
}
