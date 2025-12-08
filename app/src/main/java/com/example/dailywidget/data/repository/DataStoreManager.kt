package com.example.dailywidget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.collections.get

/**
 * 위젯별 설정 저장 관리자
 */
class DataStoreManager(private val context: Context) {

    // ==================== 장르 관리 ====================

    /**
     * 기본 장르 (삭제 불가)
     */
    enum class DefaultGenre(val id: String, val displayName: String) {
        NOVEL("novel", "소설"),
        FANTASY("fantasy", "판타지"),
        POEM("poem", "시");

        companion object {
            fun getAll(): List<Genre> = values().map {
                Genre(it.id, it.displayName, isBuiltIn = true)
            }

            fun fromId(id: String): DefaultGenre? = values().find { it.id == id }

            fun isDefault(id: String): Boolean = values().any { it.id == id }

        }
    }

    /**
     * 장르 (기본 + 사용자 정의)
     */
    data class Genre(
        val id: String,
        val displayName: String,
        val isBuiltIn: Boolean
    )

    /**
     * 사용자 정의 장르 (DataStore 저장용)
     */
    @kotlinx.serialization.Serializable
    data class CustomGenre(
        val id: String,
        val displayName: String
    )

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

        // ⭐ 홈 화면 뷰 모드 키
        private val HOME_VIEW_MODE = stringPreferencesKey("home_view_mode")

        // ⭐ 사용자 정의 장르 키
        private val CUSTOM_GENRES_KEY = stringPreferencesKey("custom_genres")
    }

    data class WidgetConfig(
        val styleId: Int = StyleManager.DEFAULT_STYLE_ID,
        val backgroundId: String = StyleManager.DEFAULT_BACKGROUND_ID,
        val genreId: String = "novel"
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

    /**
     * 위젯이 이미 설정되었는지 확인
     */
    suspend fun isWidgetConfigured(appWidgetId: Int): Boolean {
        val preferences = context.dataStore.data.first()
        val key = stringPreferencesKey("widget_genre_$appWidgetId")
        return preferences.contains(key)
    }

    // ==================== 위젯별 설정 ====================

    suspend fun saveWidgetConfig(appWidgetId: Int, config: WidgetConfig) {
        context.dataStore.edit { preferences ->
            preferences[getStyleIdKey(appWidgetId)] = config.styleId
            preferences[getBackgroundIdKey(appWidgetId)] = config.backgroundId
            // ⭐ genreId도 저장!
            preferences[stringPreferencesKey("widget_genre_$appWidgetId")] = config.genreId
        }

        android.util.Log.d("DataStore", "Saved config for widget $appWidgetId: genreId=${config.genreId}")
    }

    suspend fun getWidgetConfig(appWidgetId: Int): WidgetConfig {
        val preferences = context.dataStore.data.first()

        // ⭐ genreId도 조회
        val genreId = preferences[stringPreferencesKey("widget_genre_$appWidgetId")] ?: "novel"

        return WidgetConfig(
            styleId = preferences[getStyleIdKey(appWidgetId)] ?: StyleManager.DEFAULT_STYLE_ID,
            backgroundId = preferences[getBackgroundIdKey(appWidgetId)] ?: StyleManager.DEFAULT_BACKGROUND_ID,
            genreId = genreId  // ⭐ 추가
        )
    }

    fun getWidgetConfigFlow(appWidgetId: Int): Flow<WidgetConfig> {
        return context.dataStore.data.map { preferences ->
            val genreId = preferences[stringPreferencesKey("widget_genre_$appWidgetId")] ?: "novel"

            WidgetConfig(
                styleId = preferences[getStyleIdKey(appWidgetId)] ?: StyleManager.DEFAULT_STYLE_ID,
                backgroundId = preferences[getBackgroundIdKey(appWidgetId)] ?: StyleManager.DEFAULT_BACKGROUND_ID,
                genreId = genreId  // ⭐ 추가
            )
        }
    }

    suspend fun deleteWidgetConfig(appWidgetId: Int) {
        context.dataStore.edit { preferences ->
            preferences.remove(getStyleIdKey(appWidgetId))
            preferences.remove(getBackgroundIdKey(appWidgetId))
            preferences.remove(getLastSentenceIdKey(appWidgetId))
            // ⭐ genreId도 삭제
            preferences.remove(stringPreferencesKey("widget_genre_$appWidgetId"))
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

    /**
     * ⭐ 홈 화면 뷰 모드
     */
    enum class HomeViewMode {
        CARD, LIST
    }

    /**
     * ⭐ 홈 화면 뷰 모드 저장
     */
    suspend fun saveHomeViewMode(mode: HomeViewMode) {
        context.dataStore.edit { preferences ->
            preferences[HOME_VIEW_MODE] = mode.name
        }
    }

    /**
     * ⭐ 홈 화면 뷰 모드 불러오기
     */
    suspend fun getHomeViewMode(): HomeViewMode {
        val preferences = context.dataStore.data.first()
        val modeName = preferences[HOME_VIEW_MODE] ?: HomeViewMode.CARD.name
        return try {
            HomeViewMode.valueOf(modeName)
        } catch (e: Exception) {
            HomeViewMode.CARD
        }
    }

    /**
     * ⭐ 홈 화면 뷰 모드 Flow로 관찰
     */
    fun getHomeViewModeFlow(): Flow<HomeViewMode> {
        return context.dataStore.data.map { preferences ->
            val modeName = preferences[HOME_VIEW_MODE] ?: HomeViewMode.CARD.name
            try {
                HomeViewMode.valueOf(modeName)
            } catch (e: Exception) {
                HomeViewMode.CARD
            }
        }
    }


// ==================== 위젯 탭 액션 ====================

    /**
     * 위젯 탭 액션
     */
    enum class WidgetTapAction(val label: String, val description: String) {
        OPEN_APP("앱 열기", "메인 화면으로 이동"),
        SHOW_NEXT("다음 문장", "새로운 문장으로 변경"),
        SHARE("공유하기", "문장 공유하기"),
        OPEN_CONFIG("위젯 설정", "위젯 편집 화면으로"),
        OPEN_LIST("목록 화면", "문장 목록 화면으로")
    }

    /**
     * 위젯 탭 액션 저장
     */
    suspend fun saveWidgetTapAction(appWidgetId: Int, action: WidgetTapAction) {
        context.dataStore.edit { preferences ->
            preferences[getWidgetTapActionKey(appWidgetId)] = action.name
        }
    }

    /**
     * 위젯 탭 액션 불러오기
     */
    suspend fun getWidgetTapAction(appWidgetId: Int): WidgetTapAction {
        val preferences = context.dataStore.data.first()
        val actionName = preferences[getWidgetTapActionKey(appWidgetId)]
            ?: WidgetTapAction.OPEN_APP.name
        return try {
            WidgetTapAction.valueOf(actionName)
        } catch (e: Exception) {
            WidgetTapAction.OPEN_APP
        }
    }

    /**
     * 위젯 탭 액션 키 생성
     */
    private fun getWidgetTapActionKey(appWidgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("widget_${appWidgetId}_tap_action")
    }

    // ==================== 위젯 장르 관리 ====================

    // 장르 저장
    suspend fun saveWidgetGenre(appWidgetId: Int, genreId: String) {
        context.dataStore.edit { preferences ->
            val key = stringPreferencesKey("widget_genre_$appWidgetId")
            preferences[key] = genreId
            android.util.Log.d("DataStore", "Saved: widget_genre_$appWidgetId = $genreId")
        }

        // ⭐ 저장 완료 확인
        val saved = getWidgetGenre(appWidgetId)
        android.util.Log.d("DataStore", "Verification: widget_genre_$appWidgetId = $saved")
    }

    // 장르 조회
    suspend fun getWidgetGenre(appWidgetId: Int): String {
        val preferences = context.dataStore.data.first()
        val key = stringPreferencesKey("widget_genre_$appWidgetId")
        return preferences[key] ?: "novel"
    }

// ==================== 사용자 정의 장르 관리 ====================

    /**
     * 모든 장르 가져오기 (기본 + 사용자 정의)
     */
    suspend fun getAllGenres(): List<Genre> {
        val defaultGenres = DefaultGenre.getAll()
        val customGenres = getCustomGenres().map {
            Genre(it.id, it.displayName, isBuiltIn = false)
        }
        return defaultGenres + customGenres
    }

    /**
     * 사용자 정의 장르 목록 가져오기
     */
    suspend fun getCustomGenres(): List<CustomGenre> {
        val preferences = context.dataStore.data.first()
        val json = preferences[CUSTOM_GENRES_KEY] ?: return emptyList()
        return try {
            kotlinx.serialization.json.Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 사용자 정의 장르 추가
     */
    suspend fun addCustomGenre(id: String, displayName: String): Boolean {
        // ID 유효성 검사
        if (id.isBlank() || displayName.isBlank()) return false
        if (DefaultGenre.isDefault(id)) return false  // 기본 장르 ID와 중복 불가

        // ID 규칙: 소문자 영문, 숫자, 언더스코어만
        if (!id.matches(Regex("^[a-z0-9_]+$"))) return false

        val currentGenres = getCustomGenres().toMutableList()

        // 중복 체크
        if (currentGenres.any { it.id == id }) return false

        currentGenres.add(CustomGenre(id, displayName))

        context.dataStore.edit { preferences ->
            val json = kotlinx.serialization.json.Json.encodeToString(currentGenres)
            preferences[CUSTOM_GENRES_KEY] = json
        }

        return true
    }

    /**
     * 사용자 정의 장르 삭제
     */
    suspend fun removeCustomGenre(id: String): Boolean {
        // 기본 장르는 삭제 불가
        if (DefaultGenre.isDefault(id)) return false

        val currentGenres = getCustomGenres().toMutableList()
        val removed = currentGenres.removeIf { it.id == id }

        if (removed) {
            context.dataStore.edit { preferences ->
                val json = kotlinx.serialization.json.Json.encodeToString(currentGenres)
                preferences[CUSTOM_GENRES_KEY] = json
            }
        }

        return removed
    }

    /**
     * 장르 표시명 가져오기
     */
    suspend fun getGenreDisplayName(genreId: String): String {
        // 기본 장르 확인
        DefaultGenre.fromId(genreId)?.let {
            return it.displayName
        }

        // 사용자 정의 장르 확인
        getCustomGenres().find { it.id == genreId }?.let {
            return it.displayName
        }

        // 없으면 ID 그대로 반환
        return genreId
    }

    // ==================== 업데이트 잠금 ====================

    private val WIDGET_UPDATE_LOCK_PREFIX = "widget_update_lock_"

    /**
     * 위젯 업데이트 잠금 (설정 중 자동 업데이트 방지)
     */
    suspend fun setWidgetUpdateLock(appWidgetId: Int, locked: Boolean) {
        context.dataStore.edit { preferences ->
            val key = booleanPreferencesKey("$WIDGET_UPDATE_LOCK_PREFIX$appWidgetId")
            preferences[key] = locked
        }
        android.util.Log.d("DataStore", "🔒 Widget $appWidgetId lock: $locked")
    }

    /**
     * 위젯 업데이트 잠금 확인
     */
    suspend fun isWidgetUpdateLocked(appWidgetId: Int): Boolean {
        val preferences = context.dataStore.data.first()
        val key = booleanPreferencesKey("$WIDGET_UPDATE_LOCK_PREFIX$appWidgetId")
        return preferences[key] ?: false
    }
}

fun Context.getWidgetDataStore(): DataStoreManager {
    return DataStoreManager(this)
}
