package com.cms.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Brand Colors ─────────────────────────────────────────────────────────────

val Primary        = Color(0xFF1A56DB)
val PrimaryDark    = Color(0xFF1E3A8A)
val PrimaryLight   = Color(0xFFEFF6FF)
val Secondary      = Color(0xFF7E3AF2)
val SecondaryLight = Color(0xFFF5F3FF)

val SurfaceBg   = Color(0xFFF9FAFB)
val CardBg      = Color(0xFFFFFFFF)
val BorderColor = Color(0xFFE5E7EB)

// Status Colors
val StatusPending    = Color(0xFFE3A008)
val StatusProgress   = Color(0xFF1A56DB)
val StatusResolved   = Color(0xFF057A55)
val StatusClosed     = Color(0xFF6B7280)

val PendingBg  = Color(0xFFFEF3C7)
val ProgressBg = Color(0xFFDBEAFE)
val ResolvedBg = Color(0xFFD1FAE5)
val ClosedBg   = Color(0xFFF3F4F6)

val DangerRed  = Color(0xFFE02424)
val TextPrimary   = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)
val TextHint      = Color(0xFF9CA3AF)

// ─── Color Scheme ─────────────────────────────────────────────────────────────

private val LightColors = lightColorScheme(
    primary        = Primary,
    onPrimary      = Color.White,
    secondary      = Secondary,
    onSecondary    = Color.White,
    background     = SurfaceBg,
    surface        = CardBg,
    onBackground   = TextPrimary,
    onSurface      = TextPrimary,
    error          = DangerRed,
    onError        = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    outline        = BorderColor
)

@Composable
fun CMSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography  = AppTypography,
        content     = content
    )
}
