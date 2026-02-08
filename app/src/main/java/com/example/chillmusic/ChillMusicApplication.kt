package com.example.chillmusic

import android.app.Application
import com.example.chillmusic.data.repository.MusicRepository
import com.example.chillmusic.data.repository.SettingsRepository
import com.example.chillmusic.logic.audio.AudioPlayerManager
import com.example.chillmusic.logic.sensor.MotionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel 

class ChillMusicApplication : Application() {

    // Manual Dependency Injection container
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    lateinit var musicRepository: MusicRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var audioPlayerManager: AudioPlayerManager
    lateinit var motionDetector: MotionDetector

    override fun onCreate() {
        super.onCreate()
        
        musicRepository = MusicRepository(this)
        settingsRepository = SettingsRepository(this)
        
        // These managers need scope for coroutines
        audioPlayerManager = AudioPlayerManager(this, applicationScope)
        motionDetector = MotionDetector(this, applicationScope)
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }
}
