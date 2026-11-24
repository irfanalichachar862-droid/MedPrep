package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Medical Student",
    val email: String? = null,
    val contactEmail: String? = null,
    val phoneNumber: String? = null,
    val subscriptionPlan: String = "free",
    val scheduleDays: Long = 30L,
    val targetExam: String = "MDCAT",
    val dailyGoal: Long = 25L,
    val totalAnswered: Long = 0L,
    val totalCorrect: Long = 0L,
    @ServerTimestamp val lastSyncedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyNote(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val subject: String = "Biology",
    val questionId: String? = null,
    val highlightColor: String = "teal",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DifficultQuestionItem(
    @DocumentId val id: String = "",
    val sourceQuestionId: String? = null,
    val subject: String = "Biology",
    val topic: String = "",
    val stem: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctOption: String = "A",
    val explanation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMcqQuestion(): McqQuestion = McqQuestion(
        id = sourceQuestionId?.takeIf { it.isNotBlank() } ?: "diff_$id",
        subject = subject,
        topic = topic,
        stem = stem,
        optionA = optionA,
        optionB = optionB,
        optionC = optionC,
        optionD = optionD,
        correctOption = correctOption,
        explanation = explanation,
        difficulty = "hard",
        status = "published"
    )
}

enum class SubscriptionPlanTier(
    val key: String,
    val title: String,
    val priceTag: String,
    val badge: String,
    val features: List<String>
) {
    FREE(
        key = "free",
        title = "Free",
        priceTag = "PKR 0 / mo",
        badge = "Starter",
        features = listOf(
            "Daily practice across all 5 core subjects",
            "Randomized A–D options & instant Reasons",
            "Basic subject progress overview",
            "Personal notes & highlights"
        )
    ),
    BASIC(
        key = "basic",
        title = "Basic",
        priceTag = "PKR 950 / mo",
        badge = "Popular",
        features = listOf(
            "Everything in Free + Timed Exam simulations",
            "PDF MCQ downloads with Answer Key & Reasons",
            "Offline interactive downloaded MCQ packs",
            "Smart study schedule generator (cover in N days)",
            "Difficult Questions exam vault & practice"
        )
    ),
    ADVANCE(
        key = "advance",
        title = "Advance",
        priceTag = "PKR 1,950 / mo",
        badge = "Complete",
        features = listOf(
            "Everything in Basic + All Interactive HTML lessons",
            "Two-way Topic ↔ MCQ deep links & YouTube lectures",
            "Exact Chemistry formula structural sheets in PDF",
            "Full JSON Backup Export & Restore",
            "Priority 1-device Access Key support from Admin"
        )
    );

    companion object {
        fun fromKey(raw: String?): SubscriptionPlanTier =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: FREE
    }
}

@Keep
@IgnoreExtraProperties
data class McqQuestion(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val subject: String = "Biology",
    val topic: String = "",
    val stem: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctOption: String = "A",
    val explanation: String = "",
    val youtubeUrl: String? = null,
    val linkedPostId: String? = null,
    val difficulty: String = "medium",
    val status: String = "published",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun optionText(letter: String): String = when (letter.uppercase()) {
        "A" -> optionA
        "B" -> optionB
        "C" -> optionC
        "D" -> optionD
        else -> ""
    }
}

@Keep
@IgnoreExtraProperties
data class StudyPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val title: String = "",
    val body: String = "",
    val subject: String = "Biology",
    val topic: String = "",
    val webUrl: String? = null,
    val youtubeUrl: String? = null,
    val htmlContent: String? = null,
    val htmlFileName: String? = null,
    val courseId: String? = null,
    val status: String = "published",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val hasInteractiveHtml: Boolean
        get() = !htmlContent.isNullOrBlank()
}

@Keep
@IgnoreExtraProperties
data class CourseItem(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val title: String = "",
    val description: String = "",
    val subject: String = "Biology",
    val instructor: String = "MedPrep Faculty",
    val durationLabel: String = "4 Weeks",
    val level: String = "intermediate",
    val syllabusOutline: String = "",
    val youtubeUrl: String? = null,
    val webUrl: String? = null,
    val status: String = "published",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AccessKey(
    @DocumentId val id: String = "",
    val keyCode: String = "",
    val label: String = "",
    val status: String = "active",
    val assignedUserId: String = "",
    val assignedEmail: String = "",
    val boundDeviceId: String = "",
    val createdBy: String = "",
    @ServerTimestamp val activatedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestionAttempt(
    @DocumentId val id: String = "",
    val questionId: String = "",
    val subject: String = "Biology",
    val selectedOption: String = "",
    val correct: Boolean = false,
    val answered: Boolean = false,
    val bookmarked: Boolean = false,
    val attemptsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamSession(
    @DocumentId val id: String = "",
    val mode: String = "practice",
    val subjectFilter: String = "All Subjects",
    val totalQuestions: Long = 0L,
    val correctCount: Long = 0L,
    val durationSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class ShuffledOption(
    val displayLetter: String,
    val originalLetter: String,
    val text: String
)

data class ShuffledQuestion(
    val question: McqQuestion,
    val options: List<ShuffledOption>
) {
    val correctDisplayLetter: String
        get() = options.firstOrNull {
            it.originalLetter.equals(question.correctOption, ignoreCase = true)
        }?.displayLetter ?: question.correctOption.uppercase()

    fun originalLetterForDisplay(displayLetter: String): String {
        return options.firstOrNull {
            it.displayLetter.equals(displayLetter, ignoreCase = true)
        }?.originalLetter ?: displayLetter.uppercase()
    }

    companion object {
        private val DISPLAY_SLOTS = listOf("A", "B", "C", "D")

        fun from(question: McqQuestion, shuffleOptions: Boolean = true): ShuffledQuestion {
            val baseOptions = listOf(
                "A" to question.optionA,
                "B" to question.optionB,
                "C" to question.optionC,
                "D" to question.optionD
            )
            val ordered = if (shuffleOptions) baseOptions.shuffled() else baseOptions

            val mapped = ordered.mapIndexed { idx, (origLetter, text) ->
                ShuffledOption(
                    displayLetter = DISPLAY_SLOTS[idx],
                    originalLetter = origLetter,
                    text = text
                )
            }
            return ShuffledQuestion(question = question, options = mapped)
        }
    }
}

sealed interface StudentAccessStatus {
    data object Checking : StudentAccessStatus
    data class Unlocked(val key: AccessKey?) : StudentAccessStatus
    data object NeedsKey : StudentAccessStatus
    data class DeviceMismatch(val key: AccessKey) : StudentAccessStatus
    data class Revoked(val key: AccessKey) : StudentAccessStatus
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StudyMode(val key: String, val label: String) {
    PRACTICE("practice", "Practice Mode"),
    EXAM("exam", "Timed Exam"),
    MISTAKES("mistakes", "Mistake Review"),
    BOOKMARKS("bookmarks", "Bookmarked")
}

data class QuestionDraftInput(
    val existingId: String? = null,
    val subject: String,
    val topic: String,
    val stem: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String,
    val explanation: String,
    val youtubeUrl: String? = null,
    val linkedPostId: String? = null,
    val difficulty: String,
    val status: String
)

data class StudyPostDraftInput(
    val existingId: String? = null,
    val title: String,
    val body: String,
    val subject: String,
    val topic: String,
    val webUrl: String? = null,
    val youtubeUrl: String? = null,
    val htmlContent: String? = null,
    val htmlFileName: String? = null,
    val courseId: String? = null,
    val status: String
)

data class CourseDraftInput(
    val existingId: String? = null,
    val title: String,
    val description: String,
    val subject: String,
    val instructor: String,
    val durationLabel: String,
    val level: String,
    val syllabusOutline: String,
    val youtubeUrl: String? = null,
    val webUrl: String? = null,
    val status: String
)

data class CsvParseResult(
    val validQuestions: List<QuestionDraftInput>,
    val errors: List<String>
)

data class SyncReport(
    val syncedQuestionsCount: Int,
    val fromServer: Boolean,
    val message: String
)

data class SubjectStats(
    val subject: String,
    val totalQuestions: Int,
    val answeredCount: Int,
    val correctCount: Int
) {
    val masteryFraction: Float
        get() = if (totalQuestions == 0) 0f else (correctCount.toFloat() / totalQuestions.toFloat()).coerceIn(0f, 1f)

    val accuracyPercent: Int
        get() = if (answeredCount == 0) 0 else ((correctCount * 100) / answeredCount)
}

object MedPrepConstants {
    const val BOOTSTRAP_ADMIN_EMAIL = "irfanalichachar862@gmail.com"

    val EXAM_TRACKS = listOf(
        "MDCAT",
        "NUMS",
        "ETEA",
        "SZABMU",
        "USMLE Step 1",
        "FCPS Part 1"
    )

    val DEFAULT_SUBJECTS = listOf(
        "Biology",
        "Chemistry",
        "Physics",
        "English",
        "Logical Reasoning"
    )

    fun isCoreSubject(subject: String): Boolean =
        DEFAULT_SUBJECTS.any { it.equals(subject.trim(), ignoreCase = true) }

    val DIFFICULTIES = listOf("easy", "medium", "hard")

    const val MAX_HTML_CONTENT_CHARS = 200_000
}

/**
 * Preserves exact line breaks and leading indentation for pasted Chemistry formulas
 * and multi-line structural diagrams without collapsing whitespace.
 */
object FormulaTextPreserver {
    fun preserveExactStructure(raw: String, maxLength: Int): String {
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
        if (normalized.isBlank()) return ""
        val lines = normalized.split("\n")
        val firstNonBlank = lines.indexOfFirst { it.isNotBlank() }
        val lastNonBlank = lines.indexOfLast { it.isNotBlank() }
        if (firstNonBlank == -1 || lastNonBlank == -1) return ""
        val trimmedLines = lines.subList(firstNonBlank, lastNonBlank + 1)
        val joined = if (trimmedLines.size == 1) {
            // Single-line text: trim only trailing spaces so leading spaces in aligned snippets stay safe
            trimmedLines.first().trimEnd()
        } else {
            // Multi-line text: preserve leading indentation on EVERY line so chemical structures stay aligned
            trimmedLines.joinToString("\n") { it.trimEnd() }
        }
        return joined.take(maxLength)
    }
}

/**
 * Bidirectional topic-to-MCQ linking between StudyPosts (including Interactive HTML lessons) and McqQuestions.
 */
object TopicLinkResolver {
    fun findLinkedPost(question: McqQuestion, posts: List<StudyPost>): StudyPost? {
        if (posts.isEmpty()) return null

        // 1. Explicit link by linkedPostId
        val explicitId = question.linkedPostId?.trim().orEmpty()
        if (explicitId.isNotEmpty()) {
            posts.firstOrNull { it.id == explicitId }?.let { return it }
        }

        // 2. Exact case-insensitive Subject + Topic match (preferring interactive HTML lessons)
        val qTopic = question.topic.trim()
        if (qTopic.isNotEmpty()) {
            val sameSubject = posts.filter { it.subject.equals(question.subject, ignoreCase = true) }
            val exactMatches = sameSubject.filter { it.topic.trim().equals(qTopic, ignoreCase = true) }
            if (exactMatches.isNotEmpty()) {
                return exactMatches.firstOrNull { it.hasInteractiveHtml } ?: exactMatches.first()
            }

            // 3. Partial Topic or Title match within the same Subject
            val fuzzyMatches = sameSubject.filter { post ->
                val pTopic = post.topic.trim()
                (pTopic.isNotEmpty() && (pTopic.contains(qTopic, ignoreCase = true) || qTopic.contains(pTopic, ignoreCase = true))) ||
                    post.title.contains(qTopic, ignoreCase = true)
            }
            if (fuzzyMatches.isNotEmpty()) {
                return fuzzyMatches.firstOrNull { it.hasInteractiveHtml } ?: fuzzyMatches.first()
            }
        }

        return null
    }

    fun findLinkedQuestions(post: StudyPost, questions: List<McqQuestion>): List<McqQuestion> {
        if (questions.isEmpty()) return emptyList()
        val pTopic = post.topic.trim()

        val directAndTopic = questions.filter { q ->
            if (!q.linkedPostId.isNullOrBlank() && q.linkedPostId == post.id) {
                true
            } else if (pTopic.isNotEmpty() && q.subject.equals(post.subject, ignoreCase = true)) {
                val qTopic = q.topic.trim()
                qTopic.isNotEmpty() && (
                    qTopic.equals(pTopic, ignoreCase = true) ||
                        qTopic.contains(pTopic, ignoreCase = true) ||
                        pTopic.contains(qTopic, ignoreCase = true)
                    )
            } else {
                false
            }
        }
        return directAndTopic.distinctBy { it.id }
    }
}

