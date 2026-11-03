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
    val Background = Color(0xFFFAF9F7)
    val White = Color(0xFFFFFFFF)
    val Ink = Color(0xFF26243D)
    val Body = Color(0xFF49465D)
    val Muted = Color(0xFF8C8A9A)
    val Primary = Color(0xFF6855BF)
    val DeepPurple = Color(0xFF4D429A)
    val Lavender = Color(0xFFEEEAFF)
    val Lilac = Color(0xFFDCD5FB)
    val Coral = Color(0xFFF48272)
    val Peach = Color(0xFFFFE7DE)
    val Mint = Color(0xFFE4F2E8)
    val MintInk = Color(0xFF43735A)
    val Sky = Color(0xFFE6F0FB)
    val Sand = Color(0xFFFFF1DE)
    val Border = Color(0xFFEFEDF1)
}

private val AppColors = lightColorScheme(
    primary = Palette.Primary,
    secondary = Palette.Coral,
    background = Palette.Background,
    surface = Palette.White,
    onPrimary = Palette.White,
    onSurface = Palette.Ink,
    onBackground = Palette.Ink,
    onSurfaceVariant = Palette.Body,
    outline = Palette.Border,
    surfaceVariant = Palette.Background,
    surfaceContainerHighest = Palette.Background
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
 * Input text: Ink | placeholder: Muted | focus border/cursor: Primary.
 */
@Composable
fun healtheatTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Palette.Ink,
    unfocusedTextColor = Palette.Ink,
    disabledTextColor = Palette.Muted,
    focusedPlaceholderColor = Palette.Muted,
    unfocusedPlaceholderColor = Palette.Muted,
    focusedLabelColor = Palette.Primary,
    unfocusedLabelColor = Palette.Body,
    focusedBorderColor = Palette.Primary,
    unfocusedBorderColor = Palette.Border,
    cursorColor = Palette.Primary,
    focusedContainerColor = Palette.White,
    unfocusedContainerColor = Palette.White
)
