package com.example.saftymonitoringsystem.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saftymonitoringsystem.ai.ThreatEngine
import com.example.saftymonitoringsystem.data.AlertManager
import com.example.saftymonitoringsystem.data.ContactManager
import com.example.saftymonitoringsystem.data.IncidentManager
import com.example.saftymonitoringsystem.data.LocationHelper
import com.example.saftymonitoringsystem.data.SettingsManager
import com.example.saftymonitoringsystem.data.model.DetectionResult
import com.example.saftymonitoringsystem.data.model.EmergencyContact
import com.example.saftymonitoringsystem.data.model.SafetyIncident
import com.example.saftymonitoringsystem.data.model.UserSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── UI State ─────────────────────────────────────────────────────────────────

data class SafetyUiState(
    val latestDetection : DetectionResult = DetectionResult(),
    val threatLevel     : Int             = 0,
    val isMonitoring    : Boolean         = false,
    val alertSent       : Boolean         = false,
    val panicActive     : Boolean         = false,
    val panicCountdown  : Int             = 0,
    val statusMessage   : String          = "Monitoring inactive"
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

/**
 * Central ViewModel for the safety monitoring system.
 *
 * Responsibilities:
 * - Coordinate AI detection results from FaceAnalyzer, ObjectAnalyzer, MotionAnalyzer
 * - Run the multi-factor ThreatEngine
 * - Trigger emergency alerts (SMS + push) when threshold is exceeded
 * - Manage the SOS panic countdown
 * - Persist and expose incidents, contacts, and settings
 */
class SafetyViewModel(application: Application) : AndroidViewModel(application) {

    // ── Managers ──────────────────────────────────────────────────────────────
    private val locationHelper  = LocationHelper(application)
    private val alertManager    = AlertManager(application)
    private val contactManager  = ContactManager(application)
    private val incidentManager = IncidentManager(application)
    private val settingsManager = SettingsManager(application)

    // ── State flows ───────────────────────────────────────────────────────────
    private val _uiState    = MutableStateFlow(SafetyUiState())
    val uiState: StateFlow<SafetyUiState> = _uiState.asStateFlow()

    private val _contacts   = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private val _incidents  = MutableStateFlow<List<SafetyIncident>>(emptyList())
    val incidents: StateFlow<List<SafetyIncident>> = _incidents.asStateFlow()

    private val _settings   = MutableStateFlow(settingsManager.getSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    // ── Partial detection buffers (updated independently by each analyzer) ────
    private var lastEmotion      = "Neutral"
    private var lastEmotionConf  = 0f
    private var lastEmotionBreak: Map<String, Float> = emptyMap()
    private var lastObjects      = listOf<String>()
    private var lastObjConfs     = listOf<Float>()
    private var lastMotion       = "Still"
    private var lastMotionInt    = 0f

    private var panicJob: Job? = null
    private var alertCooldownActive = false

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        loadContacts()
        loadIncidents()
        locationHelper.startTracking()
    }

    override fun onCleared() {
        super.onCleared()
        locationHelper.stopTracking()
    }

    // ── Detection updates (called from AI analyzers via MonitoringScreen) ─────

    fun updateEmotion(emotion: String, confidence: Float, breakdown: Map<String, Float>) {
        lastEmotion      = emotion
        lastEmotionConf  = confidence
        lastEmotionBreak = breakdown
        recomputeThreat()
    }

    fun updateObjects(labels: List<String>, confidences: List<Float>) {
        lastObjects  = labels
        lastObjConfs = confidences
        recomputeThreat()
    }

    fun updateMotion(activity: String, intensity: Float) {
        lastMotion    = activity
        lastMotionInt = intensity
        recomputeThreat()
    }

    // ── Threat calculation ────────────────────────────────────────────────────

    private fun recomputeThreat() {
        val score = ThreatEngine.calculate(
            emotion            = lastEmotion,
            emotionConfidence  = lastEmotionConf,
            detectedObjects    = lastObjects,
            objectConfidences  = lastObjConfs,
            motionActivity     = lastMotion,
            motionIntensity    = lastMotionInt
        )

        val detection = DetectionResult(
            emotion            = lastEmotion,
            emotionConfidence  = lastEmotionConf,
            emotionBreakdown   = lastEmotionBreak,
            detectedObjects    = lastObjects,
            objectConfidences  = lastObjConfs,
            motionActivity     = lastMotion,
            motionIntensity    = lastMotionInt,
            threatScore        = score
        )

        val statusMsg = when {
            score < 40 -> "✅ Environment appears safe"
            score < 60 -> "⚠️ Mild distress signals detected"
            score < 80 -> "🟠 Elevated threat level – stay alert"
            else       -> "🔴 HIGH THREAT – alert triggered"
        }

        _uiState.update {
            it.copy(
                latestDetection = detection,
                threatLevel     = score,
                statusMessage   = statusMsg
            )
        }

        val threshold = _settings.value.alertThreshold
        if (score >= threshold && !_uiState.value.alertSent && !alertCooldownActive) {
            triggerEmergencyAlert()
        }
    }

    // ── Emergency alert ───────────────────────────────────────────────────────

    private fun triggerEmergencyAlert() {
        _uiState.update { it.copy(alertSent = true) }
        alertCooldownActive = true

        viewModelScope.launch {
            val location = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                locationHelper.getLastLocation { loc -> cont.resume(loc) {} }
            }

            val incident = SafetyIncident(
                emotion        = lastEmotion,
                emotionConf    = lastEmotionConf,
                motionActivity = lastMotion,
                threatLevel    = _uiState.value.threatLevel,
                location       = locationHelper.locationToString(location),
                mapsUrl        = locationHelper.locationToMapsUrl(location),
                detectedObjects = lastObjects,
                alertSent      = true
            )

            incidentManager.saveIncident(incident)
            loadIncidents()

            val smsBody = alertManager.buildSmsBody(incident)
            if (_settings.value.notifySms) {
                _contacts.value.forEach { alertManager.sendSmsAlert(it.phone, smsBody) }
            }
            if (_settings.value.notifyPush) {
                alertManager.sendPushNotification(incident)
            }

            // 5-minute cool-down before another auto alert
            delay(300_000L)
            alertCooldownActive = false
        }
    }

    // ── SOS Panic Button ──────────────────────────────────────────────────────

    /**
     * Starts a countdown timer; if not cancelled before expiry, sends emergency alert.
     */
    fun startPanic() {
        val countdown = _settings.value.panicCountdown
        panicJob?.cancel()
        _uiState.update { it.copy(panicActive = true, panicCountdown = countdown) }

        panicJob = viewModelScope.launch {
            for (i in countdown downTo 1) {
                _uiState.update { it.copy(panicCountdown = i) }
                delay(1000L)
            }
            // Countdown finished → force alert at 100% threat
            lastEmotion = "Fear"
            lastEmotionConf = 1f
            _uiState.update { it.copy(panicActive = false, alertSent = false, threatLevel = 100) }
            triggerEmergencyAlert()
        }
    }

    fun cancelPanic() {
        panicJob?.cancel()
        panicJob = null
        _uiState.update { it.copy(panicActive = false, panicCountdown = 0) }
    }

    // ── Monitoring toggle ─────────────────────────────────────────────────────

    fun setMonitoring(active: Boolean) {
        _uiState.update { it.copy(isMonitoring = active) }
    }

    // ── Alert reset ───────────────────────────────────────────────────────────

    fun resetAlert() {
        _uiState.update { it.copy(alertSent = false) }
    }

    // ── Contacts ──────────────────────────────────────────────────────────────

    fun loadContacts() { _contacts.value = contactManager.getContacts() }

    fun addContact(name: String, phone: String, email: String, relationship: String, isPrimary: Boolean = false) {
        contactManager.saveContact(
            com.example.saftymonitoringsystem.data.model.EmergencyContact(
                name = name, phone = phone, email = email, relationship = relationship, isPrimary = isPrimary
            )
        )
        loadContacts()
    }

    fun deleteContact(id: String) {
        contactManager.deleteContact(id)
        loadContacts()
    }

    // ── Incidents ─────────────────────────────────────────────────────────────

    fun loadIncidents() { _incidents.value = incidentManager.getIncidents() }

    fun deleteIncident(id: String) {
        incidentManager.deleteIncident(id)
        loadIncidents()
    }

    fun clearAllIncidents() {
        incidentManager.clearAll()
        loadIncidents()
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun saveSettings(settings: UserSettings) {
        settingsManager.saveSettings(settings)
        _settings.value = settings
    }
}
