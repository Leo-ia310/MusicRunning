package com.example.chillmusic.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chillmusic.ChillMusicApplication
import com.example.chillmusic.data.model.AppSettings
import com.example.chillmusic.data.repository.MusicRepository
import com.example.chillmusic.data.repository.SettingsRepository
import com.example.chillmusic.logic.audio.AudioPlayerManager
import com.example.chillmusic.logic.sensor.MotionDetector
import com.example.chillmusic.data.model.MotionSettings
import com.example.chillmusic.data.model.MotionState
import com.example.chillmusic.data.model.PlayerState
import com.example.chillmusic.data.model.StopBehavior
import com.example.chillmusic.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val player: PlayerState = PlayerState(),
    val motionState: MotionState = MotionState.STOPPED,
    val currentSpeed: Float = 0f,
    val settings: AppSettings = AppSettings(),
    val catalog: List<Track> = emptyList(),
    val userTracks: List<Track> = emptyList(),
    val activeTab: String = "home",
    val permissionsGranted: PermissionsState = PermissionsState()
)

data class LocalUiState(
    val catalog: List<Track> = emptyList(),
    val userTracks: List<Track> = emptyList(),
    val activeTab: String = "home",
    val permissionsGranted: PermissionsState = PermissionsState()
)

data class PermissionsState(
    val motion: Boolean = false,
    val location: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ChillMusicApplication
    private val musicRepo: MusicRepository = app.musicRepository
    private val settingsRepo: SettingsRepository = app.settingsRepository
    private val audioManager: AudioPlayerManager = app.audioPlayerManager
    private val motionDetector: MotionDetector = app.motionDetector

    private val _localUiState = MutableStateFlow(LocalUiState())

    // Combined UI State
    val uiState: StateFlow<UiState> = combine(
        audioManager.playerState,
        motionDetector.motionState,
        motionDetector.currentSpeed,
        settingsRepo.settings,
        _localUiState
    ) { player, motion, speed, settings, local ->
        UiState(
            player = player,
            motionState = motion,
            currentSpeed = speed,
            settings = settings,
            catalog = local.catalog,
            userTracks = local.userTracks,
            activeTab = local.activeTab,
            permissionsGranted = local.permissionsGranted
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    // internal tracking for smart playback
    private var previousMotionState = MotionState.STOPPED
    private var storedVolume = 0.8f

    init {
        loadTracks()
        observeMotion()
    }

    private fun loadTracks() {
        viewModelScope.launch {
            val userTracks = musicRepo.getUserTracks()
            val catalog = musicRepo.catalog
            _localUiState.value = _localUiState.value.copy(
                catalog = catalog,
                userTracks = userTracks
            )
            // Initial playlist
            audioManager.setPlaylist(catalog + userTracks)
        }
    }

    private fun observeMotion() {
        viewModelScope.launch {
            uiState.collect { state ->
                val motionEnabled = state.settings.motion.enabled
                val currentMotion = state.motionState

                if (motionEnabled) {
                    handleSmartPlayback(currentMotion, previousMotionState, state)
                }
                
                previousMotionState = currentMotion
            }
        }
    }

    private fun handleSmartPlayback(current: MotionState, previous: MotionState, state: UiState) {
        if (current == previous) return

        val isMoving = current == MotionState.WALKING || current == MotionState.RUNNING
        val wasMoving = previous == MotionState.WALKING || previous == MotionState.RUNNING

        if (isMoving && !wasMoving) {
            // Started moving
            if (storedVolume > 0) {
                audioManager.setVolume(storedVolume)
            }
            if (!state.player.isPlaying && state.player.currentTrack != null) {
                audioManager.play()
            }
        } else if (!isMoving && wasMoving) {
            // Stopped moving
            when (state.settings.motion.stopBehavior) {
                StopBehavior.PAUSE -> {
                    audioManager.pause()
                }
                StopBehavior.LOWER_VOLUME -> {
                    storedVolume = state.player.volume
                    audioManager.setVolume(storedVolume * 0.3f)
                }
                StopBehavior.NEXT_TRACK -> {
                    audioManager.next()
                    audioManager.pause()
                }
            }
        }
    }

    // --- Actions ---

    fun togglePlayPause() {
        if (uiState.value.player.isPlaying) audioManager.pause() else audioManager.play()
    }

    fun nextTrack() = audioManager.next()
    fun prevTrack() = audioManager.previous()
    fun setVolume(v: Float) = audioManager.setVolume(v)

    fun playTrack(track: Track) {
        audioManager.playTrack(track)
    }

    fun updateMotionSettings(newSettings: MotionSettings) {
        settingsRepo.updateMotionSettings(newSettings)
        motionDetector.updateSettings(newSettings)
    }
    
    fun updateLanguage(lang: String) {
        settingsRepo.updateLanguage(lang)
    }

    fun toggleMotionDetection(enable: Boolean) {
        val currentSettings = uiState.value.settings.motion
        // Avoid loop if setting is same
        if (currentSettings.enabled == enable) return
        
        updateMotionSettings(currentSettings.copy(enabled = enable))
        
        if (enable) {
            motionDetector.startDetection()
        } else {
            motionDetector.stopDetection()
        }
    }

    fun onTabSelected(index: Int) {
        val tab = when(index) {
            0 -> "home"
            1 -> "library"
            else -> "settings"
        }
        _localUiState.value = _localUiState.value.copy(activeTab = tab)
    }

    fun updatePermissionsStatus(motion: Boolean, location: Boolean) {
        _localUiState.value = _localUiState.value.copy(
            permissionsGranted = PermissionsState(motion, location)
        )
        // If granted and enabled, ensure detector is running
        if (motion && location && uiState.value.settings.motion.enabled) {
            motionDetector.startDetection()
        }
    }

    fun addUserTrack(uri: Uri) {
        viewModelScope.launch {
            val newTrack = musicRepo.addUserTrack(uri)
            if (newTrack != null) {
                loadTracks() // reload list
            }
        }
    }

    fun removeUserTrack(track: Track) {
        viewModelScope.launch {
            musicRepo.removeUserTrack(track)
            loadTracks()
        }
    }
}
