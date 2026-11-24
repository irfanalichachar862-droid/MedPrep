package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Provides a stable, zero-permission app-scoped device fingerprint (SHA-256 hex)
 * used to enforce 1-User 1-Device binding for student Access Keys.
 */
object DeviceIdProvider {
    private const val PREFS_NAME = "medprep_device_identity"
    private const val KEY_FALLBACK_UUID = "fallback_device_uuid"
    private const val APP_SALT = "medprep_qrvkxn_device_lock_v1"

    @Volatile
    private var cachedDeviceId: String? = null

    suspend fun getDeviceIdAsync(context: Context): String {
        cachedDeviceId?.let { return it }
        return withContext(Dispatchers.IO) {
            getDeviceId(context)
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        cachedDeviceId?.let { return it }
        val appContext = context.applicationContext
        val androidId = runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.trim().orEmpty()

        val rawSeed = if (androidId.length >= 8 && androidId != "9774d56d682e549c") {
            androidId
        } else {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_FALLBACK_UUID, null) ?: UUID.randomUUID().toString().also {
                prefs.edit().putString(KEY_FALLBACK_UUID, it).apply()
            }
        }
        val computed = sha256Hex("$APP_SALT:$rawSeed").take(32)
        cachedDeviceId = computed
        return computed
    }

    fun shortLabel(deviceId: String): String {
        val clean = deviceId.trim().uppercase()
        if (clean.length <= 12) return clean
        return "${clean.take(4)}-${clean.substring(4, 8)}-${clean.substring(8, 12)}"
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
