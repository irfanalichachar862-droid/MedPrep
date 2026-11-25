package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AccessKey
import com.example.data.CourseItem
import com.example.data.DifficultQuestionDraft
import com.example.data.DifficultQuestionItem
import com.example.data.DownloadedMcqPack
import com.example.data.LocalStudyCache
import com.example.data.McqQuestion
import com.example.data.QuestionAttempt
import com.example.data.StudyMode
import com.example.data.StudyNote
import com.example.data.StudyNoteDraft
import com.example.data.StudyPost
import com.example.data.SubscriptionPlanTier
import com.example.data.SubscriptionRequest
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.LiquidGlassScrollableTabBar
import com.example.ui.components.LiquidGlassTabItem
import com.example.ui.theme.AppAppearanceConfig
import com.example.ui.theme.AppFunctionalSettings

enum class MoreMenuTab(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SEARCH("search", "Search / Find", Icons.Rounded.Search, Icons.Outlined.Search, "hub_tab_search"),
    PROFILE("profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person, "hub_tab_profile"),
    PLANS("plans", "Plans", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "hub_tab_plans"),
    APPEARANCE("appearance", "Appearance", Icons.Rounded.Palette, Icons.Outlined.Palette, "hub_tab_appearance"),
    PROGRESS("progress", "Progress", Icons.Rounded.Timeline, Icons.Outlined.Timeline, "hub_tab_progress"),
    HIGHLIGHTS_NOTES("notes", "Highlights & Notes", Icons.Rounded.BookmarkAdded, Icons.Outlined.BookmarkAdded, "hub_tab_notes"),
    DIFFICULT("difficult", "Difficult Qs", Icons.Rounded.PriorityHigh, Icons.Outlined.PriorityHigh, "hub_tab_difficult"),
    SETTINGS_BACKUP("settings", "Settings & Storage", Icons.Rounded.Backup, Icons.Outlined.Backup, "hub_tab_settings"),
    APP_GUIDE("guide", "App Guide", Icons.AutoMirrored.Rounded.Help, Icons.AutoMirrored.Outlined.HelpOutline, "hub_tab_guide")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuHubScreen(
    initialTab: MoreMenuTab = MoreMenuTab.PROFILE,
    currentUserId: String,
    profileState: UiState<UserProfile?>,
    publishedQuestions: List<McqQuestion>,
    publishedCourses: List<CourseItem>,
    publishedPosts: List<StudyPost>,
    attemptsMap: Map<String, QuestionAttempt>,
    studyNotes: List<StudyNote>,
    difficultQuestions: List<DifficultQuestionItem>,
    downloadedPacks: List<DownloadedMcqPack>,
    offlineStudyCache: LocalStudyCache?,
    localStorageKb: Long,
    userSubscriptionRequests: List<SubscriptionRequest>,
    activeAccessKey: AccessKey?,
    deviceId: String,
    appearanceConfig: AppAppearanceConfig,
    functionalSettings: AppFunctionalSettings,
    isDarkTheme: Boolean,
    isAdminUser: Boolean,
    isSyncing: Boolean,
    isMutating: Boolean,
    snackbarHostState: SnackbarHostState,
    onUpdateAppearance: (AppAppearanceConfig) -> Unit,
    onToggleTheme: () -> Unit,
    onUpdateFunctionalSettings: (AppFunctionalSettings) -> Unit,
    onSaveFullProfile: (String, String, Long, String?, String?, Long?) -> Unit,
    onActivateOptionalKey: (String) -> Unit,
    onSubmitPaymentRequest: (SubscriptionPlanTier, String, String, String, String, () -> Unit) -> Unit,
    onCancelPendingRequest: (String) -> Unit,
    onSaveScheduleDays: (Long) -> Unit,
    onStartStudy: (StudyMode, String?, Int) -> Unit,
    onStartCustomQuiz: (String, List<McqQuestion>) -> Unit,
    onOpenInteractiveLesson: (StudyPost) -> Unit,
    onNavigateMainTab: (String) -> Unit,
    onSaveOfflinePack: (String, String, List<McqQuestion>) -> Unit,
    onDeleteOfflinePack: (String) -> Unit,
    onSaveOfflineSnapshotNow: () -> Unit,
    onClearOfflineCache: () -> Unit,
    onSaveNote: (StudyNoteDraft, () -> Unit) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSaveDifficultQuestion: (DifficultQuestionDraft, () -> Unit) -> Unit,
    onDeleteDifficultQuestion: (String) -> Unit,
    onSyncNow: () -> Unit,
    onOpenAdminPanel: (() -> Unit)?,
    onSignOut: () -> Unit,
    onPostMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    val profile = (profileState as? UiState.Success)?.data
    val activePlan = SubscriptionPlanTier.fromKey(profile?.subscriptionPlan)

    val tabItems = remember {
        MoreMenuTab.entries.map { tab ->
            LiquidGlassTabItem(
                id = tab.id,
                label = tab.label,
                selectedIcon = tab.selectedIcon,
                unselectedIcon = tab.unselectedIcon,
                testTag = tab.testTag
            )
        } + LiquidGlassTabItem(
            id = "logout",
            label = "Logout",
            selectedIcon = Icons.AutoMirrored.Rounded.Logout,
            unselectedIcon = Icons.AutoMirrored.Rounded.Logout,
            testTag = "hub_tab_logout"
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("more_hub_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Study"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = selectedTab.label,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "MedPrep Menu Hub • Liquid Glass",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("more_hub_logout_action")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
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
                    .widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassScrollableTabBar(
                    items = tabItems,
                    selectedId = selectedTab.id,
                    onSelect = { id ->
                        if (id == "logout") {
                            onSignOut()
                        } else {
                            selectedTab = MoreMenuTab.entries.firstOrNull { it.id == id } ?: MoreMenuTab.PROFILE
                        }
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        MoreMenuTab.SEARCH -> SearchFindTabContent(
                            activePlan = activePlan,
                            publishedQuestions = publishedQuestions,
                            publishedCourses = publishedCourses,
                            publishedPosts = publishedPosts,
                            studyNotes = studyNotes,
                            difficultQuestions = difficultQuestions,
                            isDarkTheme = isDarkTheme,
                            isAdminUser = isAdminUser,
                            onJumpToMoreTab = { target -> selectedTab = target },
                            onNavigateMainTab = { mainTabId ->
                                onClose()
                                onNavigateMainTab(mainTabId)
                            },
                            onStartStudy = { mode, subject, count ->
                                onClose()
                                onStartStudy(mode, subject, count)
                            },
                            onStartCustomQuiz = { title, qs ->
                                onClose()
                                onStartCustomQuiz(title, qs)
                            },
                            onOpenInteractiveLesson = { post ->
                                onClose()
                                onOpenInteractiveLesson(post)
                            },
                            onToggleTheme = onToggleTheme,
                            onSyncNow = onSyncNow,
                            onSaveOfflineSnapshotNow = onSaveOfflineSnapshotNow,
                            onOpenAdminPanel = onOpenAdminPanel,
                            onSignOut = onSignOut,
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.PROFILE -> ProfileTabContent(
                            currentUserId = currentUserId,
                            profile = profile,
                            activeAccessKey = activeAccessKey,
                            deviceId = deviceId,
                            isAdminUser = isAdminUser,
                            isMutating = isMutating,
                            onSaveFullProfile = onSaveFullProfile,
                            onActivateOptionalKey = onActivateOptionalKey,
                            onJumpToPlans = { selectedTab = MoreMenuTab.PLANS },
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.PLANS -> PlansTabContent(
                            profile = profile,
                            currentPlanKey = activePlan.key,
                            userRequests = userSubscriptionRequests,
                            isMutating = isMutating,
                            onSubmitPaymentRequest = onSubmitPaymentRequest,
                            onCancelPendingRequest = onCancelPendingRequest,
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.APPEARANCE -> AppearanceTabContent(
                            activePlan = activePlan,
                            appearance = appearanceConfig,
                            onUpdateAppearance = onUpdateAppearance,
                            onUpgradeToAdvance = { selectedTab = MoreMenuTab.PLANS },
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.PROGRESS -> ProgressScheduleTabContent(
                            activePlan = activePlan,
                            profile = profile,
                            publishedQuestions = publishedQuestions,
                            attemptsMap = attemptsMap,
                            isDarkTheme = isDarkTheme,
                            onSaveScheduleDays = onSaveScheduleDays,
                            onStartStudy = { mode, subject, count ->
                                onClose()
                                onStartStudy(mode, subject, count)
                            },
                            onUpgradeToBasic = { selectedTab = MoreMenuTab.PLANS }
                        )

                        MoreMenuTab.HIGHLIGHTS_NOTES -> HighlightsNotesTabContent(
                            activePlan = activePlan,
                            publishedQuestions = publishedQuestions,
                            studyNotes = studyNotes,
                            downloadedPacks = downloadedPacks,
                            isMutating = isMutating,
                            onStartPackQuiz = { pack ->
                                onClose()
                                onStartCustomQuiz(pack.title, pack.questions)
                            },
                            onSaveOfflinePack = onSaveOfflinePack,
                            onDeleteOfflinePack = onDeleteOfflinePack,
                            onSaveNote = onSaveNote,
                            onDeleteNote = onDeleteNote,
                            onUpgradePlan = { selectedTab = MoreMenuTab.PLANS },
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.DIFFICULT -> DifficultQuestionsTabContent(
                            activePlan = activePlan,
                            publishedQuestions = publishedQuestions,
                            personalDifficult = difficultQuestions,
                            isMutating = isMutating,
                            onPracticeDifficult = { questions ->
                                onClose()
                                onStartCustomQuiz("Difficult Exam Vault", questions)
                            },
                            onSaveDifficultQuestion = onSaveDifficultQuestion,
                            onDeleteDifficultQuestion = onDeleteDifficultQuestion,
                            onUpgradePlan = { selectedTab = MoreMenuTab.PLANS },
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.SETTINGS_BACKUP -> SettingsBackupTabContent(
                            settings = functionalSettings,
                            studyNotes = studyNotes,
                            difficultQuestions = difficultQuestions,
                            downloadedPacks = downloadedPacks,
                            offlineStudyCache = offlineStudyCache,
                            localStorageKb = localStorageKb,
                            isAdminUser = isAdminUser,
                            isSyncing = isSyncing,
                            onUpdateSettings = onUpdateFunctionalSettings,
                            onSyncNow = onSyncNow,
                            onSaveOfflineSnapshotNow = onSaveOfflineSnapshotNow,
                            onClearOfflineCache = onClearOfflineCache,
                            onOpenAdminPanel = onOpenAdminPanel,
                            onSignOut = onSignOut,
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.APP_GUIDE -> AppGuideTabContent()
                    }
                }
            }
        }
    }
}
