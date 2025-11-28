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
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import com.example.dailywidget.util.InitialLoadHelper

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

    // 상태 관리
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

    // 기본 설정 상태
    var isWidgetForceEnabled by remember { mutableStateOf(false) }
    var isHomeForceEnabled by remember { mutableStateOf(false) }
    var displayConfig by remember { mutableStateOf(DataStoreManager.DisplayConfig()) }
    var fontSizeConfig by remember { mutableStateOf(DataStoreManager.FontSizeConfig()) }

    // 초기 로딩
    LaunchedEffect(Unit) {
        isWidgetForceEnabled = dataStoreManager.isWidgetForceStyleEnabled()
        isHomeForceEnabled = dataStoreManager.isHomeForceStyleEnabled()
        displayConfig = dataStoreManager.getDisplayConfig()
        fontSizeConfig = dataStoreManager.getFontSizeConfig()
    }

    // 백업 파일 생성
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

    // 백업 파일 선택
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

        // ==================== 백업 및 복원 섹션 ====================
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
                headlineContent = { Text("JSON 데이터 업데이트") },
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
                                "JSON 데이터가 업데이트되었습니다.",
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

        // ==================== 표시 설정 섹션 ====================
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

        // ==================== 폰트 크기 설정 섹션 ====================
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
                    valueRange = 12f..32f,
                    steps = 19
                )
                Text(
                    text = "${fontSizeConfig.textSize.toInt()}p",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    valueRange = 10f..24f,
                    steps = 13
                )
                Text(
                    text = "${fontSizeConfig.sourceSize.toInt()}p",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    valueRange = 8f..20f,
                    steps = 11
                )
                Text(
                    text = "${fontSizeConfig.extraSize.toInt()}p",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "미리보기",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "예시 문장입니다",
                            fontSize = fontSizeConfig.textSize.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "- 출처, 작가",
                            fontSize = fontSizeConfig.sourceSize.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "특이사항",
                            fontSize = fontSizeConfig.extraSize.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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

        // ==================== 앱 정보 섹션 ====================
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
                    supportingContent = { Text("강태인") },
                    leadingContent = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )
            }
        }
    }

    // ==================== 백업 정보 다이얼로그 ====================
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
                    Text("• 에세이: ${backupInfo!!.essayCount}개")
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

    // ==================== 복원 미리보기 다이얼로그 ====================
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
                            // 중복 없으면 바로 복원
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

    // ==================== 중복 처리 옵션 다이얼로그 ====================
    if (showDuplicateOptionDialog) {
        var selectedOption by remember { mutableStateOf(BackupManager.DuplicateHandling.SKIP) }

        AlertDialog(
            onDismissRequest = { showDuplicateOptionDialog = false },
            title = { Text("중복 처리 방법") },
            text = {
                Column {
                    Text("중복된 문장을 어떻게 처리하시겠습니까?")
                    Spacer(modifier = Modifier.height(16.dp))

                    // 덮어쓰기
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

                    // 건너뛰기 (기본값)
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

                    // 모두 추가
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

    // ==================== 성공 메시지 ====================
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

    // ==================== 오류 메시지 ====================
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

/**
 * 설정 섹션 헤더
 */
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