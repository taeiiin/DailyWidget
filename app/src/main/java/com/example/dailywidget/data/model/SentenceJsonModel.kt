package com.example.dailywidget.data.model

import com.google.gson.annotations.SerializedName

/**
 * JSON 파일의 문장 데이터 모델
 * 스타일/배경은 설정에서 전역 관리
 */
data class SentenceJsonModel(
    @SerializedName("date")
    val date: String,           // 필수: MMDD 형식

    @SerializedName("text")
    val text: String,           // 필수: 문장 내용

    @SerializedName("source")
    val source: String?,        // 선택: 출처 (null 가능)

    @SerializedName("writer")
    val writer: String?,        // 선택: 작가 (null 가능)

    @SerializedName("extra")
    val extra: String?,         // 선택: 특이사항 (null 가능)

    @SerializedName("genre")
    val genre: String?          // 선택: 장르 (null 가능)
)