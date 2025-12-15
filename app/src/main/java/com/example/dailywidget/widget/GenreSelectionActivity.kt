package com.example.dailywidget.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.dailywidget.data.repository.DataStoreManager
import com.example.dailywidget.ui.theme.DailyWidgetTheme
import kotlinx.coroutines.launch

/**
 * 위젯 추가 시 장르 선택 Activity
 * - 복수 장르 선택 지원 (체크박스)
 * - 신규 위젯: 장르 선택 → 설정 화면
 * - 기존 위젯: 바로 설정 화면
 */
class GenreSelectionActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // 이미 설정된 위젯인지 확인
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        scope.launch {
            val dataStoreManager = DataStoreManager(this@GenreSelectionActivity)

            if (dataStoreManager.isWidgetConfigured(appWidgetId)) {
                // 편집 모드: 바로 설정 화면으로
                val configIntent = Intent(this@GenreSelectionActivity, DailyWidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                startActivityForResult(configIntent, REQUEST_CONFIG)
                return@launch
            }

            // 새 위젯 추가: 장르 선택 화면 표시
            setContent {
                DailyWidgetTheme {
                    GenreSelectionScreen(
                        appWidgetId = appWidgetId,
                        onGenreSelected = { genreId ->
                            saveGenreAndProceed(genreId)
                        },
                        onCancel = {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }

    /** 선택한 장르 저장 및 설정 화면으로 이동 */
    private fun saveGenreAndProceed(genreIds: List<String>) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        scope.launch {
            try {
                val dataStoreManager = DataStoreManager(this@GenreSelectionActivity)

                // 1. 잠금 설정
                dataStoreManager.setWidgetUpdateLock(appWidgetId, true)

                // 2. 복수 장르 저장 + 탭 액션 기본값 설정
                // 장르만 저장
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    dataStoreManager.saveWidgetGenres(appWidgetId, genreIds)
                }

                // 3. 확인
                val savedGenres = dataStoreManager.getWidgetGenres(appWidgetId)

                if (savedGenres != genreIds) {
                }

                // 4. 설정 화면으로 이동
                val configIntent = Intent(this@GenreSelectionActivity, DailyWidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                startActivityForResult(configIntent, REQUEST_CONFIG)
            } catch (e: Exception) {
                e.printStackTrace()

                // 에러 발생 시 잠금 해제
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val dataStoreManager = DataStoreManager(this@GenreSelectionActivity)
                        dataStoreManager.setWidgetUpdateLock(appWidgetId, false)
                    } catch (ignored: Exception) {}
                }

                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CONFIG) {
            // 설정 화면에서 돌아온 경우
            if (resultCode == RESULT_CANCELED) {
                // 취소된 경우 잠금 해제
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val dataStoreManager = DataStoreManager(this@GenreSelectionActivity)
                        dataStoreManager.setWidgetUpdateLock(appWidgetId, false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 설정 화면의 결과를 그대로 전달
            setResult(resultCode, data)
            finish()
        }
    }

    companion object {
        private const val REQUEST_CONFIG = 1001
    }
}

/**
 * 장르 선택 화면
 * 체크박스로 복수 선택 가능
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreSelectionScreen(
    appWidgetId: Int,
    onGenreSelected: (List<String>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }

    var allGenres by remember { mutableStateOf<List<DataStoreManager.Genre>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGenreIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        allGenres = dataStoreManager.getAllGenres()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("위젯 장르 선택") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "취소")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 안내 카드
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "이 위젯에 표시할 장르를 선택하세요",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "여러 장르를 선택하면 모두 표시됩니다",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 기본 장르
                    val defaultGenres = allGenres.filter { it.isBuiltIn }
                    if (defaultGenres.isNotEmpty()) {
                        Text(
                            text = "기본 장르",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        defaultGenres.forEach { genre ->
                            GenreCard(
                                genre = genre,
                                isSelected = genre.id in selectedGenreIds,
                                onClick = {
                                    selectedGenreIds = if (genre.id in selectedGenreIds) {
                                        selectedGenreIds - genre.id
                                    } else {
                                        selectedGenreIds + genre.id
                                    }
                                }
                            )
                        }
                    }

                    // 사용자 정의 장르
                    val customGenres = allGenres.filter { !it.isBuiltIn }
                    if (customGenres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "사용자 정의 장르",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        customGenres.forEach { genre ->
                            GenreCard(
                                genre = genre,
                                isSelected = genre.id in selectedGenreIds,
                                onClick = {
                                    selectedGenreIds = if (genre.id in selectedGenreIds) {
                                        selectedGenreIds - genre.id
                                    } else {
                                        selectedGenreIds + genre.id
                                    }
                                }
                            )
                        }
                    }

                    // 장르 관리 안내
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "장르 추가/삭제는 앱의 설정 화면에서 가능합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 하단 버튼
                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }

                    Button(
                        onClick = {
                            if (selectedGenreIds.isNotEmpty()) {
                                onGenreSelected(selectedGenreIds.toList())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedGenreIds.isNotEmpty()
                    ) {
                        Text("${selectedGenreIds.size}개 장르 선택")
                    }
                }
            }
        }
    }
}

/**
 * 장르 선택 카드 (체크박스)
 */
@Composable
private fun GenreCard(
    genre: DataStoreManager.Genre,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = genre.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (!genre.isBuiltIn) {
                    Text(
                        text = "ID: ${genre.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}