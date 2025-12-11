package com.example.dailywidget.data.repository

import com.example.dailywidget.data.db.dao.DailySentenceDao
import com.example.dailywidget.data.db.entity.DailySentenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * 문장 데이터 Repository
 * DAO와 UI 레이어 사이의 중간 계층
 */
class DailySentenceRepository(private val dao: DailySentenceDao) {

    // ==================== 조회 ====================

    suspend fun getSentences(date: String): List<DailySentenceEntity> {
        return dao.getSentencesByDate(date)
    }

    fun getAllSentencesFlow(): Flow<List<DailySentenceEntity>> {
        return dao.getAllSentences()
    }

    suspend fun getAllSentencesList(): List<DailySentenceEntity> {
        return dao.getAllSentencesList()
    }

    suspend fun getAll(): List<DailySentenceEntity> {
        return dao.getAllSentencesList()
    }

    suspend fun getSentenceById(id: Int): DailySentenceEntity? {
        return dao.getSentenceById(id)
    }

    suspend fun getSentencesByGenre(genre: String): List<DailySentenceEntity> {
        return dao.getSentencesByGenre(genre)
    }

    // ==================== 검색 ====================

    suspend fun searchSentences(query: String): List<DailySentenceEntity> {
        return dao.searchSentences(query)
    }

    suspend fun searchAll(query: String): List<DailySentenceEntity> {
        return dao.searchSentences(query)
    }

    suspend fun searchByText(query: String): List<DailySentenceEntity> {
        return dao.searchByText(query)
    }

    suspend fun searchBySource(query: String): List<DailySentenceEntity> {
        return dao.searchBySource(query)
    }

    suspend fun searchByWriter(query: String): List<DailySentenceEntity> {
        return dao.searchByWriter(query)
    }

    suspend fun searchByDate(query: String): List<DailySentenceEntity> {
        return dao.searchByDate(query)
    }

    suspend fun searchSentencesByGenre(genre: String, query: String): List<DailySentenceEntity> {
        return dao.searchSentencesByGenre(genre, query)
    }

    // ==================== 추가 ====================

    suspend fun insertSentence(sentence: DailySentenceEntity): Long {
        return dao.insertSentence(sentence)
    }

    suspend fun insert(sentence: DailySentenceEntity): Long {
        return dao.insertSentence(sentence)
    }

    suspend fun insertSentences(sentences: List<DailySentenceEntity>) {
        dao.insertSentences(sentences)
    }

    // ==================== 수정 ====================

    suspend fun updateSentence(sentence: DailySentenceEntity) {
        dao.updateSentence(sentence)
    }

    suspend fun update(sentence: DailySentenceEntity) {
        dao.updateSentence(sentence)
    }

    // ==================== 삭제 ====================

    suspend fun deleteSentence(sentence: DailySentenceEntity) {
        dao.deleteSentence(sentence)
    }

    suspend fun delete(sentence: DailySentenceEntity) {
        dao.deleteSentence(sentence)
    }

    suspend fun deleteAllSentences() {
        dao.deleteAllSentences()
    }

    // ==================== 통계 ====================

    suspend fun getSentenceCount(): Int {
        return dao.getSentenceCount()
    }

    suspend fun getSentenceCountByGenre(genre: String): Int {
        return dao.getSentenceCountByGenre(genre)
    }
}