package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healtheat.app.data.HealthFactor
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.*
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors

@Composable
fun OnboardingScreen(vm: HealtheatViewModel, onBack: () -> Unit, onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var age by remember { mutableStateOf(vm.profile.age.toString()) }
    var gender by remember { mutableStateOf(vm.profile.gender) }
    var diningOut by remember { mutableStateOf(vm.profile.allowDiningOut) }
    var healthText by remember { mutableStateOf("") }
    var analyzed by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    var factors by remember { mutableStateOf(listOf<HealthFactor>()) }
    var allergyStatus by remember { mutableStateOf(vm.profile.allergyStatus) }
    val allergies = remember { mutableStateListOf<String>().apply { addAll(vm.profile.allergies) } }
    var otherAllergy by remember { mutableStateOf("") }
    var liked by remember { mutableStateOf(vm.profile.liked) }
    var disliked by remember { mutableStateOf(vm.profile.disliked) }
    var style by remember { mutableStateOf(vm.profile.style) }
    val title = when(step) {
        1 -> "먼저, 당신을 알려주세요"
        2 -> "건강 이야기를 들려주세요"
        3 -> "알레르기를 확인할게요"
        4 -> "어떤 음식을 좋아하세요?"
        else -> "이제 준비가 끝났어요!"
    }
    val subtitle = when(step) {
        1 -> "더 잘 맞는 한 끼를 위한 첫걸음이에요."
        2 -> "편하게 작성하면 AI가 건강 요인을 정리해요."
        3 -> "안전한 선택을 위한 중요한 정보예요."
        4 -> "좋아하는 맛도 건강한 식단의 일부니까요."
        else -> "내 정보를 확인하고 저장해 주세요."
    }
    Column(Modifier.fillMaxSize().background(Palette.Background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (step > 1) step-- else onBack() }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "뒤로", tint = Palette.Ink)
            }
            Spacer(Modifier.weight(1f))
            Text("STEP 0$step / 05", fontSize = 12.sp, color = Palette.Primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onBack) { Icon(Icons.Outlined.Close, "닫기", tint = Palette.Muted) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..5).forEach { i ->
                Box(Modifier.weight(1f).height(5.dp)
                    .background(if (i <= step) Palette.Primary else Palette.Border, RoundedCornerShape(100.dp)))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 20.dp)) {
            Text(title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, lineHeight = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, fontSize = 13.sp, color = Palette.Muted)
            Spacer(Modifier.height(25.dp))
            when(step) {
                1 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        Text("나이", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Palette.Ink)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(age, onValueChange = { age = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.fillMaxWidth(), label = { Text("나이") },
                            suffix = { Text("세") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp), colors = healtheatTextFieldColors())
                        Spacer(Modifier.height(20.dp))
                        Text("성별 · 선택사항", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Palette.Ink)
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("여성", "남성", "선택 안 함").forEach { opt ->
                                FilterChip(selected = gender == opt, onClick = { gender = opt },
                                    label = { Text(opt, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Palette.Lavender))
                            }
                        }
                        Spacer(Modifier.height(17.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("배달·외식 추천도 받아볼래요", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Palette.Ink)
                                Text("하루 최대 1끼까지", fontSize = 10.sp, color = Palette.Muted)
                            }
                            Switch(checked = diningOut, onCheckedChange = { diningOut = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = Palette.Primary))
                        }
                    }
                }
                2 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        Text("현재 건강 상태 / 관리 목표", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Palette.Ink)
                        Spacer(Modifier.height(9.dp))
                        OutlinedTextField(
                            healthText, onValueChange = { healthText = it; analyzed = false; confirmed = false },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                            placeholder = { Text("예: 콜레스테롤이 높고 통풍 진단을 받았어") },
                            minLines = 4, shape = RoundedCornerShape(15.dp), colors = healtheatTextFieldColors()
                        )
                        Spacer(Modifier.height(13.dp))
                        FilledAction(if (vm.analyzing) "AI 분석 중…" else "건강 요인 분석하기", onClick = {
                            vm.analyzeHealth(healthText) { result ->
                                factors = result
                                analyzed = true
                                confirmed = false
                            }
                        }, modifier = Modifier.fillMaxWidth(), icon = Icons.Outlined.AutoAwesome, enabled = healthText.isNotBlank() && !vm.analyzing)
                    }
                    Spacer(Modifier.height(15.dp))
                    if (analyzed) {
                        RoundedPanel(Modifier.fillMaxWidth(), Palette.Lavender) {
                            Text("AI 건강 요인 분석 결과", fontWeight = FontWeight.Bold, color = Palette.Primary, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            if (factors.isEmpty()) Text("서버가 추출한 건강 요인이 없어요. 입력 내용을 확인해 주세요.", fontSize = 12.sp, color = Palette.Body)
                            factors.forEach { factor ->
                                Text("• ${factor.label}  (${factor.status})", fontSize = 12.sp, color = Palette.Body, modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Spacer(Modifier.height(7.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = confirmed, onCheckedChange = { confirmed = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Palette.Primary))
                                Text("이 내용으로 저장하는 데 동의해요", fontSize = 11.sp, color = Palette.Ink)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    QuietNote("AI가 추출한 건강 요인을 확인해 주세요. 본인의 진단이나 관리 목표와 다를 경우 입력을 수정하고 다시 분석해 주세요.")
                }
                3 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        Text("알레르기 상태", color = Palette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("있음", "없음").forEach { status ->
                                FilterChip(selected = allergyStatus == status, onClick = {
                                    allergyStatus = status
                                    if (status != "있음") { allergies.clear(); otherAllergy = "" }
                                }, label = { Text(status, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Palette.Lavender))
                            }
                        }
                        if (allergyStatus == "있음") {
                            Spacer(Modifier.height(14.dp))
                            Text("해당하는 항목을 선택해 주세요", fontSize = 11.sp, color = Palette.Muted)
                            Spacer(Modifier.height(6.dp))
                            listOf(listOf("밀", "메밀", "대두", "땅콩"), listOf("우유", "달걀", "돼지고기", "쇠고기"), listOf("닭고기", "고등어", "게", "새우"), listOf("오징어", "조개류", "전복", "홍합"), listOf("굴", "복숭아", "토마토", "호두"), listOf("잣", "아황산류")).forEach { group ->
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    group.forEach { item ->
                                        FilterChip(selected = item in allergies, onClick = {
                                            if (item in allergies) allergies.remove(item) else allergies.add(item)
                                        }, label = { Text(item, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Palette.Peach))
                                    }
                                }
                            }
                            Spacer(Modifier.height(7.dp))
                            OutlinedTextField(otherAllergy, onValueChange = { otherAllergy = it },
                                label = { Text("기타 알레르기") }, modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp), singleLine = true, colors = healtheatTextFieldColors())
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    QuietNote("음식의 숨은 재료나 교차접촉 여부는 이 앱에서 보장할 수 없어요. 실제 섭취 전 확인이 필요합니다.")
                }
                4 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        Text("좋아하는 음식", fontSize = 12.sp, color = Palette.Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(liked, { liked = it }, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("연어, 두부, 나물") }, shape = RoundedCornerShape(14.dp), colors = healtheatTextFieldColors())
                        Spacer(Modifier.height(17.dp))
                        Text("피하고 싶은 음식", fontSize = 12.sp, color = Palette.Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(disliked, { disliked = it }, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("짜고 기름진 음식") }, shape = RoundedCornerShape(14.dp), colors = healtheatTextFieldColors())
                        Spacer(Modifier.height(17.dp))
                        Text("선호하는 식사 스타일", fontSize = 12.sp, color = Palette.Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(style, { style = it }, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("담백한 집밥") }, shape = RoundedCornerShape(14.dp), colors = healtheatTextFieldColors())
                    }
                }
                5 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        Text("💜  나의 헬싯 프로필", color = Palette.Primary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        SummaryRow("기본 정보", "${age}세 · $gender")
                        SummaryRow("배달·외식", if (diningOut) "허용" else "허용 안 함")
                        SummaryRow("건강 요인", factors.joinToString { it.label }.ifBlank { "기존 정보 유지" })
                        SummaryRow("알레르기", if (allergyStatus == "있음") (allergies + otherAllergy.takeIf { it.isNotBlank() }.orEmpty()).filter { it.isNotBlank() }.joinToString().ifBlank { "미선택" } else allergyStatus)
                        SummaryRow("좋아하는 음식", liked.ifBlank { "미입력" })
                        SummaryRow("피하고 싶은 음식", disliked.ifBlank { "미입력" })
                        SummaryRow("식사 스타일", style.ifBlank { "미입력" })
                    }
                    Spacer(Modifier.height(14.dp))
                    QuietNote("기본 정보와 건강 요인·알레르기·음식 취향은 서버에 저장됩니다. 음식과 레시피의 알레르기 안전성은 보장되지 않습니다.")
                }
            }
        }
        HorizontalDivider(color = Palette.Border)
        Row(Modifier.fillMaxWidth().background(Palette.White).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 1) OutlineAction("이전", { step-- }, Modifier.weight(1f))
            FilledAction(if (step == 5 && vm.savingProfile) "서버 저장 중…" else if (step == 5) "설정 저장하기" else "계속하기", {
                when {
                    step == 2 && healthText.isNotBlank() && (!analyzed || !confirmed) -> vm.toast("분석 결과를 먼저 확인해 주세요")
                    step == 1 && (age.toIntOrNull() == null || age.toInt() !in 1..120) -> vm.toast("나이를 확인해 주세요")
                    step == 3 && allergyStatus !in listOf("있음", "없음") -> vm.toast("알레르기 상태를 선택해 주세요")
                    step == 3 && allergyStatus == "있음" && allergies.isEmpty() && otherAllergy.isBlank() -> vm.toast("알레르기를 하나 이상 선택해 주세요")
                    step < 5 -> step++
                    else -> {
                        val finalAllergies = if (allergyStatus == "있음")
                            (allergies + otherAllergy.trim().takeIf { it.isNotEmpty() }.orEmpty()).filter { it.isNotBlank() }
                            else emptyList()
                        vm.updateProfile(age.toIntOrNull() ?: vm.profile.age, gender, diningOut, healthText,
                            finalAllergies, allergyStatus, liked, disliked, style, factors, onSuccess = onFinish)
                    }
                }
            }, Modifier.weight(2f), Palette.Primary, icon = if (step == 5) Icons.Outlined.CheckCircleOutline else null)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, fontSize = 10.sp, color = Palette.Muted)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Palette.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = Palette.Border)
}
