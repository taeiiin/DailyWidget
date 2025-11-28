package com.example.dailywidget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dailywidget.util.StyleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 위젯별 설정 저장 관리자
 */
class DataStoreManager(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "widget_settings"
        )

        // 기본 설정 키
        private val DEFAULT_STYLE_ID = intPreferencesKey("default_style_id")
        private val DEFAULT_BACKGROUND_ID = stringPreferencesKey("default_background_id")

        // 강제 적용 플래그 키
        private val IS_WIDGET_FORCE_STYLE_ENABLED = booleanPreferencesKey("is_widget_force_style_enabled")
        private val IS_HOME_FORCE_STYLE_ENABLED = booleanPreferencesKey("is_home_force_style_enabled")

        // 표시 설정 키
        private val SHOW_SOURCE = booleanPreferencesKey("show_source")
        private val SHOW_WRITER = booleanPreferencesKey("show_writer")
        private val SHOW_EXTRA = booleanPreferencesKey("show_extra")

        // ⭐ 폰트 크기 설정 키
        private val FONT_SIZE_TEXT = floatPreferencesKey("font_size_text")
        private val FONT_SIZE_SOURCE = floatPreferencesKey("font_size_source")
        private val FONT_SIZE_WRITER = floatPreferencesKey("font_size_writer")
        private val FONT_SIZE_EXTRA = floatPreferencesKey("font_size_extra")
    }

    data class WidgetConfig(
        val styleId: Int = StyleManager.DEFAULT_STYLE_ID,
        val backgroundId: String = StyleManager.DEFAULT_BACKGROUND_ID
    )

    data class DefaultConfig(
        val styleId: Int = StyleManager.DEFAULT_STYLE_ID,
        val backgroundId: String = StyleManager.DEFAULT_BACKGROUND_ID
    )

    data class DisplayConfig(
        val showSource: Boolean = true,
        val showWriter: Boolean = true,
        val showExtra: Boolean = true
    )

    /**
     * ⭐ 폰트 크기 설정 데이터 클래스
     */
    data class FontSizeConfig(
        val textSize: Float = 20f,      // 기본 20sp
        val sourceSize: Float = 14f,    // 기본 14sp
        val writerSize: Float = 14f,    // 기본 14sp
        val extraSize: Float = 12f      // 기본 12sp
    )

    // ==================== 위젯별 설정 ====================

    suspend fun saveWidgetConfig(appWidgetId: Int, config: WidgetConfig) {
        context.dataStore.edit { preferences ->
            preferences[getStyleIdKey(appWidgetId)] = config.styleId
            preferences[getBackgroundIdKey(appWidgetId)] = config.backgroundId
        }
    }

    suspend fun getWidgetConfig(appWidgetId: Int): WidgetConfig {
        val preferences = context.dataStore.data.first()
        return WidgetConfig(
            styleId = preferences[getStyleIdKey(appWidgetId)] ?: StyleManager.DEFAULT_STYLE_ID,
            backgroundId = preferences[getBackgroundIdKey(appWidgetId)] ?: StyleManager.DEFAULT_BACKGROUND_ID
        )
    }

    fun getWidgetConfigFlow(appWidgetId: Int): Flow<WidgetConfig> {
        return context.dataStore.data.map { preferences ->
            WidgetConfig(
                styleId = preferences[getStyleIdKey(appWidgetId)] ?: StyleManager.DEFAULT_STYLE_ID,
                backgroundId = preferences[getBackgroundIdKey(appWidgetId)] ?: StyleManager.DEFAULT_BACKGROUND_ID
            )
        }
    }

    suspend fun deleteWidgetConfig(appWidgetId: Int) {
        context.dataStore.edit { preferences ->
            preferences.remove(getStyleIdKey(appWidgetId))
            preferences.remove(getBackgroundIdKey(appWidgetId))
            preferences.remove(getLastSentenceIdKey(appWidgetId))
        }
    }

    suspend fun clearAllWidgetConfigs() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // ==================== 기본 설정 ====================

    suspend fun saveDefaultConfig(config: DefaultConfig) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_STYLE_ID] = config.styleId
            preferences[DEFAULT_BACKGROUND_ID] = config.backgroundId
        }
    }

    suspend fun getDefaultConfig(): DefaultConfig {
        val preferences = context.dataStore.data.first()
        return DefaultConfig(
            styleId = preferences[DEFAULT_STYLE_ID] ?: StyleManager.DEFAULT_STYLE_ID,
            backgroundId = preferences[DEFAULT_BACKGROUND_ID] ?: StyleManager.DEFAULT_BACKGROUND_ID
        )
    }

    fun getDefaultConfigFlow(): Flow<DefaultConfig> {
        return context.dataStore.data.map { preferences ->
            DefaultConfig(
                styleId = preferences[DEFAULT_STYLE_ID] ?: StyleManager.DEFAULT_STYLE_ID,
                backgroundId = preferences[DEFAULT_BACKGROUND_ID] ?: StyleManager.DEFAULT_BACKGROUND_ID
            )
        }
    }

    // ==================== 강제 적용 플래그 ====================

    suspend fun setWidgetForceStyleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_WIDGET_FORCE_STYLE_ENABLED] = enabled
        }
    }

    suspend fun isWidgetForceStyleEnabled(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[IS_WIDGET_FORCE_STYLE_ENABLED] ?: false
    }

    suspend fun setHomeForceStyleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_HOME_FORCE_STYLE_ENABLED] = enabled
        }
    }

    suspend fun isHomeForceStyleEnabled(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[IS_HOME_FORCE_STYLE_ENABLED] ?: false
    }

    // ==================== 표시 설정 ====================

    suspend fun saveDisplayConfig(config: DisplayConfig) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_SOURCE] = config.showSource
            preferences[SHOW_WRITER] = config.showWriter
            preferences[SHOW_EXTRA] = config.showExtra
        }
    }

    suspend fun getDisplayConfig(): DisplayConfig {
        val preferences = context.dataStore.data.first()
        return DisplayConfig(
            showSource = preferences[SHOW_SOURCE] ?: true,
            showWriter = preferences[SHOW_WRITER] ?: true,
            showExtra = preferences[SHOW_EXTRA] ?: true
        )
    }

    fun getDisplayConfigFlow(): Flow<DisplayConfig> {
        return context.dataStore.data.map { preferences ->
            DisplayConfig(
                showSource = preferences[SHOW_SOURCE] ?: true,
                showWriter = preferences[SHOW_WRITER] ?: true,
                showExtra = preferences[SHOW_EXTRA] ?: true
            )
        }
    }

    // ==================== ⭐ 폰트 크기 설정 ====================

    /**
     * 폰트 크기 설정 저장
     */
    suspend fun saveFontSizeConfig(config: FontSizeConfig) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_TEXT] = config.textSize
            preferences[FONT_SIZE_SOURCE] = config.sourceSize
            preferences[FONT_SIZE_WRITER] = config.writerSize
            preferences[FONT_SIZE_EXTRA] = config.extraSize
        }
    }

    /**
     * 폰트 크기 설정 불러오기
     */
    suspend fun getFontSizeConfig(): FontSizeConfig {
        val preferences = context.dataStore.data.first()
        return FontSizeConfig(
            textSize = preferences[FONT_SIZE_TEXT] ?: 20f,
            sourceSize = preferences[FONT_SIZE_SOURCE] ?: 14f,
            writerSize = preferences[FONT_SIZE_WRITER] ?: 14f,
            extraSize = preferences[FONT_SIZE_EXTRA] ?: 12f
        )
    }

    /**
     * 폰트 크기 설정 Flow로 관찰
     */
    fun getFontSizeConfigFlow(): Flow<FontSizeConfig> {
        return context.dataStore.data.map { preferences ->
            FontSizeConfig(
                textSize = preferences[FONT_SIZE_TEXT] ?: 20f,
                sourceSize = preferences[FONT_SIZE_SOURCE] ?: 14f,
                writerSize = preferences[FONT_SIZE_WRITER] ?: 14f,
                extraSize = preferences[FONT_SIZE_EXTRA] ?: 12f
            )
        }
    }

    // ==================== 현재 문장 ID (새로고침용) ====================

    suspend fun saveLastSentenceId(appWidgetId: Int, sentenceId: Int) {
        context.dataStore.edit { preferences ->
            preferences[getLastSentenceIdKey(appWidgetId)] = sentenceId
        }
    }

    suspend fun getLastSentenceId(appWidgetId: Int): Int? {
        val preferences = context.dataStore.data.first()
        return preferences[getLastSentenceIdKey(appWidgetId)]
    }

    // ==================== Private Helper Methods ====================

    private fun getStyleIdKey(appWidgetId: Int): Preferences.Key<Int> {
        return intPreferencesKey("widget_${appWidgetId}_style")
    }

    private fun getBackgroundIdKey(appWidgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("widget_${appWidgetId}_background")
    }

    private fun getLastSentenceIdKey(appWidgetId: Int): Preferences.Key<Int> {
        return intPreferencesKey("widget_${appWidgetId}_last_sentence_id")
    }
}

fun Context.getWidgetDataStore(): DataStoreManager {
    return DataStoreManager(this)
}