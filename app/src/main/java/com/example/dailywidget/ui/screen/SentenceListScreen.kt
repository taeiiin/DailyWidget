package com.example.dailywidget.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import com.example.dailywidget.data.repository.DailySentenceRepository
import com.example.dailywidget.ui.screens.SentenceEditorScreen
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.example.dailywidget.ui.theme.Gray100
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 문장 목록 화면
 * - 장르별 필터링
 * - 검색 (전체/문장/출처/작가/날짜)
 * - 정렬 (최신순/오래된순)
 * - 문장 추가/편집/삭제
 * - 터치 가능한 스크롤바
 * - 스크롤 위치 유지 (편집 화면에서 돌아와도 기존 위치 유지)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceListScreen(
    repository: DailySentenceRepository,
    modifier: Modifier = Modifier,
    sharedText: String? = null,
    onEditModeChange: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var sentences by remember { mutableStateOf<List<DailySentenceEntity>>(emptyList()) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditor by remember { mutableStateOf(false) }
    var editingSentence by remember { mutableStateOf<DailySentenceEntity?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchType by remember { mutableStateOf(SearchType.ALL) }

    var sortOrder by remember { mutableStateOf(SortOrder.ASCENDING) }
    var showSortDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(showEditor) {
        onEditModeChange(showEditor)
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null && !showEditor) {
            editingSentence = null
            showEditor = true
        }
    }

    /** 문장 목록 로드 */
    fun loadSentences(preserveScroll: Boolean = false) {
        scope.launch {
            try {
                if (!preserveScroll) {
                    isLoading = true
                }

                val q = searchQuery.trim()

                val allSentences = if (q.isNotBlank()) {
                    when (searchType) {
                        SearchType.ALL -> repository.searchAll(q)
                        SearchType.TEXT -> repository.searchByText(q)
                        SearchType.SOURCE -> repository.searchBySource(q)
                        SearchType.WRITER -> repository.searchByWriter(q)
                        SearchType.DATE -> repository.searchByDate(q)
                    }
                } else {
                    repository.getAll()
                }

                val filtered = if (selectedGenre != null) {
                    allSentences.filter { it.genre == selectedGenre }
                } else {
                    allSentences
                }

                val newSentences = when (sortOrder) {
                    SortOrder.ASCENDING -> filtered.sortedBy { it.date }
                    SortOrder.DESCENDING -> filtered.sortedByDescending { it.date }
                }

                if (sentences != newSentences) {
                    sentences = newSentences
                }

                if (!preserveScroll) {
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!preserveScroll) {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(selectedGenre, searchQuery, searchType, sortOrder) {
        loadSentences()
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSentence = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "문장 추가")
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // 검색 UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    placeholder = {
                        Text(
                            getSearchPlaceholder(searchType),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 15.sp
                        )
                    },
                    textStyle = TextStyle(fontSize = 15.sp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "검색",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "지우기",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            loadSentences()
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Gray100,
                        unfocusedContainerColor = Gray100,
                        disabledContainerColor = Gray100,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    )
                )

                // 검색 옵션 확장
                AnimatedVisibility(visible = isSearchExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "검색 옵션",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchType.values().forEach { type ->
                                FilterChip(
                                    selected = searchType == type,
                                    onClick = { searchType = type },
                                    label = { Text(type.label) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // 검색 옵션 펼치기/정렬 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            if (isSearchExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSearchExpanded) "접기" else "검색 옵션")
                    }

                    Box {
                        OutlinedButton(
                            onClick = { showSortDropdown = !showSortDropdown },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(sortOrder.label)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                if (showSortDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false }
                        ) {
                            SortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            RadioButton(
                                                selected = sortOrder == order,
                                                onClick = null
                                            )
                                            Text(order.label)
                                        }
                                    },
                                    onClick = {
                                        sortOrder = order
                                        showSortDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 장르 필터 칩
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedGenre == null,
                    onClick = { selectedGenre = null },
                    label = { Text("전체") }
                )
                FilterChip(
                    selected = selectedGenre == "novel",
                    onClick = { selectedGenre = "novel" },
                    label = { Text("소설") }
                )
                FilterChip(
                    selected = selectedGenre == "fantasy",
                    onClick = { selectedGenre = "fantasy" },
                    label = { Text("판타지") }
                )
                FilterChip(
                    selected = selectedGenre == "poem",
                    onClick = { selectedGenre = "poem" },
                    label = { Text("시") }
                )
            }

            HorizontalDivider()

            // 로딩 중
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

            } else if (sentences.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "검색 결과가 없습니다" else "문장이 없습니다",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "다른 검색어를 시도해보세요" else "+ 버튼을 눌러 추가하세요",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

            } else {
                // 문장 목록 + 스크롤바
                val listState = rememberSaveable(saver = LazyListState.Saver) {
                    LazyListState()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            item {
                                Text(
                                    text = "검색 결과: ${sentences.size}개",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        // 날짜별 그룹핑
                        val groupedSentences = sentences.groupBy { it.date }

                        groupedSentences.forEach { (date, dateSentences) ->
                            item {
                                Text(
                                    text = formatDate(date),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(
                                items = dateSentences,
                                key = { it.id }
                            ) { sentence ->
                                SentenceListItem(
                                    sentence = sentence,
                                    searchQuery = searchQuery,
                                    onClick = {
                                        editingSentence = sentence
                                        showEditor = true
                                    },
                                    onDelete = {
                                        scope.launch {
                                            repository.delete(sentence)
                                            loadSentences(preserveScroll = true)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 터치 가능한 스크롤바
                    LazyColumnScrollbar(
                        listState = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(top = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }

        // 편집 화면 (오버레이)
        if (showEditor) {
            SentenceEditorScreen(
                repository = repository,
                sentence = editingSentence,
                sharedText = sharedText,
                onDismiss = { needsRefresh ->
                    showEditor = false
                    editingSentence = null
                    if (needsRefresh) {
                        loadSentences(preserveScroll = true)
                    }
                }
            )
        }
    }
}

// ==================== 터치 가능한 스크롤바 ====================

/**
 * 터치 가능한 스크롤바
 * 드래그하여 빠른 스크롤 가능
 */
@Composable
fun LazyColumnScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    if (listState.layoutInfo.totalItemsCount == 0) return

    val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
    if (viewportHeight <= 0) return

    var isDragging by remember { mutableStateOf(false) }
    var dragStartOffset by remember { mutableStateOf(0f) }

    val estimatedItemHeight = remember(listState.layoutInfo.visibleItemsInfo) {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        if (visibleItems.isNotEmpty()) {
            visibleItems.sumOf { it.size } / visibleItems.size
        } else {
            100
        }
    }

    val totalContentHeight = listState.layoutInfo.totalItemsCount * estimatedItemHeight

    if (totalContentHeight <= viewportHeight) return

    val scrollbarHeight = remember(viewportHeight, totalContentHeight) {
        (viewportHeight.toFloat() / totalContentHeight * viewportHeight)
            .coerceAtLeast(40f)
            .coerceAtMost(viewportHeight.toFloat())
    }

    val scrollbarOffset = if (!isDragging) {
        val firstVisibleItemIndex = listState.firstVisibleItemIndex
        val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset

        val scrolledAmount = firstVisibleItemIndex * estimatedItemHeight + firstVisibleItemScrollOffset
        val maxScrollAmount = totalContentHeight - viewportHeight
        val scrollProgress = if (maxScrollAmount > 0) {
            (scrolledAmount.toFloat() / maxScrollAmount).coerceIn(0f, 1f)
        } else {
            0f
        }

        val maxScrollbarOffset = viewportHeight - scrollbarHeight
        (scrollProgress * maxScrollbarOffset).coerceIn(0f, maxScrollbarOffset)
    } else {
        dragStartOffset
    }

    val scope = rememberCoroutineScope()
    val maxScrollbarOffset = viewportHeight - scrollbarHeight
    val maxScrollAmount = totalContentHeight - viewportHeight

    Box(
        modifier = modifier
            .width(16.dp)
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(0, scrollbarOffset.toInt()) }
                .width(if (isDragging) 12.dp else 8.dp)
                .height(scrollbarHeight.dp)
                .background(
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    },
                    shape = RoundedCornerShape(if (isDragging) 6.dp else 4.dp)
                )
                .pointerInput(Unit) {
                    var initialTouchY = 0f
                    var initialScrollbarOffset = 0f

                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            initialTouchY = offset.y
                            initialScrollbarOffset = scrollbarOffset
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()

                            val currentTouchY = change.position.y
                            val touchDelta = currentTouchY - initialTouchY

                            val newScrollbarOffset = (initialScrollbarOffset + touchDelta)
                                .coerceIn(0f, maxScrollbarOffset)

                            dragStartOffset = newScrollbarOffset

                            val newScrollProgress = if (maxScrollbarOffset > 0) {
                                newScrollbarOffset / maxScrollbarOffset
                            } else {
                                0f
                            }

                            val targetScrollAmount = (newScrollProgress * maxScrollAmount).toInt()
                            val targetIndex = (targetScrollAmount / estimatedItemHeight)
                                .coerceIn(0, listState.layoutInfo.totalItemsCount - 1)
                            val targetOffset = (targetScrollAmount % estimatedItemHeight)
                                .coerceAtLeast(0)

                            scope.launch {
                                listState.scrollToItem(targetIndex, targetOffset)
                            }
                        }
                    )
                }
        )
    }
}

// ==================== 기타 컴포넌트 ====================

/** 검색 타입 */
enum class SearchType(val label: String) {
    ALL("전체"),
    TEXT("문장"),
    SOURCE("출처"),
    WRITER("작가"),
    DATE("날짜")
}

/** 정렬 순서 */
enum class SortOrder(val label: String) {
    ASCENDING("1월부터"),
    DESCENDING("12월부터")
}

/** 검색 타입별 플레이스홀더 */
private fun getSearchPlaceholder(type: SearchType): String {
    return when (type) {
        SearchType.ALL -> "문장, 출처, 작가 검색"
        SearchType.TEXT -> "문장 내용 검색"
        SearchType.SOURCE -> "출처 검색"
        SearchType.WRITER -> "작가 이름 검색"
        SearchType.DATE -> "날짜 검색 (예: 01)"
    }
}

/** 문장 목록 아이템 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceListItem(
    sentence: DailySentenceEntity,
    searchQuery: String = "",
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
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
                Text(
                    text = sentence.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    sentence.genre?.let {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = getGenreLabel(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

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

/** 날짜 포맷팅 (MMDD → M월 D일) */
private fun formatDate(date: String): String {
    return if (date.length == 4) {
        val month = date.substring(0, 2).toIntOrNull() ?: 0
        val day = date.substring(2, 4).toIntOrNull() ?: 0
        "${month}월 ${day}일"
    } else {
        date
    }
}

/** 장르 라벨 */
private fun getGenreLabel(genre: String): String {
    return when (genre) {
        "novel" -> "소설"
        "fantasy" -> "판타지"
        "poem" -> "시"
        else -> genre
    }
}