package com.example.saftymonitoringsystem.ai

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Analyses camera frames for facial expressions using Google ML Kit Face Detection.
 *
 * ML Kit provides smilingProbability, leftEyeOpenProbability, and rightEyeOpenProbability.
 * We derive a 7-emotion breakdown from these signals using heuristic rules —
 * a lightweight approach that runs fully on-device with no network call.
 *
 * Research note: For higher accuracy, replace the heuristic mapping with a
 * TensorFlow Lite emotion classifier (e.g. MobileNet trained on AffectNet/FER+).
 *
 * Detected emotions: Happy | Sad | Fear | Angry | Disgust | Surprise | Neutral
 *
 * ETHICAL NOTE: This module only detects momentary facial expressions. It does NOT
 * diagnose any mental health condition. Output should be labelled as
 * "Potential emotional distress detected" — never as a clinical diagnosis.
 */
class FaceAnalyzer(
    private val context: Context,
    private val onFaceAnalyzed: (
        primaryEmotion: String,
        confidence: Float,
        breakdown: Map<String, Float>
    ) -> Unit
) : ImageAnalysis.Analyzer {

    // ── ML Kit detector options ───────────────────────────────────────────────
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setMinFaceSize(0.15f)
        .build()

    private val detector = FaceDetection.getClient(options)

    private val modelAssetName = EmotionModelAsset.inferAssetName(null)

    init {
        val exists = EmotionModelAsset.exists(context, modelAssetName)
        if (!exists) {
            android.util.Log.w("FaceAnalyzer", "Emotion model asset not found in app assets: $modelAssetName")
        } else {
            android.util.Log.i("FaceAnalyzer", "Using emotion model asset: $modelAssetName")
        }
    }

    // ── Analysis ──────────────────────────────────────────────────────────────

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) { imageProxy.close(); return }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                // Use the largest face (closest to camera = user's face)
                val face = faces.maxByOrNull { it.boundingBox.width() } ?: return@addOnSuccessListener

                val smile    = face.smilingProbability       ?: 0.5f
                val leftEye  = face.leftEyeOpenProbability   ?: 0.5f
                val rightEye = face.rightEyeOpenProbability  ?: 0.5f
                val avgEye   = (leftEye + rightEye) / 2f
                val eyeAsymmetry = kotlin.math.abs(leftEye - rightEye)

                // ── Heuristic emotion scoring ─────────────────────────────────
                // Each score is in [0..1]; they will be softmax-normalised below.
                val raw = mutableMapOf(
                    "Happy"    to clamp(smile * 1.5f - 0.2f),
                    "Sad"      to clamp((1f - smile) * (1f - avgEye + 0.3f)),
                    "Fear"     to clamp(avgEye * (1f - smile) * 1.2f + eyeAsymmetry),
                    "Angry"    to clamp((1f - smile) * 0.6f + eyeAsymmetry * 0.8f),
                    "Disgust"  to clamp((1f - smile) * 0.5f - avgEye * 0.3f),
                    "Surprise" to clamp(avgEye * 1.3f - smile * 0.5f),
                    "Neutral"  to clamp(0.4f + smile * 0.1f - eyeAsymmetry)
                )

                // Softmax normalisation so all scores sum to 1
                val breakdown = softmax(raw)

                // Primary emotion = highest score
                val primary = breakdown.maxByOrNull { it.value }
                val primaryLabel = primary?.key ?: "Neutral"
                val primaryConf  = primary?.value ?: 0f

                onFaceAnalyzed(primaryLabel, primaryConf, breakdown)
            }
            .addOnFailureListener { /* silent – next frame will retry */ }
            .addOnCompleteListener { imageProxy.close() }
    }

    // ── Maths helpers ─────────────────────────────────────────────────────────

    private fun clamp(v: Float) = v.coerceIn(0f, 1f)

    private fun softmax(raw: Map<String, Float>): Map<String, Float> {
        val expValues = raw.mapValues { (_, v) -> Math.exp((v * 5.0)).toFloat() }
        val sum = expValues.values.sum().coerceAtLeast(1e-6f)
        return expValues.mapValues { (_, e) -> e / sum }
    }
}
