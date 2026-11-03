package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.healtheat.app.data.SourceType
import com.healtheat.app.data.PreparationType
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.*
import com.healtheat.app.ui.theme.Palette

@Composable
fun RecipeSheet(item: FoodItem, source: SourceType, vm: HealtheatViewModel, onDismiss: () -> Unit) {
    LaunchedEffect(item.recipeId) {
        if (source != SourceType.DINING_OUT && item.recipeId != null) vm.fetchRecipe(item)
    }
    val liveRecipe = item.recipeId?.let { vm.recipes[it] } ?: item.recipe
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Palette.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(22.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Capsule(if (source == SourceType.DINING_OUT) "외식 선택 가이드" else "FOOD RECIPE", Palette.Lavender, Palette.Primary)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = Palette.Muted)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(item.name, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Palette.Ink)
                Spacer(Modifier.height(6.dp))
                Text("추천 분량 · ${item.serving}", fontSize = 12.sp, color = Palette.Muted)
                Spacer(Modifier.height(18.dp))
                if (source == SourceType.DINING_OUT) {
                    Text("🍽️  주문 전 체크할 것", color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "소스·양념은 별도로 제공 가능한지 확인해 보세요.",
                        "알레르기 유발 재료와 교차접촉 가능성은 식당에 직접 확인해야 해요.",
                        "일부 재료가 불분명하면 먹기 전에 추가로 확인하세요."
                    ).forEachIndexed { index, line -> GuideStep(index + 1, line) }
                } else if (item.preparation == PreparationType.READY_TO_EAT) {
                    Text("별도 조리가 필요 없는 음식이에요.", fontSize = 13.sp, color = Palette.Body)
                    Spacer(Modifier.height(15.dp))
                } else if (vm.loadingRecipe && item.recipeId != null && liveRecipe == null) {
                    CircularProgressIndicator(color = Palette.Primary)
                    Spacer(Modifier.height(8.dp))
                    Text("서버에서 레시피를 불러오는 중…", fontSize = 12.sp, color = Palette.Muted)
                } else if (liveRecipe != null) {
                    val recipe = liveRecipe
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, tint = Palette.Primary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("약 ${recipe.minutes}분 소요", color = Palette.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(17.dp))
                    Text("필요한 재료", color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(9.dp))
                    Column(Modifier.fillMaxWidth().background(Palette.Background, RoundedCornerShape(18.dp)).padding(15.dp)) {
                        recipe.ingredients.forEach { ingredient ->
                            Text("• $ingredient", color = Palette.Body, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("이렇게 만들어봐요", color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(9.dp))
                    recipe.steps.forEachIndexed { idx, step -> GuideStep(idx + 1, step) }
                } else {
                    QuietNote(vm.recipeFailure ?: "이 음식에는 연결된 레시피가 없어요.")
                }
                Spacer(Modifier.height(18.dp))
                QuietNote("참고용 예시이며, 재료·분량·알레르기 안전을 보장하지 않습니다. 건강상 특별한 식이 제한이 있다면 개인별 지침을 우선하세요.")
                Spacer(Modifier.height(20.dp))
                FilledAction("확인했어요", onDismiss, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GuideStep(number: Int, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(26.dp).background(Palette.Lavender, CircleShape), contentAlignment = Alignment.Center) {
            Text(number.toString(), color = Palette.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, fontSize = 12.sp, lineHeight = 19.sp, color = Palette.Body, modifier = Modifier.weight(1f).padding(top = 3.dp))
    }
}
