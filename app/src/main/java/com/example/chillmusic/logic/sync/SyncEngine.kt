package com.example.chillmusic.logic.sync

import com.example.chillmusic.data.model.MotionState
import com.example.chillmusic.data.model.PlayerState
import com.example.chillmusic.data.model.StopBehavior
import com.example.chillmusic.data.repository.SettingsRepository
import com.example.chillmusic.logic.audio.AudioPlayerManager
import com.example.chillmusic.logic.sensor.MotionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SyncEngine(
    private val scope: CoroutineScope,
    private val audioManager: AudioPlayerManager,
    private val motionDetector: MotionDetector,
    private val settingsRepo: SettingsRepository
) {
    private var previousMotionState = MotionState.STOPPED
    private var storedVolume = 0.8f
    private var debounceJob: Job? = null

    init {
        scope.launch {
            combine(
                motionDetector.motionState,
                motionDetector.stepCadence,
                settingsRepo.settings,
                audioManager.playerState
            ) { motionState, cadence, settings, playerState ->
                Triple(motionState, cadence, Pair(settings, playerState))
            }.collect { (motionState, cadence, pairs) ->
                val settings = pairs.first
                val playerState = pairs.second

                val motionEnabled = settings.motion.enabled

                // If motion sync is disabled, cancel any pending debounce, optionally
                // restore speed, and do nothing else to avoid interfering with manual playback.
                if (!motionEnabled) {
                    debounceJob?.cancel()
                    if (playerState.isPlaying && playerState.speed != 1.0f) {
                        audioManager.setPlaybackSpeed(1.0f)
                    }
                    previousMotionState = motionState
                    return@collect
                }

                if (settings.motion.autoPlayEnabled) {
                    handleSmartPlayback(motionState, playerState, settings.motion.stopBehavior)
                }
                if (settings.motion.syncSpeedEnabled) {
                    handleSpeedSync(motionState, cadence, settings.motion.syncIntensity)
                } else if (playerState.speed != 1.0f) {
                    audioManager.setPlaybackSpeed(1.0f)
                }

                previousMotionState = motionState
            }
        }
    }

    private fun handleSmartPlayback(current: MotionState, playerState: PlayerState, stopBehavior: StopBehavior) {
        if (current == previousMotionState) return

        val isMoving = current == MotionState.WALKING || current == MotionState.RUNNING
        val wasMoving = previousMotionState == MotionState.WALKING || previousMotionState == MotionState.RUNNING

        if (isMoving && !wasMoving) {
            // Started moving: cancel pause debounce and play immediately!
            debounceJob?.cancel()
            debounceJob = scope.launch {
                if (storedVolume > 0) {
                    audioManager.setVolume(storedVolume)
                }
                if (!playerState.isPlaying && playerState.currentTrack != null) {
                    audioManager.play()
                }
            }
        } else if (!isMoving && wasMoving) {
            // Stopped moving: Debounce for 3 seconds before pausing to prevent stuttering
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(3000) // 3 seconds grace period
                
                // Double check if toggle was disabled during delay
                if (!settingsRepo.settings.value.motion.enabled) return@launch
                
                when (stopBehavior) {
                    StopBehavior.PAUSE -> {
                        audioManager.pause()
                    }
                    StopBehavior.LOWER_VOLUME -> {
                        storedVolume = playerState.volume
                        audioManager.setVolume(storedVolume * 0.3f)
                    }
                    StopBehavior.NEXT_TRACK -> {
                        audioManager.next()
                        audioManager.pause()
                    }
                }
            }
        }
    }

    private fun handleSpeedSync(motionState: MotionState, cadence: Int, intensity: Float) {
        if (motionState == MotionState.RUNNING || motionState == MotionState.WALKING) {
            val targetSpeed = if (cadence > 0) {
                val clampedCadence = cadence.coerceIn(80, 200).toFloat()
                // diff from 120 baseline. 160 cadence at full intensity = 1.2x speed
                val diff = clampedCadence - 120f
                1.0f + (diff * 0.005f * intensity)
            } else {
                1.0f
            }
            audioManager.setPlaybackSpeed(targetSpeed)
        } else {
            audioManager.setPlaybackSpeed(1.0f)
        }
    }
}
