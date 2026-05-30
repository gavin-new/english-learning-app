package com.englishlearning.app.data.local

import androidx.room.*
import com.englishlearning.app.data.model.DictationRecord
import com.englishlearning.app.data.model.DictationSession
import kotlinx.coroutines.flow.Flow

/**
 * 听写数据访问对象
 */
@Dao
interface DictationDao {

    // ==================== 听写记录 ====================

    @Insert
    suspend fun insertRecord(record: DictationRecord): Long

    @Insert
    suspend fun insertRecords(records: List<DictationRecord>)

    @Update
    suspend fun updateRecord(record: DictationRecord)

    @Delete
    suspend fun deleteRecord(record: DictationRecord)

    @Query("SELECT * FROM dictation_records WHERE id = :id")
    suspend fun getRecordById(id: Long): DictationRecord?

    @Query("SELECT * FROM dictation_records WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getRecordsBySession(sessionId: String): Flow<List<DictationRecord>>

    @Query("SELECT * FROM dictation_records WHERE wordId = :wordId ORDER BY timestamp DESC")
    fun getRecordsByWord(wordId: Long): Flow<List<DictationRecord>>

    @Query("""
        SELECT * FROM dictation_records 
        WHERE sessionId = :sessionId AND isCorrect = 0
        ORDER BY timestamp ASC
    """)
    suspend fun getIncorrectRecords(sessionId: String): List<DictationRecord>

    @Query("""
        SELECT COUNT(*) FROM dictation_records 
        WHERE sessionId = :sessionId AND isCorrect = 1
    """)
    suspend fun getCorrectCount(sessionId: String): Int

    @Query("""
        SELECT COUNT(*) FROM dictation_records 
        WHERE sessionId = :sessionId
    """)
    suspend fun getTotalCount(sessionId: String): Int

    // ==================== 听写会话 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DictationSession)

    @Update
    suspend fun updateSession(session: DictationSession)

    @Delete
    suspend fun deleteSession(session: DictationSession)

    @Query("SELECT * FROM dictation_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): DictationSession?

    @Query("SELECT * FROM dictation_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<DictationSession>>

    @Query("SELECT * FROM dictation_sessions WHERE isCompleted = 1 ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<DictationSession>

    @Query("""
        SELECT * FROM dictation_sessions 
        WHERE startTime >= :startOfDay 
        ORDER BY startTime DESC
    """)
    fun getTodaySessions(startOfDay: Long): Flow<List<DictationSession>>

    // ==================== 统计信息 ====================

    @Query("""
        SELECT 
            COUNT(DISTINCT sessionId) as totalSessions,
            SUM(CASE WHEN isCorrect = 1 THEN 1 ELSE 0 END) as totalCorrect,
            COUNT(*) as totalAttempts
        FROM dictation_records
    """)
    suspend fun getOverallStats(): DictationOverallStats

    @Query("""
        SELECT 
            COUNT(*) as attemptCount,
            SUM(CASE WHEN isCorrect = 1 THEN 1 ELSE 0 END) as correctCount
        FROM dictation_records 
        WHERE wordId = :wordId
    """)
    suspend fun getWordStats(wordId: Long): WordDictationStats

    @Query("""
        SELECT wordId, word, COUNT(*) as errorCount
        FROM dictation_records 
        WHERE isCorrect = 0
        GROUP BY wordId, word
        ORDER BY errorCount DESC
        LIMIT :limit
    """)
    suspend fun getMostErrorProneWords(limit: Int): List<ErrorProneWord>

    @Query("DELETE FROM dictation_records")
    suspend fun clearAllRecords()

    @Query("DELETE FROM dictation_sessions")
    suspend fun clearAllSessions()
}

/**
 * 听写整体统计
 */
data class DictationOverallStats(
    val totalSessions: Int,
    val totalCorrect: Int,
    val totalAttempts: Int
) {
    val accuracy: Float get() = if (totalAttempts > 0) totalCorrect.toFloat() / totalAttempts else 0f
}

/**
 * 单词听写统计
 */
data class WordDictationStats(
    val attemptCount: Int,
    val correctCount: Int
) {
    val accuracy: Float get() = if (attemptCount > 0) correctCount.toFloat() / attemptCount else 0f
}

/**
 * 易错单词
 */
data class ErrorProneWord(
    val wordId: Long,
    val word: String,
    val errorCount: Int
)
