package com.example.chillmusic.ui.utils

object Translation {
    private val en = mapOf(
        "home" to "Home",
        "library" to "Library",
        "settings" to "Settings",
        "move_to_beat" to "Move to the beat",
        "chill_music" to "Chill Music",
        "while_running" to " While Running",
        "language" to "Language",
        "english" to "English",
        "spanish" to "Español",
        "running_mode" to "Running Mode",
        "play_only_moving" to "Play music only while moving",
        "sensitivity" to "Sensitivity",
        "less_sensitive" to "Less sensitive",
        "more_sensitive" to "More sensitive",
        "when_stop_moving" to "When you stop moving:",
        "pause_music" to "Pause music",
        "resume_moving" to "Resume when you start moving again",
        "lower_volume" to "Lower volume",
        "reduce_30" to "Reduce to 30% and restore on movement",
        "skip_next" to "Skip to next track",
        "change_song_pause" to "Change song and pause",
        "permissions" to "Permissions",
        "motion_sensors" to "Motion Sensors",
        "location" to "Location",
        "grant_permissions" to "Grant Permissions",
        "granted" to "Granted",
        "not_granted" to "Not granted",
        "upload_mp3" to "Upload MP3 Files",
        "no_tracks" to "No tracks found",
        "your_music" to "Your Music",
        "auto_playback" to "Auto Playback",
        "speed_sync" to "Sync Music Speed with Cadence",
        "sync_intensity" to "Sync Intensity"
    )

    private val es = mapOf(
        "home" to "Inicio",
        "library" to "Biblioteca",
        "settings" to "Ajustes",
        "move_to_beat" to "Muévete al ritmo",
        "chill_music" to "Música Chill",
        "while_running" to " Mientras Corres",
        "language" to "Idioma",
        "english" to "English",
        "spanish" to "Español",
        "running_mode" to "Modo Correr",
        "play_only_moving" to "Reproducir solo al moverte",
        "sensitivity" to "Sensibilidad",
        "less_sensitive" to "Menos sensible",
        "more_sensitive" to "Más sensible",
        "when_stop_moving" to "Cuando dejas de moverte:",
        "pause_music" to "Pausar música",
        "resume_moving" to "Reanudar cuando vuelvas a moverte",
        "lower_volume" to "Bajar volumen",
        "reduce_30" to "Reducir al 30% y restaurar al moverte",
        "skip_next" to "Saltar a la siguiente",
        "change_song_pause" to "Cambiar canción y pausar",
        "permissions" to "Permisos",
        "motion_sensors" to "Sensores de movimiento",
        "location" to "Ubicación",
        "grant_permissions" to "Conceder permisos",
        "granted" to "Concedido",
        "not_granted" to "No concedido",
        "upload_mp3" to "Subir archivos MP3",
        "no_tracks" to "No se encontraron canciones",
        "your_music" to "Tu música",
        "auto_playback" to "Reproducción Automática",
        "speed_sync" to "Sincronizar velocidad con el ritmo",
        "sync_intensity" to "Intensidad de Sincronización"
    )

    fun getString(key: String, language: String): String {
        val map = if (language == "es") es else en
        return map[key] ?: key
    }
}
