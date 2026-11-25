package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutGoogle
import com.example.data.DeviceIdProvider
import com.example.data.MedPrepConstants
import com.example.data.MedPrepRepository
import com.example.data.PlanEntitlements
import com.example.data.StudentAccessStatus
import com.example.data.StudyPost
import com.example.data.SubscriptionPlanTier
import com.example.data.TopicLinkResolver
import com.example.data.UiState
import com.example.notifications.MedPrepNotificationHelper
import com.example.ui.components.LiquidGlassBottomNavBar
import com.example.ui.components.LiquidGlassTabItem
import com.example.ui.screens.AccessKeyGateScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InteractiveLessonViewerScreen
import com.example.ui.screens.MoreMenuHubScreen
import com.example.ui.screens.MoreMenuTab
import com.example.ui.screens.PostsFeedScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizSessionScreen
import com.example.ui.screens.StudentCoursesScreen
import com.example.ui.theme.AppAppearanceConfig
import com.example.ui.theme.AppEntryMode
import com.example.ui.theme.AppFunctionalSettings
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MedPrepTheme
import com.example.ui.theme.ThemePreferences
import com.example.viewmodel.ActiveQuizState
import com.example.viewmodel.MedPrepViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AppShellState(
    val auth: FirebaseAuth,
    val deviceId: String,
    val repository: MedPrepRepository,
    val appearanceConfig: AppAppearanceConfig,
    val functionalSettings: AppFunctionalSettings,
    val entryMode: AppEntryMode,
    val pendingAccessKey: String,
    val activeStudentSessionUid: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Configure edge-to-edge before super.onCreate so WindowManager avoids a redundant wmsSync relayout
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var shellState by remember { mutableStateOf<AppShellState?>(null) }

            LaunchedEffect(Unit) {
                // Commit initial opaque frame cleanly so SurfaceSyncGroup finishes well under 1000ms
                withFrameNanos { }
                withFrameNanos { }
                val appContext = context.applicationContext
                val loaded = withContext(Dispatchers.IO) {
                    // Pre-warm bundled TTF fonts into Android's Typeface cache off the Main thread
                    runCatching { ResourcesCompat.getFont(appContext, R.font.plus_jakarta_sans) }
                    runCatching { ResourcesCompat.getFont(appContext, R.font.inter) }
                    runCatching { ResourcesCompat.getFont(appContext, R.font.jetbrains_mono) }

                    val appearance = ThemePreferences.loadAppearanceConfig(appContext)
                    val settings = ThemePreferences.loadFunctionalSettings(appContext)
                    val savedEntryMode = ThemePreferences.loadEntryMode(appContext)
                    val savedPendingKey = ThemePreferences.loadPendingAccessKey(appContext)
                    val savedActiveStudentUid = ThemePreferences.loadActiveStudentSessionUid(appContext)
                    val deviceId = DeviceIdProvider.getDeviceId(appContext)
                    val auth = Firebase.auth
                    val repo = MedPrepRepository(appContext)
                    if (settings.notificationsEnabled) {
                        MedPrepNotificationHelper.syncSchedule(appContext, settings)
                    }
                    AppShellState(
                        auth = auth,
                        deviceId = deviceId,
                        repository = repo,
                        appearanceConfig = appearance,
                        functionalSettings = settings,
                        entryMode = savedEntryMode,
                        pendingAccessKey = savedPendingKey,
                        activeStudentSessionUid = savedActiveStudentUid
                    )
                }
                shellState = loaded
                withFrameNanos { }
                runCatching { reportFullyDrawn() }
            }

            val currentShell = shellState
            val appearanceConfig = currentShell?.appearanceConfig ?: AppAppearanceConfig()
            val systemDark = isSystemInDarkTheme()
            val resolvedDarkTheme = when (appearanceConfig.themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> systemDark
            }

            MedPrepTheme(
                darkTheme = resolvedDarkTheme,
                colorPalette = appearanceConfig.colorPalette,
                fontStyle = appearanceConfig.fontStyle,
                textScale = appearanceConfig.textScale
            ) {
                if (currentShell == null) {
                    // Instant zero-overhead opaque Frame-0 surface while IO bootstrap & font warm-up finishes
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else {
                    val appCtx = context.applicationContext
                    MedPrepAppRoot(
                        shell = currentShell,
                        isDarkTheme = resolvedDarkTheme,
                        onSelectEntryMode = { mode, key ->
                            shellState = shellState?.copy(entryMode = mode, pendingAccessKey = key)
                            scope.launch(Dispatchers.IO) {
                                ThemePreferences.saveEntryMode(appCtx, mode)
                                ThemePreferences.savePendingAccessKey(appCtx, key)
                            }
                        },
                        onClearPendingAccessKey = {
                            shellState = shellState?.copy(pendingAccessKey = "")
                            scope.launch(Dispatchers.IO) {
                                ThemePreferences.savePendingAccessKey(appCtx, "")
                            }
                        },
                        onSetActiveStudentSessionUid = { uid ->
                            if (shellState?.activeStudentSessionUid != uid) {
                                shellState = shellState?.copy(activeStudentSessionUid = uid)
                                scope.launch(Dispatchers.IO) {
                                    ThemePreferences.saveActiveStudentSessionUid(appCtx, uid)
                                }
                            }
                        },
                        onUpdateAppearance = { updated ->
                            shellState = shellState?.copy(appearanceConfig = updated)
                            scope.launch(Dispatchers.IO) {
                                ThemePreferences.saveAppearanceConfig(appCtx, updated)
                            }
                        },
                        onUpdateFunctionalSettings = { updated ->
                            shellState = shellState?.copy(functionalSettings = updated)
                            scope.launch(Dispatchers.IO) {
                                ThemePreferences.saveFunctionalSettings(appCtx, updated)
                                MedPrepNotificationHelper.syncSchedule(appCtx, updated)
                            }
                        },
                        onSelectThemeMode = { newMode ->
                            val updated = appearanceConfig.copy(themeMode = newMode)
                            shellState = shellState?.copy(appearanceConfig = updated)
                            scope.launch(Dispatchers.IO) {
                                ThemePreferences.saveAppearanceConfig(appCtx, updated)
                            }
                        },
                        onToggleTheme = {
                            val nextMode = if (resolvedDarkTheme) AppThemeMode.LIGHT else AppThemeMode.DARK
                            val updated = appearanceConfig.copy(themeMode = nextMode)
                            shellState = shellState?.copy(appearanceConfig = updated)
                            scope.launch(Dispatchers.IO) {
                                ThemePreferences.saveAppearanceConfig(appCtx, updated)
                            }
                        }
                    )
                }
            }
        }
    }
}

private enum class StudentTab(
    val id: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    STUDY("study", R.string.nav_study, Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, "nav_tab_study"),
    COURSES("courses", R.string.nav_courses, Icons.Rounded.School, Icons.Outlined.School, "nav_tab_courses"),
    POSTS("posts", R.string.nav_posts, Icons.AutoMirrored.Rounded.Article, Icons.AutoMirrored.Outlined.Article, "nav_tab_posts"),
    PROFILE("profile", R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person, "nav_tab_profile")
}

@Composable
private fun MedPrepAppRoot(
    shell: AppShellState,
    isDarkTheme: Boolean,
    onSelectEntryMode: (AppEntryMode, String) -> Unit,
    onClearPendingAccessKey: () -> Unit,
    onSetActiveStudentSessionUid: (String) -> Unit,
    onUpdateAppearance: (AppAppearanceConfig) -> Unit,
    onUpdateFunctionalSettings: (AppFunctionalSettings) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleTheme: () -> Unit
) {
    val authFlow = remember(shell.auth) { shell.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = shell.auth.currentUser)
    var forcedLogoutNotice by rememberSaveable { mutableStateOf<String?>(null) }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            forcedLogoutNotice = forcedLogoutNotice,
            onDismissForcedLogoutNotice = { forcedLogoutNotice = null },
            onSelectEntryMode = onSelectEntryMode,
            onAuthSuccess = { forcedLogoutNotice = null }
        )
    } else {
        key(user.uid) {
            AuthenticatedMedPrepShell(
                currentUserId = user.uid,
                currentUserEmail = user.email,
                deviceId = shell.deviceId,
                prewarmedRepository = shell.repository,
                appearanceConfig = shell.appearanceConfig,
                functionalSettings = shell.functionalSettings,
                entryMode = shell.entryMode,
                pendingAccessKey = shell.pendingAccessKey,
                activeStudentSessionUid = shell.activeStudentSessionUid,
                isDarkTheme = isDarkTheme,
                onChangeEntryMode = { newMode -> onSelectEntryMode(newMode, "") },
                onClearPendingAccessKey = onClearPendingAccessKey,
                onSetActiveStudentSessionUid = onSetActiveStudentSessionUid,
                onSetForcedLogoutNotice = { notice -> forcedLogoutNotice = notice },
                onUpdateAppearance = onUpdateAppearance,
                onUpdateFunctionalSettings = onUpdateFunctionalSettings,
                onSelectThemeMode = onSelectThemeMode,
                onToggleTheme = onToggleTheme
            )
        }
    }
}

@Composable
private fun AuthenticatedMedPrepShell(
    currentUserId: String,
    currentUserEmail: String?,
    deviceId: String,
    prewarmedRepository: MedPrepRepository,
    appearanceConfig: AppAppearanceConfig,
    functionalSettings: AppFunctionalSettings,
    entryMode: AppEntryMode,
    pendingAccessKey: String,
    activeStudentSessionUid: String,
    isDarkTheme: Boolean,
    onChangeEntryMode: (AppEntryMode) -> Unit,
    onClearPendingAccessKey: () -> Unit,
    onSetActiveStudentSessionUid: (String) -> Unit,
    onSetForcedLogoutNotice: (String?) -> Unit,
    onUpdateAppearance: (AppAppearanceConfig) -> Unit,
    onUpdateFunctionalSettings: (AppFunctionalSettings) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: MedPrepViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                MedPrepViewModel(prewarmedRepository, currentUserId, deviceId)
            }
        }
    )

    // Collect ONLY lightweight routing & feedback flows at the shell root;
    // heavy Firestore collections are scoped to the specific screen host below.
    val isAdminUser by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val activeQuiz by viewModel.activeQuiz.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val performSignOut: () -> Unit = {
        onClearPendingAccessKey()
        onSetActiveStudentSessionUid("")
        val credentialManager = CredentialManager.create(context)
        signOutGoogle(credentialManager = credentialManager, scope = scope)
    }

    var adminPreviewingStudent by rememberSaveable { mutableStateOf(false) }
    var activeHtmlLesson by remember { mutableStateOf<Pair<StudyPost, Boolean>?>(null) }
    var activeMoreMenuTab by rememberSaveable { mutableStateOf<MoreMenuTab?>(null) }
    var forcedLogoutTriggered by remember { mutableStateOf(false) }

    val lessonEntry = activeHtmlLesson

    // 1. STRICT ADMIN APP PORTAL (Admin Portal + Admin HTML Preview — bypasses student key gate)
    if (isAdminUser && entryMode == AppEntryMode.ADMIN && !adminPreviewingStudent) {
        if (lessonEntry != null && lessonEntry.second) {
            InteractiveLessonHost(
                viewModel = viewModel,
                lessonPost = lessonEntry.first,
                isAdminPreviewMode = true,
                hasActiveQuiz = false,
                isDarkTheme = isDarkTheme,
                onPracticeLinkedMcqs = {},
                onBack = { activeHtmlLesson = null }
            )
        } else {
            AdminPortalHost(
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                isMutating = isMutating,
                snackbarHostState = snackbarHostState,
                onToggleTheme = onToggleTheme,
                onPreviewHtmlLesson = { post -> activeHtmlLesson = post to true },
                onPreviewStudentApp = { adminPreviewingStudent = true },
                onSignOut = performSignOut
            )
        }
        return
    }

    // 2. LIVE STUDENT ACCESS KEY & 1-DEVICE SESSION WATCHER
    // Observed above quizzes & lessons so an old device logs out immediately if the matching email + key sign in elsewhere
    val studentAccessStatus by viewModel.studentAccessStatus.collectAsStateWithLifecycle()
    val strictKeyAccessStatus by viewModel.strictKeyAccessStatus.collectAsStateWithLifecycle()
    val requiresStudentKey = !isAdminUser || entryMode == AppEntryMode.STUDENT_KEY

    val effectiveAccessStatus = if (entryMode == AppEntryMode.STUDENT_KEY) {
        strictKeyAccessStatus
    } else {
        studentAccessStatus
    }

    // Record that THIS device actively holds a bound Access Key for currentUserId
    // (only when a key is bound, so non-key Free users are unaffected while key transfers auto-logout old devices)
    LaunchedEffect(requiresStudentKey, effectiveAccessStatus, currentUserId) {
        if (requiresStudentKey &&
            effectiveAccessStatus is StudentAccessStatus.Unlocked &&
            effectiveAccessStatus.key != null
        ) {
            onSetActiveStudentSessionUid(currentUserId)
        }
    }

    // OPTIONAL KEY: Automatically claim or transfer the key if the student entered one on AuthScreen
    LaunchedEffect(entryMode, pendingAccessKey, strictKeyAccessStatus) {
        if (entryMode == AppEntryMode.STUDENT_KEY &&
            pendingAccessKey.isNotBlank() &&
            (strictKeyAccessStatus is StudentAccessStatus.NeedsKey ||
                strictKeyAccessStatus is StudentAccessStatus.DeviceMismatch)
        ) {
            val codeToActivate = pendingAccessKey
            onClearPendingAccessKey()
            viewModel.activateStudentKey(codeToActivate)
        }
    }

    // OLD DEVICE: Automatically log out when this device previously held a bound key and another device took over
    LaunchedEffect(requiresStudentKey, effectiveAccessStatus, activeStudentSessionUid, pendingAccessKey, forcedLogoutTriggered) {
        if (requiresStudentKey &&
            !forcedLogoutTriggered &&
            effectiveAccessStatus is StudentAccessStatus.DeviceMismatch &&
            activeStudentSessionUid == currentUserId &&
            pendingAccessKey.isBlank()
        ) {
            forcedLogoutTriggered = true
            viewModel.exitQuizSession()
            activeHtmlLesson = null
            activeMoreMenuTab = null
            onSetForcedLogoutNotice(
                "You were automatically logged out because your email and Access Key (${effectiveAccessStatus.key.keyCode}) were signed in on another device."
            )
            performSignOut()
        }
    }

    // Access Key is now OPTIONAL: students without a key enter directly on the Free plan.
    // Only block if an admin explicitly revoked this student's bound key.
    if (requiresStudentKey && effectiveAccessStatus is StudentAccessStatus.Revoked) {
        val keyActivationError by viewModel.keyActivationError.collectAsStateWithLifecycle()
        AccessKeyGateScreen(
            accessStatus = effectiveAccessStatus,
            deviceId = deviceId,
            userEmail = currentUserEmail,
            initialKeyCode = pendingAccessKey,
            isMutating = isMutating,
            errorMessage = keyActivationError,
            snackbarHostState = snackbarHostState,
            onActivateKey = viewModel::activateStudentKey,
            onClearError = viewModel::clearKeyError,
            onPostMessage = viewModel::postMessage,
            onSwitchToAdminPortal = if (isAdminUser) ({
                adminPreviewingStudent = false
                onChangeEntryMode(AppEntryMode.ADMIN)
            }) else null,
            onSignOut = performSignOut
        )
        return
    }

    // 3. Full-Screen Interactive HTML Topic Lesson Viewer (Student)
    if (lessonEntry != null) {
        InteractiveLessonHost(
            viewModel = viewModel,
            lessonPost = lessonEntry.first,
            isAdminPreviewMode = lessonEntry.second,
            hasActiveQuiz = activeQuiz != null,
            isDarkTheme = isDarkTheme,
            onPracticeLinkedMcqs = { post ->
                activeHtmlLesson = null
                viewModel.startLinkedMcqsForPost(post)
            },
            onBack = { activeHtmlLesson = null }
        )
        return
    }

    // 4. Active Quiz / Timed Exam / Interactive Offline Pack Player
    val quiz = activeQuiz
    if (quiz != null) {
        ActiveQuizHost(
            viewModel = viewModel,
            quiz = quiz,
            functionalSettings = functionalSettings,
            onOpenInteractiveLesson = { post -> activeHtmlLesson = post to false },
            onOpenPlansTab = {
                viewModel.exitQuizSession()
                activeMoreMenuTab = MoreMenuTab.PLANS
            }
        )
        return
    }

    val activeKey = (effectiveAccessStatus as? StudentAccessStatus.Unlocked)?.key

    // 4 & 5. UNLOCKED STUDENT APP (Scoped Student Firestore flows only)
    StudentUnlockedHost(
        viewModel = viewModel,
        currentUserId = currentUserId,
        deviceId = deviceId,
        activeKey = activeKey,
        appearanceConfig = appearanceConfig,
        functionalSettings = functionalSettings,
        isDarkTheme = isDarkTheme,
        isAdminUser = isAdminUser,
        adminPreviewingStudent = adminPreviewingStudent,
        isMutating = isMutating,
        activeMoreMenuTab = activeMoreMenuTab,
        snackbarHostState = snackbarHostState,
        onSetActiveMoreMenuTab = { activeMoreMenuTab = it },
        onOpenInteractiveLesson = { post -> activeHtmlLesson = post to false },
        onExitAdminPreview = { adminPreviewingStudent = false },
        onReturnToAdminPortal = {
            activeMoreMenuTab = null
            adminPreviewingStudent = false
            onChangeEntryMode(AppEntryMode.ADMIN)
        },
        onUpdateAppearance = onUpdateAppearance,
        onUpdateFunctionalSettings = onUpdateFunctionalSettings,
        onSelectThemeMode = onSelectThemeMode,
        onToggleTheme = onToggleTheme,
        onSignOut = performSignOut
    )
}

@Composable
private fun InteractiveLessonHost(
    viewModel: MedPrepViewModel,
    lessonPost: StudyPost,
    isAdminPreviewMode: Boolean,
    hasActiveQuiz: Boolean,
    isDarkTheme: Boolean,
    onPracticeLinkedMcqs: (StudyPost) -> Unit,
    onBack: () -> Unit
) {
    val publishedState by viewModel.publishedQuestionsState.collectAsStateWithLifecycle()
    val corePublishedQuestions = remember(publishedState) {
        (publishedState as? UiState.Success)?.data
            ?.filter { MedPrepConstants.isCoreSubject(it.subject) }
            .orEmpty()
    }
    val linkedMcqCount = remember(lessonPost.id, lessonPost.subject, lessonPost.topic, corePublishedQuestions) {
        TopicLinkResolver.findLinkedQuestions(lessonPost, corePublishedQuestions).size
    }
    InteractiveLessonViewerScreen(
        post = lessonPost,
        linkedMcqCount = linkedMcqCount,
        isDarkTheme = isDarkTheme,
        isAdminPreview = isAdminPreviewMode,
        onPracticeLinkedMcqs = if (!isAdminPreviewMode && !hasActiveQuiz) {
            { onPracticeLinkedMcqs(lessonPost) }
        } else null,
        onPostMessage = viewModel::postMessage,
        onBack = onBack
    )
}

@Composable
private fun ActiveQuizHost(
    viewModel: MedPrepViewModel,
    quiz: ActiveQuizState,
    functionalSettings: AppFunctionalSettings,
    onOpenInteractiveLesson: (StudyPost) -> Unit,
    onOpenPlansTab: () -> Unit
) {
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val attemptsMap by viewModel.attemptsMapState.collectAsStateWithLifecycle()
    val publishedPostsState by viewModel.publishedPostsState.collectAsStateWithLifecycle()
    val difficultQuestions by viewModel.difficultQuestionsState.collectAsStateWithLifecycle()
    val studyNotes by viewModel.studyNotesState.collectAsStateWithLifecycle()

    val activePlan = SubscriptionPlanTier.fromKey((profileState as? UiState.Success)?.data?.subscriptionPlan)
    val publishedPosts = remember(publishedPostsState) {
        (publishedPostsState as? UiState.Success)?.data.orEmpty()
    }
    val difficultSourceIds = remember(difficultQuestions) {
        difficultQuestions.mapNotNull { it.sourceQuestionId }.toSet()
    }

    QuizSessionScreen(
        quizState = quiz,
        attemptsMap = attemptsMap,
        publishedPosts = publishedPosts,
        activePlan = activePlan,
        currentNotesCount = studyNotes.size,
        difficultSourceIds = difficultSourceIds,
        showInstantReasonSetting = functionalSettings.showInstantReason,
        onSelectAnswer = viewModel::selectAnswer,
        onToggleBookmark = viewModel::toggleBookmark,
        onToggleDifficult = viewModel::markMcqAsDifficult,
        onSaveQuickNote = viewModel::saveStudyNote,
        onOpenInteractiveLesson = onOpenInteractiveLesson,
        onOpenPlansTab = onOpenPlansTab,
        onNavigateTo = viewModel::navigateQuizTo,
        onPrevious = viewModel::previousQuestion,
        onNext = viewModel::nextQuestion,
        onFinish = viewModel::finishQuizSession,
        onRetest = viewModel::retestCurrentSession,
        onEnterReview = viewModel::enterSummaryReview,
        onPostMessage = viewModel::postMessage,
        onExit = viewModel::exitQuizSession
    )
}

@Composable
private fun AdminPortalHost(
    viewModel: MedPrepViewModel,
    isDarkTheme: Boolean,
    isMutating: Boolean,
    snackbarHostState: SnackbarHostState,
    onToggleTheme: () -> Unit,
    onPreviewHtmlLesson: (StudyPost) -> Unit,
    onPreviewStudentApp: () -> Unit,
    onSignOut: () -> Unit
) {
    var streamsReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        streamsReady = true
    }

    val studioState by viewModel.studioQuestionsState.collectAsStateWithLifecycle()
    val adminCoursesState = if (streamsReady) {
        viewModel.adminCoursesState.collectAsStateWithLifecycle().value
    } else UiState.Loading
    val adminPostsState = if (streamsReady) {
        viewModel.adminPostsState.collectAsStateWithLifecycle().value
    } else UiState.Loading
    val allAccessKeysState = if (streamsReady) {
        viewModel.allAccessKeysState.collectAsStateWithLifecycle().value
    } else UiState.Loading
    val allUsersState = if (streamsReady) {
        viewModel.allUsersState.collectAsStateWithLifecycle().value
    } else UiState.Loading
    val allSubscriptionRequestsState = if (streamsReady) {
        viewModel.allSubscriptionRequestsState.collectAsStateWithLifecycle().value
    } else UiState.Loading

    AdminPanelScreen(
        studioQuestionsState = studioState,
        adminCoursesState = adminCoursesState,
        adminPostsState = adminPostsState,
        accessKeysState = allAccessKeysState,
        allUsersState = allUsersState,
        allSubscriptionRequestsState = allSubscriptionRequestsState,
        isDarkTheme = isDarkTheme,
        isMutating = isMutating,
        snackbarHostState = snackbarHostState,
        onToggleTheme = onToggleTheme,
        onSaveQuestion = viewModel::saveQuestion,
        onToggleStatus = viewModel::toggleQuestionStatus,
        onDuplicateQuestion = viewModel::duplicateQuestion,
        onPublishAllDrafts = viewModel::publishAllDrafts,
        onCleanupLegacySubjects = viewModel::cleanupLegacySubjects,
        onDeleteQuestion = viewModel::deleteQuestion,
        onImportBatch = viewModel::importQuestions,
        onLoadStarterPack = viewModel::loadStarterPack,
        onSaveCourse = viewModel::saveCourse,
        onToggleCourseStatus = viewModel::toggleCourseStatus,
        onDeleteCourse = viewModel::deleteCourse,
        onSavePost = viewModel::savePost,
        onTogglePostStatus = viewModel::togglePostStatus,
        onDeletePost = viewModel::deletePost,
        onPreviewHtmlLesson = onPreviewHtmlLesson,
        onGenerateAccessKey = viewModel::generateAccessKey,
        onResetAccessKeyDevice = viewModel::resetAccessKeyDevice,
        onToggleAccessKeyStatus = viewModel::toggleAccessKeyStatus,
        onDeleteAccessKey = viewModel::deleteAccessKey,
        onReviewSubscriptionRequest = viewModel::adminReviewSubscriptionRequest,
        onSetUserPlan = viewModel::adminSetUserPlan,
        onDeleteSubscriptionRequest = viewModel::adminDeleteSubscriptionRequest,
        onPostMessage = viewModel::postMessage,
        onPreviewStudentApp = onPreviewStudentApp,
        onSignOut = onSignOut
    )
}

@Composable
private fun StudentUnlockedHost(
    viewModel: MedPrepViewModel,
    currentUserId: String,
    deviceId: String,
    activeKey: com.example.data.AccessKey?,
    appearanceConfig: AppAppearanceConfig,
    functionalSettings: AppFunctionalSettings,
    isDarkTheme: Boolean,
    isAdminUser: Boolean,
    adminPreviewingStudent: Boolean,
    isMutating: Boolean,
    activeMoreMenuTab: MoreMenuTab?,
    snackbarHostState: SnackbarHostState,
    onSetActiveMoreMenuTab: (MoreMenuTab?) -> Unit,
    onOpenInteractiveLesson: (StudyPost) -> Unit,
    onExitAdminPreview: () -> Unit,
    onReturnToAdminPortal: () -> Unit,
    onUpdateAppearance: (AppAppearanceConfig) -> Unit,
    onUpdateFunctionalSettings: (AppFunctionalSettings) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(currentUserId) {
        withFrameNanos { }
        viewModel.loadOfflinePacks(context)
    }

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val publishedState by viewModel.publishedQuestionsState.collectAsStateWithLifecycle()
    val publishedCoursesState by viewModel.publishedCoursesState.collectAsStateWithLifecycle()
    val publishedPostsState by viewModel.publishedPostsState.collectAsStateWithLifecycle()
    val attemptsMap by viewModel.attemptsMapState.collectAsStateWithLifecycle()
    val downloadedPacks by viewModel.downloadedPacks.collectAsStateWithLifecycle()
    val offlineStudyCache by viewModel.offlineStudyCache.collectAsStateWithLifecycle()
    val localStorageKb by viewModel.localStorageKb.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val activePlan = SubscriptionPlanTier.fromKey((profileState as? UiState.Success)?.data?.subscriptionPlan)
    val maxPracticeCap = PlanEntitlements.maxPracticeQuestionsPerSession(activePlan)

    val onlineCoreQuestions = remember(publishedState) {
        (publishedState as? UiState.Success)?.data
            ?.filter { MedPrepConstants.isCoreSubject(it.subject) }
            .orEmpty()
    }
    val onlineCourses = remember(publishedCoursesState) {
        (publishedCoursesState as? UiState.Success)?.data.orEmpty()
    }
    val onlinePosts = remember(publishedPostsState) {
        (publishedPostsState as? UiState.Success)?.data.orEmpty()
    }

    // Automatically save core MCQs, Courses & Study Posts into private mobile storage for offline study
    LaunchedEffect(onlineCoreQuestions, onlineCourses, onlinePosts) {
        if (onlineCoreQuestions.isNotEmpty() || onlineCourses.isNotEmpty() || onlinePosts.isNotEmpty()) {
            viewModel.autoCacheStudyData(
                context = context,
                questions = onlineCoreQuestions,
                courses = onlineCourses,
                posts = onlinePosts
            )
        }
    }

    // Seamless offline fallback to private mobile storage whenever network/Firestore is unavailable
    val effectivePublishedState = remember(publishedState, offlineStudyCache) {
        when (val s = publishedState) {
            is UiState.Success -> {
                if (s.data.isNotEmpty()) s
                else offlineStudyCache?.questions?.takeIf { it.isNotEmpty() }?.let { UiState.Success(it) } ?: s
            }
            is UiState.Loading, is UiState.Error -> {
                offlineStudyCache?.questions?.takeIf { it.isNotEmpty() }?.let { UiState.Success(it) } ?: s
            }
        }
    }
    val effectiveCoursesState = remember(publishedCoursesState, offlineStudyCache) {
        when (val s = publishedCoursesState) {
            is UiState.Success -> {
                if (s.data.isNotEmpty()) s
                else offlineStudyCache?.courses?.takeIf { it.isNotEmpty() }?.let { UiState.Success(it) } ?: s
            }
            is UiState.Loading, is UiState.Error -> {
                offlineStudyCache?.courses?.takeIf { it.isNotEmpty() }?.let { UiState.Success(it) } ?: s
            }
        }
    }
    val effectivePostsState = remember(publishedPostsState, offlineStudyCache) {
        when (val s = publishedPostsState) {
            is UiState.Success -> {
                if (s.data.isNotEmpty()) s
                else offlineStudyCache?.posts?.takeIf { it.isNotEmpty() }?.let { UiState.Success(it) } ?: s
            }
            is UiState.Loading, is UiState.Error -> {
                offlineStudyCache?.posts?.takeIf { it.isNotEmpty() }?.let { UiState.Success(it) } ?: s
            }
        }
    }
    val isOfflineFallbackActive = publishedState !is UiState.Success && (offlineStudyCache?.questions?.isNotEmpty() == true)

    val corePublishedQuestions = remember(effectivePublishedState) {
        (effectivePublishedState as? UiState.Success)?.data
            ?.filter { MedPrepConstants.isCoreSubject(it.subject) }
            .orEmpty()
    }
    val effectivePostsList = remember(effectivePostsState) {
        (effectivePostsState as? UiState.Success)?.data.orEmpty()
    }

    val saveOfflineSnapshotNow: () -> Unit = {
        viewModel.autoCacheStudyData(
            context = context,
            questions = corePublishedQuestions,
            courses = (effectiveCoursesState as? UiState.Success)?.data.orEmpty(),
            posts = effectivePostsList,
            force = true,
            showToast = true
        )
    }

    // 4. THREE-DOTS (⋮) LIQUID GLASS 8-TAB MENU HUB
    if (activeMoreMenuTab != null) {
        val studyNotes by viewModel.studyNotesState.collectAsStateWithLifecycle()
        val difficultQuestions by viewModel.difficultQuestionsState.collectAsStateWithLifecycle()
        val userSubscriptionRequests by viewModel.userSubscriptionRequestsState.collectAsStateWithLifecycle()

        MoreMenuHubScreen(
            initialTab = activeMoreMenuTab,
            currentUserId = currentUserId,
            profileState = profileState,
            publishedQuestions = corePublishedQuestions,
            attemptsMap = attemptsMap,
            studyNotes = studyNotes,
            difficultQuestions = difficultQuestions,
            downloadedPacks = downloadedPacks,
            offlineStudyCache = offlineStudyCache,
            localStorageKb = localStorageKb,
            userSubscriptionRequests = userSubscriptionRequests,
            activeAccessKey = activeKey,
            deviceId = deviceId,
            appearanceConfig = appearanceConfig,
            functionalSettings = functionalSettings,
            isDarkTheme = isDarkTheme,
            isAdminUser = isAdminUser,
            isSyncing = isSyncing,
            isMutating = isMutating,
            snackbarHostState = snackbarHostState,
            onUpdateAppearance = onUpdateAppearance,
            onUpdateFunctionalSettings = onUpdateFunctionalSettings,
            onSaveFullProfile = { name, exam, goal, email, phone, days ->
                viewModel.saveProfilePreferences(name, exam, goal, email, phone, days)
            },
            onActivateOptionalKey = viewModel::activateStudentKey,
            onSubmitPaymentRequest = viewModel::submitSubscriptionPaymentRequest,
            onCancelPendingRequest = viewModel::cancelSubscriptionRequest,
            onSaveScheduleDays = viewModel::saveScheduleDays,
            onStartStudy = { mode, subj, count ->
                viewModel.startQuizSession(
                    mode = mode,
                    subjectFilter = subj,
                    maxQuestions = count,
                    shuffleOptions = functionalSettings.shuffleOptions,
                    examSecondsPerQuestion = functionalSettings.examSecondsPerQuestion
                )
            },
            onStartCustomQuiz = { title, qs ->
                viewModel.startCustomQuestionsQuiz(
                    title = title,
                    questions = qs,
                    shuffleOptions = functionalSettings.shuffleOptions,
                    examSecondsPerQuestion = functionalSettings.examSecondsPerQuestion
                )
            },
            onSaveOfflinePack = { title, subj, qs ->
                viewModel.saveOfflineInteractivePack(context, title, subj, qs)
            },
            onDeleteOfflinePack = { packId ->
                viewModel.deleteOfflineInteractivePack(context, packId)
            },
            onSaveOfflineSnapshotNow = saveOfflineSnapshotNow,
            onClearOfflineCache = { viewModel.clearOfflineStudyCache(context) },
            onSaveNote = viewModel::saveStudyNote,
            onDeleteNote = viewModel::deleteStudyNote,
            onSaveDifficultQuestion = viewModel::saveDifficultQuestion,
            onDeleteDifficultQuestion = viewModel::deleteDifficultQuestion,
            onSyncNow = viewModel::syncNow,
            onOpenAdminPanel = if (isAdminUser) onReturnToAdminPortal else null,
            onSignOut = onSignOut,
            onPostMessage = viewModel::postMessage,
            onClose = { onSetActiveMoreMenuTab(null) }
        )
        return
    }

    // 5. PURE STUDENT APP WITH LIQUID GLASS BOTTOM TABS
    var currentTab by rememberSaveable { mutableStateOf(StudentTab.STUDY) }

    if (currentTab != StudentTab.STUDY) {
        BackHandler { currentTab = StudentTab.STUDY }
    } else if (isAdminUser && adminPreviewingStudent) {
        BackHandler { onExitAdminPreview() }
    }

    val studyLabel = stringResource(StudentTab.STUDY.labelRes)
    val coursesLabel = stringResource(StudentTab.COURSES.labelRes)
    val postsLabel = stringResource(StudentTab.POSTS.labelRes)
    val profileLabel = stringResource(StudentTab.PROFILE.labelRes)
    val navGlassItems = remember(studyLabel, coursesLabel, postsLabel, profileLabel) {
        listOf(
            LiquidGlassTabItem(
                id = StudentTab.STUDY.id,
                label = studyLabel,
                selectedIcon = StudentTab.STUDY.selectedIcon,
                unselectedIcon = StudentTab.STUDY.unselectedIcon,
                testTag = StudentTab.STUDY.tag
            ),
            LiquidGlassTabItem(
                id = StudentTab.COURSES.id,
                label = coursesLabel,
                selectedIcon = StudentTab.COURSES.selectedIcon,
                unselectedIcon = StudentTab.COURSES.unselectedIcon,
                testTag = StudentTab.COURSES.tag
            ),
            LiquidGlassTabItem(
                id = StudentTab.POSTS.id,
                label = postsLabel,
                selectedIcon = StudentTab.POSTS.selectedIcon,
                unselectedIcon = StudentTab.POSTS.unselectedIcon,
                testTag = StudentTab.POSTS.tag
            ),
            LiquidGlassTabItem(
                id = StudentTab.PROFILE.id,
                label = profileLabel,
                selectedIcon = StudentTab.PROFILE.selectedIcon,
                unselectedIcon = StudentTab.PROFILE.unselectedIcon,
                testTag = StudentTab.PROFILE.tag
            )
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    LiquidGlassBottomNavBar(
                        items = navGlassItems,
                        selectedId = currentTab.id,
                        onSelect = { id ->
                            currentTab = StudentTab.entries.firstOrNull { it.id == id } ?: StudentTab.STUDY
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        StudentTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        StudentTab.STUDY -> DashboardScreen(
                            profileState = profileState,
                            publishedState = effectivePublishedState,
                            attemptsMap = attemptsMap,
                            downloadedPacks = downloadedPacks,
                            offlineStudyCache = offlineStudyCache,
                            localStorageKb = localStorageKb,
                            isOfflineFallbackActive = isOfflineFallbackActive,
                            isDarkTheme = isDarkTheme,
                            isSyncing = isSyncing,
                            onToggleTheme = onToggleTheme,
                            onSyncNow = {
                                viewModel.syncNow()
                                saveOfflineSnapshotNow()
                            },
                            onStartStudy = { mode, subj, count ->
                                viewModel.startQuizSession(
                                    mode = mode,
                                    subjectFilter = subj,
                                    maxQuestions = count,
                                    shuffleOptions = functionalSettings.shuffleOptions,
                                    examSecondsPerQuestion = functionalSettings.examSecondsPerQuestion
                                )
                            },
                            onStartCustomQuiz = { title, qs ->
                                viewModel.startCustomQuestionsQuiz(
                                    title = title,
                                    questions = qs,
                                    shuffleOptions = functionalSettings.shuffleOptions,
                                    examSecondsPerQuestion = functionalSettings.examSecondsPerQuestion
                                )
                            },
                            onSaveOfflinePack = { title, subj, qs ->
                                viewModel.saveOfflineInteractivePack(context, title, subj, qs, silent = false)
                            },
                            onSaveOfflineSnapshot = saveOfflineSnapshotNow,
                            onOpenMoreTab = onSetActiveMoreMenuTab,
                            onSignOut = onSignOut,
                            onPostMessage = viewModel::postMessage
                        )

                        StudentTab.COURSES -> {
                            StudentCoursesScreen(
                                activePlan = activePlan,
                                coursesState = effectiveCoursesState,
                                publishedPosts = effectivePostsList,
                                publishedQuestions = corePublishedQuestions,
                                isDarkTheme = isDarkTheme,
                                onOpenInteractiveLesson = onOpenInteractiveLesson,
                                onPracticeCourseMcqs = { mode, subj, count ->
                                    viewModel.startQuizSession(
                                        mode = mode,
                                        subjectFilter = subj,
                                        maxQuestions = minOf(count, maxPracticeCap),
                                        shuffleOptions = functionalSettings.shuffleOptions,
                                        examSecondsPerQuestion = functionalSettings.examSecondsPerQuestion
                                    )
                                },
                                onOpenPlansTab = { onSetActiveMoreMenuTab(MoreMenuTab.PLANS) },
                                onPostMessage = viewModel::postMessage
                            )
                        }

                        StudentTab.POSTS -> {
                            PostsFeedScreen(
                                activePlan = activePlan,
                                postsState = effectivePostsState,
                                publishedQuestions = corePublishedQuestions,
                                isDarkTheme = isDarkTheme,
                                onOpenInteractiveLesson = onOpenInteractiveLesson,
                                onPracticeLinkedMcqs = { post ->
                                    viewModel.startLinkedMcqsForPost(post, maxPracticeCap)
                                },
                                onOpenPlansTab = { onSetActiveMoreMenuTab(MoreMenuTab.PLANS) },
                                onPostMessage = viewModel::postMessage
                            )
                        }

                        StudentTab.PROFILE -> {
                            val examSessions by viewModel.examSessionsState.collectAsStateWithLifecycle()
                            val lastSyncReport by viewModel.lastSyncReport.collectAsStateWithLifecycle()
                            ProfileScreen(
                                profileState = profileState,
                                publishedQuestions = corePublishedQuestions,
                                offlineStudyCache = offlineStudyCache,
                                localStorageKb = localStorageKb,
                                examSessions = examSessions,
                                activeAccessKey = activeKey,
                                deviceId = deviceId,
                                themeMode = appearanceConfig.themeMode,
                                isAdminUser = isAdminUser,
                                isSyncing = isSyncing,
                                lastSyncReport = lastSyncReport,
                                isMutating = isMutating,
                                onSelectThemeMode = onSelectThemeMode,
                                onOpenMoreTab = onSetActiveMoreMenuTab,
                                onSaveOfflineSnapshot = saveOfflineSnapshotNow,
                                onActivateOptionalKey = viewModel::activateStudentKey,
                                onSyncNow = {
                                    viewModel.syncNow()
                                    saveOfflineSnapshotNow()
                                },
                                onSavePreferences = { name, exam, goal ->
                                    viewModel.saveProfilePreferences(name, exam, goal)
                                },
                                onPostMessage = viewModel::postMessage,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}
