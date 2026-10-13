package com.example.christmastrees.ui.theme

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
    onPrimary = Color.White,
    primaryContainer = ChristmasGreenLight,
    onPrimaryContainer = ChristmasCream,
    
    secondary = ChristmasGold,
    onSecondary = ChristmasDark,
    secondaryContainer = ChristmasGoldLight,
    onSecondaryContainer = ChristmasDark,
    
    tertiary = ChristmasRed,
    onTertiary = Color.White,
    tertiaryContainer = ChristmasRedLight,
    onTertiaryContainer = Color.White,
    
    background = ForestDark,
    onBackground = ChristmasCream,
    
    surface = ChristmasGreenDark,
    onSurface = ChristmasCream,
    surfaceVariant = ChristmasSurfaceDark,
    onSurfaceVariant = ChristmasCream,
    
    surfaceTint = ChristmasGreen,
    inverseSurface = ChristmasGreen,
    inverseOnSurface = ChristmasCream,
    error = ChristmasRed
)

private val LightColorScheme = lightColorScheme(
    primary = ChristmasGreen,
    onPrimary = Color.White,
    primaryContainer = ChristmasGreenLight,
    onPrimaryContainer = Color.White,
    
    secondary = ChristmasGold,
    onSecondary = ChristmasDark,
    secondaryContainer = ChristmasGoldLight,
    onSecondaryContainer = ChristmasDark,
    
    tertiary = ChristmasRed,
    onTertiary = Color.White,
    tertiaryContainer = ChristmasRedLight,
    onTertiaryContainer = Color.White,
    
    background = ChristmasCream,
    onBackground = ChristmasDark,
    
    surface = ChristmasSurface,
    onSurface = ChristmasDark,
    surfaceVariant = ChristmasCream,
    onSurfaceVariant = ChristmasDark,
    
    surfaceTint = ChristmasGreen,
    inverseSurface = ChristmasGreenDark,
    inverseOnSurface = ChristmasCream,
    error = ChristmasRed
)

@Composable
fun ChristmasTreesTheme(
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
