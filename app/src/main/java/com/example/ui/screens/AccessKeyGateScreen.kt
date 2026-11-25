package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DeviceIdProvider
import com.example.data.MedPrepConstants
import com.example.data.StudentAccessStatus
import com.example.ui.theme.MonoFontFamily

@Composable
fun AccessKeyGateScreen(
    accessStatus: StudentAccessStatus,
    deviceId: String,
    userEmail: String?,
    isMutating: Boolean,
    errorMessage: String?,
    snackbarHostState: SnackbarHostState,
    initialKeyCode: String = "",
    onActivateKey: (String) -> Unit,
    onClearError: () -> Unit,
    onPostMessage: (String) -> Unit,
    onSwitchToAdminPortal: (() -> Unit)? = null,
    onSignOut: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var keyInput by remember(initialKeyCode) { mutableStateOf(initialKeyCode) }
    val shortDevice = remember(deviceId) { DeviceIdProvider.shortLabel(deviceId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            if (accessStatus is StudentAccessStatus.Checking) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Verifying your Access Key & Device…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Box
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (accessStatus is StudentAccessStatus.DeviceMismatch || accessStatus is StudentAccessStatus.Revoked) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (accessStatus) {
                            is StudentAccessStatus.DeviceMismatch, is StudentAccessStatus.Revoked -> Icons.Rounded.Lock
                            else -> Icons.Rounded.VpnKey
                        },
                        contentDescription = "Access Key Verification",
                        tint = if (accessStatus is StudentAccessStatus.DeviceMismatch || accessStatus is StudentAccessStatus.Revoked) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = when (accessStatus) {
                        is StudentAccessStatus.DeviceMismatch -> stringResource(R.string.key_gate_device_locked_title)
                        is StudentAccessStatus.Revoked -> stringResource(R.string.key_gate_revoked_title)
                        else -> stringResource(R.string.key_gate_title)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when (accessStatus) {
                        is StudentAccessStatus.DeviceMismatch ->
                            "Your Access Key (${accessStatus.key.keyCode}) is currently active on another device. Enter your matching Access Key below — since your email matches, your old device will automatically log out and this device will log in."
                        is StudentAccessStatus.Revoked ->
                            "Your Access Key (${accessStatus.key.keyCode}) has been revoked by Admin. Contact Admin below or enter a new active key."
                        else -> stringResource(R.string.key_gate_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Device Identity Badge (1-User 1-Device Transparency)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "This Device ID (1-Device Lock)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = shortDevice,
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(shortDevice))
                            onPostMessage("Copied Device ID: $shortDevice")
                        }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Device ID")
                        }
                    }
                }

                // Special Key Activation Card
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
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = {
                                keyInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }.take(40)
                                if (errorMessage != null) onClearError()
                            },
                            label = { Text("Student Specific Access Key") },
                            placeholder = { Text("e.g. ALI-MDCAT-2026 or MED-XXXX-XXXX") },
                            leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_access_key_input")
                        )

                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { onActivateKey(keyInput) },
                            enabled = !isMutating && keyInput.trim().length >= 6,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("student_activate_key_button")
                        ) {
                            if (isMutating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (accessStatus is StudentAccessStatus.DeviceMismatch) "Switching Device…"
                                    else "Activating Key…"
                                )
                            } else {
                                Icon(Icons.Rounded.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (accessStatus is StudentAccessStatus.DeviceMismatch) {
                                        "Verify Key & Switch to This Device"
                                    } else {
                                        stringResource(R.string.key_gate_activate_button)
                                    }
                                )
                            }
                        }
                    }
                }

                // Admin Contact Card with Admin's Profile Photo
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                            Text(
                                text = "Need an Access Key or Device Reset?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = MedPrepConstants.SUPPORT_EMAIL,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(MedPrepConstants.SUPPORT_EMAIL))
                            onPostMessage("Copied support email")
                        }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Support Email")
                        }
                    }
                }

                if (onSwitchToAdminPortal != null) {
                    OutlinedButton(
                        onClick = onSwitchToAdminPortal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("key_gate_open_admin_button")
                    ) {
                        Text("Return to Admin Portal")
                    }
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("key_gate_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out & Switch Account")
                }
            }
        }
    }
}
