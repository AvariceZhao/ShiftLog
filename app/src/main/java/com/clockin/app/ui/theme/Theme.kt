package com.clockin.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ShiftLogColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = NightBase,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberSoft,
    secondary = CyanSecondary,
    onSecondary = NightBase,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanSoft,
    background = NightBase,
    onBackground = TextPrimary,
    surface = NightSurface,
    onSurface = TextPrimary,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = NightBorder,
    outlineVariant = NightBorder,
    error = StatusMissed,
    onError = TextPrimary,
)

@Composable
fun ClockInTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = NightBase.toArgb()
        window.navigationBarColor = NightSurface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    MaterialTheme(
        colorScheme = ShiftLogColorScheme,
        typography = ShiftLogTypography,
        shapes = ShiftLogShapes,
        content = content,
    )
}
