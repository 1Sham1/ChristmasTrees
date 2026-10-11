package com.example.christmastrees.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = ChristmasGreen,
    secondary = ChristmasGold,
    tertiary = ChristmasRed,
    background = ChristmasDark,
    surface = ChristmasGreen,
    onPrimary = ChristmasGold,
    onSecondary = ChristmasDark,
    onTertiary = Color.White,
    onBackground = ChristmasCream,
    onSurface = ChristmasCream
)

private val LightColorScheme = lightColorScheme(
    primary = ChristmasGreen,
    secondary = ChristmasGold,
    tertiary = ChristmasRed,
    background = ChristmasCream,
    surface = ChristmasGreenLight,
    onPrimary = Color.White,
    onSecondary = ChristmasDark,
    onTertiary = Color.White,
    onBackground = ChristmasDark,
    onSurface = Color.White
)

@Composable
fun ChristmasTreesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
