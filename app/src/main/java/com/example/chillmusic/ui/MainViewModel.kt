package com.example.chillmusic.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chillmusic.ChillMusicApplication
import com.example.chillmusic.data.model.AppSettings
import com.example.chillmusic.data.model.MotionSettings
import com.example.chillmusic.data.model.MotionState
import com.example.chillmusic.data.model.PlayerState
import com.example.chillmusic.data.model.Track
import com.example.chillmusic.data.repository.MusicRepository
import com.example.chillmusic.data.repository.SettingsRepository
import com.example.chillmusic.logic.audio.AudioPlayerManager
import com.example.chillmusic.logic.sensor.MotionDetector
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
    val userTracks: List<Track> = emptyList(),
    val activeTab: String = "home",
    val permissionsGranted: PermissionsState = PermissionsState(),
    val stepCadence: Int = 0,
    val todaySteps: Int = 0
)

data class LocalUiState(
    val userTracks: List<Track> = emptyList(),
    val activeTab: String = "home",
    val permissionsGranted: PermissionsState = PermissionsState()
)

data class PermissionsState(
    val motion: Boolean = false,
    val location: Boolean = false
) {
    val allGranted: Boolean
        get() = motion && location
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ChillMusicApplication
    private val musicRepo: MusicRepository = app.musicRepository
    private val settingsRepo: SettingsRepository = app.settingsRepository
    private val audioManager: AudioPlayerManager = app.audioPlayerManager
    private val motionDetector: MotionDetector = app.motionDetector
    private val fitnessRepo = app.fitnessRepository
    private val _localUiState = MutableStateFlow(LocalUiState())

    val uiState: StateFlow<UiState> = combine(
        audioManager.playerState,
        motionDetector.motionState,
        motionDetector.currentSpeed,
        motionDetector.stepCadence,
        combine(settingsRepo.settings, fitnessRepo.dailySteps, _localUiState, ::Triple)
    ) { player, motion, speed, cadence, extra ->
        UiState(
            player = player,
            motionState = motion,
            currentSpeed = speed,
            stepCadence = cadence,
            settings = extra.first,
            todaySteps = extra.second,
            userTracks = extra.third.userTracks,
            activeTab = extra.third.activeTab,
            permissionsGranted = extra.third.permissionsGranted
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        loadTracks()
    }

    private fun loadTracks() {
        viewModelScope.launch {
            val userTracks = musicRepo.getUserTracks()
            _localUiState.value = _localUiState.value.copy(userTracks = userTracks)
            audioManager.refreshPlaylist(userTracks)
        }
    }

    fun togglePlayPause() {
        if (audioManager.playerState.value.isPlaying) audioManager.pause() else audioManager.play()
    }

    fun nextTrack() = audioManager.next()

    fun prevTrack() = audioManager.previous()

    fun setVolume(v: Float) = audioManager.setVolume(v)

    fun seekTo(position: Long) = audioManager.seekTo(position)

    fun toggleRepeatMode() = audioManager.toggleRepeatMode()

    fun playTrack(track: Track) {
        audioManager.playTrack(track, _localUiState.value.userTracks)
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
        if (currentSettings.enabled == enable) return

        updateMotionSettings(currentSettings.copy(enabled = enable))

        if (enable) {
            val started = motionDetector.startDetection()
            if (!started) {
                updateMotionSettings(currentSettings.copy(enabled = false))
            }
        } else {
            motionDetector.stopDetection()
        }
    }

    fun onTabSelected(index: Int) {
        val tab = when (index) {
            0 -> "home"
            1 -> "music"
            else -> "settings"
        }
        _localUiState.value = _localUiState.value.copy(activeTab = tab)
    }

    fun updatePermissionsStatus(motion: Boolean, location: Boolean) {
        _localUiState.value = _localUiState.value.copy(
            permissionsGranted = PermissionsState(motion, location)
        )

        if (motion && location && uiState.value.settings.motion.enabled) {
            motionDetector.startDetection()
        } else if (!motion || !location) {
            motionDetector.stopDetection()
        }
    }

    fun addUserTrack(uri: Uri) {
        viewModelScope.launch {
            val newTrack = musicRepo.addUserTrack(uri)
            if (newTrack != null) {
                loadTracks()
            }
        }
    }

    fun addUserTracks(uris: List<Uri>) {
        viewModelScope.launch {
            var anyAdded = false
            uris.forEach { uri ->
                val newTrack = musicRepo.addUserTrack(uri)
                if (newTrack != null) anyAdded = true
            }
            if (anyAdded) {
                loadTracks()
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
