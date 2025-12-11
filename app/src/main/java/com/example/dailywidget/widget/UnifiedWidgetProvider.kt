package com.example.dailywidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.example.dailywidget.data.repository.DataStoreManager
import kotlinx.coroutines.launch

/**
 * 통합 위젯 Provider
 * - 모든 장르를 처리하는 단일 위젯
 * - 복수 장르 선택 지원
 * - 장르는 GenreSelectionActivity에서 선택
 * - 설정된 위젯만 업데이트 (잠금/미설정 체크)
 */
class UnifiedWidgetProvider : DailyWidgetProvider() {

    override fun getGenre(): String {
        // 하위 호환용 (더 이상 사용되지 않음)
        return "novel"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        DailyWidgetReceiver.scheduleMidnightUpdate(context)

        appWidgetIds.forEach { appWidgetId ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val dataStoreManager = DataStoreManager(context)

                // 잠금 상태 확인
                val isLocked = dataStoreManager.isWidgetUpdateLocked(appWidgetId)
                if (isLocked) {
                    android.util.Log.d("UnifiedWidget", "Widget $appWidgetId is locked, skipping update")
                    return@launch
                }

                // 복수 장르 조회
                val genres = dataStoreManager.getWidgetGenres(appWidgetId)
                if (genres.isEmpty()) {
                    android.util.Log.d("UnifiedWidget", "Widget $appWidgetId has no genres, skipping update")
                    return@launch
                }

                // 업데이트 실행
                updateAppWidgetWithGenres(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    genres = genres,
                    forceRefresh = false
                )
            }
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
     * DataStore에서 복수 장르 조회 후 위젯 업데이트
     * 잠금 및 설정 여부 확인
     */
    private fun updateAppWidgetWithStoredGenres(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val dataStoreManager = com.example.dailywidget.data.repository.DataStoreManager(context)

                kotlinx.coroutines.delay(500)

                // 1. 잠금 확인 (설정 중에는 업데이트 안함)
                val isLocked = dataStoreManager.isWidgetUpdateLocked(appWidgetId)

                if (isLocked) {
                    return@launch
                }

                // 2. 설정 완료 여부 확인
                val isConfigured = dataStoreManager.isWidgetConfigured(appWidgetId)

                if (!isConfigured) {
                    return@launch
                }

                // 3. 복수 장르 조회
                val genres = dataStoreManager.getWidgetGenres(appWidgetId)

                // 4. 위젯 업데이트 (복수 장르 버전 호출)
                updateAppWidgetWithGenres(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    genres = genres,
                    forceRefresh = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}