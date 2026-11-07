
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.components.*
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors

@Composable
fun ProfileScreen(
    vm: HealtheatViewModel,
    onOnboarding: () -> Unit,
    onAuth: () -> Unit
) {
    val profile = vm.profile

    var showNicknameDialog by remember {
        mutableStateOf(false)
    }
    var nicknameInput by remember {
        mutableStateOf("")
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    var deletePassword by remember {
        mutableStateOf("")
    }
    var showHealthDialog by remember { mutableStateOf(false) }
    val selectedHealth = remember { mutableStateListOf<String>() }
    var otherHealth by remember { mutableStateOf("") }
    var noHealth by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 21.dp)
            .padding(top = 15.dp, bottom = 29.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        PageHeader(
            "마이페이지",
            "내 몸과 취향에 맞게 조절해요"
        )

        // 내 계정 정보
        RoundedPanel(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .background(
                            Palette.Lavender,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profile.name.take(1).ifBlank { "나" },
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.Primary
                    )
                }

                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${profile.name}님",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Palette.Ink,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                nicknameInput = profile.name
                                showNicknameDialog = true
                            },
                            contentPadding = PaddingValues(
                                horizontal = 5.dp,
                                vertical = 0.dp
                            )
                        ) {
                            Text(
                                "변경",
                                fontSize = 12.sp,
                                color = Palette.Primary
                            )
                        }
                    }

                    Text(
                        profile.email,
                        fontSize = 11.sp,
                        color = Palette.Muted
                    )
                }
            }
        }

        // 건강 요인
        RoundedPanel(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "나의 건강 요인",
                    color = Palette.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = {
                    selectedHealth.clear()
                    selectedHealth.addAll(activeDiagnosisNames(profile.factors))
                    otherHealth = ""
                    noHealth = selectedHealth.isEmpty()
                    showHealthDialog = true
                }) {
                    Text(
                        "수정하기",
                        fontSize = 11.sp,
                        color = Palette.Primary
                    )
                }
            }

            val active = profile.factors.filter { it.isActive }

            if (active.isEmpty()) {
                Text(
                    "현재 등록된 건강 요인이 없어요",
                    color = Palette.Muted,
                    fontSize = 12.sp
                )
            }

            active.forEach { factor ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .background(
                            Palette.Background,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            factor.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.Ink
                        )
                    }

                    Capsule(
                        "활성",
                        Palette.Mint,
                        Palette.MintInk
                    )
                }
            }
        }

        // 배달·외식 설정
        RoundedPanel(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "배달 · 외식 추천",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.Ink
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "허용 시 하루 최대 1끼 추천",
                        fontSize = 11.sp,
                        color = Palette.Muted
                    )
                }

                Switch(
                    checked = profile.allowDiningOut,
                    onCheckedChange = vm::updateDiningOut,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Palette.Primary,
                        checkedThumbColor = Palette.White
                    )
                )
            }
        }

        // 음식 취향
        RoundedPanel(Modifier.fillMaxWidth()) {
            Text(
                "알레르기 & 음식 취향",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.Ink
            )

            Spacer(Modifier.height(12.dp))

            PreferenceRow(
                "알레르기",
                if (profile.allergyStatus == "있음") {
                    profile.allergies.joinToString(", ")
                        .ifBlank { "없음" }
                } else {
                    profile.allergyStatus
                }
            )

            HorizontalDivider(color = Palette.Border)

            PreferenceRow(
                "좋아하는 음식",
                profile.liked.ifBlank { "미입력" }
            )

            HorizontalDivider(color = Palette.Border)

            PreferenceRow(
                "피하고 싶은 음식",
                profile.disliked.ifBlank { "미입력" }
            )

            HorizontalDivider(color = Palette.Border)

            PreferenceRow(
                "식사 스타일",
                profile.style.ifBlank { "미입력" }
            )

            Spacer(Modifier.height(10.dp))

            OutlineAction(
                "내 설정 수정하기",
                onOnboarding,
                Modifier.fillMaxWidth(),
                Icons.Outlined.Edit
            )
        }

        // 계정 관리
        RoundedPanel(Modifier.fillMaxWidth()) {
            InfoRow(
                Icons.Outlined.PersonOutline,
                "로그아웃",
                "현재 계정에서 로그아웃합니다",
                { vm.logout(onAuth) }
            )

            HorizontalDivider(color = Palette.Border)

            InfoRow(
                Icons.Outlined.Security,
                "회원탈퇴",
                "계정을 삭제합니다"
            ) {
                deletePassword = ""
                vm.clearDeleteAccountError()
                showDeleteDialog = true
            }
        }
    }

    // 건강요인 전용 편집: 회원가입의 다섯 단계 화면으로 이동하지 않는다.
    if (showHealthDialog) {
        Dialog(onDismissRequest = { if (!vm.savingProfile) showHealthDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().imePadding(),
                shape = RoundedCornerShape(18.dp),
                color = Palette.White,
                shadowElevation = 10.dp
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "진단받은 질환 수정",
                            modifier = Modifier.weight(1f),
                            fontSize = 19.sp, fontWeight = FontWeight.Bold,
                            color = Palette.Ink
                        )
                        IconButton(
                            onClick = { showHealthDialog = false },
                            enabled = !vm.savingProfile
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = Palette.Muted)
                        }
                    }
                    Column(
                        Modifier
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 21.dp)
                    ) {
                        Text(
                            "체크를 해제한 질환은 저장 후 비활성화돼요.",
                            fontSize = 12.sp,
                            color = Palette.Muted
                        )
                        Spacer(Modifier.height(15.dp))
                        HealthFactorSelector(
                            selected = selectedHealth.toList(),
                            onToggle = { name ->
                                noHealth = false
                                if (name in selectedHealth) selectedHealth.remove(name)
                                else selectedHealth.add(name)
                            },
                            other = otherHealth,
                            onOtherChanged = {
                                otherHealth = it.take(200)
                                if (it.isNotBlank()) noHealth = false
                            },
                            noneSelected = noHealth,
                            onNoneSelected = {
                                noHealth = !noHealth
                                if (noHealth) { selectedHealth.clear(); otherHealth = "" }
                            },
                            onKeyboardDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        )
                    }
                    HorizontalDivider(color = Palette.Border)
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showHealthDialog = false },
                            enabled = !vm.savingProfile,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) { Text("취소") }
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                if (!noHealth && selectedHealth.isEmpty() && otherHealth.isBlank()) {
                                    vm.toast("질환을 선택하거나 '없음'을 선택해 주세요.")
                                } else {
                                    vm.saveSelectedHealthFactors(
                                        selectedDiagnosisFactors(
                                            selectedHealth.toList(), otherHealth, noHealth,
                                            profile.factors
                                        )
                                    ) { showHealthDialog = false }
                                }
                            },
                            enabled = !vm.savingProfile,
                            modifier = Modifier.weight(1.5f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Palette.Primary)
                        ) {
                            if (vm.savingProfile) {
                                CircularProgressIndicator(
                                    Modifier.size(17.dp), color = Palette.White, strokeWidth = 2.dp
                                )
                            } else Text("변경사항 저장")
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 닉네임 변경 팝업
    // ==========================================

    if (showNicknameDialog) {
        val closeNicknameDialog = {
            showNicknameDialog = false
            nicknameInput = ""
        }

        Dialog(
            onDismissRequest = {
                if (!vm.updatingNickname) {
                    closeNicknameDialog()
                }
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Palette.White,
                shadowElevation = 10.dp
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "닉네임 변경",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Palette.Ink,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = closeNicknameDialog,
                            enabled = !vm.updatingNickname,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "닫기",
                                tint = Palette.Muted
                            )
                        }
                    }

                    Spacer(Modifier.height(9.dp))

                    Text(
                        "앱에서 사용할 이름을 변경할 수 있어요.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Palette.Muted
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "새 닉네임",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.Ink
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = {
                            if (it.length <= 50) {
                                nicknameInput = it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("닉네임을 입력해 주세요")
                        },
                        singleLine = true,
                        enabled = !vm.updatingNickname,
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

                    Spacer(Modifier.height(7.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "${nicknameInput.length}/50자",
                            fontSize = 11.sp,
                            color = Palette.Muted
                        )
                    }

                    Spacer(Modifier.height(25.dp))

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = closeNicknameDialog,
                            enabled = !vm.updatingNickname,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("취소")
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()

                                vm.updateNickname(nicknameInput) {
                                    closeNicknameDialog()
                                }
                            },
                            enabled =
                                nicknameInput.trim().isNotEmpty() &&
                                        nicknameInput.trim() != profile.name &&
                                        !vm.updatingNickname,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Palette.Primary
                            )
                        ) {
                            Text(
                                if (vm.updatingNickname) {
                                    "저장 중..."
                                } else {
                                    "저장하기"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 회원탈퇴 팝업
    // ==========================================

    if (showDeleteDialog) {
        val closeDeleteDialog = {
            showDeleteDialog = false
            deletePassword = ""
            vm.clearDeleteAccountError()
        }

        Dialog(
            onDismissRequest = {
                if (!vm.deletingAccount) {
                    closeDeleteDialog()
                }
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Palette.White,
                shadowElevation = 10.dp
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        "계정을 삭제할까요?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.Ink
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "탈퇴하면 계정을 복구할 수 없어요.\n" +
                                "계속하려면 현재 비밀번호를 입력해 주세요.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Palette.Muted
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "현재 비밀번호",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.Ink
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = {
                            deletePassword = it
                            vm.clearDeleteAccountError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("비밀번호를 입력해 주세요")
                        },
                        visualTransformation =
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        singleLine = true,
                        isError = vm.deleteAccountError != null,
                        enabled = !vm.deletingAccount,
                        shape = RoundedCornerShape(10.dp),
                        colors = healtheatTextFieldColors()
                    )

                    vm.deleteAccountError?.let { error ->
                        Spacer(Modifier.height(7.dp))

                        Text(
                            text = error,
                            fontSize = 12.sp,
                            color = Palette.Coral,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = closeDeleteDialog,
                            enabled = !vm.deletingAccount,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("취소")
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()

                                vm.deleteAccount(deletePassword) {
                                    closeDeleteDialog()
                                    onAuth()
                                }
                            },
                            enabled =
                                deletePassword.isNotBlank() &&
                                        !vm.deletingAccount,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Palette.Coral
                            )
                        ) {
                            Text(
                                if (vm.deletingAccount) {
                                    "처리 중..."
                                } else {
                                    "탈퇴하기"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceRow(
    label: String,
    value: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = Palette.Muted,
            modifier = Modifier.width(105.dp)
        )

        Text(
            value,
            fontSize = 12.sp,
            color = Palette.Body,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
