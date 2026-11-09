package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.window.Dialog
import com.healtheat.app.ui.theme.healtheatTextFieldColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healtheat.app.data.FoodItem
import com.healtheat.app.data.Meal
import com.healtheat.app.data.MealType
import com.healtheat.app.data.SourceType
import com.healtheat.app.R
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.FilledAction
import com.healtheat.app.ui.components.RoundedPanel
import com.healtheat.app.ui.components.MealTimeIcon
import com.healtheat.app.ui.theme.Palette
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(vm: HealtheatViewModel, onRecipe: (FoodItem, SourceType) -> Unit) {
    var customRecordType by remember { mutableStateOf<MealType?>(null) }
    var showCustomRecord by remember { mutableStateOf(false) }
    val openCustomRecord: (MealType?) -> Unit = { type ->
        customRecordType = type
        showCustomRecord = true
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HomeHero(vm)
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!vm.hasPlan) {
                if (vm.generating || vm.checkingPlan) GeneratingCard() else GenerateEmptyCard(onGenerate = vm::generatePlan)
                TextButton(onClick = { openCustomRecord(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("오늘 먹은 음식 기록하기", color = Palette.Primary)
                }
            } else {
                MealType.entries.forEach { type ->
                    vm.meals[type]?.let { meal ->
                        key(meal.id, meal.version) {
                            MealCard(
                                meal = meal,
                                onRecipe = onRecipe,
                                onRegenerate = { vm.regenerate(type) },
                                onRecord = { vm.recordRecommended(type) },
                                onCustomRecord = { openCustomRecord(type) },
                                regenerating = vm.busyMeal == type,
                                recorded = vm.records.any { it.date == vm.today && it.type == type },
                                recordedRecommended = vm.records.any { it.date == vm.today && it.type == type && it.fromRecommendation },
                                recording = vm.recording
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
        }
    }

    if (showCustomRecord) {
        HomeRecordDialog(
            initialType = customRecordType,
            saving = vm.recording,
            onDismiss = { if (!vm.recording) showCustomRecord = false },
            onSave = { type, food ->
                vm.recordCustom(type, food, onSuccess = { showCustomRecord = false })
            }
        )
    }
}

@Composable
private fun HomeHero(vm: HealtheatViewModel) {
    Box(
        Modifier.fillMaxWidth().height(285.dp)
    ) {
        Column(
            Modifier.padding(start = 21.dp, top = 20.dp, end = 150.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(vm.today.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)), fontSize = 13.sp, color = Palette.Body)
            if (vm.profile.name.isNotBlank()) Text("${vm.profile.name}님, 안녕하세요.", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink)
            Text("오늘의 식단", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
            Text("건강 정보와 식사 기록을 반영한\n추천 메뉴예요.", fontSize = 13.sp, lineHeight = 20.sp, color = Palette.Body)
        }
        HeroSaladIllustration(Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 8.dp).size(154.dp, 132.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 21.dp, bottom = 15.dp)) {
            Text("오늘의 식탁", style = MaterialTheme.typography.titleLarge, color = Palette.Ink)
            Text("나를 위해 준비한 세 끼", fontSize = 13.sp, color = Palette.Body)
        }
    }
}

@Composable
private fun GenerateEmptyCard(onGenerate: () -> Unit) {
    RoundedPanel(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(9.dp))
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(56.dp)
                .background(Palette.Background, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Restaurant, contentDescription = null,
                tint = Palette.Primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(19.dp))
        Text(
            "오늘의 식단을 만들어볼까요?", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Ink
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "건강 상태와 음식 취향을 바탕으로\n아침·점심·저녁을 추천해 드려요.",
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = Palette.Muted
        )
        Spacer(Modifier.height(22.dp))
        FilledAction(
            "나만의 하루 식단 만들기",
            onGenerate,
            Modifier.fillMaxWidth(),
            Palette.Coral,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GeneratingCard() {
    RoundedPanel(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(17.dp))
        CircularProgressIndicator(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(43.dp),
            color = Palette.Primary,
            strokeWidth = 4.dp
        )
        Spacer(Modifier.height(21.dp))
        Text(
            "오늘의 식탁을 준비하는 중…", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Palette.Ink
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "건강정보와 취향을 반영하고 있어요", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            fontSize = 12.sp, color = Palette.Muted
        )
        Spacer(Modifier.height(18.dp))
    }
}


@Composable
private fun MealCard(
    meal: Meal,
    onRecipe: (FoodItem, SourceType) -> Unit,
    onRegenerate: () -> Unit,
    onRecord: () -> Unit,
    onCustomRecord: () -> Unit,
    regenerating: Boolean,
    recorded: Boolean,
    recordedRecommended: Boolean,
    recording: Boolean
) {
    var expanded by remember(meal.type) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Palette.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, Color.White)
    ) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 15.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MealTimeIcon(type = meal.type, modifier = Modifier.size(25.dp), tint = Palette.Primary)
                    Text(meal.type.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                    // Keep the meal source in its original, immediately visible position.
                    SourceBadge(meal.source)
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    meal.headline,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = onRegenerate,
                contentPadding = PaddingValues(horizontal = 2.dp),
                enabled = !recorded && !regenerating && !recording
            ) {
                if (regenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp,
                        color = Palette.Primary
                    )
                } else {
                    Icon(Icons.Outlined.Refresh, contentDescription = null,
                        modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(3.dp))
                Text(if (regenerating) "추천 중" else "재추천", fontWeight = FontWeight.SemiBold)
            }
        }

        Box(Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = meal.reason.ifBlank { meal.items.joinToString(" · ") { it.name } },
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Palette.Body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(0.dp)) {
                    Text(if (expanded) "메뉴 접기" else "메뉴 자세히 보기", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.Primary)
                    Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp), tint = Palette.Primary)
                }
            }
        }

        if (expanded) {
            HorizontalDivider(color = Palette.Border)

            meal.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.name,
                            fontSize = 13.sp,
                            color = Palette.Ink
                        )
                        if (item.serving.isNotBlank()) {
                            Text(
                                item.serving,
                                fontSize = 11.sp,
                                color = Palette.Muted
                            )
                        }
                    }
                    TextButton(
                        onClick = { onRecipe(item, meal.source) }
                    ) {
                        Text(
                            if (meal.source == SourceType.DINING_OUT)
                                "가이드" else "레시피",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (meal.reason.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "추천 이유",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.Ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    meal.reason,
                    fontSize = 12.sp,
                    color = Palette.Body,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        FilledAction(
            text = when {
                recording -> "저장 중..."
                recorded -> if (recordedRecommended) "추천 메뉴 기록 완료" else "식사 기록 완료"
                else -> "추천 메뉴 먹었어요"
            },
            onClick = onRecord,
            modifier = Modifier.fillMaxWidth(),
            color = if (recorded) Palette.MintInk else Palette.Primary,
            enabled = !recorded && !recording && !regenerating,
            icon = Icons.Outlined.CheckCircle
        )

        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = onCustomRecord,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            enabled = !recorded && !recording && !regenerating,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Palette.Lilac)
        ) {
            if (recorded) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Palette.MintInk)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                if (recorded) "기록이 완료됐어요" else "다른 메뉴를 먹었어요",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (recorded) Palette.MintInk else Palette.Primary
            )
        }
        }
    }
}

@Composable
private fun MealBadge(text: String, background: Color, foreground: Color) {
    Surface(color = background, shape = RoundedCornerShape(10.dp)) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SourceBadge(source: SourceType) {
    val homeCooked = source == SourceType.HOME_COOKED
    MealBadge(if (homeCooked) "집밥" else "배달·외식", if (homeCooked) Palette.Mint else Palette.Peach, if (homeCooked) Palette.MintInk else Color(0xFF9A5135))
}

/** Generic decoration only; recommendation text remains the source of menu truth. */
@Composable
private fun HeroSaladIllustration(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.hero_salad),
        contentDescription = "장식용 샐러드 일러스트",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/** 홈 화면에서만 직접 기록하며, 추천 식사와 동일한 끼니 중복 등록을 막는다. */
@Composable
private fun HomeRecordDialog(
    initialType: MealType?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (MealType, String) -> Unit
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType ?: MealType.LUNCH) }
    var food by remember(initialType) { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().imePadding(),
            color = Palette.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("먹은 음식 기록하기", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                Spacer(Modifier.height(8.dp))
                Text("오늘 실제로 먹은 메뉴를 입력해 주세요.", fontSize = 12.sp, color = Palette.Muted)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            enabled = initialType == null && !saving,
                            label = { Text(type.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Palette.Lavender)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = food,
                    onValueChange = { food = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !saving,
                    label = { Text("음식 이름") },
                    placeholder = { Text("예: 김치볶음밥 1인분") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }),
                    shape = RoundedCornerShape(10.dp),
                    colors = healtheatTextFieldColors()
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !saving,
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("취소") }
                    FilledAction(
                        text = if (saving) "저장 중..." else "저장하기",
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onSave(selectedType, food)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = food.isNotBlank() && !saving
                    )
                }
            }
        }
    }
}
