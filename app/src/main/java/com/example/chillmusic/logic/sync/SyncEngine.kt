package com.example.chillmusic.logic.sync

import com.example.chillmusic.data.model.MotionState
import com.example.chillmusic.data.model.StopBehavior
import com.example.chillmusic.data.repository.SettingsRepository
import com.example.chillmusic.logic.audio.AudioPlayerManager
import com.example.chillmusic.logic.sensor.MotionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs

class SyncEngine(
    private val scope: CoroutineScope,
    private val audioManager: AudioPlayerManager,
    private val motionDetector: MotionDetector,
    private val settingsRepo: SettingsRepository
) {
    private var previousMotionState = MotionState.STOPPED
    private var storedVolume = 0.8f
    private var debounceJob: Job? = null
    private var volumeLoweredByStopBehavior = false

    private data class SyncPlayerSnapshot(
        val trackId: String?,
        val isPlaying: Boolean,
        val volume: Float,
        val speed: Float
    )

    init {
        scope.launch {
            val syncPlayerState = audioManager.playerState
                .map { state ->
                    SyncPlayerSnapshot(
                        trackId = state.currentTrack?.id,
                        isPlaying = state.isPlaying,
                        volume = state.volume,
                        speed = state.speed
                    )
                }
                .distinctUntilChanged()

            combine(
                motionDetector.motionState,
                motionDetector.stepCadence,
                settingsRepo.settings,
                syncPlayerState
            ) { motionState, cadence, settings, playerState ->
                Triple(motionState, cadence, Pair(settings, playerState))
            }.collect { (motionState, cadence, pair) ->
                val settings = pair.first
                val playerState = pair.second

                if (!settings.motion.enabled) {
                    debounceJob?.cancel()
                    if (volumeLoweredByStopBehavior) {
                        audioManager.setVolume(storedVolume)
                        volumeLoweredByStopBehavior = false
                    }
                    if (playerState.isPlaying && abs(playerState.speed - 1.0f) > 0.01f) {
                        audioManager.setPlaybackSpeed(1.0f)
                    }
                    previousMotionState = motionState
                    return@collect
                }

                if (settings.motion.autoPlayEnabled) {
                    handleSmartPlayback(motionState, settings.motion.stopBehavior)
                }

                if (settings.motion.syncSpeedEnabled) {
                    handleSpeedSync(motionState, cadence, settings.motion.syncIntensity)
                } else if (abs(playerState.speed - 1.0f) > 0.01f) {
                    audioManager.setPlaybackSpeed(1.0f)
                }

                previousMotionState = motionState
            }
        }
    }

    private fun handleSmartPlayback(current: MotionState, stopBehavior: StopBehavior) {
        if (current == previousMotionState) return

        val isMoving = current == MotionState.WALKING || current == MotionState.RUNNING
        val wasMoving = previousMotionState == MotionState.WALKING || previousMotionState == MotionState.RUNNING

        if (isMoving && !wasMoving) {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                if (volumeLoweredByStopBehavior) {
                    audioManager.setVolume(storedVolume)
                    volumeLoweredByStopBehavior = false
                }
                if (!audioManager.playerState.value.isPlaying) {
                    audioManager.play()
                }
            }
        } else if (!isMoving && wasMoving) {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(3000)
                if (!settingsRepo.settings.value.motion.enabled) return@launch

                val currentPlayerState = audioManager.playerState.value
                when (stopBehavior) {
                    StopBehavior.PAUSE -> audioManager.pause()

                    StopBehavior.LOWER_VOLUME -> {
                        if (!volumeLoweredByStopBehavior) {
                            storedVolume = currentPlayerState.volume
                            audioManager.setVolume((storedVolume * 0.3f).coerceIn(0f, 1f))
                            volumeLoweredByStopBehavior = true
                        }
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
        val currentSpeed = audioManager.playerState.value.speed

        if (motionState == MotionState.RUNNING || motionState == MotionState.WALKING) {
            val targetSpeed = if (cadence > 0) {
                val clampedCadence = cadence.coerceIn(80, 200).toFloat()
                val diff = clampedCadence - 120f
                1.0f + (diff * 0.005f * intensity)
            } else {
                1.0f
            }

            if (abs(currentSpeed - targetSpeed) > 0.01f) {
                audioManager.setPlaybackSpeed(targetSpeed)
            }
        } else if (abs(currentSpeed - 1.0f) > 0.01f) {
            audioManager.setPlaybackSpeed(1.0f)
        }
    }
}
