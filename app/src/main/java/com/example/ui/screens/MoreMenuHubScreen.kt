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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PriorityHigh
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
import com.example.data.DifficultQuestionDraft
import com.example.data.DifficultQuestionItem
import com.example.data.DownloadedMcqPack
import com.example.data.McqQuestion
import com.example.data.MedPrepBackupPayload
import com.example.data.QuestionAttempt
import com.example.data.StudyMode
import com.example.data.StudyNote
import com.example.data.StudyNoteDraft
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
    PROFILE("profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person, "hub_tab_profile"),
    PLANS("plans", "Plans", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "hub_tab_plans"),
    APPEARANCE("appearance", "Appearance", Icons.Rounded.Palette, Icons.Outlined.Palette, "hub_tab_appearance"),
    PROGRESS("progress", "Progress", Icons.Rounded.Timeline, Icons.Outlined.Timeline, "hub_tab_progress"),
    HIGHLIGHTS_NOTES("notes", "Highlights & Notes", Icons.Rounded.BookmarkAdded, Icons.Outlined.BookmarkAdded, "hub_tab_notes"),
    DIFFICULT("difficult", "Difficult Qs", Icons.Rounded.PriorityHigh, Icons.Outlined.PriorityHigh, "hub_tab_difficult"),
    SETTINGS_BACKUP("settings", "Settings & Backup", Icons.Rounded.Backup, Icons.Outlined.Backup, "hub_tab_settings"),
    APP_GUIDE("guide", "App Guide", Icons.AutoMirrored.Rounded.Help, Icons.AutoMirrored.Outlined.HelpOutline, "hub_tab_guide")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuHubScreen(
    initialTab: MoreMenuTab = MoreMenuTab.PROFILE,
    currentUserId: String,
    profileState: UiState<UserProfile?>,
    publishedQuestions: List<McqQuestion>,
    attemptsMap: Map<String, QuestionAttempt>,
    studyNotes: List<StudyNote>,
    difficultQuestions: List<DifficultQuestionItem>,
    downloadedPacks: List<DownloadedMcqPack>,
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
    onUpdateFunctionalSettings: (AppFunctionalSettings) -> Unit,
    onSaveFullProfile: (String, String, Long, String?, String?, String?, Long?) -> Unit,
    onSelectPlan: (String, String) -> Unit,
    onSaveScheduleDays: (Long) -> Unit,
    onStartStudy: (StudyMode, String?, Int) -> Unit,
    onStartCustomQuiz: (String, List<McqQuestion>) -> Unit,
    onSaveOfflinePack: (String, String, List<McqQuestion>) -> Unit,
    onDeleteOfflinePack: (String) -> Unit,
    onSaveNote: (StudyNoteDraft, () -> Unit) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSaveDifficultQuestion: (DifficultQuestionDraft, () -> Unit) -> Unit,
    onDeleteDifficultQuestion: (String) -> Unit,
    onImportBackup: (MedPrepBackupPayload) -> Unit,
    onSyncNow: () -> Unit,
    onOpenAdminPanel: (() -> Unit)?,
    onSignOut: () -> Unit,
    onPostMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    val profile = (profileState as? UiState.Success)?.data

    val tabItems = remember {
        MoreMenuTab.entries.map { tab ->
            LiquidGlassTabItem(
                id = tab.id,
                label = tab.label,
                selectedIcon = tab.selectedIcon,
                unselectedIcon = tab.unselectedIcon,
                testTag = tab.testTag
            )
        }
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
                        selectedTab = MoreMenuTab.entries.firstOrNull { it.id == id } ?: MoreMenuTab.PROFILE
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        MoreMenuTab.PROFILE -> ProfileTabContent(
                            currentUserId = currentUserId,
                            profile = profile,
                            activeAccessKey = activeAccessKey,
                            deviceId = deviceId,
                            isAdminUser = isAdminUser,
                            isMutating = isMutating,
                            onSaveFullProfile = onSaveFullProfile,
                            onJumpToPlans = { selectedTab = MoreMenuTab.PLANS },
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.PLANS -> PlansTabContent(
                            currentPlanKey = profile?.subscriptionPlan ?: "free",
                            isMutating = isMutating,
                            onSelectPlan = onSelectPlan,
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.APPEARANCE -> AppearanceTabContent(
                            appearance = appearanceConfig,
                            onUpdateAppearance = onUpdateAppearance
                        )

                        MoreMenuTab.PROGRESS -> ProgressScheduleTabContent(
                            profile = profile,
                            publishedQuestions = publishedQuestions,
                            attemptsMap = attemptsMap,
                            isDarkTheme = isDarkTheme,
                            onSaveScheduleDays = onSaveScheduleDays,
                            onStartStudy = { mode, subject, count ->
                                onClose()
                                onStartStudy(mode, subject, count)
                            }
                        )

                        MoreMenuTab.HIGHLIGHTS_NOTES -> HighlightsNotesTabContent(
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
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.DIFFICULT -> DifficultQuestionsTabContent(
                            publishedQuestions = publishedQuestions,
                            personalDifficult = difficultQuestions,
                            isMutating = isMutating,
                            onPracticeDifficult = { questions ->
                                onClose()
                                onStartCustomQuiz("Difficult Exam Vault", questions)
                            },
                            onSaveDifficultQuestion = onSaveDifficultQuestion,
                            onDeleteDifficultQuestion = onDeleteDifficultQuestion,
                            onPostMessage = onPostMessage
                        )

                        MoreMenuTab.SETTINGS_BACKUP -> SettingsBackupTabContent(
                            profile = profile,
                            appearance = appearanceConfig,
                            settings = functionalSettings,
                            studyNotes = studyNotes,
                            difficultQuestions = difficultQuestions,
                            downloadedPacks = downloadedPacks,
                            isAdminUser = isAdminUser,
                            isSyncing = isSyncing,
                            isMutating = isMutating,
                            onUpdateSettings = onUpdateFunctionalSettings,
                            onSyncNow = onSyncNow,
                            onImportBackup = onImportBackup,
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
