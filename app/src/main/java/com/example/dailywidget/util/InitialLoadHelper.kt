package com.example.dailywidget.util

import android.content.Context
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * 초기 데이터 로딩 헬퍼
 * - assets/sentences.json 파일을 Room DB에 로드
 * - 최초 1회만 실행 (SharedPreferences로 추적)
 * - JSON 데이터 업데이트 기능 제공
 */
object InitialLoadHelper {

    private const val PREFS_NAME = "InitialLoadPrefs"
    private const val KEY_DATA_LOADED = "data_loaded"

    /** 데이터가 이미 로드되었는지 확인 */
    private fun isDataLoaded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DATA_LOADED, false)
    }

    /** 데이터 로드 완료 표시 */
    private fun markDataAsLoaded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DATA_LOADED, true).apply()
    }

    /**
     * 초기 데이터 로드 (최초 1회만 실행)
     * assets/sentences.json 파일을 파싱하여 DB에 저장
     */
    suspend fun loadInitialData(context: Context) = withContext(Dispatchers.IO) {
        try {
            if (isDataLoaded(context)) {
                android.util.Log.d("InitialLoadHelper", "Data already loaded, skipping")
                return@withContext
            }

            android.util.Log.d("InitialLoadHelper", "Starting initial data load...")

            val db = AppDatabase.getDatabase(context)
            val dao = db.dailySentenceDao()

            // JSON 파일 읽기
            val jsonString = context.assets.open("sentences.json").bufferedReader().use {
                it.readText()
            }

            android.util.Log.d("InitialLoadHelper", "JSON file loaded, length: ${jsonString.length}")

            // JSON 파싱
            val jsonArray = JSONArray(jsonString)
            val sentences = mutableListOf<DailySentenceEntity>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val sentence = DailySentenceEntity(
                    date = jsonObject.getString("date"),
                    genre = jsonObject.getString("genre"),
                    text = jsonObject.getString("text"),
                    source = jsonObject.optString("source").takeIf { it.isNotEmpty() },
                    writer = jsonObject.optString("writer").takeIf { it.isNotEmpty() },
                    extra = jsonObject.optString("extra").takeIf { it.isNotEmpty() }
                )
                sentences.add(sentence)
            }

            android.util.Log.d("InitialLoadHelper", "Parsed ${sentences.size} sentences")

            // 데이터베이스에 삽입
            dao.insertSentences(sentences)

            android.util.Log.d("InitialLoadHelper", "Inserted ${sentences.size} sentences to database")

            // 로드 완료 표시
            markDataAsLoaded(context)

            android.util.Log.d("InitialLoadHelper", "Initial data load completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("InitialLoadHelper", "Error loading initial data", e)
            e.printStackTrace()
        }
    }

    /** 데이터 로딩 플래그 리셋 (재로딩용) */
    fun resetDataLoadFlag(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("initial_data_loaded", false).apply()
    }

    /**
     * JSON 데이터만 업데이트 (사용자 추가 데이터는 유지)
     * 휴리스틱: id < 10000인 데이터를 JSON 데이터로 간주
     */
    suspend fun updateJsonData(context: Context): Int {
        try {
            val database = AppDatabase.getDatabase(context)
            val dao = database.dailySentenceDao()

            // JSON 파일 읽기
            val json = context.assets.open("sentences.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)

            val jsonSentences = mutableListOf<DailySentenceEntity>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val sentence = DailySentenceEntity(
                    id = 0,
                    date = obj.getString("date"),
                    genre = obj.getString("genre"),
                    text = obj.getString("text"),
                    source = obj.optString("source").takeIf { it.isNotEmpty() },
                    writer = obj.optString("writer").takeIf { it.isNotEmpty() },
                    extra = obj.optString("extra").takeIf { it.isNotEmpty() }
                )

                jsonSentences.add(sentence)
            }

            // 기존 JSON 데이터 찾기 (id < 10000)
            val allData = dao.getAllSentencesList()
            val existingJsonData = allData.filter { it.id < 10000 }

            // 새로 추가: 중복 체크 (날짜 + 장르 + 텍스트)
            val existingKeys = existingJsonData.map {
                "${it.date}|${it.genre}|${it.text}"
            }.toSet()

            val newSentences = jsonSentences.filter {
                "${it.date}|${it.genre}|${it.text}" !in existingKeys
            }

            // JSON 데이터만 삭제
            existingJsonData.forEach { dao.deleteSentence(it) }

            // 새 JSON 데이터 삽입
            dao.insertSentences(jsonSentences)

            android.util.Log.d("InitialLoadHelper", "JSON 데이터 업데이트 완료: 전체 ${jsonSentences.size}개, 신규 ${newSentences.size}개")

            return newSentences.size  // 새 문장 개수 반환

        } catch (e: Exception) {
            android.util.Log.e("InitialLoadHelper", "JSON 데이터 업데이트 실패", e)
            throw e
        }
    }
}

/**
 * 새 버전의 문장 개수 계산 (실제 업데이트 없이 개수만 확인)
 */
suspend fun calculateNewSentenceCount(context: Context): Int {
    try {
        val database = AppDatabase.getDatabase(context)
        val dao = database.dailySentenceDao()

        // JSON 파일 읽기
        val json = context.assets.open("sentences.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(json)

        val jsonSentences = mutableListOf<DailySentenceEntity>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val sentence = DailySentenceEntity(
                id = 0,
                date = obj.getString("date"),
                genre = obj.getString("genre"),
                text = obj.getString("text"),
                source = obj.optString("source").takeIf { it.isNotEmpty() },
                writer = obj.optString("writer").takeIf { it.isNotEmpty() },
                extra = obj.optString("extra").takeIf { it.isNotEmpty() }
            )

            jsonSentences.add(sentence)
        }

        // 기존 데이터
        val allData = dao.getAllSentencesList()
        val existingJsonData = allData.filter { it.id < 10000 }

        val existingKeys = existingJsonData.map {
            "${it.date}|${it.genre}|${it.text}"
        }.toSet()

        // 새 문장만 필터링
        val newSentences = jsonSentences.filter {
            "${it.date}|${it.genre}|${it.text}" !in existingKeys
        }

        return newSentences.size

    } catch (e: Exception) {
        android.util.Log.e("InitialLoadHelper", "새 문장 개수 계산 실패", e)
        return 0
    }
}