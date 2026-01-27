package com.example.dailywidget.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.repository.DataStoreManager
import com.example.dailywidget.util.SentenceFileParser
import com.example.dailywidget.util.SentenceFileValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 파일 가져오기 다이얼로그
 * - 파일 파싱 및 검증
 * - 검증 결과 표시
 * - 장르 설정
 * - DB 저장
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFileDialog(
    uri: Uri,
    fileType: com.example.dailywidget.ui.screens.ImportFileType,
    context: Context,
    onDismiss: () -> Unit,
    onSuccess: (message: String) -> Unit,
    onError: (message: String) -> Unit
) {
    val scope = rememberCoroutineScope()

    // 파싱 및 검증 상태
    var isLoading by remember { mutableStateOf(true) }
    var parsedItems by remember { mutableStateOf<List<SentenceFileParser.SentenceJsonItem>?>(null) }
    var validationResult by remember { mutableStateOf<SentenceFileValidator.ValidationResult?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    // 장르 설정
    var genreId by remember { mutableStateOf("") }
    var genreDisplayName by remember { mutableStateOf("") }
    var genreIdError by remember { mutableStateOf<String?>(null) }
    var genreNameError by remember { mutableStateOf<String?>(null) }

    // 저장 중 상태
    var isSaving by remember { mutableStateOf(false) }
    var saveProgress by remember { mutableStateOf(0f) }

    // 파일 읽기 및 파싱
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                inputStream?.close()

                // 파싱
                val result = when (fileType) {
                    com.example.dailywidget.ui.screens.ImportFileType.JSON -> SentenceFileParser.parseJson(content)
                    com.example.dailywidget.ui.screens.ImportFileType.CSV -> SentenceFileParser.parseCsv(content)
                }

                result.fold(
                    onSuccess = { items ->
                        parsedItems = items
                        validationResult = SentenceFileValidator.validate(items)
                        isLoading = false
                    },
                    onFailure = { e ->
                        parseError = e.message
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                parseError = "파일 읽기 실패: ${e.message}"
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 상단 바
                TopAppBar(
                    title = { Text("파일 가져오기") },
                    navigationIcon = {
                        IconButton(
                            onClick = { if (!isSaving) onDismiss() },
                            enabled = !isSaving
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "닫기")
                        }
                    },
                    actions = {
                        if (!isLoading && validationResult?.isValid == true && !isSaving) {
                            TextButton(
                                onClick = {
                                    if (genreId.isNotBlank() &&
                                        genreDisplayName.isNotBlank() &&
                                        genreIdError == null &&
                                        genreNameError == null
                                    ) {
                                        scope.launch {
                                            isSaving = true
                                            saveToDatabase(
                                                context = context,
                                                items = parsedItems!!,
                                                genreId = genreId,
                                                genreDisplayName = genreDisplayName,
                                                onProgress = { progress -> saveProgress = progress },
                                                onSuccess = onSuccess,
                                                onError = onError
                                            )
                                            isSaving = false
                                        }
                                    }
                                },
                                enabled = genreId.isNotBlank() &&
                                        genreDisplayName.isNotBlank() &&
                                        genreIdError == null &&
                                        genreNameError == null
                            ) {
                                Text("가져오기")
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("파일을 분석하는 중...")
                            }
                        }
                    }

                    parseError != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "파일 파싱 실패",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = parseError!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    isSaving -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("문장을 가져오는 중...")
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = saveProgress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${(saveProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    validationResult != null -> {
                        ValidationResultContent(
                            validationResult = validationResult!!,
                            parsedItems = parsedItems!!,
                            genreId = genreId,
                            genreDisplayName = genreDisplayName,
                            genreIdError = genreIdError,
                            genreNameError = genreNameError,
                            onGenreIdChange = { value ->
                                genreId = value.lowercase()
                                genreIdError = validateGenreId(value, context)
                            },
                            onGenreNameChange = { value ->
                                genreDisplayName = value
                                genreNameError = if (value.isBlank()) "표시명을 입력하세요" else null
                            }
                        )
                    }
                }
            }
        }
    }
}

/** 검증 결과 표시 */
@Composable
private fun ValidationResultContent(
    validationResult: SentenceFileValidator.ValidationResult,
    parsedItems: List<SentenceFileParser.SentenceJsonItem>,
    genreId: String,
    genreDisplayName: String,
    genreIdError: String?,
    genreNameError: String?,
    onGenreIdChange: (String) -> Unit,
    onGenreNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 검증 결과 요약
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (validationResult.isValid) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (validationResult.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (validationResult.isValid) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (validationResult.isValid) "검증 성공" else "검증 실패",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("총 ${validationResult.totalCount}개 문장")
                Text("유효: ${validationResult.validCount}개")
                validationResult.dateRange?.let { (start, end) ->
                    Text("날짜 범위: $start ~ $end")
                }
            }
        }

        // 경고 및 오류
        if (validationResult.warnings.isNotEmpty() || validationResult.errors.isNotEmpty()) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (validationResult.errors.isNotEmpty()) {
                        Text(
                            text = "오류 ${validationResult.errors.size}개",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        validationResult.errors.take(5).forEach { error ->
                            Row {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${error.lineNumber?.let { "줄 $it: " } ?: ""}${error.message}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (validationResult.errors.size > 5) {
                            Text(
                                text = "외 ${validationResult.errors.size - 5}개...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (validationResult.warnings.isNotEmpty()) {
                        if (validationResult.errors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Text(
                            text = "경고 ${validationResult.warnings.size}개",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        validationResult.warnings.take(5).forEach { warning ->
                            Row {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${warning.lineNumber?.let { "줄 $it: " } ?: ""}${warning.message}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (validationResult.warnings.size > 5) {
                            Text(
                                text = "외 ${validationResult.warnings.size - 5}개...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 장르 설정 (검증 성공 시만 표시)
        if (validationResult.isValid) {
            Text(
                text = "장르 설정",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = genreId,
                onValueChange = onGenreIdChange,
                label = { Text("장르 ID") },
                placeholder = { Text("예: my_quotes") },
                isError = genreIdError != null,
                supportingText = {
                    Text(
                        text = genreIdError ?: "영문 소문자, 숫자, 언더스코어만 가능",
                        color = if (genreIdError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = genreDisplayName,
                onValueChange = onGenreNameChange,
                label = { Text("장르 표시명") },
                placeholder = { Text("예: 나만의 명언") },
                isError = genreNameError != null,
                supportingText = {
                    if (genreNameError != null) {
                        Text(
                            text = genreNameError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 미리보기
            Text(
                text = "미리보기 (첫 5개)",
                style = MaterialTheme.typography.titleSmall
            )

            parsedItems.take(5).forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDateDisplay(item.date),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!item.source.isNullOrBlank() || !item.writer.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    if (!item.source.isNullOrBlank()) append(item.source)
                                    if (!item.writer.isNullOrBlank()) {
                                        if (isNotEmpty()) append(", ")
                                        append(item.writer)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (parsedItems.size > 5) {
                Text(
                    text = "외 ${parsedItems.size - 5}개...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 장르 ID 검증 */
private fun validateGenreId(id: String, context: Context): String? {
    return when {
        id.isBlank() -> "ID를 입력하세요"
        !id.matches(Regex("^[a-z0-9_]*$")) -> "소문자, 숫자, 언더스코어(_)만 가능"
        id in listOf("novel", "fantasy", "poem") -> "기본 장르 ID는 사용할 수 없습니다"
        else -> null
    }
}

/** DB에 저장 */
private suspend fun saveToDatabase(
    context: Context,
    items: List<SentenceFileParser.SentenceJsonItem>,
    genreId: String,
    genreDisplayName: String,
    onProgress: (Float) -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.dailySentenceDao()
            val dataStoreManager = DataStoreManager(context)

            // 장르 추가 (이미 존재하면 무시)
            dataStoreManager.addCustomGenre(genreId, genreDisplayName)

            // Entity로 변환 (장르 덮어쓰기)
            val entities = SentenceFileParser.toEntities(items, overrideGenre = genreId)

            // 중복 체크 및 저장
            var savedCount = 0
            var duplicateCount = 0

            entities.forEachIndexed { index, entity ->
                val exists = dao.getSentenceByDateGenreText(
                    entity.date,
                    entity.genre,
                    entity.text
                ) != null

                if (!exists) {
                    dao.insertSentence(entity)
                    savedCount++
                } else {
                    duplicateCount++
                }

                onProgress((index + 1).toFloat() / entities.size)
            }

            val message = buildString {
                append("$savedCount 개의 문장이 추가되었습니다")
                if (duplicateCount > 0) {
                    append(" (중복 ${duplicateCount}개 건너뜀)")
                }
            }

            withContext(Dispatchers.Main) {
                onSuccess(message)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("저장 실패: ${e.message}")
            }
        }
    }
}

/** 날짜 표시 형식 변환 */
private fun formatDateDisplay(date: String): String {
    return try {
        val month = date.substring(0, 2).toInt()
        val day = date.substring(2, 4).toInt()
        "${month}월 ${day}일"
    } catch (e: Exception) {
        date
    }
}