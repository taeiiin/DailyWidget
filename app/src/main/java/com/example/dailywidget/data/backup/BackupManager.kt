package com.example.dailywidget.data.backup

import android.content.Context
import android.net.Uri
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import com.example.dailywidget.data.repository.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * 백업/복원 관리자
 * - JSON 형식의 백업 파일 생성 및 복원
 * - 사용자 정의 장르 포함
 * - 중복 처리 옵션 제공
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    /**
     * 백업 정보 데이터 클래스
     */
    data class BackupInfo(
        val totalCount: Int,
        val novelCount: Int,
        val fantasyCount: Int,
        val poemCount: Int,
        val dateRange: String
    )

    /**
     * 복원 미리보기 데이터 클래스
     */
    data class RestorePreview(
        val totalCount: Int,
        val newCount: Int,
        val duplicateCount: Int,
        val sentences: List<DailySentenceEntity>
    )

    /**
     * 중복 처리 옵션
     */
    enum class DuplicateHandling {
        REPLACE,    // 덮어쓰기: 중복 삭제 후 새 문장 추가
        SKIP,       // 건너뛰기: 중복은 무시하고 신규만 추가
        ADD_ALL     // 모두 추가: 중복 상관없이 전부 추가
    }

    /**
     * 백업 전 정보 조회
     * 전체 문장 개수, 장르별 개수, 날짜 범위 반환
     */
    suspend fun getBackupInfo(): BackupInfo = withContext(Dispatchers.IO) {
        val dao = database.dailySentenceDao()
        val allSentences = dao.getAllSentencesList()

        val totalCount = allSentences.size
        val novelCount = allSentences.count { it.genre.equals("novel", ignoreCase = true) }
        val fantasyCount = allSentences.count { it.genre.equals("fantasy", ignoreCase = true) }
        val poemCount = allSentences.count { it.genre.equals("poem", ignoreCase = true) }

        val dateRange = if (allSentences.isNotEmpty()) {
            val sortedDates = allSentences.map { it.date }.sorted()
            "${sortedDates.first()} ~ ${sortedDates.last()}"
        } else {
            "없음"
        }

        BackupInfo(
            totalCount = totalCount,
            novelCount = novelCount,
            fantasyCount = fantasyCount,
            poemCount = poemCount,
            dateRange = dateRange
        )
    }

    /**
     * JSON 형식으로 백업 파일 내보내기
     * 버전 2 형식: 사용자 정의 장르 + 문장 데이터
     */
    suspend fun exportToJson(uri: Uri) = withContext(Dispatchers.IO) {
        val dao = database.dailySentenceDao()
        val sentences = dao.getAllSentencesList()

        val dataStoreManager = DataStoreManager(context)
        val customGenres = dataStoreManager.getCustomGenres()

        // 루트 JSON 객체 생성 (버전 2)
        val rootObject = JSONObject().apply {
            put("version", 2)
            put("exportDate", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))

            // 사용자 정의 장르
            val genresArray = JSONArray()
            customGenres.forEach { genre ->
                val genreObject = JSONObject().apply {
                    put("id", genre.id)
                    put("displayName", genre.displayName)
                }
                genresArray.put(genreObject)
            }
            put("customGenres", genresArray)

            // 문장 데이터
            val sentencesArray = JSONArray()
            sentences.forEach { sentence ->
                val jsonObject = JSONObject().apply {
                    put("date", sentence.date)
                    put("genre", sentence.genre)
                    put("text", sentence.text)
                    put("source", sentence.source ?: "")
                    put("writer", sentence.writer ?: "")
                    put("extra", sentence.extra ?: "")
                }
                sentencesArray.put(jsonObject)
            }
            put("sentences", sentencesArray)
        }

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(rootObject.toString(2).toByteArray())
        }
    }

    /**
     * 복원 미리보기
     * 파일 분석 후 총 개수, 신규 개수, 중복 개수 반환
     */
    suspend fun getRestorePreview(uri: Uri): RestorePreview = withContext(Dispatchers.IO) {
        val sentences = parseJsonFromUri(uri)
        val dao = database.dailySentenceDao()
        val existingSentences = dao.getAllSentencesList()

        // 중복 체크: 날짜 + 장르 + 텍스트 모두 일치
        val duplicates = sentences.filter { newSentence ->
            existingSentences.any { existing ->
                existing.date == newSentence.date &&
                        existing.genre.equals(newSentence.genre, ignoreCase = true) &&
                        existing.text == newSentence.text
            }
        }

        RestorePreview(
            totalCount = sentences.size,
            newCount = sentences.size - duplicates.size,
            duplicateCount = duplicates.size,
            sentences = sentences
        )
    }

    /**
     * JSON 백업 파일에서 복원
     * @param uri 백업 파일 URI
     * @param duplicateHandling 중복 처리 방식
     */
    suspend fun importFromJson(
        uri: Uri,
        duplicateHandling: DuplicateHandling = DuplicateHandling.SKIP
    ) = withContext(Dispatchers.IO) {
        // 1단계: 사용자 정의 장르 복원
        restoreCustomGenres(uri)

        // 2단계: 문장 복원
        val sentences = parseJsonFromUri(uri)
        val dao = database.dailySentenceDao()
        val existingSentences = dao.getAllSentencesList()

        when (duplicateHandling) {
            DuplicateHandling.REPLACE -> {
                // 중복된 문장 삭제 후 새 문장 추가
                sentences.forEach { newSentence ->
                    existingSentences.find { existing ->
                        existing.date == newSentence.date &&
                                existing.genre.equals(newSentence.genre, ignoreCase = true) &&
                                existing.text == newSentence.text
                    }?.let { duplicate ->
                        dao.deleteSentence(duplicate)
                    }
                    dao.insertSentence(newSentence)
                }
            }

            DuplicateHandling.SKIP -> {
                // 중복 제외하고 신규만 추가
                val newSentences = sentences.filter { newSentence ->
                    existingSentences.none { existing ->
                        existing.date == newSentence.date &&
                                existing.genre.equals(newSentence.genre, ignoreCase = true) &&
                                existing.text == newSentence.text
                    }
                }
                dao.insertSentences(newSentences)
            }

            DuplicateHandling.ADD_ALL -> {
                // 모두 추가: 중복 체크 없이 전부 추가
                dao.insertSentences(sentences)
            }
        }
    }

    /**
     * URI에서 JSON 파싱하여 문장 리스트 반환
     * 버전 1(배열) 및 버전 2(객체) 모두 지원
     */
    private fun parseJsonFromUri(uri: Uri): List<DailySentenceEntity> {
        val sentences = mutableListOf<DailySentenceEntity>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()

            // 버전 체크: 루트가 배열이면 v1, 객체면 v2
            val isV2 = jsonString.trim().startsWith("{")

            val jsonArray = if (isV2) {
                // v2: {"version": 2, "customGenres": [...], "sentences": [...]}
                val rootObject = JSONObject(jsonString)
                rootObject.getJSONArray("sentences")
            } else {
                // v1: [...] (배열 직접)
                JSONArray(jsonString)
            }

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
        }

        return sentences
    }

    /**
     * 사용자 정의 장르 복원
     * 버전 2 백업 파일에서 customGenres 추출하여 DataStore에 저장
     */
    private suspend fun restoreCustomGenres(uri: Uri) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()

            // 버전 체크
            val isV2 = jsonString.trim().startsWith("{")

            if (isV2) {
                val rootObject = JSONObject(jsonString)

                // customGenres 필드 확인
                if (rootObject.has("customGenres")) {
                    val genresArray = rootObject.getJSONArray("customGenres")
                    val dataStoreManager = DataStoreManager(context)

                    for (i in 0 until genresArray.length()) {
                        val genreObject = genresArray.getJSONObject(i)
                        val id = genreObject.getString("id")
                        val displayName = genreObject.getString("displayName")

                        // 장르 추가 (중복이면 무시됨)
                        dataStoreManager.addCustomGenre(id, displayName)
                    }
                }
            }
            // v1 백업에는 customGenres 없음 (무시)
        }
    }

    companion object {
        /**
         * 타임스탬프 포함 백업 파일명 생성
         * 형식: backup_YYYY_MM_DD_HHmmss.json
         */
        fun generateBackupFileName(): String {
            val dateFormat = SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            return "backup_$timestamp.json"
        }
    }
}