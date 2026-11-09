package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healtheat.app.ui.components.rememberImeVisibility
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(vm: HealtheatViewModel, onGoHome: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    val keyboardVisible = rememberImeVisibility()
    val inputBring = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val sessionReady = vm.sessionId != null && !vm.chatBusy

    LaunchedEffect(vm.messages.size, vm.sending, vm.chatBusy, imeHeight, keyboardVisible) {
        if (keyboardVisible) {
            delay(170)
            inputBring.bringIntoView()
        }
        delay(80)
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(
        Modifier.fillMaxSize().imePadding().padding(horizontal = 17.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(47.dp)
                    .background(Palette.PrimaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null,
                    tint = Palette.Primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("식사 상담", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Palette.TextPrimary)
                Text("오늘의 식단을 함께 고민해요", fontSize = 11.sp, color = Palette.TextMuted)
            }
            TextButton(
                onClick = {
                    if (vm.sessionId != null) vm.endChat() else vm.openChat()
                },
                enabled = !vm.chatBusy && !vm.sending
            ) {
                Text(if (vm.sessionId != null) "채팅 종료" else "새 채팅",
                    color = Palette.Danger, fontSize = 11.sp)
            }
        }
        HorizontalDivider(color = Palette.Border)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (vm.chatBusy) {
                item(key = "connecting") {
                    Row(Modifier.fillMaxWidth().padding(vertical = 22.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp),
                            color = Palette.Primary, strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("채팅을 연결하고 있어요…", fontSize = 12.sp, color = Palette.TextMuted)
                    }
                }
            }
            if (vm.sessionId == null && !vm.chatBusy) {
                item(key = "retry") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("채팅 연결을 확인해 주세요.", fontSize = 12.sp, color = Palette.TextMuted)
                        TextButton(onClick = vm::openChat) {
                            Text("다시 연결하기", color = Palette.Primary)
                        }
                    }
                }
            }
            if (vm.messages.isEmpty() && sessionReady) {
                item(key = "empty") {
                    Text("궁금한 음식을 물어보세요.", color = Palette.TextMuted,
                        fontSize = 13.sp, modifier = Modifier.padding(18.dp))
                }
            }
            // 서버 id와 로컬 id가 같아도 LazyColumn key가 충돌하지 않는다.
            itemsIndexed(
                items = vm.messages,
                key = { index, message -> "chat_${index}_${message.id}" }
            ) { _, message ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!message.fromUser) {
                        Box(
                            Modifier.padding(top = 4.dp).size(26.dp)
                                .background(Palette.Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("h", color = Color.White,
                                fontWeight = FontWeight.Black, fontSize = 17.sp)
                        }
                        Spacer(Modifier.width(7.dp))
                    }
                    Column(
                        horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            modifier = Modifier.widthIn(max = 272.dp),
                            shape = RoundedCornerShape(
                                topStart = if (message.fromUser) 19.dp else 5.dp,
                                topEnd = 19.dp,
                                bottomEnd = if (message.fromUser) 5.dp else 19.dp,
                                bottomStart = 19.dp
                            ),
                            color = if (message.fromUser) Palette.Primary else Palette.Surface
                        ) {
                            Text(
                                formatMessageText(message.text),
                                Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = if (message.fromUser) Color.White else Palette.TextBody
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(message.time, fontSize = 9.sp, color = Palette.TextMuted)
                    }
                }
            }
            if (vm.sending) {
                item(key = "answer_loading") {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 34.dp, top = 7.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(23.dp),
                            color = Palette.Primary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(11.dp))
                        Text("답변을 생성하고 있어요…", fontSize = 12.sp, color = Palette.TextMuted)
                    }
                }
            }
        }

        if (!keyboardVisible) {
            Text("이런 것도 물어볼 수 있어요", color = Palette.TextMuted,
                fontSize = 10.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 7.dp))
            Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf("이 음식 먹어도 될까?", "얼마나 자주 먹었을까?")
                .forEachIndexed { index, prompt ->
                    Surface(
                        onClick = { if (sessionReady && !vm.sending) vm.selectChatExample(index) },
                        shape = RoundedCornerShape(10.dp),
                        color = Palette.PrimaryContainer
                    ) {
                        Text(prompt,
                            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            color = Palette.Primary, fontSize = 10.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
        }
        }
        Spacer(Modifier.height(if (keyboardVisible) 4.dp else 12.dp))
        Row(
            Modifier.fillMaxWidth().padding(bottom = if (keyboardVisible) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = {
                    Text(if (sessionReady) "무엇이 궁금하세요?" else "채팅 연결 후 입력할 수 있어요",
                        fontSize = 12.sp)
                },
                modifier = Modifier
                    .weight(1f)
                    .bringIntoViewRequester(inputBring)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            coroutineScope.launch {
                                delay(170)
                                inputBring.bringIntoView()
                            }
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }),
                shape = RoundedCornerShape(12.dp),
                colors = healtheatTextFieldColors()
            )
            IconButton(
                onClick = {
                    val message = input.trim()
                    if (sessionReady && !vm.sending && message.isNotEmpty()) {
                        vm.sendMessage(message)
                        input = ""
                    }
                },
                modifier = Modifier.size(53.dp)
                    .background(Palette.Primary, RoundedCornerShape(12.dp)),
                enabled = sessionReady && !vm.sending && input.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "전송",
                    tint = Color.White, modifier = Modifier.size(21.dp))
            }
        }
    }
}

private fun formatMessageText(source: String) = buildAnnotatedString {
    val markers = Regex("\\*\\*(.+?)\\*\\*", RegexOption.DOT_MATCHES_ALL)
    var cursor = 0
    for (match in markers.findAll(source)) {
        append(source.substring(cursor, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        cursor = match.range.last + 1
    }
    append(source.substring(cursor))
}
