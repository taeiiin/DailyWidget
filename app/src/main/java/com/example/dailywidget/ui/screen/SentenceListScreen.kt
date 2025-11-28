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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
//import androidx.compose.runtime.composed

/**
 * 문장 목록 화면
 *
 * 기능:
 * - 장르별 필터링
 * - 검색 (전체/문장/출처/작가/날짜)
 * - 정렬 (최신순/오래된순)
 * - 문장 추가/편집/삭제
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceListScreen(
    repository: DailySentenceRepository,
    modifier: Modifier = Modifier,
    onEditModeChange: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var sentences by remember { mutableStateOf<List<DailySentenceEntity>>(emptyList()) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditor by remember { mutableStateOf(false) }
    var editingSentence by remember { mutableStateOf<DailySentenceEntity?>(null) }

    // 검색 상태
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }  // ⭐ 변수명 변경
    var searchType by remember { mutableStateOf(SearchType.ALL) }

    // ⭐ 정렬 상태
    var sortOrder by remember { mutableStateOf(SortOrder.ASCENDING) }
    var showSortDropdown by remember { mutableStateOf(false) }

    // 편집 모드 변경 알림
    LaunchedEffect(showEditor) {
        onEditModeChange(showEditor)
    }

    fun loadSentences() {
        scope.launch {
            try {
                isLoading = true

                val q = searchQuery.trim()

                // 검색 중이면 검색 쿼리 실행 (trim 처리)
                val allSentences = if (q.isNotBlank()) {
                    // 검색어로 호출 전 로그(디버깅)
                    // Log.d("Search", "query='$q'")
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

                // 장르 필터링
                val filtered = if (selectedGenre != null) {
                    allSentences.filter { it.genre == selectedGenre }
                } else {
                    allSentences
                }

                // ⭐ 정렬 적용
                sentences = when (sortOrder) {
                    SortOrder.ASCENDING -> filtered.sortedBy { it.date }  // 오래된순
                    SortOrder.DESCENDING -> filtered.sortedByDescending { it.date }  // 최신순
                }

                // (디버깅) 검색 상태일 때 결과 개수 즉시 확인 가능
                // Log.d("Search", "results=${sentences.size}")

                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }


    LaunchedEffect(selectedGenre, searchQuery, searchType, sortOrder) {
        loadSentences()
    }

    // 편집 화면
    if (showEditor) {
        SentenceEditorScreen(
            repository = repository,
            sentence = editingSentence,
            onDismiss = {
                showEditor = false
                editingSentence = null
                loadSentences()
            }
        )
        return
    }

    // 목록 화면
    Scaffold(
        modifier = modifier,
        // ⭐ topBar 삭제
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSentence = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,  // ⭐ 명시적 색상 지정
                contentColor = MaterialTheme.colorScheme.onPrimary   // ⭐ 아이콘 색상
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

            // ⭐ 새로운 검색 UI
            // ⭐ 검색 UI (iOS 스타일)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // 검색 입력 필드 (iOS 스타일)
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),  // ⭐ 48dp → 52dp로 증가
                    placeholder = {
                        Text(
                            getSearchPlaceholder(searchType),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 15.sp  // ⭐ 폰트 크기 명시
                        )
                    },
                    textStyle = TextStyle(fontSize = 15.sp),  // ⭐ 입력 텍스트도 15sp
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
                    shape = RoundedCornerShape(12.dp),  // ⭐ 둥근 모서리
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Gray100,  // ⭐ 밝은 회색 배경
                        unfocusedContainerColor = Gray100,
                        disabledContainerColor = Gray100,
                        focusedIndicatorColor = Color.Transparent,  // ⭐ 테두리 제거
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    )
                )

                // 검색 옵션 토글
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
                    // 검색 옵션 펼치기
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

                    // 정렬 버튼 (드롭다운)
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

                        // ⭐ 드롭다운 메뉴
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
                    selected = selectedGenre == "essay",
                    onClick = { selectedGenre = "essay" },
                    label = { Text("에세이") }
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
                // 문장 없음
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
                // 문장 목록
                val scrollState = rememberLazyListState()

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawVerticalScrollbar(scrollState),  // ⭐ 스크롤바 추가
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // 검색 결과 개수 표시
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

                        // 날짜 헤더
                        item {
                            Text(
                                text = formatDate(date),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // 해당 날짜의 문장들
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
                                        loadSentences()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 검색 타입
 */
enum class SearchType(val label: String) {
    ALL("전체"),
    TEXT("문장"),
    SOURCE("출처"),
    WRITER("작가"),
    DATE("날짜")
}

/**
 * ⭐ 정렬 순서
 */
enum class SortOrder(val label: String) {
    ASCENDING("오래된순"),      // 날짜 오름차순 (0101 → 1231)
    DESCENDING("최신순")        // 날짜 내림차순 (1231 → 0101)
}

/**
 * 검색 타입별 플레이스홀더
 */
private fun getSearchPlaceholder(type: SearchType): String {
    return when (type) {
        SearchType.ALL -> "문장, 출처, 작가 검색"
        SearchType.TEXT -> "문장 내용 검색"
        SearchType.SOURCE -> "출처 검색"
        SearchType.WRITER -> "작가 이름 검색"
        SearchType.DATE -> "날짜 검색 (예: 01)"
    }
}

/**
 * 문장 목록 아이템
 */
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
                // 문장
                Text(
                    text = sentence.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 출처, 작가 & 장르
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 출처, 작가
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 장르
                    sentence.genre?.let {
                        Text(
                            text = if (sourceWriter.isNotEmpty()) " • ${getGenreLabel(it)}" else getGenreLabel(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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

/**
 * 날짜 포맷팅 (MMDD → M월 D일)
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

/**
 * 장르 라벨
 */
private fun getGenreLabel(genre: String): String {
    return when (genre) {
        "novel" -> "소설"
        "fantasy" -> "판타지"
        "essay" -> "에세이"
        else -> genre
    }
}

/**
 * iOS 스타일 스크롤바
 */
@Composable
fun Modifier.drawVerticalScrollbar(
    state: LazyListState
): Modifier {
    // ⭐ 스크롤 후 1.5초 동안 유지
    var hideScrollbar by remember { mutableStateOf(false) }

    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            hideScrollbar = false
        } else {
            kotlinx.coroutines.delay(1500)  // ⭐ 1.5초 대기
            hideScrollbar = true
        }
    }

    val targetAlpha = if (!hideScrollbar) 0.6f else 0f  // ⭐ 0.5f → 0.6f (더 진하게)
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(
            durationMillis = if (targetAlpha > 0f) 150 else 300,  // ⭐ 사라질 때 300ms
            delayMillis = 0
        ),
        label = "scrollbar"
    )

    return drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index

        if (firstVisibleElementIndex != null && alpha > 0f) {
            val totalItemsCount = state.layoutInfo.totalItemsCount
            val visibleItemsCount = state.layoutInfo.visibleItemsInfo.size

            if (totalItemsCount > visibleItemsCount) {
                val elementHeight = size.height / totalItemsCount
                val scrollbarHeight = visibleItemsCount * elementHeight * 10
                val scrollbarOffsetY = firstVisibleElementIndex * elementHeight

                val scrollbarWidth = 4.dp.toPx()
                val scrollbarX = size.width - scrollbarWidth - 8.dp.toPx()  // ⭐ 여백도 증가

                drawRoundRect(
                    color = Color.Gray.copy(alpha = alpha),
                    topLeft = Offset(scrollbarX, scrollbarOffsetY),
                    size = Size(scrollbarWidth, scrollbarHeight),
                    cornerRadius = CornerRadius(scrollbarWidth / 2)
                )
            }
        }
    }
}