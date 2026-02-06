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
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore 설정 관리자
 * - 위젯별 설정 (스타일, 배경, 장르, 터치 동작)
 * - 전역 설정 (기본값, 표시 옵션, 폰트 크기)
 * - 장르 관리 (기본 + 사용자 정의)
 * - 업데이트 잠금 (설정 중 자동 업데이트 방지)
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
            /** 모든 기본 장르를 Genre 리스트로 반환 */
            fun getAll(): List<Genre> = values().map {
                Genre(it.id, it.displayName, isBuiltIn = true)
            }

            /** ID로 기본 장르 찾기 */
            fun fromId(id: String): DefaultGenre? = values().find { it.id == id }

            /** 기본 장르 ID 여부 확인 */
            fun isDefault(id: String): Boolean = values().any { it.id == id }
        }
    }

    /** 장르 데이터 클래스 (기본 + 사용자 정의 통합) */
    data class Genre(
        val id: String,
        val displayName: String,
        val isBuiltIn: Boolean
    )

    /** 사용자 정의 장르 (JSON 직렬화용) */
    @kotlinx.serialization.Serializable
    data class CustomGenre(
        val id: String,
        val displayName: String
    )

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "widget_settings"
        )

        // 기본 설정
        private val DEFAULT_STYLE_ID = intPreferencesKey("default_style_id")
        private val DEFAULT_BACKGROUND_ID = stringPreferencesKey("default_background_id")

        // 강제 적용 플래그
        private val IS_WIDGET_FORCE_STYLE_ENABLED = booleanPreferencesKey("is_widget_force_style_enabled")
        private val IS_HOME_FORCE_STYLE_ENABLED = booleanPreferencesKey("is_home_force_style_enabled")

        // 표시 설정
        private val SHOW_SOURCE = booleanPreferencesKey("show_source")
        private val SHOW_WRITER = booleanPreferencesKey("show_writer")
        private val SHOW_EXTRA = booleanPreferencesKey("show_extra")

        // 폰트 크기
        private val FONT_SIZE_TEXT = floatPreferencesKey("font_size_text")
        private val FONT_SIZE_SOURCE = floatPreferencesKey("font_size_source")
        private val FONT_SIZE_WRITER = floatPreferencesKey("font_size_writer")
        private val FONT_SIZE_EXTRA = floatPreferencesKey("font_size_extra")

        // 홈 화면 뷰 모드
        private val HOME_VIEW_MODE = stringPreferencesKey("home_view_mode")

        // 사용자 정의 장르
        private val CUSTOM_GENRES_KEY = stringPreferencesKey("custom_genres")

        // 업데이트 잠금
        private const val WIDGET_UPDATE_LOCK_PREFIX = "widget_update_lock_"

        // 알림 설정
        private val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")

        // 앱 버전 관리
        private val LAST_LOADED_VERSION = intPreferencesKey("last_loaded_version")
        private val HAS_UPDATE_AVAILABLE = booleanPreferencesKey("has_update_available")
        private val NEW_SENTENCE_COUNT = intPreferencesKey("new_sentence_count")
    }

    /** 위젯 설정 데이터 클래스 */
    data class WidgetConfig(
        val styleId: Int = StyleManager.DEFAULT_STYLE_ID,
        val backgroundId: String = StyleManager.DEFAULT_BACKGROUND_ID,
        val genreId: String = "novel"
    )

    /** 기본 설정 데이터 클래스 */
    data class DefaultConfig(
        val styleId: Int = StyleManager.DEFAULT_STYLE_ID,
        val backgroundId: String = StyleManager.DEFAULT_BACKGROUND_ID
    )

    /** 표시 설정 데이터 클래스 */
    data class DisplayConfig(
        val showSource: Boolean = true,
        val showWriter: Boolean = true,
        val showExtra: Boolean = true
    )

    /** 폰트 크기 설정 데이터 클래스 (기본값: 메인 16sp, 출처/작가 13sp, 특이사항 10sp) */
    data class FontSizeConfig(
        val textSize: Float = 16f,
        val sourceSize: Float = 13f,
        val writerSize: Float = 13f,
        val extraSize: Float = 10f
    )

    /** 홈 화면 뷰 모드 */
    enum class HomeViewMode {
        CARD, LIST
    }

    /** 위젯 터치 동작 */
    enum class WidgetTapAction(val label: String, val description: String) {
        SHOW_NEXT("다음 문장", "새로운 문장으로 변경"),
        OPEN_APP("앱 열기", "메인 화면으로 이동"),
        SHARE("공유하기", "문장 공유하기"),
        OPEN_CONFIG("위젯 설정", "위젯 편집 화면으로"),
        OPEN_LIST("목록 화면", "문장 목록 화면으로")
    }

    // ==================== 위젯 설정 상태 ====================

    /** 위젯이 설정되었는지 확인 */
    suspend fun isWidgetConfigured(appWidgetId: Int): Boolean {
        val preferences = context.dataStore.data.first()
        val key = stringPreferencesKey("widget_genre_$appWidgetId")
        return preferences.contains(key)
    }

    // ==================== 위젯별 설정 ====================

    /** 위젯 설정 저장 (스타일, 배경, 장르) */
    suspend fun saveWidgetConfig(appWidgetId: Int, config: WidgetConfig) {
        context.dataStore.edit { preferences ->
            preferences[getStyleIdKey(appWidgetId)] = config.styleId
            preferences[getBackgroundIdKey(appWidgetId)] = config.backgroundId
            preferences[stringPreferencesKey("widget_genre_$appWidgetId")] = config.genreId
        }
    }

    /** 위젯 설정 조회 */
    suspend fun getWidgetConfig(appWidgetId: Int): WidgetConfig {
        val preferences = context.dataStore.data.first()
        val genreId = preferences[stringPreferencesKey("widget_genre_$appWidgetId")] ?: "novel"

        return WidgetConfig(
            styleId = preferences[getStyleIdKey(appWidgetId)] ?: StyleManager.DEFAULT_STYLE_ID,
            backgroundId = preferences[getBackgroundIdKey(appWidgetId)] ?: StyleManager.DEFAULT_BACKGROUND_ID,
            genreId = genreId
        )
    }

    /** 위젯 설정 Flow로 관찰 */
    fun getWidgetConfigFlow(appWidgetId: Int): Flow<WidgetConfig> {
        return context.dataStore.data.map { preferences ->
            val genreId = preferences[stringPreferencesKey("widget_genre_$appWidgetId")] ?: "novel"

            WidgetConfig(
                styleId = preferences[getStyleIdKey(appWidgetId)] ?: StyleManager.DEFAULT_STYLE_ID,
                backgroundId = preferences[getBackgroundIdKey(appWidgetId)] ?: StyleManager.DEFAULT_BACKGROUND_ID,
                genreId = genreId
            )
        }
    }

    /** 위젯 설정 삭제 */
    suspend fun deleteWidgetConfig(appWidgetId: Int) {
        context.dataStore.edit { preferences ->
            preferences.remove(getStyleIdKey(appWidgetId))
            preferences.remove(getBackgroundIdKey(appWidgetId))
            preferences.remove(getLastSentenceIdKey(appWidgetId))
            preferences.remove(stringPreferencesKey("widget_genre_$appWidgetId"))
        }
    }

    /** 모든 위젯 설정 초기화 */
    suspend fun clearAllWidgetConfigs() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /** 스타일과 배경만 저장 (장르는 건드리지 않음, 위젯 편집 시 장르 덮어쓰기 방지) */
    suspend fun saveWidgetStyleAndBackground(appWidgetId: Int, styleId: Int, backgroundId: String) {
        context.dataStore.edit { preferences ->
            preferences[intPreferencesKey("widget_${appWidgetId}_style")] = styleId
            preferences[stringPreferencesKey("widget_${appWidgetId}_background")] = backgroundId
        }
    }

    // ==================== 기본 설정 ====================

    /** 기본 설정 저장 */
    suspend fun saveDefaultConfig(config: DefaultConfig) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_STYLE_ID] = config.styleId
            preferences[DEFAULT_BACKGROUND_ID] = config.backgroundId
        }
    }

    /** 기본 설정 조회 */
    suspend fun getDefaultConfig(): DefaultConfig {
        val preferences = context.dataStore.data.first()
        return DefaultConfig(
            styleId = preferences[DEFAULT_STYLE_ID] ?: StyleManager.DEFAULT_STYLE_ID,
            backgroundId = preferences[DEFAULT_BACKGROUND_ID] ?: StyleManager.DEFAULT_BACKGROUND_ID
        )
    }

    /** 기본 설정 Flow로 관찰 */
    fun getDefaultConfigFlow(): Flow<DefaultConfig> {
        return context.dataStore.data.map { preferences ->
            DefaultConfig(
                styleId = preferences[DEFAULT_STYLE_ID] ?: StyleManager.DEFAULT_STYLE_ID,
                backgroundId = preferences[DEFAULT_BACKGROUND_ID] ?: StyleManager.DEFAULT_BACKGROUND_ID
            )
        }
    }

    // ==================== 강제 적용 플래그 ====================

    /** 위젯 강제 스타일 적용 활성화/비활성화 */
    suspend fun setWidgetForceStyleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_WIDGET_FORCE_STYLE_ENABLED] = enabled
        }
    }

    /** 위젯 강제 스타일 적용 여부 조회 */
    suspend fun isWidgetForceStyleEnabled(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[IS_WIDGET_FORCE_STYLE_ENABLED] ?: false
    }

    /** 홈 화면 강제 스타일 적용 활성화/비활성화 */
    suspend fun setHomeForceStyleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_HOME_FORCE_STYLE_ENABLED] = enabled
        }
    }

    /** 홈 화면 강제 스타일 적용 여부 조회 */
    suspend fun isHomeForceStyleEnabled(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[IS_HOME_FORCE_STYLE_ENABLED] ?: false
    }

    // ==================== 표시 설정 ====================

    /** 표시 설정 저장 */
    suspend fun saveDisplayConfig(config: DisplayConfig) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_SOURCE] = config.showSource
            preferences[SHOW_WRITER] = config.showWriter
            preferences[SHOW_EXTRA] = config.showExtra
        }
    }

    /** 표시 설정 조회 */
    suspend fun getDisplayConfig(): DisplayConfig {
        val preferences = context.dataStore.data.first()
        return DisplayConfig(
            showSource = preferences[SHOW_SOURCE] ?: true,
            showWriter = preferences[SHOW_WRITER] ?: true,
            showExtra = preferences[SHOW_EXTRA] ?: true
        )
    }

    /** 표시 설정 Flow로 관찰 */
    fun getDisplayConfigFlow(): Flow<DisplayConfig> {
        return context.dataStore.data.map { preferences ->
            DisplayConfig(
                showSource = preferences[SHOW_SOURCE] ?: true,
                showWriter = preferences[SHOW_WRITER] ?: true,
                showExtra = preferences[SHOW_EXTRA] ?: true
            )
        }
    }

    // ==================== 폰트 크기 설정 ====================

    /** 폰트 크기 설정 저장 */
    suspend fun saveFontSizeConfig(config: FontSizeConfig) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_TEXT] = config.textSize
            preferences[FONT_SIZE_SOURCE] = config.sourceSize
            preferences[FONT_SIZE_WRITER] = config.writerSize
            preferences[FONT_SIZE_EXTRA] = config.extraSize
        }
    }

    /** 폰트 크기 설정 조회 */
    suspend fun getFontSizeConfig(): FontSizeConfig {
        val preferences = context.dataStore.data.first()
        return FontSizeConfig(
            textSize = preferences[FONT_SIZE_TEXT] ?: 16f,
            sourceSize = preferences[FONT_SIZE_SOURCE] ?: 13f,
            writerSize = preferences[FONT_SIZE_WRITER] ?: 13f,
            extraSize = preferences[FONT_SIZE_EXTRA] ?: 10f
        )
    }

    /** 폰트 크기 설정 Flow로 관찰 */
    fun getFontSizeConfigFlow(): Flow<FontSizeConfig> {
        return context.dataStore.data.map { preferences ->
            FontSizeConfig(
                textSize = preferences[FONT_SIZE_TEXT] ?: 16f,
                sourceSize = preferences[FONT_SIZE_SOURCE] ?: 13f,
                writerSize = preferences[FONT_SIZE_WRITER] ?: 13f,
                extraSize = preferences[FONT_SIZE_EXTRA] ?: 10f
            )
        }
    }

    // ==================== 홈 화면 뷰 모드 ====================

    /** 홈 화면 뷰 모드 저장 */
    suspend fun saveHomeViewMode(mode: HomeViewMode) {
        context.dataStore.edit { preferences ->
            preferences[HOME_VIEW_MODE] = mode.name
        }
    }

    /** 홈 화면 뷰 모드 조회 */
    suspend fun getHomeViewMode(): HomeViewMode {
        val preferences = context.dataStore.data.first()
        val modeName = preferences[HOME_VIEW_MODE] ?: HomeViewMode.CARD.name
        return try {
            HomeViewMode.valueOf(modeName)
        } catch (e: Exception) {
            HomeViewMode.CARD
        }
    }

    /** 홈 화면 뷰 모드 Flow로 관찰 */
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

    // ==================== 현재 문장 ID (새로고침용) ====================

    /** 마지막으로 표시된 문장 ID 저장 */
    suspend fun saveLastSentenceId(appWidgetId: Int, sentenceId: Int) {
        context.dataStore.edit { preferences ->
            preferences[getLastSentenceIdKey(appWidgetId)] = sentenceId
        }
    }

    /** 마지막으로 표시된 문장 ID 조회 */
    suspend fun getLastSentenceId(appWidgetId: Int): Int? {
        val preferences = context.dataStore.data.first()
        return preferences[getLastSentenceIdKey(appWidgetId)]
    }

    // ==================== 위젯 터치 동작 ====================

    /** 위젯 터치 동작 저장 */
    suspend fun saveWidgetTapAction(appWidgetId: Int, tapAction: WidgetTapAction) {
        context.dataStore.edit { preferences ->
            preferences[getWidgetTapActionKey(appWidgetId)] = tapAction.name
        }
        // 저장 완료 확인
        kotlinx.coroutines.delay(100)
    }

    /** 위젯 터치 동작 조회 (기본값: SHOW_NEXT) */
    suspend fun getWidgetTapAction(appWidgetId: Int): WidgetTapAction {
        val preferences = context.dataStore.data.first()
        val actionName = preferences[getWidgetTapActionKey(appWidgetId)]
            ?: WidgetTapAction.SHOW_NEXT.name
        return try {
            WidgetTapAction.valueOf(actionName)
        } catch (e: Exception) {
            WidgetTapAction.SHOW_NEXT
        }
    }

    // ==================== 위젯 장르 관리 ====================

    /** 복수 장르 저장 (쉼표로 구분) */
    suspend fun saveWidgetGenres(appWidgetId: Int, genreIds: List<String>) {
        context.dataStore.edit { preferences ->
            val key = stringPreferencesKey("widget_genre_$appWidgetId")
            val genreString = genreIds.joinToString(",")
            preferences[key] = genreString
        }

        val saved = getWidgetGenres(appWidgetId)
    }

    /** 단일 장르 저장 (하위 호환성) */
    suspend fun saveWidgetGenre(appWidgetId: Int, genreId: String) {
        saveWidgetGenres(appWidgetId, listOf(genreId))
    }

    /** 복수 장르 조회 (쉼표로 분리) */
    suspend fun getWidgetGenres(appWidgetId: Int): List<String> {
        val preferences = context.dataStore.data.first()
        val key = stringPreferencesKey("widget_genre_$appWidgetId")
        val genreString = preferences[key] ?: "novel"
        return genreString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** 단일 장르 조회 (하위 호환성 - 첫 번째 장르 반환) */
    suspend fun getWidgetGenre(appWidgetId: Int): String {
        return getWidgetGenres(appWidgetId).firstOrNull() ?: "novel"
    }

    // ==================== 사용자 정의 장르 관리 ====================

    /** 모든 장르 조회 (기본 + 사용자 정의) */
    suspend fun getAllGenres(): List<Genre> {
        val defaultGenres = DefaultGenre.getAll()
        val customGenres = getCustomGenres().map {
            Genre(it.id, it.displayName, isBuiltIn = false)
        }
        return defaultGenres + customGenres
    }

    /** 사용자 정의 장르 목록 조회 */
    suspend fun getCustomGenres(): List<CustomGenre> {
        val preferences = context.dataStore.data.first()
        val json = preferences[CUSTOM_GENRES_KEY] ?: return emptyList()
        return try {
            kotlinx.serialization.json.Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 사용자 정의 장르 추가 (ID 규칙: 영문 소문자, 숫자, 언더스코어만) */
    suspend fun addCustomGenre(id: String, displayName: String): Boolean {
        if (id.isBlank() || displayName.isBlank()) return false
        if (DefaultGenre.isDefault(id)) return false
        if (!id.matches(Regex("^[a-z0-9_]+$"))) return false

        val currentGenres = getCustomGenres().toMutableList()
        if (currentGenres.any { it.id == id }) return false

        currentGenres.add(CustomGenre(id, displayName))

        context.dataStore.edit { preferences ->
            val json = kotlinx.serialization.json.Json.encodeToString(currentGenres)
            preferences[CUSTOM_GENRES_KEY] = json
        }

        return true
    }

    /** 사용자 정의 장르 삭제 (기본 장르는 삭제 불가) */
    suspend fun removeCustomGenre(id: String): Boolean {
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

    /** 장르 표시명 조회 (단일) */
    suspend fun getGenreDisplayName(genreId: String): String {
        DefaultGenre.fromId(genreId)?.let {
            return it.displayName
        }

        getCustomGenres().find { it.id == genreId }?.let {
            return it.displayName
        }

        return genreId
    }

    /** 장르 표시명 조회 (복수, 예: ["novel", "fantasy"] → "소설 & 판타지") */
    suspend fun getGenresDisplayName(genreIds: List<String>): String {
        if (genreIds.isEmpty()) return "선택 안함"
        if (genreIds.size == 1) return getGenreDisplayName(genreIds[0])

        val displayNames = genreIds.map { genreId ->
            getGenreDisplayName(genreId)
        }
        return displayNames.joinToString(" & ")
    }

    // ==================== 업데이트 잠금 ====================

    /** 위젯 업데이트 잠금 설정 (설정 프로세스 중 자동 업데이트 방지) */
    suspend fun setWidgetUpdateLock(appWidgetId: Int, locked: Boolean) {
        context.dataStore.edit { preferences ->
            val key = booleanPreferencesKey("$WIDGET_UPDATE_LOCK_PREFIX$appWidgetId")
            preferences[key] = locked
        }
    }

    /** 위젯 업데이트 잠금 확인 */
    suspend fun isWidgetUpdateLocked(appWidgetId: Int): Boolean {
        val preferences = context.dataStore.data.first()
        val key = booleanPreferencesKey("$WIDGET_UPDATE_LOCK_PREFIX$appWidgetId")
        return preferences[key] ?: false
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

    private fun getWidgetTapActionKey(appWidgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("widget_${appWidgetId}_tap_action")
    }

    /** 알림 설정 데이터 클래스 */
    data class NotificationConfig(
        val enabled: Boolean = false,
        val hour: Int = 0,
        val minute: Int = 0
    )

    /** 알림 설정 저장 */
    suspend fun saveNotificationConfig(config: NotificationConfig) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_ENABLED] = config.enabled
            preferences[NOTIFICATION_HOUR] = config.hour
            preferences[NOTIFICATION_MINUTE] = config.minute
        }
    }

    /** 알림 설정 조회 */
    suspend fun getNotificationConfig(): NotificationConfig {
        val preferences = context.dataStore.data.first()
        return NotificationConfig(
            enabled = preferences[NOTIFICATION_ENABLED] ?: false,
            hour = preferences[NOTIFICATION_HOUR] ?: 0,
            minute = preferences[NOTIFICATION_MINUTE] ?: 0
        )
    }

    /** 알림 설정 Flow로 관찰 */
    fun getNotificationConfigFlow(): Flow<NotificationConfig> {
        return context.dataStore.data.map { preferences ->
            NotificationConfig(
                enabled = preferences[NOTIFICATION_ENABLED] ?: false,
                hour = preferences[NOTIFICATION_HOUR] ?: 0,
                minute = preferences[NOTIFICATION_MINUTE] ?: 0
            )
        }
    }

    // ==================== 앱 버전 및 업데이트 관리 ====================

    /** 마지막으로 로드된 버전 코드 저장 */
    suspend fun saveLastLoadedVersion(versionCode: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_LOADED_VERSION] = versionCode
        }
    }

    /** 마지막으로 로드된 버전 코드 조회 */
    suspend fun getLastLoadedVersion(): Int {
        val preferences = context.dataStore.data.first()
        return preferences[LAST_LOADED_VERSION] ?: 0
    }

    /** 업데이트 가능 여부 및 새 문장 개수 저장 */
    suspend fun setUpdateAvailable(hasUpdate: Boolean, newCount: Int = 0) {
        context.dataStore.edit { preferences ->
            preferences[HAS_UPDATE_AVAILABLE] = hasUpdate
            preferences[NEW_SENTENCE_COUNT] = newCount
        }
    }

    /** 업데이트 가능 여부 조회 */
    suspend fun hasUpdateAvailable(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[HAS_UPDATE_AVAILABLE] ?: false
    }

    /** 새 문장 개수 조회 */
    suspend fun getNewSentenceCount(): Int {
        val preferences = context.dataStore.data.first()
        return preferences[NEW_SENTENCE_COUNT] ?: 0
    }

    /** 업데이트 가능 여부 Flow로 관찰 */
    fun getUpdateAvailableFlow(): Flow<Pair<Boolean, Int>> {
        return context.dataStore.data.map { preferences ->
            val hasUpdate = preferences[HAS_UPDATE_AVAILABLE] ?: false
            val newCount = preferences[NEW_SENTENCE_COUNT] ?: 0
            hasUpdate to newCount
        }
    }
}

/** Context 확장 함수 */
fun Context.getWidgetDataStore(): DataStoreManager {
    return DataStoreManager(this)
}