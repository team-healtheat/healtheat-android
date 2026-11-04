package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.*
import com.healtheat.app.ui.theme.Palette

@Composable
fun ProfileScreen(vm: HealtheatViewModel, onOnboarding: () -> Unit, onAuth: () -> Unit) {
    val profile = vm.profile
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 21.dp).padding(top = 15.dp, bottom = 29.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        PageHeader("마이페이지", "내 몸과 취향에 맞게 조절해요")
        RoundedPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(65.dp).background(Palette.Peach, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    Text("🌷", fontSize = 38.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("${profile.name}님", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                    Spacer(Modifier.height(3.dp))
                    Text(profile.email, fontSize = 11.sp, color = Palette.Muted)
                    Spacer(Modifier.height(7.dp))
                    Capsule("건강한 하루를 기록 중 ✨", Palette.Lavender, Palette.Primary)
                }
            }
        }
        RoundedPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("나의 건강 요인", color = Palette.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onOnboarding) { Text("수정하기", fontSize = 11.sp, color = Palette.Primary) }
            }
            val active = profile.factors.filter { it.isActive }
            if (active.isEmpty()) Text("현재 등록된 건강 요인이 없어요", color = Palette.Muted, fontSize = 12.sp)
            active.forEach { factor ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .background(Palette.Background, RoundedCornerShape(15.dp)).padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💟", fontSize = 17.sp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(factor.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink)
                        Text(factor.status, fontSize = 10.sp, color = Palette.Muted)
                    }
                    Capsule("활성", Palette.Mint, Palette.MintInk)
                }
            }
        }
        RoundedPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("배달 · 외식 추천", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("허용 시 하루 최대 1끼 추천", fontSize = 11.sp, color = Palette.Muted)
                }
                Switch(checked = profile.allowDiningOut, onCheckedChange = vm::updateDiningOut,
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.Primary, checkedThumbColor = Palette.White))
            }
        }
        RoundedPanel(Modifier.fillMaxWidth()) {
            Text("알레르기 & 음식 취향", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
            Spacer(Modifier.height(12.dp))
            PreferenceRow("알레르기", if (profile.allergyStatus == "있음") profile.allergies.joinToString(", ").ifBlank { "없음" } else profile.allergyStatus)
            HorizontalDivider(color = Palette.Border)
            PreferenceRow("좋아하는 음식", profile.liked.ifBlank { "미입력" })
            HorizontalDivider(color = Palette.Border)
            PreferenceRow("피하고 싶은 음식", profile.disliked.ifBlank { "미입력" })
            HorizontalDivider(color = Palette.Border)
            PreferenceRow("식사 스타일", profile.style.ifBlank { "미입력" })
            Spacer(Modifier.height(10.dp))
            OutlineAction("내 설정 수정하기", onOnboarding, Modifier.fillMaxWidth(), Icons.Outlined.Edit)
        }
        RoundedPanel(Modifier.fillMaxWidth()) {
            InfoRow(Icons.Outlined.PersonOutline, "로그아웃", "현재 계정에서 로그아웃합니다", { vm.logout(onAuth) })
            HorizontalDivider(color = Palette.Border)
            InfoRow(Icons.Outlined.Security, "개인정보와 보안", "실제 서버 연동 시 사용자별 접근 제한 적용")
        }
        QuietNote("계정·건강 정보는 서버를 통해 관리됩니다. 실제 식단과 레시피는 의학적 적합성이나 알레르기 안전성을 보장하지 않습니다.")
    }
}

@Composable
private fun PreferenceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, color = Palette.Muted, modifier = Modifier.width(105.dp))
        Text(value, fontSize = 12.sp, color = Palette.Body, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}
