
package com.healtheat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.healtheat.app.ui.HealtheatViewModel
import com.healtheat.app.ui.theme.Palette
import com.healtheat.app.ui.theme.healtheatTextFieldColors

@Composable
fun ChatScreen(
    vm: HealtheatViewModel,
    onGoHome: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController =
        LocalSoftwareKeyboardController.current
    val imeBottom = WindowInsets.ime.getBottom(
        LocalDensity.current
    )
    val listState = rememberLazyListState()

    LaunchedEffect(vm.messages.size, imeBottom) {
        if (vm.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                vm.messages.lastIndex
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 17.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(47.dp)
                    .background(
                        Palette.Lavender,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = Palette.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "식사 상담",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Palette.Ink
                )
                Text(
                    "오늘의 식단을 함께 고민해요",
                    fontSize = 11.sp,
                    color = Palette.Muted
                )
            }

            TextButton(
                onClick = {
                    if (vm.sessionId != null) {
                        vm.endChat()
                    } else {
                        vm.openChat()
                    }
                },
                enabled = !vm.chatBusy
            ) {
                Text(
                    if (vm.sessionId != null) {
                        "채팅 종료"
                    } else {
                        "새 채팅"
                    },
                    color = Palette.Coral,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = Palette.Border)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                vertical = 18.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(15.dp)
        ) {
            items(
                items = vm.messages,
                key = { it.id }
            ) { message ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (
                        message.fromUser
                    ) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    }
                ) {
                    if (!message.fromUser) {
                        Box(
                            Modifier
                                .padding(top = 4.dp)
                                .size(26.dp)
                                .background(
                                    Palette.Primary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "h",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                        }

                        Spacer(Modifier.width(7.dp))
                    }

                    Column(
                        horizontalAlignment = if (
                            message.fromUser
                        ) {
                            Alignment.End
                        } else {
                            Alignment.Start
                        }
                    ) {
                        Surface(
                            modifier = Modifier.widthIn(
                                max = 272.dp
                            ),
                            shape = RoundedCornerShape(
                                topStart = if (
                                    message.fromUser
                                ) 19.dp else 5.dp,
                                topEnd = 19.dp,
                                bottomEnd = if (
                                    message.fromUser
                                ) 5.dp else 19.dp,
                                bottomStart = 19.dp
                            ),
                            color = if (message.fromUser) {
                                Palette.Primary
                            } else {
                                Palette.White
                            }
                        ) {
                            Text(
                                text = formatMessageText(
                                    message.text
                                ),
                                modifier = Modifier.padding(
                                    horizontal = 15.dp,
                                    vertical = 13.dp
                                ),
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = if (message.fromUser) {
                                    Color.White
                                } else {
                                    Palette.Body
                                }
                            )
                        }

                        Spacer(Modifier.height(5.dp))

                        Text(
                            message.time,
                            fontSize = 9.sp,
                            color = Palette.Muted
                        )
                    }
                }
            }

            if (vm.sending) {
                item {
                    Row(
                        Modifier.padding(start = 34.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(17.dp),
                            color = Palette.Primary,
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "답변을 작성하고 있어요…",
                            color = Palette.Muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Text(
            "이런 것도 물어볼 수 있어요",
            color = Palette.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 7.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            listOf(
                "이 음식 먹어도 될까?",
                "건강정보 바꾸기",
                "얼마나 자주 먹었을까?",
                "식단 다시 추천받기"
            ).forEachIndexed { index, prompt ->
                Surface(
                    onClick = {
                        vm.selectChatExample(index)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = Palette.Lavender
                ) {
                    Text(
                        prompt,
                        Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 9.dp
                        ),
                        color = Palette.Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                },
                placeholder = {
                    Text(
                        "무엇이 궁금하세요?",
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = healtheatTextFieldColors()
            )

            IconButton(
                onClick = {
                    vm.sendMessage(input)
                    input = ""
                },
                modifier = Modifier
                    .size(53.dp)
                    .background(
                        Palette.Primary,
                        RoundedCornerShape(12.dp)
                    ),
                enabled = input.isNotBlank() &&
                        !vm.sending
            ) {
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = "전송",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

private fun formatMessageText(
    source: String
) = buildAnnotatedString {
    val markers = Regex(
        "\\*\\*(.+?)\\*\\*",
        RegexOption.DOT_MATCHES_ALL
    )

    var cursor = 0

    for (match in markers.findAll(source)) {
        append(source.substring(cursor, match.range.first))

        withStyle(
            SpanStyle(fontWeight = FontWeight.Bold)
        ) {
            append(match.groupValues[1])
        }

        cursor = match.range.last + 1
    }

    append(source.substring(cursor))
}
