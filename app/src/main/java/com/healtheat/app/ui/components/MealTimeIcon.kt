package com.healtheat.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.healtheat.app.data.MealType

/** 외부 이미지나 폰트 파일 없이 동일한 벡터 계열 아이콘 사용 */
@Composable
fun MealTimeIcon(
    type: MealType,
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = Color.Unspecified
) {
    val image = when (type) {
        MealType.BREAKFAST -> Icons.Outlined.WbSunny
        MealType.LUNCH -> Icons.Outlined.Restaurant
        MealType.DINNER -> Icons.Outlined.NightsStay
    }
    Icon(imageVector = image, contentDescription = type.label, modifier = modifier, tint = tint)
}
