package com.example.chillmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chillmusic.data.model.MotionState
import com.example.chillmusic.ui.theme.ButtonGray
import com.example.chillmusic.ui.theme.NetflixRed
import com.example.chillmusic.ui.theme.StatusRunning
import com.example.chillmusic.ui.theme.StatusStopped
import com.example.chillmusic.ui.theme.StatusWalking

@Composable
fun MotionIndicator(
    motionState: MotionState,
    enabled: Boolean,
    permissionsGranted: Boolean
) {
    if (!enabled) {
        DisabledMotionIndicator()
        return
    }

    val (color, icon, text, bars) = when (motionState) {
        MotionState.STOPPED -> Quad(StatusStopped, Icons.Filled.StopCircle, "Stopped", listOf(Color.Gray, Color.Gray, Color.Gray))
        MotionState.WALKING -> Quad(StatusWalking, Icons.Filled.DirectionsWalk, "Walking", listOf(StatusWalking, StatusWalking, Color.Gray))
        MotionState.RUNNING -> Quad(StatusRunning, Icons.Filled.DirectionsRun, "Running", listOf(StatusWalking, StatusWalking, StatusRunning))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        if (motionState != MotionState.STOPPED) {
            PulseAnimation(modifier = Modifier.matchParentSize(), color = color)
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = color
                    )
                    Text(
                        text = when(motionState) {
                            MotionState.STOPPED -> "Start moving to play music"
                            MotionState.WALKING -> "Walking pace detected"
                            MotionState.RUNNING -> "Running pace detected"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            // Bars
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.width(6.dp).height(12.dp).background(bars[0], RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.width(6.dp).height(20.dp).background(bars[1], RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.width(6.dp).height(28.dp).background(bars[2], RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
fun DisabledMotionIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(ButtonGray, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Motion Detection Off", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
            Text("Enable in Settings", color = Color.DarkGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
