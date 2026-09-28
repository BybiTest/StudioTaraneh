package com.studiotaraneh.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private val NeonDark = darkColorScheme(primary = Color(0xFFE55CFF), secondary = Color(0xFF7C6CFF), tertiary = Color(0xFF38CFFF), background = Color(0xFF070912), surface = Color(0xFF101322))
private val NeonLight = lightColorScheme(primary = Color(0xFF8A25B8), secondary = Color(0xFF5140B8), tertiary = Color(0xFF087E9C))
private val MidnightDark = darkColorScheme(primary = Color(0xFF55A8FF), secondary = Color(0xFF6D7CFF), tertiary = Color(0xFF42D6FF), background = Color(0xFF05070D), surface = Color(0xFF0B101A))
private val GraphiteDark = darkColorScheme(primary = Color(0xFFD1D5DB), secondary = Color(0xFF9CA3AF), tertiary = Color(0xFFE5E7EB), background = Color(0xFF090909), surface = Color(0xFF171717))
private val PurpleDark = darkColorScheme(primary = Color(0xFFB78CFF), secondary = Color(0xFFE080FF), tertiary = Color(0xFF6CA8FF), background = Color(0xFF090611), surface = Color(0xFF160F20))
private val AmoledDark = darkColorScheme(primary = Color(0xFFFFFFFF), secondary = Color(0xFFBDBDBD), tertiary = Color(0xFF8AB4F8), background = Color.Black, surface = Color(0xFF050505))

@Composable
fun StudioTaranehTheme(theme: String = "Neon Studio", darkMode: String = "system", fontScale: Float = 1f, content: @Composable () -> Unit) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (darkMode) { "light" -> false; "dark" -> true; else -> systemDark }
    val colors = if (!dark) NeonLight else when (theme) {
        "Midnight" -> MidnightDark
        "Graphite" -> GraphiteDark
        "Purple Night" -> PurpleDark
        "AMOLED" -> AmoledDark
        else -> NeonDark
    }
    val base = Typography()
    val scaled = base.copy(
        bodyLarge = base.bodyLarge.copy(fontSize = (base.bodyLarge.fontSize.value * fontScale).sp),
        bodyMedium = base.bodyMedium.copy(fontSize = (base.bodyMedium.fontSize.value * fontScale).sp),
        bodySmall = base.bodySmall.copy(fontSize = (base.bodySmall.fontSize.value * fontScale).sp),
        titleLarge = base.titleLarge.copy(fontSize = (base.titleLarge.fontSize.value * fontScale).sp),
        titleMedium = base.titleMedium.copy(fontSize = (base.titleMedium.fontSize.value * fontScale).sp),
        titleSmall = base.titleSmall.copy(fontSize = (base.titleSmall.fontSize.value * fontScale).sp)
    )
    MaterialTheme(colorScheme = colors, typography = scaled, content = content)
}
