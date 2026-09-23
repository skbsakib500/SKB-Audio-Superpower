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
import com.skbsakib.audiosuperpower.cloud.CloudAuthStore
import com.skbsakib.audiosuperpower.cloud.CloudProvider
import com.skbsakib.audiosuperpower.cloud.CloudProviderRegistry
import kotlinx.coroutines.launch

@Composable
fun CloudSourceScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val providers = remember { CloudProviderRegistry.all() }

    // Per-provider UI state
    var configuredMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var authMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // Simple client-id input state per provider
    var editingId by remember { mutableStateOf<String?>(null) }
    var clientIdInput by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val c = providers.associate { it.id to it.isConfigured(ctx.applicationContext) }
        val a = providers.associate { it.id to it.isAuthenticated(ctx.applicationContext) }
        configuredMap = c
        authMap = a
    }

    LaunchedEffect(Unit) { refresh() }

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
                Text("CLOUD SOURCES",
                    color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace)
                Text("BRING YOUR OWN OAUTH",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Honest banner
        Surface(
            color = Color(0x33FFB300),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x66FFB300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("HONEST SCOPE",
                    color = Color(0xFFFFB300), fontSize = 10.sp,
                    letterSpacing = 3.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(6.dp))
                Text(
                    "These providers require YOUR OWN OAuth client ID. " +
                    "Register an app in each provider's developer console, " +
                    "then paste the client id here. No credentials are " +
                    "hardcoded or transmitted anywhere except to the provider.",
                    color = Color(0xFFB0C2D0), fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        providers.forEach { p ->
            ProviderCard(
                p = p,
                configured = configuredMap[p.id] ?: false,
                authenticated = authMap[p.id] ?: false,
                onEdit = {
                    editingId = p.id
                    clientIdInput = ""
                },
                onSignIn = {
                    scope.launch {
                        val r = p.beginAuth(ctx)
                        statusMsg = r.error ?: "ok"
                        refresh()
                    }
                },
                onSignOut = {
                    scope.launch {
                        p.signOut(ctx)
                        statusMsg = "Signed out of ${p.displayName}"
                        refresh()
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
        }

        statusMsg?.let { msg ->
            Surface(color = Color(0x1A00E5FF), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()) {
                Text(msg, color = Color(0xFFB0C2D0),
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp), lineHeight = 15.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("credentials never committed · user-owned OAuth\ncreator · SKB Sakib",
            color = Color(0xFF3D5266), fontSize = 10.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
    }

    // Client id entry dialog
    editingId?.let { providerId ->
        val provider = providers.first { it.id == providerId }
        AlertDialog(
            onDismissRequest = { editingId = null },
            containerColor = Color(0xFF0E1826),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB0C2D0),
            title = {
                Text("Configure ${provider.displayName}",
                    fontFamily = FontFamily.Monospace, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Paste your OAuth client id:",
                        color = Color(0xFF7A8FA6), fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = clientIdInput,
                        onValueChange = { clientIdInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0x3300E5FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        CloudAuthStore.setClientId(ctx.applicationContext, providerId, clientIdInput.trim())
                        statusMsg = "${provider.displayName} client id saved. Full OAuth wiring is scheduled for a future phase."
                        editingId = null
                        refresh()
                    }
                }) {
                    Text("SAVE",
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingId = null }) {
                    Text("CANCEL", color = Color(0xFF7A8FA6),
                        fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        )
    }
}

@Composable
private fun ProviderCard(
    p: CloudProvider,
    configured: Boolean,
    authenticated: Boolean,
    onEdit: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val accent = when {
        authenticated -> Color(0xFFB4FF39)
        configured    -> MaterialTheme.colorScheme.primary
        else          -> Color(0xFF7A8FA6)
    }
    Surface(
        color = Color(0x0D00E5FF),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.displayName, color = Color.White,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(2.dp))
                    Text(p.description, color = Color(0xFF7A8FA6),
                        fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Text(
                    when {
                        authenticated -> "CONNECTED"
                        configured    -> "READY"
                        else          -> "NOT CONFIGURED"
                    },
                    color = accent, fontSize = 9.sp, letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction(
                    label = if (configured) "EDIT ID" else "SET CLIENT ID",
                    onClick = onEdit
                )
                if (configured && !authenticated) {
                    SmallAction(label = "SIGN IN", onClick = onSignIn, primary = true)
                }
                if (authenticated) {
                    SmallAction(label = "SIGN OUT", onClick = onSignOut, danger = true)
                }
            }
        }
    }
}

@Composable
private fun SmallAction(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    danger: Boolean = false
) {
    val color = when {
        danger  -> Color(0xFFFF4D4D)
        primary -> MaterialTheme.colorScheme.primary
        else    -> Color(0xFF7A8FA6)
    }
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(label, color = color, fontSize = 10.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
