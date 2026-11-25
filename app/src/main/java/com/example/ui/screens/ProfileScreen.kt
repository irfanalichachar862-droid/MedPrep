package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Apps
import com.example.R
import com.example.data.AccessKey
import com.example.data.DeviceIdProvider
import com.example.data.ExamSession
import com.example.data.LocalStudyCache
import com.example.data.McqQuestion
import com.example.data.MedPrepConstants
import com.example.data.SubscriptionPlanTier
import com.example.data.SyncReport
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.PlanTierBadge
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalMedPrepColors
import com.example.ui.theme.MonoFontFamily
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile?>,
    publishedQuestions: List<McqQuestion>,
    offlineStudyCache: LocalStudyCache?,
    localStorageKb: Long,
    examSessions: List<ExamSession>,
    activeAccessKey: AccessKey?,
    deviceId: String,
    themeMode: AppThemeMode,
    isAdminUser: Boolean,
    isSyncing: Boolean,
    lastSyncReport: SyncReport?,
    isMutating: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onOpenMoreTab: (MoreMenuTab) -> Unit,
    onSaveOfflineSnapshot: () -> Unit,
    onActivateOptionalKey: (String) -> Unit,
    onSyncNow: () -> Unit,
    onSavePreferences: (String, String, Long) -> Unit,
    onPostMessage: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val extendedColors = LocalMedPrepColors.current
    val clipboard = LocalClipboardManager.current
    val profile = (profileState as? UiState.Success)?.data
    val activePlan = SubscriptionPlanTier.fromKey(profile?.subscriptionPlan)
    val publishedCount = publishedQuestions.size

    var displayName by remember { mutableStateOf(profile?.displayName ?: "Medical Student") }
    var targetExam by remember { mutableStateOf(profile?.targetExam ?: MedPrepConstants.EXAM_TRACKS.first()) }
    var dailyGoal by remember { mutableLongStateOf(profile?.dailyGoal ?: 25L) }
    var optionalKeyInput by remember { mutableStateOf("") }

    LaunchedEffect(profile?.id, profile?.displayName, profile?.targetExam, profile?.dailyGoal) {
        if (profile != null) {
            displayName = profile.displayName
            targetExam = profile.targetExam
            dailyGoal = profile.dailyGoal
        }
    }

    val initials = remember(displayName) {
        displayName.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifBlank { "MD" }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Account Summary Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (isAdminUser) {
                            Image(
                                painter = painterResource(R.drawable.admin_profile_photo),
                                contentDescription = "Admin Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = profile?.displayName ?: displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isAdminUser) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.profile_admin_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                PlanTierBadge(
                                    tier = activePlan,
                                    modifier = Modifier
                                        .clickable { onOpenMoreTab(MoreMenuTab.PLANS) }
                                        .testTag("profile_plan_badge")
                                )
                            }
                            val email = profile?.email
                            if (!email.isNullOrBlank()) {
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Student Optional Access Key Card (Active Key badge OR Optional Activation input)
            if (!isAdminUser) {
                item {
                    if (activeAccessKey != null) {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = extendedColors.successContainer.copy(alpha = 0.55f)),
                            border = BorderStroke(1.dp, extendedColors.success.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(extendedColors.success.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Key,
                                        contentDescription = "Active Access Key",
                                        tint = extendedColors.onSuccessContainer
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Verified 1-Device Access Key",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = extendedColors.onSuccessContainer
                                    )
                                    Text(
                                        text = activeAccessKey.keyCode,
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                        color = extendedColors.onSuccessContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Locked to Device: ${DeviceIdProvider.shortLabel(deviceId)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extendedColors.onSuccessContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text("Optional Student Access Key", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "Not required for Free access. Have a key from Admin? Enter it below (auto-transfers from old device if email matches).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = optionalKeyInput,
                                        onValueChange = {
                                            optionalKeyInput = it.uppercase()
                                                .filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }
                                                .take(40)
                                        },
                                        label = { Text("Access Key") },
                                        placeholder = { Text("MED-XXXX-XXXX") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("profile_optional_key_input")
                                    )
                                    Button(
                                        onClick = {
                                            val clean = optionalKeyInput.trim().uppercase()
                                            if (clean.length < 6) {
                                                onPostMessage("Please enter a valid Access Key (at least 6 characters).")
                                            } else {
                                                onActivateOptionalKey(clean)
                                                optionalKeyInput = ""
                                            }
                                        },
                                        enabled = !isMutating && optionalKeyInput.trim().length >= 6,
                                        modifier = Modifier
                                            .height(54.dp)
                                            .testTag("profile_activate_optional_key_button")
                                    ) {
                                        Text("Apply")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Launcher into the 8-Tab Liquid Glass Menu Hub (Profile, Plans, Appearance, Progress, Notes, Difficult, Settings & Backup, App Guide)
            item {
                Card(
                    onClick = { onOpenMoreTab(MoreMenuTab.PROFILE) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_open_hub_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = "Open Full Menu Hub",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open Full Menu Hub (8 Liquid Glass Tabs)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Photo, Plans, Palettes, Schedule, Notes, Difficult Qs, Backup & Guide",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Open Menu Hub",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Light & Dark Theme Switcher Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.profile_appearance),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeChip(
                                label = "Light",
                                icon = Icons.Rounded.LightMode,
                                selected = themeMode == AppThemeMode.LIGHT,
                                tag = "theme_light_chip",
                                onClick = { onSelectThemeMode(AppThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeChip(
                                label = "Dark",
                                icon = Icons.Rounded.DarkMode,
                                selected = themeMode == AppThemeMode.DARK,
                                tag = "theme_dark_chip",
                                onClick = { onSelectThemeMode(AppThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeChip(
                                label = "System",
                                icon = Icons.Rounded.BrightnessAuto,
                                selected = themeMode == AppThemeMode.SYSTEM,
                                tag = "theme_system_chip",
                                onClick = { onSelectThemeMode(AppThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Offline Mobile Storage Sync Card (No External File Downloads — Private In-App Offline Study)
            item {
                val cachedMcqs = offlineStudyCache?.questions?.size ?: 0
                val cachedCourses = offlineStudyCache?.courses?.size ?: 0
                val cachedPosts = offlineStudyCache?.posts?.size ?: 0
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OfflinePin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.profile_sync_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "$cachedMcqs MCQs • $cachedCourses Courses • $cachedPosts Lessons saved in mobile storage (${localStorageKb} KB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        val syncTimestamp = profile?.lastSyncedAt
                        if (syncTimestamp != null || lastSyncReport != null) {
                            Text(
                                text = lastSyncReport?.message
                                    ?: "Last synced: ${formatTimestamp(syncTimestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onSyncNow,
                                enabled = !isSyncing,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("profile_sync_now_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.profile_syncing))
                                } else {
                                    Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.profile_sync_now))
                                }
                            }
                            OutlinedButton(
                                onClick = onSaveOfflineSnapshot,
                                enabled = publishedCount > 0,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("profile_save_mobile_storage_button")
                            ) {
                                Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Offline")
                            }
                        }
                    }
                }
            }

            // Study Plan Settings Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Study Plan Settings",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it.take(80) },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                        )

                        Text(
                            text = stringResource(R.string.profile_exam_track),
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MedPrepConstants.EXAM_TRACKS.forEach { track ->
                                FilterChip(
                                    selected = targetExam == track,
                                    onClick = { targetExam = track },
                                    label = { Text(track) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.profile_daily_goal),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Target MCQs per day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(onClick = { dailyGoal = (dailyGoal - 5L).coerceAtLeast(5L) }) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease daily goal")
                                }
                                Text(
                                    text = "$dailyGoal",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { dailyGoal = (dailyGoal + 5L).coerceAtMost(200L) }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase daily goal")
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (displayName.trim().isNotEmpty()) {
                                    onSavePreferences(displayName.trim(), targetExam, dailyGoal)
                                }
                            },
                            enabled = !isMutating && displayName.trim().isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_save_preferences_button")
                        ) {
                            Text("Save Study Plan")
                        }
                    }
                }
            }

            // Recent Sessions History
            if (examSessions.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.profile_recent_sessions),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                items(examSessions.take(8), key = { it.id }) { session ->
                    val pct = if (session.totalQuestions > 0) {
                        ((session.correctCount * 100) / session.totalQuestions).toInt()
                    } else 0
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${session.mode.replaceFirstChar { it.uppercase() }} • ${session.subjectFilter}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${session.correctCount}/${session.totalQuestions} correct • ${formatDuration(session.durationSeconds)} • ${formatTimestamp(session.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (pct >= 70) extendedColors.successContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                    color = if (pct >= 70) extendedColors.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Sign Out
            item {
                OutlinedButton(
                    onClick = onSignOut,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.profile_sign_out),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Admin Contact Footer with Admin's Profile Photo & Email
            item {
                val adminEmail = stringResource(R.string.profile_admin_email)
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_admin_contact_footer")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.admin_profile_photo),
                            contentDescription = "Admin Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.profile_admin_contact_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Support: $adminEmail",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(adminEmail))
                                onPostMessage("Copied support email: $adminEmail")
                            },
                            modifier = Modifier.testTag("copy_admin_email_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy support email",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .height(48.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(date)
}
