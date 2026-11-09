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
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(5.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
            }
        }
        action?.invoke()
    }
}

@Composable
fun BrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.Background)
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(37.dp).background(Palette.Primary, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("h", fontWeight = FontWeight.Black, fontSize = 27.sp, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text("healtheat", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Palette.TextPrimary)
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
        shape = RoundedCornerShape(10.dp),
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
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Palette.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.TextBody)
    ) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun RoundedPanel(
    modifier: Modifier = Modifier,
    color: Color = Palette.Surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (color == Palette.Surface) Palette.Border
            else Color.Transparent
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun Capsule(text: String, background: Color = Palette.PrimaryContainer, foreground: Color = Palette.Primary) {
    Box(Modifier.background(background, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(text, color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun MenuIllustration(emoji: String, background: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(69.dp).background(background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
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
            Text(title, fontWeight = FontWeight.SemiBold, color = Palette.TextPrimary, fontSize = 13.sp)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Palette.TextMuted)
        }
        if (onClick != null) Icon(Icons.Outlined.ArrowForward, null, tint = Palette.TextMuted, modifier = Modifier.size(18.dp))
    }
}
