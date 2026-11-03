package com.healtheat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healtheat.app.data.FoodItem
import com.healtheat.app.data.SourceType
import com.healtheat.app.ui.components.BrandHeader
import com.healtheat.app.ui.screens.*
import com.healtheat.app.ui.theme.Palette
import kotlinx.coroutines.delay

private data class BottomPage(val route: String, val label: String, val icon: ImageVector)
private val pages = listOf(
    BottomPage("home", "홈", Icons.Outlined.Home),
    BottomPage("records", "식사 기록", Icons.Outlined.DateRange),
    BottomPage("chat", "AI 챗봇", Icons.Outlined.ChatBubbleOutline),
    BottomPage("profile", "마이페이지", Icons.Outlined.PersonOutline)
)

@Composable
fun HealtheatApp(vm: HealtheatViewModel = viewModel()) {
    var page by remember { mutableStateOf("auth") }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var selectedFoodSource by remember { mutableStateOf(SourceType.HOME_COOKED) }
    val isMain = pages.any { it.route == page }

    LaunchedEffect(page, vm.authenticated) {
        if (!vm.authenticated && page != "auth") {
            // JWT expiry / 401: never leave protected screens visible.
            page = "auth"
        }
        if (vm.authenticated) {
            when (page) {
                "home" -> { vm.refreshToday(); vm.refreshRecords() }
                "records" -> vm.refreshRecords()
                "chat" -> vm.openChat()
                "profile" -> vm.refreshProfile()
            }
        }
    }

    LaunchedEffect(vm.notice) {
        if (vm.notice != null) {
            delay(2700)
            vm.dismissNotice()
        }
    }

    Scaffold(
        containerColor = Palette.Background,
        topBar = { if (isMain) BrandHeader(onOnboarding = { page = "onboarding" }) },
        bottomBar = {
            if (isMain) {
                NavigationBar(
                    containerColor = Palette.White,
                    tonalElevation = 1.dp,
                    modifier = Modifier.height(76.dp)
                ) {
                    pages.forEach { item ->
                        val active = page == item.route
                        NavigationBarItem(
                            selected = active,
                            onClick = { page = item.route },
                            icon = { Icon(item.icon, null, modifier = Modifier.size(23.dp)) },
                            label = { Text(item.label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Palette.Primary,
                                selectedTextColor = Palette.Primary,
                                indicatorColor = Palette.Lavender,
                                unselectedIconColor = Palette.Muted,
                                unselectedTextColor = Palette.Muted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                "home" -> HomeScreen(vm) { food, source -> selectedFood = food; selectedFoodSource = source }
                "records" -> RecordsScreen(vm)
                "chat" -> ChatScreen(vm, onGoHome = { page = "home" })
                "profile" -> ProfileScreen(vm, onOnboarding = { page = "onboarding" }, onAuth = { page = "auth" })
                "onboarding" -> OnboardingScreen(vm, onBack = { page = "home" }, onFinish = { page = "home" })
                "auth" -> AuthScreen(vm = vm, onBack = { if (vm.authenticated) page = "profile" }, onAuthenticated = { page = if (vm.profile.age > 0) "home" else "onboarding" })
            }

            val notice = vm.notice
            if (notice != null) {
                Surface(
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = 22.dp, vertical = 13.dp),
                    color = Palette.Ink,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 10.dp
                ) {
                    Row(Modifier.padding(horizontal = 17.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircleOutline, null, tint = Palette.Coral, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(notice, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    selectedFood?.let { item ->
        RecipeSheet(item, selectedFoodSource, vm, onDismiss = { selectedFood = null })
    }
}
