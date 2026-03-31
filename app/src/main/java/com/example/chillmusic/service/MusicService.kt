package com.example.chillmusic.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.chillmusic.ChillMusicApplication

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val app = application as ChillMusicApplication
        val player = app.audioPlayerManager.getPlayer()
        if (player != null) {
            mediaSession = MediaSession.Builder(this, player).build()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val app = application as ChillMusicApplication
        val shouldStop = MusicServicePolicy.shouldStopOnTaskRemoved(
            isPlaying = mediaSession?.player?.isPlaying == true,
            motionModeEnabled = app.settingsRepository.settings.value.motion.enabled
        )
        if (shouldStop) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
