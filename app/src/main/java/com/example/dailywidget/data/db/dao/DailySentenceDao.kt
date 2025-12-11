package com.example.dailywidget.data.db.dao

import androidx.room.*
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * 일일 문장 DAO
 * Room 데이터베이스 접근 인터페이스
 */
@Dao
interface DailySentenceDao {

    // ==================== 조회 ====================

    /**
     * 모든 문장 조회 (Flow - 실시간 업데이트)
     */
    @Query("SELECT * FROM daily_sentences ORDER BY date ASC")
    fun getAllSentences(): Flow<List<DailySentenceEntity>>

    /**
     * 모든 문장 조회 (List - 일회성 조회)
     */
    @Query("SELECT * FROM daily_sentences ORDER BY date ASC")
    suspend fun getAllSentencesList(): List<DailySentenceEntity>

    /**
     * 날짜별 문장 조회
     */
    @Query("SELECT * FROM daily_sentences WHERE date = :date ORDER BY id ASC")
    suspend fun getSentencesByDate(date: String): List<DailySentenceEntity>

    /**
     * ID로 문장 조회
     */
    @Query("SELECT * FROM daily_sentences WHERE id = :id")
    suspend fun getSentenceById(id: Int): DailySentenceEntity?

    /**
     * 장르별 문장 조회
     */
    @Query("SELECT * FROM daily_sentences WHERE genre = :genre ORDER BY date ASC")
    suspend fun getSentencesByGenre(genre: String): List<DailySentenceEntity>

    /**
     * 복수 장르 + 날짜로 랜덤 문장 조회 (위젯용)
     */
    @Query("SELECT * FROM daily_sentences WHERE date = :date AND genre IN (:genres) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomSentenceByDateAndGenres(date: String, genres: List<String>): DailySentenceEntity?

    /**
     * 복수 장르 + 날짜로 모든 문장 조회
     */
    @Query("SELECT * FROM daily_sentences WHERE date = :date AND genre IN (:genres) ORDER BY id ASC")
    suspend fun getSentencesByDateAndGenres(date: String, genres: List<String>): List<DailySentenceEntity>

    // ==================== 검색 ====================

    /**
     * 통합 검색 (텍스트 + 출처 + 작가 + 날짜)
     */
    @Query("""
        SELECT * FROM daily_sentences
        WHERE (text LIKE '%' || :query || '%' COLLATE NOCASE
            OR source LIKE '%' || :query || '%' COLLATE NOCASE
            OR writer LIKE '%' || :query || '%' COLLATE NOCASE
            OR date LIKE '%' || :query || '%' COLLATE NOCASE)
        ORDER BY date ASC
    """)
    suspend fun searchSentences(query: String): List<DailySentenceEntity>

    /**
     * 텍스트로 검색
     */
    @Query("SELECT * FROM daily_sentences WHERE text LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY date ASC")
    suspend fun searchByText(query: String): List<DailySentenceEntity>

    /**
     * 출처로 검색
     */
    @Query("SELECT * FROM daily_sentences WHERE source LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY date ASC")
    suspend fun searchBySource(query: String): List<DailySentenceEntity>

    /**
     * 작가로 검색
     */
    @Query("SELECT * FROM daily_sentences WHERE writer LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY date ASC")
    suspend fun searchByWriter(query: String): List<DailySentenceEntity>

    /**
     * 날짜로 검색
     */
    @Query("SELECT * FROM daily_sentences WHERE date LIKE '%' || :query || '%' ORDER BY date ASC")
    suspend fun searchByDate(query: String): List<DailySentenceEntity>

    /**
     * 장르별 텍스트 검색
     */
    @Query("SELECT * FROM daily_sentences WHERE genre = :genre AND text LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY date ASC")
    suspend fun searchSentencesByGenre(genre: String, query: String): List<DailySentenceEntity>

    // ==================== 추가 ====================

    /**
     * 문장 추가 (단일)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentence(sentence: DailySentenceEntity): Long

    /**
     * 문장 추가 (다중)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentences(sentences: List<DailySentenceEntity>)

    // ==================== 수정 ====================

    /**
     * 문장 수정
     */
    @Update
    suspend fun updateSentence(sentence: DailySentenceEntity)

    // ==================== 삭제 ====================

    /**
     * 문장 삭제 (단일)
     */
    @Delete
    suspend fun deleteSentence(sentence: DailySentenceEntity)

    /**
     * 모든 문장 삭제
     */
    @Query("DELETE FROM daily_sentences")
    suspend fun deleteAllSentences()

    // ==================== 통계 ====================

    /**
     * 전체 문장 개수 조회
     */
    @Query("SELECT COUNT(*) FROM daily_sentences")
    suspend fun getSentenceCount(): Int

    /**
     * 장르별 문장 개수 조회
     */
    @Query("SELECT COUNT(*) FROM daily_sentences WHERE genre = :genre")
    suspend fun getSentenceCountByGenre(genre: String): Int
}