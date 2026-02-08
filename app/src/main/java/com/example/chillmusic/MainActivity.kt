package com.example.chillmusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.chillmusic.service.MusicService
import com.example.chillmusic.ui.MainScreen
import com.example.chillmusic.ui.MainViewModel
import com.example.chillmusic.ui.theme.ChillMusicWhileRunningTheme

class MainActivity : ComponentActivity() {
    
    // Using default ViewModel factory which works with AndroidViewModel
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the service to ensure it's alive (optional here if we bind later, but good for foreground persistence)
        // Actually, Media3 session service is usually started by MediaController or explicit intent
        // We'll start it to ensure it runs
        startService(Intent(this, MusicService::class.java))

        setContent {
            ChillMusicWhileRunningTheme {
                MainScreen(viewModel)
            }
        }
    }
}
