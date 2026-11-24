package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DownloadedMcqPack
import com.example.data.McqQuestion
import com.example.data.MedPrepConstants
import com.example.data.PdfMcqExporter
import com.example.data.PlanEntitlements
import com.example.data.QuestionAttempt
import com.example.data.StudyMode
import com.example.data.SubjectStats
import com.example.data.SubscriptionPlanTier
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.PlanTierBadge
import com.example.ui.theme.LocalMedPrepColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.subjectVisualFor
import java.util.Calendar

@Composable
fun DashboardScreen(
    profileState: UiState<UserProfile?>,
    publishedState: UiState<List<McqQuestion>>,
    attemptsMap: Map<String, QuestionAttempt>,
    downloadedPacks: List<DownloadedMcqPack>,
    isDarkTheme: Boolean,
    isSyncing: Boolean,
    onToggleTheme: () -> Unit,
    onSyncNow: () -> Unit,
    onStartStudy: (StudyMode, String?, Int) -> Unit,
    onStartCustomQuiz: (String, List<McqQuestion>) -> Unit,
    onSaveOfflinePack: (String, String, List<McqQuestion>) -> Unit,
    onOpenMoreTab: (MoreMenuTab) -> Unit,
    onPostMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalMedPrepColors.current
    val profile = (profileState as? UiState.Success)?.data
    val activePlan = SubscriptionPlanTier.fromKey(profile?.subscriptionPlan)
    val maxPracticeCap = PlanEntitlements.maxPracticeQuestionsPerSession(activePlan)
    val canTimedExam = PlanEntitlements.canUseTimedExam(activePlan)
    val canDownloadPdfAndPacks = PlanEntitlements.canDownloadPdfAndOfflinePacks(activePlan)
    // Strictly keep ONLY the 5 core subjects
    val questions = remember(publishedState) {
        (publishedState as? UiState.Success)?.data
            ?.filter { MedPrepConstants.isCoreSubject(it.subject) }
            .orEmpty()
    }

    var showThreeDotsMenu by remember { mutableStateOf(false) }

    val downloadStudyPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfMcqExporter.writeMcqsPdf(
                        outputStream = out,
                        title = "MedPrep — Complete 5-Subject MCQs",
                        subtitle = "Offline PDF with Answer Keys & Reasons",
                        questions = questions
                    )
                }
                onSaveOfflinePack("All Subjects Interactive Pack", "All Subjects", questions)
                onPostMessage("Downloaded ${questions.size} MCQs as PDF + Interactive Offline Pack!")
            } catch (e: Exception) {
                onPostMessage("PDF download failed: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> R.string.study_greeting_morning
            hour < 17 -> R.string.study_greeting_afternoon
            else -> R.string.study_greeting_evening
        }
    }

    val mistakesCount = remember(questions, attemptsMap) {
        questions.count { q ->
            val a = attemptsMap[q.id]
            a != null && a.answered && !a.correct
        }
    }
    val bookmarksCount = remember(questions, attemptsMap) {
        questions.count { q -> attemptsMap[q.id]?.bookmarked == true }
    }

    // Strictly ONLY the 5 core subjects (Biology, Chemistry, Physics, English, Logical Reasoning)
    val subjectStats = remember(questions, attemptsMap) {
        MedPrepConstants.DEFAULT_SUBJECTS.map { subjectName ->
            val matching = questions.filter { it.subject.equals(subjectName, ignoreCase = true) }
            val answered = matching.count { attemptsMap[it.id]?.answered == true }
            val correct = matching.count { attemptsMap[it.id]?.correct == true }
            SubjectStats(
                subject = subjectName,
                totalQuestions = matching.size,
                answeredCount = answered,
                correctCount = correct
            )
        }
    }

    val totalAnswered = profile?.totalAnswered ?: attemptsMap.values.count { it.answered }.toLong()
    val totalCorrect = profile?.totalCorrect ?: attemptsMap.values.count { it.correct }.toLong()
    val accuracyPercent = if (totalAnswered > 0) ((totalCorrect * 100) / totalAnswered).toInt() else 0
    val dailyGoal = (profile?.dailyGoal ?: 25L).coerceAtLeast(1L)
    val goalProgress = (attemptsMap.values.count { it.answered }.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

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
            // Clinical Hero Banner (Admin login is strictly inside Three-Dots -> Settings & Backup, NEVER on top of screen)
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(extendedColors.heroStart, extendedColors.heroEnd)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${stringResource(greeting)}, ${profile?.displayName?.substringBefore(" ") ?: "Student"}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = profile?.targetExam ?: "MDCAT",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        PlanTierBadge(
                                            tier = activePlan,
                                            modifier = Modifier
                                                .clickable { onOpenMoreTab(MoreMenuTab.PLANS) }
                                                .testTag("dashboard_plan_badge")
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Quick Light/Dark Theme Toggle
                                    Surface(
                                        color = Color.White.copy(alpha = 0.16f),
                                        shape = CircleShape,
                                        modifier = Modifier.testTag("dashboard_theme_toggle")
                                    ) {
                                        IconButton(
                                            onClick = onToggleTheme,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                                contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Offline Sync Pill
                                    Surface(
                                        color = Color.White.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.testTag("dashboard_sync_button")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clickable(enabled = !isSyncing, onClick = onSyncNow)
                                                .padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Rounded.CloudSync,
                                                    contentDescription = "Sync offline bank",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isSyncing) "Sync" else "${questions.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Three-Dots Menu Button (Opens all 8 Liquid Glass Menu Tabs)
                                    Box {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.18f),
                                            shape = CircleShape,
                                            modifier = Modifier.testTag("dashboard_three_dots_menu")
                                        ) {
                                            IconButton(
                                                onClick = { showThreeDotsMenu = true },
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MoreVert,
                                                    contentDescription = "Open Menu Hub",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showThreeDotsMenu,
                                            onDismissRequest = { showThreeDotsMenu = false }
                                        ) {
                                            MoreMenuTab.entries.forEach { tab ->
                                                DropdownMenuItem(
                                                    text = { Text(tab.label) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = tab.selectedIcon,
                                                            contentDescription = tab.label,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    },
                                                    onClick = {
                                                        showThreeDotsMenu = false
                                                        onOpenMoreTab(tab)
                                                    },
                                                    modifier = Modifier.testTag("menu_item_${tab.id}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HeroMetricPill(
                                    label = "BANK",
                                    value = "${questions.size}",
                                    modifier = Modifier.weight(1f)
                                )
                                HeroMetricPill(
                                    label = "ANSWERED",
                                    value = "$totalAnswered",
                                    modifier = Modifier.weight(1f)
                                )
                                HeroMetricPill(
                                    label = "ACCURACY",
                                    value = "$accuracyPercent%",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Daily Goal Progress
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.study_daily_goal),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = "${attemptsMap.values.count { it.answered }} / $dailyGoal MCQs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { goalProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = Color(0xFF6EE7B7),
                                    trackColor = Color.White.copy(alpha = 0.22f),
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }

            when (publishedState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
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
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = publishedState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onSyncNow) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Retry")
                                }
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    if (questions.isEmpty()) {
                        item {
                            StudentEmptyBankCard(onSyncNow = onSyncNow)
                        }
                    } else {
                        // Primary Launch Actions (Shuffled Questions + Shuffled A/B/C/D Options)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ModeLaunchCard(
                                    title = stringResource(R.string.study_quick_practice),
                                    subtitle = if (activePlan == SubscriptionPlanTier.FREE) {
                                        "Free Plan: up to $maxPracticeCap MCQs/run"
                                    } else {
                                        stringResource(R.string.study_quick_practice_sub)
                                    },
                                    icon = Icons.Rounded.Quiz,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    tag = "start_practice_button",
                                    onClick = { onStartStudy(StudyMode.PRACTICE, null, minOf(20, maxPracticeCap)) },
                                    modifier = Modifier.weight(1f)
                                )
                                ModeLaunchCard(
                                    title = stringResource(R.string.study_timed_exam),
                                    subtitle = if (canTimedExam) {
                                        stringResource(R.string.study_timed_exam_sub)
                                    } else {
                                        "Locked • Basic Plan (PKR 499/mo)"
                                    },
                                    icon = if (canTimedExam) Icons.Rounded.Timer else Icons.Rounded.Lock,
                                    containerColor = if (canTimedExam) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = if (canTimedExam) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    tag = "start_exam_button",
                                    onClick = {
                                        if (canTimedExam) {
                                            onStartStudy(StudyMode.EXAM, null, minOf(15, questions.size))
                                        } else {
                                            onPostMessage("Timed Exam Mode requires Basic Plan (PKR 499/mo via JazzCash).")
                                            onOpenMoreTab(MoreMenuTab.PLANS)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Secondary Review Chips (Mistakes & Bookmarks)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CompactReviewCard(
                                    title = stringResource(R.string.study_mistakes),
                                    count = mistakesCount,
                                    icon = Icons.Rounded.Replay,
                                    accent = MaterialTheme.colorScheme.error,
                                    tag = "review_mistakes_button",
                                    onClick = { onStartStudy(StudyMode.MISTAKES, null, maxPracticeCap) },
                                    modifier = Modifier.weight(1f)
                                )
                                CompactReviewCard(
                                    title = stringResource(R.string.study_bookmarks),
                                    count = bookmarksCount,
                                    icon = Icons.Rounded.Bookmarks,
                                    accent = extendedColors.warning,
                                    tag = "review_bookmarks_button",
                                    onClick = { onStartStudy(StudyMode.BOOKMARKS, null, maxPracticeCap) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Download MCQs as PDF + Interactive Offline Mode Card (Basic Plan+)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        if (canDownloadPdfAndPacks) {
                                            downloadStudyPdfLauncher.launch("MedPrep_MCQs_Study_Sheet.pdf")
                                        } else {
                                            onPostMessage("PDF & Interactive Offline MCQ Packs require Basic Plan (PKR 499/mo).")
                                            onOpenMoreTab(MoreMenuTab.PLANS)
                                        }
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("dashboard_download_mcqs_button")
                                ) {
                                    Icon(
                                        imageVector = if (canDownloadPdfAndPacks) Icons.Rounded.PictureAsPdf else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (canDownloadPdfAndPacks) {
                                            "Download ${questions.size} MCQs as PDF + Interactive Pack"
                                        } else {
                                            "Unlock PDF & Offline Pack (Basic Plan • PKR 499/mo)"
                                        }
                                    )
                                }

                                val latestPack = downloadedPacks.firstOrNull()
                                if (latestPack != null) {
                                    Surface(
                                        onClick = {
                                            if (canDownloadPdfAndPacks) {
                                                onStartCustomQuiz(latestPack.title, latestPack.questions)
                                            } else {
                                                onPostMessage("Interactive Offline Packs require Basic Plan (PKR 499/mo).")
                                                onOpenMoreTab(MoreMenuTab.PLANS)
                                            }
                                        },
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("dashboard_run_downloaded_pack_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (canDownloadPdfAndPacks) Icons.Rounded.PlayArrow else Icons.Rounded.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (canDownloadPdfAndPacks) {
                                                        "Run Downloaded MCQs Interactively"
                                                    } else {
                                                        "Offline Pack Locked (Basic Plan)"
                                                    },
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "${latestPack.title} (${latestPack.questions.size} MCQs offline-ready)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                contentDescription = "Launch offline interactive quiz",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Strictly ONLY the 5 Core Subjects
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.study_subjects_heading),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (activePlan == SubscriptionPlanTier.FREE) "5 subjects • 10 MCQs/run" else "5 subjects",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(subjectStats, key = { it.subject }) { stat ->
                        SubjectMasteryCard(
                            stats = stat,
                            isDarkTheme = isDarkTheme,
                            onClick = { onStartStudy(StudyMode.PRACTICE, stat.subject, minOf(25, maxPracticeCap)) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.13f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModeLaunchCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        modifier = modifier
            .height(142.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactReviewCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$count MCQs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubjectMasteryCard(
    stats: SubjectStats,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val visual = subjectVisualFor(stats.subject)
    val badgeBg = if (isDarkTheme) visual.containerDark else visual.containerLight
    val iconTint = if (isDarkTheme) visual.containerLight else visual.accent

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${stats.subject.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = stats.subject,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stats.subject,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (stats.totalQuestions == 0) {
                                "0 MCQs published yet"
                            } else {
                                "${stats.answeredCount}/${stats.totalQuestions} practiced • ${stats.accuracyPercent}% accuracy"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Practice ${stats.subject}",
                    tint = visual.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            LinearProgressIndicator(
                progress = { stats.masteryFraction },
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

@Composable
private fun StudentEmptyBankCard(onSyncNow: () -> Unit) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Waiting for Admin MCQs",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "As soon as Admin publishes MCQs for Biology, Chemistry, Physics, English, or Logical Reasoning, they will sync here automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onSyncNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_bank_sync_button")
            ) {
                Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check & Sync Now")
            }
        }
    }
}
