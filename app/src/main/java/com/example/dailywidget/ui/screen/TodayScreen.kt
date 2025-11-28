package com.example.dailywidget.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import com.example.dailywidget.data.repository.DailySentenceRepository
import com.example.dailywidget.data.repository.DataStoreManager
import com.example.dailywidget.util.StyleManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.*

// Pager 관련 import
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun TodayScreen(
    repository: DailySentenceRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var sentences by remember { mutableStateOf<List<DailySentenceEntity>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var displayConfig by remember { mutableStateOf(DataStoreManager.DisplayConfig()) }
    var fontSizeConfig by remember { mutableStateOf(DataStoreManager.FontSizeConfig()) }
    var lastCheckedDate by remember { mutableStateOf("") }

    val swipeOffset by remember { mutableStateOf(0f) } // 기존 카드 드래그는 유지

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("MMdd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun loadTodayData(date: String) {
        scope.launch {
            try {
                isLoading = true
                val dataStoreManager = DataStoreManager(context)
                displayConfig = dataStoreManager.getDisplayConfig()
                fontSizeConfig = dataStoreManager.getFontSizeConfig()
                val result = repository.getSentences(date)
                sentences = result
                currentIndex = 0
                lastCheckedDate = date
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTodayData(getCurrentDate())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentDate = getCurrentDate()
                if (currentDate != lastCheckedDate && lastCheckedDate.isNotEmpty()) {
                    loadTodayData(currentDate)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (sentences.isNotEmpty()) {

            // ==================== 날짜 표시 ====================
            val dateText = remember(lastCheckedDate) {
                try {
                    // 현재 연도 추가
                    val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                    val inputFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val date = inputFormat.parse("$currentYear$lastCheckedDate")
                    if (date != null) {
                        val outputFormat = SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN)
                        outputFormat.format(date)
                    } else ""
                } catch (e: Exception) { "" }
            }

            if (dateText.isNotEmpty()) {
                Text(
                    text = dateText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            val pagerState = rememberPagerState { sentences.size }

            HorizontalPager(
                state = pagerState,
                pageSpacing = 16.dp,
                contentPadding = PaddingValues(horizontal = 32.dp),
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val sentence = sentences[page]
                val style = StyleManager.getWidgetStyle(1)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = sentence.text,
                            fontSize = fontSizeConfig.textSize.sp,
                            color = style.textStyle.color,
                            textAlign = style.textStyle.align,
                            lineHeight = (fontSizeConfig.textSize * 1.5).sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val sourceWriterText = buildSourceWriterText(
                            sentence.source,
                            sentence.writer,
                            displayConfig.showSource,
                            displayConfig.showWriter
                        )
                        if (sourceWriterText.isNotEmpty()) {
                            Text(
                                text = sourceWriterText,
                                fontSize = fontSizeConfig.sourceSize.sp,
                                color = style.sourceStyle.color,
                                textAlign = style.sourceStyle.align,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!sentence.extra.isNullOrEmpty() && displayConfig.showExtra) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = sentence.extra!!,
                                fontSize = fontSizeConfig.extraSize.sp,
                                color = style.extraStyle.color,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== 페이징 UI ====================
            if (sentences.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (pagerState.currentPage > 0) scope.launch { pagerState.scrollToPage(pagerState.currentPage - 1) } },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "이전",
                            tint = if (pagerState.currentPage > 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    Text(
                        text = "${pagerState.currentPage + 1} / ${sentences.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { if (pagerState.currentPage < sentences.size - 1) scope.launch { pagerState.scrollToPage(pagerState.currentPage + 1) } },
                        enabled = pagerState.currentPage < sentences.size - 1
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "다음",
                            tint = if (pagerState.currentPage < sentences.size - 1)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ==================== 공유 버튼 ====================
            Button(
                onClick = { shareSentence(context, sentences[pagerState.currentPage], displayConfig) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("공유하기")
            }

        } else {
            // 문장 없을 때 UI
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "오늘의 문장이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "문장을 추가해보세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun buildSourceWriterText(
    source: String?,
    writer: String?,
    showSource: Boolean,
    showWriter: Boolean
): String {
    val parts = mutableListOf<String>()
    if (showSource && !source.isNullOrEmpty()) parts.add(source)
    if (showWriter && !writer.isNullOrEmpty()) parts.add(writer)
    return if (parts.isNotEmpty()) "- ${parts.joinToString(", ")}" else ""
}

private fun shareSentence(
    context: Context,
    sentence: DailySentenceEntity,
    displayConfig: DataStoreManager.DisplayConfig
) {
    val shareText = buildString {
        append(sentence.text)
        val sourceWriter = buildSourceWriterText(
            sentence.source,
            sentence.writer,
            displayConfig.showSource,
            displayConfig.showWriter
        )
        if (sourceWriter.isNotEmpty()) append("\n\n$sourceWriter")
        if (!sentence.extra.isNullOrEmpty() && displayConfig.showExtra) append("\n${sentence.extra}")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "문장 공유하기")
    context.startActivity(shareIntent)
}
