package com.healtheat.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healtheat.app.data.HealthFactor
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors

/** 서버의 DIAGNOSIS type에 매핑되는 다중 선택 목록. */
val diagnosedConditions = listOf(
    "비만", "고혈압", "당뇨병", "이상지질혈증(고지혈증)",
    "통풍", "만성콩팥병(만성 신장질환)", "지방간", "심혈관질환",
    "빈혈", "골다공증", "갑상선질환", "위식도역류질환"
)

fun activeDiagnosisNames(factors: List<HealthFactor>): List<String> =
    factors.filter { it.isActive && it.type.equals("DIAGNOSIS", ignoreCase = true) }
        .map { it.label }.distinct()

/** 서버가 전체 목록에서 빠진 항목을 비활성화하므로 활성 목록만 전송한다. */
fun selectedDiagnosisFactors(
    choices: Collection<String>,
    other: String,
    noneSelected: Boolean,
    previous: List<HealthFactor>
): List<HealthFactor> {
    if (noneSelected) return emptyList()
    val names = (choices.toList() + listOfNotNull(other.takeIf { it.isNotBlank() }))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct() // 대소문자 등 내용이 달라진 이름은 임의로 합치지 않는다.
    return names.map { name ->
        val original = previous.firstOrNull {
            it.label == name && it.type.equals("DIAGNOSIS", ignoreCase = true)
        }
        HealthFactor(
            id = original?.id ?: 0L,
            label = name,
            status = "진단 받음",
            isActive = true,
            type = "DIAGNOSIS",
            supported = original?.supported ?: "UNSUPPORTED",
            originalText = if (name in diagnosedConditions) "" else name
        )
    }
}

@Composable
fun HealthFactorSelector(
    selected: List<String>,
    onToggle: (String) -> Unit,
    other: String,
    onOtherChanged: (String) -> Unit,
    noneSelected: Boolean,
    onNoneSelected: () -> Unit,
    onKeyboardDone: () -> Unit = {}
) {
    Text(
        "진단받은 질환을 선택해 주세요",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Palette.TextPrimary
    )
    Spacer(Modifier.height(5.dp))
    Text(
        "여러 개를 선택할 수 있어요. 해당하는 질환이 없다면 '없음'을 선택해 주세요.",
        fontSize = 12.sp,
        color = Palette.TextMuted
    )
    Spacer(Modifier.height(12.dp))
    diagnosedConditions.forEach { name ->
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = !noneSelected && name in selected,
                onCheckedChange = { onToggle(name) },
                colors = CheckboxDefaults.colors(checkedColor = Palette.Primary)
            )
            Text(name, fontSize = 13.sp, color = Palette.TextPrimary)
        }
    }
    selected.filter { it !in diagnosedConditions }.forEach { name ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = !noneSelected, onCheckedChange = { onToggle(name) },
                colors = CheckboxDefaults.colors(checkedColor = Palette.Primary))
            Text(name, fontSize = 13.sp, color = Palette.TextPrimary)
        }
    }
    HorizontalDivider(color = Palette.Border)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = noneSelected,
            onCheckedChange = { onNoneSelected() },
            colors = CheckboxDefaults.colors(checkedColor = Palette.Primary)
        )
        Text("없음", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Palette.TextPrimary)
    }
    Spacer(Modifier.height(9.dp))
    OutlinedTextField(
        value = other,
        onValueChange = onOtherChanged,
        modifier = Modifier.fillMaxWidth(),
        enabled = !noneSelected,
        label = { Text("기타 직접 입력") },
        placeholder = { Text("목록에 없는 질환 이름") },
        supportingText = { Text("입력한 이름 그대로 저장해요. 최대 200자") },
        singleLine = true,
        shape = RoundedCornerShape(13.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onKeyboardDone() }),
        colors = healtheatTextFieldColors()
    )
}
