package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.PlaylistConfig
import com.example.model.UpdateInfo
import com.example.ui.components.TvFocusButton
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    playlistConfig: PlaylistConfig,
    updateInfo: UpdateInfo,
    spoilerFree: Boolean,
    lastCrashReport: String? = null,
    onClearCrashReport: () -> Unit = {},
    onSaveConfig: (PlaylistConfig) -> Unit,
    onLoadDemo: () -> Unit,
    onToggleSpoilerFree: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadAndInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playlistType by remember(playlistConfig.type) { mutableStateOf(playlistConfig.type) }
    var m3uUrl by remember(playlistConfig.m3uUrl) { mutableStateOf(playlistConfig.m3uUrl) }
    var xtreamServer by remember(playlistConfig.xtreamServer) { mutableStateOf(playlistConfig.xtreamServer) }
    var xtreamUser by remember(playlistConfig.xtreamUsername) { mutableStateOf(playlistConfig.xtreamUsername) }
    var xtreamPass by remember(playlistConfig.xtreamPassword) { mutableStateOf(playlistConfig.xtreamPassword) }

    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Settings & IPTV Configuration",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        color = TvLimeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    com.example.ui.components.TvClock()
                }
            }
        }

        // 2. Playlist Configuration Box
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvDarkCard)
                    .border(1.dp, TvDarkCardElevated, RoundedCornerShape(14.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Sports Playlist Source",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Source Type Selector
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvFocusButton(
                        text = "M3U Playlist URL",
                        isPrimary = playlistType == "m3u",
                        onClick = { playlistType = "m3u" }
                    )
                    TvFocusButton(
                        text = "Xtream Codes API",
                        isPrimary = playlistType == "xtream",
                        onClick = { playlistType = "xtream" }
                    )
                    TvFocusButton(
                        text = "Curated Demo Channels",
                        isPrimary = playlistType == "demo",
                        onClick = {
                            playlistType = "demo"
                            onLoadDemo()
                            saveFeedbackMessage = "Loaded curated sports channels!"
                        }
                    )
                }

                if (playlistType == "m3u") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "M3U Playlist URL:",
                            color = TvTextSecondary,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            placeholder = { Text("http://example.com/sports.m3u", color = TvTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = TvOrangeFocus,
                                unfocusedBorderColor = TvDarkCardElevated,
                                focusedContainerColor = TvDarkSurface,
                                unfocusedContainerColor = TvDarkSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (playlistType == "xtream") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Server URL
                        Text(text = "Server URL (e.g. http://server.com:8080):", color = TvTextSecondary, fontSize = 12.sp)
                        OutlinedTextField(
                            value = xtreamServer,
                            onValueChange = { xtreamServer = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = TvOrangeFocus,
                                unfocusedBorderColor = TvDarkCardElevated,
                                focusedContainerColor = TvDarkSurface,
                                unfocusedContainerColor = TvDarkSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Username:", color = TvTextSecondary, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = xtreamUser,
                                    onValueChange = { xtreamUser = it },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = TvOrangeFocus,
                                        unfocusedBorderColor = TvDarkCardElevated,
                                        focusedContainerColor = TvDarkSurface,
                                        unfocusedContainerColor = TvDarkSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Password:", color = TvTextSecondary, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = xtreamPass,
                                    onValueChange = { xtreamPass = it },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = TvOrangeFocus,
                                        unfocusedBorderColor = TvDarkCardElevated,
                                        focusedContainerColor = TvDarkSurface,
                                        unfocusedContainerColor = TvDarkSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvFocusButton(
                            text = "Save & Refresh Playlist",
                            icon = Icons.Default.Save,
                            isPrimary = true,
                            onClick = {
                                val updated = PlaylistConfig(
                                    type = playlistType,
                                    m3uUrl = m3uUrl.trim(),
                                    xtreamServer = xtreamServer.trim(),
                                    xtreamUsername = xtreamUser.trim(),
                                    xtreamPassword = xtreamPass.trim(),
                                    spoilerFree = spoilerFree
                                )
                                onSaveConfig(updated)
                                saveFeedbackMessage = "Settings saved & refreshing playlist..."
                            }
                        )

                        TvFocusButton(
                            text = "Reset to Demo Channels",
                            onClick = {
                                onLoadDemo()
                                saveFeedbackMessage = "Reset to demo channels"
                            }
                        )
                    }

                    if (saveFeedbackMessage != null) {
                        Text(
                            text = saveFeedbackMessage!!,
                            color = TvLimeAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Preferences (Spoiler-Free Mode)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvDarkCard)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spoiler-Free Mode",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hides real live scores behind '?' until you press OK on remote. Prevents game spoilers!",
                        color = TvTextSecondary,
                        fontSize = 12.sp
                    )
                }

                TvFocusButton(
                    text = if (spoilerFree) "Enabled (Hiding Scores)" else "Disabled (Showing Scores)",
                    icon = if (spoilerFree) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    isPrimary = spoilerFree,
                    onClick = onToggleSpoilerFree
                )
            }
        }

        // 4. Update Checker Box
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvDarkCard)
                    .border(1.dp, TvDarkCardElevated, RoundedCornerShape(14.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Software Updates",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Checks GitHub automated releases tagged 'Gemini-latest' and compares with BuildConfig.VERSION_CODE",
                            color = TvTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    TvFocusButton(
                        text = if (updateInfo.isChecking) "Checking..." else "Check for Updates",
                        icon = Icons.Default.SystemUpdateAlt,
                        onClick = onCheckForUpdates
                    )
                }

                // Update Status Details
                if (updateInfo.isChecking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = TvOrangeFocus,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(text = "Connecting to GitHub Releases...", color = TvTextSecondary, fontSize = 12.sp)
                    }
                } else if (updateInfo.hasUpdate) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TvDarkCardElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "New Update Available: ${updateInfo.releaseTitle} (Build ${updateInfo.latestBuildNumber})",
                            color = TvLimeAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current: Build ${updateInfo.currentBuildNumber} → Latest: Build ${updateInfo.latestBuildNumber}",
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        if (updateInfo.isDownloading) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { updateInfo.downloadProgress },
                                    color = TvOrangeFocus,
                                    trackColor = TvDarkSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Downloading APK: ${(updateInfo.downloadProgress * 100).toInt()}%",
                                    color = TvTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            TvFocusButton(
                                text = "Download & Prompt Install",
                                icon = Icons.Default.Download,
                                isPrimary = true,
                                onClick = onDownloadAndInstall
                            )
                        }
                    }
                } else if (updateInfo.errorMessage != null) {
                    Text(
                        text = updateInfo.errorMessage,
                        color = if (updateInfo.errorMessage.contains("up to date")) TvLimeAccent else TvOrangeFocus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5. Last Crash Report Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvDarkCard)
                    .border(1.dp, TvDarkCardElevated, RoundedCornerShape(14.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Last Crash Report",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lastCrashReport != null) "Crash captured on device before process exited" else "No crash recorded - app running normally",
                            color = if (lastCrashReport != null) TvOrangeFocus else TvTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (lastCrashReport != null) {
                        TvFocusButton(
                            text = "Clear Report",
                            icon = Icons.Default.Delete,
                            onClick = onClearCrashReport
                        )
                    }
                }

                if (lastCrashReport != null) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    val scrollState = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 260.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TvDarkSurface)
                            .border(
                                width = if (isFocused) 2.5.dp else 1.dp,
                                color = if (isFocused) TvOrangeFocus else TvDarkCardElevated,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .focusable(interactionSource = interactionSource)
                            .verticalScroll(scrollState)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = lastCrashReport,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
