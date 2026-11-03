package com.healtheat.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healtheat.app.ui.theme.Palette

@Composable
fun PageHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Palette.Ink)
            if (subtitle != null) {
                Spacer(Modifier.height(5.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
        }
        action?.invoke()
    }
}

@Composable
fun BrandHeader(onOnboarding: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Palette.Background).padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(37.dp).background(Palette.Primary, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text("h", fontWeight = FontWeight.Black, fontSize = 27.sp, color = Color.White)
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 7.dp, bottom = 8.dp).size(5.dp).background(Palette.Coral, CircleShape))
            }
            Column {
                Text("healtheat", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Palette.Ink, letterSpacing = (-0.8).sp)
                Text("EAT WELL, FEEL GOOD", fontSize = 8.sp, color = Palette.Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
        Surface(
            onClick = onOnboarding,
            color = Palette.Lavender,
            shape = RoundedCornerShape(50.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Palette.Primary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("내 건강 설정", fontSize = 11.sp, color = Palette.Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FilledAction(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    color: Color = Palette.Primary, enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(49.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun OutlineAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.height(49.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Palette.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Body)
    ) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun RoundedPanel(modifier: Modifier = Modifier, color: Color = Palette.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.dp, if (color == Palette.White) Palette.Border else Color.Transparent),
        shadowElevation = 0.dp
    ) { Column(Modifier.padding(18.dp), content = content) }
}

@Composable
fun Capsule(text: String, background: Color = Palette.Lavender, foreground: Color = Palette.Primary) {
    Box(Modifier.background(background, RoundedCornerShape(50.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(text, color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun MenuIllustration(emoji: String, background: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(69.dp).background(background, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = 36.sp)
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(37.dp).background(Palette.Background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Palette.Primary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Palette.Ink, fontSize = 13.sp)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Palette.Muted)
        }
        if (onClick != null) Icon(Icons.Outlined.ArrowForward, null, tint = Palette.Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun QuietNote(text: String) {
    Row(
        Modifier.fillMaxWidth().background(Palette.Lavender.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("✦", fontSize = 13.sp, color = Palette.Primary)
        Text(text, fontSize = 11.sp, lineHeight = 17.sp, color = Palette.Body)
    }
}
