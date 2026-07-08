package com.example.saftymonitoringsystem.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Dark scheme (primary UI) ─────────────────────────────────────────────────
private val AppDarkColorScheme = darkColorScheme(
    primary            = SafetyBlue,
    onPrimary          = Color.White,
    primaryContainer   = DarkSurface2,
    onPrimaryContainer = SafetyBlueLight,

    secondary          = AccentPurple,
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFF2D1A5E),
    onSecondaryContainer = Color(0xFFDDD6FE),

    error              = AccentRed,
    errorContainer     = Color(0xFF7F1D1D),
    onErrorContainer   = Color(0xFFFECACA),

    background         = DarkBackground,
    onBackground       = OnDark,
    surface            = DarkSurface,
    onSurface          = OnSurface,
    surfaceVariant     = DarkSurface2,
    onSurfaceVariant   = OnDarkSecondary,
    outline            = DarkOutline,
)

// ─── Light scheme (fallback) ──────────────────────────────────────────────────
private val AppLightColorScheme = lightColorScheme(
    primary            = SafetyBlue,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFDBEAFF),
    onPrimaryContainer = SafetyBlueDark,

    secondary          = AccentPurple,
    onSecondary        = Color.White,

    background         = Color(0xFFF0F4FF),
    onBackground       = Color(0xFF0F172A),
    surface            = Color(0xFFFFFFFF),
    onSurface          = Color(0xFF1E293B),
    surfaceVariant     = Color(0xFFE2E8F0),
    onSurfaceVariant   = Color(0xFF475569),
    outline            = Color(0xFFCBD5E1),
)

@Composable
fun SaftyMonitoringSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme,
        typography  = Typography,
        content     = content
    )
}