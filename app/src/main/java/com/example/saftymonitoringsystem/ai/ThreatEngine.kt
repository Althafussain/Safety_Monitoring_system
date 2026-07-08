package com.example.saftymonitoringsystem.ai

import java.util.Calendar

/**
 * Multi-factor Threat Assessment Engine.
 *
 * Combines emotion, object, motion, and contextual signals using a
 * weighted, rule-augmented fuzzy scoring model.
 *
 * ── Scoring formula ───────────────────────────────────────────────────────────
 *
 *  Base score = w_emo × emotionScore
 *             + w_obj × objectScore
 *             + w_mot × motionScore
 *
 *  Rule bonuses are added for jointly dangerous combinations:
 *    • Fear + object detected  → +15
 *    • Struggle + any distress → +15
 *    • Night-time (22:00–06:00) modifier → +5
 *
 *  Final score is clamped to [0..100].
 *
 * ── Weights (research-calibrated) ────────────────────────────────────────────
 *   emotion : 35 %
 *   object  : 45 %
 *   motion  : 20 %
 *
 * ── Research extension ────────────────────────────────────────────────────────
 * For publication-grade novelty, this engine can be replaced with:
 *   • Random Forest classifier (scikit-learn → ONNX → TFLite)
 *   • Fuzzy Inference System (Mamdani / Sugeno)
 *   • Lightweight feed-forward neural network
 */
object ThreatEngine {

    // ── Weights ───────────────────────────────────────────────────────────────
    private const val W_EMOTION = 0.35f
    private const val W_OBJECT  = 0.45f
    private const val W_MOTION  = 0.20f

    /** Emotions classified as distress signals. */
    private val DISTRESS_EMOTIONS = setOf("Fear", "Sad", "Angry", "Disgust")
    private val HIGH_RISK_EMOTIONS = setOf("Fear")

    /** Object labels that immediately raise score to max object contribution. */
    private val CRITICAL_OBJECTS = setOf(
        "knife", "gun", "pistol", "rifle", "firearm", "sword", "axe", "machete", "grenade"
    )
    private val HIGH_OBJECTS = setOf("hammer", "baseball bat", "bat", "crowbar", "fire", "smoke")

    /** Motion labels that contribute to threat. */
    private val THREAT_MOTIONS = setOf("Rapid Movement", "Possible Running", "Possible Struggle")

    // ── Main API ──────────────────────────────────────────────────────────────

    /**
     * Calculate a threat score in [0..100].
     *
     * @param emotion          Primary detected emotion label
     * @param emotionConfidence Confidence [0..1] for that emotion
     * @param detectedObjects  List of detected dangerous object labels
     * @param objectConfidences Parallel confidence list for objects
     * @param motionActivity   Classified motion activity label
     * @param motionIntensity  Motion intensity [0..1]
     */
    fun calculate(
        emotion: String,
        emotionConfidence: Float,
        detectedObjects: List<String>,
        objectConfidences: List<Float>,
        motionActivity: String,
        motionIntensity: Float
    ): Int {
        // ── Emotion score [0..1] ──────────────────────────────────────────────
        val emotionScore: Float = when {
            emotion in HIGH_RISK_EMOTIONS  -> emotionConfidence.coerceIn(0f, 1f)
            emotion in DISTRESS_EMOTIONS   -> emotionConfidence * 0.75f
            emotion == "Surprise"          -> emotionConfidence * 0.4f
            else                           -> emotionConfidence * 0.1f   // Happy / Neutral
        }

        // ── Object score [0..1] ───────────────────────────────────────────────
        val objectScore: Float = if (detectedObjects.isEmpty()) {
            0f
        } else {
            val maxLabel = detectedObjects.zipWithIndex()
                .maxByOrNull { (_, i) -> objectConfidences.getOrElse(i) { 0f } }
                ?.first?.lowercase() ?: ""
            val topConf  = objectConfidences.maxOrNull() ?: 0f

            when {
                CRITICAL_OBJECTS.any { maxLabel.contains(it) } -> topConf.coerceIn(0f, 1f)
                HIGH_OBJECTS.any    { maxLabel.contains(it) }  -> topConf * 0.75f
                else                                            -> topConf * 0.40f
            }
        }

        // ── Motion score [0..1] ───────────────────────────────────────────────
        val motionScore: Float = when {
            motionActivity == "Possible Struggle" -> motionIntensity
            motionActivity == "Possible Running"  -> motionIntensity * 0.75f
            motionActivity == "Rapid Movement"    -> motionIntensity * 0.50f
            else                                  -> motionIntensity * 0.10f
        }

        // ── Weighted base score ───────────────────────────────────────────────
        var score = (W_EMOTION * emotionScore + W_OBJECT * objectScore + W_MOTION * motionScore) * 100f

        // ── Rule-based bonuses ────────────────────────────────────────────────
        if (emotion in HIGH_RISK_EMOTIONS && detectedObjects.isNotEmpty()) score += 15f
        if (motionActivity == "Possible Struggle" && emotion in DISTRESS_EMOTIONS) score += 15f
        if (isNightTime()) score += 5f

        return score.toInt().coerceIn(0, 100)
    }

    // ── Context helpers ───────────────────────────────────────────────────────

    private fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 6
    }
}

// Extension helper
private fun <T> List<T>.zipWithIndex(): List<Pair<T, Int>> =
    mapIndexed { i, v -> v to i }
