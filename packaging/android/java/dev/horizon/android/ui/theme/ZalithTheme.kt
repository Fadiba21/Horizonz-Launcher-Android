package dev.horizon.android.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.horizon.android.model.ZalithThemePalette

val ZalithTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun HorizonZalithTheme(
    palette: ZalithThemePalette = ZalithThemePalette.MATERIAL_YOU,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        val primary = Color(palette.primaryHex)
        val secondary = Color(palette.secondaryHex)
        val surface = Color(palette.surfaceHex)
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF0A141D),
            primaryContainer = primary.copy(alpha = 0.22f),
            onPrimaryContainer = Color(0xFFE8F8FF),
            secondary = secondary,
            onSecondary = Color(0xFF0A141D),
            secondaryContainer = secondary.copy(alpha = 0.22f),
            onSecondaryContainer = Color(0xFFE0F7FA),
            background = surface,
            onBackground = Color(0xFFF2F6FA),
            surface = surface,
            onSurface = Color(0xFFF2F6FA),
            surfaceVariant = Color(0xFF1E2433),
            onSurfaceVariant = Color(0xFFC4D2E0),
            outline = Color(0xFF333D52)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZalithTypography,
        content = content
    )
}
