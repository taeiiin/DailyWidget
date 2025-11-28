package com.example.dailywidget.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_sentences")
data class DailySentenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,           // MMdd 형식 (예: "0101")
    val genre: String,          // "novel", "fantasy", "essay"
    val text: String,           // 메인 문장
    val source: String?,        // 출처 (선택)
    val writer: String?,        // 작가 (선택)
    val extra: String?          // 특이사항 (선택)
)