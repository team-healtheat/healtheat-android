
package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.healtheat.app.data.FoodItem
import com.healtheat.app.data.PreparationType
import com.healtheat.app.data.SourceType
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.*
import com.healtheat.app.ui.theme.Palette

@Composable
fun RecipeSheet(
    item: FoodItem,
    source: SourceType,
    vm: HealtheatViewModel,
    onDismiss: () -> Unit
) {
    LaunchedEffect(item.recipeId) {
        if (
            source != SourceType.DINING_OUT &&
            item.recipeId != null
        ) {
            vm.fetchRecipe(item)
        }
    }

    val liveRecipe =
        item.recipeId?.let { vm.recipes[it] } ?: item.recipe

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(18.dp),
            color = Palette.Surface,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                // ==================================
                // 고정 상단: 제목 + X 버튼
                // ==================================

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            end = 12.dp,
                            top = 12.dp,
                            bottom = 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (
                            source == SourceType.DINING_OUT
                        ) {
                            "외식 선택 가이드"
                        } else {
                            "레시피"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "닫기",
                            tint = Palette.TextMuted
                        )
                    }
                }

                HorizontalDivider(
                    color = Palette.Border
                )

                // ==================================
                // 중간: 이 영역만 스크롤
                // ==================================

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = 20.dp,
                            vertical = 18.dp
                        )
                ) {

                    Text(
                        item.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Palette.TextPrimary
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "추천 분량 · ${item.serving}",
                        fontSize = 12.sp,
                        color = Palette.TextMuted
                    )

                    Spacer(Modifier.height(20.dp))

                    if (source == SourceType.DINING_OUT) {

                        Text(
                            "주문 전 확인할 사항",
                            color = Palette.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        listOf(
                            "소스·양념은 별도로 제공 가능한지 확인해 보세요.",
                            "알레르기 유발 재료와 교차접촉 가능성은 식당에 직접 확인해야 해요.",
                            "일부 재료가 불분명하면 먹기 전에 추가로 확인하세요."
                        ).forEachIndexed { index, line ->
                            GuideStep(
                                number = index + 1,
                                text = line
                            )
                        }

                    } else if (
                        item.preparation ==
                        PreparationType.READY_TO_EAT
                    ) {

                        Text(
                            "별도 조리가 필요 없는 음식이에요.",
                            fontSize = 13.sp,
                            color = Palette.TextBody
                        )

                    } else if (
                        vm.loadingRecipe &&
                        item.recipeId != null &&
                        liveRecipe == null
                    ) {

                        CircularProgressIndicator(
                            color = Palette.Primary
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "레시피를 불러오는 중이에요.",
                            fontSize = 12.sp,
                            color = Palette.TextMuted
                        )

                    } else if (liveRecipe != null) {

                        val recipe = liveRecipe

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Palette.Primary,
                                modifier = Modifier.size(17.dp)
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(
                                "약 ${recipe.minutes}분 소요",
                                color = Palette.Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "필요한 재료",
                            color = Palette.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Palette.Background,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(15.dp)
                        ) {
                            recipe.ingredients.forEach { ingredient ->
                                Text(
                                    "• $ingredient",
                                    color = Palette.TextBody,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(
                                        vertical = 4.dp
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        Text(
                            "조리 방법",
                            color = Palette.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(Modifier.height(9.dp))

                        recipe.steps.forEachIndexed { idx, step ->
                            GuideStep(
                                number = idx + 1,
                                text = step
                            )
                        }

                    }

                    Spacer(Modifier.height(20.dp))
                }

                // ==================================
                // 고정 하단: 확인했어요 버튼
                // ==================================

                HorizontalDivider(
                    color = Palette.Border
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.Surface)
                        .padding(
                            horizontal = 20.dp,
                            vertical = 14.dp
                        )
                ) {
                    FilledAction(
                        "확인했어요",
                        onDismiss,
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideStep(
    number: Int,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    Palette.PrimaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                color = Palette.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = Palette.TextBody,
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp)
        )
    }
}
