package com.healtheat.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp
import com.healtheat.app.R

object Palette {
    /** Semantic app colours — use these instead of screen-specific colour literals. */
    val Primary = Color(0xFF24734B)
    val PrimaryDark = Color(0xFF17563B)
    val PrimaryContainer = Color(0xFFE7F3EA)
    val Secondary = Color(0xFF4A8C70)
    val Background = Color(0xFFF5FAF6)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1D3328)
    val TextBody = Color(0xFF42564C)
    val TextMuted = Color(0xFF698172)
    val Border = Color(0xFFDCE9DE)
    val Mint = Color(0xFFD8F0DF)
    val MintInk = Color(0xFF24734B)
    val Danger = Color(0xFFC34A4F)
    val DangerContainer = Color(0xFFFCE8E8)
    val Warning = Color(0xFFB77926)
    val WarningContainer = Color(0xFFFFF3E3)
}

private val AppColors = lightColorScheme(
    primary = Palette.Primary,
    secondary = Palette.Secondary,
    background = Palette.Background,
    surface = Palette.Surface,
    onPrimary = Palette.Surface,
    primaryContainer = Palette.PrimaryContainer,
    onPrimaryContainer = Palette.PrimaryDark,
    secondaryContainer = Palette.Mint,
    onSecondaryContainer = Palette.PrimaryDark,
    onSurface = Palette.TextPrimary,
    onBackground = Palette.TextPrimary,
    onSurfaceVariant = Palette.TextBody,
    outline = Palette.Border,
    surfaceVariant = Palette.PrimaryContainer,
    surfaceContainerHighest = Palette.PrimaryContainer,
    error = Palette.Danger,
    errorContainer = Palette.DangerContainer,
    onError = Palette.Surface,
    onErrorContainer = Palette.Danger
)

private val AppFont = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)
private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = AppFont, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = AppFont, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 12.sp)
)

@Composable
fun HealtheatTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}

/** All Compose OutlinedTextFields use one accessible, branded palette.
 * Input text: primary text | placeholder: muted text | focus border/cursor: primary.
 */
@Composable
fun healtheatTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Palette.TextPrimary,
    unfocusedTextColor = Palette.TextPrimary,
    disabledTextColor = Palette.TextMuted,
    focusedPlaceholderColor = Palette.TextMuted,
    unfocusedPlaceholderColor = Palette.TextMuted,
    focusedLabelColor = Palette.Primary,
    unfocusedLabelColor = Palette.TextBody,
    focusedBorderColor = Palette.Primary,
    unfocusedBorderColor = Palette.Border,
    cursorColor = Palette.Primary,
    focusedContainerColor = Palette.Surface,
    unfocusedContainerColor = Palette.Surface
)
