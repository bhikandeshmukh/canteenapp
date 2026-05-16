package com.collegecanteen.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF126C5A),
    onPrimary = Color.White,
    secondary = Color(0xFFB47B00),
    onSecondary = Color.White,
    tertiary = Color(0xFFE85D4A),
    onTertiary = Color.White,
    background = Color(0xFFF7F8F4),
    onBackground = Color(0xFF17201D),
    surface = Color.White,
    onSurface = Color(0xFF17201D),
    surfaceVariant = Color(0xFFE8ECE7),
    onSurfaceVariant = Color(0xFF4D5651),
    outline = Color(0xFF707871)
)

@Composable
fun CollegeCanteenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
