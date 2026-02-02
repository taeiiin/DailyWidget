package com.example.dailywidget.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dailywidget.data.repository.DailySentenceRepository
import com.example.dailywidget.util.SentenceFileExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * 전체 내보내기 다이얼로그
 * 모든 문장을 JSON 또는 CSV로 내보내기
 */
@Composable
fun ExportAllDialog(
    repository: DailySentenceRepository,
    context: Context,
    onDismiss: () -> Unit,
    onSuccess: (message: String) -> Unit,
    onError: (message: String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(SentenceFileExporter.ExportFormat.JSON) }
    var isExporting by remember { mutableStateOf(false) }
    var totalCount by remember { mutableStateOf(0) }

    // 전체 문장 개수 로드
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            totalCount = repository.getSentenceCount()
        }
    }

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
                exportAllToFile(
                    uri = it,
                    repository = repository,
                    format = selectedFormat,
                    context = context,
                    onSuccess = onSuccess,
                    onError = onError
                )
                isExporting = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = { Text("전체 내보내기") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "모든 문장을 파일로 내보냅니다",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 파일 형식 선택
                Text(
                    text = "파일 형식",
                    style = MaterialTheme.typography.titleSmall
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

                // 요약
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "내보내기 정보",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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

                if (isExporting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val extension = when (selectedFormat) {
                        SentenceFileExporter.ExportFormat.JSON -> "json"
                        SentenceFileExporter.ExportFormat.CSV -> "csv"
                    }
                    val fileName = "dailywidget_all_$timestamp.$extension"
                    createFileLauncher.launch(fileName)
                },
                enabled = !isExporting
            ) {
                Text("내보내기")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("취소")
            }
        }
    )
}

/** 전체 파일 내보내기 */
private suspend fun exportAllToFile(
    uri: Uri,
    repository: DailySentenceRepository,
    format: SentenceFileExporter.ExportFormat,
    context: Context,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            // 모든 문장 가져오기
            val sentences = repository.getAll()

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