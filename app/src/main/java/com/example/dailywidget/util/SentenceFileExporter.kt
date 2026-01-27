package com.example.dailywidget.util

import android.content.Context
import android.net.Uri
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

/**
 * 문장 파일 내보내기
 * JSON/CSV 형식으로 내보내기
 */
object SentenceFileExporter {

    @Serializable
    data class ExportItem(
        val date: String,
        val text: String,
        val source: String?,
        val writer: String?,
        val extra: String?,
        val genre: String
    )

    /**
     * JSON 형식으로 내보내기
     */
    fun exportToJson(
        context: Context,
        uri: Uri,
        sentences: List<DailySentenceEntity>
    ): Result<Unit> {
        return try {
            val items = sentences.map { entity ->
                ExportItem(
                    date = entity.date,
                    text = entity.text,
                    source = entity.source,
                    writer = entity.writer,
                    extra = entity.extra,
                    genre = entity.genre
                )
            }

            val json = Json {
                prettyPrint = true
                encodeDefaults = true
            }
            val jsonString = json.encodeToString(items)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("JSON 내보내기 실패: ${e.message}"))
        }
    }

    /**
     * CSV 형식으로 내보내기
     */
    fun exportToCsv(
        context: Context,
        uri: Uri,
        sentences: List<DailySentenceEntity>
    ): Result<Unit> {
        return try {
            val csvBuilder = StringBuilder()

            // 헤더
            csvBuilder.appendLine("date,text,source,writer,extra,genre")

            // 데이터
            sentences.forEach { entity ->
                val line = listOf(
                    entity.date,
                    escapeCsv(entity.text),
                    escapeCsv(entity.source ?: ""),
                    escapeCsv(entity.writer ?: ""),
                    escapeCsv(entity.extra ?: ""),
                    entity.genre
                ).joinToString(",")

                csvBuilder.appendLine(line)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvBuilder.toString().toByteArray())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("CSV 내보내기 실패: ${e.message}"))
        }
    }

    /**
     * CSV 필드 이스케이프 (쉼표, 따옴표 처리)
     */
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * 내보내기 파일명 생성
     */
    fun generateExportFileName(
        genres: List<String>,
        format: ExportFormat
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val genreNames = genres.joinToString("_").take(30)
        val extension = when (format) {
            ExportFormat.JSON -> "json"
            ExportFormat.CSV -> "csv"
        }
        return "dailywidget_${genreNames}_$timestamp.$extension"
    }

    enum class ExportFormat {
        JSON, CSV
    }
}