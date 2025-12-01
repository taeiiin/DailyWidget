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
import java.util.*
import kotlin.math.roundToInt

// ==================== 배경 설정 데이터 및 파싱 도우미 함수 ====================

data class BackgroundConfig(
    val type: String,
    val hexColor: String = "#000000",
    val alpha: Float = 1.0f,
    val imageName: String? = null,
    val gradientStartColor: String? = null,
    val gradientEndColor: String? = null,
    val gradientDirection: String? = null
) {
    val isSolid: Boolean get() = type == "solid"
    val isImage: Boolean get() = type == "image"
    val isGradient: Boolean get() = type == "gradient"
}

fun parseBackgroundId(id: String): BackgroundConfig {
    return when {
        id.startsWith("solid:") -> {
            val parts = id.substringAfter("solid:").split(",")
            val hex = parts.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "#000000"
            val alphaStr = parts.lastOrNull()?.substringAfter("alpha:") ?: "1.0"
            val alpha = alphaStr.toFloatOrNull() ?: 1.0f
            BackgroundConfig("solid", hexColor = hex, alpha = alpha)
        }
        id.startsWith("image:") -> {
            BackgroundConfig("image", imageName = id.substringAfter("image:"))
        }
        id.startsWith("gradient:") -> {
            // gradient:색1,색2,방향,alpha:0.5
            val parts = id.substringAfter("gradient:").split(",")
            val startColor = parts.getOrNull(0) ?: "#FFFFFF"
            val endColor = parts.getOrNull(1) ?: "#000000"
            val direction = parts.getOrNull(2) ?: "horizontal"
            val alphaStr = parts.getOrNull(3)?.substringAfter("alpha:") ?: "1.0"
            val alpha = alphaStr.toFloatOrNull() ?: 1.0f
            BackgroundConfig(
                type = "gradient",
                gradientStartColor = startColor,
                gradientEndColor = endColor,
                gradientDirection = direction,
                alpha = alpha
            )
        }
        else -> {
            BackgroundConfig("solid", hexColor = "#FFFFFF", alpha = 1.0f)
        }
    }
}

// ==================== DailyWidgetProvider 클래스 ====================

abstract class DailyWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_REFRESH = "com.example.dailywidget.ACTION_REFRESH"
        private const val EXTRA_GENRE = "extra_genre"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val novelIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NovelWidgetProvider::class.java))
            val fantasyIds = appWidgetManager.getAppWidgetIds(ComponentName(context, FantasyWidgetProvider::class.java))
            val essayIds = appWidgetManager.getAppWidgetIds(ComponentName(context, EssayWidgetProvider::class.java))

            novelIds.forEach { NovelWidgetProvider().updateAppWidget(context, appWidgetManager, it, "novel") }
            fantasyIds.forEach { FantasyWidgetProvider().updateAppWidget(context, appWidgetManager, it, "fantasy") }
            essayIds.forEach { EssayWidgetProvider().updateAppWidget(context, appWidgetManager, it, "essay") }
        }

        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, genre: String) {
            val provider = when (genre) {
                "novel" -> NovelWidgetProvider()
                "fantasy" -> FantasyWidgetProvider()
                "essay" -> EssayWidgetProvider()
                else -> NovelWidgetProvider()
            }
            appWidgetIds.forEach { appWidgetId ->
                provider.updateAppWidget(context, appWidgetManager, appWidgetId, genre, forceRefresh = false)
            }
        }
    }

    abstract fun getGenre(): String

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId, getGenre(), forceRefresh = false)
        }
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

                    setupPendingIntents(context, views, appWidgetId, genre, filteredSentences.size)
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

                setupPendingIntents(context, views, appWidgetId, genre, filteredSentences.size)
                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

// ==================== Helper 함수들 ====================

    /**
     * 배경 적용
     */
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
                    setDefaultBackground(views)
                }
            }
            bgConfig.isImage && bgConfig.imageName != null -> {
                // 이미지 배경
                views.setViewVisibility(R.id.widget_background_solid, View.GONE)
                views.setViewVisibility(R.id.widget_background_image, View.VISIBLE)

                if (bgConfig.imageName.startsWith("file://")) {
                    // 사용자 이미지
                    val fileName = bgConfig.imageName.substringAfter("file://")
                    val imageFile = ImageManager.getImageFile(context, fileName)

                    if (imageFile != null && imageFile.exists()) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                            if (bitmap != null) {
                                views.setImageViewBitmap(R.id.widget_background_image, bitmap)
                            } else {
                                setDefaultBackground(views)
                            }
                        } catch (e: Exception) {
                            setDefaultBackground(views)
                        }
                    } else {
                        setDefaultBackground(views)
                    }
                } else {
                    // 기본 이미지
                    val resId = context.resources.getIdentifier(bgConfig.imageName, "drawable", context.packageName)
                    if (resId != 0) {
                        views.setImageViewResource(R.id.widget_background_image, resId)
                    } else {
                        setDefaultBackground(views)
                    }
                }
            }
            bgConfig.isGradient && bgConfig.gradientStartColor != null && bgConfig.gradientEndColor != null -> {
                // 그라디언트 배경
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
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    gradientDrawable.setBounds(0, 0, width, height)
                    gradientDrawable.alpha = alphaInt
                    gradientDrawable.draw(canvas)

                    views.setViewVisibility(R.id.widget_background_solid, View.GONE)
                    views.setViewVisibility(R.id.widget_background_image, View.VISIBLE)
                    views.setImageViewBitmap(R.id.widget_background_image, bitmap)

                } catch (e: Exception) {
                    setDefaultBackground(views)
                }
            }
            else -> {
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
        sentenceCount: Int
    ) {
        // 위젯 클릭 → 앱 열기
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // 새로고침 버튼
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
    private fun getProviderClass(genre: String) = when(genre) {"novel"->NovelWidgetProvider::class.java;"fantasy"->FantasyWidgetProvider::class.java;"essay"->EssayWidgetProvider::class.java;else->NovelWidgetProvider::class.java}

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

class NovelWidgetProvider : DailyWidgetProvider() { override fun getGenre() = "novel" }
class FantasyWidgetProvider : DailyWidgetProvider() { override fun getGenre() = "fantasy" }
class EssayWidgetProvider : DailyWidgetProvider() { override fun getGenre() = "essay" }