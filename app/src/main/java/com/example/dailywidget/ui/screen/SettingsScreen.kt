package com.example.dailywidget.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailywidget.data.backup.BackupManager
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.repository.DailySentenceRepository
import com.example.dailywidget.data.repository.DataStoreManager
import com.example.dailywidget.widget.DailyWidgetProvider
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.example.dailywidget.util.InitialLoadHelper

/**
 * 설정 화면
 * - 백업/복원
 * - 장르 관리 (기본 + 사용자 정의)
 * - 표시 설정
 * - 폰트 크기
 * - 앱 정보
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: DailySentenceRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = AppDatabase.getDatabase(context)
    val backupManager = BackupManager(context, db)
    val dataStoreManager = DataStoreManager(context)

    // 다이얼로그 상태
    var showBackupInfoDialog by remember { mutableStateOf(false) }
    var showRestorePreviewDialog by remember { mutableStateOf(false) }
    var showDuplicateOptionDialog by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // 백업/복원 데이터
    var backupInfo by remember { mutableStateOf<BackupManager.BackupInfo?>(null) }
    var restorePreview by remember { mutableStateOf<BackupManager.RestorePreview?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // 설정 상태
    var isWidgetForceEnabled by remember { mutableStateOf(false) }
    var isHomeForceEnabled by remember { mutableStateOf(false) }
    var displayConfig by remember { mutableStateOf(DataStoreManager.DisplayConfig()) }
    var fontSizeConfig by remember { mutableStateOf(DataStoreManager.FontSizeConfig()) }

    LaunchedEffect(Unit) {
        isWidgetForceEnabled = dataStoreManager.isWidgetForceStyleEnabled()
        isHomeForceEnabled = dataStoreManager.isHomeForceStyleEnabled()
        displayConfig = dataStoreManager.getDisplayConfig()
        fontSizeConfig = dataStoreManager.getFontSizeConfig()
    }

    // 백업 파일 생성 런처
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    backupManager.exportToJson(it)
                    successMessage = "백업이 완료되었습니다"
                    showSuccess = true
                } catch (e: Exception) {
                    errorMessage = "백업 실패: ${e.message}"
                    showError = true
                }
            }
        }
    }

    // 백업 파일 선택 런처
    val selectRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val preview = backupManager.getRestorePreview(it)
                    restorePreview = preview
                    pendingRestoreUri = it
                    showRestorePreviewDialog = true
                } catch (e: Exception) {
                    errorMessage = "파일 읽기 실패: ${e.message}"
                    showError = true
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // ==================== 백업 및 복원 ====================
        SectionHeader(title = "백업 및 복원")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text("백업하기") },
                    supportingContent = { Text("모든 문장을 파일로 저장") },
                    leadingContent = {
                        Icon(Icons.Default.Upload, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        scope.launch {
                            try {
                                backupInfo = backupManager.getBackupInfo()
                                showBackupInfoDialog = true
                            } catch (e: Exception) {
                                errorMessage = "백업 정보 조회 실패: ${e.message}"
                                showError = true
                            }
                        }
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("복원하기") },
                    supportingContent = { Text("파일에서 문장 불러오기") },
                    leadingContent = {
                        Icon(Icons.Default.Download, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        selectRestoreLauncher.launch(arrayOf("application/json"))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== 데이터 관리 ====================
        SectionHeader(title = "데이터 관리")

        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("초기 데이터 재부팅") },
                supportingContent = { Text("초기 데이터만 업데이트 (추가한 문장 유지)") },
                leadingContent = {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                InitialLoadHelper.updateJsonData(context)
                            }

                            android.widget.Toast.makeText(
                                context,
                                "초기 데이터가 업데이트되었습니다.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "오류: ${e.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== 장르 관리 ====================
        SectionHeader(title = "장르 관리")

        var customGenres by remember { mutableStateOf<List<DataStoreManager.CustomGenre>>(emptyList()) }
        var showAddGenreDialog by remember { mutableStateOf(false) }
        var genreToDelete by remember { mutableStateOf<DataStoreManager.CustomGenre?>(null) }

        LaunchedEffect(Unit) {
            customGenres = dataStoreManager.getCustomGenres()
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                // 기본 장르
                ListItem(
                    headlineContent = { Text("소설") },
                    supportingContent = { Text("기본 장르") },
                    leadingContent = {
                        Icon(Icons.Default.MenuBook, contentDescription = null)
                    },
                    trailingContent = {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "기본",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("판타지") },
                    supportingContent = { Text("기본 장르") },
                    leadingContent = {
                        Icon(Icons.Default.AutoStories, contentDescription = null)
                    },
                    trailingContent = {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "기본",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("시") },
                    supportingContent = { Text("기본 장르") },
                    leadingContent = {
                        Icon(Icons.Default.Article, contentDescription = null)
                    },
                    trailingContent = {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "기본",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )

                // 사용자 정의 장르
                if (customGenres.isNotEmpty()) {
                    Divider()

                    customGenres.forEach { genre ->
                        ListItem(
                            headlineContent = { Text(genre.displayName) },
                            supportingContent = {
                                Text(
                                    "ID: ${genre.id}",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                Icon(Icons.Default.Label, contentDescription = null)
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { genreToDelete = genre }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "삭제",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                        if (genre != customGenres.last()) {
                            Divider()
                        }
                    }
                }

                Divider()

                ListItem(
                    headlineContent = { Text("장르 추가") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { showAddGenreDialog = true }
                )
            }
        }

        // 장르 관리 안내
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "추가한 장르는 위젯 설정 시 선택할 수 있으며, 문장 추가/편집 시에도 사용됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 장르 추가 다이얼로그
        if (showAddGenreDialog) {
            AddGenreDialog(
                onConfirm = { id, displayName ->
                    scope.launch {
                        val success = dataStoreManager.addCustomGenre(id, displayName)
                        if (success) {
                            customGenres = dataStoreManager.getCustomGenres()
                            successMessage = "장르가 추가되었습니다"
                            showSuccess = true
                        } else {
                            errorMessage = "장르 추가 실패: 중복된 ID이거나 잘못된 형식입니다"
                            showError = true
                        }
                    }
                    showAddGenreDialog = false
                },
                onDismiss = { showAddGenreDialog = false }
            )
        }

        // 장르 삭제 확인 다이얼로그
        genreToDelete?.let { genre ->
            AlertDialog(
                onDismissRequest = { genreToDelete = null },
                title = { Text("장르 삭제") },
                text = {
                    Column {
                        Text("'${genre.displayName}' 장르를 삭제하시겠습니까?")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "이 장르를 사용하는 문장은 삭제되지 않습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val success = dataStoreManager.removeCustomGenre(genre.id)
                                if (success) {
                                    customGenres = dataStoreManager.getCustomGenres()
                                    successMessage = "장르가 삭제되었습니다"
                                    showSuccess = true
                                }
                                genreToDelete = null
                            }
                        }
                    ) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { genreToDelete = null }) {
                        Text("취소")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== 일일 알림 설정 ====================
        SectionHeader(title = "일일 알림")

        var notificationConfig by remember { mutableStateOf(DataStoreManager.NotificationConfig()) }
        var showTimePickerDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            notificationConfig = dataStoreManager.getNotificationConfig()
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("알림 활성화", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "매일 설정한 시간에 오늘의 문장 개수를 알려드립니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationConfig.enabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                // Android 13+ 알림 권한 요청은 별도 처리 필요
                                if (enabled) {
                                    // 알림 채널 생성
                                    com.example.dailywidget.util.NotificationHelper.createNotificationChannel(context)

                                    // 기본 시간으로 알림 스케줄링
                                    val hour = notificationConfig.hour
                                    val minute = notificationConfig.minute
                                    com.example.dailywidget.widget.DailyNotificationReceiver.scheduleNotification(
                                        context, hour, minute
                                    )
                                } else {
                                    // 알림 취소
                                    com.example.dailywidget.widget.DailyNotificationReceiver.cancelNotification(context)
                                }

                                val newConfig = notificationConfig.copy(enabled = enabled)
                                dataStoreManager.saveNotificationConfig(newConfig)
                                notificationConfig = newConfig
                            }
                        }
                    )
                }

                if (notificationConfig.enabled) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showTimePickerDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(
                                "%02d:%02d에 알림",
                                notificationConfig.hour,
                                notificationConfig.minute
                            )
                        )
                    }
                }
            }
        }

// 시간 선택 다이얼로그
        if (showTimePickerDialog) {
            TimePickerDialog(
                currentHour = notificationConfig.hour,
                currentMinute = notificationConfig.minute,
                onConfirm = { hour, minute ->
                    scope.launch {
                        val newConfig = notificationConfig.copy(hour = hour, minute = minute)
                        dataStoreManager.saveNotificationConfig(newConfig)
                        notificationConfig = newConfig

                        // 알림 재스케줄링
                        if (newConfig.enabled) {
                            com.example.dailywidget.widget.DailyNotificationReceiver.cancelNotification(context)
                            com.example.dailywidget.widget.DailyNotificationReceiver.scheduleNotification(
                                context, hour, minute
                            )
                        }
                    }
                    showTimePickerDialog = false
                },
                onDismiss = { showTimePickerDialog = false }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "알림은 장르별 문장 개수를 보여줍니다 (예: 소설 5개, 판타지 3개)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== 표시 설정 ====================
        SectionHeader(title = "표시 설정")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("출처 표시", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = displayConfig.showSource,
                        onCheckedChange = { show ->
                            scope.launch {
                                val newConfig = displayConfig.copy(showSource = show)
                                dataStoreManager.saveDisplayConfig(newConfig)
                                displayConfig = newConfig
                                DailyWidgetProvider.updateAllWidgets(context)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("작가 표시", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = displayConfig.showWriter,
                        onCheckedChange = { show ->
                            scope.launch {
                                val newConfig = displayConfig.copy(showWriter = show)
                                dataStoreManager.saveDisplayConfig(newConfig)
                                displayConfig = newConfig
                                DailyWidgetProvider.updateAllWidgets(context)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("특이사항 표시", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = displayConfig.showExtra,
                        onCheckedChange = { show ->
                            scope.launch {
                                val newConfig = displayConfig.copy(showExtra = show)
                                dataStoreManager.saveDisplayConfig(newConfig)
                                displayConfig = newConfig
                                DailyWidgetProvider.updateAllWidgets(context)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== 폰트 크기 설정 ====================
        SectionHeader(title = "폰트 크기 설정")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "앱과 위젯은 사용자 폰 설정의 기본 폰트를 사용합니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {

                // 메인 문장 폰트 크기
                Text("메인 문장", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = fontSizeConfig.textSize,
                    onValueChange = { newSize ->
                        scope.launch {
                            val newConfig = fontSizeConfig.copy(textSize = newSize)
                            dataStoreManager.saveFontSizeConfig(newConfig)
                            fontSizeConfig = newConfig
                            DailyWidgetProvider.updateAllWidgets(context)
                        }
                    },
                    valueRange = 8f..25f,
                    steps = 16
                )
                Text(
                    text = "${fontSizeConfig.textSize.toInt()}sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 출처&작가 폰트 크기
                Text("출처&작가", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = fontSizeConfig.sourceSize,
                    onValueChange = { newSize ->
                        scope.launch {
                            val newConfig = fontSizeConfig.copy(sourceSize = newSize)
                            dataStoreManager.saveFontSizeConfig(newConfig)
                            fontSizeConfig = newConfig
                            DailyWidgetProvider.updateAllWidgets(context)
                        }
                    },
                    valueRange = 6f..20f,
                    steps = 13
                )
                Text(
                    text = "${fontSizeConfig.sourceSize.toInt()}sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 특이사항 폰트 크기
                Text("특이사항", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = fontSizeConfig.extraSize,
                    onValueChange = { newSize ->
                        scope.launch {
                            val newConfig = fontSizeConfig.copy(extraSize = newSize)
                            dataStoreManager.saveFontSizeConfig(newConfig)
                            fontSizeConfig = newConfig
                            DailyWidgetProvider.updateAllWidgets(context)
                        }
                    },
                    valueRange = 5f..15f,
                    steps = 9
                )
                Text(
                    text = "${fontSizeConfig.extraSize.toInt()}sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 미리보기
                Text(
                    text = "미리보기",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color.White
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "예시 문장입니다",
                            fontSize = fontSizeConfig.textSize.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "- 출처, 작가",
                            fontSize = fontSizeConfig.sourceSize.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "특이사항",
                            fontSize = fontSizeConfig.extraSize.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 기본값 초기화 버튼
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val defaultFontConfig = DataStoreManager.FontSizeConfig()
                            dataStoreManager.saveFontSizeConfig(defaultFontConfig)
                            fontSizeConfig = defaultFontConfig
                            DailyWidgetProvider.updateAllWidgets(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("기본값으로 초기화")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== 앱 정보 ====================
        SectionHeader(title = "앱 정보")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text("버전") },
                    supportingContent = { Text("1.0.0") },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("개발자") },
                    supportingContent = { Text("Taeiiin") },
                    leadingContent = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )
            }
        }
    }

    // ==================== 다이얼로그들 ====================

    // 백업 정보 다이얼로그
    if (showBackupInfoDialog && backupInfo != null) {
        AlertDialog(
            onDismissRequest = { showBackupInfoDialog = false },
            title = { Text("백업 정보") },
            text = {
                Column {
                    Text("총 ${backupInfo!!.totalCount}개의 문장을 백업합니다")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 소설: ${backupInfo!!.novelCount}개")
                    Text("• 판타지: ${backupInfo!!.fantasyCount}개")
                    Text("• 시: ${backupInfo!!.poemCount}개")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("날짜 범위: ${backupInfo!!.dateRange}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackupInfoDialog = false
                        createBackupLauncher.launch(BackupManager.generateBackupFileName())
                    }
                ) {
                    Text("백업하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupInfoDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 복원 미리보기 다이얼로그
    if (showRestorePreviewDialog && restorePreview != null) {
        AlertDialog(
            onDismissRequest = { showRestorePreviewDialog = false },
            title = { Text("복원 미리보기") },
            text = {
                Column {
                    Text("총 ${restorePreview!!.totalCount}개의 문장이 있습니다")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 신규: ${restorePreview!!.newCount}개")
                    Text("• 중복: ${restorePreview!!.duplicateCount}개",
                        color = if (restorePreview!!.duplicateCount > 0)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )

                    if (restorePreview!!.duplicateCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "⚠️ 중복된 문장이 발견되었습니다.\n중복 처리 방법을 선택해주세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestorePreviewDialog = false
                        if (restorePreview!!.duplicateCount > 0) {
                            showDuplicateOptionDialog = true
                        } else {
                            scope.launch {
                                try {
                                    pendingRestoreUri?.let {
                                        backupManager.importFromJson(it, BackupManager.DuplicateHandling.SKIP)
                                        successMessage = "${restorePreview!!.newCount}개의 문장이 복원되었습니다"
                                        showSuccess = true
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "복원 실패: ${e.message}"
                                    showError = true
                                }
                            }
                        }
                    }
                ) {
                    Text("다음")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePreviewDialog = false
                    pendingRestoreUri = null
                    restorePreview = null
                }) {
                    Text("취소")
                }
            }
        )
    }

    // 중복 처리 옵션 다이얼로그
    if (showDuplicateOptionDialog) {
        var selectedOption by remember { mutableStateOf(BackupManager.DuplicateHandling.SKIP) }

        AlertDialog(
            onDismissRequest = { showDuplicateOptionDialog = false },
            title = { Text("중복 처리 방법") },
            text = {
                Column {
                    Text("중복된 문장을 어떻게 처리하시겠습니까?")
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = BackupManager.DuplicateHandling.REPLACE },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == BackupManager.DuplicateHandling.REPLACE,
                            onClick = { selectedOption = BackupManager.DuplicateHandling.REPLACE }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("덮어쓰기", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "중복된 문장을 새 문장으로 교체",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = BackupManager.DuplicateHandling.SKIP },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == BackupManager.DuplicateHandling.SKIP,
                            onClick = { selectedOption = BackupManager.DuplicateHandling.SKIP }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("건너뛰기 (권장)", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "중복은 무시하고 새 문장만 추가",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = BackupManager.DuplicateHandling.ADD_ALL },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == BackupManager.DuplicateHandling.ADD_ALL,
                            onClick = { selectedOption = BackupManager.DuplicateHandling.ADD_ALL }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("모두 추가", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "중복 상관없이 전부 추가",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDuplicateOptionDialog = false
                        scope.launch {
                            try {
                                pendingRestoreUri?.let {
                                    backupManager.importFromJson(it, selectedOption)
                                    val message = when (selectedOption) {
                                        BackupManager.DuplicateHandling.REPLACE ->
                                            "${restorePreview?.totalCount}개의 문장이 복원되었습니다 (중복 덮어쓰기)"
                                        BackupManager.DuplicateHandling.SKIP ->
                                            "${restorePreview?.newCount}개의 문장이 복원되었습니다 (중복 건너뛰기)"
                                        BackupManager.DuplicateHandling.ADD_ALL ->
                                            "${restorePreview?.totalCount}개의 문장이 복원되었습니다 (모두 추가)"
                                    }
                                    successMessage = message
                                    showSuccess = true
                                    pendingRestoreUri = null
                                    restorePreview = null
                                }
                            } catch (e: Exception) {
                                errorMessage = "복원 실패: ${e.message}"
                                showError = true
                            }
                        }
                    }
                ) {
                    Text("복원하기")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDuplicateOptionDialog = false
                    pendingRestoreUri = null
                    restorePreview = null
                }) {
                    Text("취소")
                }
            }
        )
    }

    // 성공 메시지
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            title = { Text("완료") },
            text = { Text(successMessage) },
            confirmButton = {
                TextButton(onClick = { showSuccess = false }) {
                    Text("확인")
                }
            }
        )
    }

    // 오류 메시지
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("오류") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("확인")
                }
            }
        )
    }
}

/** 설정 섹션 헤더 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(vertical = 12.dp)
    )
}

/** 장르 추가 다이얼로그 */
@Composable
private fun AddGenreDialog(
    onConfirm: (id: String, displayName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var genreId by remember { mutableStateOf("") }
    var genreDisplayName by remember { mutableStateOf("") }
    var idError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("장르 추가") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "새로운 장르를 추가합니다",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = genreId,
                    onValueChange = { value ->
                        genreId = value.lowercase()
                        idError = when {
                            value.isBlank() -> "ID를 입력하세요"
                            !value.matches(Regex("^[a-z0-9_]*$")) -> "소문자, 숫자, 언더스코어(_)만 가능"
                            value in listOf("novel", "fantasy", "poem") -> "기본 장르 ID는 사용할 수 없습니다"
                            else -> null
                        }
                    },
                    label = { Text("장르 ID") },
                    placeholder = { Text("예: poem, movie_quote") },
                    isError = idError != null,
                    supportingText = {
                        Text(
                            text = idError ?: "영문 소문자, 숫자, 언더스코어만 사용 가능",
                            color = if (idError != null)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = genreDisplayName,
                    onValueChange = { value ->
                        genreDisplayName = value
                        nameError = if (value.isBlank()) "표시명을 입력하세요" else null
                    },
                    label = { Text("장르 표시명") },
                    placeholder = { Text("예: 시, 영화 대사") },
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(
                                text = nameError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "장르 추가 팁",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• ID는 영문 소문자로 시작하세요\n• 표시명은 한글로 입력 가능합니다\n• 예: poem → 시, quote → 명언",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (genreId.isNotBlank() &&
                        genreDisplayName.isNotBlank() &&
                        idError == null &&
                        nameError == null) {
                        onConfirm(genreId, genreDisplayName)
                    }
                },
                enabled = genreId.isNotBlank() &&
                        genreDisplayName.isNotBlank() &&
                        idError == null &&
                        nameError == null
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/** 시간 선택 다이얼로그 */
@Composable
private fun TimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(currentHour) }
    var selectedMinute by remember { mutableStateOf(currentMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 시간 설정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 시 선택
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                selectedHour = (selectedHour + 1) % 24
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "시간 증가")
                        }

                        Text(
                            text = String.format("%02d", selectedHour),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                selectedHour = if (selectedHour == 0) 23 else selectedHour - 1
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "시간 감소")
                        }
                    }

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // 분 선택
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                selectedMinute = (selectedMinute + 1) % 60
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "분 증가")
                        }

                        Text(
                            text = String.format("%02d", selectedMinute),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                selectedMinute = if (selectedMinute == 0) 59 else selectedMinute - 1
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "분 감소")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedHour, selectedMinute) }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}