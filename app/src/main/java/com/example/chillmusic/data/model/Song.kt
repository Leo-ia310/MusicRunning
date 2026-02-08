package com.example.chillmusic.data.model

import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val uri: Uri,
    val albumArtUri: Uri? = null,
    val isAppMusic: Boolean = false // True for built-in chill music, false for user music
) // simple model for now
