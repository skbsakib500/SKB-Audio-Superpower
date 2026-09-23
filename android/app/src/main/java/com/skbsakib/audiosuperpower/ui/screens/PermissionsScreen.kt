package com.skbsakib.audiosuperpower.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val perms = buildList {
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_AUDIO)
        else add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val audioOk = result[perms.firstOrNull { it.contains("MEDIA_AUDIO") || it.contains("EXTERNAL") }] ?: false
        if (audioOk) onAllGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Color(0xFF0A1220))))
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "PERMISSIONS",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            letterSpacing = 6.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "REQUIRED",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(30.dp))

        PermCard("Audio Library", "Read your music files from device storage")
        Spacer(Modifier.height(10.dp))
        PermCard("Notifications", "Show playback controls on lock screen")

        Spacer(Modifier.height(34.dp))
        Button(
            onClick = { launcher.launch(perms) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color(0xFF001318)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("GRANT", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PermCard(title: String, desc: String) {
    Surface(
        color = Color(0x1100E5FF),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            Text(desc, color = Color(0xFF7A8FA6), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
