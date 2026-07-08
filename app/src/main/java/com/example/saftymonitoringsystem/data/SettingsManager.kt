package com.example.saftymonitoringsystem.data

import android.content.Context
import android.content.SharedPreferences
import com.example.saftymonitoringsystem.data.model.UserSettings

/**
 * Manages reading and writing [UserSettings] to SharedPreferences.
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("safety_settings", Context.MODE_PRIVATE)

    fun getSettings(): UserSettings = UserSettings(
        alertThreshold    = prefs.getInt("alert_threshold", 80),
        darkMode          = prefs.getBoolean("dark_mode", true),
        monitoringEnabled = prefs.getBoolean("monitoring_enabled", true),
        userName          = prefs.getString("user_name", "User") ?: "User",
        panicCountdown    = prefs.getInt("panic_countdown", 5),
        notifySms         = prefs.getBoolean("notify_sms", true),
        notifyPush        = prefs.getBoolean("notify_push", true),
        privacyMode       = prefs.getBoolean("privacy_mode", true),
    )

    fun saveSettings(settings: UserSettings) {
        prefs.edit().apply {
            putInt("alert_threshold",     settings.alertThreshold)
            putBoolean("dark_mode",       settings.darkMode)
            putBoolean("monitoring_enabled", settings.monitoringEnabled)
            putString("user_name",        settings.userName)
            putInt("panic_countdown",     settings.panicCountdown)
            putBoolean("notify_sms",      settings.notifySms)
            putBoolean("notify_push",     settings.notifyPush)
            putBoolean("privacy_mode",    settings.privacyMode)
            apply()
        }
    }
}
