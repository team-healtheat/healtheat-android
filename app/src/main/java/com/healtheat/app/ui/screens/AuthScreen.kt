package com.healtheat.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.healtheat.app.ui.theme.healtheatTextFieldColors
import com.healtheat.app.ui.components.*
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.HealtheatViewModel

@Composable
fun AuthScreen(vm: HealtheatViewModel, onBack: () -> Unit, onAuthenticated: () -> Unit) {
    var signup by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 25.dp, vertical = 18.dp)) {
        if (vm.authenticated) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "뒤로", tint = Palette.Ink) }
        }
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(72.dp).background(Palette.Lavender, RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) {
            Text("h.", fontSize = 39.sp, fontWeight = FontWeight.Black, color = Palette.Primary)
        }
        Spacer(Modifier.height(22.dp))
        Text(if (signup) "나의 건강한 식탁을\n함께 시작해요." else "반가워요,\n오늘도 맛있는 하루!", color = Palette.Ink,
            fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 36.sp)
        Spacer(Modifier.height(10.dp))
        Text("계정으로 로그인해 나만의 식단을 확인하세요.", color = Palette.Muted, fontSize = 13.sp)
        Spacer(Modifier.height(35.dp))
        Text("이메일", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("healtheat@example.com") }, shape = RoundedCornerShape(16.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = healtheatTextFieldColors())
        Spacer(Modifier.height(19.dp))
        Text("비밀번호", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = healtheatTextFieldColors())
        if (signup) {
            Spacer(Modifier.height(19.dp))
            Text("비밀번호 확인", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(value = confirm, onValueChange = { confirm = it }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = healtheatTextFieldColors())
        }
        Spacer(Modifier.height(23.dp))
        FilledAction(if (vm.authBusy) "연결 중…" else if (signup) "회원가입하고 시작하기" else "로그인",
            onClick = { vm.authenticate(email, password, signup, confirm, onAuthenticated) },
            modifier = Modifier.fillMaxWidth(), color = Palette.Primary, enabled = !vm.authBusy)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { signup = !signup }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (signup) "이미 계정이 있나요? 로그인" else "처음인가요? 회원가입", color = Palette.Primary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(17.dp))
        QuietNote("JWT Access Token은 HttpOnly 쿠키로만 보관하며 30분 후 재로그인이 필요해요. 서버가 HTTPS·쿠키·CSRF를 올바르게 설정해야 로그인이 유지돼요.")
    }
}
