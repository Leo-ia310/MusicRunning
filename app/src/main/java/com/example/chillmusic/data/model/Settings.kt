package com.example.chillmusic.data.model

enum class StopBehavior {
    PAUSE,
    LOWER_VOLUME,
    NEXT_TRACK
}

enum class RepeatMode {
    NONE,
    ALL,
    ONE
}

data class MotionSettings(
    val enabled: Boolean = false,
    val sensitivity: Int = 5,
    val stopBehavior: StopBehavior = StopBehavior.PAUSE,
    val walkingThreshold: Float = 1.5f,
    val runningThreshold: Float = 4.0f
)

data class AppSettings(
    val motion: MotionSettings = MotionSettings(),
    val musicSource: String = "each", // "catalog"|"user"|"all"
    val language: String = "en"
)
