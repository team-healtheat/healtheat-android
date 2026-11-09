
package com.healtheat.app.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.*
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    vm: HealtheatViewModel,
    onBack: () -> Unit,
    onAuthenticated: (Boolean) -> Unit,
    initialSignup: Boolean = false
) {
    // 로그인 / 회원가입 화면 상태
    var signup by remember(initialSignup) {
        mutableStateOf(initialSignup)
    }

    // 입력값
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    // 이메일 검증 상태
    var emailTouched by remember { mutableStateOf(false) }
    var emailFocused by remember { mutableStateOf(false) }

    // 입력값 검증
    val emailValid = Patterns.EMAIL_ADDRESS
        .matcher(email.trim())
        .matches()

    val passwordValid = password.length in 8..72

    val confirmValid =
        confirm.isNotEmpty() && password == confirm

    val nicknameValid =
        nickname.trim().length in 1..50

    val signupReady =
        emailValid &&
                passwordValid &&
                confirmValid &&
                nicknameValid

    // 키보드 및 포커스 관리
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val passwordFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }
    val nicknameFocus = remember { FocusRequester() }

    // 키보드가 입력창을 가리지 않도록 자동 스크롤
    val confirmBring = remember { BringIntoViewRequester() }
    val nicknameBring = remember { BringIntoViewRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 25.dp,
                vertical = 18.dp
            )
    ) {

        // 뒤로가기
        if (vm.authenticated) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "뒤로",
                    tint = Palette.TextPrimary
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        // 앱 로고
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Palette.PrimaryContainer,
                    RoundedCornerShape(25.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "h.",
                fontSize = 39.sp,
                fontWeight = FontWeight.Black,
                color = Palette.Primary
            )
        }

        Spacer(Modifier.height(22.dp))

        // 제목
        Text(
            text = if (signup) {
                "나의 건강한 식탁을\n함께 시작해요."
            } else {
                "반가워요,\n오늘도 맛있는 하루!"
            },
            color = Palette.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (signup) {
                "계정을 만들고 나만의 식단을 시작해 보세요."
            } else {
                "계정으로 로그인해 나만의 식단을 확인하세요."
            },
            color = Palette.TextMuted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(35.dp))

        // =========================
        // 1. 이메일
        // =========================

        Text(
            text = "이메일",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.TextPrimary
        )

        Spacer(Modifier.height(7.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        emailTouched = true
                    }
                    emailFocused = focusState.isFocused
                },
            placeholder = {
                Text("healtheat@example.com")
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    passwordFocus.requestFocus()
                }
            ),
            colors = healtheatTextFieldColors()
        )

        // 이메일 형식 오류
        if (
            signup &&
            emailTouched &&
            !emailFocused &&
            !emailValid
        ) {
            Spacer(Modifier.height(5.dp))

            Text(
                text = "올바른 이메일 주소를 입력해 주세요.",
                fontSize = 11.sp,
                color = Palette.Danger
            )
        }

        Spacer(Modifier.height(19.dp))

        // =========================
        // 2. 비밀번호
        // =========================

        Text(
            text = "비밀번호",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.TextPrimary
        )

        Spacer(Modifier.height(7.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it.take(72) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocus),
            visualTransformation =
                PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (signup) {
                    ImeAction.Next
                } else {
                    ImeAction.Done
                }
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    if (signup) {
                        confirmFocus.requestFocus()
                    }
                },
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ),
            colors = healtheatTextFieldColors()
        )

        // 회원가입에서만 비밀번호 길이 검증
        if (signup) {
            Spacer(Modifier.height(6.dp))

            Text(
                text = when {
                    password.isEmpty() ->
                        "8자 이상 입력해 주세요."

                    passwordValid ->
                        "사용할 수 있는 비밀번호예요."

                    else ->
                        "8자 이상 입력해 주세요."
                },
                fontSize = 11.sp,
                color = when {
                    password.isEmpty() -> Palette.TextMuted
                    passwordValid -> Palette.MintInk
                    else -> Palette.Danger
                }
            )
        }

        // =========================
        // 회원가입 전용 입력창
        // =========================

        if (signup) {

            Spacer(Modifier.height(19.dp))

            // =========================
            // 3. 비밀번호 확인
            // =========================

            Text(
                text = "비밀번호 확인",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.TextPrimary
            )

            Spacer(Modifier.height(7.dp))

            OutlinedTextField(
                value = confirm,
                onValueChange = {
                    confirm = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(confirmBring)
                    .focusRequester(confirmFocus)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            scope.launch {
                                delay(250)
                                confirmBring.bringIntoView()
                            }
                        }
                    },
                visualTransformation =
                    PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        nicknameFocus.requestFocus()
                    }
                ),
                colors = healtheatTextFieldColors()
            )

            // 비밀번호 일치 여부
            if (confirm.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (confirmValid) {
                        "비밀번호가 일치합니다."
                    } else {
                        "비밀번호가 일치하지 않습니다."
                    },
                    fontSize = 11.sp,
                    color = if (confirmValid) {
                        Palette.MintInk
                    } else {
                        Palette.Danger
                    }
                )
            }

            Spacer(Modifier.height(19.dp))

            // =========================
            // 4. 닉네임
            // =========================

            Text(
                text = "닉네임",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.TextPrimary
            )

            Spacer(Modifier.height(7.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    if (it.length <= 50) {
                        nickname = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(nicknameBring)
                    .focusRequester(nicknameFocus)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            scope.launch {
                                delay(250)
                                nicknameBring.bringIntoView()
                            }
                        }
                    },
                placeholder = {
                    Text("사용할 닉네임을 입력해 주세요")
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
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
                colors = healtheatTextFieldColors()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${nickname.length}/50자",
                fontSize = 11.sp,
                color = Palette.TextMuted
            )
        }

        Spacer(Modifier.height(23.dp))

        // =========================
        // 로그인 / 회원가입 버튼
        // =========================

        FilledAction(
            text = if (vm.authBusy) {
                "연결 중…"
            } else if (signup) {
                "회원가입하고 시작하기"
            } else {
                "로그인"
            },
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()

                vm.authenticate(
                    email = email,
                    password = password,
                    signup = signup,
                    confirm = confirm,
                    nickname = nickname
                ) {
                    onAuthenticated(signup)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            color = Palette.Primary,
            enabled = !vm.authBusy &&
                    (!signup || signupReady) &&
                    !(initialSignup && vm.authenticated)
        )

        Spacer(Modifier.height(8.dp))

        // =========================
        // 로그인 / 회원가입 화면 전환
        // =========================

        TextButton(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                signup = !signup
            },
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        ) {
            Text(
                text = if (signup) {
                    "이미 계정이 있나요? 로그인"
                } else {
                    "처음인가요? 회원가입"
                },
                color = Palette.Primary,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}
