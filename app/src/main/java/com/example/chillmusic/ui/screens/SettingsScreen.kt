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
import com.google.accompanist.permissions.isGranted

import com.example.chillmusic.ui.utils.Translation

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val motionSettings = uiState.settings.motion
    val lang = uiState.settings.language

    // Permission handling using Accompanist
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            "android.permission.ACTIVITY_RECOGNITION"
        )
    )
    
    // Update VM with permission status
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        val motionPermissionGranted = permissionsState.permissions.any { 
            it.permission == "android.permission.ACTIVITY_RECOGNITION" && it.status.isGranted 
        } || android.os.Build.VERSION.SDK_INT < 29 // Android 10+ needs explicit permission
        
        viewModel.updatePermissionsStatus(
            motion = motionPermissionGranted,
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
            text = Translation.getString("settings", lang),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Language
        SectionTitle(Translation.getString("language", lang))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LanguageButton(
                text = Translation.getString("english", lang),
                selected = uiState.settings.language == "en",
                onClick = { viewModel.updateLanguage("en") }
            )
            LanguageButton(
                text = Translation.getString("spanish", lang),
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
                Text(Translation.getString("running_mode", lang), color = Color.White, fontWeight = FontWeight.Bold)
                Text(Translation.getString("play_only_moving", lang), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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
            
            // Auto Playback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Translation.getString("auto_playback", lang), color = Color.White)
                Switch(
                    checked = motionSettings.autoPlayEnabled,
                    onCheckedChange = { viewModel.updateMotionSettings(motionSettings.copy(autoPlayEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NetflixRed,
                        checkedTrackColor = ButtonGray,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Black
                    )
                )
            }
            
            // Speed Sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Translation.getString("speed_sync", lang), color = Color.White)
                Switch(
                    checked = motionSettings.syncSpeedEnabled,
                    onCheckedChange = { viewModel.updateMotionSettings(motionSettings.copy(syncSpeedEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NetflixRed,
                        checkedTrackColor = ButtonGray,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Black
                    )
                )
            }

            if (motionSettings.syncSpeedEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(Translation.getString("sync_intensity", lang), color = Color.White)
                Slider(
                    value = motionSettings.syncIntensity,
                    onValueChange = { viewModel.updateMotionSettings(motionSettings.copy(syncIntensity = it)) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = NetflixRed,
                        activeTrackColor = NetflixRed,
                        inactiveTrackColor = ButtonGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Sensitivity
            Text(Translation.getString("sensitivity", lang), color = Color.White)
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
                Text(Translation.getString("less_sensitive", lang), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text(Translation.getString("more_sensitive", lang), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stop Behavior
            Text(Translation.getString("when_stop_moving", lang), color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
            RadioOption(
                text = Translation.getString("pause_music", lang),
                subtext = Translation.getString("resume_moving", lang),
                selected = motionSettings.stopBehavior == StopBehavior.PAUSE,
                onClick = { viewModel.updateMotionSettings(motionSettings.copy(stopBehavior = StopBehavior.PAUSE)) }
            )
            RadioOption(
                text = Translation.getString("lower_volume", lang),
                subtext = Translation.getString("reduce_30", lang),
                selected = motionSettings.stopBehavior == StopBehavior.LOWER_VOLUME,
                onClick = { viewModel.updateMotionSettings(motionSettings.copy(stopBehavior = StopBehavior.LOWER_VOLUME)) }
            )
            RadioOption(
                text = Translation.getString("skip_next", lang),
                subtext = Translation.getString("change_song_pause", lang),
                selected = motionSettings.stopBehavior == StopBehavior.NEXT_TRACK,
                onClick = { viewModel.updateMotionSettings(motionSettings.copy(stopBehavior = StopBehavior.NEXT_TRACK)) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Permissions Status
        SectionTitle(Translation.getString("permissions", lang))
        PermissionItem(Translation.getString("motion_sensors", lang), true, lang) // Assuming always available or implied
        PermissionItem(Translation.getString("location", lang), permissionsState.allPermissionsGranted, lang)
        
        if (!permissionsState.allPermissionsGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGray)
            ) {
                Text(Translation.getString("grant_permissions", lang), color = Color.White)
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
fun PermissionItem(name: String, granted: Boolean, lang: String) {
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
                if (granted) Translation.getString("granted", lang) else Translation.getString("not_granted", lang),
                color = if (granted) Color.Green else Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
