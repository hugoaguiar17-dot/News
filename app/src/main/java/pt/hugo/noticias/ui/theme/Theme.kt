package pt.hugo.noticias.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BreakingRed = Color(0xFFC8102E)

private val Light = lightColorScheme(
    primary = BreakingRed,
    onPrimary = Color.White,
    secondary = Color(0xFF006847),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB3B0),
    onPrimary = Color(0xFF680011),
    secondary = Color(0xFF7FD8A8),
)

@Composable
fun NoticiasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
