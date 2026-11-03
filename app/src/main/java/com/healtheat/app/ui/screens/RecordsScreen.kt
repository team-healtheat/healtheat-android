package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.healtheat.app.data.*
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.*
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RecordsScreen(vm: HealtheatViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    val recordsOnDate = vm.records.filter { it.date == vm.selectedDate }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 21.dp).padding(top = 15.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageHeader("식사 기록", "오늘 먹은 것부터 차곡차곡") {
            FilledAction("직접 기록", onClick = { showAdd = true },
                modifier = Modifier.width(112.dp), color = Palette.Coral, icon = Icons.Outlined.Add)
        }
        RoundedPanel(Modifier.fillMaxWidth(), Palette.Lavender) {
            Text("MY FOOD JOURNAL", color = Palette.Primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("하루 한 끼의 기록이\n나를 더 잘 알게 해요", color = Palette.Ink, fontWeight = FontWeight.Bold,
                        fontSize = 17.sp, lineHeight = 24.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("총 ${vm.records.size}개의 식사 기록", fontSize = 11.sp, color = Palette.Body)
                }
                Text("📔", fontSize = 49.sp)
            }
        }
        Column {
            Text("날짜 선택", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (6 downTo 0).forEach { offset ->
                    val day = vm.today.minusDays(offset.toLong())
                    val selected = day == vm.selectedDate
                    Column(
                        Modifier.width(47.dp)
                            .background(if (selected) Palette.Primary else Palette.White, RoundedCornerShape(16.dp))
                            .clickable { vm.selectedDate = day }
                            .padding(vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (day == vm.today) "오늘" else day.format(DateTimeFormatter.ofPattern("E", Locale.KOREAN)),
                            fontSize = 10.sp, color = if (selected) Color.White else Palette.Muted)
                        Spacer(Modifier.height(6.dp))
                        Text(day.dayOfMonth.toString(), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else Palette.Ink)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(vm.selectedDate.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)) + "의 식사", fontSize = 17.sp,
                fontWeight = FontWeight.Bold, color = Palette.Ink)
            Capsule("${recordsOnDate.size}건 기록됨", Palette.Mint, Palette.MintInk)
        }
        MealType.entries.forEach { type ->
            val typeRecords = recordsOnDate.filter { it.type == type }
            RoundedPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MenuIllustration(type.emoji, when(type) {
                        MealType.BREAKFAST -> Palette.Sand
                        MealType.LUNCH -> Palette.Mint
                        MealType.DINNER -> Palette.Lavender
                    }, Modifier.size(56.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(type.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                            Capsule(if (typeRecords.isEmpty()) "미기록" else "기록 ${typeRecords.size}건",
                                if (typeRecords.isEmpty()) Palette.Background else Palette.Mint,
                                if (typeRecords.isEmpty()) Palette.Muted else Palette.MintInk)
                        }
                        Spacer(Modifier.height(7.dp))
                        if (typeRecords.isEmpty()) {
                            Text("아직 기록이 없어요", fontSize = 12.sp, color = Palette.Muted)
                        } else {
                            typeRecords.forEach { r ->
                                Text(r.foods.joinToString(" · "), fontSize = 12.sp, color = Palette.Body, lineHeight = 18.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(if (r.fromRecommendation) "✓ 추천 식단 간편 기록" else "✎ 직접 입력", fontSize = 10.sp,
                                    color = if (r.fromRecommendation) Palette.MintInk else Palette.Primary)
                            }
                        }
                    }
                }
                if (vm.selectedDate == vm.today && vm.hasPlan && typeRecords.isEmpty() && !vm.recording) {
                    Spacer(Modifier.height(12.dp))
                    FilledAction("추천 식단 간편 기록", { vm.recordRecommended(type) },
                        Modifier.fillMaxWidth(), Palette.Primary, icon = Icons.Outlined.CheckCircleOutline)
                }
            }
        }
        QuietNote("'미기록'은 식사를 거른 것이 아니라, 입력된 기록이 없다는 뜻이에요. 정확한 섭취량이 확인되지 않았다면 실제 섭취량으로 추정하지 않아요.")
    }

    if (showAdd) {
        RecordDialog(onDismiss = { showAdd = false }, onSave = { type, food ->
            vm.recordCustom(type, food, vm.selectedDate, onSuccess = { showAdd = false })
        })
    }
}

@Composable
private fun RecordDialog(onDismiss: () -> Unit, onSave: (MealType, String) -> Unit) {
    var selectedType by remember { mutableStateOf(MealType.LUNCH) }
    var food by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Palette.White, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text("무엇을 드셨나요?", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                Text("직접 먹은 음식을 자유롭게 남겨보세요.", color = Palette.Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { type ->
                        FilterChip(selected = selectedType == type, onClick = { selectedType = type },
                            label = { Text(type.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Palette.Lavender,
                                selectedLabelColor = Palette.Primary))
                    }
                }
                OutlinedTextField(
                    value = food, onValueChange = { food = it },
                    label = { Text("음식 이름") }, placeholder = { Text("예: 곱도리탕 1그릇") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                    colors = healtheatTextFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineAction("취소", onDismiss, Modifier.weight(1f))
                    FilledAction("저장하기", { onSave(selectedType, food) }, Modifier.weight(1f), enabled = food.isNotBlank())
                }
            }
        }
    }
}
