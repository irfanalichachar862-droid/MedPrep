package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Unpublished
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AccessKey
import com.example.data.DeviceIdProvider
import com.example.data.McqQuestion
import com.example.data.StudyPost
import com.example.data.TopicLinkResolver
import com.example.data.UiState
import com.example.ui.components.FormulaText
import com.example.ui.theme.LocalMedPrepColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.subjectVisualFor

@Composable
fun AdminPostsManagerTab(
    adminPostsState: UiState<List<StudyPost>>,
    allQuestions: List<McqQuestion>,
    isDarkTheme: Boolean,
    onCreatePost: () -> Unit,
    onEditPost: (StudyPost) -> Unit,
    onPreviewHtml: (StudyPost) -> Unit,
    onTogglePostStatus: (StudyPost) -> Unit,
    onDeletePost: (StudyPost) -> Unit,
    onPostMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalMedPrepColors.current
    val posts = (adminPostsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Upload Interactive HTML Lessons, Web Links & Videos", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Upload interactive .html topic explanations, web articles, and YouTube links. Topics automatically link with matching MCQs so students can understand concepts and practice seamlessly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Button(
                        onClick = onCreatePost,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_upload_post_button")
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload HTML Lesson / Study Post")
                    }
                }
            }
        }

        if (adminPostsState is UiState.Loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (posts.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No study posts uploaded yet. Tap 'Create New Study Post' to share web links and YouTube videos with students.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(posts, key = { it.id }) { post ->
                val isPublished = post.status == "published"
                val visual = subjectVisualFor(post.subject)
                val badgeBg = if (isDarkTheme) visual.containerDark else visual.containerLight
                val badgeFg = if (isDarkTheme) visual.containerLight else visual.accent
                val linkedCount = remember(post.id, post.subject, post.topic, allQuestions) {
                    TopicLinkResolver.findLinkedQuestions(post, allQuestions).size
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isPublished) extendedColors.successContainer else extendedColors.warningContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isPublished) "PUBLISHED" else "DRAFT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPublished) extendedColors.onSuccessContainer else extendedColors.onWarningContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(visual.icon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(13.dp))
                                    Text(post.subject, style = MaterialTheme.typography.labelSmall, color = badgeFg)
                                }
                            }
                            if (post.hasInteractiveHtml) {
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Code,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "HTML",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            if (linkedCount > 0) {
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Link,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "$linkedCount MCQs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Text(post.title, style = MaterialTheme.typography.titleMedium)
                        if (post.topic.isNotBlank()) {
                            Text(
                                text = "Topic: ${post.topic}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (post.body.isNotBlank()) {
                            FormulaText(
                                text = post.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (post.hasInteractiveHtml) {
                            Button(
                                onClick = { onPreviewHtml(post) },
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview Interactive HTML (${post.htmlFileName ?: "Lesson"})")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!post.webUrl.isNullOrBlank()) {
                                OutlinedButton(onClick = { openExternalLink(context, post.webUrl, onPostMessage) }) {
                                    Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Web Link")
                                }
                            }
                            if (!post.youtubeUrl.isNullOrBlank()) {
                                OutlinedButton(onClick = { openExternalLink(context, post.youtubeUrl, onPostMessage) }) {
                                    Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("YouTube")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onTogglePostStatus(post) }) {
                                Icon(
                                    imageVector = if (isPublished) Icons.Rounded.Unpublished else Icons.Rounded.Publish,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPublished) "Move to Draft" else "Publish")
                            }
                            Row {
                                IconButton(onClick = { onEditPost(post) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Post", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeletePost(post) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Post", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminKeysManagerTab(
    accessKeysState: UiState<List<AccessKey>>,
    onOpenGenerateDialog: () -> Unit,
    onResetDevice: (String) -> Unit,
    onToggleStatus: (AccessKey) -> Unit,
    onDeleteKey: (AccessKey) -> Unit,
    onPostMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val extendedColors = LocalMedPrepColors.current
    val keys = (accessKeysState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Student-Only Specific Keys (1-User • 1-Device)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Only students require a specific Access Key to log in (Admin signs in directly). Create a custom key or auto-generate one — it locks automatically to 1 student & 1 device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Button(
                        onClick = onOpenGenerateDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_generate_key_button")
                    ) {
                        Icon(Icons.Rounded.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Specific Student Key")
                    }
                }
            }
        }

        if (accessKeysState is UiState.Loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (keys.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No Student Access Keys yet. Tap 'Generate New Student Key' to create a 1-device login key.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(keys, key = { it.keyCode }) { key ->
                val isClaimed = key.assignedUserId.isNotBlank()
                val isRevoked = key.status != "active"

                Card(
                    shape = MaterialTheme.shapes.medium,
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = key.keyCode,
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = when {
                                    isRevoked -> MaterialTheme.colorScheme.errorContainer
                                    isClaimed -> extendedColors.successContainer
                                    else -> extendedColors.warningContainer
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isRevoked -> "REVOKED"
                                        isClaimed -> "1-DEVICE LOCKED"
                                        else -> "AVAILABLE"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isRevoked -> MaterialTheme.colorScheme.onErrorContainer
                                        isClaimed -> extendedColors.onSuccessContainer
                                        else -> extendedColors.onWarningContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (key.label.isNotBlank()) {
                            Text(
                                text = "Label: ${key.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isClaimed) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${key.assignedEmail.ifBlank { key.assignedUserId }} • Device: ${DeviceIdProvider.shortLabel(key.boundDeviceId)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Unused — share this key with 1 student.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(key.keyCode))
                                    onPostMessage("Copied Key: ${key.keyCode}")
                                }) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy")
                                }
                                if (isClaimed) {
                                    TextButton(onClick = { onResetDevice(key.keyCode) }) {
                                        Icon(Icons.Rounded.LockReset, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reset Device")
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = { onToggleStatus(key) }) {
                                    Icon(
                                        imageVector = if (isRevoked) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                                        contentDescription = if (isRevoked) "Reactivate Key" else "Revoke Key",
                                        tint = if (isRevoked) extendedColors.success else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteKey(key) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Key", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
