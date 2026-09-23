package com.skbsakib.audiosuperpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.skbsakib.audiosuperpower.settings.SkbSettings
import com.skbsakib.audiosuperpower.ui.theme.AccentPalette

@Composable
fun SettingsScreen(
    settings: SkbSettings,
    onBack: () -> Unit,
    onAccentChange: (String) -> Unit,
    onHapticsChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Color(0xFF0A1220))))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp).clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Text("SETTINGS", color = Color.White, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 4.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(28.dp))

        SectionLabel("ACCENT COLOR")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AccentPalette.entries.forEach { p ->
                val selected = p.id == settings.accentColor
                Surface(
                    color = p.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(if (selected) 44.dp else 36.dp)
                        .clickable { onAccentChange(p.id) },
                    border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, Color.White) else null
                ) {}
            }
        }

        Spacer(Modifier.height(30.dp))

        SectionLabel("HAPTIC FEEDBACK")
        Spacer(Modifier.height(8.dp))
        Surface(color = Color(0x1100E5FF), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vibration on interaction",
                    color = Color(0xFFB0C2D0), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Switch(
                    checked = settings.hapticsEnabled,
                    onCheckedChange = onHapticsChange
                )
            }
        }

        Spacer(Modifier.height(30.dp))
        SectionLabel("ABOUT")
        Spacer(Modifier.height(8.dp))
        Surface(color = Color(0x1100E5FF), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("SKB Audio Superpower", color = Color.White,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text("v0.2.0-alpha.1 · 2080 AUDIO LAB",
                    color = Color(0xFF7A8FA6), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text("creator . SKB Sakib · skbsakib500",
                    color = Color(0xFF3D5266), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF4A6272), fontSize = 10.sp,
        letterSpacing = 4.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
}
