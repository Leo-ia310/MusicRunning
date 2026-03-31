package com.example.chillmusic.logic.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.chillmusic.data.model.PlayerErrorType
import com.example.chillmusic.data.model.PlayerState
import com.example.chillmusic.data.model.RepeatMode
import com.example.chillmusic.data.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class AudioPlayerManager(private val context: Context, private val scope: CoroutineScope) {

    private var exoPlayer: ExoPlayer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private var loadTrackJob: Job? = null
    private var playlist: List<Track> = emptyList()
    private var currentTrackIndex = -1

    fun getPlayer(): ExoPlayer? {
        initPlayer()
        return exoPlayer
    }

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (exoPlayer != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            handleTrackEnd()
                        }
                        updateState()
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateState()
                        if (isPlaying) startProgressUpdates() else stopProgressUpdates()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        stopProgressUpdates()
                        setError(mapError(error), error.message ?: error.errorCodeName)
                        updateState()
                    }
                })
            }
    }

    fun refreshPlaylist(tracks: List<Track>) {
        initPlayer()
        val previousTrackId = _playerState.value.currentTrack?.id
        val wasPlaying = exoPlayer?.isPlaying == true
        val snapshot = PlaylistCoordinator.sync(tracks, previousTrackId)

        playlist = snapshot.tracks
        currentTrackIndex = snapshot.currentIndex

        if (snapshot.currentTrack == null) {
            loadTrackJob?.cancel()
            currentTrackIndex = -1
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            _playerState.value = PlayerState(
                volume = exoPlayer?.volume ?: _playerState.value.volume,
                speed = exoPlayer?.playbackParameters?.speed ?: _playerState.value.speed,
                repeatMode = _playerState.value.repeatMode
            )
            return
        }

        val currentTrack = snapshot.currentTrack
        val trackChanged = previousTrackId != currentTrack.id

        _playerState.value = _playerState.value.copy(
            currentTrack = currentTrack,
            errorType = null,
            errorDetail = null
        )

        when {
            trackChanged && wasPlaying -> playTrack(currentTrack, snapshot.tracks)
            trackChanged -> prepareTrack(currentTrack, snapshot.tracks)
            exoPlayer?.mediaItemCount == 0 -> prepareTrack(currentTrack, snapshot.tracks)
        }
    }

    fun prepareTrack(track: Track, playlistContext: List<Track> = playlist) {
        initPlayer()
        val snapshot = updateSelection(track, playlistContext)
        loadTrack(snapshot.currentTrack ?: track, playWhenReady = false)
    }

    fun playTrack(track: Track, playlistContext: List<Track> = playlist) {
        initPlayer()
        val snapshot = updateSelection(track, playlistContext)
        loadTrack(snapshot.currentTrack ?: track, playWhenReady = true)
    }

    fun play() {
        initPlayer()
        val player = exoPlayer ?: return
        clearError()

        if (playlist.isEmpty()) {
            setError(PlayerErrorType.NO_TRACKS, null)
            return
        }

        when {
            player.mediaItemCount == 0 && currentTrackIndex >= 0 -> {
                playTrack(playlist[currentTrackIndex], playlist)
            }

            player.mediaItemCount > 0 && player.playbackState == Player.STATE_IDLE -> {
                player.prepare()
                player.play()
            }

            player.mediaItemCount > 0 -> {
                player.play()
            }

            else -> playTrack(playlist.first(), playlist)
        }
    }

    fun pause() {
        initPlayer()
        exoPlayer?.pause()
    }

    fun next() {
        if (playlist.isEmpty()) {
            setError(PlayerErrorType.NO_TRACKS, null)
            return
        }
        val snapshot = PlaylistCoordinator.next(playlist, currentTrackIndex)
        currentTrackIndex = snapshot.currentIndex
        playTrack(snapshot.currentTrack ?: return, snapshot.tracks)
    }

    fun previous() {
        if (playlist.isEmpty()) {
            setError(PlayerErrorType.NO_TRACKS, null)
            return
        }
        if ((exoPlayer?.currentPosition ?: 0) > 3000) {
            exoPlayer?.seekTo(0)
        } else {
            val snapshot = PlaylistCoordinator.previous(playlist, currentTrackIndex)
            currentTrackIndex = snapshot.currentIndex
            playTrack(snapshot.currentTrack ?: return, snapshot.tracks)
        }
    }

    fun setVolume(volume: Float) {
        initPlayer()
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
        updateState()
    }

    fun setPlaybackSpeed(speed: Float) {
        initPlayer()
        val currentParams = exoPlayer?.playbackParameters ?: PlaybackParameters.DEFAULT
        exoPlayer?.playbackParameters = currentParams.withSpeed(speed.coerceIn(0.5f, 2.0f))
        updateState()
    }

    fun seekTo(position: Long) {
        initPlayer()
        exoPlayer?.seekTo(position)
        updateState()
    }

    fun toggleRepeatMode() {
        initPlayer()
        val newMode = when (_playerState.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        _playerState.value = _playerState.value.copy(repeatMode = newMode)
    }

    fun release() {
        loadTrackJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        stopProgressUpdates()
    }

    private fun updateSelection(track: Track, playlistContext: List<Track>): PlaylistSnapshot {
        val basePlaylist = playlistContext.ifEmpty { playlist }
        val snapshot = PlaylistCoordinator.select(basePlaylist, track)
        playlist = snapshot.tracks
        currentTrackIndex = snapshot.currentIndex
        _playerState.value = _playerState.value.copy(
            currentTrack = snapshot.currentTrack ?: track,
            errorType = null,
            errorDetail = null
        )
        return snapshot
    }

    private fun loadTrack(track: Track, playWhenReady: Boolean) {
        loadTrackJob?.cancel()
        clearError()

        loadTrackJob = scope.launch {
            try {
                val uri = resolveTrackUri(track)
                withContext(Dispatchers.Main) {
                    exoPlayer?.run {
                        setMediaItem(MediaItem.fromUri(uri))
                        prepare()
                        if (playWhenReady) {
                            play()
                        }
                    }
                    updateState()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    setError(PlayerErrorType.SOURCE_UNAVAILABLE, error.message)
                }
            }
        }
    }

    private suspend fun resolveTrackUri(track: Track): Uri {
        return when {
            track.url.startsWith("synth://") -> getOrGenerateSynthTrack(track)
            track.url.contains("://") -> Uri.parse(track.url)
            else -> Uri.fromFile(File(track.url))
        }
    }

    private fun handleTrackEnd() {
        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }

            RepeatMode.ALL -> next()

            RepeatMode.NONE -> {
                if (currentTrackIndex < playlist.size - 1) {
                    next()
                } else {
                    exoPlayer?.seekTo(0)
                    pause()
                }
            }
        }
    }

    private fun updateState() {
        val player = exoPlayer ?: return
        _playerState.value = _playerState.value.copy(
            isPlaying = player.isPlaying,
            volume = player.volume,
            speed = player.playbackParameters.speed,
            duration = player.duration.coerceAtLeast(0),
            progress = player.currentPosition,
            repeatMode = _playerState.value.repeatMode,
            errorType = _playerState.value.errorType,
            errorDetail = _playerState.value.errorDetail
        )
    }

    private fun clearError() {
        _playerState.value = _playerState.value.copy(
            errorType = null,
            errorDetail = null
        )
    }

    private fun setError(type: PlayerErrorType, detail: String?) {
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            errorType = type,
            errorDetail = detail
        )
    }

    private fun mapError(error: PlaybackException): PlayerErrorType {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> PlayerErrorType.SOURCE_UNAVAILABLE

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> PlayerErrorType.UNSUPPORTED_FORMAT

            else -> PlayerErrorType.PLAYBACK_FAILED
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updateState()
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
    }

    private suspend fun getOrGenerateSynthTrack(track: Track): Uri = withContext(Dispatchers.IO) {
        val filename = "${track.id}.wav"
        val file = File(context.cacheDir, filename)

        if (!file.exists()) {
            generateWavFile(file, track)
        }
        Uri.fromFile(file)
    }

    private fun generateWavFile(file: File, track: Track) {
        val sampleRate = 22050
        val durationSec = track.duration / 1000
        val numSamples = (durationSec * sampleRate).toInt()
        val numChannels = 1
        val bitsPerSample = 16
        val baseFreq = 220.0 + (track.id.hashCode() % 10) * 30.0

        val bufferSize = numSamples * numChannels * (bitsPerSample / 8)
        val audioData = ByteArray(bufferSize)
        val buffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val fadeIn = (t / 4.0).coerceAtMost(1.0)
            val fadeOut = ((durationSec - t) / 4.0).coerceAtMost(1.0)
            val envelope = fadeIn * fadeOut

            val w1 = sin(2.0 * Math.PI * baseFreq * t) * 0.4
            val w2 = sin(2.0 * Math.PI * (baseFreq * 1.5) * t) * 0.2
            val w3 = sin(2.0 * Math.PI * (baseFreq / 2.0) * t) * 0.2

            val signal = (w1 + w2 + w3) * envelope
            val value = (signal * 32767).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(value)
        }

        FileOutputStream(file).use { fos ->
            writeWavHeader(fos, sampleRate, bitsPerSample.toShort(), numChannels.toShort(), numSamples)
            fos.write(audioData)
        }
    }

    private fun writeWavHeader(
        fos: FileOutputStream,
        sampleRate: Int,
        bitsPerSample: Short,
        channels: Short,
        numSamples: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = numSamples * channels * bitsPerSample / 8
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(chunkSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(channels)
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign)
        header.putShort(bitsPerSample)
        header.put("data".toByteArray())
        header.putInt(dataSize)

        fos.write(header.array())
    }
}
