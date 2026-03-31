package com.example.chillmusic.data.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long,
    val url: String,
    val source: Source,
    val license: String? = null,
    val coverUrl: String? = null
) {
    enum class Source {
        USER
    }
}
