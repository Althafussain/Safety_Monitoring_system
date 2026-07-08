package com.example.saftymonitoringsystem.data.model

/**
 * User preferences stored in SharedPreferences.
 *
 * @property alertThreshold    Threat score [0..100] above which alerts are sent (default 80)
 * @property darkMode          Force dark mode regardless of system setting
 * @property monitoringEnabled Whether background monitoring is allowed
 * @property userName          Display name of the user
 * @property panicCountdown    Seconds shown in SOS countdown before alert fires (3–10)
 * @property notifySms         Send SMS to emergency contacts on alert
 * @property notifyPush        Show local notification on alert
 * @property privacyMode       Don't store incident images when enabled
 */
data class UserSettings(
    val alertThreshold: Int = 80,
    val darkMode: Boolean = true,
    val monitoringEnabled: Boolean = true,
    val userName: String = "User",
    val panicCountdown: Int = 5,
    val notifySms: Boolean = true,
    val notifyPush: Boolean = true,
    val privacyMode: Boolean = true
)
