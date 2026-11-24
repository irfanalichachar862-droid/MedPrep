package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.McqQuestion
import com.example.data.MedPrepConstants
import com.example.data.StudyPost
import com.example.data.TopicLinkResolver
import com.example.data.UiState
import com.example.ui.components.FormulaText
import com.example.ui.theme.subjectVisualFor

@Composable
fun PostsFeedScreen(
    postsState: UiState<List<StudyPost>>,
    publishedQuestions: List<McqQuestion>,
    isDarkTheme: Boolean,
    onOpenInteractiveLesson: (StudyPost) -> Unit,
    onPracticeLinkedMcqs: (StudyPost) -> Unit,
    onPostMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedSubject by remember { mutableStateOf("All") }
    val posts = (postsState as? UiState.Success)?.data.orEmpty()

    val filtered = remember(posts, selectedSubject) {
        if (selectedSubject == "All") posts
        else posts.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.posts_feed_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.posts_feed_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (listOf("All") + MedPrepConstants.DEFAULT_SUBJECTS).forEach { subj ->
                        val visual = if (subj == "All") null else subjectVisualFor(subj)
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = subj },
                            leadingIcon = visual?.let { v ->
                                { Icon(v.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            },
                            label = { Text(subj) }
                        )
                    }
                }
            }

            when (postsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = postsState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (filtered.isEmpty()) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Article,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Text(
                                        text = "No Study Posts Yet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Admin-published web articles, study guides, and YouTube lecture recommendations will appear here automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { post ->
                            val linkedCount = remember(post.id, post.subject, post.topic, publishedQuestions) {
                                TopicLinkResolver.findLinkedQuestions(post, publishedQuestions).size
                            }
                            StudyPostCard(
                                post = post,
                                linkedMcqCount = linkedCount,
                                isDarkTheme = isDarkTheme,
                                onOpenInteractiveHtml = { onOpenInteractiveLesson(post) },
                                onPracticeLinkedMcqs = { onPracticeLinkedMcqs(post) },
                                onOpenUrl = { url -> openExternalLink(context, url, onPostMessage) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun StudyPostCard(
    post: StudyPost,
    linkedMcqCount: Int,
    isDarkTheme: Boolean,
    onOpenInteractiveHtml: () -> Unit,
    onPracticeLinkedMcqs: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val visual = subjectVisualFor(post.subject)
    val badgeBg = if (isDarkTheme) visual.containerDark else visual.containerLight
    val badgeFg = if (isDarkTheme) visual.containerLight else visual.accent

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study_post_card_${post.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(visual.icon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(14.dp))
                        Text(post.subject, style = MaterialTheme.typography.labelSmall, color = badgeFg)
                    }
                }
                if (post.topic.isNotBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = post.topic,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (post.hasInteractiveHtml) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "INTERACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (post.body.isNotBlank()) {
                FormulaText(
                    text = post.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (post.hasInteractiveHtml) {
                Button(
                    onClick = onOpenInteractiveHtml,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("post_open_interactive_html_button")
                ) {
                    Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Interactive Topic Explanation")
                }
            }

            if (linkedMcqCount > 0) {
                OutlinedButton(
                    onClick = onPracticeLinkedMcqs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("post_practice_linked_mcqs_button")
                ) {
                    Icon(Icons.Rounded.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Practice $linkedMcqCount Linked MCQs")
                }
            }

            val webUrl = post.webUrl?.takeIf { it.isNotBlank() }
            val youtubeUrl = post.youtubeUrl?.takeIf { it.isNotBlank() }

            if (webUrl != null || youtubeUrl != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (webUrl != null) {
                        OutlinedButton(
                            onClick = { onOpenUrl(webUrl) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("post_open_web_button")
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Web Link")
                        }
                    }
                    if (youtubeUrl != null) {
                        Button(
                            onClick = { onOpenUrl(youtubeUrl) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("post_open_youtube_button")
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Watch YouTube")
                        }
                    }
                }
            }
        }
    }
}

fun openExternalLink(context: Context, rawUrl: String, onError: (String) -> Unit = {}) {
    try {
        val safeUrl = rawUrl.trim()
        if (!safeUrl.startsWith("http://", ignoreCase = true) && !safeUrl.startsWith("https://", ignoreCase = true)) {
            onError("Invalid web link.")
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        onError("No browser or YouTube app available to open link.")
    }
}

fun buildSuggestedYoutubeUrl(subject: String, topic: String, stem: String, explicitUrl: String?): String {
    val cleanExplicit = explicitUrl?.trim().orEmpty()
    if (cleanExplicit.startsWith("https://", ignoreCase = true) || cleanExplicit.startsWith("http://", ignoreCase = true)) {
        return cleanExplicit
    }
    val query = listOf(subject, topic.ifBlank { stem.take(45) }, "MDCAT explanation")
        .filter { it.isNotBlank() }
        .joinToString(" ")
    return "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
}
