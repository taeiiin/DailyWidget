package com.example.dailywidget.data.backup

import android.content.Context
import android.net.Uri
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.db.entity.DailySentenceEntity
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
        val essayCount: Int,
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
        REPLACE,    // 덮어쓰기
        SKIP,       // 건너뛰기
        ADD_ALL     // 모두 추가
    }

    /**
     * 백업 전 정보 조회
     */
    suspend fun getBackupInfo(): BackupInfo = withContext(Dispatchers.IO) {
        val dao = database.dailySentenceDao()
        val allSentences = dao.getAllSentencesList()

        val totalCount = allSentences.size
        val novelCount = allSentences.count { it.genre.equals("novel", ignoreCase = true) }
        val fantasyCount = allSentences.count { it.genre.equals("fantasy", ignoreCase = true) }
        val essayCount = allSentences.count { it.genre.equals("essay", ignoreCase = true) }

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
            essayCount = essayCount,
            dateRange = dateRange
        )
    }

    /**
     * JSON으로 내보내기 (날짜 포함 파일명)
     */
    suspend fun exportToJson(uri: Uri) = withContext(Dispatchers.IO) {
        val dao = database.dailySentenceDao()
        val sentences = dao.getAllSentencesList()

        val jsonArray = JSONArray()
        sentences.forEach { sentence ->
            val jsonObject = JSONObject().apply {
                put("date", sentence.date)
                put("genre", sentence.genre)
                put("text", sentence.text)
                put("source", sentence.source ?: "")
                put("writer", sentence.writer ?: "")
                put("extra", sentence.extra ?: "")
                // ⭐ styleId, backgroundId 제거됨
            }
            jsonArray.put(jsonObject)
        }

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonArray.toString(2).toByteArray())
        }
    }

    /**
     * 복원 미리보기 (파일 분석)
     */
    suspend fun getRestorePreview(uri: Uri): RestorePreview = withContext(Dispatchers.IO) {
        val sentences = parseJsonFromUri(uri)
        val dao = database.dailySentenceDao()
        val existingSentences = dao.getAllSentencesList()

        // 중복 체크 (같은 날짜 + 장르 + 텍스트)
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
     * JSON에서 가져오기 (중복 처리 옵션)
     */
    suspend fun importFromJson(
        uri: Uri,
        duplicateHandling: DuplicateHandling = DuplicateHandling.SKIP
    ) = withContext(Dispatchers.IO) {
        val sentences = parseJsonFromUri(uri)
        val dao = database.dailySentenceDao()
        val existingSentences = dao.getAllSentencesList()

        when (duplicateHandling) {
            DuplicateHandling.REPLACE -> {
                // 덮어쓰기: 중복이면 삭제 후 추가
                sentences.forEach { newSentence ->
                    existingSentences.find { existing ->
                        existing.date == newSentence.date &&
                                existing.genre.equals(newSentence.genre, ignoreCase = true)
                    }?.let { duplicate ->
                        dao.deleteSentence(duplicate)
                    }
                    dao.insertSentence(newSentence)
                }
            }

            DuplicateHandling.SKIP -> {
                // 건너뛰기: 중복 제외하고 추가
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
                // 모두 추가: 중복 상관없이 전부 추가
                dao.insertSentences(sentences)
            }
        }
    }

    /**
     * URI에서 JSON 파싱
     */
    private fun parseJsonFromUri(uri: Uri): List<DailySentenceEntity> {
        val sentences = mutableListOf<DailySentenceEntity>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val sentence = DailySentenceEntity(
                    date = jsonObject.getString("date"),
                    genre = jsonObject.getString("genre"),
                    text = jsonObject.getString("text"),
                    source = jsonObject.optString("source").takeIf { it.isNotEmpty() },
                    writer = jsonObject.optString("writer").takeIf { it.isNotEmpty() },
                    extra = jsonObject.optString("extra").takeIf { it.isNotEmpty() }
                    // ⭐ styleId, backgroundId 제거됨
                )
                sentences.add(sentence)
            }
        }

        return sentences
    }

    companion object {
        /**
         * 날짜 포함 백업 파일명 생성
         */
        fun generateBackupFileName(): String {
            val dateFormat = SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            return "backup_$timestamp.json"
        }
    }
}