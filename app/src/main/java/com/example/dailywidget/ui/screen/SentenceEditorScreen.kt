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
 *
 * ⭐ styleId, backgroundId 제거됨
 * 스타일/배경은 설정에서 전역 관리
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceEditorScreen(
    repository: DailySentenceRepository,
    sentence: DailySentenceEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 편집 모드 (null이면 추가, 아니면 수정)
    val isEditMode = sentence != null

    // 상태 관리
    var date by remember { mutableStateOf(sentence?.date ?: getCurrentDate()) }
    var genre by remember { mutableStateOf(sentence?.genre ?: "novel") }
    var text by remember { mutableStateOf(sentence?.text ?: "") }
    var source by remember { mutableStateOf(sentence?.source ?: "") }
    var writer by remember { mutableStateOf(sentence?.writer ?: "") }
    var extra by remember { mutableStateOf(sentence?.extra ?: "") }

    // 다이얼로그 상태
    var showDatePicker by remember { mutableStateOf(false) }

    // 저장 처리
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
                    // ⭐ styleId, backgroundId 제거됨
                )

                if (isEditMode) {
                    repository.update(newSentence)
                } else {
                    repository.insert(newSentence)
                }

                onDismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
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
                        IconButton(onClick = onDismiss) {
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
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
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

                    // 장르 선택
                    Column {
                        Text("장르", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = genre == "novel",
                                onClick = { genre = "novel" },
                                label = { Text("소설") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = genre == "fantasy",
                                onClick = { genre = "fantasy" },
                                label = { Text("판타지") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = genre == "essay",
                                onClick = { genre = "essay" },
                                label = { Text("에세이") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 문장 입력
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("문장 *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    // 출처 입력
                    OutlinedTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = { Text("출처") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 작가 입력
                    OutlinedTextField(
                        value = writer,
                        onValueChange = { writer = it },
                        label = { Text("작가") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 특이사항 입력
                    OutlinedTextField(
                        value = extra,
                        onValueChange = { extra = it },
                        label = { Text("특이사항") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    // ⭐ 스타일/배경 선택 카드 제거됨
                    // 이제 설정에서 전역적으로 관리
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

/**
 * 현재 날짜를 MMdd 형식으로 반환
 */
private fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("MMdd", Locale.getDefault())
    return sdf.format(Date())
}

/**
 * 날짜 포맷팅 (MMdd → M월 d일)
 */
private fun formatDate(date: String): String {
    return if (date.length == 4) {
        val month = date.substring(0, 2).toIntOrNull() ?: 0
        val day = date.substring(2, 4).toIntOrNull() ?: 0
        "${month}월 ${day}일"
    } else {
        date
    }
}