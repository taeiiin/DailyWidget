package com.example.dailywidget.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.ui.window.Dialog
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import com.example.dailywidget.data.repository.DailySentenceRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 문장 편집 화면 (추가/수정)
 * 날짜, 장르, 텍스트, 출처, 작가, 특이사항 입력
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceEditorScreen(
    repository: DailySentenceRepository,
    sentence: DailySentenceEntity? = null,
    sharedText: String? = null,
    onDismiss: (needsRefresh: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isEditMode = sentence != null

    // 입력 상태
    var date by remember { mutableStateOf(sentence?.date ?: getCurrentDate()) }
    var genre by remember { mutableStateOf(sentence?.genre ?: "novel") }
    var text by remember { mutableStateOf(sentence?.text ?: sharedText ?: "") }
    var source by remember { mutableStateOf(sentence?.source ?: "") }
    var writer by remember { mutableStateOf(sentence?.writer ?: "") }
    var extra by remember { mutableStateOf(sentence?.extra ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }

    /** 문장 저장 */
    fun saveSentence() {
        scope.launch {
            try {
                val newSentence = DailySentenceEntity(
                    id = sentence?.id ?: 0,
                    date = date,
                    genre = genre,
                    text = text,
                    source = source.takeIf { it.isNotBlank() },
                    writer = writer.takeIf { it.isNotBlank() },
                    extra = extra.takeIf { it.isNotBlank() }
                )

                if (isEditMode) {
                    repository.update(newSentence)
                } else {
                    repository.insert(newSentence)
                }

                onDismiss(true)  // 저장 완료 → 목록 갱신 필요
            } catch (e: Exception) {
                e.printStackTrace()
                onDismiss(false)  // 에러 발생 → 갱신 불필요
            }
        }
    }

    Dialog(onDismissRequest = { onDismiss(false) }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 상단 바
                TopAppBar(
                    title = { Text(if (isEditMode) "문장 수정" else "문장 추가") },
                    navigationIcon = {
                        IconButton(onClick = { onDismiss(false) }) {  // 취소 → 갱신 불필요
                            Icon(Icons.Default.Close, contentDescription = "닫기")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { saveSentence() },
                            enabled = text.isNotBlank()
                        ) {
                            Text("저장")
                        }
                    }
                )

                // 콘텐츠
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // 날짜 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("날짜", style = MaterialTheme.typography.labelMedium)
                                    Text(formatDate(date), style = MaterialTheme.typography.bodyLarge)
                                }
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            }
                        }

                        // 랜덤 날짜 버튼
                        OutlinedCard(
                            modifier = Modifier.clickable {
                                date = generateRandomDate()
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .height(42.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = "랜덤 날짜",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // 장르 선택 (기본 + 사용자 정의)
                    var allGenres by remember { mutableStateOf<List<com.example.dailywidget.data.repository.DataStoreManager.Genre>>(emptyList()) }
                    var showGenreMenu by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        val dataStoreManager = com.example.dailywidget.data.repository.DataStoreManager(context)
                        allGenres = dataStoreManager.getAllGenres()
                    }

                    Column {
                        Text("장르", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        // 현재 선택된 장르 표시
                        val currentGenreDisplayName = remember(genre, allGenres) {
                            allGenres.find { it.id == genre }?.displayName
                                ?: when(genre) {
                                    "novel" -> "소설"
                                    "fantasy" -> "판타지"
                                    "poem" -> "시"
                                    else -> genre
                                }
                        }

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGenreMenu = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currentGenreDisplayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (allGenres.find { it.id == genre }?.isBuiltIn == false) {
                                        Text(
                                            text = "사용자 정의 장르",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 장르 선택 드롭다운
                        DropdownMenu(
                            expanded = showGenreMenu,
                            onDismissRequest = { showGenreMenu = false }
                        ) {
                            // 기본 장르
                            val defaultGenres = allGenres.filter { it.isBuiltIn }
                            if (defaultGenres.isNotEmpty()) {
                                Text(
                                    text = "기본 장르",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )

                                defaultGenres.forEach { genreItem ->
                                    DropdownMenuItem(
                                        text = { Text(genreItem.displayName) },
                                        onClick = {
                                            genre = genreItem.id
                                            showGenreMenu = false
                                        },
                                        leadingIcon = {
                                            if (genre == genreItem.id) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            // 사용자 정의 장르
                            val customGenres = allGenres.filter { !it.isBuiltIn }
                            if (customGenres.isNotEmpty()) {
                                HorizontalDivider()

                                Text(
                                    text = "사용자 정의 장르",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )

                                customGenres.forEach { genreItem ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(genreItem.displayName)
                                                Text(
                                                    text = "ID: ${genreItem.id}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                            }
                                        },
                                        onClick = {
                                            genre = genreItem.id
                                            showGenreMenu = false
                                        },
                                        leadingIcon = {
                                            if (genre == genreItem.id) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 문장 입력 (필수)
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("문장 *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // 출처 입력 (선택)
                    OutlinedTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = { Text("출처") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // 작가 입력 (선택)
                    OutlinedTextField(
                        value = writer,
                        onValueChange = { writer = it },
                        label = { Text("작가") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // 특이사항 입력 (선택)
                    OutlinedTextField(
                        value = extra,
                        onValueChange = { extra = it },
                        label = { Text("특이사항") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    }

    // DatePicker 다이얼로그
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        val month = date.substring(0, 2).toIntOrNull()?.minus(1) ?: 0
        val day = date.substring(2, 4).toIntOrNull() ?: 1
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)

        DatePickerDialog(
            context,
            { _, year, monthOfYear, dayOfMonth ->
                date = String.format("%02d%02d", monthOfYear + 1, dayOfMonth)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showDatePicker = false }
            show()
        }
    }
}

/** 현재 날짜를 MMdd 형식으로 반환 */
private fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("MMdd", Locale.getDefault())
    return sdf.format(Date())
}

/** 날짜 포맷팅 (MMdd → M월 d일) */
private fun formatDate(date: String): String {
    return if (date.length == 4) {
        val month = date.substring(0, 2).toIntOrNull() ?: 0
        val day = date.substring(2, 4).toIntOrNull() ?: 0
        "${month}월 ${day}일"
    } else {
        date
    }
}

/** 랜덤 날짜 생성 (1월 1일 ~ 12월 31일) */
private fun generateRandomDate(): String {
    val month = (1..12).random()
    val maxDay = when (month) {
        2 -> 29  // 윤년 고려
        4, 6, 9, 11 -> 30
        else -> 31
    }
    val day = (1..maxDay).random()
    return String.format("%02d%02d", month, day)
}