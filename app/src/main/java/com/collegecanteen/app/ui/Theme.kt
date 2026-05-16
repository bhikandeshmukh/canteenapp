package com.collegecanteen.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF126C5A),
    onPrimary = Color.White,
    secondary = Color(0xFF735B00),
    onSecondary = Color.White,
    tertiary = Color(0xFF9B3A34),
    onTertiary = Color.White,
    background = Color(0xFFF8FAF8),
    onBackground = Color(0xFF171D1A),
    surface = Color.White,
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFE3E8E3),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF737970)
)

@Composable
fun CollegeCanteenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
