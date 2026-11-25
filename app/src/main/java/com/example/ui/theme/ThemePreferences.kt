package com.example.ui.theme

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppThemeMode(val key: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromKey(raw: String?): AppThemeMode =
            entries.firstOrNull { it.key == raw } ?: SYSTEM
    }
}

enum class AppColorPalette(
    val key: String,
    val label: String,
    val subtitle: String,
    val swatchPrimary: Color,
    val swatchAccent: Color
) {
    CLINICAL_TEAL(
        key = "clinical_teal",
        label = "Clinical Teal",
        subtitle = "Crisp medical focus",
        swatchPrimary = Color(0xFF0D6E6E),
        swatchAccent = Color(0xFF4FD1C5)
    ),
    ROYAL_SAPPHIRE(
        key = "royal_sapphire",
        label = "Royal Sapphire",
        subtitle = "Deep ocean blue clarity",
        swatchPrimary = Color(0xFF1D4ED8),
        swatchAccent = Color(0xFF60A5FA)
    ),
    EMERALD_BOTANICAL(
        key = "emerald_botanical",
        label = "Emerald Forest",
        subtitle = "Calm eye-friendly green",
        swatchPrimary = Color(0xFF047857),
        swatchAccent = Color(0xFF34D399)
    ),
    AMETHYST_GLASS(
        key = "amethyst_glass",
        label = "Amethyst Glass",
        subtitle = "Vibrant violet liquid tint",
        swatchPrimary = Color(0xFF6D28D9),
        swatchAccent = Color(0xFFA78BFA)
    ),
    AMBER_SUNSET(
        key = "amber_sunset",
        label = "Amber Terracotta",
        subtitle = "Warm paper study glow",
        swatchPrimary = Color(0xFFB45309),
        swatchAccent = Color(0xFFFBBF24)
    );

    companion object {
        fun fromKey(raw: String?): AppColorPalette =
            entries.firstOrNull { it.key == raw } ?: CLINICAL_TEAL
    }
}

enum class AppFontStyle(val key: String, val label: String, val sample: String) {
    MODERN_SANS("modern_sans", "Modern Sans", "Plus Jakarta + Inter"),
    CLASSIC_SERIF("classic_serif", "Editorial Serif", "Book-like reading feel"),
    TECHNICAL_MONO("technical_mono", "Scientific Mono", "JetBrains precision"),
    CLEAN_SYSTEM("clean_system", "Clean Rounded", "Crisp Android Sans");

    companion object {
        fun fromKey(raw: String?): AppFontStyle =
            entries.firstOrNull { it.key == raw } ?: MODERN_SANS
    }
}

enum class AppTextScale(val key: String, val label: String, val multiplier: Float) {
    COMPACT("compact", "Compact", 0.92f),
    NORMAL("normal", "Normal", 1.0f),
    LARGE("large", "Large", 1.12f),
    EXTRA_LARGE("xlarge", "XL", 1.22f);

    companion object {
        fun fromKey(raw: String?): AppTextScale =
            entries.firstOrNull { it.key == raw } ?: NORMAL
    }
}

data class AppAppearanceConfig(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val colorPalette: AppColorPalette = AppColorPalette.CLINICAL_TEAL,
    val fontStyle: AppFontStyle = AppFontStyle.MODERN_SANS,
    val textScale: AppTextScale = AppTextScale.NORMAL
)

enum class AppEntryMode(val key: String) {
    STUDENT_KEY("student_key"),
    ADMIN("admin");

    companion object {
        fun fromKey(raw: String?): AppEntryMode =
            entries.firstOrNull { it.key == raw } ?: ADMIN
    }
}

data class AppFunctionalSettings(
    val shuffleOptions: Boolean = true,
    val showInstantReason: Boolean = true,
    val examSecondsPerQuestion: Int = 60,
    val monospaceChemistry: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val dailyStudyReminder: Boolean = true,
    val newCoursePostAlerts: Boolean = true,
    val examStreakAlerts: Boolean = true,
    val reminderHour: Int = 20
)

object ThemePreferences {
    private const val PREFS_NAME = "medprep_ui_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_COLOR_PALETTE = "color_palette"
    private const val KEY_FONT_STYLE = "font_style"
    private const val KEY_TEXT_SCALE = "text_scale"
    private const val KEY_SHUFFLE_OPTIONS = "shuffle_options"
    private const val KEY_INSTANT_REASON = "instant_reason"
    private const val KEY_EXAM_SECONDS = "exam_seconds_per_q"
    private const val KEY_MONO_CHEM = "mono_chemistry"
    private const val KEY_NOTIF_ENABLED = "notif_enabled"
    private const val KEY_NOTIF_DAILY = "notif_daily_study"
    private const val KEY_NOTIF_COURSES = "notif_courses_posts"
    private const val KEY_NOTIF_EXAM = "notif_exam_streak"
    private const val KEY_NOTIF_HOUR = "notif_reminder_hour"
    private const val KEY_ENTRY_MODE = "app_entry_mode"
    private const val KEY_PENDING_ACCESS_KEY = "pending_access_key"
    private const val KEY_ACTIVE_STUDENT_SESSION_UID = "active_student_session_uid"

    fun loadEntryMode(context: Context): AppEntryMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppEntryMode.fromKey(prefs.getString(KEY_ENTRY_MODE, AppEntryMode.ADMIN.key))
    }

    fun saveEntryMode(context: Context, mode: AppEntryMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ENTRY_MODE, mode.key).apply()
    }

    fun loadPendingAccessKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PENDING_ACCESS_KEY, "").orEmpty()
    }

    fun savePendingAccessKey(context: Context, keyCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_ACCESS_KEY, keyCode.trim().uppercase()).apply()
    }

    fun loadActiveStudentSessionUid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVE_STUDENT_SESSION_UID, "").orEmpty()
    }

    fun saveActiveStudentSessionUid(context: Context, uid: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_STUDENT_SESSION_UID, uid.trim()).apply()
    }

    fun loadThemeMode(context: Context): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.key))
    }

    fun saveThemeMode(context: Context, mode: AppThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.key).apply()
    }

    fun loadAppearanceConfig(context: Context): AppAppearanceConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppAppearanceConfig(
            themeMode = AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.key)),
            colorPalette = AppColorPalette.fromKey(prefs.getString(KEY_COLOR_PALETTE, AppColorPalette.CLINICAL_TEAL.key)),
            fontStyle = AppFontStyle.fromKey(prefs.getString(KEY_FONT_STYLE, AppFontStyle.MODERN_SANS.key)),
            textScale = AppTextScale.fromKey(prefs.getString(KEY_TEXT_SCALE, AppTextScale.NORMAL.key))
        )
    }

    fun saveAppearanceConfig(context: Context, config: AppAppearanceConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_THEME_MODE, config.themeMode.key)
            .putString(KEY_COLOR_PALETTE, config.colorPalette.key)
            .putString(KEY_FONT_STYLE, config.fontStyle.key)
            .putString(KEY_TEXT_SCALE, config.textScale.key)
            .apply()
    }

    fun loadFunctionalSettings(context: Context): AppFunctionalSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppFunctionalSettings(
            shuffleOptions = prefs.getBoolean(KEY_SHUFFLE_OPTIONS, true),
            showInstantReason = prefs.getBoolean(KEY_INSTANT_REASON, true),
            examSecondsPerQuestion = prefs.getInt(KEY_EXAM_SECONDS, 60).coerceIn(30, 180),
            monospaceChemistry = prefs.getBoolean(KEY_MONO_CHEM, true),
            notificationsEnabled = prefs.getBoolean(KEY_NOTIF_ENABLED, false),
            dailyStudyReminder = prefs.getBoolean(KEY_NOTIF_DAILY, true),
            newCoursePostAlerts = prefs.getBoolean(KEY_NOTIF_COURSES, true),
            examStreakAlerts = prefs.getBoolean(KEY_NOTIF_EXAM, true),
            reminderHour = prefs.getInt(KEY_NOTIF_HOUR, 20).coerceIn(0, 23)
        )
    }

    fun saveFunctionalSettings(context: Context, settings: AppFunctionalSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SHUFFLE_OPTIONS, settings.shuffleOptions)
            .putBoolean(KEY_INSTANT_REASON, settings.showInstantReason)
            .putInt(KEY_EXAM_SECONDS, settings.examSecondsPerQuestion.coerceIn(30, 180))
            .putBoolean(KEY_MONO_CHEM, settings.monospaceChemistry)
            .putBoolean(KEY_NOTIF_ENABLED, settings.notificationsEnabled)
            .putBoolean(KEY_NOTIF_DAILY, settings.dailyStudyReminder)
            .putBoolean(KEY_NOTIF_COURSES, settings.newCoursePostAlerts)
            .putBoolean(KEY_NOTIF_EXAM, settings.examStreakAlerts)
            .putInt(KEY_NOTIF_HOUR, settings.reminderHour.coerceIn(0, 23))
            .apply()
    }
}

data class SubjectVisual(
    val icon: ImageVector,
    val accent: Color,
    val containerLight: Color,
    val containerDark: Color
)

fun subjectVisualFor(subject: String): SubjectVisual = when (subject.trim().lowercase()) {
    "biology" -> SubjectVisual(
        icon = Icons.Rounded.Biotech,
        accent = Color(0xFF15803D),
        containerLight = Color(0xFFDCFCE7),
        containerDark = Color(0xFF133A22)
    )
    "chemistry" -> SubjectVisual(
        icon = Icons.Rounded.Science,
        accent = Color(0xFF0369A1),
        containerLight = Color(0xFFE0F2FE),
        containerDark = Color(0xFF113248)
    )
    "physics" -> SubjectVisual(
        icon = Icons.Rounded.Bolt,
        accent = Color(0xFFD97706),
        containerLight = Color(0xFFFEF3C7),
        containerDark = Color(0xFF44290A)
    )
    "english" -> SubjectVisual(
        icon = Icons.Rounded.MenuBook,
        accent = Color(0xFFBE185D),
        containerLight = Color(0xFFFCE7F3),
        containerDark = Color(0xFF46162E)
    )
    "logical reasoning" -> SubjectVisual(
        icon = Icons.Rounded.Psychology,
        accent = Color(0xFF7C3AED),
        containerLight = Color(0xFFEDE9FE),
        containerDark = Color(0xFF2E1A55)
    )
    else -> SubjectVisual(
        icon = Icons.Rounded.School,
        accent = ClinicalTeal,
        containerLight = ClinicalMintContainer,
        containerDark = Color(0xFF104044)
    )
}
