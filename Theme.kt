package com.iosexperience.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IosBlue = Color(0xFF007AFF)

@Composable
fun IosTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = IosBlue, background = Color(0xFF000000), surface = Color(0xFF1C1C1E))
    } else {
        lightColorScheme(primary = IosBlue, background = Color(0xFFF2F2F7), surface = Color.White)
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
