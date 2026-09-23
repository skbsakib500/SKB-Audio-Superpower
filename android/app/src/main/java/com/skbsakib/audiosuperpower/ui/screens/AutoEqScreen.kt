package com.skbsakib.audiosuperpower.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skbsakib.audiosuperpower.autoeq.AutoEqController
import com.skbsakib.audiosuperpower.autoeq.AutoEqProfiles

@Composable
fun AutoEqScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val st by AutoEqController.state.collectAsStateWithLifecycle()

    val grouped = remember { AutoEqProfiles.byBrand() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Color(0xFF0A1220))))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp).clickable { onClose() })
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("AUTOEQ LAB",
                    color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Text("HEADPHONE CORRECTION",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Filled.Headphones, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
        }

        Spacer(Modifier.height(20.dp))

        // Master toggle
        Surface(color = Color(0x1A00E5FF), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AUTOEQ",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                    val active = AutoEqProfiles.byId(st.profileId)
                    Text(if (st.enabled) "${active?.brand} ${active?.model}"
                         else "BYPASSED",
                        color = if (st.enabled) Color.White else Color(0xFF7A8FA6),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace)
                }
                Switch(
                    checked = st.enabled,
                    onCheckedChange = { AutoEqController.setEnabled(ctx, it) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Profiles by brand
        grouped.forEach { (brand, profiles) ->
            Text(brand.uppercase(),
                color = Color(0xFF4A6272), fontSize = 10.sp,
                letterSpacing = 4.sp, fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))

            profiles.forEach { p ->
                val active = st.profileId == p.id && st.enabled
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color(0x1A00E5FF),
                    shape = RoundedCornerShape(10.dp),
                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { AutoEqController.toggleProfile(ctx, p.id) }
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.model,
                                color = if (active) MaterialTheme.colorScheme.primary else Color.White,
                                fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(2.dp))
                            Text("preamp %.1f dB · %d filters".format(p.preampDb, p.filters.size),
                                color = Color(0xFF7A8FA6), fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                        if (active) {
                            Text("ACTIVE",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp, letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("profiles are representative approximations\nAutoEQ project schema · import-ready\ncreator · SKB Sakib",
            color = Color(0xFF3D5266), fontSize = 9.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp)
    }
}
