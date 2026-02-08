package com.example.chillmusic.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.chillmusic.data.MotionRepository
import com.example.chillmusic.data.MusicRepository
import com.example.chillmusic.data.model.Song
import com.example.chillmusic.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

data class UiState(
    val currentSpeed: Float = 0f,
    val isRunningModeEnabled: Boolean = false,
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val appMusic: List<Song> = emptyList(),
    val userMusic: List<Song> = emptyList(),
    val isPermissionGranted: Boolean = false
)

class MainViewModel(
    private val context: Context, // In a real app, use DI.
    private val musicRepository: MusicRepository,
    private val motionRepository: MotionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    init {
        loadMusic()
        initializeController()
        observeMotion()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                }
                
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Update current song from mediaItem
                     super.onMediaItemTransition(mediaItem, reason)
                     // In a real app we'd map mediaItem back to Song
                }
            })
        }, MoreExecutors.directExecutor())
    }

    private fun loadMusic() {
        viewModelScope.launch {
            val app = musicRepository.getAppMusic()
            val user = musicRepository.getUserMusic()
            _uiState.value = _uiState.value.copy(appMusic = app, userMusic = user)
        }
    }

    private fun observeMotion() {
        // Only observe if permission is granted, handled by UI calling this or re-calling
        // For simplicity, we assume permission might be granted later.
    }
    
    fun startLocationUpdates() {
         motionRepository.getLocationFlow()
            .onEach { location ->
                val speed = motionRepository.getSpeedKmh(location)
                _uiState.value = _uiState.value.copy(currentSpeed = speed)
            }
            .launchIn(viewModelScope)
    }

    fun toggleRunningMode() {
        val newMode = !_uiState.value.isRunningModeEnabled
        _uiState.value = _uiState.value.copy(isRunningModeEnabled = newMode)
        
        val intent = Intent(context, MusicService::class.java).apply {
            action = if (newMode) MusicService.ACTION_START_RUNNING_MODE else MusicService.ACTION_STOP_RUNNING_MODE
        }
        context.startService(intent)
    }

    fun playSong(song: Song) {
        val item = MediaItem.fromUri(song.uri)
        mediaController?.setMediaItem(item)
        mediaController?.prepare()
        mediaController?.play()
        _uiState.value = _uiState.value.copy(currentSong = song)
    }
    
    fun togglePlayPause() {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
        } else {
            mediaController?.play()
        }
    }
    
    fun skipNext() {
        mediaController?.seekToNext()
    }
    
    fun skipPrevious() {
        mediaController?.seekToPrevious()
    }

    override fun onCleared() {
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
    
    // Factory for manual DI
    companion object {
        fun provideFactory(
            context: Context,
            musicRepository: MusicRepository,
            motionRepository: MotionRepository
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(context, musicRepository, motionRepository) as T
            }
        }
    }
}
