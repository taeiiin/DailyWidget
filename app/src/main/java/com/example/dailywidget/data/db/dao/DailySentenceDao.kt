package com.example.dailywidget.data.db.dao

import androidx.room.*
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySentenceDao {

    // ==================== 조회 ====================

    @Query("SELECT * FROM daily_sentences ORDER BY date ASC")
    fun getAllSentences(): Flow<List<DailySentenceEntity>>

    @Query("SELECT * FROM daily_sentences ORDER BY date ASC")
    suspend fun getAllSentencesList(): List<DailySentenceEntity>

    @Query("SELECT * FROM daily_sentences WHERE date = :date ORDER BY id ASC")
    suspend fun getSentencesByDate(date: String): List<DailySentenceEntity>

    @Query("SELECT * FROM daily_sentences WHERE id = :id")
    suspend fun getSentenceById(id: Int): DailySentenceEntity?

    @Query("SELECT * FROM daily_sentences WHERE genre = :genre ORDER BY date ASC")
    suspend fun getSentencesByGenre(genre: String): List<DailySentenceEntity>

    // ==================== 검색 (전체) ====================

    @Query("""
    SELECT * FROM daily_sentences
    WHERE (text LIKE '%' || :query || '%' COLLATE NOCASE
        OR source LIKE '%' || :query || '%' COLLATE NOCASE
        OR writer LIKE '%' || :query || '%' COLLATE NOCASE)
    ORDER BY date ASC
""")
    suspend fun searchSentences(query: String): List<DailySentenceEntity>

// ==================== 검색 (타입별) ====================

    @Query("SELECT * FROM daily_sentences WHERE text LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY date ASC")
    suspend fun searchByText(query: String): List<DailySentenceEntity>

    @Query("SELECT * FROM daily_sentences WHERE source LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY date ASC")
    suspend fun searchBySource(query: String): List<DailySentenceEntity>

    @Query("SELECT * FROM daily_sentences WHERE writer LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY date ASC")
    suspend fun searchByWriter(query: String): List<DailySentenceEntity>

    @Query("SELECT * FROM daily_sentences WHERE date LIKE '%' || :query || '%' ORDER BY date ASC")
    suspend fun searchByDate(query: String): List<DailySentenceEntity>

    @Query("SELECT * FROM daily_sentences WHERE genre = :genre AND text LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY date ASC")
    suspend fun searchSentencesByGenre(genre: String, query: String): List<DailySentenceEntity>


    // ==================== 추가 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentence(sentence: DailySentenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentences(sentences: List<DailySentenceEntity>)

    // ==================== 수정 ====================

    @Update
    suspend fun updateSentence(sentence: DailySentenceEntity)

    // ==================== 삭제 ====================

    @Delete
    suspend fun deleteSentence(sentence: DailySentenceEntity)

    @Query("DELETE FROM daily_sentences")
    suspend fun deleteAllSentences()

    // ==================== 통계 ====================

    @Query("SELECT COUNT(*) FROM daily_sentences")
    suspend fun getSentenceCount(): Int

    @Query("SELECT COUNT(*) FROM daily_sentences WHERE genre = :genre")
    suspend fun getSentenceCountByGenre(genre: String): Int
}