package com.example.saftymonitoringsystem.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

/**
 * Detects dangerous objects in camera frames using a TensorFlow Lite object-detection model.
 *
 * ── Model requirements ────────────────────────────────────────────────────────
 * Place a file named `danger_objects.tflite` in `app/src/main/assets/`.
 * Recommended: YOLOv8n / EfficientDet-Lite0 fine-tuned on dangerous object classes.
 * Fallback: Generic COCO model (`model.tflite`) — dangerous labels are post-filtered.
 *
 * ── Dangerous classes (COCO subset + custom) ─────────────────────────────────
 *   knife, scissors, gun, pistol, rifle, hammer, baseball bat,
 *   bottle (broken heuristic applied), fire, smoke, crowbar
 */
class ObjectAnalyzer(
    context: Context,
    private val onObjectsDetected: (
        labels: List<String>,
        confidences: List<Float>
    ) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "ObjectAnalyzer"
        private const val SCORE_THRESHOLD = 0.45f
        private const val MAX_RESULTS     = 5

        // Post-filter: only report objects in this set
        private val DANGEROUS_LABELS = setOf(
            "knife", "scissors", "gun", "pistol", "rifle", "firearm",
            "hammer", "baseball bat", "bat", "bottle", "fire", "smoke",
            "crowbar", "sword", "axe", "machete", "grenade"
        )
    }

    private val detectorOptions = ObjectDetector.ObjectDetectorOptions.builder()
        .setScoreThreshold(SCORE_THRESHOLD)
        .setMaxResults(MAX_RESULTS)
        .build()

    private var detector: ObjectDetector? = try {
        ObjectDetector.createFromFileAndOptions(context, "danger_objects.tflite", detectorOptions)
    } catch (e1: Exception) {
        Log.w(TAG, "danger_objects.tflite not found, trying model.tflite")
        try {
            ObjectDetector.createFromFileAndOptions(context, "model.tflite", detectorOptions)
        } catch (e2: Exception) {
            Log.e(TAG, "No detection model found – ObjectAnalyzer disabled", e2)
            null
        }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (detector == null) { imageProxy.close(); return }

        try {
            val bitmap: Bitmap = imageProxy.toBitmap()
            val tensorImage   = TensorImage.fromBitmap(bitmap)
            val results       = detector!!.detect(tensorImage)

            val filteredLabels     = mutableListOf<String>()
            val filteredConfs      = mutableListOf<Float>()

            results.forEach { detection ->
                detection.categories.forEach { cat ->
                    val label = cat.label.lowercase().trim()
                    if (DANGEROUS_LABELS.any { label.contains(it) }) {
                        filteredLabels.add(cat.label)
                        filteredConfs.add(cat.score)
                    }
                }
            }

            onObjectsDetected(filteredLabels, filteredConfs)
        } catch (e: Exception) {
            Log.e(TAG, "Object detection failed", e)
        } finally {
            imageProxy.close()
        }
    }
}
