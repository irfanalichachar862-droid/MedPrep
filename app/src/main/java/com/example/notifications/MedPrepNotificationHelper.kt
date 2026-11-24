package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.AppFunctionalSettings
import com.example.ui.theme.ThemePreferences
import java.util.Calendar

object MedPrepNotificationHelper {
    private const val TAG = "MedPrepNotifications"
    const val CHANNEL_STUDY_REMINDERS = "medprep_study_reminders"
    private const val REMINDER_NOTIFICATION_ID = 4101
    private const val UPDATE_NOTIFICATION_ID = 4102
    private const val ALARM_REQUEST_CODE = 5101

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_STUDY_REMINDERS,
                "MedPrep Study & Course Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily MCQ study reminders, new courses, and exam streak notifications."
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun syncSchedule(context: Context, settings: AppFunctionalSettings) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildReminderPendingIntent(appContext)

        if (!settings.notificationsEnabled || !settings.dailyStudyReminder) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.reminderHour.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not schedule inexact daily study reminder: ${e.message}")
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = UPDATE_NOTIFICATION_ID
    ): Boolean {
        val appContext = context.applicationContext
        if (!hasNotificationPermission(appContext)) return false
        ensureChannel(appContext)

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_STUDY_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted: ${e.message}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Notification dispatch failed: ${e.message}")
            false
        }
    }

    private fun buildReminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, StudyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun formatReminderHour(hour24: Int): String {
        val h = hour24.coerceIn(0, 23)
        val suffix = if (h >= 12) "PM" else "AM"
        val display12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "%02d:00 %s".format(display12, suffix)
    }
}

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val settings = ThemePreferences.loadFunctionalSettings(context.applicationContext)
        if (!settings.notificationsEnabled || !settings.dailyStudyReminder) return
        MedPrepNotificationHelper.showNotification(
            context = context,
            title = "Time for Your Daily MedPrep Practice",
            body = "Keep your MDCAT streak alive — complete today's Biology, Chemistry, Physics, English & Logical Reasoning MCQs.",
            notificationId = 4101
        )
    }
}
