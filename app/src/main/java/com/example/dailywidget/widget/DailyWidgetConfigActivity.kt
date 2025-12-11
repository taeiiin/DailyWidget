package com.example.dailywidget.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import com.example.dailywidget.data.model.ColorPaletteData
import com.example.dailywidget.data.repository.DataStoreManager
import com.example.dailywidget.ui.components.StylePreview
import com.example.dailywidget.ui.theme.DailyWidgetTheme
import com.example.dailywidget.util.ImageManager
import com.example.dailywidget.util.StyleManager
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.example.dailywidget.util.ThemeManager

/**
 * 위젯 설정 액티비티
 * - 스타일 선택 (10가지)
 * - 배경 선택 (컬러/그라디언트/이미지)
 * - 투명도 조절 (10%~100%, 5% 단위)
 * - 탭 액션 선택
 * - 위젯 미리보기
 */
class DailyWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            DailyWidgetTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    onSave = { styleId: Int, backgroundId: String ->
                        saveWidgetConfig(styleId, backgroundId)
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun saveWidgetConfig(styleId: Int, backgroundId: String) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        scope.launch {
            try {
                val dataStoreManager = DataStoreManager(this@DailyWidgetConfigActivity)

                // 1. 복수 장르 조회
                val genres = dataStoreManager.getWidgetGenres(appWidgetId)

                // 2. 스타일과 배경 저장
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    dataStoreManager.saveWidgetStyleAndBackground(appWidgetId, styleId, backgroundId)
                }

                // 3. 위젯 즉시 업데이트 (IO 스레드에서 동기 실행)
                val updateSuccess = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val appWidgetManager = AppWidgetManager.getInstance(this@DailyWidgetConfigActivity)
                        val provider = UnifiedWidgetProvider()

                        // 직접 업데이트 (코루틴 내부에서)
                        val db = AppDatabase.getDatabase(this@DailyWidgetConfigActivity)
                        val dao = db.dailySentenceDao()

                        val today = java.text.SimpleDateFormat("MMdd", java.util.Locale.getDefault()).format(java.util.Date())
                        val filteredSentences = dao.getSentencesByDateAndGenres(today, genres)

                        if (filteredSentences.isNotEmpty()) {
                            val views = android.widget.RemoteViews(packageName, R.layout.widget_daily)

                            // 탭 액션 설정
                            val tapAction = dataStoreManager.getWidgetTapAction(appWidgetId)
                            val genresString = genres.joinToString(",")

                            val tapIntent = Intent(this@DailyWidgetConfigActivity, UnifiedWidgetProvider::class.java).apply {
                                action = "com.example.dailywidget.ACTION_REFRESH"
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                putExtra("extra_genre", genresString)
                            }

                            val tapPendingIntent = PendingIntent.getBroadcast(
                                this@DailyWidgetConfigActivity,
                                appWidgetId,
                                tapIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            views.setOnClickPendingIntent(R.id.widget_container, tapPendingIntent)
                            appWidgetManager.updateAppWidget(appWidgetId, views)

                            kotlinx.coroutines.delay(500)

                            // 이제 정식 업데이트
                            provider.updateAppWidgetWithGenres(
                                context = this@DailyWidgetConfigActivity,
                                appWidgetManager = appWidgetManager,
                                appWidgetId = appWidgetId,
                                genres = genres,
                                forceRefresh = true
                            )

                            kotlinx.coroutines.delay(1000)
                        }

                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }

                // 4. 잠금 해제
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    dataStoreManager.setWidgetUpdateLock(appWidgetId, false)
                }

                // 5. Activity 종료
                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, resultValue)
                finish()

            } catch (e: Exception) {
                e.printStackTrace()

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val dataStoreManager = DataStoreManager(this@DailyWidgetConfigActivity)
                        dataStoreManager.setWidgetUpdateLock(appWidgetId, false)
                    } catch (ignored: Exception) {}
                }

                finish()
            }
        }
    }
}

/** 위젯 설정 화면 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    onSave: (styleId: Int, backgroundId: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    val initialConfig = dataStoreManager.getWidgetConfigFlow(appWidgetId).collectAsState(
        initial = DataStoreManager.WidgetConfig(
            StyleManager.DEFAULT_STYLE_ID,
            StyleManager.DEFAULT_BACKGROUND_ID
        )
    )

    var selectedStyleId by remember { mutableStateOf(initialConfig.value.styleId) }
    var selectedBackgroundId by remember { mutableStateOf(initialConfig.value.backgroundId) }
    var showStyleDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }

    // 복수 장르 이름 가져오기 (제목용)
    var genresDisplayName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val genres = dataStoreManager.getWidgetGenres(appWidgetId)
        genresDisplayName = dataStoreManager.getGenresDisplayName(genres)
    }

    LaunchedEffect(initialConfig.value) {
        selectedStyleId = initialConfig.value.styleId
        selectedBackgroundId = initialConfig.value.backgroundId
    }

    val currentBgConfig = parseBackgroundId(selectedBackgroundId)

    // alpha를 State로 관리 (반응형)
    var currentAlpha by remember(selectedBackgroundId) { mutableStateOf(currentBgConfig.alpha) }
    val currentHex by remember(selectedBackgroundId) { mutableStateOf(currentBgConfig.hexColor) }

    // 배경 업데이트 함수들
    val updateSolidBackgroundId: (hex: String, alpha: Float) -> Unit = { hex, alpha ->
        selectedBackgroundId = "solid:$hex,alpha:${"%.2f".format(alpha)}"
        currentAlpha = alpha
    }

    val updateImageBackgroundId: (imageName: String, alpha: Float) -> Unit = { imageName, alpha ->
        selectedBackgroundId = "image:$imageName,alpha:${"%.2f".format(alpha)}"
        currentAlpha = alpha
    }

    val updateGradientBackgroundId: (start: String, end: String, dir: String, alpha: Float) -> Unit =
        { start, end, dir, alpha ->
            selectedBackgroundId = "gradient:$start,$end,$dir,alpha:${"%.2f".format(alpha)}"
            currentAlpha = alpha
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (genresDisplayName.isNotEmpty()) {
                            "$genresDisplayName 위젯 설정"
                        } else {
                            "위젯 설정"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "취소")
                    }
                },
                actions = {
                    TextButton(onClick = { onSave(selectedStyleId, selectedBackgroundId) }) {
                        Text("완료")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. 스타일 선택
                Text(
                    text = "스타일",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStyleDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "스타일 $selectedStyleId - ${StyleManager.getStyleDescription(selectedStyleId)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "탭하여 변경",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "스타일 아이콘",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 2. 배경 선택
                Text(text = "배경", style = MaterialTheme.typography.titleMedium)
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBackgroundDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val desc = when {
                            currentBgConfig.isGradient -> "그라디언트 배경"
                            currentBgConfig.isImage -> "이미지: ${currentBgConfig.imageName}"
                            currentBgConfig.hexColor == "#FFFFFF" -> "기본 배경 (흰색)"
                            else -> "컬러 배경"
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(desc, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "탭하여 변경",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "배경 아이콘",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 3. 투명도 슬라이더
                Column {
                    Text(
                        "불투명도 (${"%.0f".format(currentAlpha * 100)}%)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = currentAlpha,
                        onValueChange = { newAlpha ->
                            val roundedAlpha = (kotlin.math.round(newAlpha * 20) / 20).coerceIn(0.1f, 1f)
                            currentAlpha = roundedAlpha

                            when {
                                currentBgConfig.isSolid -> {
                                    updateSolidBackgroundId(currentHex, roundedAlpha)
                                }
                                currentBgConfig.isImage && currentBgConfig.imageName != null -> {
                                    updateImageBackgroundId(currentBgConfig.imageName, roundedAlpha)
                                }
                                currentBgConfig.isGradient &&
                                        currentBgConfig.gradientStartColor != null &&
                                        currentBgConfig.gradientEndColor != null &&
                                        currentBgConfig.gradientDirection != null -> {
                                    updateGradientBackgroundId(
                                        currentBgConfig.gradientStartColor,
                                        currentBgConfig.gradientEndColor,
                                        currentBgConfig.gradientDirection,
                                        roundedAlpha
                                    )
                                }
                            }
                        },
                        valueRange = 0.1f..1f,
                        steps = 17
                    )
                }

                // 3. 탭 액션 선택
                Text(
                    text = "위젯 클릭 동작",
                    style = MaterialTheme.typography.titleMedium
                )

                var showTapActionDialog by remember { mutableStateOf(false) }
                var selectedTapAction by remember {
                    mutableStateOf(DataStoreManager.WidgetTapAction.OPEN_APP)
                }

                LaunchedEffect(Unit) {
                    selectedTapAction = dataStoreManager.getWidgetTapAction(appWidgetId)
                }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTapActionDialog = true }
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
                                text = selectedTapAction.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedTapAction.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = "탭 액션",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 탭 액션 선택 다이얼로그
                if (showTapActionDialog) {
                    TapActionSelectionDialog(
                        selectedAction = selectedTapAction,
                        onActionSelected = { action ->
                            selectedTapAction = action
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                dataStoreManager.saveWidgetTapAction(appWidgetId, action)
                            }
                        },
                        onDismiss = { showTapActionDialog = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 위젯 미리보기
            HorizontalDivider()
            WidgetPreview(
                selectedStyleId = selectedStyleId,
                currentBgConfig = currentBgConfig.copy(alpha = currentAlpha)
            )
        }
    }

    // 스타일 선택 다이얼로그
    if (showStyleDialog) {
        StyleSelectionDialog(
            selectedStyleId = selectedStyleId,
            onStyleSelected = { selectedStyleId = it },
            onDismiss = { showStyleDialog = false }
        )
    }

    // 배경 선택 다이얼로그
    if (showBackgroundDialog) {
        BackgroundSelectionDialog(
            currentBgConfig = currentBgConfig,
            currentAlpha = currentAlpha,
            onBackgroundSelected = { newBgId ->
                selectedBackgroundId = newBgId
                currentAlpha = parseBackgroundId(newBgId).alpha
            },
            onDismiss = { showBackgroundDialog = false }
        )
    }
}

/** 위젯 미리보기 컴포넌트 */
@Composable
fun WidgetPreview(
    selectedStyleId: Int,
    currentBgConfig: BackgroundConfig
) {
    val context = LocalContext.current
    val style = StyleManager.getWidgetStyle(selectedStyleId)

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            "위젯 미리보기",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .then(
                    if (currentBgConfig.isGradient &&
                        currentBgConfig.gradientStartColor != null &&
                        currentBgConfig.gradientEndColor != null) {
                        Modifier.background(
                            brush = when (currentBgConfig.gradientDirection) {
                                "horizontal" -> androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientStartColor)),
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientEndColor))
                                    )
                                )
                                "vertical" -> androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientStartColor)),
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientEndColor))
                                    )
                                )
                                "diagonal_down" -> androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientStartColor)),
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientEndColor))
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset.Infinite
                                )
                                "diagonal_up" -> androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientStartColor)),
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientEndColor))
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY),
                                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
                                )
                                else -> androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientStartColor)),
                                        Color(android.graphics.Color.parseColor(currentBgConfig.gradientEndColor))
                                    )
                                )
                            },
                            alpha = currentBgConfig.alpha,
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else if (currentBgConfig.isSolid) {
                        Modifier.background(
                            color = Color(android.graphics.Color.parseColor(currentBgConfig.hexColor))
                                .copy(alpha = currentBgConfig.alpha),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        Modifier.background(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            // 이미지 배경 (투명도 적용)
            if (currentBgConfig.isImage && currentBgConfig.imageName != null) {
                if (currentBgConfig.isThemeImage) {
                    val assetPath = ThemeManager.getAssetPath(currentBgConfig.imageName)

                    if (assetPath != null) {
                        AsyncImage(
                            model = "file:///android_asset/$assetPath",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = currentBgConfig.alpha
                        )
                    }
                } else if (currentBgConfig.imageName.startsWith("file://")) {
                    // 사용자 이미지
                    val fileName = currentBgConfig.imageName.substringAfter("file://")
                    val file = ImageManager.getImageFile(context, fileName)
                    if (file != null) {
                        AsyncImage(
                            model = file,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = currentBgConfig.alpha
                        )
                    }
                } else {
                    // Drawable 이미지
                    val resId = context.resources.getIdentifier(
                        currentBgConfig.imageName,
                        "drawable",
                        context.packageName
                    )
                    if (resId != 0) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(resId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = currentBgConfig.alpha
                        )
                    }
                }
            }

            // 텍스트 내용
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "만나서 반갑습니다.\n자유롭게 사용하시길 바랍니다.",
                    color = style.textStyle.color,
                    fontSize = 20.sp,
                    textAlign = style.textStyle.align,
                    fontWeight = if (style.textStyle.isBold) {
                        androidx.compose.ui.text.font.FontWeight.Bold
                    } else {
                        androidx.compose.ui.text.font.FontWeight.Normal
                    },
                    lineHeight = 30.sp,
                    modifier = Modifier.weight(1f).wrapContentHeight(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "- 작가명, 책 제목",
                    color = style.sourceStyle.color,
                    fontSize = 14.sp,
                    textAlign = style.sourceStyle.align,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "P. 123",
                    color = style.extraStyle.color,
                    fontSize = 12.sp,
                    textAlign = style.extraStyle.align,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** 스타일 선택 다이얼로그 */
@Composable
fun StyleSelectionDialog(
    selectedStyleId: Int,
    onStyleSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("스타일 선택") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                StyleManager.getAllStyleIds().forEach { id ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onStyleSelected(id)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStyleId == id,
                            onClick = {
                                onStyleSelected(id)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StylePreview(styleId = id)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

/** 배경 선택 다이얼로그 */
@Composable
fun BackgroundSelectionDialog(
    currentBgConfig: BackgroundConfig,
    currentAlpha: Float,
    onBackgroundSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: 컬러, 1: 그라디언트, 2: 이미지

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("배경 선택") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 500.dp)
            ) {
                // 메인 탭
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("컬러") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "그라데이션",
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("이미지") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 탭 내용
                when (selectedTab) {
                    0 -> ColorTabContent(
                        currentBgConfig = currentBgConfig,
                        currentAlpha = currentAlpha,
                        onColorSelected = onBackgroundSelected
                    )
                    1 -> GradientTabContent(
                        currentBgConfig = currentBgConfig,
                        currentAlpha = currentAlpha,
                        onGradientSelected = onBackgroundSelected
                    )
                    2 -> ImageTabContent(
                        currentBgConfig = currentBgConfig,
                        currentAlpha = currentAlpha,
                        onImageSelected = onBackgroundSelected
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

/** 컬러 탭 */
@Composable
fun ColorTabContent(
    currentBgConfig: BackgroundConfig,
    currentAlpha: Float,
    onColorSelected: (String) -> Unit
) {
    var colorSubTab by remember { mutableStateOf(0) } // 0: 팔레트, 1: 커스텀

    Column(modifier = Modifier.fillMaxWidth()) {
        // 서브 탭
        TabRow(selectedTabIndex = colorSubTab) {
            Tab(
                selected = colorSubTab == 0,
                onClick = { colorSubTab = 0 },
                text = { Text("팔레트") }
            )
            Tab(
                selected = colorSubTab == 1,
                onClick = { colorSubTab = 1 },
                text = { Text("커스텀") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (colorSubTab) {
            0 -> PaletteColorPicker(
                currentBgConfig = currentBgConfig,
                currentAlpha = currentAlpha,
                onColorSelected = onColorSelected
            )
            1 -> CustomColorPicker(
                currentBgConfig = currentBgConfig,
                currentAlpha = currentAlpha,
                onColorSelected = onColorSelected
            )
        }
    }
}

/** 팔레트 색상 피커 (세로 쉐이드) */
@Composable
fun PaletteColorPicker(
    currentBgConfig: BackgroundConfig,
    currentAlpha: Float,
    onColorSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorPaletteData.specialColors.forEach { colorInfo ->
                ColorCircleButton(
                    hex = colorInfo.hex,
                    isSelected = currentBgConfig.isSolid && currentBgConfig.hexColor == colorInfo.hex,
                    onClick = {
                        onColorSelected("solid:${colorInfo.hex},alpha:${"%.2f".format(currentAlpha)}")
                    }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 팔레트 그리드 (세로 쉐이드)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (shadeIndex in 0 until 8) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ColorPaletteData.palette.forEach { colorColumn ->
                        val colorInfo = colorColumn[shadeIndex]
                        ColorSquareButton(
                            hex = colorInfo.hex,
                            isSelected = currentBgConfig.isSolid && currentBgConfig.hexColor == colorInfo.hex,
                            onClick = {
                                onColorSelected("solid:${colorInfo.hex},alpha:${"%.2f".format(currentAlpha)}")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** 커스텀 색상 피커 (HSV) */
@Composable
fun CustomColorPicker(
    currentBgConfig: BackgroundConfig,
    currentAlpha: Float,
    onColorSelected: (String) -> Unit
) {
    var selectedColor by remember {
        mutableStateOf(
            try {
                HsvColor.from(Color(android.graphics.Color.parseColor(currentBgConfig.hexColor)))
            } catch (e: Exception) {
                HsvColor.from(Color.White)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 컬러 피커
        ClassicColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            color = selectedColor,
            onColorChanged = { hsvColor ->
                selectedColor = hsvColor
                val hex = String.format("#%06X", 0xFFFFFF and hsvColor.toColor().toArgb())
                onColorSelected("solid:$hex,alpha:${"%.2f".format(currentAlpha)}")
            }
        )

        // 선택된 색상 미리보기
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("선택된 색상", style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hex = String.format("#%06X", 0xFFFFFF and selectedColor.toColor().toArgb())
                Text(
                    hex,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            color = selectedColor.toColor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = Color.Gray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }
    }
}

/** 그라디언트 탭 (HSV) */
@Composable
fun GradientTabContent(
    currentBgConfig: BackgroundConfig,
    currentAlpha: Float,
    onGradientSelected: (String) -> Unit
) {
    var startColor by remember {
        mutableStateOf(currentBgConfig.gradientStartColor ?: "#FF6B9D")
    }
    var endColor by remember {
        mutableStateOf(currentBgConfig.gradientEndColor ?: "#FFC371")
    }
    var direction by remember {
        mutableStateOf(currentBgConfig.gradientDirection ?: "horizontal")
    }
    var showStartColorPicker by remember { mutableStateOf(false) }
    var showEndColorPicker by remember { mutableStateOf(false) }

    val updateGradient: () -> Unit = {
        onGradientSelected("gradient:$startColor,$endColor,$direction,alpha:${"%.2f".format(currentAlpha)}")
    }

    LaunchedEffect(Unit) {
        updateGradient()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("미리보기", style = MaterialTheme.typography.titleSmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    brush = when (direction) {
                        "horizontal" -> androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(startColor)),
                                Color(android.graphics.Color.parseColor(endColor))
                            )
                        )
                        "vertical" -> androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(startColor)),
                                Color(android.graphics.Color.parseColor(endColor))
                            )
                        )
                        "diagonal_down" -> androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(startColor)),
                                Color(android.graphics.Color.parseColor(endColor))
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset.Infinite
                        )
                        "diagonal_up" -> androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(startColor)),
                                Color(android.graphics.Color.parseColor(endColor))
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                        else -> androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(startColor)),
                                Color(android.graphics.Color.parseColor(endColor))
                            )
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    alpha = currentAlpha
                )
        )

        HorizontalDivider()

        // 시작 색상
        Text("시작 색상", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(startColor, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(android.graphics.Color.parseColor(startColor)),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                Button(onClick = { showStartColorPicker = true }) {
                    Text("변경")
                }
            }
        }

        // 끝 색상
        Text("끝 색상", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(endColor, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(android.graphics.Color.parseColor(endColor)),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                Button(onClick = { showEndColorPicker = true }) {
                    Text("변경")
                }
            }
        }

        HorizontalDivider()

        // 방향 선택
        Text("그라디언트 방향", style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GradientDirectionOption(
                label = "가로 (→)",
                direction = "horizontal",
                selected = direction == "horizontal",
                onSelect = { direction = it; updateGradient() }
            )
            GradientDirectionOption(
                label = "세로 (↓)",
                direction = "vertical",
                selected = direction == "vertical",
                onSelect = { direction = it; updateGradient() }
            )
            GradientDirectionOption(
                label = "대각선 (↘)",
                direction = "diagonal_down",
                selected = direction == "diagonal_down",
                onSelect = { direction = it; updateGradient() }
            )
            GradientDirectionOption(
                label = "대각선 (↙)",
                direction = "diagonal_up",
                selected = direction == "diagonal_up",
                onSelect = { direction = it; updateGradient() }
            )
        }
    }

    // 시작 색상 피커 다이얼로그
    if (showStartColorPicker) {
        GradientColorPickerDialog(
            title = "시작 색상 선택",
            currentColor = startColor,
            onColorSelected = {
                startColor = it
                updateGradient()
                showStartColorPicker = false
            },
            onDismiss = { showStartColorPicker = false }
        )
    }

    // 끝 색상 피커 다이얼로그
    if (showEndColorPicker) {
        GradientColorPickerDialog(
            title = "끝 색상 선택",
            currentColor = endColor,
            onColorSelected = {
                endColor = it
                updateGradient()
                showEndColorPicker = false
            },
            onDismiss = { showEndColorPicker = false }
        )
    }
}

/** 그라디언트 방향 옵션 */
@Composable
fun GradientDirectionOption(
    label: String,
    direction: String,
    selected: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(direction) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = { onSelect(direction) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 그라디언트 색상 피커 다이얼로그 */
@Composable
fun GradientColorPickerDialog(
    title: String,
    currentColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var colorSubTab by remember { mutableStateOf(0) }
    var tempSelectedColor by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = colorSubTab) {
                    Tab(
                        selected = colorSubTab == 0,
                        onClick = { colorSubTab = 0 },
                        text = { Text("팔레트") }
                    )
                    Tab(
                        selected = colorSubTab == 1,
                        onClick = { colorSubTab = 1 },
                        text = { Text("커스텀") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (colorSubTab) {
                    0 -> GradientPalettePicker(
                        selectedColor = tempSelectedColor,
                        onColorSelected = { color ->
                            tempSelectedColor = color
                            onColorSelected(color)
                            onDismiss()
                        }
                    )
                    1 -> GradientCustomPicker(
                        selectedColor = tempSelectedColor,
                        onColorSelected = { color ->
                            tempSelectedColor = color
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(tempSelectedColor)
                    onDismiss()
                }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/** 그라디언트 팔레트 피커 */
@Composable
fun GradientPalettePicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 특별 색상
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorPaletteData.specialColors.forEach { colorInfo ->
                ColorCircleButton(
                    hex = colorInfo.hex,
                    isSelected = selectedColor == colorInfo.hex,
                    onClick = { onColorSelected(colorInfo.hex) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 팔레트 그리드
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (shadeIndex in 0 until 8) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ColorPaletteData.palette.forEach { colorColumn ->
                        val colorInfo = colorColumn[shadeIndex]
                        ColorSquareButton(
                            hex = colorInfo.hex,
                            isSelected = selectedColor == colorInfo.hex,
                            onClick = { onColorSelected(colorInfo.hex) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** 그라디언트 커스텀 피커 */
@Composable
fun GradientCustomPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    var currentColor by remember {
        mutableStateOf(
            try {
                HsvColor.from(Color(android.graphics.Color.parseColor(selectedColor)))
            } catch (e: Exception) {
                HsvColor.from(Color.White)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 컬러 피커
        ClassicColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            color = currentColor,
            onColorChanged = { hsvColor ->
                currentColor = hsvColor
                val hex = String.format("#%06X", 0xFFFFFF and hsvColor.toColor().toArgb())
                onColorSelected(hex)
            }
        )

        // 선택된 색상 미리보기
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("선택된 색상", style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hex = String.format("#%06X", 0xFFFFFF and currentColor.toColor().toArgb())
                Text(
                    hex,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            color = currentColor.toColor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = Color.Gray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }
    }
}

/** 이미지 탭 (테마별 + 내 이미지) */
@Composable
fun ImageTabContent(
    currentBgConfig: BackgroundConfig,
    currentAlpha: Float,
    onImageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userImages by remember { mutableStateOf(ImageManager.getUserImages(context)) }

    // 탭: 0=테마별, 1=내 이미지
    var selectedTab by remember { mutableStateOf(0) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val fileName = ImageManager.saveImageFromUri(context, it)
                if (fileName != null) {
                    userImages = ImageManager.getUserImages(context)
                    onImageSelected("image:file://$fileName,alpha:${"%.2f".format(currentAlpha)}")
                    android.widget.Toast.makeText(context, "이미지가 추가되었습니다", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "이미지 추가에 실패했습니다", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("테마별") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("내 이미지") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> ThemeImagesContent(
                currentBgConfig = currentBgConfig,
                currentAlpha = currentAlpha,
                onImageSelected = onImageSelected
            )
            1 -> UserImagesContent(
                currentBgConfig = currentBgConfig,
                currentAlpha = currentAlpha,
                userImages = userImages,
                onImageSelected = onImageSelected,
                onImageAdded = { galleryLauncher.launch("image/*") },
                onImageDeleted = { fileName ->
                    scope.launch {
                        val deleted = ImageManager.deleteImage(context, fileName)
                        if (deleted) {
                            userImages = ImageManager.getUserImages(context)
                            if (currentBgConfig.isImage && !currentBgConfig.isThemeImage &&
                                currentBgConfig.imageName == "file://$fileName") {
                                onImageSelected("solid:#FFFFFF,alpha:1.0")
                            }
                        }
                    }
                }
            )
        }
    }
}

/** 테마별 이미지 컨텐츠 (가로 스크롤) */
@Composable
fun ThemeImagesContent(
    currentBgConfig: BackgroundConfig,
    currentAlpha: Float,
    onImageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val themes = remember { com.example.dailywidget.util.ThemeManager.getAllThemes() }

    var selectedTheme by remember { mutableStateOf<com.example.dailywidget.util.ThemeManager.Theme?>(null) }
    var themeImages by remember { mutableStateOf<List<com.example.dailywidget.util.ThemeManager.ThemeImage>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        if (selectedTheme == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("테마를 선택하세요", style = MaterialTheme.typography.titleSmall)

                themes.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { theme ->
                            ThemeCard(
                                theme = theme,
                                onClick = {
                                    selectedTheme = theme
                                    themeImages = com.example.dailywidget.util.ThemeManager.getThemeImages(context, theme.id)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedTheme!!.displayName} (${themeImages.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextButton(onClick = { selectedTheme = null }) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("테마 목록")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (themeImages.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "이 테마에는 아직 이미지가 없습니다",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // 2열 그리드 (세로 스크롤)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        themeImages.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { themeImage ->
                                    val imagePath = com.example.dailywidget.util.ThemeManager.buildThemeImagePath(
                                        themeImage.themeId,
                                        themeImage.fileName
                                    )
                                    val isSelected = currentBgConfig.isThemeImage &&
                                            currentBgConfig.imageName == imagePath

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable {
                                                onImageSelected("image:$imagePath,alpha:${"%.2f".format(currentAlpha)}")
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (isSelected) {
                                            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            BorderStroke(1.dp, Color.Gray)
                                        }
                                    ) {
                                        AsyncImage(
                                            model = "file:///android_asset/${themeImage.path}",
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // 홀수 개일 때 빈 공간 채우기
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 테마 카드 */
@Composable
fun ThemeCard(
    theme: com.example.dailywidget.util.ThemeManager.Theme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = theme.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 내 이미지 컨텐츠 */
@Composable
fun UserImagesContent(
    currentBgConfig: BackgroundConfig,
    currentAlpha: Float,
    userImages: List<String>,
    onImageSelected: (String) -> Unit,
    onImageAdded: () -> Unit,
    onImageDeleted: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("내 이미지", style = MaterialTheme.typography.titleSmall)
            Text(
                "${userImages.size}/10 (${String.format("%.1f", ImageManager.getTotalImageSizeMB(context))}MB/50MB)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (userImages.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "아직 추가한 이미지가 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            userImages.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { fileName ->
                        val file = ImageManager.getImageFile(context, fileName)
                        val isSelected = currentBgConfig.isImage &&
                                !currentBgConfig.isThemeImage &&
                                currentBgConfig.imageName == "file://$fileName"

                        Box(modifier = Modifier.size(80.dp)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        onImageSelected("image:file://$fileName,alpha:${"%.2f".format(currentAlpha)}")
                                    },
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) {
                                    BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    BorderStroke(1.dp, Color.Gray)
                                }
                            ) {
                                if (file != null) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            var showDeleteDialog by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { showDeleteDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "삭제",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("이미지 삭제") },
                                    text = { Text("이 이미지를 삭제하시겠습니까?") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                onImageDeleted(fileName)
                                                showDeleteDialog = false
                                            }
                                        ) {
                                            Text("삭제", color = MaterialTheme.colorScheme.error)
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
                    }
                }
            }
        }

        val canAdd = ImageManager.canAddImage(context)
        val buttonText = when {
            userImages.size >= 10 -> "최대 10개 도달"
            ImageManager.getTotalImageSizeMB(context) >= 50 -> "최대 용량 도달 (50MB)"
            else -> "사진 추가"
        }

        OutlinedButton(
            onClick = {
                if (canAdd) {
                    onImageAdded()
                } else {
                    val message = if (userImages.size >= 10) {
                        "최대 10개까지만 추가할 수 있습니다"
                    } else {
                        "최대 용량(50MB)을 초과했습니다"
                    }
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canAdd
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText)
        }
    }
}

/** 원형 색상 버튼 */
@Composable
fun ColorCircleButton(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(android.graphics.Color.parseColor(hex)),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(44.dp),
            border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
        ) {}
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                null,
                tint = if (hex == "#FFFFFF") Color.Black else Color.White
            )
        }
    }
}

/** 사각형 색상 버튼 */
@Composable
fun ColorSquareButton(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(android.graphics.Color.parseColor(hex)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxSize(),
            border = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else null
        ) {}
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 탭 액션 선택 다이얼로그 */
@Composable
private fun TapActionSelectionDialog(
    selectedAction: DataStoreManager.WidgetTapAction,
    onActionSelected: (DataStoreManager.WidgetTapAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("위젯 클릭 동작") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "위젯을 탭했을 때 실행할 동작을 선택하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DataStoreManager.WidgetTapAction.entries.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onActionSelected(action)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAction == action,
                            onClick = {
                                onActionSelected(action)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = action.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}