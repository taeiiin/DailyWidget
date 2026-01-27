package com.example.dailywidget.util

/**
 * 문장 파일 검증
 * 상세한 오류 메시지 생성
 */
object SentenceFileValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val totalCount: Int,
        val validCount: Int,
        val warnings: List<ValidationWarning>,
        val errors: List<ValidationError>,
        val dateRange: Pair<String, String>? // (최소 날짜, 최대 날짜)
    )

    data class ValidationWarning(
        val lineNumber: Int?,
        val message: String
    )

    data class ValidationError(
        val lineNumber: Int?,
        val message: String
    )

    /**
     * 문장 리스트 검증
     */
    fun validate(items: List<SentenceFileParser.SentenceJsonItem>): ValidationResult {
        val warnings = mutableListOf<ValidationWarning>()
        val errors = mutableListOf<ValidationError>()
        var validCount = 0

        items.forEachIndexed { index, item ->
            val lineNumber = index + 1

            // 날짜 검증
            val dateValidation = validateDate(item.date)
            when {
                dateValidation is DateValidation.Invalid -> {
                    errors.add(ValidationError(lineNumber, "유효하지 않은 날짜: ${item.date}"))
                }
                dateValidation is DateValidation.Warning -> {
                    warnings.add(ValidationWarning(lineNumber, dateValidation.message))
                }
            }

            // 텍스트 검증
            if (item.text.isBlank()) {
                errors.add(ValidationError(lineNumber, "문장 내용이 비어있습니다"))
            } else if (item.text.length > 500) {
                warnings.add(ValidationWarning(lineNumber, "문장이 너무 깁니다 (${item.text.length}자)"))
            }

            // 장르 검증
            if (item.genre.isBlank()) {
                errors.add(ValidationError(lineNumber, "장르가 비어있습니다"))
            } else if (!item.genre.matches(Regex("^[a-z0-9_]+$"))) {
                warnings.add(ValidationWarning(lineNumber, "장르 ID는 영문 소문자, 숫자, 언더스코어만 가능합니다: ${item.genre}"))
            }

            // 선택 필드 경고
            if (item.source.isNullOrBlank()) {
                warnings.add(ValidationWarning(lineNumber, "출처가 비어있습니다 (선택사항)"))
            }
            if (item.writer.isNullOrBlank()) {
                warnings.add(ValidationWarning(lineNumber, "작가가 비어있습니다 (선택사항)"))
            }

            // 유효성 카운트
            if (dateValidation !is DateValidation.Invalid && item.text.isNotBlank() && item.genre.isNotBlank()) {
                validCount++
            }
        }

        // 날짜 범위 계산
        val dateRange = try {
            val validDates = items.mapNotNull {
                if (validateDate(it.date) !is DateValidation.Invalid) it.date else null
            }.sorted()

            if (validDates.isNotEmpty()) {
                Pair(formatDateDisplay(validDates.first()), formatDateDisplay(validDates.last()))
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        return ValidationResult(
            isValid = errors.isEmpty() && validCount > 0,
            totalCount = items.size,
            validCount = validCount,
            warnings = warnings,
            errors = errors,
            dateRange = dateRange
        )
    }

    /**
     * 날짜 검증
     */
    private fun validateDate(date: String): DateValidation {
        if (date.length != 4) {
            return DateValidation.Invalid("날짜는 4자리여야 합니다 (MMDD)")
        }

        val month = date.substring(0, 2).toIntOrNull() ?: return DateValidation.Invalid("월 형식 오류")
        val day = date.substring(2, 4).toIntOrNull() ?: return DateValidation.Invalid("일 형식 오류")

        if (month !in 1..12) {
            return DateValidation.Invalid("월은 01~12 사이여야 합니다")
        }

        val maxDay = when (month) {
            2 -> 29
            4, 6, 9, 11 -> 30
            else -> 31
        }

        if (day !in 1..maxDay) {
            return DateValidation.Invalid("${month}월은 1~${maxDay}일까지만 가능합니다")
        }

        // 2월 30일 같은 경고
        if (month == 2 && day > 28) {
            return DateValidation.Warning("2월 ${day}일은 윤년에만 존재합니다")
        }

        return DateValidation.Valid
    }

    private sealed class DateValidation {
        object Valid : DateValidation()
        data class Warning(val message: String) : DateValidation()
        data class Invalid(val message: String) : DateValidation()
    }

    /**
     * 날짜 표시 형식 변환 (MMDD → M월 D일)
     */
    private fun formatDateDisplay(date: String): String {
        return try {
            val month = date.substring(0, 2).toInt()
            val day = date.substring(2, 4).toInt()
            "${month}월 ${day}일"
        } catch (e: Exception) {
            date
        }
    }
}