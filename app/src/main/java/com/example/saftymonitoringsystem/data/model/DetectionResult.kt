package com.example.saftymonitoringsystem.data.model

/**
 * Comprehensive result aggregating all AI-detection modules.
 *
 * @property emotion             Primary detected emotion label
 * @property emotionConfidence   Confidence score [0..1] for the primary emotion
 * @property emotionBreakdown    Map of emotion → confidence for all 7 emotions
 * @property detectedObjects     List of dangerous object labels detected
 * @property objectConfidences   Parallel list of confidence scores per detected object
 * @property motionActivity      Classified activity label (e.g. "Running", "Fighting")
 * @property motionIntensity     Intensity score [0..1] of detected motion
 * @property threatScore         Final fused threat score [0..100]
 * @property timestamp           System time of detection
 */
data class DetectionResult(
    val emotion: String = "Neutral",
    val emotionConfidence: Float = 0f,
    val emotionBreakdown: Map<String, Float> = emptyMap(),

    val detectedObjects: List<String> = emptyList(),
    val objectConfidences: List<Float> = emptyList(),

    val motionActivity: String = "Still",
    val motionIntensity: Float = 0f,

    val threatScore: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
