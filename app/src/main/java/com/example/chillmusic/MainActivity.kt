package com.example.chillmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.chillmusic.ui.MainScreen
import com.example.chillmusic.ui.MainViewModel
import com.example.chillmusic.ui.theme.ChillMusicWhileRunningTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ChillMusicApplication).ensureMusicServiceStarted()

        setContent {
            ChillMusicWhileRunningTheme {
                MainScreen(viewModel)
            }
        }
    }
}
