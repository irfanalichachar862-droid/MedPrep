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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.R
import com.example.data.AccessKey
import com.example.data.DeviceIdProvider
import com.example.data.McqQuestion
import com.example.data.MedPrepConstants
import com.example.data.OfflinePackStorage
import com.example.data.PlanEntitlements
import com.example.data.QuestionAttempt
import com.example.data.StudyMode
import com.example.data.SubjectStats
import com.example.data.SubscriptionPlanTier
import com.example.data.SubscriptionRequest
import com.example.data.UserProfile
import com.example.ui.components.JazzCashPaymentAccountBanner
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlanLockCard
import com.example.ui.components.PlanTierBadge
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

/**
 * Allows only digits 0–9 and an optional single leading '+' (capped at 15 digits per E.164).
 * Rejects all letters (abc...) and invalid symbols on both typing and paste.
 */
private fun sanitizeMobileNumberInput(raw: String): String {
    if (raw.isEmpty()) return ""
    val hasLeadingPlus = raw.startsWith("+")
    val digitsOnly = raw.filter { it.isDigit() }.take(15)
    return if (hasLeadingPlus) "+$digitsOnly" else digitsOnly
}

/**
 * Returns null if blank (optional) or a valid 10–15 digit mobile number, otherwise a user-facing error message.
 */
private fun mobileNumberValidationError(value: String): String? {
    if (value.isEmpty()) return null
    val digitsCount = value.count { it.isDigit() }
    return when {
        !value.matches(Regex("^\\+?\\d*$")) -> "Only digits (0–9) and an optional leading + are allowed"
        digitsCount < 10 -> "Enter at least 10 digits ($digitsCount/10)"
        digitsCount > 15 -> "Mobile number cannot exceed 15 digits"
        else -> null
    }
}

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
    onSaveFullProfile: (String, String, Long, String?, String?, Long?) -> Unit,
    onActivateOptionalKey: (String) -> Unit,
    onJumpToPlans: () -> Unit,
    onPostMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalMedPrepColors.current

    var displayName by remember { mutableStateOf(profile?.displayName ?: "Medical Student") }
    var contactEmail by remember { mutableStateOf(profile?.contactEmail ?: "") }
    var phoneNumber by remember { mutableStateOf(sanitizeMobileNumberInput(profile?.phoneNumber.orEmpty())) }
    var targetExam by remember { mutableStateOf(profile?.targetExam ?: MedPrepConstants.EXAM_TRACKS.first()) }
    var dailyGoal by remember { mutableLongStateOf(profile?.dailyGoal ?: 25L) }
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var optionalKeyCode by remember { mutableStateOf("") }

    LaunchedEffect(currentUserId) {
        avatarBitmap = withContext(Dispatchers.IO) {
            OfflinePackStorage.loadUserAvatarBitmap(context.applicationContext, currentUserId)
        }
    }

    LaunchedEffect(profile?.id, profile?.displayName, profile?.contactEmail, profile?.phoneNumber, profile?.targetExam, profile?.dailyGoal) {
        if (profile != null) {
            displayName = profile.displayName
            contactEmail = profile.contactEmail.orEmpty()
            phoneNumber = sanitizeMobileNumberInput(profile.phoneNumber.orEmpty())
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
                            val avatarImage = remember(bmp) { bmp.asImageBitmap() }
                            Image(
                                bitmap = avatarImage,
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

        // Student Access Key Card (Active Key badge OR Optional Activation)
        if (!isAdminUser) {
            item {
                if (activeAccessKey != null) {
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
                } else {
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Optional Access Key", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = "Free features work without a key. If Admin issued you an Access Key, apply it below anytime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = optionalKeyCode,
                                    onValueChange = {
                                        optionalKeyCode = it.uppercase()
                                            .filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }
                                            .take(40)
                                    },
                                    label = { Text("Enter Access Key") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("hub_optional_key_input")
                                )
                                Button(
                                    onClick = {
                                        val clean = optionalKeyCode.trim().uppercase()
                                        if (clean.length < 6) {
                                            onPostMessage("Enter at least 6 characters for an Access Key.")
                                        } else {
                                            onActivateOptionalKey(clean)
                                            optionalKeyCode = ""
                                        }
                                    },
                                    enabled = !isMutating && optionalKeyCode.trim().length >= 6,
                                    modifier = Modifier.height(54.dp).testTag("hub_apply_optional_key_button")
                                ) {
                                    Text("Apply")
                                }
                            }
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("hub_profile_email_input")
                    )

                    val phoneError = mobileNumberValidationError(phoneNumber)
                    val phoneDigitCount = phoneNumber.count { it.isDigit() }
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { raw ->
                            phoneNumber = sanitizeMobileNumberInput(raw)
                        },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("e.g. 03001234567 or +923001234567") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null,
                        supportingText = {
                            Text(
                                text = phoneError
                                    ?: if (phoneDigitCount == 0) {
                                        "Numbers only (10–15 digits, optional leading +)"
                                    } else {
                                        "Valid mobile number ($phoneDigitCount/15 digits)"
                                    }
                            )
                        },
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
                            val validationError = mobileNumberValidationError(phoneNumber)
                            if (validationError != null) {
                                onPostMessage(validationError)
                            } else if (displayName.trim().isNotEmpty()) {
                                onSaveFullProfile(
                                    displayName.trim(),
                                    targetExam,
                                    dailyGoal,
                                    contactEmail.trim(),
                                    phoneNumber.trim(),
                                    null
                                )
                            }
                        },
                        enabled = !isMutating &&
                            displayName.trim().isNotEmpty() &&
                            mobileNumberValidationError(phoneNumber) == null,
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

// --- TAB 2: PLANS (FREE, BASIC PKR 499 & ADVANCE PKR 999 VIA JAZZCASH) ---

@Composable
fun PlansTabContent(
    profile: UserProfile?,
    currentPlanKey: String,
    userRequests: List<SubscriptionRequest>,
    isMutating: Boolean,
    onSubmitPaymentRequest: (SubscriptionPlanTier, String, String, String, String, () -> Unit) -> Unit,
    onCancelPendingRequest: (String) -> Unit,
    onPostMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val ext = LocalMedPrepColors.current
    val activeTier = SubscriptionPlanTier.fromKey(currentPlanKey)
    var checkoutTier by remember { mutableStateOf<SubscriptionPlanTier?>(null) }

    val pendingRequest = remember(userRequests) {
        userRequests.firstOrNull { it.status == "pending" }
    }
    val latestReviewed = remember(userRequests) {
        userRequests.firstOrNull { it.status != "pending" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Subscription Plans",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    PlanTierBadge(tier = activeTier)
                }
                Text(
                    text = "Pay via JazzCash to unlock Basic (PKR 499/mo) or Advance (PKR 999/mo). Admin verifies your payment and activates your plan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // JazzCash IBAN Account Details Banner
        item {
            JazzCashPaymentAccountBanner(
                onCopied = { onPostMessage("Copied JazzCash IBAN: ${MedPrepConstants.JAZZCASH_IBAN}") }
            )
        }

        // Pending Payment Verification Status Card
        if (pendingRequest != null) {
            item {
                val reqTier = SubscriptionPlanTier.fromKey(pendingRequest.requestedPlan)
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = ext.warningContainer.copy(alpha = 0.65f)),
                    border = BorderStroke(1.2.dp, ext.warning.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_pending_request_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HourglassTop,
                                contentDescription = null,
                                tint = ext.onWarningContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Verification Pending — ${reqTier.title} (PKR ${pendingRequest.amountPkr})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ext.onWarningContainer
                                )
                                Text(
                                    text = "JazzCash TID: ${pendingRequest.transactionRef} • Waiting for Admin approval",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.onWarningContainer.copy(alpha = 0.88f)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { onCancelPendingRequest(pendingRequest.id) },
                            enabled = !isMutating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("plan_cancel_pending_button")
                        ) {
                            Text("Cancel Pending Request")
                        }
                    }
                }
            }
        } else if (latestReviewed != null && latestReviewed.status == "rejected") {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Previous Payment Rejected (TID: ${latestReviewed.transactionRef})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = latestReviewed.adminNote?.takeIf { it.isNotBlank() }
                                    ?: "Please check your JazzCash TID and submit again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        items(SubscriptionPlanTier.entries, key = { it.key }) { tier ->
            val isSelected = tier == activeTier
            val isIncludedInHigher = activeTier.level > tier.level
            val isHighlight = tier == SubscriptionPlanTier.ADVANCE
            val hasPendingForThis = pendingRequest?.requestedPlan.equals(tier.key, ignoreCase = true)

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

                    when {
                        isSelected -> {
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
                        }
                        tier == SubscriptionPlanTier.FREE || isIncludedInHigher -> {
                            OutlinedButton(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text("Included in Your ${activeTier.title} Plan")
                            }
                        }
                        hasPendingForThis -> {
                            OutlinedButton(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text("Payment Submitted — Pending Verification")
                            }
                        }
                        else -> {
                            Button(
                                onClick = { checkoutTier = tier },
                                enabled = !isMutating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("plan_buy_${tier.key}")
                            ) {
                                Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buy ${tier.title} — PKR ${tier.amountPkr} (JazzCash)")
                            }
                        }
                    }
                }
            }
        }

        // Admin Contact for Payment Verification & Support
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
                            text = "Plan Activation & Billing Support",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Support: ${MedPrepConstants.SUPPORT_EMAIL}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(MedPrepConstants.SUPPORT_EMAIL))
                            onPostMessage("Copied support email!")
                        }
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy support email")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // JazzCash Payment Verification Dialog
    val buyingTier = checkoutTier
    if (buyingTier != null) {
        JazzCashCheckoutDialog(
            tier = buyingTier,
            defaultPhone = profile?.phoneNumber.orEmpty(),
            isSubmitting = isMutating,
            onCopyIban = { onPostMessage("Copied JazzCash IBAN: ${MedPrepConstants.JAZZCASH_IBAN}") },
            onDismiss = { checkoutTier = null },
            onSubmit = { tid, senderPhone ->
                onSubmitPaymentRequest(
                    buyingTier,
                    tid,
                    senderPhone,
                    profile?.displayName ?: "Medical Student",
                    profile?.email ?: profile?.contactEmail.orEmpty()
                ) {
                    checkoutTier = null
                }
            }
        )
    }
}

@Composable
private fun JazzCashCheckoutDialog(
    tier: SubscriptionPlanTier,
    defaultPhone: String,
    isSubmitting: Boolean,
    onCopyIban: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var transactionRef by remember { mutableStateOf("") }
    var senderPhone by remember { mutableStateOf(sanitizeMobileNumberInput(defaultPhone)) }
    val senderPhoneError = mobileNumberValidationError(senderPhone)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Pay for ${tier.title} Plan")
                Text(
                    text = "Amount: PKR ${tier.amountPkr} / month via JazzCash",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                JazzCashPaymentAccountBanner(onCopied = onCopyIban)

                Text(
                    text = "Step 1: Send PKR ${tier.amountPkr} via JazzCash / Raast / Bank transfer to the IBAN above.\nStep 2: Enter your JazzCash Transaction ID (TID) below for Admin verification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it.take(80) },
                    label = { Text("JazzCash Transaction ID (TID) *") },
                    placeholder = { Text("e.g. JC-8492019482") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jazzcash_tid_input")
                )

                OutlinedTextField(
                    value = senderPhone,
                    onValueChange = { raw -> senderPhone = sanitizeMobileNumberInput(raw) },
                    label = { Text("Your JazzCash Mobile No. (Optional)") },
                    placeholder = { Text("e.g. 03001234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = senderPhoneError != null,
                    supportingText = {
                        Text(senderPhoneError ?: "Numbers only (10–15 digits, optional leading +)")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jazzcash_sender_phone_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(transactionRef.trim(), senderPhone.trim()) },
                enabled = !isSubmitting && transactionRef.trim().length >= 4 && senderPhoneError == null,
                modifier = Modifier.testTag("jazzcash_submit_payment_button")
            ) {
                Text(if (isSubmitting) "Submitting…" else "Submit Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- TAB 3: APPEARANCE (DARK/LIGHT/SYSTEM, SUGGESTED COLOR PALETTES, TEXT STYLE & FONT SIZE) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceTabContent(
    activePlan: SubscriptionPlanTier,
    appearance: AppAppearanceConfig,
    onUpdateAppearance: (AppAppearanceConfig) -> Unit,
    onUpgradeToAdvance: () -> Unit,
    onPostMessage: (String) -> Unit
) {
    val canUsePremium = PlanEntitlements.canUsePremiumAppearance(activePlan)
    val defaultPalette = AppColorPalette.entries.first()
    val defaultFont = AppFontStyle.entries.first()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Theme Mode (Free for all plans)
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

        // 2. Suggested Color Palettes (Default free, premium palettes Advance)
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Suggested Color Palettes", style = MaterialTheme.typography.titleMedium)
                        }
                        if (!canUsePremium) {
                            PlanTierBadge(tier = SubscriptionPlanTier.ADVANCE)
                        }
                    }

                    AppColorPalette.entries.forEach { palette ->
                        val selected = appearance.colorPalette == palette
                        val locked = !canUsePremium && palette != defaultPalette
                        Surface(
                            onClick = {
                                if (locked) {
                                    onPostMessage("${palette.label} palette requires Advance Plan (PKR 999/mo).")
                                    onUpgradeToAdvance()
                                } else {
                                    onUpdateAppearance(appearance.copy(colorPalette = palette))
                                }
                            },
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
                                        if (locked) "Advance Plan • ${palette.subtitle}" else palette.subtitle,
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
                                } else if (locked) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Advance plan required",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Text Style & Font Family (Custom fonts Advance)
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Font & Text Style", style = MaterialTheme.typography.titleMedium)
                        }
                        if (!canUsePremium) {
                            PlanTierBadge(tier = SubscriptionPlanTier.ADVANCE)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFontStyle.entries.forEach { style ->
                            val locked = !canUsePremium && style != defaultFont
                            FilterChip(
                                selected = appearance.fontStyle == style,
                                onClick = {
                                    if (locked) {
                                        onPostMessage("${style.label} font requires Advance Plan (PKR 999/mo).")
                                        onUpgradeToAdvance()
                                    } else {
                                        onUpdateAppearance(appearance.copy(fontStyle = style))
                                    }
                                },
                                leadingIcon = if (locked) {
                                    { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
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
    activePlan: SubscriptionPlanTier,
    profile: UserProfile?,
    publishedQuestions: List<McqQuestion>,
    attemptsMap: Map<String, QuestionAttempt>,
    isDarkTheme: Boolean,
    onSaveScheduleDays: (Long) -> Unit,
    onStartStudy: (StudyMode, String?, Int) -> Unit,
    onUpgradeToBasic: () -> Unit
) {
    val canUseSchedule = PlanEntitlements.canUseStudySchedule(activePlan)
    val maxPracticeCap = PlanEntitlements.maxPracticeQuestionsPerSession(activePlan)
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
        // Smart Cover-in-N-Days Study Schedule Planner (Basic+ Plan)
        item {
            if (!canUseSchedule) {
                PlanLockCard(
                    requiredTier = SubscriptionPlanTier.BASIC,
                    featureTitle = "Smart N-Day Study Schedule",
                    description = "Generate a custom daily quota across all 5 subjects to finish your syllabus in 7, 15, 30, or any number of days.",
                    onUpgradeClick = onUpgradeToBasic,
                    testTag = "schedule_upgrade_basic_button"
                )
            } else {
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
                                onClick = { onStartStudy(StudyMode.PRACTICE, null, dailyTotalTarget.coerceIn(10, maxPracticeCap)) },
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
                            onClick = { onStartStudy(StudyMode.PRACTICE, stat.subject, dailyQuota.coerceIn(10, maxPracticeCap)) },
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
