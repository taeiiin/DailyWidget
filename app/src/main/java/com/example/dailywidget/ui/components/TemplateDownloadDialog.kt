package com.example.dailywidget.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dailywidget.util.TemplateGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * 템플릿 다운로드 다이얼로그
 * JSON/CSV 템플릿 및 가이드 다운로드
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDownloadDialog(
    context: Context,
    onDismiss: () -> Unit,
    onSuccess: (message: String) -> Unit,
    onError: (message: String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTypes by remember { mutableStateOf(setOf(
        TemplateGenerator.TemplateType.JSON,
        TemplateGenerator.TemplateType.GUIDE
    )) }
    var isDownloading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text("템플릿 다운로드") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "다운로드할 파일을 선택하세요",
                    style = MaterialTheme.typography.bodyMedium
                )

                // JSON 템플릿
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTypes = if (TemplateGenerator.TemplateType.JSON in selectedTypes) {
                                selectedTypes - TemplateGenerator.TemplateType.JSON
                            } else {
                                selectedTypes + TemplateGenerator.TemplateType.JSON
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (TemplateGenerator.TemplateType.JSON in selectedTypes) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = TemplateGenerator.TemplateType.JSON in selectedTypes,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "JSON 템플릿",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "dailywidget_template.json",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // CSV 템플릿
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTypes = if (TemplateGenerator.TemplateType.CSV in selectedTypes) {
                                selectedTypes - TemplateGenerator.TemplateType.CSV
                            } else {
                                selectedTypes + TemplateGenerator.TemplateType.CSV
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (TemplateGenerator.TemplateType.CSV in selectedTypes) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = TemplateGenerator.TemplateType.CSV in selectedTypes,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CSV 템플릿",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "dailywidget_template.csv (엑셀 편집 가능)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 사용 가이드
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTypes = if (TemplateGenerator.TemplateType.GUIDE in selectedTypes) {
                                selectedTypes - TemplateGenerator.TemplateType.GUIDE
                            } else {
                                selectedTypes + TemplateGenerator.TemplateType.GUIDE
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (TemplateGenerator.TemplateType.GUIDE in selectedTypes) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = TemplateGenerator.TemplateType.GUIDE in selectedTypes,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "사용 가이드",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "dailywidget_guide.txt (필독 권장)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 안내
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
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
                            text = "파일은 Downloads 폴더에 저장됩니다",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedTypes.isNotEmpty()) {
                        scope.launch {
                            isDownloading = true
                            downloadTemplates(
                                context = context,
                                types = selectedTypes,
                                onSuccess = onSuccess,
                                onError = onError
                            )
                            isDownloading = false
                        }
                    }
                },
                enabled = selectedTypes.isNotEmpty() && !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("다운로드")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading
            ) {
                Text("취소")
            }
        }
    )
}

/** 템플릿 다운로드 */
private suspend fun downloadTemplates(
    context: Context,
    types: Set<TemplateGenerator.TemplateType>,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val downloadedFiles = mutableListOf<String>()

            types.forEach { type ->
                val content = when (type) {
                    TemplateGenerator.TemplateType.JSON -> TemplateGenerator.generateJsonTemplate()
                    TemplateGenerator.TemplateType.CSV -> TemplateGenerator.generateCsvTemplate()
                    TemplateGenerator.TemplateType.GUIDE -> TemplateGenerator.generateGuideText()
                }

                val fileName = TemplateGenerator.generateTemplateFileName(type)

                val result = TemplateGenerator.saveToDownloads(context, fileName, content)
                result.fold(
                    onSuccess = { path ->
                        downloadedFiles.add(fileName)
                    },
                    onFailure = { e ->
                        throw e
                    }
                )
            }

            withContext(Dispatchers.Main) {
                val message = buildString {
                    append("${downloadedFiles.size}개의 파일이 다운로드되었습니다\n")
                    append("위치: Downloads 폴더\n\n")
                    downloadedFiles.forEach { fileName ->
                        append("• $fileName\n")
                    }
                }
                onSuccess(message)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("다운로드 실패: ${e.message}")
            }
        }
    }
}