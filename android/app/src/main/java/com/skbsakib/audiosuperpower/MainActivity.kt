package com.skbsakib.audiosuperpower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skbsakib.audiosuperpower.ui.theme.SkbTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SkbTheme { Surface(Modifier.fillMaxSize(), color = Color.Black) { Home() } } }
    }
}

@Composable
private fun Home() {
    val nv = remember { runCatching { NativeBridge.nativeVersion() }.getOrElse { "native: not loaded" } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Color(0xFF0A1220), Color(0xFF050810))))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SKB AUDIO", color = Color(0xFF00E5FF), fontSize = 12.sp, letterSpacing = 8.sp,
            fontWeight = FontWeight.Light, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Text("SUPERPOWER", color = Color.White, fontSize = 32.sp, letterSpacing = 4.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text("2080 AUDIO LAB", color = Color(0xFF7A8FA6), fontSize = 11.sp, letterSpacing = 6.sp,
            fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(48.dp))
        Surface(color = Color(0x1100E5FF), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                SL("PHASE", "1B . SKELETON")
                SL("BUILD", "OK")
                SL("NATIVE", nv)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("creator . SKB Sakib\nskbsakib500", color = Color(0xFF3D5266), fontSize = 10.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SL(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, color = Color(0xFF4A6272), fontSize = 10.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color(0xFF00E5FF), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}
