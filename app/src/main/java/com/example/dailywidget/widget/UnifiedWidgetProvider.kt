package com.example.dailywidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import kotlinx.coroutines.launch

/**
 * 통합 위젯 Provider
 *
 * 모든 장르를 처리하는 단일 위젯
 * 장르는 위젯 설정 화면에서 선택
 */
class UnifiedWidgetProvider : DailyWidgetProvider() {

    override fun getGenre(): String {
        // ⭐ 이 함수는 더 이상 사용되지 않음
        return "novel"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        android.util.Log.d("UnifiedWidget", "=== onUpdate called ===")
        android.util.Log.d("UnifiedWidget", "Widget IDs: ${appWidgetIds.joinToString()}")

        appWidgetIds.forEach { appWidgetId ->
            // ⭐ 설정된 위젯만 업데이트
            updateAppWidgetWithStoredGenre(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        DailyWidgetReceiver.scheduleMidnightUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        DailyWidgetReceiver.cancelMidnightUpdate(context)
    }

    /**
     * DataStore에서 장르를 조회하여 위젯 업데이트
     */
    private fun updateAppWidgetWithStoredGenre(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val dataStoreManager = com.example.dailywidget.data.repository.DataStoreManager(context)

                android.util.Log.d("UnifiedWidget", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("UnifiedWidget", "🔄 updateAppWidgetWithStoredGenre called")
                android.util.Log.d("UnifiedWidget", "📱 appWidgetId: $appWidgetId")

                kotlinx.coroutines.delay(500)

                // ⭐ 1. 잠금 확인
                val isLocked = dataStoreManager.isWidgetUpdateLocked(appWidgetId)
                android.util.Log.d("UnifiedWidget", "🔒 isLocked: $isLocked")

                if (isLocked) {
                    android.util.Log.d("UnifiedWidget", "⏸️ Widget is LOCKED, skipping update")
                    android.util.Log.d("UnifiedWidget", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@launch
                }

                // ⭐ 2. 설정 확인
                val isConfigured = dataStoreManager.isWidgetConfigured(appWidgetId)
                android.util.Log.d("UnifiedWidget", "⚙️ isConfigured: $isConfigured")

                if (!isConfigured) {
                    android.util.Log.d("UnifiedWidget", "⏸️ Widget NOT configured, skipping update")
                    android.util.Log.d("UnifiedWidget", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@launch
                }

                // ⭐ 3. 장르 조회
                val genreId = dataStoreManager.getWidgetGenre(appWidgetId)
                android.util.Log.d("UnifiedWidget", "📚 genreId: $genreId")

                // ⭐ 4. 위젯 업데이트
                updateAppWidget(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    genre = genreId,
                    forceRefresh = false
                )

                android.util.Log.d("UnifiedWidget", "✅ Widget updated successfully with genre: $genreId")
                android.util.Log.d("UnifiedWidget", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                android.util.Log.e("UnifiedWidget", "❌ Error: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
}