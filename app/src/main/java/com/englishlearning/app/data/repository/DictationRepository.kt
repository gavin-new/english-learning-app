package com.englishlearning.app.data.repository

import com.englishlearning.app.data.local.AppDatabase
import com.englishlearning.app.data.model.DictationRecord
import com.englishlearning.app.data.model.DictationSession
import com.englishlearning.app.data.model.VocabularyBookItem
import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 听写仓库 + 生词本管理
 */
class DictationRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dictationDao = db.dictationDao()
    private val vocabularyBookDao = db.vocabularyBookDao()

    // ==================== 听写记录 ====================

    suspend fun insertRecord(record: DictationRecord): Long =
        dictationDao.insertRecord(record)

    suspend fun insertRecords(records: List<DictationRecord>) =
        dictationDao.insertRecords(records)

    fun getRecordsBySession(sessionId: String): Flow<List<DictationRecord>> =
        dictationDao.getRecordsBySession(sessionId)

    fun getRecordsByWord(wordId: Long): Flow<List<DictationRecord>> =
        dictationDao.getRecordsByWord(wordId)

    suspend fun getIncorrectRecords(sessionId: String): List<DictationRecord> =
        dictationDao.getIncorrectRecords(sessionId)

    suspend fun getCorrectCount(sessionId: String): Int =
        dictationDao.getCorrectCount(sessionId)

    suspend fun getTotalCount(sessionId: String): Int =
        dictationDao.getTotalCount(sessionId)

    // ==================== 听写会话 ====================

    suspend fun insertSession(session: DictationSession) =
        dictationDao.insertSession(session)

    suspend fun updateSession(session: DictationSession) =
        dictationDao.updateSession(session)

    fun getAllSessions(): Flow<List<DictationSession>> =
        dictationDao.getAllSessions()

    suspend fun getRecentSessions(limit: Int = 10): List<DictationSession> =
        dictationDao.getRecentSessions(limit)

    fun getTodaySessions(): Flow<List<DictationSession>> {
        val startOfDay = getStartOfDay()
        return dictationDao.getTodaySessions(startOfDay)
    }

    /** 创建新的听写会话 */
    suspend fun startNewSession(
        totalWords: Int,
        category: String? = null
    ): DictationSession {
        val session = DictationSession(
            id = UUID.randomUUID().toString(),
            totalWords = totalWords,
            category = category
        )
        dictationDao.insertSession(session)
        return session
    }

    /** 完成听写会话 */
    suspend fun completeSession(sessionId: String) {
        val session = dictationDao.getSessionById(sessionId) ?: return
        val correctCount = dictationDao.getCorrectCount(sessionId)
        dictationDao.updateSession(
            session.copy(
                isCompleted = true,
                endTime = System.currentTimeMillis(),
                correctCount = correctCount
            )
        )
    }

    // ==================== 听写统计 ====================

    suspend fun getOverallStats() = dictationDao.getOverallStats()

    suspend fun getWordStats(wordId: Long) = dictationDao.getWordStats(wordId)

    suspend fun getMostErrorProneWords(limit: Int = 20) =
        dictationDao.getMostErrorProneWords(limit)

    // ==================== 生词本管理 ====================

    fun getVocabularyBook(): Flow<List<VocabularyBookItem>> =
        vocabularyBookDao.getAllVocabulary()

    fun getWordsForReview(): Flow<List<VocabularyBookItem>> =
        vocabularyBookDao.getWordsForReview()

    fun getReviewCount(): Flow<Int> =
        vocabularyBookDao.getReviewCount()

    fun getTodayWords(): Flow<List<VocabularyBookItem>> =
        vocabularyBookDao.getTodayWords(getStartOfDay())

    suspend fun addToVocabularyBook(
        wordId: Long,
        word: String,
        meaning: String,
        category: String = "通用",
        phonetic: String? = null
    ): Long {
        // 检查是否已存在
        if (vocabularyBookDao.isInVocabulary(wordId)) {
            return vocabularyBookDao.getByWordId(wordId)?.id ?: -1
        }
        val item = VocabularyBookItem(
            wordId = wordId,
            word = word,
            meaning = meaning,
            category = category,
            phonetic = phonetic,
            nextReviewTime = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000L
        )
        return vocabularyBookDao.insert(item)
    }

    suspend fun removeFromVocabularyBook(id: Long) =
        vocabularyBookDao.deleteById(id)

    suspend fun markAsMastered(id: Long) =
        vocabularyBookDao.markAsMastered(id)

    /** 复习后更新进度 (艾宾浩斯) */
    suspend fun updateReview(
        id: Long,
        currentReviewCount: Int,
        difficulty: Int = 0
    ) {
        val nextTime = VocabularyBookItem.calculateNextReviewTime(
            currentReviewCount + 1,
            difficulty
        )
        vocabularyBookDao.updateReviewInfo(
            id = id,
            count = currentReviewCount + 1,
            lastTime = System.currentTimeMillis(),
            nextTime = nextTime,
            difficulty = difficulty
        )
    }

    suspend fun getVocabularyStats() =
        vocabularyBookDao.getStatistics()

    suspend fun isInVocabulary(wordId: Long): Boolean =
        vocabularyBookDao.isInVocabulary(wordId)

    // ==================== 工具 ====================

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
