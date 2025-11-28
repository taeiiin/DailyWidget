package com.example.dailywidget.data.repository

import com.example.dailywidget.data.db.dao.DailySentenceDao
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * 문장 데이터 Repository
 */
class DailySentenceRepository(private val dao: DailySentenceDao) {

    // ==================== 조회 ====================

    /**
     * 특정 날짜의 문장 조회
     */
    suspend fun getSentences(date: String): List<DailySentenceEntity> {
        return dao.getSentencesByDate(date)
    }

    /**
     * 모든 문장 조회 (Flow)
     */
    fun getAllSentencesFlow(): Flow<List<DailySentenceEntity>> {
        return dao.getAllSentences()
    }

    /**
     * 모든 문장 조회 (List)
     */
    suspend fun getAllSentencesList(): List<DailySentenceEntity> {
        return dao.getAllSentencesList()
    }

    /**
     * 모든 문장 조회 (getAll alias)
     */
    suspend fun getAll(): List<DailySentenceEntity> {
        return dao.getAllSentencesList()
    }

    /**
     * 특정 ID의 문장 조회
     */
    suspend fun getSentenceById(id: Int): DailySentenceEntity? {
        return dao.getSentenceById(id)
    }

    /**
     * 장르별 문장 조회
     */
    suspend fun getSentencesByGenre(genre: String): List<DailySentenceEntity> {
        return dao.getSentencesByGenre(genre)
    }

    // ==================== 검색 ====================

    /**
     * 전체 검색 (문장, 출처, 작가)
     */
    suspend fun searchSentences(query: String): List<DailySentenceEntity> {
        return dao.searchSentences(query)
    }

    /**
     * 전체 검색 (searchAll alias)
     */
    suspend fun searchAll(query: String): List<DailySentenceEntity> {
        return dao.searchSentences(query)
    }

    /**
     * 문장 내용 검색
     */
    suspend fun searchByText(query: String): List<DailySentenceEntity> {
        return dao.searchByText(query)
    }

    /**
     * 출처 검색
     */
    suspend fun searchBySource(query: String): List<DailySentenceEntity> {
        return dao.searchBySource(query)
    }

    /**
     * 작가 검색
     */
    suspend fun searchByWriter(query: String): List<DailySentenceEntity> {
        return dao.searchByWriter(query)
    }

    /**
     * 날짜 검색
     */
    suspend fun searchByDate(query: String): List<DailySentenceEntity> {
        return dao.searchByDate(query)
    }

    /**
     * 장르별 문장 검색
     */
    suspend fun searchSentencesByGenre(genre: String, query: String): List<DailySentenceEntity> {
        return dao.searchSentencesByGenre(genre, query)
    }

    // ==================== 추가 ====================

    /**
     * 문장 추가
     */
    suspend fun insertSentence(sentence: DailySentenceEntity): Long {
        return dao.insertSentence(sentence)
    }

    /**
     * 문장 추가 (insert alias)
     */
    suspend fun insert(sentence: DailySentenceEntity): Long {
        return dao.insertSentence(sentence)
    }

    /**
     * 여러 문장 추가
     */
    suspend fun insertSentences(sentences: List<DailySentenceEntity>) {
        dao.insertSentences(sentences)
    }

    // ==================== 수정 ====================

    /**
     * 문장 수정
     */
    suspend fun updateSentence(sentence: DailySentenceEntity) {
        dao.updateSentence(sentence)
    }

    /**
     * 문장 수정 (update alias)
     */
    suspend fun update(sentence: DailySentenceEntity) {
        dao.updateSentence(sentence)
    }

    // ==================== 삭제 ====================

    /**
     * 문장 삭제
     */
    suspend fun deleteSentence(sentence: DailySentenceEntity) {
        dao.deleteSentence(sentence)
    }

    /**
     * 문장 삭제 (delete alias)
     */
    suspend fun delete(sentence: DailySentenceEntity) {
        dao.deleteSentence(sentence)
    }

    /**
     * 모든 문장 삭제
     */
    suspend fun deleteAllSentences() {
        dao.deleteAllSentences()
    }

    // ==================== 통계 ====================

    /**
     * 문장 개수 조회
     */
    suspend fun getSentenceCount(): Int {
        return dao.getSentenceCount()
    }

    /**
     * 장르별 문장 개수 조회
     */
    suspend fun getSentenceCountByGenre(genre: String): Int {
        return dao.getSentenceCountByGenre(genre)
    }
}