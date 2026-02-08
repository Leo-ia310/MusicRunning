package com.example.chillmusic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chillmusic.ui.theme.NetflixRed
import com.example.chillmusic.ui.theme.StatusWalking
import com.example.chillmusic.ui.theme.StatusStopped
import java.util.Locale

@Composable
fun Speedometer(speedMs: Float) {
    val speedKmh = speedMs * 3.6f
    
    val color = when {
        speedKmh < 2 -> StatusStopped
        speedKmh < 8 -> StatusWalking
        else -> NetflixRed
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Text(
            text = String.format(Locale.US, "%.1f", speedKmh),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = (-2).sp
        )
        Text(
            text = "KM/H",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 2.sp
        )
        Text(
            text = "SPEED",
            fontSize = 10.sp,
            color = com.example.chillmusic.ui.theme.MutedText,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
