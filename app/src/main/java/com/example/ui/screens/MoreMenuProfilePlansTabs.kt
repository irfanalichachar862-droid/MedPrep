package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AccessKey
import com.example.data.DeviceIdProvider
import com.example.data.McqQuestion
import com.example.data.MedPrepConstants
import com.example.data.OfflinePackStorage
import com.example.data.QuestionAttempt
import com.example.data.StudyMode
import com.example.data.SubjectStats
import com.example.data.SubscriptionPlanTier
import com.example.data.UserProfile
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.AppAppearanceConfig
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.AppFontStyle
import com.example.ui.theme.AppTextScale
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalMedPrepColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.subjectVisualFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

// --- TAB 1: PROFILE (EDIT NAME, PHOTO PICKER, EMAILS, MOBILE NO, SUBSCRIPTION PLAN) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileTabContent(
    currentUserId: String,
    profile: UserProfile?,
    activeAccessKey: AccessKey?,
    deviceId: String,
    isAdminUser: Boolean,
    isMutating: Boolean,
    onSaveFullProfile: (String, String, Long, String?, String?, String?, Long?) -> Unit,
    onJumpToPlans: () -> Unit,
    onPostMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalMedPrepColors.current

    var displayName by remember { mutableStateOf(profile?.displayName ?: "Medical Student") }
    var contactEmail by remember { mutableStateOf(profile?.contactEmail ?: "") }
    var phoneNumber by remember { mutableStateOf(profile?.phoneNumber ?: "") }
    var targetExam by remember { mutableStateOf(profile?.targetExam ?: MedPrepConstants.EXAM_TRACKS.first()) }
    var dailyGoal by remember { mutableLongStateOf(profile?.dailyGoal ?: 25L) }
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentUserId) {
        avatarBitmap = withContext(Dispatchers.IO) {
            OfflinePackStorage.loadUserAvatarBitmap(context.applicationContext, currentUserId)
        }
    }

    LaunchedEffect(profile?.id, profile?.displayName, profile?.contactEmail, profile?.phoneNumber, profile?.targetExam, profile?.dailyGoal) {
        if (profile != null) {
            displayName = profile.displayName
            contactEmail = profile.contactEmail.orEmpty()
            phoneNumber = profile.phoneNumber.orEmpty()
            targetExam = profile.targetExam
            dailyGoal = profile.dailyGoal
        }
    }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    OfflinePackStorage.saveUserAvatarFromUri(context.applicationContext, currentUserId, uri)
                }
                if (saved != null) {
                    avatarBitmap = saved
                    onPostMessage("Profile photo updated!")
                } else {
                    onPostMessage("Could not load selected image.")
                }
            }
        }
    }

    val activePlan = SubscriptionPlanTier.fromKey(profile?.subscriptionPlan)
    val initials = remember(displayName) {
        displayName.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifBlank { "MD" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Avatar + Plan Header
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        val bmp = avatarBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        } else if (isAdminUser) {
                            Image(
                                painter = painterResource(R.drawable.admin_profile_photo),
                                contentDescription = "Admin photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("profile_pick_photo_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = "Change profile photo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = profile?.displayName ?: displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!profile?.email.isNullOrBlank()) {
                        Text(
                            text = "Google Account: ${profile?.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onJumpToPlans,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("profile_plan_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${activePlan.title} Plan • Change",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (avatarBitmap != null) {
                            IconButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        OfflinePackStorage.clearUserAvatar(context.applicationContext, currentUserId)
                                    }
                                    avatarBitmap = null
                                    onPostMessage("Profile photo removed.")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove photo",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Student Access Key Card
        if (!isAdminUser && activeAccessKey != null) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = ext.successContainer.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, ext.success.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null, tint = ext.onSuccessContainer)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1-Device Key: ${activeAccessKey.keyCode}",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                color = ext.onSuccessContainer
                            )
                            Text(
                                text = "Device Lock: ${DeviceIdProvider.shortLabel(deviceId)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.onSuccessContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Editable Profile Details Form
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Personal Info & Study Details",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(80) },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("hub_profile_name_input")
                    )

                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it.take(160) },
                        label = { Text("Additional / Contact Email") },
                        placeholder = { Text("e.g. student.backup@gmail.com") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("hub_profile_email_input")
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.take(30) },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("e.g. +92 300 1234567") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("hub_profile_phone_input")
                    )

                    Text(
                        text = "Target Exam Track",
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
                            Text("Daily MCQ Goal", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Target questions per day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { dailyGoal = (dailyGoal - 5L).coerceAtLeast(5L) }) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease goal")
                            }
                            Text(
                                text = "$dailyGoal",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { dailyGoal = (dailyGoal + 5L).coerceAtMost(200L) }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase goal")
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (displayName.trim().isNotEmpty()) {
                                onSaveFullProfile(
                                    displayName.trim(),
                                    targetExam,
                                    dailyGoal,
                                    contactEmail.trim(),
                                    phoneNumber.trim(),
                                    null,
                                    null
                                )
                            }
                        },
                        enabled = !isMutating && displayName.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("hub_profile_save_button")
                    ) {
                        Text("Save Profile Changes")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// --- TAB 2: PLANS (FREE, BASIC & ADVANCE) ---

@Composable
fun PlansTabContent(
    currentPlanKey: String,
    isMutating: Boolean,
    onSelectPlan: (String, String) -> Unit,
    onPostMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val activeTier = SubscriptionPlanTier.fromKey(currentPlanKey)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Subscription Plans",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose Free, Basic, or Advance. Your selected plan saves to your profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(SubscriptionPlanTier.entries, key = { it.key }) { tier ->
            val isSelected = tier == activeTier
            val isHighlight = tier == SubscriptionPlanTier.ADVANCE

            LiquidGlassSurface(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge)
                        } else Modifier
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tier.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = tier.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isHighlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = tier.priceTag,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Active plan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tier.features.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = feat,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (isSelected) {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("plan_active_${tier.key}")
                        ) {
                            Text("Currently Active (${tier.title})")
                        }
                    } else {
                        Button(
                            onClick = { onSelectPlan(tier.key, tier.title) },
                            enabled = !isMutating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("plan_select_${tier.key}")
                        ) {
                            Text("Switch to ${tier.title} Plan")
                        }
                    }
                }
            }
        }

        // Admin Contact for Plan Keys
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Need an Access Key for Basic / Advance?",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Contact Admin: ${MedPrepConstants.BOOTSTRAP_ADMIN_EMAIL}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(MedPrepConstants.BOOTSTRAP_ADMIN_EMAIL))
                            onPostMessage("Copied Admin email!")
                        }
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy admin email")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// --- TAB 3: APPEARANCE (DARK/LIGHT/SYSTEM, SUGGESTED COLOR PALETTES, TEXT STYLE & FONT SIZE) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceTabContent(
    appearance: AppAppearanceConfig,
    onUpdateAppearance: (AppAppearanceConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Theme Mode
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(AppThemeMode.LIGHT, "Light", Icons.Rounded.LightMode),
                            Triple(AppThemeMode.DARK, "Dark", Icons.Rounded.DarkMode),
                            Triple(AppThemeMode.SYSTEM, "System", Icons.Rounded.BrightnessAuto)
                        ).forEach { (mode, label, icon) ->
                            val selected = appearance.themeMode == mode
                            Surface(
                                onClick = { onUpdateAppearance(appearance.copy(themeMode = mode)) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    if (selected) 1.5.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("appearance_theme_${mode.key}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
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
                    }
                }
            }
        }

        // 2. Suggested Color Palettes
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Suggested Color Palettes", style = MaterialTheme.typography.titleMedium)
                    }

                    AppColorPalette.entries.forEach { palette ->
                        val selected = appearance.colorPalette == palette
                        Surface(
                            onClick = { onUpdateAppearance(appearance.copy(colorPalette = palette)) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("appearance_palette_${palette.key}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(palette.swatchPrimary, palette.swatchAccent)
                                            )
                                        )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(palette.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        palette.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected palette",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Text Style & Font Family
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Font & Text Style", style = MaterialTheme.typography.titleMedium)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFontStyle.entries.forEach { style ->
                            FilterChip(
                                selected = appearance.fontStyle == style,
                                onClick = { onUpdateAppearance(appearance.copy(fontStyle = style)) },
                                label = { Text(style.label) },
                                modifier = Modifier.testTag("appearance_font_${style.key}")
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(Icons.Rounded.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Text Size", style = MaterialTheme.typography.titleSmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppTextScale.entries.forEach { scale ->
                            val selected = appearance.textScale == scale
                            Surface(
                                onClick = { onUpdateAppearance(appearance.copy(textScale = scale)) },
                                shape = MaterialTheme.shapes.small,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("appearance_scale_${scale.key}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = scale.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Live Reading Preview
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Live MCQ Typography Preview",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Which enzyme catalyzes the rate-limiting step of glycolysis (Fructose-6-P → Fructose-1,6-BP)?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// --- TAB 4: PROGRESS & STUDY SCHEDULE (EACH SUBJECT PROGRESS + COVER IN GIVEN DAYS) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressScheduleTabContent(
    profile: UserProfile?,
    publishedQuestions: List<McqQuestion>,
    attemptsMap: Map<String, QuestionAttempt>,
    isDarkTheme: Boolean,
    onSaveScheduleDays: (Long) -> Unit,
    onStartStudy: (StudyMode, String?, Int) -> Unit
) {
    var targetDays by remember(profile?.scheduleDays) {
        mutableLongStateOf((profile?.scheduleDays ?: 30L).coerceIn(1L, 365L))
    }

    val subjectStats = remember(publishedQuestions, attemptsMap) {
        MedPrepConstants.DEFAULT_SUBJECTS.map { subject ->
            val list = publishedQuestions.filter { it.subject.equals(subject, ignoreCase = true) }
            val answered = list.count { attemptsMap[it.id]?.answered == true }
            val correct = list.count { attemptsMap[it.id]?.correct == true }
            SubjectStats(subject, list.size, answered, correct)
        }
    }

    val totalBank = subjectStats.sumOf { it.totalQuestions }
    val totalAnswered = subjectStats.sumOf { it.answeredCount }
    val totalCorrect = subjectStats.sumOf { it.correctCount }
    val totalRemaining = (totalBank - totalAnswered).coerceAtLeast(0)
    val dailyTotalTarget = if (totalRemaining == 0) 0 else ceil(totalRemaining.toDouble() / targetDays.toDouble()).toInt().coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Smart Cover-in-N-Days Study Schedule Planner
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Syllabus Schedule Planner", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Cover all remaining MCQs in your chosen number of days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick Day Presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(7L, 15L, 30L, 45L, 60L, 90L).forEach { days ->
                            FilterChip(
                                selected = targetDays == days,
                                onClick = { targetDays = days },
                                label = { Text("$days Days") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom Days:", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { targetDays = (targetDays - 1L).coerceAtLeast(1L) }) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease days")
                            }
                            Text(
                                text = "$targetDays days",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { targetDays = (targetDays + 1L).coerceAtMost(365L) }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase days")
                            }
                        }
                    }

                    // Calculated Daily Plan Box
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (totalRemaining == 0) {
                                    "All $totalBank published MCQs practiced! Ready for full Revision & Retests."
                                } else {
                                    "Daily Target: $dailyTotalTarget MCQs / day  ($totalRemaining left ÷ $targetDays days)"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            subjectStats.forEach { stat ->
                                val rem = (stat.totalQuestions - stat.answeredCount).coerceAtLeast(0)
                                val perDay = if (rem == 0) 0 else ceil(rem.toDouble() / targetDays.toDouble()).toInt().coerceAtLeast(1)
                                Text(
                                    text = "• ${stat.subject}: $perDay/day ($rem remaining of ${stat.totalQuestions})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSaveScheduleDays(targetDays) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("schedule_save_days_button")
                        ) {
                            Text("Save $targetDays-Day Plan")
                        }
                        Button(
                            onClick = { onStartStudy(StudyMode.PRACTICE, null, dailyTotalTarget.coerceIn(10, 40)) },
                            enabled = totalBank > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("schedule_start_today_button")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Today")
                        }
                    }
                }
            }
        }

        // Overall & Per-Subject Progress Cards
        item {
            val overallPct = if (totalAnswered == 0) 0 else (totalCorrect * 100) / totalAnswered
            Text(
                text = "Subject-Wise Progress ($totalAnswered/$totalBank practiced • $overallPct% accuracy)",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(subjectStats, key = { it.subject }) { stat ->
            val visual = subjectVisualFor(stat.subject)
            val remaining = (stat.totalQuestions - stat.answeredCount).coerceAtLeast(0)
            val dailyQuota = if (remaining == 0) 0 else ceil(remaining.toDouble() / targetDays.toDouble()).toInt().coerceAtLeast(1)

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkTheme) visual.containerDark else visual.containerLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = visual.icon,
                                contentDescription = stat.subject,
                                tint = if (isDarkTheme) visual.containerLight else visual.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stat.subject, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${stat.answeredCount}/${stat.totalQuestions} done • ${stat.accuracyPercent}% accuracy • $dailyQuota/day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            onClick = { onStartStudy(StudyMode.PRACTICE, stat.subject, dailyQuota.coerceIn(10, 30)) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Study",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { stat.masteryFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = visual.accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
