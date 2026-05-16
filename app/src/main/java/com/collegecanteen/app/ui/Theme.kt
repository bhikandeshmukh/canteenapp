package com.collegecanteen.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF5722),
    onPrimary = Color.White,
    secondary = Color(0xFFFF8F00),
    onSecondary = Color.White,
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color.White,
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF1C1008),
    surface = Color.White,
    onSurface = Color(0xFF1C1008),
    surfaceVariant = Color(0xFFF5EDE3),
    onSurfaceVariant = Color(0xFF78909C),
    outline = Color(0xFFD5C5B2)
)

@Composable
fun CollegeCanteenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
