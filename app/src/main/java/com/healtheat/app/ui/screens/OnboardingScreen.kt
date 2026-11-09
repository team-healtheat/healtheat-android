package com.healtheat.app.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healtheat.app.data.HealthFactor
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.FilledAction
import com.healtheat.app.ui.components.OutlineAction
import com.healtheat.app.ui.components.rememberImeVisibility
import com.healtheat.app.ui.components.RoundedPanel
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors

@Composable
fun OnboardingScreen(
    vm: HealtheatViewModel,
    isInitialSetup: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onInitialSetupExit: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val account = vm.profile.email.trim().lowercase(Locale.ROOT)
    val store = remember(context) { OnboardingDraftStore(context.applicationContext) }
    val restored = remember(account, isInitialSetup) { store.load(account, isInitialSetup) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        Unit
    }
    var step by remember(account, isInitialSetup) { mutableIntStateOf(restored?.step ?: 1) }
    var showExitDialog by remember { mutableStateOf(false) }
    var age by remember(account, isInitialSetup) {
        mutableStateOf(restored?.age ?: vm.profile.age.takeIf { it > 0 }?.toString().orEmpty())
    }
    var gender by remember(account, isInitialSetup) { mutableStateOf(restored?.gender ?: vm.profile.gender) }
    var diningOut by remember(account, isInitialSetup) {
        mutableStateOf(restored?.diningOut ?: vm.profile.allowDiningOut)
    }
    val selectedDiagnoses = remember(account, isInitialSetup) {
        mutableStateListOf<String>().apply {
            addAll(restored?.selectedDiagnoses ?: activeDiagnosisNames(vm.profile.factors))
        }
    }
    var otherDiagnosis by remember(account, isInitialSetup) {
        mutableStateOf(restored?.otherDiagnosis.orEmpty())
    }
    var noDiagnosis by remember(account, isInitialSetup) {
        mutableStateOf(restored?.noDiagnosis ?: false)
    }
    var allergyStatus by remember(account, isInitialSetup) {
        mutableStateOf(restored?.allergyStatus ?: vm.profile.allergyStatus)
    }
    val allergies = remember(account, isInitialSetup) {
        mutableStateListOf<String>().apply { addAll(restored?.allergies ?: vm.profile.allergies) }
    }
    var otherAllergy by remember(account, isInitialSetup) { mutableStateOf(restored?.otherAllergy.orEmpty()) }
    var liked by remember(account, isInitialSetup) { mutableStateOf(restored?.liked ?: vm.profile.liked) }
    var disliked by remember(account, isInitialSetup) { mutableStateOf(restored?.disliked ?: vm.profile.disliked) }
    var style by remember(account, isInitialSetup) { mutableStateOf(restored?.style ?: vm.profile.style) }
    var draftFinished by remember(account, isInitialSetup) { mutableStateOf(false) }
    var backgrounded by remember { mutableStateOf(false) }
    var lastBackgroundAt by remember { mutableLongStateOf(0L) }

    val currentDraft: () -> OnboardingDraft = {
        OnboardingDraft(
            step = step, age = age, gender = gender, diningOut = diningOut,
            selectedDiagnoses = selectedDiagnoses.toList(), otherDiagnosis = otherDiagnosis,
            noDiagnosis = noDiagnosis, allergyStatus = allergyStatus, allergies = allergies.toList(),
            otherAllergy = otherAllergy, liked = liked, disliked = disliked, style = style
        )
    }
    val clearDraft = {
        draftFinished = true
        store.clear(account, isInitialSetup)
    }
    val resetDraft = {
        step = 1
        age = vm.profile.age.takeIf { it > 0 }?.toString().orEmpty()
        gender = vm.profile.gender
        diningOut = vm.profile.allowDiningOut
        selectedDiagnoses.clear()
        selectedDiagnoses.addAll(activeDiagnosisNames(vm.profile.factors))
        otherDiagnosis = ""
        noDiagnosis = false
        allergyStatus = vm.profile.allergyStatus
        allergies.clear()
        allergies.addAll(vm.profile.allergies)
        otherAllergy = ""
        liked = vm.profile.liked
        disliked = vm.profile.disliked
        style = vm.profile.style
        showExitDialog = false
    }

    // 350ms 디바운스로 임시 저장. Activity 중단 시에는 즉시 저장.
    LaunchedEffect(account, isInitialSetup) {
        snapshotFlow { currentDraft() }.collectLatest { draft ->
            delay(350)
            if (!draftFinished && !backgrounded) store.save(account, isInitialSetup, draft)
        }
    }
    val lifecycle = remember(context) { context.onboardingLifecycle() }
    DisposableEffect(lifecycle, account, isInitialSetup) {
        if (lifecycle == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        if (!draftFinished) {
                            backgrounded = true
                            lastBackgroundAt = System.currentTimeMillis()
                            store.save(account, isInitialSetup, currentDraft())
                        }
                    }
                    Lifecycle.Event.ON_START -> {
                        if (lastBackgroundAt != 0L &&
                            System.currentTimeMillis() - lastBackgroundAt >= OnboardingDraftStore.EXPIRATION_MILLIS
                        ) {
                            store.clear(account, isInitialSetup)
                            resetDraft()
                        }
                        backgrounded = false
                        lastBackgroundAt = 0L
                    }
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }

    BackHandler {
        when {
            step > 1 -> { dismissKeyboard(); step-- }
            isInitialSetup -> showExitDialog = true
            else -> { clearDraft(); onBack() }
        }
    }
    val keyboardVisible = rememberImeVisibility()
    val title = when (step) {
        1 -> "먼저, 당신을 알려주세요"
        2 -> "진단받은 질환을 선택해 주세요"
        3 -> "알레르기를 확인할게요"
        4 -> "어떤 음식을 좋아하세요?"
        else -> "이제 준비가 끝났어요!"
    }
    val subtitle = when (step) {
        1 -> "더 잘 맞는 한 끼를 위한 첫걸음이에요."
        2 -> "여러 질환을 선택하거나 기타 항목을 직접 입력할 수 있어요."
        3 -> "안전한 선택을 위한 중요한 정보예요."
        4 -> "좋아하는 맛도 건강한 식단의 일부니까요."
        else -> "내 정보를 확인하고 저장해 주세요."
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 19.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "STEP 0$step / 05",
                fontSize = 12.sp,
                color = Palette.Primary,
                fontWeight = FontWeight.Bold
            )
            // 회원가입 직후 최초 설정은 상단 뒤로가기/X를 모두 숨긴다.
            // 마이페이지에서 수정을 시작한 경우에는 두 버튼을 표시한다.
            if (!isInitialSetup) {
                IconButton(
                    onClick = { if (step > 1) step-- else { clearDraft(); onBack() } },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "뒤로",
                        tint = Palette.TextPrimary
                    )
                }
                IconButton(
                    onClick = { clearDraft(); onClose() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = Palette.TextMuted)
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            (1..5).forEach { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(
                            if (i <= step) Palette.Primary else Palette.Border,
                            RoundedCornerShape(100.dp)
                        )
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 20.dp)
        ) {
            Text(
                title,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Palette.TextPrimary,
                lineHeight = 32.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(subtitle, fontSize = 13.sp, color = Palette.TextMuted)
            Spacer(Modifier.height(25.dp))
            when (step) {
                1 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        Text(
                            "나이",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Palette.TextPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            age, onValueChange = { age = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.fillMaxWidth(), label = { Text("나이") },
                            suffix = { Text("세") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                            shape = RoundedCornerShape(14.dp), colors = healtheatTextFieldColors(),
                            placeholder = { Text("나이를 입력해 주세요") },
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "성별 · 선택사항",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Palette.TextPrimary
                        )
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("여성", "남성", "선택 안 함").forEach { opt ->
                                FilterChip(
                                    selected = gender == opt, onClick = { gender = opt },
                                    label = { Text(opt, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Palette.PrimaryContainer
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(17.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "배달·외식 추천도 받아볼래요",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Palette.TextPrimary
                                )
                                Text("하루 최대 1끼까지", fontSize = 10.sp, color = Palette.TextMuted)
                            }
                            Switch(
                                checked = diningOut, onCheckedChange = { diningOut = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = Palette.Primary)
                            )
                        }
                    }
                }

                2 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        HealthFactorSelector(
                            selected = selectedDiagnoses.toList(),
                            onToggle = { name ->
                                noDiagnosis = false
                                if (name in selectedDiagnoses) selectedDiagnoses.remove(name)
                                else selectedDiagnoses.add(name)
                            },
                            other = otherDiagnosis,
                            onOtherChanged = {
                                otherDiagnosis = it.take(200)
                                if (it.isNotBlank()) noDiagnosis = false
                            },
                            noneSelected = noDiagnosis,
                            onNoneSelected = {
                                noDiagnosis = !noDiagnosis
                                if (noDiagnosis) {
                                    selectedDiagnoses.clear()
                                    otherDiagnosis = ""
                                }
                            },
                            onKeyboardDone = dismissKeyboard
                        )
                    }
                }

                3 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        Text(
                            "알레르기 상태",
                            color = Palette.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            listOf("있음", "없음").forEach { status ->
                                FilterChip(
                                    selected = allergyStatus == status, onClick = {
                                        allergyStatus = status
                                        if (status != "있음") {
                                            allergies.clear(); otherAllergy = ""
                                        }
                                    }, label = { Text(status, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Palette.PrimaryContainer
                                    )
                                )
                            }
                        }
                        if (allergyStatus == "있음") {
                            Spacer(Modifier.height(14.dp))
                            Text("해당하는 항목을 선택해 주세요", fontSize = 11.sp, color = Palette.TextMuted)
                            Spacer(Modifier.height(6.dp))
                            listOf(
                                listOf("밀", "메밀", "대두", "땅콩"),
                                listOf("우유", "달걀", "돼지고기", "쇠고기"),
                                listOf("닭고기", "고등어", "게", "새우"),
                                listOf("오징어", "조개류", "전복", "홍합"),
                                listOf("굴", "복숭아", "토마토", "호두"),
                                listOf("잣", "아황산류")
                            ).forEach { group ->
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    group.forEach { item ->
                                        FilterChip(
                                            selected = item in allergies, onClick = {
                                                if (item in allergies) allergies.remove(item) else allergies.add(
                                                    item
                                                )
                                            }, label = { Text(item, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Palette.WarningContainer
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(7.dp))
                            OutlinedTextField(
                                otherAllergy,
                                onValueChange = { otherAllergy = it },
                                label = { Text("기타 알레르기") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = healtheatTextFieldColors()
                            )
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                }

                4 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        Text(
                            "좋아하는 음식",
                            fontSize = 12.sp,
                            color = Palette.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(
                            liked,
                            { liked = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("연어, 두부, 나물") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                            shape = RoundedCornerShape(14.dp),
                            colors = healtheatTextFieldColors()
                        )
                        Spacer(Modifier.height(17.dp))
                        Text(
                            "피하고 싶은 음식",
                            fontSize = 12.sp,
                            color = Palette.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(
                            disliked,
                            { disliked = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("짜고 기름진 음식") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                            shape = RoundedCornerShape(14.dp),
                            colors = healtheatTextFieldColors()
                        )
                        Spacer(Modifier.height(17.dp))
                        Text(
                            "선호하는 식사 스타일",
                            fontSize = 12.sp,
                            color = Palette.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(
                            style,
                            { style = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("담백한 집밥") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                            shape = RoundedCornerShape(14.dp),
                            colors = healtheatTextFieldColors()
                        )
                    }
                }

                5 -> {
                    RoundedPanel(Modifier.fillMaxWidth()) {
                        Text(
                            "💜  나의 헬싯 프로필",
                            color = Palette.Primary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        SummaryRow("기본 정보", "${age}세 · $gender")
                        SummaryRow("배달·외식", if (diningOut) "허용" else "허용 안 함")
                        SummaryRow(
                            "건강 요인",
                            if (noDiagnosis) "없음" else
                                (selectedDiagnoses.toList() + listOfNotNull(otherDiagnosis.takeIf { it.isNotBlank() }))
                                    .distinct().joinToString().ifBlank { "미선택" })
                        SummaryRow(
                            "알레르기",
                            if (allergyStatus == "있음") (allergies + otherAllergy.takeIf { it.isNotBlank() }
                                .orEmpty()).filter { it.isNotBlank() }.joinToString()
                                .ifBlank { "미선택" } else allergyStatus)
                        SummaryRow("좋아하는 음식", liked.ifBlank { "미입력" })
                        SummaryRow("피하고 싶은 음식", disliked.ifBlank { "미입력" })
                        SummaryRow("식사 스타일", style.ifBlank { "미입력" })
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
        HorizontalDivider(color = Palette.Border)
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Surface)
                .padding(if (keyboardVisible) 6.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step > 1) OutlineAction("이전", { step-- }, Modifier.weight(1f))
            FilledAction(
                if (step == 5 && vm.savingProfile) "서버 저장 중…" else if (step == 5) "설정 저장하기" else "다음",
                {
                    dismissKeyboard()
                    when {
                        step == 2 && !noDiagnosis && selectedDiagnoses.isEmpty() && otherDiagnosis.isBlank() ->
                            vm.toast("진단받은 질환을 선택하거나 '없음'을 선택해 주세요.")

                        step == 1 && (age.toIntOrNull() == null || age.toInt() !in 1..120) -> vm.toast(
                            "나이를 확인해 주세요"
                        )

                        step == 3 && allergyStatus !in listOf(
                            "있음",
                            "없음"
                        ) -> vm.toast("알레르기 상태를 선택해 주세요")

                        step == 3 && allergyStatus == "있음" && allergies.isEmpty() && otherAllergy.isBlank() -> vm.toast(
                            "알레르기를 하나 이상 선택해 주세요"
                        )

                        step < 5 -> step++
                        else -> {
                            val finalAllergies = if (allergyStatus == "있음")
                                (allergies + otherAllergy.trim().takeIf { it.isNotEmpty() }
                                    .orEmpty()).filter { it.isNotBlank() }
                            else emptyList()
                            val chosenFactors = selectedDiagnosisFactors(
                                selectedDiagnoses.toList(), otherDiagnosis, noDiagnosis,
                                vm.profile.factors
                            )
                            vm.updateProfile(
                                age.toIntOrNull() ?: vm.profile.age,
                                gender,
                                diningOut,
                                "", // 이제 AI 분석 없이 선택된 질환만 서버에 저장
                                finalAllergies,
                                allergyStatus,
                                liked,
                                disliked,
                                style,
                                chosenFactors,
                                onSuccess = { clearDraft(); onFinish() }
                            )
                        }
                    }
                },
                Modifier.weight(2f),
                Palette.Primary,
                icon = if (step == 5) Icons.Outlined.CheckCircleOutline else null
            )
        }
    }

    // 가입은 이미 완료된 상태이므로, 중단 시 회원가입 화면 대신 로그아웃 처리한다.
    // Android 시스템 뒤로가기/제스처로 최초 설정 1단계를 벗어나려 할 때만 표시.
    if (isInitialSetup && showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = Palette.Surface,
            title = {
                Text("설정을 중단할까요?", color = Palette.TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "계정은 이미 만들어졌어요. 지금 나가면 입력 중인 내용은 저장되지 않으며, " +
                            "다음 로그인 때 이어서 설정할 수 있어요.",
                    color = Palette.TextBody,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    clearDraft()
                    onInitialSetupExit()
                }) {
                    Text("로그아웃", color = Palette.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("계속 설정하기", color = Palette.TextPrimary)
                }
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(label, fontSize = 10.sp, color = Palette.TextMuted)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Palette.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = Palette.Border)
}

/** 기본 Activity가 백그라운드로 가는 시점에 임시 입력을 저장한다. */
private fun Context.onboardingLifecycle(): Lifecycle? {
    var context: Context? = this
    while (context != null) {
        if (context is LifecycleOwner) return context.lifecycle
        context = (context as? ContextWrapper)?.baseContext
    }
    return null
}
