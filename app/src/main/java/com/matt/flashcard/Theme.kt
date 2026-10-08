package com.matt.flashcard

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Colours from the "Jadran Lexicon" design system: Adriatic cobalt accent, soft light and OLED dark surfaces. */
data class FlashColors(
    val canvas: Color,
    val surface: Color,
    val inset: Color,
    val ink: Color,
    val inkSecondary: Color,
    val divider: Color,
    val accent: Color,
    val onAccent: Color,
    val extraBg: Color,
    val extraText: Color,
    val extraBorder: Color,
    val danger: Color,
)

private fun light(canvas: Long, inset: Long, ink: Long, inkSecondary: Long, divider: Long, accent: Long) = FlashColors(
    canvas = Color(canvas), surface = Color.White, inset = Color(inset),
    ink = Color(ink), inkSecondary = Color(inkSecondary), divider = Color(divider),
    accent = Color(accent), onAccent = Color.White,
    extraBg = Color(0xFFFFFBEB), extraText = Color(0xFFB45309), extraBorder = Color(0xFFFDE68A),
    danger = Color(0xFFDC2626),
)

/** The app's colour themes; [dark] only decides the status-bar icon colour and the Material scheme. */
enum class AppTheme(val label: String, val dark: Boolean, val colors: FlashColors) {
    Daylight("Daylight", false, light(0xFFFBFBFA, 0xFFF3F3F0, 0xFF191B1F, 0xFF475569, 0xFFE4E6EA, 0xFF1D4ED8)),
    Ocean("Ocean", false, light(0xFFEAF6FB, 0xFFD8EDF7, 0xFF0B2540, 0xFF4A6A85, 0xFFC5E0EF, 0xFF0284C7)),
    Forest("Forest", false, light(0xFFEEF5EE, 0xFFDDEBDD, 0xFF14301C, 0xFF4F6B56, 0xFFCADBCB, 0xFF15803D)),
    Sunset("Sunset", false, light(0xFFFFF4EA, 0xFFFDE6D2, 0xFF3B1F12, 0xFF7A5A47, 0xFFF3D7BF, 0xFFEA580C)),
    Lavender("Lavender", false, light(0xFFF4F0FB, 0xFFE8E0F7, 0xFF25193F, 0xFF6B5B8A, 0xFFDDD2F0, 0xFF7C3AED)),
    Midnight(
        "Midnight", true,
        FlashColors(
            canvas = Color(0xFF090A0D), surface = Color(0xFF12141A), inset = Color(0xFF1A1D26),
            ink = Color(0xFFF8FAFC), inkSecondary = Color(0xFF94A3B8), divider = Color(0xFF1E2430),
            accent = Color(0xFF3B82F6), onAccent = Color.White,
            extraBg = Color(0xFF281F08), extraText = Color(0xFFF59E0B), extraBorder = Color(0xFFF59E0B),
            danger = Color(0xFFF87171),
        ),
    ),
    ;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: Daylight
    }
}

val LocalFlashColors = staticCompositionLocalOf { AppTheme.Daylight.colors }

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: Int) = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
)

/** Type scale from the design system. */
object FlashType {
    val headlineLg = TextStyle(fontFamily = jakarta(700), fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.02).em)
    val headlineMd =TextStyle(fontFamily = jakarta(600), fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em)
    val headlineSm = TextStyle(fontFamily = jakarta(600), fontSize = 18.sp, lineHeight = 26.sp)
    val bodyXl = TextStyle(fontFamily = jakarta(500), fontSize = 24.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em)
    val bodyMd = TextStyle(fontFamily = jakarta(400), fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.01.em)
    val labelLg = TextStyle(fontFamily = jakarta(600), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.02.em)
    val labelMd = TextStyle(fontFamily = jakarta(600), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em)
    val labelSm = TextStyle(fontFamily = jakarta(700), fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.06.em)
}

@Composable
fun FlashTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val colors = theme.colors
    val scheme = (if (theme.dark) darkColorScheme() else lightColorScheme()).copy(
        primary = colors.accent, onPrimary = colors.onAccent,
        surface = colors.surface, onSurface = colors.ink, onSurfaceVariant = colors.inkSecondary,
        surfaceContainerHigh = colors.surface, background = colors.canvas, onBackground = colors.ink,
        outline = colors.divider, outlineVariant = colors.divider,
    )
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !theme.dark
            isAppearanceLightNavigationBars = !theme.dark
        }
    }
    CompositionLocalProvider(LocalFlashColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
