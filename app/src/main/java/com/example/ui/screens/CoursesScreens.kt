package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unpublished
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CourseDraftInput
import com.example.data.CourseItem
import com.example.data.CsvMcqParser
import com.example.data.FormulaTextPreserver
import com.example.data.McqQuestion
import com.example.data.MedPrepConstants
import com.example.data.PlanEntitlements
import com.example.data.StudyMode
import com.example.data.StudyPost
import com.example.data.SubscriptionPlanTier
import com.example.data.UiState
import com.example.ui.components.FormulaText
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlanLockCard
import com.example.ui.components.PlanTierBadge
import com.example.ui.theme.LocalMedPrepColors
import com.example.ui.theme.subjectVisualFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCoursesScreen(
    activePlan: SubscriptionPlanTier,
    coursesState: UiState<List<CourseItem>>,
    publishedPosts: List<StudyPost>,
    publishedQuestions: List<McqQuestion>,
    isDarkTheme: Boolean,
    onOpenInteractiveLesson: (StudyPost) -> Unit,
    onPracticeCourseMcqs: (StudyMode, String?, Int) -> Unit,
    onOpenPlansTab: () -> Unit,
    onPostMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val ext = LocalMedPrepColors.current
    val canAccessFullCourses = PlanEntitlements.canAccessFullCourses(activePlan)
    val canOpenHtml = PlanEntitlements.canOpenInteractiveHtml(activePlan)
    val maxPracticeCap = PlanEntitlements.maxPracticeQuestionsPerSession(activePlan)
    var selectedSubject by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf<CourseItem?>(null) }

    val allCourses = (coursesState as? UiState.Success)?.data.orEmpty()
    val filteredCourses = remember(allCourses, selectedSubject, searchQuery) {
        allCourses.filter { c ->
            val matchesSubject = selectedSubject == "All" ||
                c.subject.equals(selectedSubject, ignoreCase = true) ||
                c.subject.equals("All Subjects", ignoreCase = true)
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                c.title.contains(q, ignoreCase = true) ||
                c.description.contains(q, ignoreCase = true) ||
                c.instructor.contains(q, ignoreCase = true) ||
                c.syllabusOutline.contains(q, ignoreCase = true)
            matchesSubject && matchesQuery
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ext.heroStart, ext.heroEnd)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Courses & Study Tracks",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (canAccessFullCourses) {
                                        "${allCourses.size} structured courses • Full Advance access active"
                                    } else {
                                        "${allCourses.size} courses preview • Upgrade to Advance (PKR 999/mo) for full syllabus & lessons"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // Search & Subject Filters
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(80) },
                    placeholder = { Text("Search courses, syllabus, or instructor…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_courses_search")
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (listOf("All") + MedPrepConstants.DEFAULT_SUBJECTS).forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = subj },
                            label = { Text(subj) }
                        )
                    }
                }
            }

            when (coursesState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = coursesState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    if (filteredCourses.isEmpty()) {
                        item {
                            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = if (allCourses.isEmpty()) "No courses published yet" else "No matching courses",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (allCourses.isEmpty()) {
                                            "Admin can add structured courses, modules, and linked lessons from the Admin Portal."
                                        } else {
                                            "Try clearing your search or switching the subject filter."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredCourses, key = { it.id }) { course ->
                            val visual = subjectVisualFor(course.subject)
                            val coursePosts = remember(course.id, course.subject, publishedPosts) {
                                findCoursePosts(course, publishedPosts)
                            }
                            val courseMcqsCount = remember(course.subject, publishedQuestions) {
                                if (course.subject.equals("All Subjects", ignoreCase = true)) {
                                    publishedQuestions.size
                                } else {
                                    publishedQuestions.count { it.subject.equals(course.subject, ignoreCase = true) }
                                }
                            }

                            Card(
                                onClick = { selectedCourse = course },
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("student_course_card_${course.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isDarkTheme) visual.containerDark else visual.containerLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = visual.icon,
                                                contentDescription = course.subject,
                                                tint = visual.accent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = course.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${course.subject} • ${course.level.replaceFirstChar { it.uppercase() }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = visual.accent
                                            )
                                        }
                                    }

                                    if (course.description.isNotBlank()) {
                                        Text(
                                            text = course.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CourseMetaPill(Icons.Rounded.Person, course.instructor)
                                        CourseMetaPill(Icons.Rounded.Schedule, course.durationLabel)
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${coursePosts.size} lessons/posts • $courseMcqsCount MCQs",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Button(
                                            onClick = { selectedCourse = course },
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            if (!canAccessFullCourses) {
                                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(5.dp))
                                            }
                                            Text(if (canAccessFullCourses) "Open Course" else "Preview")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Course Detail Bottom Sheet
    val viewingCourse = selectedCourse
    if (viewingCourse != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coursePosts = remember(viewingCourse.id, viewingCourse.subject, publishedPosts) {
            findCoursePosts(viewingCourse, publishedPosts)
        }
        val practiceSubject = if (viewingCourse.subject.equals("All Subjects", ignoreCase = true)) {
            null
        } else {
            viewingCourse.subject
        }

        ModalBottomSheet(
            onDismissRequest = { selectedCourse = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = viewingCourse.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourseMetaPill(Icons.Rounded.School, viewingCourse.subject)
                    CourseMetaPill(Icons.Rounded.Schedule, viewingCourse.durationLabel)
                    CourseMetaPill(Icons.Rounded.Person, viewingCourse.instructor)
                }

                if (viewingCourse.description.isNotBlank()) {
                    FormulaText(
                        text = viewingCourse.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (!canAccessFullCourses) {
                    PlanLockCard(
                        requiredTier = SubscriptionPlanTier.ADVANCE,
                        featureTitle = "Full Course Access & Syllabus",
                        description = "Upgrade to the Advance Plan (PKR 999/mo via JazzCash) to unlock the full course syllabus, attached video & web resources, interactive HTML lessons, and complete course MCQ drills.",
                        onUpgradeClick = {
                            selectedCourse = null
                            onOpenPlansTab()
                        },
                        testTag = "course_unlock_advance_button"
                    )
                } else {
                    if (viewingCourse.syllabusOutline.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Course Syllabus & Modules",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FormulaText(
                                    text = viewingCourse.syllabusOutline,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Quick actions: Practice MCQs + YouTube / Web links
                    Button(
                        onClick = {
                            selectedCourse = null
                            onPracticeCourseMcqs(StudyMode.PRACTICE, practiceSubject, minOf(25, maxPracticeCap))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("course_detail_practice_mcqs_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Practice ${viewingCourse.subject} MCQs")
                    }

                    if (!viewingCourse.youtubeUrl.isNullOrBlank() || !viewingCourse.webUrl.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!viewingCourse.youtubeUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { openExternalCourseUrl(context, viewingCourse.youtubeUrl, onPostMessage) },
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Video Lectures")
                                }
                            }
                            if (!viewingCourse.webUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { openExternalCourseUrl(context, viewingCourse.webUrl, onPostMessage) },
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Course Notes")
                                }
                            }
                        }
                    }

                    // Attached Posts & Interactive HTML Lessons
                    Text(
                        text = "Course Lessons & Posts (${coursePosts.size})",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (coursePosts.isEmpty()) {
                        Text(
                            text = "No posts attached to this course yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        coursePosts.forEach { post ->
                            Card(
                                onClick = {
                                    if (post.hasInteractiveHtml) {
                                        if (canOpenHtml) {
                                            selectedCourse = null
                                            onOpenInteractiveLesson(post)
                                        } else {
                                            selectedCourse = null
                                            onPostMessage("Interactive HTML lessons require Advance Plan (PKR 999/mo).")
                                            onOpenPlansTab()
                                        }
                                    } else if (!post.webUrl.isNullOrBlank()) {
                                        openExternalCourseUrl(context, post.webUrl, onPostMessage)
                                    } else if (!post.youtubeUrl.isNullOrBlank()) {
                                        openExternalCourseUrl(context, post.youtubeUrl, onPostMessage)
                                    } else {
                                        onPostMessage(post.body.ifBlank { post.title })
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (post.hasInteractiveHtml) Icons.Rounded.Code else Icons.AutoMirrored.Rounded.Article,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = post.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(post.topic.ifBlank { post.subject })
                                                if (post.hasInteractiveHtml) append(" • Interactive HTML")
                                                if (!post.youtubeUrl.isNullOrBlank()) append(" • Video")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AdminCoursesManagerTab(
    adminCoursesState: UiState<List<CourseItem>>,
    allPosts: List<StudyPost>,
    isDarkTheme: Boolean,
    onCreateCourse: () -> Unit,
    onEditCourse: (CourseItem) -> Unit,
    onAddPostForCourse: (CourseItem) -> Unit,
    onToggleCourseStatus: (CourseItem) -> Unit,
    onDeleteCourse: (CourseItem) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    val courses = (adminCoursesState as? UiState.Success)?.data.orEmpty()
    val filtered = remember(courses, search) {
        val q = search.trim()
        if (q.isEmpty()) courses
        else courses.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.subject.contains(q, ignoreCase = true) ||
                it.instructor.contains(q, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Courses Studio (${courses.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Create, edit, publish courses and attach posts/HTML lessons directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onCreateCourse,
                    modifier = Modifier.testTag("admin_new_course_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Course")
                }
            }
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it.take(80) },
                placeholder = { Text("Search courses by title, subject, or instructor…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when (adminCoursesState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = adminCoursesState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is UiState.Success -> {
                if (filtered.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("No courses created yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Tap 'New Course' to publish a structured MDCAT/Pre-Med course with syllabus modules and linked posts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { course ->
                        val visual = subjectVisualFor(course.subject)
                        val linkedPostsCount = allPosts.count {
                            it.courseId == course.id ||
                                (it.courseId.isNullOrBlank() && it.subject.equals(course.subject, ignoreCase = true))
                        }
                        val isPublished = course.status == "published"

                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_course_card_${course.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDarkTheme) visual.containerDark else visual.containerLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(visual.icon, contentDescription = null, tint = visual.accent, modifier = Modifier.size(22.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = course.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${course.subject} • ${course.durationLabel} • $linkedPostsCount posts",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = if (isPublished) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isPublished) "LIVE" else "DRAFT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isPublished) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (course.description.isNotBlank()) {
                                    Text(
                                        text = course.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onEditCourse(course) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("admin_edit_course_${course.id}")
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                    OutlinedButton(
                                        onClick = { onAddPostForCourse(course) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("admin_add_post_course_${course.id}")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Post")
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onToggleCourseStatus(course) }) {
                                        Icon(
                                            imageVector = if (isPublished) Icons.Rounded.Unpublished else Icons.Rounded.Publish,
                                            contentDescription = if (isPublished) "Move to Draft" else "Publish Course",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteCourse(course) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete Course",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CourseEditorBottomSheet(
    initialCourse: CourseItem?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CourseDraftInput) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initialCourse?.title ?: "") }
    var description by remember { mutableStateOf(initialCourse?.description ?: "") }
    var subject by remember { mutableStateOf(initialCourse?.subject ?: "Biology") }
    var instructor by remember { mutableStateOf(initialCourse?.instructor ?: "Dr. Irfan Ali") }
    var durationLabel by remember { mutableStateOf(initialCourse?.durationLabel ?: "4 Weeks") }
    var level by remember { mutableStateOf(initialCourse?.level ?: "intermediate") }
    var syllabusOutline by remember { mutableStateOf(initialCourse?.syllabusOutline ?: "") }
    var youtubeUrl by remember { mutableStateOf(initialCourse?.youtubeUrl ?: "") }
    var webUrl by remember { mutableStateOf(initialCourse?.webUrl ?: "") }
    var publishImmediately by remember { mutableStateOf((initialCourse?.status ?: "published") == "published") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (initialCourse == null) "Create New Course" else "Edit Course",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(160) },
                label = { Text("Course Title *") },
                placeholder = { Text("e.g. Complete MDCAT Organic Chemistry Crash Course") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("course_editor_title_input")
            )

            Text("Subject Track *", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (listOf("All Subjects") + MedPrepConstants.DEFAULT_SUBJECTS).forEach { subj ->
                    FilterChip(
                        selected = subject.equals(subj, ignoreCase = true),
                        onClick = { subject = subj },
                        label = { Text(subj) }
                    )
                }
            }

            Text("Course Level", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("beginner", "intermediate", "advanced").forEach { lvl ->
                    FilterChip(
                        selected = level == lvl,
                        onClick = { level = lvl },
                        label = { Text(lvl.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it.take(100) },
                    label = { Text("Instructor") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = durationLabel,
                    onValueChange = { durationLabel = it.take(60) },
                    label = { Text("Duration") },
                    placeholder = { Text("e.g. 6 Weeks") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(3000) },
                label = { Text("Course Overview & Description") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth().testTag("course_editor_description_input")
            )

            OutlinedTextField(
                value = syllabusOutline,
                onValueChange = { syllabusOutline = it.take(4000) },
                label = { Text("Syllabus / Modules Outline") },
                placeholder = { Text("Module 1: Fundamentals\nModule 2: Reactions & Formulas\nModule 3: Past Paper Drills") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth().testTag("course_editor_syllabus_input")
            )

            OutlinedTextField(
                value = youtubeUrl,
                onValueChange = { youtubeUrl = it.take(500) },
                label = { Text("Course YouTube Playlist / Intro Video (Optional)") },
                placeholder = { Text("https://www.youtube.com/playlist?list=...") },
                leadingIcon = { Icon(Icons.Rounded.PlayCircle, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = webUrl,
                onValueChange = { webUrl = it.take(500) },
                label = { Text("External Course Notes / Webpage Link (Optional)") },
                placeholder = { Text("https://example.com/course") },
                leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Publish to Student Courses Tab", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (publishImmediately) "Students can browse and study this course" else "Saved as Admin Draft",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = publishImmediately, onCheckedChange = { publishImmediately = it })
            }

            if (validationError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val cleanYt = if (youtubeUrl.isBlank()) null else CsvMcqParser.sanitizeOptionalHttpUrl(youtubeUrl)
                    val cleanWeb = if (webUrl.isBlank()) null else CsvMcqParser.sanitizeOptionalHttpUrl(webUrl)
                    when {
                        title.trim().length < 3 -> validationError = "Course title must be at least 3 characters."
                        youtubeUrl.isNotBlank() && cleanYt == null -> validationError = "YouTube link must be a valid https:// URL."
                        webUrl.isNotBlank() && cleanWeb == null -> validationError = "Web link must be a valid https:// URL."
                        else -> {
                            validationError = null
                            onSave(
                                CourseDraftInput(
                                    existingId = initialCourse?.id,
                                    title = title.trim(),
                                    description = FormulaTextPreserver.preserveExactStructure(description, 3000),
                                    subject = subject,
                                    instructor = instructor.trim().ifBlank { "MedPrep Faculty" },
                                    durationLabel = durationLabel.trim().ifBlank { "4 Weeks" },
                                    level = level,
                                    syllabusOutline = FormulaTextPreserver.preserveExactStructure(syllabusOutline, 4000),
                                    youtubeUrl = cleanYt,
                                    webUrl = cleanWeb,
                                    status = if (publishImmediately) "published" else "draft"
                                )
                            )
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("course_editor_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (initialCourse == null) "Create Course" else "Save Changes")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun CourseMetaPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun findCoursePosts(course: CourseItem, posts: List<StudyPost>): List<StudyPost> {
    val explicit = posts.filter { !it.courseId.isNullOrBlank() && it.courseId == course.id }
    val subjectFallback = if (course.subject.equals("All Subjects", ignoreCase = true)) {
        posts
    } else {
        posts.filter { it.courseId.isNullOrBlank() && it.subject.equals(course.subject, ignoreCase = true) }
    }
    return (explicit + subjectFallback).distinctBy { it.id }
}

private fun openExternalCourseUrl(context: android.content.Context, rawUrl: String?, onPostMessage: (String) -> Unit) {
    val clean = rawUrl?.trim().orEmpty()
    if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
        onPostMessage("Invalid course link.")
        return
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clean)))
    }.onFailure {
        onPostMessage("No app available to open link.")
    }
}
