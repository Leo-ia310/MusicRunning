package com.example.chillmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chillmusic.ui.theme.RedPrimary

@Composable
fun SettingsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Running Threshold", color = RedPrimary, fontWeight = FontWeight.Bold)
        Text("Current: 5.0 km/h (Fixed in Prototype)", color = Color.Gray, fontSize = 12.sp)
        Slider(
            value = 0.5f,
            onValueChange = {}, 
            colors = SliderDefaults.colors(thumbColor = RedPrimary, activeTrackColor = RedPrimary),
            enabled = false // Disabled for now to prevent confusion until wired up
        )
        Text("Sensitivity adjustment coming in v1.1", color = Color.DarkGray, fontSize = 10.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { navController.navigate("licences") },
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Licences & Credits", color = Color.White)
        }
    }
}

@Composable
fun LicenseScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Legal Info", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Music Licences", color = RedPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This app uses music under the COMA Music License (Free Copyright) or User Generated Content locally stored on the device.",
            color = Color.White
        )
         Spacer(modifier = Modifier.height(16.dp))
         Text(
            "Any user-uploaded music is the sole responsibility of the user.",
            color = Color.Gray,
            fontSize = 12.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("App Credits", color = RedPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Developed by Antigravity AI for Music+Running Enthusiasts.",
            color = Color.White
        )
    }
}
