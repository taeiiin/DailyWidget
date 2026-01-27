package com.example.dailywidget.util

import com.example.dailywidget.data.db.entity.DailySentenceEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * 문장 파일 파서
 * JSON/CSV 파일을 DailySentenceEntity 리스트로 변환
 */
object SentenceFileParser {

    @Serializable
    data class SentenceJsonItem(
        val date: String,
        val text: String,
        val source: String? = null,
        val writer: String? = null,
        val extra: String? = null,
        val genre: String
    )

    /**
     * JSON 파일 파싱
     */
    fun parseJson(jsonContent: String): Result<List<SentenceJsonItem>> {
        return try {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            val items = json.decodeFromString<List<SentenceJsonItem>>(jsonContent)
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(Exception("JSON 파싱 실패: ${e.message}"))
        }
    }

    /**
     * CSV 파일 파싱
     * 형식: date,text,source,writer,extra,genre
     */
    fun parseCsv(csvContent: String): Result<List<SentenceJsonItem>> {
        return try {
            val lines = csvContent.lines().filter { it.isNotBlank() }

            if (lines.isEmpty()) {
                return Result.failure(Exception("빈 파일입니다"))
            }

            // 첫 줄이 헤더인지 확인 (date,text,... 로 시작)
            val startIndex = if (lines[0].startsWith("date", ignoreCase = true)) 1 else 0

            val items = lines.drop(startIndex).mapNotNull { line ->
                parseCsvLine(line)
            }

            if (items.isEmpty()) {
                Result.failure(Exception("유효한 데이터가 없습니다"))
            } else {
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(Exception("CSV 파싱 실패: ${e.message}"))
        }
    }

    /**
     * CSV 한 줄 파싱
     */
    private fun parseCsvLine(line: String): SentenceJsonItem? {
        return try {
            val parts = line.split(",").map { it.trim().trim('"') }

            if (parts.size < 6) return null

            SentenceJsonItem(
                date = parts[0],
                text = parts[1],
                source = parts[2].takeIf { it.isNotBlank() },
                writer = parts[3].takeIf { it.isNotBlank() },
                extra = parts[4].takeIf { it.isNotBlank() },
                genre = parts[5]
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * SentenceJsonItem을 DailySentenceEntity로 변환
     * @param overrideGenre null이 아니면 genre 필드를 덮어씀 (사용자 지정 장르용)
     */
    fun toEntities(
        items: List<SentenceJsonItem>,
        overrideGenre: String? = null
    ): List<DailySentenceEntity> {
        return items.map { item ->
            DailySentenceEntity(
                id = 0, // Auto-generate
                date = item.date,
                text = item.text,
                source = item.source,
                writer = item.writer,
                extra = item.extra,
                genre = overrideGenre ?: item.genre
            )
        }
    }
}