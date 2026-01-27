package com.example.dailywidget.ui.components

import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dailywidget.data.repository.DailySentenceRepository
import com.example.dailywidget.data.repository.DataStoreManager
import com.example.dailywidget.util.SentenceFileExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 파일 내보내기 다이얼로그
 * - 장르 선택
 * - 파일 형식 선택 (JSON/CSV)
 * - 내보내기
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFileDialog(
    repository: DailySentenceRepository,
    context: Context,
    onDismiss: () -> Unit,
    onSuccess: (message: String) -> Unit,
    onError: (message: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val dataStoreManager = DataStoreManager(context)

    // 상태
    var allGenres by remember { mutableStateOf<List<DataStoreManager.Genre>>(emptyList()) }
    var selectedGenres by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFormat by remember { mutableStateOf(SentenceFileExporter.ExportFormat.JSON) }
    var genreCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }

    // 파일 생성 런처
    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            when (selectedFormat) {
                SentenceFileExporter.ExportFormat.JSON -> "application/json"
                SentenceFileExporter.ExportFormat.CSV -> "text/csv"
            }
        )
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isExporting = true
                exportToFile(
                    uri = it,
                    repository = repository,
                    selectedGenres = selectedGenres.toList(),
                    format = selectedFormat,
                    context = context,
                    onSuccess = onSuccess,
                    onError = onError
                )
                isExporting = false
            }
        }
    }

    // 장르 및 개수 로드
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            allGenres = dataStoreManager.getAllGenres()

            // 각 장르별 문장 개수 계산
            val counts = mutableMapOf<String, Int>()
            allGenres.forEach { genre ->
                val count = repository.getSentenceCountByGenre(genre.id)
                counts[genre.id] = count
            }
            genreCounts = counts
            isLoading = false
        }
    }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 상단 바
                TopAppBar(
                    title = { Text("파일 내보내기") },
                    navigationIcon = {
                        IconButton(
                            onClick = { if (!isExporting) onDismiss() },
                            enabled = !isExporting
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "닫기")
                        }
                    },
                    actions = {
                        if (!isLoading && !isExporting) {
                            TextButton(
                                onClick = {
                                    if (selectedGenres.isNotEmpty()) {
                                        val fileName = SentenceFileExporter.generateExportFileName(
                                            genres = selectedGenres.map { genreId ->
                                                allGenres.find { it.id == genreId }?.displayName ?: genreId
                                            },
                                            format = selectedFormat
                                        )
                                        createFileLauncher.launch(fileName)
                                    }
                                },
                                enabled = selectedGenres.isNotEmpty()
                            ) {
                                Text("내보내기")
                            }
                        }
                    }
                )

                // 콘텐츠
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    isExporting -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("파일을 생성하는 중...")
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 파일 형식 선택
                            Text(
                                text = "파일 형식",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedFormat == SentenceFileExporter.ExportFormat.JSON,
                                    onClick = { selectedFormat = SentenceFileExporter.ExportFormat.JSON },
                                    label = { Text("JSON") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedFormat == SentenceFileExporter.ExportFormat.CSV,
                                    onClick = { selectedFormat = SentenceFileExporter.ExportFormat.CSV },
                                    label = { Text("CSV") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider()

                            // 장르 선택
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "장르 선택",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                TextButton(
                                    onClick = {
                                        if (selectedGenres.size == allGenres.size) {
                                            selectedGenres = emptySet()
                                        } else {
                                            selectedGenres = allGenres.map { it.id }.toSet()
                                        }
                                    }
                                ) {
                                    Text(
                                        if (selectedGenres.size == allGenres.size) "전체 해제" else "전체 선택"
                                    )
                                }
                            }

                            allGenres.forEach { genre ->
                                val count = genreCounts[genre.id] ?: 0

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedGenres = if (genre.id in selectedGenres) {
                                                selectedGenres - genre.id
                                            } else {
                                                selectedGenres + genre.id
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (genre.id in selectedGenres) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = genre.id in selectedGenres,
                                                onCheckedChange = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = genre.displayName,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = "${count}개 문장",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (genre.isBuiltIn) {
                                            Surface(
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "기본",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            // 요약
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "내보내기 요약",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val totalCount = selectedGenres.sumOf { genreCounts[it] ?: 0 }

                                    Text("선택된 장르: ${selectedGenres.size}개")
                                    Text("총 문장 개수: ${totalCount}개")
                                    Text(
                                        "파일 형식: ${
                                            when (selectedFormat) {
                                                SentenceFileExporter.ExportFormat.JSON -> "JSON"
                                                SentenceFileExporter.ExportFormat.CSV -> "CSV"
                                            }
                                        }"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 파일로 내보내기 */
private suspend fun exportToFile(
    uri: Uri,
    repository: DailySentenceRepository,
    selectedGenres: List<String>,
    format: SentenceFileExporter.ExportFormat,
    context: Context,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            // 선택된 장르의 문장 모두 가져오기
            val sentences = mutableListOf<com.example.dailywidget.data.db.entity.DailySentenceEntity>()
            selectedGenres.forEach { genreId ->
                sentences.addAll(repository.getSentencesByGenre(genreId))
            }

            if (sentences.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onError("내보낼 문장이 없습니다")
                }
                return@withContext
            }

            // 날짜순 정렬
            val sortedSentences = sentences.sortedBy { it.date }

            // 내보내기
            val result = when (format) {
                SentenceFileExporter.ExportFormat.JSON -> {
                    SentenceFileExporter.exportToJson(context, uri, sortedSentences)
                }
                SentenceFileExporter.ExportFormat.CSV -> {
                    SentenceFileExporter.exportToCsv(context, uri, sortedSentences)
                }
            }

            result.fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        onSuccess("${sentences.size}개의 문장이 내보내졌습니다")
                    }
                },
                onFailure = { e ->
                    withContext(Dispatchers.Main) {
                        onError(e.message ?: "내보내기 실패")
                    }
                }
            )
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("내보내기 실패: ${e.message}")
            }
        }
    }
}