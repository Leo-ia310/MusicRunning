package com.example.chillmusic.logic.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.chillmusic.data.model.PlayerState
import com.example.chillmusic.data.model.Track
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
    
    // Queue management could be here or in ViewModel. Let's keep it simple here.
    private var playlist: List<Track> = emptyList()
    private var currentTrackIndex = -1

    fun getPlayer(): ExoPlayer? = exoPlayer

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
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
                })
            }
        }
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        playlist = tracks
        currentTrackIndex = startIndex.coerceIn(0, tracks.size - 1)
        playTrack(playlist[currentTrackIndex])
    }
    
    fun playTrack(track: Track) {
        scope.launch {
            val uri = if (track.url.startsWith("synth://")) {
                getOrGenerateSynthTrack(track)
            } else {
                Uri.parse(track.url)
            }
            
            withContext(Dispatchers.Main) {
                exoPlayer?.run {
                    setMediaItem(MediaItem.fromUri(uri))
                    prepare()
                    play()
                }
                _playerState.value = _playerState.value.copy(currentTrack = track)
            }
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun next() {
        if (playlist.isEmpty()) return
        // Basic next logic, can be enhanced with shuffle/repeat check
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size
        playTrack(playlist[currentTrackIndex])
    }

    fun previous() {
        if (playlist.isEmpty()) return
        if ((exoPlayer?.currentPosition ?: 0) > 3000) {
            exoPlayer?.seekTo(0)
        } else {
            currentTrackIndex = if (currentTrackIndex - 1 < 0) playlist.size - 1 else currentTrackIndex - 1
            playTrack(playlist[currentTrackIndex])
        }
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
        updateState()
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        stopProgressUpdates()
    }

    private fun handleTrackEnd() {
        // Here we could check repeat mode. Defaulting to next for now.
        next()
    }

    private fun updateState() {
        exoPlayer?.let { player ->
            _playerState.value = _playerState.value.copy(
                isPlaying = player.isPlaying,
                volume = player.volume,
                duration = player.duration.coerceAtLeast(0),
                progress = player.currentPosition
            )
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

    // --- Synthesis Logic ---
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
        
        // Frequencies based on track ID hash or index
        val baseFreq = 220.0 + (track.id.hashCode() % 10) * 30.0
        
        val bufferSize = numSamples * numChannels * (bitsPerSample / 8)
        val audioData = ByteArray(bufferSize)
        val buffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            
            // Envelope (Fade in/out 4s)
            val fadeIn = (t / 4.0).coerceAtMost(1.0)
            val fadeOut = ((durationSec - t) / 4.0).coerceAtMost(1.0)
            val envelope = fadeIn * fadeOut

            // 3 Sine waves
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

    private fun writeWavHeader(fos: FileOutputStream, sampleRate: Int, bitsPerSample: Short, channels: Short, numSamples: Int) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = numSamples * channels * bitsPerSample / 8
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(chunkSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1) // AudioFormat 1 = PCM
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



