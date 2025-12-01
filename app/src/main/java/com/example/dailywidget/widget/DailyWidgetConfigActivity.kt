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
import com.github.skydoves.colorpicker.compose.*
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import java.io.File

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
                dataStoreManager.saveWidgetConfig(
                    appWidgetId,
                    DataStoreManager.WidgetConfig(
                        styleId = styleId,
                        backgroundId = backgroundId
                    )
                )

                val appWidgetManager = AppWidgetManager.getInstance(this@DailyWidgetConfigActivity)
                val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                val genre = when {
                    info?.provider?.className?.endsWith("NovelWidgetProvider") == true -> "novel"
                    info?.provider?.className?.endsWith("FantasyWidgetProvider") == true -> "fantasy"
                    info?.provider?.className?.endsWith("EssayWidgetProvider") == true -> "essay"
                    else -> "novel"
                }

                DailyWidgetProvider.updateWidgets(
                    this@DailyWidgetConfigActivity,
                    appWidgetManager,
                    intArrayOf(appWidgetId),
                    genre
                )

                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, resultValue)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    onSave: (styleId: Int, backgroundId: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
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

    LaunchedEffect(initialConfig.value) {
        selectedStyleId = initialConfig.value.styleId
        selectedBackgroundId = initialConfig.value.backgroundId
    }

    val currentBgConfig = parseBackgroundId(selectedBackgroundId)
    val currentAlpha by remember(selectedBackgroundId) { mutableStateOf(currentBgConfig.alpha) }
    val currentHex by remember(selectedBackgroundId) { mutableStateOf(currentBgConfig.hexColor) }

    val updateSolidBackgroundId: (hex: String, alpha: Float) -> Unit = { hex, alpha ->
        selectedBackgroundId = "solid:$hex,alpha:${"%.2f".format(alpha)}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("위젯 설정") },
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
                            tint = MaterialTheme.colorScheme.primary  // ⭐ 추가: 색상
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
                            .fillMaxWidth()  // ⭐ 추가
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
                            modifier = Modifier.weight(1f)  // ⭐ 추가: 남은 공간 모두 차지
                        ) {
                            Text(desc, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "탭하여 변경",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))  // ⭐ 추가: 간격
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "배경 아이콘",
                            tint = MaterialTheme.colorScheme.primary  // ⭐ 추가: 색상
                        )
                    }
                }

                // 3. 투명도 (단색 또는 그라디언트일 때만)
                if (currentBgConfig.isSolid || currentBgConfig.isGradient) {
                    Column {
                        Text(
                            "불투명도 (${"%.0f".format(currentAlpha * 100)}%)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Slider(
                            value = currentAlpha,
                            onValueChange = {
                                if (currentBgConfig.isSolid) {
                                    updateSolidBackgroundId(currentHex, it)
                                } else if (currentBgConfig.isGradient &&
                                    currentBgConfig.gradientStartColor != null &&
                                    currentBgConfig.gradientEndColor != null &&
                                    currentBgConfig.gradientDirection != null) {
                                    selectedBackgroundId = "gradient:${currentBgConfig.gradientStartColor},${currentBgConfig.gradientEndColor},${currentBgConfig.gradientDirection},alpha:${"%.2f".format(it)}"
                                }
                            },
                            valueRange = 0.1f..1f
                        )
                    }
                }
            }

            // 위젯 미리보기
            HorizontalDivider()
            WidgetPreview(
                selectedStyleId = selectedStyleId,
                currentBgConfig = currentBgConfig
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
            onBackgroundSelected = { selectedBackgroundId = it },
            onDismiss = { showBackgroundDialog = false }
        )
    }
}

// ==================== 위젯 미리보기 ====================

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
            // 이미지 배경
            if (currentBgConfig.isImage && currentBgConfig.imageName != null) {
                if (currentBgConfig.imageName.startsWith("file://")) {
                    val fileName = currentBgConfig.imageName.substringAfter("file://")
                    val file = ImageManager.getImageFile(context, fileName)
                    if (file != null) {
                        AsyncImage(
                            model = file,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
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
                            modifier = Modifier.fillMaxSize()
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

// ==================== 스타일 선택 다이얼로그 ====================

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

// ==================== 배경 선택 다이얼로그 ====================

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
                    .widthIn(min = 500.dp)  // ⭐ 최소 가로 크기 설정
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
                                fontSize = 12.sp  // ⭐ 작은 폰트
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
                        onGradientSelected = onBackgroundSelected
                    )
                    2 -> ImageTabContent(
                        currentBgConfig = currentBgConfig,
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

// ==================== 컬러 탭 ====================

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

// ==================== 팔레트 색상 피커 (세로 쉐이드) ====================

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
        // 특별 색상 (흰색, 검정)
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

// ==================== 커스텀 색상 피커 (HSV) ====================

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
        // ⭐ 컬러 피커
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

        // ⭐ 선택된 색상 미리보기
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

// ==================== 그라디언트 탭 ====================

@Composable
fun GradientTabContent(
    currentBgConfig: BackgroundConfig,
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
    val alpha = 1.0f
    var showStartColorPicker by remember { mutableStateOf(false) }
    var showEndColorPicker by remember { mutableStateOf(false) }

    val updateGradient: () -> Unit = {
        onGradientSelected("gradient:$startColor,$endColor,$direction,alpha:${"%.2f".format(alpha)}")
    }

    // ⭐ 처음 렌더링될 때 자동으로 현재 그라디언트 적용
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
        // 미리보기
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
                    shape = RoundedCornerShape(8.dp)
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

// ==================== 그라디언트 방향 옵션 ====================

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

// ==================== 그라디언트 색상 피커 다이얼로그 ====================

@Composable
fun GradientColorPickerDialog(
    title: String,
    currentColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var colorSubTab by remember { mutableStateOf(0) }
    // ⭐ 내부에서 임시로 색상을 관리
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
                            // ⭐ 팔레트는 즉시 적용하고 닫기
                            tempSelectedColor = color
                            onColorSelected(color)
                            onDismiss()
                        }
                    )
                    1 -> GradientCustomPicker(
                        selectedColor = tempSelectedColor,
                        onColorSelected = { color ->
                            // ⭐ 커스텀은 임시 저장만 (닫지 않음)
                            tempSelectedColor = color
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // ⭐ 확인 버튼 누를 때만 실제 적용
                    onColorSelected(tempSelectedColor)
                    onDismiss()
                }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            // ⭐ 취소 버튼 추가
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// ==================== 그라디언트 팔레트 피커 ====================

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

// ==================== 그라디언트 커스텀 피커 ====================

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
        // ⭐ 컬러 피커
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

        // ⭐ 선택된 색상 미리보기
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

// ==================== 이미지 탭 ====================

@Composable
fun ImageTabContent(
    currentBgConfig: BackgroundConfig,
    onImageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userImages by remember { mutableStateOf(ImageManager.getUserImages(context)) }

    // 갤러리에서 바로 선택 (크롭 없음)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val fileName = ImageManager.saveImageFromUri(context, it)
                if (fileName != null) {
                    userImages = ImageManager.getUserImages(context)
                    onImageSelected("image:file://$fileName")

                    android.widget.Toast.makeText(
                        context,
                        "이미지가 추가되었습니다",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "이미지 추가에 실패했습니다",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 기본 이미지
        Text("기본 이미지", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("image", "ic_launcher_background").forEach { name ->
                val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
                val isSelected = currentBgConfig.isImage && currentBgConfig.imageName == name
                Card(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onImageSelected("image:$name") },
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) {
                        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, Color.Gray)
                    }
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        HorizontalDivider()

        // 사용자 이미지
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

        // 빈 상태 메시지
        if (userImages.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "아래 버튼을 눌러 이미지를 추가해보세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 이미지 그리드
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            userImages.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { fileName ->
                        val file = ImageManager.getImageFile(context, fileName)
                        val isSelected = currentBgConfig.isImage && currentBgConfig.imageName == "file://$fileName"

                        Box(modifier = Modifier.size(80.dp)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onImageSelected("image:file://$fileName") },
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

                            // 삭제 버튼
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

                            // 삭제 확인 다이얼로그
                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("이미지 삭제") },
                                    text = {
                                        Text("이 이미지를 삭제하시겠습니까?\n위젯에서 사용 중이라면 기본 배경으로 변경됩니다.")
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    val deleted = ImageManager.deleteImage(context, fileName)
                                                    if (deleted) {
                                                        userImages = ImageManager.getUserImages(context)

                                                        if (currentBgConfig.isImage &&
                                                            currentBgConfig.imageName == "file://$fileName") {
                                                            onImageSelected("solid:#FFFFFF,alpha:1.0")
                                                        }
                                                    }
                                                    showDeleteDialog = false
                                                }
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

        // 이미지 추가 버튼
        val canAdd = ImageManager.canAddImage(context)
        val buttonText = when {
            userImages.size >= 10 -> "최대 10개 도달"
            ImageManager.getTotalImageSizeMB(context) >= 50 -> "최대 용량 도달 (50MB)"
            else -> "사진 추가"
        }

        OutlinedButton(
            onClick = {
                if (canAdd) {
                    galleryLauncher.launch("image/*")
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

// ==================== 색상 버튼 컴포넌트 ====================

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