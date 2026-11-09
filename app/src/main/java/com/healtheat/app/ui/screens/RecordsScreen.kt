package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.healtheat.app.data.MealRecord
import com.healtheat.app.data.MealType
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.PageHeader
import com.healtheat.app.ui.components.RoundedPanel
import com.healtheat.app.ui.components.FilledAction
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 날짜별 조회 전용. 식사 등록은 홈 화면에서만 진행한다. */
@Composable
fun RecordsScreen(vm: HealtheatViewModel) {
    var month by remember { mutableStateOf(YearMonth.from(vm.today)) }
    var selectedDate by remember { mutableStateOf(vm.today) }
    var editingType by remember { mutableStateOf<MealType?>(null) }
    var editingRecord by remember { mutableStateOf<MealRecord?>(null) }

    LaunchedEffect(month, vm.authenticated) {
        vm.loadCalendarRecords(month)
    }

    val records = vm.calendarRecords.toList()
    val grouped = remember(records) { records.groupBy { it.date } }
    val dailyRecords = grouped[selectedDate].orEmpty()
    val first = month.atDay(1)
    val leadingBlankCount = first.dayOfWeek.value % 7 // 일요일 = 0
    val totalCells = ((leadingBlankCount + month.lengthOfMonth() + 6) / 7) * 7
    val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 21.dp)
            .padding(top = 15.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PageHeader("식사 기록", "날짜를 선택해 먹은 음식을 확인하세요")

        RoundedPanel(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    month = month.minusMonths(1)
                    selectedDate = month.atDay(1)
                }) {
                    Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "이전 달", tint = Palette.Ink)
                }
                Text(
                    "${month.year}년 ${month.monthValue}월",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Palette.Ink
                )
                IconButton(
                    onClick = {
                        month = month.plusMonths(1)
                        selectedDate = if (month == YearMonth.from(vm.today)) vm.today else month.atDay(1)
                    },
                    enabled = month < YearMonth.from(vm.today)
                ) {
                    Icon(
                        Icons.Outlined.KeyboardArrowRight,
                        contentDescription = "다음 달",
                        tint = if (month < YearMonth.from(vm.today)) Palette.Ink else Palette.Border
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth()) {
                weekdays.forEachIndexed { index, label ->
                    Box(Modifier.weight(1f).height(28.dp), contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            fontSize = 11.sp,
                            color = if (index == 0) Palette.Coral else Palette.Muted
                        )
                    }
                }
            }

            repeat(totalCells / 7) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val cell = week * 7 + weekday
                        val dayNumber = cell - leadingBlankCount + 1
                        if (dayNumber !in 1..month.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = month.atDay(dayNumber)
                            val count = grouped[date]?.size ?: 0
                            CalendarDay(
                                date = date,
                                count = count,
                                selected = date == selectedDate,
                                today = date == vm.today,
                                onClick = { selectedDate = date },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = Palette.Border)
            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("기록한 날짜 ${grouped.size}일", fontSize = 12.sp, color = Palette.Muted)
                Text("총 ${records.size}끼", fontSize = 12.sp, color = Palette.Muted)
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                selectedDate.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.Ink,
                modifier = Modifier.weight(1f)
            )
            Text("${dailyRecords.size}건", color = Palette.Muted, fontSize = 12.sp)
        }

        if (vm.calendarLoading) {
            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Palette.Primary)
            }
        } else if (vm.calendarError != null) {
            RoundedPanel(Modifier.fillMaxWidth()) {
                Text(vm.calendarError ?: "기록을 불러올 수 없어요.", color = Palette.Coral, fontSize = 13.sp)
                Text("달을 다시 선택하면 재시도할 수 있어요.", color = Palette.Muted, fontSize = 12.sp)
            }
        } else {
            MealType.entries.forEach { type ->
                val typeRecords = dailyRecords.filter { it.type == type }
                val canEdit = selectedDate <= vm.today
                RoundedPanel(
                    Modifier.fillMaxWidth().then(
                        if (canEdit) Modifier.clickable {
                            editingType = type
                            editingRecord = typeRecords.firstOrNull()
                        } else Modifier
                    )
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(type.label, fontSize = 14.sp, color = Palette.Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(if (typeRecords.isEmpty()) "미기록" else "기록됨", fontSize = 11.sp,
                            color = if (typeRecords.isEmpty()) Palette.Muted else Palette.MintInk)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (typeRecords.isEmpty()) {
                        Text("기록한 음식이 없어요.", fontSize = 12.sp, color = Palette.Muted)
                    } else {
                        typeRecords.forEachIndexed { index, record ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            Text(record.foods.joinToString(" · ").ifBlank { "음식 정보 없음" },
                                fontSize = 13.sp, color = Palette.Body, lineHeight = 19.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(if (record.fromRecommendation) "추천 메뉴 기록" else "직접 기록",
                                fontSize = 11.sp, color = Palette.Muted)
                        }
                    }
                    if (canEdit) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                editingType = type
                                editingRecord = typeRecords.firstOrNull()
                            }) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = Palette.Primary)
                                Spacer(Modifier.width(3.dp))
                                Text(if (typeRecords.isEmpty()) "기록" else "편집", fontSize = 11.sp, color = Palette.Primary)
                            }
                        }
                    }
                }
            }
        }
    }

    editingType?.let { type ->
        RecordEditorDialog(
            type = type,
            date = selectedDate,
            initialFood = editingRecord?.foods?.joinToString(" · ").orEmpty(),
            updating = editingRecord != null,
            saving = vm.recording,
            onDismiss = { if (!vm.recording) editingType = null },
            onSave = { food ->
                val onSaved = {
                    editingType = null
                    editingRecord = null
                    vm.loadCalendarRecords(month)
                }
                editingRecord?.let { record ->
                    vm.updateCustomRecord(record.id, type, food, selectedDate, onSaved)
                } ?: vm.recordCustom(type, food, selectedDate, onSaved)
            }
        )
    }
}

@Composable
private fun RecordEditorDialog(
    type: MealType,
    date: LocalDate,
    initialFood: String,
    updating: Boolean,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var food by remember(type, date, initialFood) { mutableStateOf(initialFood) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = Palette.White, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(22.dp)) {
                Text(if (updating) "식사 기록 수정" else "식사 기록 추가", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                Spacer(Modifier.height(6.dp))
                Text("${date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} ${type.label}", fontSize = 12.sp, color = Palette.Muted)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = food,
                    onValueChange = { food = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("먹은 음식") },
                    placeholder = { Text("예: 비빔밥 1인분") },
                    singleLine = true,
                    enabled = !saving,
                    shape = RoundedCornerShape(12.dp),
                    colors = healtheatTextFieldColors()
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp), enabled = !saving, shape = RoundedCornerShape(12.dp)) {
                        Text("취소")
                    }
                    FilledAction(
                        text = if (saving) "저장 중..." else if (updating) "수정하기" else "기록하기",
                        onClick = { onSave(food) },
                        modifier = Modifier.weight(1f),
                        enabled = food.isNotBlank() && !saving
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    count: Int,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .border(1.dp, if (selected) Palette.Primary else Color.Transparent, shape)
            .background(if (selected) Palette.Lavender else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(top = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.dayOfMonth.toString(),
            fontSize = 13.sp,
            fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Palette.Primary else Palette.Ink
        )
        if (count > 0) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(count.coerceAtMost(3)) {
                    Box(Modifier.size(4.dp).background(Palette.Primary, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}
