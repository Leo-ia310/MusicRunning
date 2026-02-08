package com.example.chillmusic.service

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.chillmusic.data.MotionRepository
import com.example.chillmusic.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var motionRepository: MotionRepository
    
    // Logic for "Running Mode"
    private var isRunningModeEnabled = false
    private var speedThresholdKmh = 5.0f // Default 5 km/h
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var motionJob: Job? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()

        mediaSession = MediaSession.Builder(this, exoPlayer).build()
        
        motionRepository = MotionRepository(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_RUNNING_MODE -> startRunningMode()
                ACTION_STOP_RUNNING_MODE -> stopRunningMode()
                ACTION_UPDATE_THRESHOLD -> {
                    val threshold = it.getFloatExtra(EXTRA_THRESHOLD, 5.0f)
                    speedThresholdKmh = threshold
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startRunningMode() {
        if (isRunningModeEnabled) return
        isRunningModeEnabled = true
        
        motionJob?.cancel()
        motionJob = motionRepository.getLocationFlow()
            .onEach { location ->
                val speed = motionRepository.getSpeedKmh(location)
                handleSpeedChange(speed)
            }
            .launchIn(serviceScope)
    }

    private fun stopRunningMode() {
        isRunningModeEnabled = false
        motionJob?.cancel()
        // Optionally pause music when stopping mode? Or leave it as is.
    }

    private fun handleSpeedChange(speed: Float) {
        if (!isRunningModeEnabled) return

        if (speed > speedThresholdKmh) {
            // User is running
            if (!exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_READY) {
                exoPlayer.play()
            }
        } else {
            // User stopped
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            }
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        motionJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_RUNNING_MODE = "ACTION_START_RUNNING_MODE"
        const val ACTION_STOP_RUNNING_MODE = "ACTION_STOP_RUNNING_MODE"
        const val ACTION_UPDATE_THRESHOLD = "ACTION_UPDATE_THRESHOLD"
        const val EXTRA_THRESHOLD = "EXTRA_THRESHOLD"
    }
}
