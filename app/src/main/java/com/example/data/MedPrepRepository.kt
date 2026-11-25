package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class MedPrepRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    companion object {
        private const val TAG = "MedPrepRepo"
        private const val KEY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val secureRandom = SecureRandom()

        @Volatile
        private var cachedFirestore: FirebaseFirestore? = null

        fun createConfiguredFirestore(context: Context): FirebaseFirestore {
            cachedFirestore?.let { return it }
            synchronized(this) {
                cachedFirestore?.let { return it }
                val databaseId = context.getString(R.string.firestore_database_id)
                val instance = FirebaseFirestore.getInstance(databaseId)
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                            PersistentCacheSettings.newBuilder()
                                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                .build()
                        )
                        .build()
                    instance.firestoreSettings = settings
                } catch (e: IllegalStateException) {
                    Log.d(TAG, "Firestore settings already configured: ${e.message}")
                }
                cachedFirestore = instance
                return instance
            }
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isBootstrapAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(MedPrepConstants.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) &&
            user.isEmailVerified
    }

    suspend fun checkIsAdmin(userId: String): Boolean {
        if (isBootstrapAdmin()) return true
        val path = "admins/$userId"
        return try {
            val doc = db.collection("admins").document(userId).get().await()
            doc.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    // --- REALTIME OBSERVATION FLOWS ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observePublishedQuestions(userId: String): Flow<List<McqQuestion>> = flow {
        val path = "questions"
        emitAll(
            db.collection("questions")
                .whereEqualTo("status", "published")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        McqQuestion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { MedPrepConstants.isCoreSubject(it.subject) }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeStudioQuestions(userId: String, bootstrapAdmin: Boolean): Flow<List<McqQuestion>> = flow {
        val path = "questions"
        val query = if (bootstrapAdmin) {
            db.collection("questions")
        } else {
            db.collection("questions").whereEqualTo("authorId", userId)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        McqQuestion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observePublishedPosts(userId: String): Flow<List<StudyPost>> = flow {
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("status", "published")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { MedPrepConstants.isCoreSubject(it.subject) }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeAdminPosts(userId: String, bootstrapAdmin: Boolean): Flow<List<StudyPost>> = flow {
        val path = "posts"
        val query = if (bootstrapAdmin) {
            db.collection("posts")
        } else {
            db.collection("posts").whereEqualTo("authorId", userId)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observePublishedCourses(userId: String): Flow<List<CourseItem>> = flow {
        val path = "courses"
        emitAll(
            db.collection("courses")
                .whereEqualTo("status", "published")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CourseItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeAdminCourses(userId: String, bootstrapAdmin: Boolean): Flow<List<CourseItem>> = flow {
        val path = "courses"
        val query = if (bootstrapAdmin) {
            db.collection("courses")
        } else {
            db.collection("courses").whereEqualTo("authorId", userId)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CourseItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeUserAccessKeys(userId: String): Flow<List<AccessKey>> = flow {
        val path = "access_keys"
        emitAll(
            db.collection("access_keys")
                .whereEqualTo("assignedUserId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AccessKey::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.activatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeAllAccessKeys(userId: String): Flow<List<AccessKey>> = flow {
        val path = "access_keys"
        emitAll(
            db.collection("access_keys")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AccessKey::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeUserSubscriptionRequests(userId: String): Flow<List<SubscriptionRequest>> = flow {
        val path = "subscription_requests"
        emitAll(
            db.collection("subscription_requests")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SubscriptionRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeAllSubscriptionRequests(userId: String): Flow<List<SubscriptionRequest>> = flow {
        val path = "subscription_requests"
        emitAll(
            db.collection("subscription_requests")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SubscriptionRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeAllUserProfiles(userId: String): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeAttempts(userId: String): Flow<List<QuestionAttempt>> = flow {
        val path = "users/$userId/attempts"
        emitAll(
            db.collection("users").document(userId).collection("attempts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QuestionAttempt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeExamSessions(userId: String): Flow<List<ExamSession>> = flow {
        val path = "users/$userId/exam_sessions"
        emitAll(
            db.collection("users").document(userId).collection("exam_sessions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ExamSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeStudyNotes(userId: String): Flow<List<StudyNote>> = flow {
        val path = "users/$userId/notes"
        emitAll(
            db.collection("users").document(userId).collection("notes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    fun observeDifficultQuestions(userId: String): Flow<List<DifficultQuestionItem>> = flow {
        val path = "users/$userId/difficult_questions"
        emitAll(
            db.collection("users").document(userId).collection("difficult_questions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DifficultQuestionItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }.flowOn(Dispatchers.IO)

    private fun handleStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- USER PROFILE & OFFLINE SYNC ---

    suspend fun ensureUserProfile(): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("User must be signed in with Google.")
            val uid = user.uid
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path

            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val displayName = user.displayName?.trim()?.take(80)
                    ?.takeIf { it.isNotEmpty() } ?: "Medical Student"
                val email = user.email?.trim()?.take(160)?.takeIf { it.isNotEmpty() }
                val payload = mapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to displayName,
                    "email" to email,
                    "subscriptionPlan" to "free",
                    "targetExam" to MedPrepConstants.EXAM_TRACKS.first(),
                    "dailyGoal" to 25L,
                    "totalAnswered" to 0L,
                    "totalCorrect" to 0L,
                    "lastSyncedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        displayName: String,
        targetExam: String,
        dailyGoal: Long,
        contactEmail: String? = null,
        phoneNumber: String? = null,
        scheduleDays: Long? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(80)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
            val cleanExam = targetExam.trim().take(60)
            require(cleanExam.isNotEmpty()) { "Target exam cannot be empty." }
            val clampedGoal = dailyGoal.coerceIn(5L, 200L)

            val payload = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "targetExam" to cleanExam,
                "dailyGoal" to clampedGoal,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (contactEmail != null) {
                payload["contactEmail"] = contactEmail.trim().take(160)
            }
            if (phoneNumber != null) {
                payload["phoneNumber"] = phoneNumber.trim().take(30)
            }
            if (scheduleDays != null) {
                payload["scheduleDays"] = scheduleDays.coerceIn(1L, 365L)
            }

            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- JAZZCASH SUBSCRIPTION REQUESTS & ADMIN PLAN ACCESS CONTROL ---

    suspend fun submitSubscriptionRequest(input: SubscriptionRequestInput): Result<String> {
        var docPath = "subscription_requests"
        return try {
            val uid = requireUserId()
            require(input.requestedPlan == SubscriptionPlanTier.BASIC || input.requestedPlan == SubscriptionPlanTier.ADVANCE) {
                "Select Basic (PKR 499) or Advance (PKR 999)."
            }
            val cleanRef = input.transactionRef.trim().take(80)
            require(cleanRef.length >= 4) {
                "Enter your JazzCash Transaction ID (at least 4 characters)."
            }
            val cleanPhone = input.senderPhone.trim().take(30).takeIf { it.isNotEmpty() }
            val cleanName = input.userName.trim().take(100).ifBlank { "Medical Student" }
            val cleanEmail = input.userEmail.trim().take(160)

            val docRef = db.collection("subscription_requests").document()
            docPath = docRef.path
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "userName" to cleanName,
                "userEmail" to cleanEmail,
                "requestedPlan" to input.requestedPlan.key,
                "amountPkr" to input.requestedPlan.amountPkr.toLong(),
                "paymentMethod" to MedPrepConstants.PAYMENT_METHOD_NAME,
                "recipientIban" to MedPrepConstants.JAZZCASH_IBAN,
                "transactionRef" to cleanRef,
                "senderPhone" to cleanPhone,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminSetUserSubscriptionPlan(targetUserId: String, planKey: String): Result<Unit> {
        var docPath = "users/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "Target user ID is required." }
            val safePlan = planKey.trim().lowercase().takeIf { it in setOf("free", "basic", "advance") }
                ?: throw IllegalArgumentException("Plan must be free, basic, or advance.")
            val docRef = db.collection("users").document(targetUserId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "subscriptionPlan" to safePlan,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminReviewSubscriptionRequest(
        request: SubscriptionRequest,
        approve: Boolean,
        adminNote: String = ""
    ): Result<Unit> {
        var docPath = "subscription_requests/${request.id}"
        return try {
            val adminUid = requireUserId()
            require(request.id.isNotBlank()) { "Request ID is required." }
            require(request.userId.isNotBlank()) { "Student user ID is required." }

            val newStatus = if (approve) "approved" else "rejected"
            val cleanNote = adminNote.trim().take(240)
            val batch = db.batch()
            val reqRef = db.collection("subscription_requests").document(request.id)
            batch.update(
                reqRef,
                mapOf(
                    "status" to newStatus,
                    "adminNote" to cleanNote,
                    "reviewedBy" to adminUid,
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (approve) {
                val safePlan = request.requestedPlan.trim().lowercase()
                    .takeIf { it in setOf("basic", "advance") } ?: "basic"
                val userRef = db.collection("users").document(request.userId)
                batch.update(
                    userRef,
                    mapOf(
                        "subscriptionPlan" to safePlan,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSubscriptionRequest(requestId: String): Result<Unit> {
        var docPath = "subscription_requests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank()) { "Request ID is required." }
            val docRef = db.collection("subscription_requests").document(requestId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateScheduleDays(days: Long): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val clamped = days.coerceIn(1L, 365L)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "scheduleDays" to clamped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- STUDENT HIGHLIGHTS & NOTES ---

    suspend fun saveStudyNote(draft: StudyNoteDraft): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notes"
        return try {
            val uid = requireUserId()
            val title = draft.title.trim().take(160)
            require(title.isNotEmpty()) { "Note title cannot be empty." }
            val content = FormulaTextPreserver.preserveExactStructure(draft.content, 4000)
            require(content.isNotBlank()) { "Note content cannot be empty." }
            val subject = CsvMcqParser.canonicalSubject(draft.subject)
            val color = draft.highlightColor.trim().lowercase()
                .takeIf { it in setOf("teal", "amber", "rose", "indigo", "emerald") } ?: "teal"
            val qId = draft.questionId?.trim()?.take(128)?.takeIf { it.isNotEmpty() }

            val colRef = db.collection("users").document(uid).collection("notes")
            if (draft.existingId.isNullOrBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                val payload = mapOf<String, Any?>(
                    "title" to title,
                    "content" to content,
                    "subject" to subject,
                    "questionId" to qId,
                    "highlightColor" to color,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = colRef.document(draft.existingId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "title" to title,
                        "content" to content,
                        "subject" to subject,
                        "questionId" to qId.orEmpty(),
                        "highlightColor" to color,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(docRef.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteStudyNote(noteId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notes/$noteId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- STUDENT DIFFICULT QUESTIONS ---

    suspend fun saveDifficultQuestion(draft: DifficultQuestionDraft): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/difficult_questions"
        return try {
            val uid = requireUserId()
            val subject = CsvMcqParser.canonicalSubject(draft.subject)
            val topic = draft.topic.trim().take(120)
            val stem = FormulaTextPreserver.preserveExactStructure(draft.stem, 2000)
            require(stem.trim().length >= 5) { "Question stem must be at least 5 characters." }
            val optionA = FormulaTextPreserver.preserveExactStructure(draft.optionA, 1000)
            val optionB = FormulaTextPreserver.preserveExactStructure(draft.optionB, 1000)
            val optionC = FormulaTextPreserver.preserveExactStructure(draft.optionC, 1000)
            val optionD = FormulaTextPreserver.preserveExactStructure(draft.optionD, 1000)
            require(optionA.isNotBlank() && optionB.isNotBlank() && optionC.isNotBlank() && optionD.isNotBlank()) {
                "All four options A–D are required."
            }
            val correct = draft.correctOption.trim().uppercase()
            require(correct in setOf("A", "B", "C", "D")) { "Correct option must be A, B, C, or D." }
            val explanation = FormulaTextPreserver.preserveExactStructure(draft.explanation, 3000)
            val srcId = draft.sourceQuestionId?.trim()?.take(128)?.takeIf { it.isNotEmpty() }

            val colRef = db.collection("users").document(uid).collection("difficult_questions")
            val docRef = if (!draft.existingId.isNullOrBlank()) {
                colRef.document(draft.existingId)
            } else if (srcId != null) {
                val safeSrcId = srcId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(110)
                colRef.document("src_$safeSrcId")
            } else {
                colRef.document()
            }
            docPath = docRef.path

            val payload = mapOf<String, Any?>(
                "sourceQuestionId" to srcId,
                "subject" to subject,
                "topic" to topic,
                "stem" to stem,
                "optionA" to optionA,
                "optionB" to optionB,
                "optionC" to optionC,
                "optionD" to optionD,
                "correctOption" to correct,
                "explanation" to explanation,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDifficultQuestion(itemId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/difficult_questions/$itemId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("difficult_questions").document(itemId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncOfflineNow(): Result<SyncReport> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val publishedSnap = db.collection("questions")
                .whereEqualTo("status", "published")
                .get(Source.SERVER)
                .await()

            db.collection("posts")
                .whereEqualTo("status", "published")
                .get(Source.SERVER)
                .await()

            db.collection("courses")
                .whereEqualTo("status", "published")
                .get(Source.SERVER)
                .await()

            db.collection("users").document(uid).collection("attempts")
                .get(Source.SERVER)
                .await()

            val userRef = db.collection("users").document(uid)
            if (userRef.get(Source.SERVER).await().exists()) {
                userRef.update(
                    mapOf(
                        "lastSyncedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val count = publishedSnap.toObjects(McqQuestion::class.java)
                .count { MedPrepConstants.isCoreSubject(it.subject) }
            Result.success(
                SyncReport(
                    syncedQuestionsCount = count,
                    fromServer = true,
                    message = "Synced $count published MCQs & posts for offline study."
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            try {
                val cachedSnap = db.collection("questions")
                    .whereEqualTo("status", "published")
                    .get(Source.CACHE)
                    .await()
                val count = cachedSnap.toObjects(McqQuestion::class.java)
                    .count { MedPrepConstants.isCoreSubject(it.subject) }
                Result.success(
                    SyncReport(
                        syncedQuestionsCount = count,
                        fromServer = false,
                        message = "Offline mode active: $count cached MCQs ready."
                    )
                )
            } catch (cacheErr: Exception) {
                handleFirestoreError(e, OperationType.LIST, "questions")
                Result.failure(e)
            }
        }
    }

    // --- ADMIN MCQ CRUD, LEGACY CLEANUP & BULK IMPORT ---

    suspend fun saveQuestion(input: QuestionDraftInput): Result<String> {
        var docPath = "questions"
        return try {
            val uid = requireUserId()
            val sanitized = validateAndSanitizeQuestion(input)

            if (input.existingId.isNullOrBlank()) {
                val docRef = db.collection("questions").document()
                docPath = docRef.path
                val payload = mapOf<String, Any?>(
                    "authorId" to uid,
                    "subject" to sanitized.subject,
                    "topic" to sanitized.topic,
                    "stem" to sanitized.stem,
                    "optionA" to sanitized.optionA,
                    "optionB" to sanitized.optionB,
                    "optionC" to sanitized.optionC,
                    "optionD" to sanitized.optionD,
                    "correctOption" to sanitized.correctOption,
                    "explanation" to sanitized.explanation,
                    "youtubeUrl" to sanitized.youtubeUrl,
                    "linkedPostId" to sanitized.linkedPostId,
                    "difficulty" to sanitized.difficulty,
                    "status" to sanitized.status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = db.collection("questions").document(input.existingId)
                docPath = docRef.path
                val payload = mapOf<String, Any>(
                    "subject" to sanitized.subject,
                    "topic" to sanitized.topic,
                    "stem" to sanitized.stem,
                    "optionA" to sanitized.optionA,
                    "optionB" to sanitized.optionB,
                    "optionC" to sanitized.optionC,
                    "optionD" to sanitized.optionD,
                    "correctOption" to sanitized.correctOption,
                    "explanation" to sanitized.explanation,
                    "youtubeUrl" to sanitized.youtubeUrl.orEmpty(),
                    "linkedPostId" to sanitized.linkedPostId.orEmpty(),
                    "difficulty" to sanitized.difficulty,
                    "status" to sanitized.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
                Result.success(docRef.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (input.existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun toggleQuestionStatus(questionId: String, newStatus: String): Result<Unit> {
        var docPath = "questions/$questionId"
        return try {
            requireUserId()
            require(questionId.isNotBlank()) { "Question ID is required." }
            require(newStatus in setOf("draft", "published")) { "Status must be draft or published." }
            val docRef = db.collection("questions").document(questionId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun publishAllDrafts(drafts: List<McqQuestion>): Result<Int> {
        val path = "questions"
        return try {
            requireUserId()
            val pending = drafts.filter { it.status == "draft" && it.id.isNotBlank() }
            if (pending.isEmpty()) return Result.success(0)

            var updated = 0
            pending.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { q ->
                    val docRef = db.collection("questions").document(q.id)
                    batch.update(
                        docRef,
                        mapOf(
                            "status" to "published",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
                updated += chunk.size
            }
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNonCoreSubjectQuestions(questions: List<McqQuestion>): Result<Int> {
        val path = "questions"
        return try {
            requireUserId()
            val legacy = questions.filter { !MedPrepConstants.isCoreSubject(it.subject) && it.id.isNotBlank() }
            if (legacy.isEmpty()) return Result.success(0)

            var deleted = 0
            legacy.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { q ->
                    batch.delete(db.collection("questions").document(q.id))
                }
                batch.commit().await()
                deleted += chunk.size
            }
            Result.success(deleted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteQuestion(questionId: String): Result<Unit> {
        var docPath = "questions/$questionId"
        return try {
            requireUserId()
            require(questionId.isNotBlank()) { "Question ID is required." }
            val docRef = db.collection("questions").document(questionId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun importQuestionsBatch(questions: List<QuestionDraftInput>): Result<Int> {
        val path = "questions"
        return try {
            val uid = requireUserId()
            require(questions.isNotEmpty()) { "No valid questions to import." }

            var totalImported = 0
            questions.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { raw ->
                    val q = validateAndSanitizeQuestion(raw)
                    val docRef = db.collection("questions").document()
                    val payload = mapOf<String, Any?>(
                        "authorId" to uid,
                        "subject" to q.subject,
                        "topic" to q.topic,
                        "stem" to q.stem,
                        "optionA" to q.optionA,
                        "optionB" to q.optionB,
                        "optionC" to q.optionC,
                        "optionD" to q.optionD,
                        "correctOption" to q.correctOption,
                        "explanation" to q.explanation,
                        "youtubeUrl" to q.youtubeUrl,
                        "linkedPostId" to q.linkedPostId,
                        "difficulty" to q.difficulty,
                        "status" to q.status,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    batch.set(docRef, payload)
                }
                batch.commit().await()
                totalImported += chunk.size
            }
            Result.success(totalImported)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN STUDY POSTS CRUD (WEBPAGE LINKS + YOUTUBE SUGGESTED VIDEOS) ---

    suspend fun savePost(input: StudyPostDraftInput): Result<String> {
        var docPath = "posts"
        return try {
            val uid = requireUserId()
            val title = input.title.trim().take(180)
            require(title.length >= 3) { "Post title must be at least 3 characters." }
            val body = FormulaTextPreserver.preserveExactStructure(input.body, 3000)
            val subject = CsvMcqParser.canonicalSubject(input.subject)
            require(MedPrepConstants.isCoreSubject(subject)) { "Select one of the 5 core subjects." }
            val topic = input.topic.trim().take(120)
            val webUrl = validateOptionalUrlOrThrow(input.webUrl, "Web page link")
            val youtubeUrl = validateOptionalUrlOrThrow(input.youtubeUrl, "YouTube link")
            val rawHtml = input.htmlContent?.takeIf { it.isNotBlank() }
            if (rawHtml != null && rawHtml.length > MedPrepConstants.MAX_HTML_CONTENT_CHARS) {
                throw IllegalArgumentException("Interactive HTML file exceeds the 200 KB size limit.")
            }
            val htmlFileName = input.htmlFileName?.trim()?.take(160)?.takeIf { it.isNotEmpty() }
            val courseId = input.courseId?.trim()?.take(128)?.takeIf { it.isNotEmpty() }
            val status = input.status.trim().lowercase().takeIf { it in setOf("draft", "published") } ?: "published"

            if (input.existingId.isNullOrBlank()) {
                val docRef = db.collection("posts").document()
                docPath = docRef.path
                val payload = mapOf<String, Any?>(
                    "authorId" to uid,
                    "title" to title,
                    "body" to body,
                    "subject" to subject,
                    "topic" to topic,
                    "webUrl" to webUrl,
                    "youtubeUrl" to youtubeUrl,
                    "htmlContent" to rawHtml,
                    "htmlFileName" to htmlFileName,
                    "courseId" to courseId,
                    "status" to status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = db.collection("posts").document(input.existingId)
                docPath = docRef.path
                val payload = mapOf<String, Any>(
                    "title" to title,
                    "body" to body,
                    "subject" to subject,
                    "topic" to topic,
                    "webUrl" to webUrl.orEmpty(),
                    "youtubeUrl" to youtubeUrl.orEmpty(),
                    "htmlContent" to rawHtml.orEmpty(),
                    "htmlFileName" to htmlFileName.orEmpty(),
                    "courseId" to courseId.orEmpty(),
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
                Result.success(docRef.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (input.existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun togglePostStatus(postId: String, newStatus: String): Result<Unit> {
        var docPath = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank()) { "Post ID is required." }
            require(newStatus in setOf("draft", "published")) { "Invalid post status." }
            val docRef = db.collection("posts").document(postId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        var docPath = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank()) { "Post ID is required." }
            val docRef = db.collection("posts").document(postId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- ADMIN COURSES CRUD ---

    suspend fun saveCourse(input: CourseDraftInput): Result<String> {
        var docPath = "courses"
        return try {
            val uid = requireUserId()
            val title = input.title.trim().take(160)
            require(title.length >= 3) { "Course title must be at least 3 characters." }
            val description = FormulaTextPreserver.preserveExactStructure(input.description, 3000)
            val cleanSubject = if (input.subject.equals("All Subjects", ignoreCase = true)) {
                "All Subjects"
            } else {
                CsvMcqParser.canonicalSubject(input.subject)
            }
            val instructor = input.instructor.trim().take(100).ifBlank { "MedPrep Faculty" }
            val durationLabel = input.durationLabel.trim().take(60).ifBlank { "4 Weeks" }
            val level = input.level.trim().lowercase()
                .takeIf { it in setOf("beginner", "intermediate", "advanced") } ?: "intermediate"
            val syllabus = FormulaTextPreserver.preserveExactStructure(input.syllabusOutline, 4000)
            val youtubeUrl = validateOptionalUrlOrThrow(input.youtubeUrl, "Course YouTube link")
            val webUrl = validateOptionalUrlOrThrow(input.webUrl, "Course web link")
            val status = input.status.trim().lowercase().takeIf { it in setOf("draft", "published") } ?: "published"

            if (input.existingId.isNullOrBlank()) {
                val docRef = db.collection("courses").document()
                docPath = docRef.path
                val payload = mapOf<String, Any?>(
                    "authorId" to uid,
                    "title" to title,
                    "description" to description,
                    "subject" to cleanSubject,
                    "instructor" to instructor,
                    "durationLabel" to durationLabel,
                    "level" to level,
                    "syllabusOutline" to syllabus,
                    "youtubeUrl" to youtubeUrl,
                    "webUrl" to webUrl,
                    "status" to status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = db.collection("courses").document(input.existingId)
                docPath = docRef.path
                val payload = mapOf<String, Any>(
                    "title" to title,
                    "description" to description,
                    "subject" to cleanSubject,
                    "instructor" to instructor,
                    "durationLabel" to durationLabel,
                    "level" to level,
                    "syllabusOutline" to syllabus,
                    "youtubeUrl" to youtubeUrl.orEmpty(),
                    "webUrl" to webUrl.orEmpty(),
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
                Result.success(docRef.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (input.existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun toggleCourseStatus(courseId: String, newStatus: String): Result<Unit> {
        var docPath = "courses/$courseId"
        return try {
            requireUserId()
            require(courseId.isNotBlank()) { "Course ID is required." }
            require(newStatus in setOf("draft", "published")) { "Invalid course status." }
            val docRef = db.collection("courses").document(courseId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(courseId: String): Result<Unit> {
        var docPath = "courses/$courseId"
        return try {
            requireUserId()
            require(courseId.isNotBlank()) { "Course ID is required." }
            val docRef = db.collection("courses").document(courseId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- SPECIAL ACCESS KEYS (1-USER 1-DEVICE LOCK) ---

    suspend fun generateAccessKey(label: String, customKeyCode: String? = null): Result<String> {
        var docPath = "access_keys"
        return try {
            val uid = requireUserId()
            val cleanedCustom = customKeyCode?.trim()?.uppercase().orEmpty()
            val keyCode = if (cleanedCustom.isNotEmpty()) {
                require(cleanedCustom.length in 6..40 && cleanedCustom.matches(Regex("^[A-Z0-9_\\-]+$"))) {
                    "Specific key must be 6–40 characters using only A–Z, 0–9, '-' or '_'."
                }
                cleanedCustom
            } else {
                buildRandomKeyCode()
            }
            val cleanLabel = label.trim().take(100)
            val docRef = db.collection("access_keys").document(keyCode)
            docPath = docRef.path

            if (cleanedCustom.isNotEmpty()) {
                val existing = docRef.get(Source.SERVER).await()
                if (existing.exists()) {
                    return Result.failure(
                        IllegalArgumentException("Access Key '$keyCode' already exists. Enter a different specific key.")
                    )
                }
            }

            docRef.set(
                mapOf(
                    "keyCode" to keyCode,
                    "label" to cleanLabel,
                    "status" to "active",
                    "assignedUserId" to "",
                    "assignedEmail" to "",
                    "boundDeviceId" to "",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(keyCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun activateAccessKey(rawKeyCode: String, deviceId: String): Result<KeyActivationOutcome> {
        val keyCode = rawKeyCode.trim().uppercase()
        var docPath = "access_keys/$keyCode"
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("Please sign in with Google first.")
            val uid = user.uid
            require(keyCode.length in 6..40 && keyCode.matches(Regex("^[A-Z0-9_\\-]+$"))) {
                "Enter a valid Access Key (e.g. MED-XXXX-XXXX)."
            }
            require(deviceId.length >= 16) { "Device fingerprint could not be verified." }

            val docRef = db.collection("access_keys").document(keyCode)
            docPath = docRef.path
            val snapshot = docRef.get(Source.SERVER).await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalArgumentException("Invalid Access Key. Please check the key from Admin."))
            }

            val existingKey = snapshot.toObject(
                AccessKey::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Unable to read Access Key."))

            if (existingKey.status != "active") {
                return Result.failure(IllegalStateException("This Access Key has been revoked by Admin."))
            }

            val safeEmail = user.email?.trim()?.lowercase()?.take(160).orEmpty()
            val isUnclaimed = existingKey.assignedUserId.isBlank() && existingKey.boundDeviceId.isBlank()
            if (isUnclaimed) {
                docRef.update(
                    mapOf(
                        "assignedUserId" to uid,
                        "assignedEmail" to safeEmail,
                        "boundDeviceId" to deviceId,
                        "activatedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val claimed = existingKey.copy(
                    assignedUserId = uid,
                    assignedEmail = safeEmail,
                    boundDeviceId = deviceId
                )
                return Result.success(KeyActivationOutcome(key = claimed, transferredFromOldDevice = false))
            }

            if (existingKey.assignedUserId == uid && existingKey.boundDeviceId == deviceId) {
                return Result.success(KeyActivationOutcome(key = existingKey, transferredFromOldDevice = false))
            }

            // Automatic device transfer: if verified Email + Access Key match the key owner,
            // update boundDeviceId to this new device (the old device automatically logs out via live listener)
            val keyEmail = existingKey.assignedEmail.trim().lowercase()
            if (existingKey.assignedUserId == uid &&
                user.isEmailVerified &&
                safeEmail.isNotBlank() &&
                keyEmail.isNotBlank() &&
                safeEmail == keyEmail
            ) {
                docRef.update(
                    mapOf(
                        "boundDeviceId" to deviceId,
                        "activatedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val transferred = existingKey.copy(boundDeviceId = deviceId)
                return Result.success(KeyActivationOutcome(key = transferred, transferredFromOldDevice = true))
            }

            if (existingKey.assignedUserId == uid && existingKey.boundDeviceId != deviceId) {
                return Result.failure(
                    IllegalStateException("Your verified email could not be matched to this key. Contact Support (${MedPrepConstants.SUPPORT_EMAIL}) to reset your device lock.")
                )
            }

            Result.failure(
                IllegalStateException("This Access Key is registered to a different email account. Sign in with the matching email to transfer it here.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun resetAccessKeyDevice(keyCode: String): Result<Unit> {
        var docPath = "access_keys/$keyCode"
        return try {
            requireUserId()
            val docRef = db.collection("access_keys").document(keyCode)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "assignedUserId" to "",
                    "assignedEmail" to "",
                    "boundDeviceId" to "",
                    "activatedAt" to null,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleAccessKeyStatus(keyCode: String, newStatus: String): Result<Unit> {
        var docPath = "access_keys/$keyCode"
        return try {
            requireUserId()
            require(newStatus in setOf("active", "revoked")) { "Invalid key status." }
            val docRef = db.collection("access_keys").document(keyCode)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAccessKey(keyCode: String): Result<Unit> {
        var docPath = "access_keys/$keyCode"
        return try {
            requireUserId()
            val docRef = db.collection("access_keys").document(keyCode)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun buildRandomKeyCode(): String {
        fun chunk(len: Int): String = buildString(len) {
            repeat(len) {
                append(KEY_ALPHABET[secureRandom.nextInt(KEY_ALPHABET.length)])
            }
        }
        return "MED-${chunk(4)}-${chunk(4)}"
    }

    // --- STUDY ATTEMPTS, BOOKMARKS & EXAM SESSIONS ---

    suspend fun recordAnswer(
        question: McqQuestion,
        selectedOption: String,
        existingAttempt: QuestionAttempt?
    ): Result<Boolean> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/attempts/${question.id}"
        return try {
            val uid = requireUserId()
            require(question.id.isNotBlank()) { "Question ID is required." }
            val cleanOption = selectedOption.uppercase()
            require(cleanOption in setOf("A", "B", "C", "D")) { "Option must be A, B, C, or D." }
            val isCorrect = cleanOption == question.correctOption.uppercase()

            val attemptRef = db.collection("users").document(uid)
                .collection("attempts").document(question.id)
            docPath = attemptRef.path

            val safeSubject = question.subject.trim().ifBlank { "Biology" }.take(80)
            val newCount = ((existingAttempt?.attemptsCount ?: 0L) + 1L).coerceAtMost(10000L)

            if (existingAttempt == null) {
                attemptRef.set(
                    mapOf(
                        "questionId" to question.id,
                        "subject" to safeSubject,
                        "selectedOption" to cleanOption,
                        "correct" to isCorrect,
                        "answered" to true,
                        "bookmarked" to false,
                        "attemptsCount" to newCount,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                attemptRef.update(
                    mapOf(
                        "subject" to safeSubject,
                        "selectedOption" to cleanOption,
                        "correct" to isCorrect,
                        "answered" to true,
                        "attemptsCount" to newCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val userRef = db.collection("users").document(uid)
            val profileUpdates = mutableMapOf<String, Any>(
                "totalAnswered" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCorrect) {
                profileUpdates["totalCorrect"] = FieldValue.increment(1L)
            }
            userRef.update(profileUpdates).await()

            Result.success(isCorrect)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleBookmark(
        question: McqQuestion,
        existingAttempt: QuestionAttempt?
    ): Result<Boolean> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/attempts/${question.id}"
        return try {
            val uid = requireUserId()
            require(question.id.isNotBlank()) { "Question ID is required." }
            val attemptRef = db.collection("users").document(uid)
                .collection("attempts").document(question.id)
            docPath = attemptRef.path

            val newBookmarked = !(existingAttempt?.bookmarked ?: false)
            val safeSubject = question.subject.trim().ifBlank { "Biology" }.take(80)

            if (existingAttempt == null) {
                attemptRef.set(
                    mapOf(
                        "questionId" to question.id,
                        "subject" to safeSubject,
                        "selectedOption" to "",
                        "correct" to false,
                        "answered" to false,
                        "bookmarked" to newBookmarked,
                        "attemptsCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                attemptRef.update(
                    mapOf(
                        "bookmarked" to newBookmarked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(newBookmarked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordExamSession(
        mode: StudyMode,
        subjectFilter: String,
        totalQuestions: Int,
        correctCount: Int,
        durationSeconds: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/exam_sessions"
        return try {
            val uid = requireUserId()
            val safeTotal = totalQuestions.coerceIn(1, 500)
            val safeCorrect = correctCount.coerceIn(0, safeTotal)
            val safeDuration = durationSeconds.coerceIn(0L, 86400L)
            val safeSubject = subjectFilter.trim().ifBlank { "All Subjects" }.take(80)

            val docRef = db.collection("users").document(uid)
                .collection("exam_sessions").document()
            docPath = docRef.path

            docRef.set(
                mapOf(
                    "mode" to mode.key,
                    "subjectFilter" to safeSubject,
                    "totalQuestions" to safeTotal.toLong(),
                    "correctCount" to safeCorrect.toLong(),
                    "durationSeconds" to safeDuration,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    private fun validateOptionalUrlOrThrow(raw: String?, fieldLabel: String): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val normalized = CsvMcqParser.sanitizeOptionalHttpUrl(trimmed)
            ?: throw IllegalArgumentException("$fieldLabel must start with https:// or http://")
        return normalized
    }

    private fun validateAndSanitizeQuestion(input: QuestionDraftInput): QuestionDraftInput {
        val subject = CsvMcqParser.canonicalSubject(input.subject)
        val topic = input.topic.trim().take(120)
        val stem = FormulaTextPreserver.preserveExactStructure(input.stem, 2000)
        val optionA = FormulaTextPreserver.preserveExactStructure(input.optionA, 1000)
        val optionB = FormulaTextPreserver.preserveExactStructure(input.optionB, 1000)
        val optionC = FormulaTextPreserver.preserveExactStructure(input.optionC, 1000)
        val optionD = FormulaTextPreserver.preserveExactStructure(input.optionD, 1000)
        val correct = input.correctOption.trim().uppercase()
        val explanation = FormulaTextPreserver.preserveExactStructure(input.explanation, 3000)
        val youtubeUrl = validateOptionalUrlOrThrow(input.youtubeUrl, "YouTube link")
        val linkedPostId = input.linkedPostId?.trim()?.take(128)?.takeIf { it.isNotEmpty() }
        val difficulty = input.difficulty.trim().lowercase().takeIf { it in setOf("easy", "medium", "hard") } ?: "medium"
        val status = input.status.trim().lowercase().takeIf { it in setOf("draft", "published") } ?: "draft"

        require(MedPrepConstants.isCoreSubject(subject)) {
            "Subject must be Biology, Chemistry, Physics, English, or Logical Reasoning."
        }
        require(stem.trim().length >= 5) { "Question stem must be at least 5 characters." }
        require(optionA.isNotBlank() && optionB.isNotBlank() && optionC.isNotBlank() && optionD.isNotBlank()) {
            "All four options (A, B, C, D) must be provided."
        }
        require(correct in setOf("A", "B", "C", "D")) { "Correct option must be A, B, C, or D." }

        return input.copy(
            subject = subject,
            topic = topic,
            stem = stem,
            optionA = optionA,
            optionB = optionB,
            optionC = optionC,
            optionD = optionD,
            correctOption = correct,
            explanation = explanation,
            youtubeUrl = youtubeUrl,
            linkedPostId = linkedPostId,
            difficulty = difficulty,
            status = status
        )
    }
}
