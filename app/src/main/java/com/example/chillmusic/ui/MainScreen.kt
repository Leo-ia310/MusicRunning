package com.example.chillmusic.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chillmusic.ui.components.BottomNavigationBar
import com.example.chillmusic.ui.screens.HomeScreen
import com.example.chillmusic.ui.screens.LibraryScreen
import com.example.chillmusic.ui.screens.SettingsScreen

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentTab = uiState.activeTab,
                onTabSelected = { index ->
                    viewModel.onTabSelected(index)
                    when (index) {
                        0 -> navController.navigate("home") { launchSingleTop = true }
                        1 -> navController.navigate("library") { launchSingleTop = true }
                        2 -> navController.navigate("settings") { launchSingleTop = true }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(viewModel)
            }
            composable("library") {
                LibraryScreen(viewModel)
            }
            composable("settings") {
                SettingsScreen(viewModel)
            }
        }
    }
}
