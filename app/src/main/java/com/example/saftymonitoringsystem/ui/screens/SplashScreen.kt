package com.example.saftymonitoringsystem.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saftymonitoringsystem.ui.theme.DarkBackground
import com.example.saftymonitoringsystem.ui.theme.SafetyBlue
import com.example.saftymonitoringsystem.ui.theme.SafetyBlueLight
import kotlinx.coroutines.delay

/**
 * Animated splash screen shown on cold start.
 * Navigates to the dashboard automatically after [DISPLAY_MS] milliseconds.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {

    val DISPLAY_MS = 2200L

    // ── Animations ────────────────────────────────────────────────────────────
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, animationSpec = tween(700, easing = EaseOutBack))
        logoAlpha.animateTo(1f, animationSpec = tween(500))
        delay(200)
        subtitleAlpha.animateTo(1f, animationSpec = tween(600))
        delay(DISPLAY_MS - 1300)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF0D1A3A),
                        DarkBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Shield icon (text-based for zero-dependency approach)
            Text(
                text = "🛡️",
                fontSize = 72.sp,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SafeGuard AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(
                        colors = listOf(SafetyBlueLight, SafetyBlue)
                    )
                ),
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI-Powered Personal Safety Monitor",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(60.dp))

            PulsingDot(modifier = Modifier.alpha(subtitleAlpha.value))
        }
    }
}

@Composable
private fun PulsingDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(700), RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Text(
        text = "●  ●  ●",
        color = SafetyBlue.copy(alpha = alpha),
        fontSize = 12.sp,
        modifier = modifier
    )
}
