package com.example.chillmusic.service

object MusicServicePolicy {

    fun shouldStopOnTaskRemoved(
        isPlaying: Boolean,
        motionModeEnabled: Boolean
    ): Boolean = !isPlaying && !motionModeEnabled
}
