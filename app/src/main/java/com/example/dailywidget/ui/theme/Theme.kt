package com.example.dailywidget.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ==================== Light Color Scheme ====================
private val LightColorScheme = lightColorScheme(
    // Primary (파란색)
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueLight.copy(alpha = 0.15f),
    onPrimaryContainer = Blue,

    // Secondary (연한 파란색)
    secondary = BlueLight,
    onSecondary = Color.White,
    secondaryContainer = BlueVeryLight.copy(alpha = 0.15f),
    onSecondaryContainer = BlueLight,

    // Tertiary (회색)
    tertiary = Gray600,
    onTertiary = Color.White,
    tertiaryContainer = Gray200,
    onTertiaryContainer = Gray700,

    // Background (순백색)
    background = BackgroundLight,
    onBackground = TextPrimary,

    // Surface (순백색 + #F5F5F5 변형)
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,        // ⭐ #F5F5F5
    onSurfaceVariant = TextSecondary,
    surfaceTint = Blue,

    // 추가 Surface
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = Gray50,
    surfaceContainerHighest = Gray100,
    surfaceContainerLow = Color.White,
    surfaceContainerLowest = Color.White,

    // Error
    error = Red,
    onError = Color.White,
    errorContainer = Red.copy(alpha = 0.1f),
    onErrorContainer = Red,

    // Outline (구분선)
    outline = OutlineLight,
    outlineVariant = DividerLight,

    // Inverse
    inverseSurface = Gray800,
    inverseOnSurface = Color.White,
    inversePrimary = BlueLight,

    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

// ==================== Dark Color Scheme ====================
private val DarkColorScheme = darkColorScheme(
    // Primary
    primary = BlueDark,
    onPrimary = Color.White,
    primaryContainer = BlueDark.copy(alpha = 0.3f),
    onPrimaryContainer = BlueLight,

    // Secondary
    secondary = BlueVeryLight,
    onSecondary = Gray900,
    secondaryContainer = BlueVeryLight.copy(alpha = 0.3f),
    onSecondaryContainer = BlueVeryLight,

    // Tertiary
    tertiary = Gray400,
    onTertiary = Gray900,
    tertiaryContainer = Gray700,
    onTertiaryContainer = Gray300,

    // Background
    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    // Surface
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = BlueDark,

    // 추가 Surface
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = Gray700,
    surfaceContainerHighest = Gray600,
    surfaceContainerLow = Gray900,
    surfaceContainerLowest = Color.Black,

    // Error
    error = Red,
    onError = Color.White,
    errorContainer = Red.copy(alpha = 0.2f),
    onErrorContainer = Red,

    // Outline
    outline = OutlineDark,
    outlineVariant = DividerDark,

    // Inverse
    inverseSurface = Gray200,
    inverseOnSurface = Gray900,
    inversePrimary = Blue,

    // Scrim
    scrim = Color.Black.copy(alpha = 0.5f)
)

@Composable
fun DailyWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}