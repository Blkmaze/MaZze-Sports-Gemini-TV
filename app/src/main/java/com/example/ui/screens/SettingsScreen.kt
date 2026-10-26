package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlaylistConfig
import com.example.model.UpdateInfo
import com.example.ui.components.TvFocusButton
import com.example.ui.theme.*
import java.io.File

@Composable
fun SettingsScreen(
    playlistConfig: PlaylistConfig,
    updateInfo: UpdateInfo,
    spoilerFree: Boolean,
    onSaveConfig: (PlaylistConfig) -> Unit,
    onLoadDemo: () -> Unit,
    onToggleSpoilerFree: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadAndInstall: () -> Unit,
    lastCrashReport: String? = null,
    onClearCrashReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var playlistType by remember { mutableStateOf(playlistConfig.type) }
    var m3uUrl by remember { mutableStateOf(playlistConfig.m3uUrl) }
    var xtreamServer by remember { mutableStateOf(playlistConfig.xtreamServer) }
    var xtreamUser by remember { mutableStateOf(playlistConfig.xtreamUsername) }
    var xtreamPass by remember { mutableStateOf(playlistConfig.xtreamPassword) }
    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Crash report state
    var crashReportText by remember(lastCrashReport) {
        mutableStateOf(
            lastCrashReport ?: try {
                val crashFile = File(context.filesDir, "last_crash.txt")
                if (crashFile.exists()) crashFile.readText() else null
            } catch (e: Throwable) {
                null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvDarkBackground)
            .padding(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Settings & Feeds",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }

        // 1. Playlist Source Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TvDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "IPTV Playlist Source",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvFocusButton(
                            text = "Demo Channels",
                            isPrimary = playlistType == "demo",
                            onClick = {
                                playlistType = "demo"
                                onLoadDemo()
                                saveFeedbackMessage = "Loaded 8 Curated Sports Feeds"
                            }
                        )
                        TvFocusButton(
                            text = "M3U URL",
                            isPrimary = playlistType == "m3u",
                            onClick = { playlistType = "m3u" }
                        )
                        TvFocusButton(
                            text = "Xtream Codes",
                            isPrimary = playlistType == "xtream",
                            onClick = { playlistType = "xtream" }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (playlistType == "m3u") {
                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text("M3U Playlist URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TvOrangeFocus,
                                unfocusedBorderColor = TvTextMuted,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = TvTextPrimary
                            )
                        )
                    } else if (playlistType == "xtream") {
                        OutlinedTextField(
                            value = xtreamServer,
                            onValueChange = { xtreamServer = it },
                            label = { Text("Server URL (http://example.com:8080)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TvOrangeFocus,
                                unfocusedBorderColor = TvTextMuted,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = TvTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = xtreamUser,
                                onValueChange = { xtreamUser = it },
                                label = { Text("Username") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TvOrangeFocus,
                                    unfocusedBorderColor = TvTextMuted,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = TvTextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = xtreamPass,
                                onValueChange = { xtreamPass = it },
                                label = { Text("Password") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TvOrangeFocus,
                                    unfocusedBorderColor = TvTextMuted,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = TvTextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvFocusButton(
                            text = "Save & Apply Playlist",
                            icon = Icons.Default.Save,
                            isPrimary = true,
                            onClick = {
                                val config = PlaylistConfig(
                                    type = playlistType,
                                    m3uUrl = m3uUrl.trim(),
                                    xtreamServer = xtreamServer.trim(),
                                    xtreamUsername = xtreamUser.trim(),
                                    xtreamPassword = xtreamPass.trim(),
                                    spoilerFree = spoilerFree
                                )
                                onSaveConfig(config)
                                saveFeedbackMessage = "Settings saved! Fetching channels..."
                            }
                        )

                        saveFeedbackMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = TvLimeAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Scoreboard & Spoiler Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TvDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Scoreboard Experience",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Spoiler-Free Mode",
                                color = TvTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Hides live and final scores on Home cards until you click reveal",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        TvFocusButton(
                            text = if (spoilerFree) "Enabled" else "Disabled",
                            isPrimary = spoilerFree,
                            onClick = onToggleSpoilerFree
                        )
                    }
                }
            }
        }

        // Patio Mode (bigger text for viewing from far away)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvDarkSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Patio Mode",
                                color = TvTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Makes all text bigger so it is easy to read from across the patio",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        TvFocusButton(
                            text = if (PatioMode.enabled) "Enabled" else "Disabled",
                            isPrimary = PatioMode.enabled,
                            onClick = { PatioMode.toggle(context) }
                        )
                    }
                }
            }
        }



        // 3. Last Crash Report Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TvDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Last crash report",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recorded stack traces from previous application terminates",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (!crashReportText.isNullOrBlank()) {
                            TvFocusButton(
                                text = "Clear Report",
                                icon = Icons.Default.Delete,
                                onClick = {
                                    try {
                                        val crashFile = File(context.filesDir, "last_crash.txt")
                                        if (crashFile.exists()) {
                                            crashFile.delete()
                                        }
                                        crashReportText = null
                                    } catch (e: Throwable) {
                                        crashReportText = null
                                    }
                                    try {
                                        onClearCrashReport()
                                    } catch (e: Throwable) {}
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!crashReportText.isNullOrBlank()) {
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TvDarkBackground)
                                .border(1.dp, TvOrangeFocus, RoundedCornerShape(8.dp))
                                .focusable()
                                .padding(12.dp)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = crashReportText ?: "",
                                color = Color(0xFFFFB4AB),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TvDarkBackground)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "No crashes recorded (System healthy)",
                                color = TvLimeAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 4. App Updates
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TvDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "App Version & Updates",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current Build: ${updateInfo.currentBuildNumber}",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (updateInfo.hasUpdate) {
                        Text(
                            text = "New update available: ${updateInfo.releaseTitle}",
                            color = TvOrangeFocus,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TvFocusButton(
                            text = if (updateInfo.isDownloading) "Downloading (${(updateInfo.downloadProgress * 100).toInt()}%)..." else "Download & Install",
                            icon = Icons.Default.Download,
                            isPrimary = true,
                            onClick = onDownloadAndInstall
                        )
                    } else {
                        TvFocusButton(
                            text = if (updateInfo.isChecking) "Checking..." else "Check for Updates",
                            icon = Icons.Default.Refresh,
                            onClick = onCheckForUpdates
                        )
                    }
                }
            }
        }
    }
}

