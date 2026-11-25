package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccessKey
import com.example.data.CourseDraftInput
import com.example.data.CourseItem
import com.example.data.CsvMcqParser
import com.example.data.DifficultQuestionDraft
import com.example.data.DifficultQuestionItem
import com.example.data.DownloadedMcqPack
import com.example.data.ExamSession
import com.example.data.LocalStudyCache
import com.example.data.McqQuestion
import com.example.data.MedPrepBackupPayload
import com.example.data.MedPrepConstants
import com.example.data.MedPrepRepository
import com.example.data.OfflinePackStorage
import com.example.data.QuestionAttempt
import com.example.data.QuestionDraftInput
import com.example.data.ShuffledQuestion
import com.example.data.StudentAccessStatus
import com.example.data.StudyMode
import com.example.data.StudyNote
import com.example.data.StudyNoteDraft
import com.example.data.StudyPost
import com.example.data.StudyPostDraftInput
import com.example.data.SubscriptionPlanTier
import com.example.data.SubscriptionRequest
import com.example.data.SubscriptionRequestInput
import com.example.data.SyncReport
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.theme.AppAppearanceConfig
import com.example.ui.theme.AppFunctionalSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveQuizState(
    val mode: StudyMode,
    val subjectFilter: String,
    val questions: List<ShuffledQuestion>,
    val currentIndex: Int = 0,
    /** Maps questionId -> selected DISPLAY letter ("A".."D") for the current shuffled run */
    val selectedAnswers: Map<String, String> = emptyMap(),
    val elapsedSeconds: Long = 0L,
    val timeLimitSeconds: Long? = null,
    val completed: Boolean = false,
    val reviewingSummary: Boolean = false,
    val attemptNumber: Int = 1
) {
    val currentShuffled: ShuffledQuestion? get() = questions.getOrNull(currentIndex)
    val answeredCount: Int get() = selectedAnswers.size
    val correctCount: Int
        get() = questions.count { sq ->
            val pickedDisplay = selectedAnswers[sq.question.id]
            pickedDisplay != null && pickedDisplay.equals(sq.correctDisplayLetter, ignoreCase = true)
        }
    val remainingSeconds: Long?
        get() = timeLimitSeconds?.let { (it - elapsedSeconds).coerceAtLeast(0L) }
}

class MedPrepViewModel(
    private val repository: MedPrepRepository,
    val currentUserId: String,
    val deviceId: String
) : ViewModel() {

    private val bootstrapAdmin = repository.isBootstrapAdmin()

    private val _isAdminUser = MutableStateFlow(bootstrapAdmin)
    val isAdminUser: StateFlow<Boolean> = _isAdminUser.asStateFlow()

    private val _adminCheckDone = MutableStateFlow(bootstrapAdmin)

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiStateFlow("observeUserProfile", "Failed to load profile")

    val publishedQuestionsState: StateFlow<UiState<List<McqQuestion>>> =
        repository.observePublishedQuestions(currentUserId)
            .toUiStateFlow("observePublishedQuestions", "Failed to load question bank")

    val studioQuestionsState: StateFlow<UiState<List<McqQuestion>>> =
        (if (bootstrapAdmin) repository.observeStudioQuestions(currentUserId, true) else flowOf(emptyList()))
            .toUiStateFlow("observeStudioQuestions", "Failed to load Admin questions")

    val publishedPostsState: StateFlow<UiState<List<StudyPost>>> =
        repository.observePublishedPosts(currentUserId)
            .toUiStateFlow("observePublishedPosts", "Failed to load study posts")

    val adminPostsState: StateFlow<UiState<List<StudyPost>>> =
        (if (bootstrapAdmin) repository.observeAdminPosts(currentUserId, true) else flowOf(emptyList()))
            .toUiStateFlow("observeAdminPosts", "Failed to load Admin posts")

    val publishedCoursesState: StateFlow<UiState<List<CourseItem>>> =
        repository.observePublishedCourses(currentUserId)
            .toUiStateFlow("observePublishedCourses", "Failed to load courses")

    val adminCoursesState: StateFlow<UiState<List<CourseItem>>> =
        (if (bootstrapAdmin) repository.observeAdminCourses(currentUserId, true) else flowOf(emptyList()))
            .toUiStateFlow("observeAdminCourses", "Failed to load Admin courses")

    val userAccessKeysState: StateFlow<UiState<List<AccessKey>>> =
        repository.observeUserAccessKeys(currentUserId)
            .toUiStateFlow("observeUserAccessKeys", "Failed to verify Access Key")

    val allAccessKeysState: StateFlow<UiState<List<AccessKey>>> =
        (if (bootstrapAdmin) repository.observeAllAccessKeys(currentUserId) else flowOf(emptyList()))
            .toUiStateFlow("observeAllAccessKeys", "Failed to load Access Keys")

    val userSubscriptionRequestsState: StateFlow<List<SubscriptionRequest>> =
        repository.observeUserSubscriptionRequests(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing user subscription requests", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val allSubscriptionRequestsState: StateFlow<UiState<List<SubscriptionRequest>>> =
        (if (bootstrapAdmin) repository.observeAllSubscriptionRequests(currentUserId) else flowOf(emptyList()))
            .toUiStateFlow("observeAllSubscriptionRequests", "Failed to load payment requests")

    val allUsersState: StateFlow<UiState<List<UserProfile>>> =
        (if (bootstrapAdmin) repository.observeAllUserProfiles(currentUserId) else flowOf(emptyList()))
            .toUiStateFlow("observeAllUserProfiles", "Failed to load student accounts")

    val strictKeyAccessStatus: StateFlow<StudentAccessStatus> = userAccessKeysState
        .map { keysState ->
            when (keysState) {
                is UiState.Loading -> StudentAccessStatus.Checking
                is UiState.Error -> StudentAccessStatus.NeedsKey
                is UiState.Success -> {
                    val keys = keysState.data
                    val activeMatching = keys.firstOrNull {
                        it.status == "active" && it.boundDeviceId == deviceId
                    }
                    when {
                        activeMatching != null -> StudentAccessStatus.Unlocked(activeMatching)
                        keys.any { it.status == "active" && it.boundDeviceId.isNotBlank() && it.boundDeviceId != deviceId } -> {
                            val mismatched = keys.first { it.status == "active" && it.boundDeviceId != deviceId }
                            StudentAccessStatus.DeviceMismatch(mismatched)
                        }
                        keys.any { it.status == "revoked" } -> {
                            StudentAccessStatus.Revoked(keys.first { it.status == "revoked" })
                        }
                        else -> StudentAccessStatus.NeedsKey
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StudentAccessStatus.Checking
        )

    val studentAccessStatus: StateFlow<StudentAccessStatus> = combine(
        isAdminUser,
        _adminCheckDone,
        strictKeyAccessStatus
    ) { admin, adminChecked, strictStatus ->
        if (admin) {
            StudentAccessStatus.Unlocked(null)
        } else if (!adminChecked) {
            StudentAccessStatus.Checking
        } else {
            strictStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = if (bootstrapAdmin) StudentAccessStatus.Unlocked(null) else StudentAccessStatus.Checking
    )

    val attemptsMapState: StateFlow<Map<String, QuestionAttempt>> =
        repository.observeAttempts(currentUserId)
            .map { list -> list.associateBy { it.questionId } }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing attempts", firestoreError ?: error)
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyMap()
            )

    val examSessionsState: StateFlow<List<ExamSession>> =
        repository.observeExamSessions(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing exam sessions", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val studyNotesState: StateFlow<List<StudyNote>> =
        repository.observeStudyNotes(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing notes", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val difficultQuestionsState: StateFlow<List<DifficultQuestionItem>> =
        repository.observeDifficultQuestions(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing difficult questions", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    private val _downloadedPacks = MutableStateFlow<List<DownloadedMcqPack>>(emptyList())
    val downloadedPacks: StateFlow<List<DownloadedMcqPack>> = _downloadedPacks.asStateFlow()

    private val _offlineStudyCache = MutableStateFlow<LocalStudyCache?>(null)
    val offlineStudyCache: StateFlow<LocalStudyCache?> = _offlineStudyCache.asStateFlow()

    private val _localStorageKb = MutableStateFlow(0L)
    val localStorageKb: StateFlow<Long> = _localStorageKb.asStateFlow()

    private var lastCachedSignature: String = ""

    private val _activeQuiz = MutableStateFlow<ActiveQuizState?>(null)
    val activeQuiz: StateFlow<ActiveQuizState?> = _activeQuiz.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncReport = MutableStateFlow<SyncReport?>(null)
    val lastSyncReport: StateFlow<SyncReport?> = _lastSyncReport.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _keyActivationError = MutableStateFlow<String?>(null)
    val keyActivationError: StateFlow<String?> = _keyActivationError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureUserProfile()
            val admin = repository.checkIsAdmin(currentUserId)
            _isAdminUser.value = admin
            _adminCheckDone.value = true
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun postMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun clearKeyError() {
        _keyActivationError.value = null
    }

    // --- SPECIAL ACCESS KEY ACTIVATION & ADMIN KEY MANAGEMENT ---

    fun activateStudentKey(rawKeyCode: String) {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            _keyActivationError.value = null
            repository.activateAccessKey(rawKeyCode, deviceId)
                .onSuccess { outcome ->
                    _snackbarMessage.value = if (outcome.transferredFromOldDevice) {
                        "Email & Access Key matched! Logged out from your old device and unlocked here."
                    } else {
                        "Access Key ${outcome.key.keyCode} activated and locked to this device!"
                    }
                }
                .onFailure { err ->
                    val msg = err.localizedMessage ?: "Could not activate Access Key."
                    _keyActivationError.value = msg
                    _snackbarMessage.value = msg
                }
            _isMutating.value = false
        }
    }

    fun generateAccessKey(
        label: String,
        customKeyCode: String? = null,
        onGenerated: (String) -> Unit = {}
    ) {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            repository.generateAccessKey(label, customKeyCode)
                .onSuccess { keyCode ->
                    _snackbarMessage.value = "Created Student Access Key: $keyCode"
                    onGenerated(keyCode)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to create key."
                }
            _isMutating.value = false
        }
    }

    fun resetAccessKeyDevice(keyCode: String) {
        viewModelScope.launch {
            repository.resetAccessKeyDevice(keyCode)
                .onSuccess { _snackbarMessage.value = "Device lock reset for $keyCode. Student can now log in on a new device." }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Failed to reset device lock." }
        }
    }

    fun toggleAccessKeyStatus(key: AccessKey) {
        val next = if (key.status == "active") "revoked" else "active"
        viewModelScope.launch {
            repository.toggleAccessKeyStatus(key.keyCode, next)
                .onSuccess {
                    _snackbarMessage.value = if (next == "revoked") "Key ${key.keyCode} revoked." else "Key ${key.keyCode} reactivated."
                }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Failed to update key status." }
        }
    }

    fun deleteAccessKey(keyCode: String) {
        viewModelScope.launch {
            repository.deleteAccessKey(keyCode)
                .onSuccess { _snackbarMessage.value = "Deleted key $keyCode." }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Failed to delete key." }
        }
    }

    // --- OFFLINE SYNC ---

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncOfflineNow()
                .onSuccess { report ->
                    _lastSyncReport.value = report
                    _snackbarMessage.value = report.message
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.localizedMessage ?: "Sync failed. Check connection."
                }
            _isSyncing.value = false
        }
    }

    // --- PROFILE PREFERENCES, PLANS, SCHEDULE, NOTES, DIFFICULT QUESTIONS & OFFLINE PACKS ---

    fun loadOfflinePacks(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = OfflinePackStorage.loadDownloadedPacks(appContext, currentUserId)
            val cache = OfflinePackStorage.loadStudyCache(appContext, currentUserId)
            val sizeKb = OfflinePackStorage.getLocalStorageSizeKb(appContext, currentUserId)
            _downloadedPacks.value = loaded
            _offlineStudyCache.value = cache
            _localStorageKb.value = sizeKb
        }
    }

    fun autoCacheStudyData(
        context: Context,
        questions: List<McqQuestion>,
        courses: List<CourseItem>,
        posts: List<StudyPost>,
        force: Boolean = false,
        showToast: Boolean = false
    ) {
        if (questions.isEmpty() && courses.isEmpty() && posts.isEmpty()) {
            if (showToast) {
                _snackbarMessage.value = "Connect once to load MCQs & lessons, then they will stay saved on your phone."
            }
            return
        }
        val signature = "${questions.size}_${questions.firstOrNull()?.id}_${questions.lastOrNull()?.id}_" +
            "${courses.size}_${courses.firstOrNull()?.id}_${posts.size}_${posts.firstOrNull()?.id}"
        if (!force && signature == lastCachedSignature) return
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val saved = OfflinePackStorage.saveStudyCache(
                    context = appContext,
                    uid = currentUserId,
                    questions = questions,
                    courses = courses,
                    posts = posts
                )
                lastCachedSignature = signature
                _offlineStudyCache.value = saved
                _localStorageKb.value = OfflinePackStorage.getLocalStorageSizeKb(appContext, currentUserId)
                if (showToast) {
                    _snackbarMessage.value = "Saved ${saved.questions.size} MCQs, ${saved.courses.size} Courses & ${saved.posts.size} Lessons in mobile storage for offline study!"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to write offline study cache", e)
            }
        }
    }

    fun clearOfflineStudyCache(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            OfflinePackStorage.clearStudyCache(appContext, currentUserId)
            lastCachedSignature = ""
            _offlineStudyCache.value = null
            _localStorageKb.value = OfflinePackStorage.getLocalStorageSizeKb(appContext, currentUserId)
            _snackbarMessage.value = "Cleared local offline study cache."
        }
    }

    fun saveOfflineInteractivePack(
        context: Context,
        title: String,
        subject: String,
        questions: List<McqQuestion>,
        silent: Boolean = false
    ) {
        if (questions.isEmpty()) {
            if (!silent) _snackbarMessage.value = "No MCQs available to save in mobile storage."
            return
        }
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val updated = OfflinePackStorage.saveDownloadedPack(
                context = appContext,
                uid = currentUserId,
                title = title,
                subject = subject,
                questions = questions
            )
            _downloadedPacks.value = updated
            _localStorageKb.value = OfflinePackStorage.getLocalStorageSizeKb(appContext, currentUserId)
            if (!silent) {
                _snackbarMessage.value = "Saved '$title' (${questions.size} MCQs) in mobile storage for offline study!"
            }
        }
    }

    fun deleteOfflineInteractivePack(context: Context, packId: String) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val updated = OfflinePackStorage.deleteDownloadedPack(appContext, currentUserId, packId)
            _downloadedPacks.value = updated
            _localStorageKb.value = OfflinePackStorage.getLocalStorageSizeKb(appContext, currentUserId)
            _snackbarMessage.value = "Removed offline study pack."
        }
    }

    fun saveProfilePreferences(
        displayName: String,
        targetExam: String,
        dailyGoal: Long,
        contactEmail: String? = null,
        phoneNumber: String? = null,
        scheduleDays: Long? = null
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            repository.updateProfilePreferences(
                displayName = displayName,
                targetExam = targetExam,
                dailyGoal = dailyGoal,
                contactEmail = contactEmail,
                phoneNumber = phoneNumber,
                scheduleDays = scheduleDays
            )
                .onSuccess { _snackbarMessage.value = "Profile & study preferences saved." }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to update profile." }
            _isMutating.value = false
        }
    }

    fun submitSubscriptionPaymentRequest(
        requestedPlan: SubscriptionPlanTier,
        transactionRef: String,
        senderPhone: String,
        userName: String,
        userEmail: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            repository.submitSubscriptionRequest(
                SubscriptionRequestInput(
                    userName = userName,
                    userEmail = userEmail,
                    requestedPlan = requestedPlan,
                    transactionRef = transactionRef,
                    senderPhone = senderPhone
                )
            )
                .onSuccess {
                    _snackbarMessage.value = "JazzCash payment submitted for ${requestedPlan.title} (PKR ${requestedPlan.amountPkr}). Admin will verify & activate your plan."
                    onSuccess()
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Could not submit payment request."
                }
            _isMutating.value = false
        }
    }

    fun cancelSubscriptionRequest(requestId: String) {
        viewModelScope.launch {
            repository.deleteSubscriptionRequest(requestId)
                .onSuccess { _snackbarMessage.value = "Payment request cancelled." }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Could not cancel request." }
        }
    }

    fun adminReviewSubscriptionRequest(
        request: SubscriptionRequest,
        approve: Boolean,
        adminNote: String = ""
    ) {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            repository.adminReviewSubscriptionRequest(request, approve, adminNote)
                .onSuccess {
                    val planLabel = SubscriptionPlanTier.fromKey(request.requestedPlan).title
                    _snackbarMessage.value = if (approve) {
                        "Approved! Granted $planLabel plan to ${request.userName.ifBlank { request.userEmail }}."
                    } else {
                        "Rejected payment request from ${request.userName.ifBlank { request.userEmail }}."
                    }
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Could not process request."
                }
            _isMutating.value = false
        }
    }

    fun adminSetUserPlan(userId: String, userName: String, planKey: String) {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            repository.adminSetUserSubscriptionPlan(userId, planKey)
                .onSuccess {
                    val tier = SubscriptionPlanTier.fromKey(planKey)
                    _snackbarMessage.value = if (tier == SubscriptionPlanTier.FREE) {
                        "Removed paid plan from $userName (set to Free)."
                    } else {
                        "Granted ${tier.title} plan to $userName!"
                    }
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Could not update user plan."
                }
            _isMutating.value = false
        }
    }

    fun adminDeleteSubscriptionRequest(requestId: String) {
        viewModelScope.launch {
            repository.deleteSubscriptionRequest(requestId)
                .onSuccess { _snackbarMessage.value = "Payment request record removed." }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Could not delete record." }
        }
    }

    fun saveScheduleDays(days: Long) {
        viewModelScope.launch {
            repository.updateScheduleDays(days)
                .onSuccess { _snackbarMessage.value = "Study schedule updated to cover syllabus in $days days!" }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Could not save schedule." }
        }
    }

    fun saveStudyNote(draft: StudyNoteDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            repository.saveStudyNote(draft)
                .onSuccess {
                    _snackbarMessage.value = if (draft.existingId.isNullOrBlank()) "Highlight/Note saved!" else "Note updated."
                    onSuccess()
                }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to save note." }
            _isMutating.value = false
        }
    }

    fun deleteStudyNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteStudyNote(noteId)
                .onSuccess { _snackbarMessage.value = "Note deleted." }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to delete note." }
        }
    }

    fun saveDifficultQuestion(draft: DifficultQuestionDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            repository.saveDifficultQuestion(draft)
                .onSuccess {
                    _snackbarMessage.value = "Saved to Difficult Exam Questions!"
                    onSuccess()
                }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to save difficult question." }
            _isMutating.value = false
        }
    }

    fun markMcqAsDifficult(question: McqQuestion) {
        val existing = difficultQuestionsState.value.firstOrNull { it.sourceQuestionId == question.id }
        if (existing != null) {
            deleteDifficultQuestion(existing.id)
            return
        }
        saveDifficultQuestion(
            DifficultQuestionDraft(
                sourceQuestionId = question.id,
                subject = question.subject,
                topic = question.topic,
                stem = question.stem,
                optionA = question.optionA,
                optionB = question.optionB,
                optionC = question.optionC,
                optionD = question.optionD,
                correctOption = question.correctOption,
                explanation = question.explanation
            )
        )
    }

    fun deleteDifficultQuestion(itemId: String) {
        viewModelScope.launch {
            repository.deleteDifficultQuestion(itemId)
                .onSuccess { _snackbarMessage.value = "Removed from Difficult Questions." }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to remove item." }
        }
    }

    fun importBackupPayload(
        context: Context,
        payload: MedPrepBackupPayload,
        onApplyAppearance: (AppAppearanceConfig) -> Unit,
        onApplySettings: (AppFunctionalSettings) -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                payload.appearance?.let { onApplyAppearance(it) }
                payload.settings?.let { onApplySettings(it) }

                val currentProfile = (userProfileState.value as? UiState.Success)?.data
                repository.updateProfilePreferences(
                    displayName = payload.displayName ?: currentProfile?.displayName ?: "Medical Student",
                    targetExam = payload.targetExam ?: currentProfile?.targetExam ?: MedPrepConstants.EXAM_TRACKS.first(),
                    dailyGoal = payload.dailyGoal ?: currentProfile?.dailyGoal ?: 25L,
                    contactEmail = payload.contactEmail,
                    phoneNumber = payload.phoneNumber,
                    scheduleDays = payload.scheduleDays
                )

                var restoredNotes = 0
                payload.notes.forEach { draft ->
                    if (repository.saveStudyNote(draft).isSuccess) restoredNotes++
                }

                var restoredDiff = 0
                payload.difficultQuestions.forEach { draft ->
                    if (repository.saveDifficultQuestion(draft).isSuccess) restoredDiff++
                }

                if (payload.offlinePacks.isNotEmpty()) {
                    val merged = withContext(Dispatchers.IO) {
                        OfflinePackStorage.mergeDownloadedPacks(
                            context.applicationContext,
                            currentUserId,
                            payload.offlinePacks
                        )
                    }
                    _downloadedPacks.value = merged
                }

                _snackbarMessage.value = "Backup restored! ($restoredNotes notes, $restoredDiff difficult Qs, ${payload.offlinePacks.size} offline packs)"
            } catch (e: Exception) {
                _snackbarMessage.value = "Backup restore error: ${e.localizedMessage ?: "Failed"}"
            }
            _isMutating.value = false
        }
    }

    // --- ADMIN MCQ & LEGACY SUBJECT CLEANUP ---

    fun loadStarterPack() {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            val pack = CsvMcqParser.buildHighYieldStarterPack()
            repository.importQuestionsBatch(pack)
                .onSuccess { count ->
                    _snackbarMessage.value = "Loaded $count 5-subject MCQs with reasons & YouTube links!"
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Could not load starter pack."
                }
            _isMutating.value = false
        }
    }

    fun cleanupLegacySubjects() {
        if (_isMutating.value) return
        val all = (studioQuestionsState.value as? UiState.Success)?.data.orEmpty()
        val legacyCount = all.count { !MedPrepConstants.isCoreSubject(it.subject) }
        if (legacyCount == 0) {
            _snackbarMessage.value = "Only the 5 core subjects exist — nothing to clean."
            return
        }
        viewModelScope.launch {
            _isMutating.value = true
            repository.deleteNonCoreSubjectQuestions(all)
                .onSuccess { deleted ->
                    _snackbarMessage.value = "Deleted $deleted old-subject MCQs. Only 5 core subjects remain."
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Could not remove old subjects."
                }
            _isMutating.value = false
        }
    }

    fun saveQuestion(input: QuestionDraftInput, onSuccess: () -> Unit = {}) {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            repository.saveQuestion(input)
                .onSuccess {
                    _snackbarMessage.value = if (input.existingId.isNullOrBlank()) {
                        "Question added to ${if (input.status == "published") "Published Bank" else "Drafts"}."
                    } else {
                        "Question updated."
                    }
                    onSuccess()
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Failed to save question."
                }
            _isMutating.value = false
        }
    }

    fun toggleQuestionStatus(question: McqQuestion) {
        val nextStatus = if (question.status == "published") "draft" else "published"
        viewModelScope.launch {
            repository.toggleQuestionStatus(question.id, nextStatus)
                .onSuccess {
                    _snackbarMessage.value = if (nextStatus == "published") {
                        "Question published to Study Bank."
                    } else {
                        "Question moved to Drafts."
                    }
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Could not change status."
                }
        }
    }

    fun deleteQuestion(questionId: String) {
        viewModelScope.launch {
            repository.deleteQuestion(questionId)
                .onSuccess { _snackbarMessage.value = "Question deleted." }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Could not delete question." }
        }
    }

    fun publishAllDrafts() {
        if (_isMutating.value) return
        val drafts = (studioQuestionsState.value as? UiState.Success)?.data
            ?.filter { it.status == "draft" && MedPrepConstants.isCoreSubject(it.subject) }.orEmpty()
        if (drafts.isEmpty()) {
            _snackbarMessage.value = "All 5-subject questions are already published!"
            return
        }
        viewModelScope.launch {
            _isMutating.value = true
            repository.publishAllDrafts(drafts)
                .onSuccess { count ->
                    _snackbarMessage.value = "Published $count draft MCQs to Study Bank!"
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Could not publish drafts."
                }
            _isMutating.value = false
        }
    }

    fun duplicateQuestion(question: McqQuestion) {
        saveQuestion(
            QuestionDraftInput(
                existingId = null,
                subject = question.subject,
                topic = question.topic,
                stem = "${question.stem} (Copy)".take(2000),
                optionA = question.optionA,
                optionB = question.optionB,
                optionC = question.optionC,
                optionD = question.optionD,
                correctOption = question.correctOption,
                explanation = question.explanation,
                youtubeUrl = question.youtubeUrl,
                linkedPostId = question.linkedPostId,
                difficulty = question.difficulty,
                status = "draft"
            )
        )
    }

    fun importQuestions(questions: List<QuestionDraftInput>, onComplete: () -> Unit = {}) {
        if (_isMutating.value || questions.isEmpty()) return
        viewModelScope.launch {
            _isMutating.value = true
            repository.importQuestionsBatch(questions)
                .onSuccess { count ->
                    _snackbarMessage.value = "Imported $count MCQs with Reasons successfully."
                    onComplete()
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Bulk import failed."
                }
            _isMutating.value = false
        }
    }

    // --- ADMIN STUDY POSTS (WEBPAGE LINKS + YOUTUBE SUGGESTED VIDEOS) ---

    fun savePost(input: StudyPostDraftInput, onSuccess: () -> Unit = {}) {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            repository.savePost(input)
                .onSuccess {
                    _snackbarMessage.value = if (input.existingId.isNullOrBlank()) {
                        "Study Post uploaded to Student Feed!"
                    } else {
                        "Study Post updated."
                    }
                    onSuccess()
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Failed to save post."
                }
            _isMutating.value = false
        }
    }

    fun togglePostStatus(post: StudyPost) {
        val next = if (post.status == "published") "draft" else "published"
        viewModelScope.launch {
            repository.togglePostStatus(post.id, next)
                .onSuccess {
                    _snackbarMessage.value = if (next == "published") "Post published to Students." else "Post moved to Drafts."
                }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to change post status." }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
                .onSuccess { _snackbarMessage.value = "Study Post deleted." }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to delete post." }
        }
    }

    // --- ADMIN COURSES MANAGEMENT ---

    fun saveCourse(input: CourseDraftInput, onSuccess: () -> Unit = {}) {
        if (_isMutating.value) return
        viewModelScope.launch {
            _isMutating.value = true
            repository.saveCourse(input)
                .onSuccess {
                    _snackbarMessage.value = if (input.existingId.isNullOrBlank()) {
                        "Course created and synced!"
                    } else {
                        "Course updated."
                    }
                    onSuccess()
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Failed to save course."
                }
            _isMutating.value = false
        }
    }

    fun toggleCourseStatus(course: CourseItem) {
        val next = if (course.status == "published") "draft" else "published"
        viewModelScope.launch {
            repository.toggleCourseStatus(course.id, next)
                .onSuccess {
                    _snackbarMessage.value = if (next == "published") "Course published to Students." else "Course moved to Drafts."
                }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to update course status." }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
                .onSuccess { _snackbarMessage.value = "Course deleted." }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to delete course." }
        }
    }

    // --- QUIZ, RANDOMIZED OPTIONS (A/B/C/D) & RETEST ENGINE ---

    private var activeShuffleEnabled: Boolean = true
    private var activeExamSecondsPerQuestion: Int = 60

    fun startQuizSession(
        mode: StudyMode,
        subjectFilter: String? = null,
        maxQuestions: Int = 20,
        shuffleOptions: Boolean = true,
        examSecondsPerQuestion: Int = 60
    ) {
        activeShuffleEnabled = shuffleOptions
        activeExamSecondsPerQuestion = examSecondsPerQuestion.coerceIn(30, 180)
        val onlinePublished = (publishedQuestionsState.value as? UiState.Success)?.data
            ?.filter { MedPrepConstants.isCoreSubject(it.subject) }.orEmpty()
        val allPublished = onlinePublished.ifEmpty {
            _offlineStudyCache.value?.questions?.filter { MedPrepConstants.isCoreSubject(it.subject) }.orEmpty()
        }
        val attempts = attemptsMapState.value

        val filtered = when (mode) {
            StudyMode.MISTAKES -> allPublished.filter { q ->
                val a = attempts[q.id]
                a != null && a.answered && !a.correct
            }
            StudyMode.BOOKMARKS -> allPublished.filter { q ->
                attempts[q.id]?.bookmarked == true
            }
            StudyMode.PRACTICE, StudyMode.EXAM -> {
                if (subjectFilter.isNullOrBlank() || subjectFilter == "All Subjects") {
                    allPublished
                } else {
                    allPublished.filter { it.subject.equals(subjectFilter, ignoreCase = true) }
                }
            }
        }

        if (filtered.isEmpty()) {
            _snackbarMessage.value = when (mode) {
                StudyMode.MISTAKES -> "No mistakes to review yet — great work!"
                StudyMode.BOOKMARKS -> "No bookmarked questions yet. Tap the bookmark icon during practice."
                else -> "No published MCQs available in this subject yet."
            }
            return
        }

        val shuffledQuestions = filtered
            .shuffled()
            .take(maxQuestions)
            .map { ShuffledQuestion.from(it, shuffleOptions = shuffleOptions) }

        val timeLimit = if (mode == StudyMode.EXAM) shuffledQuestions.size * activeExamSecondsPerQuestion.toLong() else null
        _activeQuiz.value = ActiveQuizState(
            mode = mode,
            subjectFilter = subjectFilter ?: "All Subjects",
            questions = shuffledQuestions,
            timeLimitSeconds = timeLimit,
            attemptNumber = 1
        )
        startTimer()
    }

    fun startCustomQuestionsQuiz(
        title: String,
        questions: List<McqQuestion>,
        mode: StudyMode = StudyMode.PRACTICE,
        shuffleOptions: Boolean = true,
        examSecondsPerQuestion: Int = 60
    ) {
        if (questions.isEmpty()) {
            _snackbarMessage.value = "No MCQs available to start '$title'."
            return
        }
        activeShuffleEnabled = shuffleOptions
        activeExamSecondsPerQuestion = examSecondsPerQuestion.coerceIn(30, 180)
        val prepared = questions.shuffled().map { ShuffledQuestion.from(it, shuffleOptions = shuffleOptions) }
        val timeLimit = if (mode == StudyMode.EXAM) prepared.size * activeExamSecondsPerQuestion.toLong() else null
        _activeQuiz.value = ActiveQuizState(
            mode = mode,
            subjectFilter = title.take(80),
            questions = prepared,
            timeLimitSeconds = timeLimit,
            attemptNumber = 1
        )
        startTimer()
    }

    fun startLinkedMcqsForPost(post: StudyPost, maxQuestions: Int = 50) {
        val onlinePublished = (publishedQuestionsState.value as? UiState.Success)?.data
            ?.filter { MedPrepConstants.isCoreSubject(it.subject) }.orEmpty()
        val allPublished = onlinePublished.ifEmpty {
            _offlineStudyCache.value?.questions?.filter { MedPrepConstants.isCoreSubject(it.subject) }.orEmpty()
        }
        val linked = com.example.data.TopicLinkResolver.findLinkedQuestions(post, allPublished)
        if (linked.isEmpty()) {
            _snackbarMessage.value = "No published MCQs are linked to '${post.topic.ifBlank { post.title }}' yet."
            return
        }

        val shuffledQuestions = linked
            .shuffled()
            .take(maxQuestions.coerceAtLeast(1))
            .map { ShuffledQuestion.from(it, shuffleOptions = activeShuffleEnabled) }

        val label = if (post.topic.isNotBlank()) "${post.subject} • ${post.topic}" else post.title
        _activeQuiz.value = ActiveQuizState(
            mode = StudyMode.PRACTICE,
            subjectFilter = label.take(80),
            questions = shuffledQuestions,
            timeLimitSeconds = null,
            attemptNumber = 1
        )
        startTimer()
    }

    fun retestCurrentSession() {
        val current = _activeQuiz.value ?: return
        if (current.questions.isEmpty()) return

        // Re-shuffle BOTH the same MCQs order AND every MCQ's A/B/C/D option slots
        val reshuffled = current.questions
            .map { it.question }
            .shuffled()
            .map { ShuffledQuestion.from(it, shuffleOptions = activeShuffleEnabled) }

        val timeLimit = if (current.mode == StudyMode.EXAM) reshuffled.size * activeExamSecondsPerQuestion.toLong() else null
        _activeQuiz.value = ActiveQuizState(
            mode = current.mode,
            subjectFilter = current.subjectFilter,
            questions = reshuffled,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            elapsedSeconds = 0L,
            timeLimitSeconds = timeLimit,
            completed = false,
            reviewingSummary = false,
            attemptNumber = current.attemptNumber + 1
        )
        startTimer()
        _snackbarMessage.value = "Retest #${current.attemptNumber + 1}: Questions & A/B/C/D options shuffled!"
    }

    fun selectAnswer(shuffledQuestion: ShuffledQuestion, displayLetter: String) {
        val state = _activeQuiz.value ?: return
        if (state.completed) return
        val question = shuffledQuestion.question
        if (state.mode != StudyMode.EXAM && state.selectedAnswers.containsKey(question.id)) return

        _activeQuiz.update { current ->
            current?.copy(selectedAnswers = current.selectedAnswers + (question.id to displayLetter))
        }

        if (state.mode != StudyMode.EXAM) {
            val originalPicked = shuffledQuestion.originalLetterForDisplay(displayLetter)
            val existingAttempt = attemptsMapState.value[question.id]
            viewModelScope.launch {
                repository.recordAnswer(question, originalPicked, existingAttempt)
            }
        }
    }

    fun toggleBookmark(question: McqQuestion) {
        val existingAttempt = attemptsMapState.value[question.id]
        viewModelScope.launch {
            repository.toggleBookmark(question, existingAttempt)
                .onSuccess { bookmarked ->
                    _snackbarMessage.value = if (bookmarked) "Saved to Bookmarks" else "Removed from Bookmarks"
                }
        }
    }

    fun navigateQuizTo(index: Int) {
        _activeQuiz.update { state ->
            if (state == null) null
            else state.copy(currentIndex = index.coerceIn(0, (state.questions.size - 1).coerceAtLeast(0)))
        }
    }

    fun nextQuestion() {
        val state = _activeQuiz.value ?: return
        if (state.currentIndex + 1 < state.questions.size) {
            navigateQuizTo(state.currentIndex + 1)
        } else {
            finishQuizSession()
        }
    }

    fun previousQuestion() {
        val state = _activeQuiz.value ?: return
        if (state.currentIndex > 0) {
            navigateQuizTo(state.currentIndex - 1)
        }
    }

    fun finishQuizSession() {
        val state = _activeQuiz.value ?: return
        if (state.completed) return
        stopTimer()

        _activeQuiz.update { it?.copy(completed = true, reviewingSummary = false) }

        viewModelScope.launch {
            if (state.mode == StudyMode.EXAM) {
                val attempts = attemptsMapState.value
                state.questions.forEach { sq ->
                    val pickedDisplay = state.selectedAnswers[sq.question.id]
                    if (pickedDisplay != null) {
                        val origLetter = sq.originalLetterForDisplay(pickedDisplay)
                        repository.recordAnswer(sq.question, origLetter, attempts[sq.question.id])
                    }
                }
            }
            if (state.answeredCount > 0) {
                repository.recordExamSession(
                    mode = state.mode,
                    subjectFilter = state.subjectFilter,
                    totalQuestions = state.questions.size,
                    correctCount = state.correctCount,
                    durationSeconds = state.elapsedSeconds
                )
            }
        }
    }

    fun enterSummaryReview() {
        _activeQuiz.update { it?.copy(reviewingSummary = true, currentIndex = 0) }
    }

    fun exitQuizSession() {
        stopTimer()
        _activeQuiz.value = null
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _activeQuiz.value
                if (current == null || current.completed) break
                val nextElapsed = current.elapsedSeconds + 1L
                _activeQuiz.value = current.copy(elapsedSeconds = nextElapsed)
                val limit = current.timeLimitSeconds
                if (limit != null && nextElapsed >= limit) {
                    finishQuizSession()
                    break
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    private fun <T> Flow<T>.toUiStateFlow(
        label: String,
        fallbackError: String
    ): StateFlow<UiState<T>> = this
        .map<T, UiState<T>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error in $label", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "MedPrepVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
