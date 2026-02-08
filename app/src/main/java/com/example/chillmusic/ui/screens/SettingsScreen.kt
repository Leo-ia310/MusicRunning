package com.example.chillmusic.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chillmusic.data.model.StopBehavior
import com.example.chillmusic.ui.MainViewModel
import com.example.chillmusic.ui.theme.ButtonGray
import com.example.chillmusic.ui.theme.NetflixRed
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val motionSettings = uiState.settings.motion

    // Permission handling using Accompanist
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    // Update VM with permission status
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        viewModel.updatePermissionsStatus(
            motion = true, // We assume accelerometer is always available/granted for now
            location = permissionsState.allPermissionsGranted
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Language
        SectionTitle("Language")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LanguageButton(
                text = "English",
                selected = uiState.settings.language == "en",
                onClick = { viewModel.updateLanguage("en") }
            )
            LanguageButton(
                text = "Español",
                selected = uiState.settings.language == "es",
                onClick = { viewModel.updateLanguage("es") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Running Mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Running Mode", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Play music only while moving", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = motionSettings.enabled,
                onCheckedChange = { 
                    if (it && !permissionsState.allPermissionsGranted) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                    viewModel.toggleMotionDetection(it) 
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NetflixRed,
                    checkedTrackColor = ButtonGray,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Black
                )
            )
        }

        if (motionSettings.enabled) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sensitivity
            Text("Sensitivity", color = Color.White)
            Slider(
                value = motionSettings.sensitivity.toFloat(),
                onValueChange = { viewModel.updateMotionSettings(motionSettings.copy(sensitivity = it.toInt())) },
                valueRange = 1f..10f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = NetflixRed,
                    activeTrackColor = NetflixRed,
                    inactiveTrackColor = ButtonGray
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Less sensitive", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text("More sensitive", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stop Behavior
            Text("When you stop moving:", color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
            RadioOption(
                text = "Pause music",
                subtext = "Resume when you start moving again",
                selected = motionSettings.stopBehavior == StopBehavior.PAUSE,
                onClick = { viewModel.updateMotionSettings(motionSettings.copy(stopBehavior = StopBehavior.PAUSE)) }
            )
            RadioOption(
                text = "Lower volume",
                subtext = "Reduce to 30% and restore on movement",
                selected = motionSettings.stopBehavior == StopBehavior.LOWER_VOLUME,
                onClick = { viewModel.updateMotionSettings(motionSettings.copy(stopBehavior = StopBehavior.LOWER_VOLUME)) }
            )
            RadioOption(
                text = "Skip to next track",
                subtext = "Change song and pause",
                selected = motionSettings.stopBehavior == StopBehavior.NEXT_TRACK,
                onClick = { viewModel.updateMotionSettings(motionSettings.copy(stopBehavior = StopBehavior.NEXT_TRACK)) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Permissions Status
        SectionTitle("Permissions")
        PermissionItem("Motion Sensors", true) // Assuming always available or implied
        PermissionItem("Location", permissionsState.allPermissionsGranted)
        
        if (!permissionsState.allPermissionsGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGray)
            ) {
                Text("Grant Permissions", color = Color.White)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun LanguageButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) NetflixRed.copy(alpha = 0.1f) else ButtonGray)
            .border(1.dp, if (selected) NetflixRed.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun RadioOption(text: String, subtext: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = NetflixRed, unselectedColor = Color.Gray)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text, color = Color.White)
            Text(subtext, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PermissionItem(name: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (granted) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (granted) Color.Green else Color.Red,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (granted) "Granted" else "Not granted",
                color = if (granted) Color.Green else Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
