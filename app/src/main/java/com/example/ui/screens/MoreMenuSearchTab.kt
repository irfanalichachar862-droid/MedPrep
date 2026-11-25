package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CourseItem
import com.example.data.DifficultQuestionItem
import com.example.data.McqQuestion
import com.example.data.MedPrepConstants
import com.example.data.PlanEntitlements
import com.example.data.StudyMode
import com.example.data.StudyNote
import com.example.data.StudyPost
import com.example.data.SubscriptionPlanTier
import com.example.ui.components.FormulaText
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.subjectVisualFor

enum class SearchFilterCategory(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    ALL("all", "All", Icons.Rounded.Search),
    QUESTIONS("questions", "Questions", Icons.Rounded.Quiz),
    TABS_FUNCTIONS("tabs", "Tabs & Functions", Icons.Rounded.Widgets),
    COURSES("courses", "Courses", Icons.Rounded.School),
    LESSONS("lessons", "Lessons & Posts", Icons.AutoMirrored.Rounded.Article),
    NOTES("notes", "Notes", Icons.Rounded.BookmarkAdded)
}

private data class AppSearchableFunction(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val keywords: List<String>,
    val icon: ImageVector,
    val isDestructive: Boolean = false,
    val onInvoke: () -> Unit
)

@Composable
fun SearchFindTabContent(
    activePlan: SubscriptionPlanTier,
    publishedQuestions: List<McqQuestion>,
    publishedCourses: List<CourseItem>,
    publishedPosts: List<StudyPost>,
    studyNotes: List<StudyNote>,
    difficultQuestions: List<DifficultQuestionItem>,
    isDarkTheme: Boolean,
    isAdminUser: Boolean,
    onJumpToMoreTab: (MoreMenuTab) -> Unit,
    onNavigateMainTab: (String) -> Unit,
    onStartStudy: (StudyMode, String?, Int) -> Unit,
    onStartCustomQuiz: (String, List<McqQuestion>) -> Unit,
    onOpenInteractiveLesson: (StudyPost) -> Unit,
    onToggleTheme: () -> Unit,
    onSyncNow: () -> Unit,
    onSaveOfflineSnapshotNow: () -> Unit,
    onOpenAdminPanel: (() -> Unit)?,
    onSignOut: () -> Unit,
    onPostMessage: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilterId by rememberSaveable { mutableStateOf(SearchFilterCategory.ALL.id) }
    var selectedSubject by rememberSaveable { mutableStateOf("All") }

    val selectedFilter = remember(selectedFilterId) {
        SearchFilterCategory.entries.firstOrNull { it.id == selectedFilterId } ?: SearchFilterCategory.ALL
    }
    val trimmedQuery = query.trim()
    val maxPracticeCap = PlanEntitlements.maxPracticeQuestionsPerSession(activePlan)
    val canTimedExam = PlanEntitlements.canUseTimedExam(activePlan)
    val canOpenHtml = PlanEntitlements.canOpenInteractiveHtml(activePlan)

    val allFunctions = remember(
        activePlan,
        isDarkTheme,
        isAdminUser,
        maxPracticeCap,
        canTimedExam
    ) {
        buildList {
            // Core App Navigation Tabs
            add(
                AppSearchableFunction(
                    id = "tab_study",
                    title = "Study Dashboard",
                    subtitle = "Home dashboard with 5 core subjects, daily goal & accuracy stats",
                    badge = "Main Tab",
                    keywords = listOf("study", "home", "dashboard", "subjects", "biology", "chemistry", "physics", "english", "logical"),
                    icon = Icons.Rounded.Dashboard,
                    onInvoke = { onNavigateMainTab("study") }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_courses",
                    title = "Courses Tab",
                    subtitle = "Browse subject courses, syllabus outlines & linked MCQs",
                    badge = "Main Tab",
                    keywords = listOf("course", "courses", "syllabus", "lectures", "class"),
                    icon = Icons.Rounded.School,
                    onInvoke = { onNavigateMainTab("courses") }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_posts",
                    title = "Lessons & Posts Tab",
                    subtitle = "Interactive HTML topic explanations, web notes & YouTube lectures",
                    badge = "Main Tab",
                    keywords = listOf("posts", "lessons", "html", "interactive", "youtube", "topics", "explain"),
                    icon = Icons.AutoMirrored.Rounded.Article,
                    onInvoke = { onNavigateMainTab("posts") }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_profile_main",
                    title = "Profile & Account",
                    subtitle = "Edit name, target exam, phone, backup email, optional Access Key",
                    badge = "3-Dots Tab",
                    keywords = listOf("profile", "account", "name", "email", "phone", "mobile", "key", "access key", "exam"),
                    icon = Icons.Rounded.Person,
                    onInvoke = { onJumpToMoreTab(MoreMenuTab.PROFILE) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_plans",
                    title = "Subscription Plans (Free, Basic, Advance)",
                    subtitle = "View plan benefits & JazzCash upgrade instructions (PKR 499 / PKR 999)",
                    badge = "3-Dots Tab",
                    keywords = listOf("plan", "plans", "subscription", "buy", "upgrade", "jazzcash", "price", "free", "basic", "advance", "payment"),
                    icon = Icons.Rounded.AutoAwesome,
                    onInvoke = { onJumpToMoreTab(MoreMenuTab.PLANS) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_appearance",
                    title = "Appearance, Fonts & Color Palettes",
                    subtitle = "Change Light/Dark theme, color palettes, font style & text size",
                    badge = "3-Dots Tab",
                    keywords = listOf("appearance", "theme", "dark", "light", "color", "palette", "font", "text", "size", "style"),
                    icon = Icons.Rounded.Palette,
                    onInvoke = { onJumpToMoreTab(MoreMenuTab.APPEARANCE) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_progress",
                    title = "Progress Tracker & Study Schedule",
                    subtitle = "Subject-wise mastery and Cover-in-N-Days study planner",
                    badge = "3-Dots Tab",
                    keywords = listOf("progress", "schedule", "days", "planner", "stats", "accuracy", "mastery", "target"),
                    icon = Icons.Rounded.Timeline,
                    onInvoke = { onJumpToMoreTab(MoreMenuTab.PROGRESS) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_notes",
                    title = "Highlights, Notes & Offline Study Packs",
                    subtitle = "Read/create study notes and launch saved offline MCQ packs",
                    badge = "3-Dots Tab",
                    keywords = listOf("notes", "highlights", "offline", "packs", "saved", "study packs", "memo"),
                    icon = Icons.Rounded.BookmarkAdded,
                    onInvoke = { onJumpToMoreTab(MoreMenuTab.HIGHLIGHTS_NOTES) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_difficult",
                    title = "Difficult Questions Vault",
                    subtitle = "Add and drill difficult exam questions for focused revision",
                    badge = "3-Dots Tab",
                    keywords = listOf("difficult", "hard", "vault", "exam questions", "tough", "revision"),
                    icon = Icons.Rounded.PriorityHigh,
                    onInvoke = { onJumpToMoreTab(MoreMenuTab.DIFFICULT) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_settings",
                    title = "Settings, Notifications & Mobile Storage",
                    subtitle = "Daily study reminder notifications, quiz preferences & offline storage",
                    badge = "3-Dots Tab",
                    keywords = listOf("settings", "notification", "notifications", "reminder", "storage", "offline", "cache", "shuffle", "timer", "backup"),
                    icon = Icons.Rounded.Backup,
                    onInvoke = { onJumpToMoreTab(MoreMenuTab.SETTINGS_BACKUP) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "tab_guide",
                    title = "App Guide & Support Contact",
                    subtitle = "Step-by-step guide on how to use MedPrep & support email",
                    badge = "3-Dots Tab",
                    keywords = listOf("guide", "help", "how to", "tutorial", "support", "contact", "email"),
                    icon = Icons.AutoMirrored.Rounded.Help,
                    onInvoke = { onJumpToMoreTab(MoreMenuTab.APP_GUIDE) }
                )
            )

            // Direct App Functions & Quick Actions
            add(
                AppSearchableFunction(
                    id = "fn_quick_practice",
                    title = "Start Quick Practice",
                    subtitle = "Launch shuffled MCQs with instant Reasons (up to $maxPracticeCap MCQs)",
                    badge = "Function",
                    keywords = listOf("practice", "quiz", "start", "mcq", "mcqs", "test", "shuffled", "reason"),
                    icon = Icons.Rounded.Quiz,
                    onInvoke = { onStartStudy(StudyMode.PRACTICE, null, minOf(20, maxPracticeCap)) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "fn_timed_exam",
                    title = "Start Timed Exam Simulation",
                    subtitle = if (canTimedExam) "Timed MCQ test with countdown & full retest" else "Basic Plan required (PKR 499/mo)",
                    badge = if (canTimedExam) "Function" else "Basic Plan",
                    keywords = listOf("exam", "timed", "mock", "simulation", "countdown", "test", "retest"),
                    icon = if (canTimedExam) Icons.Rounded.Timer else Icons.Rounded.Lock,
                    onInvoke = {
                        if (canTimedExam) {
                            onStartStudy(StudyMode.EXAM, null, 15)
                        } else {
                            onPostMessage("Timed Exam Mode requires Basic Plan (PKR 499/mo via JazzCash).")
                            onJumpToMoreTab(MoreMenuTab.PLANS)
                        }
                    }
                )
            )
            add(
                AppSearchableFunction(
                    id = "fn_mistakes",
                    title = "Review Mistakes",
                    subtitle = "Re-attempt MCQs you answered incorrectly",
                    badge = "Function",
                    keywords = listOf("mistake", "mistakes", "wrong", "review", "errors", "retry"),
                    icon = Icons.Rounded.Replay,
                    onInvoke = { onStartStudy(StudyMode.MISTAKES, null, maxPracticeCap) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "fn_bookmarks",
                    title = "Review Bookmarked MCQs",
                    subtitle = "Practice your bookmarked questions",
                    badge = "Function",
                    keywords = listOf("bookmark", "bookmarks", "saved", "favorites", "flagged"),
                    icon = Icons.Rounded.Bookmarks,
                    onInvoke = { onStartStudy(StudyMode.BOOKMARKS, null, maxPracticeCap) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "fn_notifications",
                    title = "Study Reminder Notifications",
                    subtitle = "Enable/disable daily study reminder alerts & schedule time",
                    badge = "Function",
                    keywords = listOf("notification", "notifications", "alert", "alarm", "reminder", "daily"),
                    icon = Icons.Rounded.NotificationsActive,
                    onInvoke = { onJumpToMoreTab(MoreMenuTab.SETTINGS_BACKUP) }
                )
            )
            add(
                AppSearchableFunction(
                    id = "fn_offline_save",
                    title = "Save Study Data Offline (Mobile Storage)",
                    subtitle = "Save MCQs, courses & lessons privately on your device for offline study",
                    badge = "Function",
                    keywords = listOf("offline", "save", "mobile storage", "cache", "no internet", "download"),
                    icon = Icons.Rounded.OfflinePin,
                    onInvoke = { onSaveOfflineSnapshotNow() }
                )
            )
            add(
                AppSearchableFunction(
                    id = "fn_sync",
                    title = "Sync Latest Questions & Lessons",
                    subtitle = "Refresh MCQs, courses & posts from cloud",
                    badge = "Function",
                    keywords = listOf("sync", "refresh", "update", "cloud", "reload"),
                    icon = Icons.Rounded.CloudSync,
                    onInvoke = { onSyncNow() }
                )
            )
            add(
                AppSearchableFunction(
                    id = "fn_toggle_theme",
                    title = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                    subtitle = "Instantly toggle between Light and Dark theme",
                    badge = "Function",
                    keywords = listOf("dark mode", "light mode", "night", "theme", "toggle"),
                    icon = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    onInvoke = { onToggleTheme() }
                )
            )
            if (isAdminUser && onOpenAdminPanel != null) {
                add(
                    AppSearchableFunction(
                        id = "fn_admin_portal",
                        title = "Open Admin Portal",
                        subtitle = "Manage Courses, Excel MCQs, HTML Lessons, Keys & Plans",
                        badge = "Admin",
                        keywords = listOf("admin", "portal", "excel", "upload", "manage", "keys", "subscriptions"),
                        icon = Icons.Rounded.AdminPanelSettings,
                        onInvoke = onOpenAdminPanel
                    )
                )
            }
            add(
                AppSearchableFunction(
                    id = "fn_logout",
                    title = "Logout / Sign Out",
                    subtitle = "Sign out of your current account immediately",
                    badge = "Account",
                    keywords = listOf("logout", "log out", "sign out", "signout", "exit", "switch account"),
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    isDestructive = true,
                    onInvoke = onSignOut
                )
            )
        }
    }

    // Combined MCQ pool (published MCQs + user's custom difficult questions without duplicates)
    val searchableQuestions = remember(publishedQuestions, difficultQuestions) {
        val seenIds = publishedQuestions.map { it.id }.toHashSet()
        val extraDifficult = difficultQuestions
            .map { it.toMcqQuestion() }
            .filter { it.id !in seenIds }
        publishedQuestions + extraDifficult
    }

    // Filtered results per category
    val matchingFunctions = remember(allFunctions, trimmedQuery) {
        if (trimmedQuery.isBlank()) {
            allFunctions
        } else {
            allFunctions.filter { fn ->
                fn.title.contains(trimmedQuery, ignoreCase = true) ||
                    fn.subtitle.contains(trimmedQuery, ignoreCase = true) ||
                    fn.badge.contains(trimmedQuery, ignoreCase = true) ||
                    fn.keywords.any { it.contains(trimmedQuery, ignoreCase = true) }
            }
        }
    }

    val matchingQuestions = remember(searchableQuestions, trimmedQuery, selectedSubject) {
        searchableQuestions.filter { q ->
            val subjectOk = selectedSubject == "All" || q.subject.equals(selectedSubject, ignoreCase = true)
            if (!subjectOk) return@filter false
            if (trimmedQuery.isBlank()) true
            else {
                q.stem.contains(trimmedQuery, ignoreCase = true) ||
                    q.subject.contains(trimmedQuery, ignoreCase = true) ||
                    q.topic.contains(trimmedQuery, ignoreCase = true) ||
                    q.optionA.contains(trimmedQuery, ignoreCase = true) ||
                    q.optionB.contains(trimmedQuery, ignoreCase = true) ||
                    q.optionC.contains(trimmedQuery, ignoreCase = true) ||
                    q.optionD.contains(trimmedQuery, ignoreCase = true) ||
                    q.explanation.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingCourses = remember(publishedCourses, trimmedQuery, selectedSubject) {
        publishedCourses.filter { c ->
            val subjectOk = selectedSubject == "All" || c.subject.equals(selectedSubject, ignoreCase = true)
            if (!subjectOk) return@filter false
            if (trimmedQuery.isBlank()) true
            else {
                c.title.contains(trimmedQuery, ignoreCase = true) ||
                    c.subject.contains(trimmedQuery, ignoreCase = true) ||
                    c.description.contains(trimmedQuery, ignoreCase = true) ||
                    c.instructor.contains(trimmedQuery, ignoreCase = true) ||
                    c.syllabusOutline.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingPosts = remember(publishedPosts, trimmedQuery, selectedSubject) {
        publishedPosts.filter { p ->
            val subjectOk = selectedSubject == "All" || p.subject.equals(selectedSubject, ignoreCase = true)
            if (!subjectOk) return@filter false
            if (trimmedQuery.isBlank()) true
            else {
                p.title.contains(trimmedQuery, ignoreCase = true) ||
                    p.subject.contains(trimmedQuery, ignoreCase = true) ||
                    p.topic.contains(trimmedQuery, ignoreCase = true) ||
                    p.body.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingNotes = remember(studyNotes, trimmedQuery, selectedSubject) {
        studyNotes.filter { n ->
            val subjectOk = selectedSubject == "All" || n.subject.equals(selectedSubject, ignoreCase = true)
            if (!subjectOk) return@filter false
            if (trimmedQuery.isBlank()) true
            else {
                n.title.contains(trimmedQuery, ignoreCase = true) ||
                    n.content.contains(trimmedQuery, ignoreCase = true) ||
                    n.subject.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val totalCount = matchingFunctions.size + matchingQuestions.size + matchingCourses.size + matchingPosts.size + matchingNotes.size
    val showQuestions = selectedFilter == SearchFilterCategory.ALL || selectedFilter == SearchFilterCategory.QUESTIONS
    val showFunctions = selectedFilter == SearchFilterCategory.ALL || selectedFilter == SearchFilterCategory.TABS_FUNCTIONS
    val showCourses = selectedFilter == SearchFilterCategory.ALL || selectedFilter == SearchFilterCategory.COURSES
    val showPosts = selectedFilter == SearchFilterCategory.ALL || selectedFilter == SearchFilterCategory.LESSONS
    val showNotes = selectedFilter == SearchFilterCategory.ALL || selectedFilter == SearchFilterCategory.NOTES

    val visibleTotalCount = (if (showFunctions) matchingFunctions.size else 0) +
        (if (showQuestions) matchingQuestions.size else 0) +
        (if (showCourses) matchingCourses.size else 0) +
        (if (showPosts) matchingPosts.size else 0) +
        (if (showNotes) matchingNotes.size else 0)

    val quickSuggestions = remember {
        listOf("Biology", "Chemistry", "Timed Exam", "Plans", "Offline", "Dark Mode", "Schedule", "Notes")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Input + Filter Controls inside Liquid Glass Container
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = {
                            Text(
                                text = "Find questions, tabs, functions, lessons…",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" },
                                    modifier = Modifier.testTag("search_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_hub_input")
                    )

                    // Category Filter Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Filter by Type",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedFilter != SearchFilterCategory.ALL || selectedSubject != "All" || query.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    query = ""
                                    selectedFilterId = SearchFilterCategory.ALL.id
                                    selectedSubject = "All"
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("search_reset_filters_button")
                            ) {
                                Text("Reset", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Category Filter Chips (All, Questions, Tabs & Functions, Courses, Lessons & Posts, Notes)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchFilterCategory.entries.forEach { category ->
                            val count = when (category) {
                                SearchFilterCategory.ALL -> totalCount
                                SearchFilterCategory.QUESTIONS -> matchingQuestions.size
                                SearchFilterCategory.TABS_FUNCTIONS -> matchingFunctions.size
                                SearchFilterCategory.COURSES -> matchingCourses.size
                                SearchFilterCategory.LESSONS -> matchingPosts.size
                                SearchFilterCategory.NOTES -> matchingNotes.size
                            }
                            val selected = selectedFilter == category
                            FilterChip(
                                selected = selected,
                                onClick = { selectedFilterId = category.id },
                                label = {
                                    Text("${category.label} ($count)")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("search_filter_${category.id}")
                            )
                        }
                    }

                    // Subject Sub-Filter (shown for content categories)
                    if (selectedFilter != SearchFilterCategory.TABS_FUNCTIONS) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val subjects = remember { listOf("All") + MedPrepConstants.DEFAULT_SUBJECTS }
                            subjects.forEach { subj ->
                                val isSel = selectedSubject == subj
                                Surface(
                                    onClick = { selectedSubject = subj },
                                    shape = RoundedCornerShape(50),
                                    color = if (isSel) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSel) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.testTag("search_subject_${subj.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = if (subj == "All") "All Subjects" else subj,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick search suggestions when input is blank
                    if (trimmedQuery.isBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Try:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            quickSuggestions.forEach { hint ->
                                Surface(
                                    onClick = { query = hint },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                                    modifier = Modifier.testTag("search_hint_${hint.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = hint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empty State when nothing matches
        if (visibleTotalCount == 0) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = "No results",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "No matching results found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try a different keyword or switch the filter to All to search across questions, app tabs, courses, lessons, and notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // SECTION 1: TABS & FUNCTIONS
        if (showFunctions && matchingFunctions.isNotEmpty()) {
            val displayFunctions = if (selectedFilter == SearchFilterCategory.ALL && trimmedQuery.isBlank()) {
                matchingFunctions.take(6)
            } else {
                matchingFunctions
            }
            item {
                SearchSectionHeader(
                    title = "App Tabs & Functions",
                    count = matchingFunctions.size,
                    showSeeAll = selectedFilter == SearchFilterCategory.ALL && matchingFunctions.size > displayFunctions.size,
                    onSeeAll = { selectedFilterId = SearchFilterCategory.TABS_FUNCTIONS.id }
                )
            }
            items(displayFunctions, key = { "fn_${it.id}" }) { fn ->
                SearchFunctionCard(fn = fn)
            }
        }

        // SECTION 2: QUESTIONS (MCQs)
        if (showQuestions && matchingQuestions.isNotEmpty()) {
            val displayQuestions = if (selectedFilter == SearchFilterCategory.ALL) {
                matchingQuestions.take(if (trimmedQuery.isBlank()) 5 else 8)
            } else {
                matchingQuestions.take(40)
            }
            item {
                SearchSectionHeader(
                    title = "Questions (MCQs)",
                    count = matchingQuestions.size,
                    showSeeAll = selectedFilter == SearchFilterCategory.ALL && matchingQuestions.size > displayQuestions.size,
                    onSeeAll = { selectedFilterId = SearchFilterCategory.QUESTIONS.id }
                )
            }
            if (matchingQuestions.size > 1 && (trimmedQuery.isNotBlank() || selectedFilter == SearchFilterCategory.QUESTIONS)) {
                item {
                    val practiceBatch = matchingQuestions.take(maxPracticeCap)
                    Button(
                        onClick = {
                            val title = if (trimmedQuery.isNotBlank()) {
                                "Search: $trimmedQuery"
                            } else if (selectedSubject != "All") {
                                "$selectedSubject MCQs"
                            } else {
                                "Filtered MCQs"
                            }
                            onStartCustomQuiz(title, practiceBatch)
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("search_practice_matching_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Practice ${practiceBatch.size} Matching MCQs")
                    }
                }
            }
            items(displayQuestions, key = { "q_${it.id}" }) { mcq ->
                SearchQuestionCard(
                    question = mcq,
                    isDarkTheme = isDarkTheme,
                    onClick = {
                        onStartCustomQuiz(
                            "${mcq.subject} • ${mcq.topic.ifBlank { "MCQ" }}",
                            listOf(mcq)
                        )
                    }
                )
            }
        }

        // SECTION 3: COURSES
        if (showCourses && matchingCourses.isNotEmpty()) {
            val displayCourses = if (selectedFilter == SearchFilterCategory.ALL) {
                matchingCourses.take(3)
            } else {
                matchingCourses
            }
            item {
                SearchSectionHeader(
                    title = "Courses",
                    count = matchingCourses.size,
                    showSeeAll = selectedFilter == SearchFilterCategory.ALL && matchingCourses.size > displayCourses.size,
                    onSeeAll = { selectedFilterId = SearchFilterCategory.COURSES.id }
                )
            }
            items(displayCourses, key = { "c_${it.id}" }) { course ->
                SearchCourseCard(
                    course = course,
                    onClick = { onNavigateMainTab("courses") }
                )
            }
        }

        // SECTION 4: LESSONS & POSTS
        if (showPosts && matchingPosts.isNotEmpty()) {
            val displayPosts = if (selectedFilter == SearchFilterCategory.ALL) {
                matchingPosts.take(4)
            } else {
                matchingPosts
            }
            item {
                SearchSectionHeader(
                    title = "Interactive Lessons & Posts",
                    count = matchingPosts.size,
                    showSeeAll = selectedFilter == SearchFilterCategory.ALL && matchingPosts.size > displayPosts.size,
                    onSeeAll = { selectedFilterId = SearchFilterCategory.LESSONS.id }
                )
            }
            items(displayPosts, key = { "p_${it.id}" }) { post ->
                SearchPostCard(
                    post = post,
                    canOpenHtml = canOpenHtml,
                    onClick = {
                        if (post.hasInteractiveHtml) {
                            if (canOpenHtml) {
                                onOpenInteractiveLesson(post)
                            } else {
                                onPostMessage("Interactive HTML Lessons require Advance Plan (PKR 999/mo via JazzCash).")
                                onJumpToMoreTab(MoreMenuTab.PLANS)
                            }
                        } else {
                            onNavigateMainTab("posts")
                        }
                    }
                )
            }
        }

        // SECTION 5: HIGHLIGHTS & NOTES
        if (showNotes && matchingNotes.isNotEmpty()) {
            val displayNotes = if (selectedFilter == SearchFilterCategory.ALL) {
                matchingNotes.take(4)
            } else {
                matchingNotes
            }
            item {
                SearchSectionHeader(
                    title = "Highlights & Notes",
                    count = matchingNotes.size,
                    showSeeAll = selectedFilter == SearchFilterCategory.ALL && matchingNotes.size > displayNotes.size,
                    onSeeAll = { selectedFilterId = SearchFilterCategory.NOTES.id }
                )
            }
            items(displayNotes, key = { "n_${it.id}" }) { note ->
                SearchNoteCard(
                    note = note,
                    onClick = { onJumpToMoreTab(MoreMenuTab.HIGHLIGHTS_NOTES) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int,
    showSeeAll: Boolean,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        if (showSeeAll) {
            TextButton(
                onClick = onSeeAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("View all")
            }
        }
    }
}

@Composable
private fun SearchFunctionCard(fn: AppSearchableFunction) {
    val accent = if (fn.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val iconBg = if (fn.isDestructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    Card(
        onClick = fn.onInvoke,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_result_${fn.id}")
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fn.icon,
                    contentDescription = fn.title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = fn.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (fn.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = fn.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = fn.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Open ${fn.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SearchQuestionCard(
    question: McqQuestion,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val visual = subjectVisualFor(question.subject)
    val badgeBg = if (isDarkTheme) visual.containerDark else visual.containerLight
    val badgeFg = if (isDarkTheme) visual.containerLight else visual.accent

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_result_q_${question.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = question.subject,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (question.topic.isNotBlank()) {
                    Text(
                        text = question.topic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = question.difficulty.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            FormulaText(
                text = question.stem,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Tap to practice this MCQ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SearchCourseCard(
    course: CourseItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_result_course_${course.id}")
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
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = course.title,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${course.subject} • ${course.instructor} • ${course.durationLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (course.description.isNotBlank()) {
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Open course",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SearchPostCard(
    post: StudyPost,
    canOpenHtml: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_result_post_${post.id}")
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (post.hasInteractiveHtml) Icons.Rounded.Code else Icons.AutoMirrored.Rounded.Article,
                    contentDescription = post.title,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (post.hasInteractiveHtml) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (canOpenHtml) "HTML" else "Advance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${post.subject}${if (post.topic.isNotBlank()) " • ${post.topic}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (post.body.isNotBlank()) {
                    Text(
                        text = post.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Open lesson",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SearchNoteCard(
    note: StudyNote,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_result_note_${note.id}")
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkAdded,
                    contentDescription = note.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = note.title.ifBlank { "Study Note" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = note.subject,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Open notes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
