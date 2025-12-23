package com.example.dailywidget.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.repository.DailySentenceRepository
import com.example.dailywidget.ui.screens.TodayScreen
import com.example.dailywidget.ui.screens.SentenceListScreen
import com.example.dailywidget.ui.screens.SettingsScreen
import com.example.dailywidget.ui.theme.DailyWidgetTheme

/**
 * 메인 액티비티
 * 3개 탭으로 구성: 오늘의 문장, 문장 목록, 설정
 */
class MainActivity : ComponentActivity() {

    // 알림 권한 요청 런처
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
        } else {
            android.util.Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 알림 권한 확인 및 요청
        checkAndRequestNotificationPermission()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DailySentenceRepository(database.dailySentenceDao())

        setContent {
            DailyWidgetTheme {
                // Intent로 전달된 초기 탭 처리
                val initialTab = when (intent?.getStringExtra("navigate_to")) {
                    "today" -> 0
                    "list" -> 1
                    "settings" -> 2
                    else -> 0
                }

                val sharedText = intent?.getStringExtra("shared_text")

                MainScreen(
                    repository = repository,
                    initialTab = initialTab,
                    sharedText = sharedText
                )
            }
        }
    }

    /**
     * 알림 권한 확인 및 요청 (Android 13+)
     */
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 권한 있음
                    android.util.Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    // 권한 요청
                    android.util.Log.d("MainActivity", "Requesting notification permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 이하는 권한 불필요
            android.util.Log.d("MainActivity", "Notification permission not required for this Android version")
        }
    }

    /**
     * 새 Intent 처리 (앱이 이미 실행 중일 때)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}

/**
 * 메인 화면 Composable
 * 하단 네비게이션 바를 통한 탭 전환
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: DailySentenceRepository,
    initialTab: Int = 0,
    sharedText: String? = null
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    // initialTab 변경 시 selectedTab도 업데이트
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(58.dp),
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
                1 -> SentenceListScreen(
                    repository = repository,
                    sharedText = sharedText
                )
                2 -> SettingsScreen(repository = repository)
            }
        }
    }
}