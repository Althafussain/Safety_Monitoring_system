package com.example.saftymonitoringsystem.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saftymonitoringsystem.ui.SafetyViewModel
import com.example.saftymonitoringsystem.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SafetyViewModel,
    onStartMonitoring: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState  by viewModel.uiState.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SafeGuard AI",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OnDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, "Profile", tint = OnDark)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = OnDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Threat Level Gauge ─────────────────────────────────────────────
            ThreatGaugeCard(
                threatLevel = uiState.threatLevel,
                statusMessage = uiState.statusMessage,
                isMonitoring = uiState.isMonitoring
            )

            // ── Quick Stats Row ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    label = "Contacts",
                    value = contacts.size.toString(),
                    color = SafetyBlue
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Warning,
                    label = "Incidents",
                    value = incidents.size.toString(),
                    color = AccentAmber
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Notifications,
                    label = "Threshold",
                    value = "${settings.alertThreshold}%",
                    color = AccentPurple
                )
            }

            // ── Main Action ───────────────────────────────────────────────────
            StartMonitoringButton(
                isMonitoring = uiState.isMonitoring,
                onClick = onStartMonitoring
            )

            // ── SOS Panic Button ──────────────────────────────────────────────
            PanicButton(viewModel = viewModel, uiState = uiState)

            // ── Nav Grid ──────────────────────────────────────────────────────
            Text(
                "Quick Access",
                style = MaterialTheme.typography.labelLarge,
                color = OnDarkSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ContactPhone,
                    title = "Contacts",
                    subtitle = "Manage trusted contacts",
                    onClick = onNavigateToContacts,
                    gradientStart = Color(0xFF1E3A6E),
                    gradientEnd   = Color(0xFF1253B8)
                )
                NavCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    title = "History",
                    subtitle = "View past incidents",
                    onClick = onNavigateToHistory,
                    gradientStart = Color(0xFF3B1F6E),
                    gradientEnd   = Color(0xFF7C3AED)
                )
            }

            // ── Alert sent banner ─────────────────────────────────────────────
            if (uiState.alertSent) {
                AlertBanner { viewModel.resetAlert() }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun ThreatGaugeCard(
    threatLevel: Int,
    statusMessage: String,
    isMonitoring: Boolean
) {
    val color = threatColor(threatLevel)
    val animatedProgress by animateFloatAsState(
        targetValue = threatLevel / 100f,
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "threatProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Threat Level",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnDark
                )
                StatusBadge(isMonitoring)
            }

            Spacer(Modifier.height(16.dp))

            // Circular gauge using a linear bar + big number
            Box(contentAlignment = Alignment.Center) {
                CircularProgressBar(
                    progress  = animatedProgress,
                    size      = 140.dp,
                    thickness = 14.dp,
                    color     = color,
                    trackColor = DarkSurface2
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$threatLevel%",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 36.sp
                        ),
                        color = color
                    )
                    Text(
                        when {
                            threatLevel < 40 -> "SAFE"
                            threatLevel < 60 -> "LOW"
                            threatLevel < 80 -> "MEDIUM"
                            else             -> "HIGH"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = OnDarkSecondary
            )
        }
    }
}

@Composable
private fun CircularProgressBar(
    progress: Float,
    size: Dp,
    thickness: Dp,
    color: Color,
    trackColor: Color
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color    = trackColor,
            strokeWidth = thickness
        )
        CircularProgressIndicator(
            progress    = { progress },
            modifier    = Modifier.fillMaxSize(),
            color       = color,
            strokeWidth = thickness
        )
    }
}

@Composable
private fun StatusBadge(isMonitoring: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "badgeAlpha"
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isMonitoring) ThreatSafe.copy(alpha = 0.2f) else DarkSurface2
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMonitoring) ThreatSafe.copy(alpha = alpha) else OnDarkSecondary
                    )
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (isMonitoring) "LIVE" else "IDLE",
                style = MaterialTheme.typography.labelSmall,
                color = if (isMonitoring) ThreatSafe else OnDarkSecondary
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface2),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = OnDark)
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnDarkSecondary)
        }
    }
}

@Composable
private fun StartMonitoringButton(isMonitoring: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (isMonitoring) 0.97f else 1f, label = "btnScale")

    Button(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale),
        shape  = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isMonitoring) ThreatMedium else SafetyBlue
        )
    ) {
        Icon(
            if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
            null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (isMonitoring) "Stop Monitoring" else "Start AI Monitoring",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun PanicButton(viewModel: SafetyViewModel, uiState: com.example.saftymonitoringsystem.ui.SafetyUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "panic")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "panicPulse"
    )

    if (uiState.panicActive) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🚨 SOS Alert in ${uiState.panicCountdown}s",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.cancelPanic() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Cancel SOS")
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = { viewModel.startPanic() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .scale(pulseScale),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
            border = androidx.compose.foundation.BorderStroke(2.dp, AccentRed)
        ) {
            Text("🚨  SOS Panic Button", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun NavCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    gradientStart: Color,
    gradientEnd: Color
) {
    Card(
        modifier = modifier,
        onClick  = onClick,
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(gradientStart, gradientEnd)))
                .padding(16.dp)
        ) {
            Column {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun AlertBanner(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚠️", fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Emergency Alert Sent", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("All contacts have been notified.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFCA5A5))
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val EaseOutBack    = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
