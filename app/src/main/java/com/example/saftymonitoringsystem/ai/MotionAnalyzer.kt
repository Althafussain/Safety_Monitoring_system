package com.example.saftymonitoringsystem.ai

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Analyses camera frames for suspicious motion / human activity.
 *
 * ── Research approach ─────────────────────────────────────────────────────────
 * Without MediaPipe Pose or MoveNet (both require additional AAR/Python bridge),
 * we implement an Optical-Flow-inspired pixel-difference heuristic that runs
 * entirely on-device with no extra dependencies.
 *
 * The algorithm:
 *  1. Converts each frame to a greyscale histogram.
 *  2. Computes the L1 difference between consecutive histograms.
 *  3. Uses a multi-level threshold to classify motion intensity.
 *  4. Applies temporal smoothing (EMA) to reduce flicker.
 *
 * For a production system, replace this with:
 *   - MoveNet Singlepose Thunder (TFLite) for skeleton key-points
 *   - MediaPipe Pose for fall / fight detection
 *   - A lightweight LSTM action-recognition model
 *
 * Detected activity labels:
 *   Still | Normal Movement | Rapid Movement | Possible Running | Possible Struggle
 */
class MotionAnalyzer(
    private val onMotionAnalyzed: (activity: String, intensity: Float) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG          = "MotionAnalyzer"
        private const val HIST_BINS    = 64       // greyscale histogram bins
        private const val EMA_ALPHA    = 0.3f     // smoothing factor

        // Motion thresholds (tuned empirically; adjust during calibration)
        private const val THRESH_NORMAL  = 0.03f
        private const val THRESH_RAPID   = 0.10f
        private const val THRESH_RUNNING = 0.20f
        private const val THRESH_STRUGGLE = 0.32f
    }

    private var prevHistogram: FloatArray? = null
    private var smoothedDiff  = 0f

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bitmap: Bitmap = imageProxy.toBitmap()
            val histogram      = computeGreyscaleHistogram(bitmap)

            val prev = prevHistogram
            if (prev != null) {
                // L1 distance, normalised to [0..1]
                val rawDiff = l1Distance(prev, histogram) / HIST_BINS.toFloat()

                // Exponential moving average for smoothing
                smoothedDiff = EMA_ALPHA * rawDiff + (1f - EMA_ALPHA) * smoothedDiff

                val (activity, intensity) = classify(smoothedDiff)
                onMotionAnalyzed(activity, intensity)
            }

            prevHistogram = histogram
        } catch (e: Exception) {
            Log.e(TAG, "Motion analysis failed", e)
        } finally {
            imageProxy.close()
        }
    }

    // ── Classification ────────────────────────────────────────────────────────

    private fun classify(diff: Float): Pair<String, Float> = when {
        diff < THRESH_NORMAL   -> "Still"              to diff / THRESH_NORMAL
        diff < THRESH_RAPID    -> "Normal Movement"    to diff / THRESH_RAPID
        diff < THRESH_RUNNING  -> "Rapid Movement"     to diff / THRESH_RUNNING
        diff < THRESH_STRUGGLE -> "Possible Running"   to diff / THRESH_STRUGGLE
        else                   -> "Possible Struggle"  to 1f
    }

    // ── Histogram ─────────────────────────────────────────────────────────────

    private fun computeGreyscaleHistogram(bitmap: Bitmap): FloatArray {
        val hist = FloatArray(HIST_BINS)
        val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
        val pixelCount = scaled.width * scaled.height.toFloat()

        for (y in 0 until scaled.height) {
            for (x in 0 until scaled.width) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16 and 0xFF)
                val g = (pixel shr 8  and 0xFF)
                val b = (pixel        and 0xFF)
                val grey = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                val bin  = (grey * HIST_BINS / 256).coerceIn(0, HIST_BINS - 1)
                hist[bin]++
            }
        }
        // Normalise
        for (i in hist.indices) hist[i] /= pixelCount
        return hist
    }

    private fun l1Distance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += kotlin.math.abs(a[i] - b[i])
        return sum
    }
}
