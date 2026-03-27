package com.example.chillmusic.data.model

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val progress: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.NONE
)
