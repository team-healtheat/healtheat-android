package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healtheat.app.data.*
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.*
import com.healtheat.app.ui.theme.Palette
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(vm: HealtheatViewModel, onRecipe: (FoodItem, SourceType) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 21.dp).padding(top = 13.dp, bottom = 27.dp),
        verticalArrangement = Arrangement.spacedBy(19.dp)
    ) {
        HomeHero(vm)
        if (!vm.hasPlan) {
            if (vm.generating || vm.checkingPlan) GeneratingCard() else GenerateEmptyCard(onGenerate = vm::generatePlan)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("오늘의 식탁", style = MaterialTheme.typography.titleLarge, color = Palette.Ink)
                    Text("나를 위해 준비한 세 끼", fontSize = 12.sp, color = Palette.Muted)
                }
                Capsule("AI 맞춤 추천 ✦", Palette.Peach, Palette.Coral)
            }
            MealType.entries.forEach { type ->
                vm.meals[type]?.let { meal ->
                    MealCard(
                        meal = meal,
                        onRecipe = onRecipe,
                        onRegenerate = { vm.regenerate(type) },
                        onRecord = { vm.recordRecommended(type) },
                        regenerating = vm.busyMeal == type,
                        recorded = vm.records.any { it.date == vm.today && it.type == type },
                        recording = vm.recording
                    )
                }
            }
            QuietNote("이 식단은 AI가 생성한 결과입니다. 알레르기 유발 재료나 외식 중 숨은 재료는 실제로 확인이 필요하며, 식단 추천이 의료적 안전을 보장하지는 않습니다.")
        }
        Spacer(Modifier.height(3.dp))
    }
}

@Composable
private fun HomeHero(vm: HealtheatViewModel) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Palette.Primary, Palette.DeepPurple)))
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 45.dp, y = (-30).dp).size(180.dp)
                .background(Color.White.copy(alpha = 0.065f), CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 34.dp, y = 75.dp).size(180.dp)
                .background(Palette.Coral.copy(alpha = 0.21f), CircleShape)
        )
        Text("🥗", fontSize = 83.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 13.dp, bottom = 10.dp))
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                vm.today.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)).uppercase(),
                fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f), fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp
            )
            Text("${vm.profile.name}님,\n오늘도 맛있게!", fontSize = 25.sp, lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.profile.factors.filter { it.isActive }.take(2).forEach { factor ->
                    Box(Modifier.background(Color.White.copy(alpha = 0.19f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(factor.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text("나만의 건강 루틴, 한 끼부터 시작해요", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun GenerateEmptyCard(onGenerate: () -> Unit) {
    RoundedPanel(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(9.dp))
        Box(Modifier.align(Alignment.CenterHorizontally).size(92.dp).background(Palette.Lavender, CircleShape), contentAlignment = Alignment.Center) {
            Text("🍽️", fontSize = 46.sp)
            Box(Modifier.align(Alignment.TopEnd).size(27.dp).background(Palette.Peach, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Palette.Coral, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(19.dp))
        Text("오늘의 식단을 만들어볼까요?", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Ink)
        Spacer(Modifier.height(7.dp))
        Text("건강 상태와 음식 취향을 바탕으로\n아침·점심·저녁을 추천해 드려요.", Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 19.sp, color = Palette.Muted)
        Spacer(Modifier.height(22.dp))
        FilledAction("나만의 하루 식단 만들기", onGenerate, Modifier.fillMaxWidth(), Palette.Coral, icon = Icons.Outlined.AutoAwesome)
        Spacer(Modifier.height(8.dp))
        Text("요청할 때만 생성하고, 저장된 식단을 다시 보여드려요", Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, fontSize = 10.sp, color = Palette.Muted)
    }
}

@Composable
private fun GeneratingCard() {
    RoundedPanel(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(17.dp))
        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).size(43.dp), color = Palette.Primary, strokeWidth = 4.dp)
        Spacer(Modifier.height(21.dp))
        Text("오늘의 식탁을 준비하는 중…", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Palette.Ink)
        Spacer(Modifier.height(7.dp))
        Text("건강정보와 취향을 반영하고 있어요", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            fontSize = 12.sp, color = Palette.Muted)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun MealCard(
    meal: Meal, onRecipe: (FoodItem, SourceType) -> Unit,
    onRegenerate: () -> Unit, onRecord: () -> Unit,
    regenerating: Boolean, recorded: Boolean, recording: Boolean
) {
    val illustration = when(meal.type) {
        MealType.BREAKFAST -> Palette.Sand
        MealType.LUNCH -> Palette.Mint
        MealType.DINNER -> Palette.Lavender
    }
    RoundedPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            MenuIllustration(meal.type.emoji, illustration)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.type.label, fontSize = 11.sp, color = Palette.Muted, fontWeight = FontWeight.SemiBold)
                    Capsule(if (meal.source == SourceType.DINING_OUT) "외식·배달" else "집밥", 
                        if (meal.source == SourceType.DINING_OUT) Palette.Peach else Palette.Mint,
                        if (meal.source == SourceType.DINING_OUT) Palette.Coral else Palette.MintInk)
                }
                Text(meal.headline, fontSize = 16.sp, color = Palette.Ink, fontWeight = FontWeight.Bold)
                Text("${meal.items.size}가지 메뉴  ·  추천 v${meal.version}", fontSize = 11.sp, color = Palette.Muted)
            }
        }
        Spacer(Modifier.height(15.dp))
        Column(
            Modifier.fillMaxWidth().background(Palette.Background, RoundedCornerShape(17.dp)).padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            meal.items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(Palette.Coral, CircleShape))
                    Spacer(Modifier.width(9.dp))
                    Text(item.name, fontSize = 12.sp, color = Palette.Body, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(item.serving, fontSize = 10.sp, color = Palette.Muted)
                    Spacer(Modifier.width(5.dp))
                    TextButton(onClick = { onRecipe(item, meal.source) }, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.height(30.dp)) {
                        Text(if (meal.source == SourceType.DINING_OUT) "가이드" else "레시피", fontSize = 10.sp, color = Palette.Primary, fontWeight = FontWeight.Bold)
                        Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(12.dp), tint = Palette.Primary)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("✦  ${meal.reason}", fontSize = 11.sp, lineHeight = 17.sp, color = Palette.Body)
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineAction(if (regenerating) "추천 중…" else "다시 추천", onRegenerate,
                Modifier.weight(1f), Icons.Outlined.Refresh)
            FilledAction(if (recording) "저장 중…" else if (recorded) "기록 완료" else "먹었어요!", onRecord, Modifier.weight(1f),
                if (recorded) Palette.MintInk else Palette.Ink, enabled = !recorded && !recording, icon = Icons.Outlined.Check)
        }
    }
}
