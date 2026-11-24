package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.PriorityHigh
import com.example.data.StudyNoteDraft
import com.example.data.McqQuestion
import com.example.data.PlanEntitlements
import com.example.data.QuestionAttempt
import com.example.data.ShuffledQuestion
import com.example.data.StudyMode
import com.example.data.StudyPost
import com.example.data.SubscriptionPlanTier
import com.example.data.TopicLinkResolver
import com.example.ui.components.FormulaText
import com.example.ui.theme.LocalMedPrepColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.subjectVisualFor
import com.example.viewmodel.ActiveQuizState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizSessionScreen(
    quizState: ActiveQuizState,
    attemptsMap: Map<String, QuestionAttempt>,
    publishedPosts: List<StudyPost>,
    activePlan: SubscriptionPlanTier = SubscriptionPlanTier.FREE,
    currentNotesCount: Int = 0,
    difficultSourceIds: Set<String> = emptySet(),
    showInstantReasonSetting: Boolean = true,
    onSelectAnswer: (ShuffledQuestion, String) -> Unit,
    onToggleBookmark: (McqQuestion) -> Unit,
    onToggleDifficult: (McqQuestion) -> Unit = {},
    onSaveQuickNote: (StudyNoteDraft, () -> Unit) -> Unit = { _, done -> done() },
    onOpenInteractiveLesson: (StudyPost) -> Unit,
    onOpenPlansTab: () -> Unit = {},
    onNavigateTo: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onRetest: () -> Unit,
    onEnterReview: () -> Unit,
    onPostMessage: (String) -> Unit,
    onExit: () -> Unit
) {
    BackHandler { onExit() }

    if (quizState.completed && !quizState.reviewingSummary) {
        QuizSummaryScreen(
            quizState = quizState,
            onRetest = onRetest,
            onReviewAnswers = onEnterReview,
            onExit = onExit
        )
        return
    }

    val context = LocalContext.current
    val shuffled = quizState.currentShuffled ?: return
    val question = shuffled.question
    val extendedColors = LocalMedPrepColors.current
    val selectedDisplayLetter = quizState.selectedAnswers[question.id]
    val showFeedback = quizState.reviewingSummary ||
        (quizState.mode != StudyMode.EXAM && selectedDisplayLetter != null && showInstantReasonSetting)
    val isBookmarked = attemptsMap[question.id]?.bookmarked == true
    val isMarkedDifficult = question.id in difficultSourceIds
    val canUseDifficultVault = PlanEntitlements.canUseDifficultVault(activePlan)
    val maxNotesAllowed = PlanEntitlements.maxStudyNotes(activePlan)
    val canOpenInteractiveHtml = PlanEntitlements.canOpenInteractiveHtml(activePlan)
    val linkedPost = remember(question.id, question.linkedPostId, question.subject, question.topic, publishedPosts) {
        TopicLinkResolver.findLinkedPost(question, publishedPosts)
    }
    var showNavigator by remember { mutableStateOf(false) }
    var showQuickNoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.testTag("quiz_close_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Exit session")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (quizState.reviewingSummary) "Answer Review" else quizState.mode.label,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Q ${quizState.currentIndex + 1} of ${quizState.questions.size} • Attempt #${quizState.attemptNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    val timerSeconds = quizState.remainingSeconds ?: quizState.elapsedSeconds
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (quizState.remainingSeconds != null && timerSeconds < 60L) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDuration(timerSeconds),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (canUseDifficultVault) {
                                onToggleDifficult(question)
                            } else {
                                onPostMessage("Difficult Exam Vault requires Basic Plan (PKR 499/mo via JazzCash).")
                                onOpenPlansTab()
                            }
                        },
                        modifier = Modifier.testTag("quiz_difficult_button")
                    ) {
                        Icon(
                            imageVector = if (canUseDifficultVault) Icons.Rounded.PriorityHigh else Icons.Rounded.Lock,
                            contentDescription = "Mark as difficult exam question",
                            tint = if (isMarkedDifficult && canUseDifficultVault) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            if (currentNotesCount >= maxNotesAllowed) {
                                onPostMessage("Free Plan supports up to $maxNotesAllowed study notes. Upgrade to Basic (PKR 499/mo) for unlimited notes!")
                                onOpenPlansTab()
                            } else {
                                showQuickNoteDialog = true
                            }
                        },
                        modifier = Modifier.testTag("quiz_add_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkAdded,
                            contentDescription = "Save highlight or note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { onToggleBookmark(question) },
                        modifier = Modifier.testTag("quiz_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Bookmark question",
                            tint = if (isBookmarked) extendedColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showNavigator = true },
                        modifier = Modifier.testTag("quiz_navigator_button")
                    ) {
                        Icon(Icons.Rounded.GridView, contentDescription = "Question grid")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPrevious,
                        enabled = quizState.currentIndex > 0,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("quiz_prev_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.quiz_previous))
                    }

                    val isLast = quizState.currentIndex == quizState.questions.lastIndex
                    Button(
                        onClick = {
                            if (quizState.reviewingSummary) {
                                if (isLast) onExit() else onNext()
                            } else if (isLast) {
                                onFinish()
                            } else {
                                onNext()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quiz_next_button")
                    ) {
                        Text(
                            text = when {
                                quizState.reviewingSummary && isLast -> stringResource(R.string.quiz_back_home)
                                isLast -> stringResource(R.string.quiz_finish)
                                else -> stringResource(R.string.quiz_next)
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (quizState.currentIndex + 1).toFloat() / quizState.questions.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    strokeCap = StrokeCap.Round
                )

                // Subject, Topic, Difficulty & Shuffled Badge
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val subjectVisual = subjectVisualFor(question.subject)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = subjectVisual.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = question.subject,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (question.topic.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = question.topic,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "SHUFFLED A–D",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Linked Interactive Topic Explanation Shortcut (available anytime during Practice/Review)
                if (linkedPost != null && linkedPost.hasInteractiveHtml && quizState.mode != StudyMode.EXAM) {
                    Surface(
                        onClick = {
                            if (canOpenInteractiveHtml) {
                                onOpenInteractiveLesson(linkedPost)
                            } else {
                                onPostMessage("Interactive HTML topic lessons require Advance Plan (PKR 999/mo via JazzCash).")
                                onOpenPlansTab()
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_open_linked_topic_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (canOpenInteractiveHtml) Icons.Rounded.Code else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (canOpenInteractiveHtml) {
                                        "Interactive Topic Lesson Linked"
                                    } else {
                                        "Interactive Topic Lesson (Advance Plan)"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (canOpenInteractiveHtml) {
                                        "Tap to understand '${linkedPost.title}' visually"
                                    } else {
                                        "Upgrade to Advance (PKR 999/mo) to open '${linkedPost.title}'"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Open interactive topic lesson",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Clinical / Chemistry Stem Card (exact structure preserved via FormulaText)
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FormulaText(
                        text = question.stem,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(18.dp)
                    )
                }

                // Randomized Options A–D (Shuffled every attempt & retest)
                shuffled.options.forEach { option ->
                    val displayLetter = option.displayLetter
                    val isSelected = selectedDisplayLetter == displayLetter
                    val isCorrectOption = displayLetter.equals(shuffled.correctDisplayLetter, ignoreCase = true)

                    val borderColor = when {
                        showFeedback && isCorrectOption -> extendedColors.success
                        showFeedback && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.error
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    val containerColor = when {
                        showFeedback && isCorrectOption -> extendedColors.successContainer
                        showFeedback && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.errorContainer
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        onClick = { onSelectAnswer(shuffled, displayLetter) },
                        enabled = !quizState.reviewingSummary,
                        shape = MaterialTheme.shapes.medium,
                        color = containerColor,
                        border = BorderStroke(if (isSelected || (showFeedback && isCorrectOption)) 2.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_option_$displayLetter")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            showFeedback && isCorrectOption -> extendedColors.success
                                            showFeedback && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.error
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayLetter,
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                    color = when {
                                        (showFeedback && (isCorrectOption || isSelected)) || isSelected ->
                                            MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            FormulaText(
                                text = option.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            if (showFeedback && isCorrectOption) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Correct option",
                                    tint = extendedColors.success,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else if (showFeedback && isSelected && !isCorrectOption) {
                                Icon(
                                    imageVector = Icons.Rounded.Cancel,
                                    contentDescription = "Incorrect option",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Reason Card + Suggested YouTube Topic Video Card (Shown right after answer selection)
                AnimatedVisibility(visible = showFeedback, enter = fadeIn()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_reason_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Why Option ${shuffled.correctDisplayLetter} Is Correct (Reason)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                FormulaText(
                                    text = question.explanation.ifBlank {
                                        "No additional reason was attached to this question yet."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Linked Interactive HTML / Topic Concept Card right under Reason
                        if (linkedPost != null) {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (linkedPost.hasInteractiveHtml) Icons.Rounded.Code else Icons.Rounded.MenuBook,
                                            contentDescription = "Linked Topic Explanation",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (linkedPost.hasInteractiveHtml) "Interactive Topic Explanation" else "Linked Topic Study Guide",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = linkedPost.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (linkedPost.hasInteractiveHtml) {
                                                if (canOpenInteractiveHtml) {
                                                    onOpenInteractiveLesson(linkedPost)
                                                } else {
                                                    onPostMessage("Interactive HTML topic lessons require Advance Plan (PKR 999/mo via JazzCash).")
                                                    onOpenPlansTab()
                                                }
                                            } else if (!linkedPost.webUrl.isNullOrBlank()) {
                                                openExternalLink(context, linkedPost.webUrl, onPostMessage)
                                            } else {
                                                onOpenInteractiveLesson(linkedPost)
                                            }
                                        },
                                        modifier = Modifier.testTag("quiz_open_linked_lesson_button")
                                    ) {
                                        if (linkedPost.hasInteractiveHtml && !canOpenInteractiveHtml) {
                                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Advance")
                                        } else {
                                            Text("Explain")
                                        }
                                    }
                                }
                            }
                        }

                        // Suggested YouTube Video discussing this MCQ topic
                        val ytUrl = remember(question.id, question.youtubeUrl, question.subject, question.topic) {
                            buildSuggestedYoutubeUrl(
                                subject = question.subject,
                                topic = question.topic,
                                stem = question.stem,
                                explicitUrl = question.youtubeUrl
                            )
                        }
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626).copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayCircle,
                                        contentDescription = "YouTube Video",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Suggested YouTube Lecture",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (!question.youtubeUrl.isNullOrBlank()) {
                                            "Admin-recommended video for ${question.topic.ifBlank { question.subject }}"
                                        } else {
                                            "Watch topic discussion: ${question.subject} • ${question.topic.ifBlank { "Concept Review" }}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { openExternalLink(context, ytUrl, onPostMessage) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("quiz_youtube_video_button")
                                ) {
                                    Text("Watch")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showNavigator) {
        ModalBottomSheet(onDismissRequest = { showNavigator = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question Navigator",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                showNavigator = false
                                onRetest()
                            },
                            modifier = Modifier.testTag("quiz_navigator_retest_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retest")
                        }
                        if (!quizState.reviewingSummary) {
                            Button(
                                onClick = {
                                    showNavigator = false
                                    onFinish()
                                }
                            ) {
                                Text("Finish")
                            }
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quizState.questions.forEachIndexed { idx, sq ->
                        val answered = quizState.selectedAnswers.containsKey(sq.question.id)
                        val isCurrent = idx == quizState.currentIndex
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                answered -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    onNavigateTo(idx)
                                    showNavigator = false
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                    color = when {
                                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                                        answered -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showQuickNoteDialog) {
        AddNoteDialog(
            initialSubject = question.subject,
            initialTitle = question.topic.ifBlank { "${question.subject} Highlight" },
            initialContent = buildString {
                appendLine(question.stem.take(220))
                if (question.explanation.isNotBlank()) {
                    appendLine()
                    append("Reason: ${question.explanation}")
                }
            },
            linkedQuestionId = question.id,
            isMutating = false,
            onDismiss = { showQuickNoteDialog = false },
            onSave = { draft ->
                onSaveQuickNote(draft) { showQuickNoteDialog = false }
            }
        )
    }
}

@Composable
private fun QuizSummaryScreen(
    quizState: ActiveQuizState,
    onRetest: () -> Unit,
    onReviewAnswers: () -> Unit,
    onExit: () -> Unit
) {
    val extendedColors = LocalMedPrepColors.current
    val total = quizState.questions.size.coerceAtLeast(1)
    val scorePercent = ((quizState.correctCount * 100) / total)

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                if (scorePercent >= 70) extendedColors.successContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$scorePercent%",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFontFamily),
                            color = if (scorePercent >= 70) extendedColors.onSuccessContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = stringResource(R.string.quiz_summary_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "${quizState.mode.label} • ${quizState.subjectFilter} • Attempt #${quizState.attemptNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatTile(
                            label = "Correct",
                            value = "${quizState.correctCount}/$total",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            label = "Answered",
                            value = "${quizState.answeredCount}",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            label = "Time",
                            value = formatDuration(quizState.elapsedSeconds),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Retest with freshly shuffled questions & randomized A/B/C/D options
                    Button(
                        onClick = onRetest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("quiz_retest_button")
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.quiz_retest_shuffled))
                    }

                    OutlinedButton(
                        onClick = onReviewAnswers,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("quiz_review_answers_button")
                    ) {
                        Text(stringResource(R.string.quiz_review_mistakes))
                    }

                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("quiz_back_home_button")
                    ) {
                        Text(stringResource(R.string.quiz_back_home))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}
