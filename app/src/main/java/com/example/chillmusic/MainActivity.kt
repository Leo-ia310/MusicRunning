package com.example.chillmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chillmusic.data.MotionRepository
import com.example.chillmusic.data.MusicRepository
import com.example.chillmusic.ui.MainViewModel
import com.example.chillmusic.ui.screens.HomeScreen
import com.example.chillmusic.ui.screens.LibraryScreen
import com.example.chillmusic.ui.screens.LicenseScreen
import com.example.chillmusic.ui.screens.SettingsScreen
import com.example.chillmusic.ui.theme.ChillMusicWhileRunningTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(
            applicationContext,
            MusicRepository(applicationContext),
            MotionRepository(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChillMusicWhileRunningTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("library") {
                            LibraryScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("settings") {
                            SettingsScreen(navController = navController)
                        }
                        composable("licences") {
                            LicenseScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
