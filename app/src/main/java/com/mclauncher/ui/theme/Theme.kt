package com.mclauncher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// MC 主题色
val MC_GREEN = Color(0xFF4DBD2D)
val MC_GREEN_DARK = Color(0xFF3A9121)
val MC_YELLOW = Color(0xFFFFB020)
val MC_BLACK = Color(0xFF1A1A1A)
val MC_PANEL = Color(0xFF242424)
val MC_BORDER = Color(0xFF333333)
val MC_TEXT_PRIMARY = Color(0xFFFFFFFF)
val MC_TEXT_SECONDARY = Color(0xFF999999)
val ACCENT_BLUE = Color(0xFF3B82F6)
val ACCENT_PURPLE = Color(0xFF8B5CF6)
val ACCENT_ORANGE = Color(0xFFF97316)

private val DarkColorScheme = darkColorScheme(
    primary = MC_GREEN,
    onPrimary = Color.White,
    primaryContainer = MC_GREEN_DARK,
    secondary = ACCENT_BLUE,
    onSecondary = Color.White,
    tertiary = ACCENT_PURPLE,
    background = MC_BLACK,
    onBackground = MC_TEXT_PRIMARY,
    surface = MC_PANEL,
    onSurface = MC_TEXT_PRIMARY,
    surfaceVariant = MC_PANEL,
    onSurfaceVariant = MC_TEXT_SECONDARY,
    outline = MC_BORDER
)

@Composable
fun MCLauncherTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MC_BLACK.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
