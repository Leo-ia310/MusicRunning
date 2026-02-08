package com.example.chillmusic.data.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long, // Duration in milliseconds
    val url: String, // Path to audio file
    val source: Source,
    val license: String? = null,
    val coverUrl: String? = null
) {
    enum class Source {
        CATALOG,
        USER
    }
}
