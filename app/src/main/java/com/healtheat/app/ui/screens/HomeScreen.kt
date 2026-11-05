
package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.healtheat.app.data.FoodItem
import com.healtheat.app.data.Meal
import com.healtheat.app.data.MealType
import com.healtheat.app.data.SourceType
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.FilledAction
import com.healtheat.app.ui.components.RoundedPanel
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    vm: HealtheatViewModel,
    onRecipe: (FoodItem, SourceType) -> Unit
) {
    var customRecordType by remember {
        mutableStateOf<MealType?>(null)
    }

    var showCustomRecord by remember {
        mutableStateOf(false)
    }

    val openCustomRecord: (MealType?) -> Unit = { type ->
        customRecordType = type
        showCustomRecord = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 21.dp)
            .padding(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeHero(vm)

        if (!vm.hasPlan) {
            if (vm.generating || vm.checkingPlan) {
                GeneratingCard()
            } else {
                GenerateEmptyCard(
                    onGenerate = vm::generatePlan
                )
            }

            TextButton(
                onClick = {
                    openCustomRecord(null)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "오늘 먹은 음식 기록하기",
                    color = Palette.Primary
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "오늘의 식탁",
                        style = MaterialTheme.typography.titleLarge,
                        color = Palette.Ink
                    )

                    Text(
                        "나를 위해 준비한 세 끼",
                        fontSize = 12.sp,
                        color = Palette.Muted
                    )
                }
            }

            MealType.entries.forEach { type ->
                vm.meals[type]?.let { meal ->
                    key(meal.id, meal.version) {
                        MealCard(
                            meal = meal,
                            onRecipe = onRecipe,
                            onRegenerate = {
                                vm.regenerate(type)
                            },
                            onRecord = {
                                vm.recordRecommended(type)
                            },
                            onCustomRecord = {
                                openCustomRecord(type)
                            },
                            regenerating = vm.busyMeal == type,
                            recorded = vm.records.any {
                                it.date == vm.today &&
                                        it.type == type
                            },
                            recording = vm.recording
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(3.dp))
    }

    if (showCustomRecord) {
        HomeRecordDialog(
            initialType = customRecordType,
            saving = vm.recording,
            onDismiss = {
                if (!vm.recording) {
                    showCustomRecord = false
                }
            },
            onSave = { type, food ->
                vm.recordCustom(
                    type = type,
                    foodName = food,
                    onSuccess = {
                        showCustomRecord = false
                    }
                )
            }
        )
    }
}

@Composable
private fun HomeHero(vm: HealtheatViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = vm.today.format(
                DateTimeFormatter.ofPattern(
                    "M월 d일 EEEE",
                    Locale.KOREAN
                )
            ),
            fontSize = 13.sp,
            color = Palette.Muted
        )

        if (vm.profile.name.isNotBlank()) {
            Text(
                text = "${vm.profile.name}님, 안녕하세요.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Palette.Body
            )
        }

        Text(
            "오늘의 식단",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.Ink
        )

        Text(
            "건강 정보와 식사 취향을 반영한 추천 메뉴",
            fontSize = 13.sp,
            color = Palette.Muted
        )
    }
}

@Composable
private fun GenerateEmptyCard(
    onGenerate: () -> Unit
) {
    RoundedPanel(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(9.dp))

        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(56.dp)
                .background(
                    Palette.Background,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Restaurant,
                contentDescription = null,
                tint = Palette.Primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(19.dp))

        Text(
            "오늘의 식단을 만들어볼까요?",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Palette.Ink
        )

        Spacer(Modifier.height(7.dp))

        Text(
            "건강 상태와 음식 취향을 바탕으로\n" +
                    "아침·점심·저녁을 추천해 드려요.",
            modifier = Modifier.fillMaxWidth(),
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
            Palette.Coral
        )
    }
}

@Composable
private fun GeneratingCard() {
    RoundedPanel(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(17.dp))

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(43.dp),
            color = Palette.Primary,
            strokeWidth = 4.dp
        )

        Spacer(Modifier.height(21.dp))

        Text(
            "오늘의 식탁을 준비하는 중…",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Palette.Ink
        )

        Spacer(Modifier.height(7.dp))

        Text(
            "건강정보와 취향을 반영하고 있어요",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = Palette.Muted
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
    recording: Boolean
) {
    var expanded by remember(meal.type) {
        mutableStateOf(false)
    }

    RoundedPanel(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    meal.type.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.Muted
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    meal.headline,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = onRegenerate,
                enabled = !regenerating && !recording
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )

                Spacer(Modifier.width(3.dp))

                Text(
                    if (regenerating) {
                        "추천 중"
                    } else {
                        "재추천"
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = meal.items.joinToString(" · ") {
                it.name
            },
            fontSize = 12.sp,
            color = Palette.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        TextButton(
            onClick = {
                expanded = !expanded
            },
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                if (expanded) {
                    "메뉴 접기"
                } else {
                    "메뉴 자세히 보기"
                },
                fontSize = 12.sp,
                color = Palette.Primary
            )

            Icon(
                if (expanded) {
                    Icons.Outlined.KeyboardArrowUp
                } else {
                    Icons.Outlined.KeyboardArrowDown
                },
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
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
                        onClick = {
                            onRecipe(item, meal.source)
                        }
                    ) {
                        Text(
                            if (
                                meal.source ==
                                SourceType.DINING_OUT
                            ) {
                                "가이드"
                            } else {
                                "레시피"
                            },
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

        Spacer(Modifier.height(10.dp))

        FilledAction(
            text = when {
                recording -> "저장 중..."
                recorded -> "기록 완료"
                else -> "추천 메뉴 먹었어요"
            },
            onClick = onRecord,
            modifier = Modifier.fillMaxWidth(),
            color = if (recorded) {
                Palette.MintInk
            } else {
                Palette.Primary
            },
            enabled = !recorded &&
                    !recording &&
                    !regenerating
        )

        Spacer(Modifier.height(7.dp))

        OutlinedButton(
            onClick = onCustomRecord,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            enabled = !recorded &&
                    !recording &&
                    !regenerating,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                "다른 메뉴를 먹었어요",
                fontSize = 12.sp,
                color = Palette.Body
            )
        }
    }
}

@Composable
private fun HomeRecordDialog(
    initialType: MealType?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (MealType, String) -> Unit
) {
    var selectedType by remember(initialType) {
        mutableStateOf(initialType ?: MealType.LUNCH)
    }

    var food by remember(initialType) {
        mutableStateOf("")
    }

    val focusManager = LocalFocusManager.current
    val keyboardController =
        LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            color = Palette.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    "먹은 음식 기록하기",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.Ink
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "오늘 실제로 먹은 메뉴를 입력해 주세요.",
                    fontSize = 12.sp,
                    color = Palette.Muted
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                            },
                            enabled = initialType == null &&
                                    !saving,
                            label = {
                                Text(
                                    type.label,
                                    fontSize = 12.sp
                                )
                            },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor =
                                        Palette.Lavender
                                )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = food,
                    onValueChange = {
                        food = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !saving,
                    label = {
                        Text("음식 이름")
                    },
                    placeholder = {
                        Text("예: 김치볶음밥 1인분")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = healtheatTextFieldColors()
                )

                Spacer(Modifier.height(22.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !saving,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("취소")
                    }

                    FilledAction(
                        text = if (saving) {
                            "저장 중..."
                        } else {
                            "저장하기"
                        },
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
