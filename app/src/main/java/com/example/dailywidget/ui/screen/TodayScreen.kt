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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.saveable.rememberSaveable
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
    val dataStoreManager = remember { DataStoreManager(context) }

    var sentences by remember { mutableStateOf<List<DailySentenceEntity>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var displayConfig by remember { mutableStateOf(DataStoreManager.DisplayConfig()) }
    var fontSizeConfig by remember { mutableStateOf(DataStoreManager.FontSizeConfig()) }
    var lastCheckedDate by remember { mutableStateOf("") }

    // ⭐ 뷰 모드 상태
    val viewMode by dataStoreManager.getHomeViewModeFlow().collectAsState(
        initial = DataStoreManager.HomeViewMode.CARD
    )

    // ⭐ 편집/삭제를 위한 상태
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingSentence by remember { mutableStateOf<DailySentenceEntity?>(null) }

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

            // ⭐ 뷰 모드 토글 버튼 추가
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FilterChip(
                    selected = viewMode == DataStoreManager.HomeViewMode.CARD,
                    onClick = {
                        scope.launch {
                            dataStoreManager.saveHomeViewMode(DataStoreManager.HomeViewMode.CARD)
                        }
                    },
                    label = { Text("카드뷰") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ViewModule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilterChip(
                    selected = viewMode == DataStoreManager.HomeViewMode.LIST,
                    onClick = {
                        scope.launch {
                            dataStoreManager.saveHomeViewMode(DataStoreManager.HomeViewMode.LIST)
                        }
                    },
                    label = { Text("목록뷰") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ViewList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            // ⭐ 뷰 모드에 따라 다른 UI 표시
            when (viewMode) {
                DataStoreManager.HomeViewMode.CARD -> {
                    // 기존 카드뷰 (HorizontalPager)
                    CardView(
                        sentences = sentences,
                        displayConfig = displayConfig,
                        fontSizeConfig = fontSizeConfig,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                DataStoreManager.HomeViewMode.LIST -> {
                    // 새로운 목록뷰
                    ListView(
                        sentences = sentences,
                        repository = repository,
                        onEdit = { sentence ->
                            editingSentence = sentence
                            showEditorDialog = true
                        },
                        onReload = {
                            loadTodayData(getCurrentDate())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
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

/**
 * ⭐ 카드뷰 컴포넌트
 */
@Composable
private fun CardView(
    sentences: List<DailySentenceEntity>,
    displayConfig: DataStoreManager.DisplayConfig,
    fontSizeConfig: DataStoreManager.FontSizeConfig,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { sentences.size }
    val context = LocalContext.current

    Column(modifier = modifier) {
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

        // 페이징 UI
        if (sentences.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0)
                            scope.launch { pagerState.scrollToPage(pagerState.currentPage - 1) }
                    },
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
                    onClick = {
                        if (pagerState.currentPage < sentences.size - 1)
                            scope.launch { pagerState.scrollToPage(pagerState.currentPage + 1) }
                    },
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

        // 공유 버튼
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
    }
}

/**
 * ⭐ 목록뷰 컴포넌트
 */
@Composable
private fun ListView(
    sentences: List<DailySentenceEntity>,
    repository: DailySentenceRepository,
    onEdit: (DailySentenceEntity) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = sentences,
            key = { it.id }
        ) { sentence ->
            TodayListItem(
                sentence = sentence,
                onEdit = { onEdit(sentence) },
                onDelete = {
                    scope.launch {
                        repository.delete(sentence)
                        onReload()
                    }
                }
            )
        }
    }
}

/**
 * ⭐ 오늘의 문장 목록 아이템 (편집/삭제 가능)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayListItem(
    sentence: DailySentenceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 문장
                Text(
                    text = sentence.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 출처, 작가 & 장르
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sourceWriter = buildString {
                        if (!sentence.source.isNullOrEmpty()) append(sentence.source)
                        if (!sentence.writer.isNullOrEmpty()) {
                            if (isNotEmpty()) append(", ")
                            append(sentence.writer)
                        }
                    }

                    if (sourceWriter.isNotEmpty()) {
                        Text(
                            text = sourceWriter,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    // 장르
                    sentence.genre?.let {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = when (it) {
                                    "novel" -> "소설"
                                    "fantasy" -> "판타지"
                                    "poem" -> "시"
                                    else -> it
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 삭제 버튼
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("문장 삭제") },
            text = { Text("이 문장을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}