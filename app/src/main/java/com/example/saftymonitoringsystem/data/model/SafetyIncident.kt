package com.example.saftymonitoringsystem.data.model

/**
 * A recorded safety incident stored persistently on the device.
 *
 * @property id              Unique identifier (UUID)
 * @property timestamp       Unix timestamp (ms) when the incident was recorded
 * @property emotion         Primary emotion detected at time of incident
 * @property emotionConf     Confidence [0..1] of the detected emotion
 * @property motionActivity  Detected motion/activity label
 * @property detectedObjects Dangerous objects identified by YOLO/TFLite
 * @property threatLevel     Fused threat score [0..100]
 * @property location        "lat,lng" string or "Unknown"
 * @property mapsUrl         Full Google Maps URL for the location
 * @property alertSent       Whether emergency SMS/notification was sent
 * @property notes           Optional user notes added later
 */
data class SafetyIncident(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: String,
    val emotionConf: Float = 0f,
    val motionActivity: String = "Unknown",
    val threatLevel: Int,
    val location: String = "",
    val mapsUrl: String = "",
    val detectedObjects: List<String> = emptyList(),
    val alertSent: Boolean = false,
    val notes: String = ""
)
