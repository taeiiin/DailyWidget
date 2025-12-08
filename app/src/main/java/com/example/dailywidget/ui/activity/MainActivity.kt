package com.example.dailywidget.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.repository.DailySentenceRepository
import com.example.dailywidget.ui.screens.TodayScreen
import com.example.dailywidget.ui.screens.SentenceListScreen
import com.example.dailywidget.ui.screens.SettingsScreen
import com.example.dailywidget.ui.theme.DailyWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DailySentenceRepository(database.dailySentenceDao())

        setContent {
            DailyWidgetTheme {
                // ⭐ Intent로 전달된 초기 탭 처리
                val initialTab = when (intent?.getStringExtra("navigate_to")) {
                    "today" -> 0
                    "list" -> 1
                    "settings" -> 2
                    else -> 0
                }

                MainScreen(
                    repository = repository,
                    initialTab = initialTab
                )
            }
        }
    }

    // ⭐ 새 Intent 처리 (앱이 이미 실행 중일 때)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // 새 Intent가 들어오면 Activity 재생성
        recreate()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: DailySentenceRepository,
    initialTab: Int = 0  // ⭐ 초기 탭 파라미터 추가
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    // ⭐ initialTab 변경 시 selectedTab도 업데이트
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(58.dp), // 기본 64dp → 48dp로 줄임
                title = {
                    Text(
                        when (selectedTab) {
                            0 -> "오늘의 문장"
                            1 -> "문장 목록"
                            2 -> "설정"
                            else -> "Daily Widget"
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "오늘") },
                    label = { Text("오늘") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "목록") },
                    label = { Text("목록") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "설정") },
                    label = { Text("설정") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> TodayScreen(repository = repository)
                1 -> SentenceListScreen(repository = repository)
                2 -> SettingsScreen(repository = repository)
            }
        }
    }
}