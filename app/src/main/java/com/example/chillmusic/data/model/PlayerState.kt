package com.example.chillmusic.data.model

enum class PlayerErrorType {
    NO_TRACKS,
    SOURCE_UNAVAILABLE,
    UNSUPPORTED_FORMAT,
    PLAYBACK_FAILED
}

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val progress: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val errorType: PlayerErrorType? = null,
    val errorDetail: String? = null
)
