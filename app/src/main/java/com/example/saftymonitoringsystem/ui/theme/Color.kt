package com.example.saftymonitoringsystem.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Primary Brand ────────────────────────────────────────────────────────────
val SafetyBlue      = Color(0xFF1E6FEB)   // primary actions
val SafetyBlueDark  = Color(0xFF1253B8)
val SafetyBlueLight = Color(0xFF5A9EFF)

// ─── Surface / Background (dark mode first) ───────────────────────────────────
val DarkBackground  = Color(0xFF0A0F1E)   // deep navy – main bg
val DarkSurface     = Color(0xFF111827)   // card surfaces
val DarkSurface2    = Color(0xFF1C2537)   // elevated cards
val DarkOutline     = Color(0xFF2A3550)

// ─── Threat Level Colors ──────────────────────────────────────────────────────
val ThreatSafe      = Color(0xFF22C55E)   // 0-40%   green
val ThreatLow       = Color(0xFFFBBF24)   // 40-60%  amber
val ThreatMedium    = Color(0xFFF97316)   // 60-80%  orange
val ThreatHigh      = Color(0xFFEF4444)   // 80-100% red
val ThreatCritical  = Color(0xFFDC2626)   // 100%    deep red

// ─── Accent / Status ─────────────────────────────────────────────────────────
val AccentPurple    = Color(0xFF7C3AED)
val AccentGreen     = Color(0xFF10B981)
val AccentAmber     = Color(0xFFF59E0B)
val AccentRed       = Color(0xFFEF4444)

// ─── On-Color ─────────────────────────────────────────────────────────────────
val OnDark          = Color(0xFFF1F5F9)
val OnDarkSecondary = Color(0xFF94A3B8)
val OnSurface       = Color(0xFFE2E8F0)

// ─── Legacy M3 mappings ───────────────────────────────────────────────────────
val Purple80    = SafetyBlueLight
val PurpleGrey80 = OnDarkSecondary
val Pink80      = AccentPurple

val Purple40    = SafetyBlue
val PurpleGrey40 = Color(0xFF607C8A)
val Pink40      = AccentPurple

/** Returns the appropriate color for a [0..100] threat level. */
fun threatColor(level: Int): Color = when {
    level < 40  -> ThreatSafe
    level < 60  -> ThreatLow
    level < 80  -> ThreatMedium
    level < 95  -> ThreatHigh
    else        -> ThreatCritical
}