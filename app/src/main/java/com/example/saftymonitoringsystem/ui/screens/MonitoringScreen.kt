package com.example.saftymonitoringsystem.ui.screens

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.saftymonitoringsystem.ai.FaceAnalyzer
import com.example.saftymonitoringsystem.ai.MotionAnalyzer
import com.example.saftymonitoringsystem.ai.ObjectAnalyzer
import com.example.saftymonitoringsystem.ui.SafetyViewModel
import com.example.saftymonitoringsystem.ui.theme.*
import java.util.concurrent.Executors

/**
 * Live camera monitoring screen.
 *
 * Front camera → FaceAnalyzer + MotionAnalyzer (emotion & motion)
 * Rear camera  → ObjectAnalyzer + MotionAnalyzer (dangerous objects)
 *
 * All analysis results are pushed to [SafetyViewModel] which runs ThreatEngine
 * and triggers alerts autonomously.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    viewModel: SafetyViewModel,
    onBack: () -> Unit
) {
    val uiState       by viewModel.uiState.collectAsState()
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var useFrontCamera by remember { mutableStateOf(true) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }

    // ── Analyzers ─────────────────────────────────────────────────────────────
    val faceAnalyzer = remember {
        FaceAnalyzer { emotion, conf, breakdown ->
            viewModel.updateEmotion(emotion, conf, breakdown)
        }
    }
    val objectAnalyzer = remember {
        ObjectAnalyzer(context) { labels, confs ->
            viewModel.updateObjects(labels, confs)
        }
    }
    val motionAnalyzer = remember {
        MotionAnalyzer { activity, intensity ->
            viewModel.updateMotion(activity, intensity)
        }
    }

    // Binds camera whenever selector changes
    val cameraProviderState = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(cameraSelector) {
        viewModel.setMonitoring(true)
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setMonitoring(false)
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Live Monitoring",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = OnDark)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        useFrontCamera = !useFrontCamera
                        cameraSelector = if (useFrontCamera)
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        else
                            CameraSelector.DEFAULT_BACK_CAMERA
                    }) {
                        Icon(Icons.Default.Cameraswitch, "Switch Camera", tint = OnDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Camera preview ────────────────────────────────────────────────
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    bindCamera(
                        ctx, previewView, lifecycleOwner, cameraSelector,
                        cameraExecutor, faceAnalyzer, objectAnalyzer, motionAnalyzer, useFrontCamera
                    )
                    previewView
                },
                update = { previewView ->
                    bindCamera(
                        context, previewView, lifecycleOwner, cameraSelector,
                        cameraExecutor, faceAnalyzer, objectAnalyzer, motionAnalyzer, useFrontCamera
                    )
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── Gradient scrim for readability ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(DarkBackground.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .align(Alignment.TopCenter)
            )

            // ── Detection overlay ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Threat bar
                ThreatOverlayBar(uiState.threatLevel)

                // Detection chips row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DetectionChip(
                        icon = "😐",
                        label = uiState.latestDetection.emotion,
                        subLabel = "${(uiState.latestDetection.emotionConfidence * 100).toInt()}%",
                        color = emotionColor(uiState.latestDetection.emotion)
                    )
                    if (uiState.latestDetection.detectedObjects.isNotEmpty()) {
                        DetectionChip(
                            icon = "🔍",
                            label = uiState.latestDetection.detectedObjects.first(),
                            subLabel = "DETECTED",
                            color = AccentRed
                        )
                    }
                    DetectionChip(
                        icon = "🏃",
                        label = uiState.latestDetection.motionActivity,
                        subLabel = "${(uiState.latestDetection.motionIntensity * 100).toInt()}%",
                        color = motionColor(uiState.latestDetection.motionActivity)
                    )
                }

                // Camera mode indicator
                Text(
                    if (useFrontCamera) "📷 Front Camera: Face Analysis" else "📷 Rear Camera: Object Detection",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnDarkSecondary
                )
            }

            // ── Alert overlay (bottom) ────────────────────────────────────────
            if (uiState.alertSent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    AlertOverlayCard { viewModel.resetAlert() }
                }
            }

            // ── SOS button (bottom-right) ─────────────────────────────────────
            if (!uiState.panicActive) {
                FloatingActionButton(
                    onClick = { viewModel.startPanic() },
                    containerColor = AccentRed,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Text("SOS", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ── Camera binding helper ─────────────────────────────────────────────────────

private fun bindCamera(
    context: android.content.Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraSelector: CameraSelector,
    executor: java.util.concurrent.ExecutorService,
    faceAnalyzer: FaceAnalyzer,
    objectAnalyzer: ObjectAnalyzer,
    motionAnalyzer: MotionAnalyzer,
    useFront: Boolean
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(executor) { imageProxy ->
                    // Motion runs on both cameras; face/object depends on camera
                    if (useFront) {
                        faceAnalyzer.analyze(imageProxy)
                        // Note: motion uses a second stream in research; here we reuse front
                    } else {
                        objectAnalyzer.analyze(imageProxy)
                    }
                }
            }

        // Separate motion analysis on any available frame
        val motionAnalyzerUseCase = ImageAnalysis.Builder()
            .setTargetResolution(Size(320, 240))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, motionAnalyzer) }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalyzer, motionAnalyzerUseCase
            )
        } catch (_: Exception) { /* camera in use or unavailable */ }
    }, ContextCompat.getMainExecutor(context))
}

// ── Overlay sub-composables ───────────────────────────────────────────────────

@Composable
private fun ThreatOverlayBar(threatLevel: Int) {
    val color = threatColor(threatLevel)
    val progress by animateFloatAsState(
        targetValue = threatLevel / 100f,
        animationSpec = tween(400),
        label = "monitorProgress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Threat Level", style = MaterialTheme.typography.labelMedium, color = OnDark)
            Text(
                "$threatLevel%",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = DarkOutline
        )
    }
}

@Composable
private fun DetectionChip(
    icon: String,
    label: String,
    subLabel: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = OnDark, maxLines = 1)
                Text(subLabel, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun AlertOverlayCard(onReset: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC7F1D1D)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🚨", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("ALERT SENT", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("Emergency contacts notified with location.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFCA5A5))
            }
            TextButton(onClick = onReset) { Text("Dismiss", color = Color.White) }
        }
    }
}

// ── Color helpers ─────────────────────────────────────────────────────────────

private fun emotionColor(emotion: String): Color = when (emotion) {
    "Happy"    -> ThreatSafe
    "Neutral"  -> OnDarkSecondary
    "Surprise" -> AccentAmber
    "Fear", "Sad", "Angry", "Disgust" -> AccentRed
    else       -> OnDarkSecondary
}

private fun motionColor(activity: String): Color = when {
    activity.contains("Struggle") -> AccentRed
    activity.contains("Running")  -> ThreatMedium
    activity.contains("Rapid")    -> ThreatLow
    else                          -> ThreatSafe
}
