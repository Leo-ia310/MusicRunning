package com.example.chillmusic

import android.app.Application
import android.content.Intent
import com.example.chillmusic.data.repository.FitnessRepository
import com.example.chillmusic.data.repository.MusicRepository
import com.example.chillmusic.data.repository.SettingsRepository
import com.example.chillmusic.logic.audio.AudioPlayerManager
import com.example.chillmusic.logic.sensor.MotionDetector
import com.example.chillmusic.logic.sync.SyncEngine
import com.example.chillmusic.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ChillMusicApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var musicRepository: MusicRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var fitnessRepository: FitnessRepository
    lateinit var audioPlayerManager: AudioPlayerManager
    lateinit var motionDetector: MotionDetector
    lateinit var syncEngine: SyncEngine

    override fun onCreate() {
        super.onCreate()

        musicRepository = MusicRepository(this)
        settingsRepository = SettingsRepository(this)
        fitnessRepository = FitnessRepository(this)
        audioPlayerManager = AudioPlayerManager(this, applicationScope)
        motionDetector = MotionDetector(this, fitnessRepository)
        syncEngine = SyncEngine(applicationScope, audioPlayerManager, motionDetector, settingsRepository)
    }

    fun ensureMusicServiceStarted() {
        startService(Intent(this, MusicService::class.java))
    }

    override fun onTerminate() {
        audioPlayerManager.release()
        super.onTerminate()
        applicationScope.cancel()
    }
}
