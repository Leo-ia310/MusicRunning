package com.example.chillmusic.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chillmusic.ui.MainViewModel
import com.example.chillmusic.ui.components.MotionIndicator
import com.example.chillmusic.ui.components.PlayerControls
import com.example.chillmusic.ui.components.Speedometer
import com.example.chillmusic.ui.theme.NetflixRed

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp), // padding for bottom nav
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // App Title
        Text(
            text = buildAnnotatedString {
                append("Chill Music")
                withStyle(SpanStyle(color = NetflixRed)) {
                    append(" While Running")
                }
            },
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Move to the beat",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Speedometer
        Speedometer(speedMs = uiState.currentSpeed)

        // Motion Indicator
        MotionIndicator(
            motionState = uiState.motionState,
            enabled = uiState.settings.motion.enabled,
            permissionsGranted = uiState.permissionsGranted.motion && uiState.permissionsGranted.location 
        )

        // Player Section
        PlayerControls(
            playerState = uiState.player,
            onPlayPause = viewModel::togglePlayPause,
            onNext = viewModel::nextTrack,
            onPrev = viewModel::prevTrack,
            onVolumeChange = viewModel::setVolume
        )
    }
}
