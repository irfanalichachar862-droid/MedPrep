package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.ui.theme.AppAppearanceConfig
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.AppFontStyle
import com.example.ui.theme.AppFunctionalSettings
import com.example.ui.theme.AppTextScale
import com.example.ui.theme.AppThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class DownloadedMcqPack(
    val id: String,
    val title: String,
    val subject: String,
    val downloadedAtMillis: Long,
    val questions: List<McqQuestion>
)

data class MedPrepBackupPayload(
    val exportedAtMillis: Long,
    val displayName: String?,
    val contactEmail: String?,
    val phoneNumber: String?,
    val targetExam: String?,
    val dailyGoal: Long?,
    val subscriptionPlan: String?,
    val scheduleDays: Long?,
    val appearance: AppAppearanceConfig?,
    val settings: AppFunctionalSettings?,
    val notes: List<StudyNoteDraft>,
    val difficultQuestions: List<DifficultQuestionDraft>,
    val offlinePacks: List<DownloadedMcqPack>
)

data class StudyNoteDraft(
    val existingId: String? = null,
    val title: String,
    val content: String,
    val subject: String,
    val questionId: String? = null,
    val highlightColor: String = "teal"
)

data class DifficultQuestionDraft(
    val existingId: String? = null,
    val sourceQuestionId: String? = null,
    val subject: String,
    val topic: String,
    val stem: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String,
    val explanation: String
)

/**
 * Manages:
 * 1. Offline Interactive Downloaded MCQ Packs (runs interactively inside the app even without internet)
 * 2. Zero-permission local profile avatar image saved from Android Photo Picker
 * 3. Full JSON Backup Export & Import
 */
object OfflinePackStorage {
    private const val MAX_AVATAR_PX = 256

    private fun safeUid(uid: String): String =
        uid.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64).ifBlank { "default" }

    private fun packsFile(context: Context, uid: String): File =
        File(context.filesDir, "medprep_offline_packs_${safeUid(uid)}.json")

    private fun avatarFile(context: Context, uid: String): File =
        File(context.filesDir, "medprep_avatar_${safeUid(uid)}.jpg")

    // --- OFFLINE INTERACTIVE MCQ PACKS ---

    fun loadDownloadedPacks(context: Context, uid: String): List<DownloadedMcqPack> {
        val file = packsFile(context, uid)
        if (!file.exists()) return emptyList()
        return try {
            val raw = file.readText(Charsets.UTF_8)
            val arr = JSONArray(raw)
            parsePacksArray(arr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDownloadedPack(
        context: Context,
        uid: String,
        title: String,
        subject: String,
        questions: List<McqQuestion>
    ): List<DownloadedMcqPack> {
        val existing = loadDownloadedPacks(context, uid).toMutableList()
        // Replace existing pack for the same subject/title to avoid duplicates
        existing.removeAll { it.subject.equals(subject, ignoreCase = true) && it.title.equals(title, ignoreCase = true) }
        val newPack = DownloadedMcqPack(
            id = "pack_${UUID.randomUUID().toString().take(8)}",
            title = title.trim().take(80),
            subject = subject.trim().take(60),
            downloadedAtMillis = System.currentTimeMillis(),
            questions = questions
        )
        existing.add(0, newPack)
        persistPacks(context, uid, existing.take(20))
        return existing.take(20)
    }

    fun deleteDownloadedPack(context: Context, uid: String, packId: String): List<DownloadedMcqPack> {
        val updated = loadDownloadedPacks(context, uid).filterNot { it.id == packId }
        persistPacks(context, uid, updated)
        return updated
    }

    fun mergeDownloadedPacks(context: Context, uid: String, incoming: List<DownloadedMcqPack>): List<DownloadedMcqPack> {
        val existing = loadDownloadedPacks(context, uid).toMutableList()
        incoming.forEach { pack ->
            if (existing.none { it.title.equals(pack.title, ignoreCase = true) && it.subject.equals(pack.subject, ignoreCase = true) }) {
                existing.add(pack)
            }
        }
        val capped = existing.sortedByDescending { it.downloadedAtMillis }.take(20)
        persistPacks(context, uid, capped)
        return capped
    }

    private fun persistPacks(context: Context, uid: String, packs: List<DownloadedMcqPack>) {
        val arr = serializePacksArray(packs)
        packsFile(context, uid).writeText(arr.toString(), Charsets.UTF_8)
    }

    // --- ZERO-PERMISSION PROFILE PHOTO PICKER STORAGE ---

    fun saveUserAvatarFromUri(context: Context, uid: String, uri: Uri): Bitmap? {
        return try {
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val minDim = minOf(decoded.width, decoded.height).coerceAtLeast(1)
            val startX = (decoded.width - minDim) / 2
            val startY = (decoded.height - minDim) / 2
            val square = Bitmap.createBitmap(decoded, startX, startY, minDim, minDim)
            val scaled = Bitmap.createScaledBitmap(square, MAX_AVATAR_PX, MAX_AVATAR_PX, true)

            val outFile = avatarFile(context, uid)
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 86, fos)
            }
            if (square !== decoded) square.recycle()
            if (decoded !== scaled) decoded.recycle()
            scaled
        } catch (_: Exception) {
            null
        }
    }

    fun loadUserAvatarBitmap(context: Context, uid: String): Bitmap? {
        val file = avatarFile(context, uid)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    fun clearUserAvatar(context: Context, uid: String) {
        val file = avatarFile(context, uid)
        if (file.exists()) file.delete()
    }

    // --- FULL JSON BACKUP EXPORT & IMPORT ---

    fun serializeBackup(
        profile: UserProfile?,
        appearance: AppAppearanceConfig,
        settings: AppFunctionalSettings,
        notes: List<StudyNote>,
        difficultQuestions: List<DifficultQuestionItem>,
        offlinePacks: List<DownloadedMcqPack>
    ): String {
        val root = JSONObject()
        root.put("schema", "medprep_backup_v1")
        root.put("exportedAtMillis", System.currentTimeMillis())

        val profileObj = JSONObject().apply {
            put("displayName", profile?.displayName ?: "Medical Student")
            put("contactEmail", profile?.contactEmail.orEmpty())
            put("phoneNumber", profile?.phoneNumber.orEmpty())
            put("targetExam", profile?.targetExam ?: MedPrepConstants.EXAM_TRACKS.first())
            put("dailyGoal", profile?.dailyGoal ?: 25L)
            put("subscriptionPlan", profile?.subscriptionPlan ?: "free")
            put("scheduleDays", profile?.scheduleDays ?: 30L)
        }
        root.put("profile", profileObj)

        val appearanceObj = JSONObject().apply {
            put("themeMode", appearance.themeMode.key)
            put("colorPalette", appearance.colorPalette.key)
            put("fontStyle", appearance.fontStyle.key)
            put("textScale", appearance.textScale.key)
        }
        root.put("appearance", appearanceObj)

        val settingsObj = JSONObject().apply {
            put("shuffleOptions", settings.shuffleOptions)
            put("showInstantReason", settings.showInstantReason)
            put("examSecondsPerQuestion", settings.examSecondsPerQuestion)
            put("monospaceChemistry", settings.monospaceChemistry)
            put("notificationsEnabled", settings.notificationsEnabled)
            put("dailyStudyReminder", settings.dailyStudyReminder)
            put("newCoursePostAlerts", settings.newCoursePostAlerts)
            put("examStreakAlerts", settings.examStreakAlerts)
            put("reminderHour", settings.reminderHour)
        }
        root.put("settings", settingsObj)

        val notesArr = JSONArray()
        notes.forEach { n ->
            notesArr.put(JSONObject().apply {
                put("title", n.title)
                put("content", n.content)
                put("subject", n.subject)
                put("questionId", n.questionId.orEmpty())
                put("highlightColor", n.highlightColor)
            })
        }
        root.put("notes", notesArr)

        val diffArr = JSONArray()
        difficultQuestions.forEach { dq ->
            diffArr.put(JSONObject().apply {
                put("sourceQuestionId", dq.sourceQuestionId.orEmpty())
                put("subject", dq.subject)
                put("topic", dq.topic)
                put("stem", dq.stem)
                put("optionA", dq.optionA)
                put("optionB", dq.optionB)
                put("optionC", dq.optionC)
                put("optionD", dq.optionD)
                put("correctOption", dq.correctOption)
                put("explanation", dq.explanation)
            })
        }
        root.put("difficultQuestions", diffArr)
        root.put("offlinePacks", serializePacksArray(offlinePacks))

        return root.toString(2)
    }

    fun parseBackup(rawJson: String): Result<MedPrepBackupPayload> {
        return try {
            val root = JSONObject(rawJson)
            val schema = root.optString("schema", "")
            if (schema != "medprep_backup_v1") {
                return Result.failure(IllegalArgumentException("Unsupported backup file format."))
            }

            val profileObj = root.optJSONObject("profile")
            val appObj = root.optJSONObject("appearance")
            val setObj = root.optJSONObject("settings")

            val appearance = appObj?.let {
                AppAppearanceConfig(
                    themeMode = AppThemeMode.fromKey(it.optString("themeMode")),
                    colorPalette = AppColorPalette.fromKey(it.optString("colorPalette")),
                    fontStyle = AppFontStyle.fromKey(it.optString("fontStyle")),
                    textScale = AppTextScale.fromKey(it.optString("textScale"))
                )
            }

            val settings = setObj?.let {
                AppFunctionalSettings(
                    shuffleOptions = it.optBoolean("shuffleOptions", true),
                    showInstantReason = it.optBoolean("showInstantReason", true),
                    examSecondsPerQuestion = it.optInt("examSecondsPerQuestion", 60).coerceIn(30, 180),
                    monospaceChemistry = it.optBoolean("monospaceChemistry", true),
                    notificationsEnabled = it.optBoolean("notificationsEnabled", false),
                    dailyStudyReminder = it.optBoolean("dailyStudyReminder", true),
                    newCoursePostAlerts = it.optBoolean("newCoursePostAlerts", true),
                    examStreakAlerts = it.optBoolean("examStreakAlerts", true),
                    reminderHour = it.optInt("reminderHour", 20).coerceIn(0, 23)
                )
            }

            val notes = mutableListOf<StudyNoteDraft>()
            val notesArr = root.optJSONArray("notes")
            if (notesArr != null) {
                for (i in 0 until notesArr.length()) {
                    val item = notesArr.optJSONObject(i) ?: continue
                    val title = item.optString("title").trim()
                    val content = item.optString("content").trim()
                    if (title.isNotEmpty() && content.isNotEmpty()) {
                        notes.add(
                            StudyNoteDraft(
                                title = title.take(160),
                                content = content.take(4000),
                                subject = item.optString("subject", "Biology").ifBlank { "Biology" },
                                questionId = item.optString("questionId").takeIf { it.isNotBlank() },
                                highlightColor = item.optString("highlightColor", "teal")
                                    .takeIf { it in setOf("teal", "amber", "rose", "indigo", "emerald") } ?: "teal"
                            )
                        )
                    }
                }
            }

            val difficult = mutableListOf<DifficultQuestionDraft>()
            val diffArr = root.optJSONArray("difficultQuestions")
            if (diffArr != null) {
                for (i in 0 until diffArr.length()) {
                    val item = diffArr.optJSONObject(i) ?: continue
                    val stem = item.optString("stem")
                    if (stem.trim().length >= 5) {
                        difficult.add(
                            DifficultQuestionDraft(
                                sourceQuestionId = item.optString("sourceQuestionId").takeIf { it.isNotBlank() },
                                subject = item.optString("subject", "Biology").ifBlank { "Biology" },
                                topic = item.optString("topic", ""),
                                stem = stem,
                                optionA = item.optString("optionA", "A"),
                                optionB = item.optString("optionB", "B"),
                                optionC = item.optString("optionC", "C"),
                                optionD = item.optString("optionD", "D"),
                                correctOption = item.optString("correctOption", "A").uppercase().takeIf { it in setOf("A", "B", "C", "D") } ?: "A",
                                explanation = item.optString("explanation", "")
                            )
                        )
                    }
                }
            }

            val packs = root.optJSONArray("offlinePacks")?.let { parsePacksArray(it) }.orEmpty()

            Result.success(
                MedPrepBackupPayload(
                    exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis()),
                    displayName = profileObj?.optString("displayName")?.takeIf { it.isNotBlank() },
                    contactEmail = profileObj?.optString("contactEmail"),
                    phoneNumber = profileObj?.optString("phoneNumber"),
                    targetExam = profileObj?.optString("targetExam")?.takeIf { it.isNotBlank() },
                    dailyGoal = profileObj?.optLong("dailyGoal", 25L),
                    subscriptionPlan = profileObj?.optString("subscriptionPlan")?.takeIf { it in setOf("free", "basic", "advance") },
                    scheduleDays = profileObj?.optLong("scheduleDays", 30L),
                    appearance = appearance,
                    settings = settings,
                    notes = notes,
                    difficultQuestions = difficult,
                    offlinePacks = packs
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid backup JSON: ${e.localizedMessage ?: "Parse error"}"))
        }
    }

    private fun serializePacksArray(packs: List<DownloadedMcqPack>): JSONArray {
        val arr = JSONArray()
        packs.forEach { pack ->
            val qArr = JSONArray()
            pack.questions.forEach { q ->
                qArr.put(JSONObject().apply {
                    put("id", q.id)
                    put("subject", q.subject)
                    put("topic", q.topic)
                    put("stem", q.stem)
                    put("optionA", q.optionA)
                    put("optionB", q.optionB)
                    put("optionC", q.optionC)
                    put("optionD", q.optionD)
                    put("correctOption", q.correctOption)
                    put("explanation", q.explanation)
                    put("youtubeUrl", q.youtubeUrl.orEmpty())
                    put("linkedPostId", q.linkedPostId.orEmpty())
                    put("difficulty", q.difficulty)
                })
            }
            arr.put(JSONObject().apply {
                put("id", pack.id)
                put("title", pack.title)
                put("subject", pack.subject)
                put("downloadedAtMillis", pack.downloadedAtMillis)
                put("questions", qArr)
            })
        }
        return arr
    }

    private fun parsePacksArray(arr: JSONArray): List<DownloadedMcqPack> {
        val list = mutableListOf<DownloadedMcqPack>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val qArr = obj.optJSONArray("questions") ?: JSONArray()
            val questions = mutableListOf<McqQuestion>()
            for (j in 0 until qArr.length()) {
                val q = qArr.optJSONObject(j) ?: continue
                questions.add(
                    McqQuestion(
                        id = q.optString("id", "offline_${i}_$j"),
                        subject = q.optString("subject", "Biology"),
                        topic = q.optString("topic", ""),
                        stem = q.optString("stem", ""),
                        optionA = q.optString("optionA", ""),
                        optionB = q.optString("optionB", ""),
                        optionC = q.optString("optionC", ""),
                        optionD = q.optString("optionD", ""),
                        correctOption = q.optString("correctOption", "A"),
                        explanation = q.optString("explanation", ""),
                        youtubeUrl = q.optString("youtubeUrl").takeIf { it.isNotBlank() },
                        linkedPostId = q.optString("linkedPostId").takeIf { it.isNotBlank() },
                        difficulty = q.optString("difficulty", "medium"),
                        status = "published"
                    )
                )
            }
            if (questions.isNotEmpty()) {
                list.add(
                    DownloadedMcqPack(
                        id = obj.optString("id", "pack_$i"),
                        title = obj.optString("title", "Downloaded Pack"),
                        subject = obj.optString("subject", "All Subjects"),
                        downloadedAtMillis = obj.optLong("downloadedAtMillis", System.currentTimeMillis()),
                        questions = questions
                    )
                )
            }
        }
        return list
    }
}
