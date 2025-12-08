package com.example.dailywidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import com.example.dailywidget.util.ThemeManager
import android.widget.RemoteViews
import com.example.dailywidget.R
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import com.example.dailywidget.data.repository.DataStoreManager
import com.example.dailywidget.ui.activity.MainActivity
import com.example.dailywidget.util.StyleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import com.example.dailywidget.util.ImageManager
import java.text.SimpleDateFormat
import com.example.dailywidget.widget.BackgroundConfig
import com.example.dailywidget.widget.parseBackgroundId
import java.util.*
import kotlin.math.roundToInt


// ==================== DailyWidgetProvider 클래스 ====================

abstract class DailyWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_REFRESH = "com.example.dailywidget.ACTION_REFRESH"
        private const val EXTRA_GENRE = "extra_genre"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // ⭐ UnifiedWidgetProvider만 업데이트
            val unifiedIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, UnifiedWidgetProvider::class.java)
            )

            val provider = UnifiedWidgetProvider()
            unifiedIds.forEach { appWidgetId ->
                // ⭐ 각 위젯의 장르를 DataStore에서 조회하여 업데이트
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val dataStoreManager = com.example.dailywidget.data.repository.DataStoreManager(context)
                        val genreId = dataStoreManager.getWidgetGenre(appWidgetId)

                        provider.updateAppWidget(
                            context = context,
                            appWidgetManager = appWidgetManager,
                            appWidgetId = appWidgetId,
                            genre = genreId,
                            forceRefresh = false
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, genre: String) {
            // ⭐ UnifiedWidgetProvider 사용
            val provider = UnifiedWidgetProvider()
            appWidgetIds.forEach { appWidgetId ->
                provider.updateAppWidget(context, appWidgetManager, appWidgetId, genre, forceRefresh = false)
            }
        }
    }

    open fun getGenre(): String {
        // 기본값 반환 (하위 호환성)
        // UnifiedWidgetProvider는 이 함수를 사용하지 않음
        return "novel"
    }
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // ⭐ 이 부모 클래스의 onUpdate가 호출되면 안됨!
        android.util.Log.d("DailyWidgetProvider", "⚠️ BASE onUpdate called! This should not happen!")
        DailyWidgetReceiver.scheduleMidnightUpdate(context)

        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId, getGenre(), forceRefresh = false)
        }
    }

    // ⭐ 추가: 첫 위젯 추가 시 호출
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 자정 알람 설정
        DailyWidgetReceiver.scheduleMidnightUpdate(context)
    }

    // ⭐ 추가: 마지막 위젯 삭제 시 호출
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 자정 알람 취소
        DailyWidgetReceiver.cancelMidnightUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val genre = intent.getStringExtra(EXTRA_GENRE) ?: return
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId, genre, forceRefresh = true)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val dataStoreManager = DataStoreManager(context)
            appWidgetIds.forEach { dataStoreManager.deleteWidgetConfig(it) }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId, getGenre(), forceRefresh = false)
    }

    data class WidgetSize(val widthDp: Int, val heightDp: Int, val widthRows: Int, val heightRows: Int, val hasOptions: Boolean)

    private fun getWidgetSize(appWidgetManager: AppWidgetManager, appWidgetId: Int): WidgetSize {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)

        val actualWidth = if (minWidth > 0) minWidth else 146
        val actualHeight = if (minHeight > 0) minHeight else 146

        val widthRows = ((actualWidth + 30) / 70.0).roundToInt().coerceAtLeast(1)
        val heightRows = ((actualHeight + 30) / 70.0).roundToInt().coerceAtLeast(1)

        return WidgetSize(actualWidth, actualHeight, widthRows, heightRows, actualWidth > 0)
    }

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, genre: String, forceRefresh: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.dailySentenceDao()
                val dataStoreManager = DataStoreManager(context)

                val today = SimpleDateFormat("MMdd", Locale.getDefault()).format(Date())
                val allSentences = dao.getSentencesByDate(today)
                val filteredSentences = allSentences.filter { it.genre.equals(genre, ignoreCase = true) }

                val defaultConfig = dataStoreManager.getDefaultConfig()
                val displayConfig = dataStoreManager.getDisplayConfig()
                val fontSizeConfig = dataStoreManager.getFontSizeConfig()

                val widgetSize = getWidgetSize(appWidgetManager, appWidgetId)
                val views = RemoteViews(context.packageName, R.layout.widget_daily)

                // ==================== 문장이 없는 경우 ====================
                if (filteredSentences.isEmpty()) {
                    views.setViewVisibility(R.id.widget_background_solid, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_background_image, View.GONE)
                    views.setInt(R.id.widget_background_solid, "setColorFilter", Color.WHITE)
                    views.setInt(R.id.widget_background_solid, "setImageAlpha", 255)

                    views.setTextViewText(R.id.widget_text, "오늘의 문장이 없습니다")
                    views.setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, 16f)
                    views.setTextColor(R.id.widget_text, Color.BLACK)
                    views.setViewVisibility(R.id.widget_text, View.VISIBLE)

                    views.setViewVisibility(R.id.widget_source, View.GONE)
                    views.setViewVisibility(R.id.widget_extra, View.GONE)
                    views.setViewVisibility(R.id.widget_refresh_button, View.GONE)

                    // ⭐ 문장 없을 때는 기본 액션만 (앱 열기)
                    val intent = Intent(context, com.example.dailywidget.ui.activity.MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return@launch
                }

                // ==================== 문장이 있는 경우 ====================

                val lastSentenceId = if (forceRefresh) dataStoreManager.getLastSentenceId(appWidgetId) else null
                val sentence = selectRandomSentence(filteredSentences, lastSentenceId) ?: return@launch
                dataStoreManager.saveLastSentenceId(appWidgetId, sentence.id)

                val widgetConfig = dataStoreManager.getWidgetConfig(appWidgetId)
                val finalStyleId = widgetConfig.styleId.takeIf { it != 0 } ?: defaultConfig.styleId
                val finalBackgroundId = widgetConfig.backgroundId.takeIf {
                    !it.isNullOrEmpty() && it != "default"
                } ?: defaultConfig.backgroundId

                val style = StyleManager.getWidgetStyle(finalStyleId)
                val bgConfig = parseBackgroundId(finalBackgroundId)

                // ⭐ 1행(높이) 체크: heightRows == 1이면 source/extra 숨김
                val isOnlyOneRow = widgetSize.heightRows == 1

                applyBackground(views, bgConfig, context)
                applyTextContent(
                    views = views,
                    sentence = sentence,
                    style = style,
                    fontSizeConfig = fontSizeConfig,
                    displayConfig = displayConfig,
                    widgetSize = widgetSize,
                    isOnlyOneRow = isOnlyOneRow
                )

                views.setViewVisibility(
                    R.id.widget_refresh_button,
                    if (!isOnlyOneRow && filteredSentences.size > 1) View.VISIBLE else View.GONE
                )

                setupPendingIntents(
                    context = context,
                    views = views,
                    appWidgetId = appWidgetId,
                    genre = genre,
                    sentenceCount = filteredSentences.size,
                    sentence = sentence,  // ⭐ 추가
                    displayConfig = displayConfig  // ⭐ 추가
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

// ==================== Helper 함수들 ====================

    private fun applyBackground(views: RemoteViews, bgConfig: BackgroundConfig, context: Context) {
        when {
            bgConfig.isSolid -> {
                // 단색 배경
                try {
                    val colorInt = Color.parseColor(bgConfig.hexColor)
                    val alphaInt = (bgConfig.alpha * 255).toInt().coerceIn(0, 255)
                    val opaqueColor = Color.rgb(Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))

                    views.setViewVisibility(R.id.widget_background_solid, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_background_image, View.GONE)
                    views.setInt(R.id.widget_background_solid, "setColorFilter", opaqueColor)
                    views.setInt(R.id.widget_background_solid, "setImageAlpha", alphaInt)
                } catch (e: IllegalArgumentException) {
                    android.util.Log.e("WidgetBg", "Invalid color: ${bgConfig.hexColor}", e)
                    setDefaultBackground(views)
                }
            }
            bgConfig.isImage && bgConfig.imageName != null -> {
                // ⭐ 이미지 배경
                views.setViewVisibility(R.id.widget_background_solid, View.GONE)
                views.setViewVisibility(R.id.widget_background_image, View.VISIBLE)

                try {
                    if (bgConfig.isThemeImage) {
                        // ⭐ 테마 이미지
                        android.util.Log.d("WidgetBg", "Theme image: ${bgConfig.imageName}")

                        val assetPath = ThemeManager.getAssetPath(bgConfig.imageName)
                        android.util.Log.d("WidgetBg", "Asset path: $assetPath")

                        if (assetPath != null) {
                            try {
                                val inputStream = context.assets.open(assetPath)

                                // ⭐ 1. 원본 비트맵 디코드
                                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                                inputStream.close()

                                if (originalBitmap != null) {
                                    // ⭐ 2. 위젯 크기에 맞게 리사이징 (최대 800px)
                                    val maxSize = 800
                                    val ratio = minOf(
                                        maxSize.toFloat() / originalBitmap.width,
                                        maxSize.toFloat() / originalBitmap.height
                                    )

                                    val newWidth = (originalBitmap.width * ratio).toInt()
                                    val newHeight = (originalBitmap.height * ratio).toInt()

                                    val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(
                                        originalBitmap,
                                        newWidth,
                                        newHeight,
                                        true
                                    )

                                    // ⭐ 3. 원본 비트맵 해제
                                    if (resizedBitmap != originalBitmap) {
                                        originalBitmap.recycle()
                                    }

                                    android.util.Log.d("WidgetBg", "Image resized: ${originalBitmap.width}x${originalBitmap.height} -> ${newWidth}x${newHeight}")

                                    // ⭐ 4. 위젯에 적용
                                    val alphaInt = (bgConfig.alpha * 255).toInt().coerceIn(0, 255)
                                    views.setImageViewBitmap(R.id.widget_background_image, resizedBitmap)
                                    views.setInt(R.id.widget_background_image, "setImageAlpha", alphaInt)

                                    android.util.Log.d("WidgetBg", "Theme image applied successfully")
                                } else {
                                    android.util.Log.e("WidgetBg", "Bitmap decode failed")
                                    setDefaultBackground(views)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("WidgetBg", "Error: ${e.message}", e)
                                setDefaultBackground(views)
                            }
                        } else {
                            android.util.Log.e("WidgetBg", "Asset path is null")
                            setDefaultBackground(views)
                        }
                    } else if (bgConfig.imageName.startsWith("file://")) {
                        // 사용자 이미지
                        android.util.Log.d("WidgetBg", "User image: ${bgConfig.imageName}")
                        val fileName = bgConfig.imageName.substringAfter("file://")
                        val imageFile = ImageManager.getImageFile(context, fileName)

                        if (imageFile != null && imageFile.exists()) {
                            try {
                                // ⭐ 1. 원본 디코드
                                val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

                                if (originalBitmap != null) {
                                    // ⭐ 2. 리사이징
                                    val maxSize = 800
                                    val ratio = minOf(
                                        maxSize.toFloat() / originalBitmap.width,
                                        maxSize.toFloat() / originalBitmap.height
                                    )

                                    val newWidth = (originalBitmap.width * ratio).toInt()
                                    val newHeight = (originalBitmap.height * ratio).toInt()

                                    val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(
                                        originalBitmap,
                                        newWidth,
                                        newHeight,
                                        true
                                    )

                                    if (resizedBitmap != originalBitmap) {
                                        originalBitmap.recycle()
                                    }

                                    android.util.Log.d("WidgetBg", "User image resized: ${newWidth}x${newHeight}")

                                    // ⭐ 3. 적용
                                    val alphaInt = (bgConfig.alpha * 255).toInt().coerceIn(0, 255)
                                    views.setImageViewBitmap(R.id.widget_background_image, resizedBitmap)
                                    views.setInt(R.id.widget_background_image, "setImageAlpha", alphaInt)

                                    android.util.Log.d("WidgetBg", "User image applied successfully")
                                } else {
                                    android.util.Log.e("WidgetBg", "User bitmap decode failed")
                                    setDefaultBackground(views)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("WidgetBg", "Error: ${e.message}", e)
                                setDefaultBackground(views)
                            }
                        } else {
                            android.util.Log.e("WidgetBg", "User image file not found: $fileName")
                            setDefaultBackground(views)
                        }
                    } else {
                        // Drawable 이미지
                        android.util.Log.d("WidgetBg", "Drawable image: ${bgConfig.imageName}")
                        val resId = context.resources.getIdentifier(
                            bgConfig.imageName,
                            "drawable",
                            context.packageName
                        )
                        if (resId != 0) {
                            try {
                                // ⭐ 리사이징
                                val originalBitmap = BitmapFactory.decodeResource(context.resources, resId)

                                if (originalBitmap != null) {
                                    val maxSize = 800
                                    val ratio = minOf(
                                        maxSize.toFloat() / originalBitmap.width,
                                        maxSize.toFloat() / originalBitmap.height
                                    )

                                    val newWidth = (originalBitmap.width * ratio).toInt()
                                    val newHeight = (originalBitmap.height * ratio).toInt()

                                    val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(
                                        originalBitmap,
                                        newWidth,
                                        newHeight,
                                        true
                                    )

                                    if (resizedBitmap != originalBitmap) {
                                        originalBitmap.recycle()
                                    }

                                    val alphaInt = (bgConfig.alpha * 255).toInt().coerceIn(0, 255)
                                    views.setImageViewBitmap(R.id.widget_background_image, resizedBitmap)
                                    views.setInt(R.id.widget_background_image, "setImageAlpha", alphaInt)

                                    android.util.Log.d("WidgetBg", "Drawable applied successfully")
                                } else {
                                    setDefaultBackground(views)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("WidgetBg", "Error: ${e.message}", e)
                                setDefaultBackground(views)
                            }
                        } else {
                            android.util.Log.e("WidgetBg", "Drawable resource not found: ${bgConfig.imageName}")
                            setDefaultBackground(views)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WidgetBg", "Error applying image: ${e.message}", e)
                    e.printStackTrace()
                    setDefaultBackground(views)
                }
            }
            bgConfig.isGradient && bgConfig.gradientStartColor != null && bgConfig.gradientEndColor != null -> {
                // ⭐ 그라디언트 배경
                try {
                    val startColor = Color.parseColor(bgConfig.gradientStartColor)
                    val endColor = Color.parseColor(bgConfig.gradientEndColor)
                    val alphaInt = (bgConfig.alpha * 255).toInt().coerceIn(0, 255)

                    val orientation = when (bgConfig.gradientDirection) {
                        "horizontal" -> android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                        "vertical" -> android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
                        "diagonal_down" -> android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                        "diagonal_up" -> android.graphics.drawable.GradientDrawable.Orientation.BL_TR
                        else -> android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                    }

                    val gradientDrawable = android.graphics.drawable.GradientDrawable(
                        orientation,
                        intArrayOf(startColor, endColor)
                    )

                    val width = 1080
                    val height = 1920
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        width,
                        height,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    gradientDrawable.setBounds(0, 0, width, height)
                    gradientDrawable.alpha = alphaInt
                    gradientDrawable.draw(canvas)

                    views.setViewVisibility(R.id.widget_background_solid, View.GONE)
                    views.setViewVisibility(R.id.widget_background_image, View.VISIBLE)
                    views.setImageViewBitmap(R.id.widget_background_image, bitmap)

                    android.util.Log.d("WidgetBg", "Gradient applied: $startColor -> $endColor, alpha=$alphaInt")

                } catch (e: Exception) {
                    android.util.Log.e("WidgetBg", "Gradient error: ${e.message}", e)
                    setDefaultBackground(views)
                }
            }
            else -> {
                android.util.Log.d("WidgetBg", "Using default background")
                setDefaultBackground(views)
            }
        }
    }
    /**
     * 텍스트 컨텐츠 적용
     */
    private fun applyTextContent(
        views: RemoteViews,
        sentence: DailySentenceEntity,
        style: StyleManager.WidgetStyle,
        fontSizeConfig: DataStoreManager.FontSizeConfig,
        displayConfig: DataStoreManager.DisplayConfig,
        widgetSize: WidgetSize,
        isOnlyOneRow: Boolean
    ) {
        val textSize = adjustFontSize(fontSizeConfig.textSize, sentence.text.length, widgetSize)
        val sourceSize = adjustFontSize(fontSizeConfig.sourceSize, 0, widgetSize)
        val extraSize = adjustFontSize(fontSizeConfig.extraSize, 0, widgetSize)

        // 메인 텍스트 (항상 표시)
        views.setTextViewText(R.id.widget_text, sentence.text)
        views.setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, textSize)
        views.setTextColor(R.id.widget_text, style.textStyle.toAndroidColor())
        views.setViewVisibility(R.id.widget_text, View.VISIBLE)

        if (isOnlyOneRow) {
            // ⭐ 1행일 때: source, extra 숨김
            views.setViewVisibility(R.id.widget_source, View.GONE)
            views.setViewVisibility(R.id.widget_extra, View.GONE)
        } else {
            // ⭐ 2행 이상: source, extra 표시

            val sourceWriter = buildSourceWriterText(
                sentence.source,
                sentence.writer,
                displayConfig.showSource,
                displayConfig.showWriter
            )

            if (sourceWriter.isNotEmpty()) {
                views.setTextViewText(R.id.widget_source, sourceWriter)
                views.setTextViewTextSize(R.id.widget_source, TypedValue.COMPLEX_UNIT_SP, sourceSize)
                views.setTextColor(R.id.widget_source, style.sourceStyle.toAndroidColor())
                views.setViewVisibility(R.id.widget_source, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_source, View.GONE)
            }

            if (!sentence.extra.isNullOrEmpty() && displayConfig.showExtra) {
                views.setTextViewText(R.id.widget_extra, sentence.extra)
                views.setTextViewTextSize(R.id.widget_extra, TypedValue.COMPLEX_UNIT_SP, extraSize)
                views.setTextColor(R.id.widget_extra, style.extraStyle.toAndroidColor())
                views.setViewVisibility(R.id.widget_extra, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_extra, View.GONE)
            }
        }
    }

    /**
     * PendingIntent 설정
     */
    private fun setupPendingIntents(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        genre: String,
        sentenceCount: Int,
        sentence: DailySentenceEntity,  // ⭐ 추가
        displayConfig: DataStoreManager.DisplayConfig  // ⭐ 추가
    ) {
        // ⭐ 탭 액션 불러오기
        val dataStoreManager = DataStoreManager(context)
        val tapAction = kotlinx.coroutines.runBlocking {
            dataStoreManager.getWidgetTapAction(appWidgetId)
        }

        // ⭐ 탭 액션에 따라 PendingIntent 생성
        val tapPendingIntent = when (tapAction) {
            DataStoreManager.WidgetTapAction.OPEN_APP -> {
                // 앱 열기 (기본)
                val intent = Intent(context, com.example.dailywidget.ui.activity.MainActivity::class.java)
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            DataStoreManager.WidgetTapAction.SHOW_NEXT -> {
                // 다음 문장 (새로고침)
                val intent = Intent(context, getProviderClass(genre)).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_GENRE, genre)
                }
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            DataStoreManager.WidgetTapAction.SHARE -> {
                // 공유하기
                val shareText = buildShareText(sentence, displayConfig)
                val intent = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    },
                    null
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            DataStoreManager.WidgetTapAction.OPEN_CONFIG -> {
                // 위젯 설정 화면으로
                val intent = Intent(context, DailyWidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            DataStoreManager.WidgetTapAction.OPEN_LIST -> {
                // 목록 화면으로
                val intent = Intent(context, com.example.dailywidget.ui.activity.MainActivity::class.java).apply {
                    putExtra("navigate_to", "list")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }

        // ⭐ 위젯 컨테이너에 탭 액션 적용
        views.setOnClickPendingIntent(R.id.widget_container, tapPendingIntent)

        // 새로고침 버튼 (기존 유지)
        if (sentenceCount > 1) {
            val refreshIntent = Intent(context, getProviderClass(genre)).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_GENRE, genre)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 1000,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
        }
    }

    /**
     * ⭐ 공유용 텍스트 생성
     */
    private fun buildShareText(
        sentence: DailySentenceEntity,
        displayConfig: DataStoreManager.DisplayConfig
    ): String {
        return buildString {
            append(sentence.text)

            val sourceWriter = buildSourceWriterText(
                sentence.source,
                sentence.writer,
                displayConfig.showSource,
                displayConfig.showWriter
            )
            if (sourceWriter.isNotEmpty()) {
                append("\n\n")
                append(sourceWriter)
            }

            if (!sentence.extra.isNullOrEmpty() && displayConfig.showExtra) {
                append("\n")
                append(sentence.extra)
            }
        }
    }

    private fun selectRandomSentence(list: List<DailySentenceEntity>, exclude: Int?) = if (list.isEmpty()) null else if (exclude != null && list.size > 1) list.filter { it.id != exclude }.random() else list.random()
    private fun buildSourceWriterText(s: String?, w: String?, showS: Boolean, showW: Boolean) = listOfNotNull(if(showS) s else null, if(showW) w else null).joinToString(", ").let { if(it.isNotEmpty()) "- $it" else "" }
    /**
     * 폰트 크기 자동 조정
     * @param base 기본 폰트 크기 (20sp, 14sp, 12sp)
     * @param len 텍스트 길이 (text에만 적용, source/extra는 0 전달)
     * @param size 위젯 크기
     * @return 조정된 폰트 크기
     */
    private fun adjustFontSize(base: Float, len: Int, size: WidgetSize): Float {
        val area = size.widthRows * size.heightRows

        val lengthFactor = if (len > 0) {
            when {
                len <= 20 -> 1.0f
                len <= 40 -> 0.95f
                len <= 60 -> 0.85f
                len <= 80 -> 0.75f
                else -> 0.7f
            }
        } else {
            1.0f
        }

        val sizeFactor = when {
            area <= 2 -> 0.65f
            area <= 4 -> 0.75f
            area <= 6 -> 0.85f
            area <= 8 -> 0.92f
            else -> 1.0f
        }

        val adjustedSize = base * lengthFactor * sizeFactor

        val minSize = when (base) {
            20f -> 12f
            14f -> 10f
            12f -> 9f
            else -> base * 0.6f
        }
        val maxSize = base * 1.2f

        return adjustedSize.coerceIn(minSize, maxSize)
    }

    private fun getProviderClass(genre: String) = UnifiedWidgetProvider::class.java
    /**
     * 기본 배경 설정 (흰색)
     */
    private fun setDefaultBackground(views: RemoteViews) {
        views.setViewVisibility(R.id.widget_background_solid, View.VISIBLE)
        views.setViewVisibility(R.id.widget_background_image, View.GONE)
        views.setInt(R.id.widget_background_solid, "setColorFilter", Color.WHITE)
        views.setInt(R.id.widget_background_solid, "setImageAlpha", 255)
    }
}