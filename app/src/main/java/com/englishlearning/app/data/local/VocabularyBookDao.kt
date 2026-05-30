package com.englishlearning.app.data.local

import androidx.room.*
import com.englishlearning.app.data.model.VocabularyBookItem
import kotlinx.coroutines.flow.Flow

/**
 * 生词本数据访问对象
 */
@Dao
interface VocabularyBookDao {

    /**
     * 添加单词到生词本
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VocabularyBookItem): Long

    /**
     * 批量添加
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VocabularyBookItem>)

    /**
     * 从生词本删除
     */
    @Delete
    suspend fun delete(item: VocabularyBookItem)

    /**
     * 根据ID删除
     */
    @Query("DELETE FROM vocabulary_book WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 根据wordId删除
     */
    @Query("DELETE FROM vocabulary_book WHERE wordId = :wordId")
    suspend fun deleteByWordId(wordId: Long)

    /**
     * 更新生词本条目
     */
    @Update
    suspend fun update(item: VocabularyBookItem)

    /**
     * 获取所有生词
     */
    @Query("SELECT * FROM vocabulary_book ORDER BY addedTime DESC")
    fun getAllVocabulary(): Flow<List<VocabularyBookItem>>

    /**
     * 获取所有生词（同步）
     */
    @Query("SELECT * FROM vocabulary_book ORDER BY addedTime DESC")
    suspend fun getAllVocabularySync(): List<VocabularyBookItem>

    /**
     * 根据ID获取
     */
    @Query("SELECT * FROM vocabulary_book WHERE id = :id")
    suspend fun getById(id: Long): VocabularyBookItem?

    /**
     * 根据wordId获取
     */
    @Query("SELECT * FROM vocabulary_book WHERE wordId = :wordId")
    suspend fun getByWordId(wordId: Long): VocabularyBookItem?

    /**
     * 检查单词是否在生词本中
     */
    @Query("SELECT COUNT(*) > 0 FROM vocabulary_book WHERE wordId = :wordId")
    suspend fun isInVocabulary(wordId: Long): Boolean

    /**
     * 获取需要复习的单词（根据艾宾浩斯曲线）
     */
    @Query("""
        SELECT * FROM vocabulary_book 
        WHERE isMastered = 0 
        AND (nextReviewTime IS NULL OR nextReviewTime <= :currentTime)
        ORDER BY nextReviewTime ASC, reviewCount ASC
    """)
    fun getWordsForReview(currentTime: Long = System.currentTimeMillis()): Flow<List<VocabularyBookItem>>

    /**
     * 获取需要复习的单词数量
     */
    @Query("""
        SELECT COUNT(*) FROM vocabulary_book 
        WHERE isMastered = 0 
        AND (nextReviewTime IS NULL OR nextReviewTime <= :currentTime)
    """)
    fun getReviewCount(currentTime: Long = System.currentTimeMillis()): Flow<Int>

    /**
     * 获取今日新词（今天添加的）
     */
    @Query("""
        SELECT * FROM vocabulary_book 
        WHERE addedTime >= :startOfDay 
        ORDER BY addedTime DESC
    """)
    fun getTodayWords(startOfDay: Long): Flow<List<VocabularyBookItem>>

    /**
     * 获取今日新词数量
     */
    @Query("SELECT COUNT(*) FROM vocabulary_book WHERE addedTime >= :startOfDay")
    fun getTodayWordCount(startOfDay: Long): Flow<Int>

    /**
     * 按分类获取单词
     */
    @Query("SELECT * FROM vocabulary_book WHERE category = :category ORDER BY addedTime DESC")
    fun getByCategory(category: String): Flow<List<VocabularyBookItem>>

    /**
     * 获取已掌握的单词
     */
    @Query("SELECT * FROM vocabulary_book WHERE isMastered = 1 ORDER BY lastReviewTime DESC")
    fun getMasteredWords(): Flow<List<VocabularyBookItem>>

    /**
     * 获取已掌握单词数量
     */
    @Query("SELECT COUNT(*) FROM vocabulary_book WHERE isMastered = 1")
    fun getMasteredCount(): Flow<Int>

    /**
     * 获取总单词数
     */
    @Query("SELECT COUNT(*) FROM vocabulary_book")
    fun getTotalCount(): Flow<Int>

    /**
     * 搜索生词本
     */
    @Query("""
        SELECT * FROM vocabulary_book 
        WHERE word LIKE '%' || :query || '%' 
        OR meaning LIKE '%' || :query || '%'
        ORDER BY word ASC
    """)
    fun search(query: String): Flow<List<VocabularyBookItem>>

    /**
     * 清空生词本
     */
    @Query("DELETE FROM vocabulary_book")
    suspend fun clearAll()

    /**
     * 标记为已掌握
     */
    @Query("""
        UPDATE vocabulary_book 
        SET isMastered = 1, lastReviewTime = :time 
        WHERE id = :id
    """)
    suspend fun markAsMastered(id: Long, time: Long = System.currentTimeMillis())

    /**
     * 更新复习信息
     */
    @Query("""
        UPDATE vocabulary_book 
        SET reviewCount = :count, 
            lastReviewTime = :lastTime, 
            nextReviewTime = :nextTime,
            difficulty = :difficulty
        WHERE id = :id
    """)
    suspend fun updateReviewInfo(
        id: Long,
        count: Int,
        lastTime: Long,
        nextTime: Long,
        difficulty: Int
    )

    /**
     * 获取学习统计
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN isMastered = 1 THEN 1 ELSE 0 END) as mastered,
            SUM(CASE WHEN isMastered = 0 AND (nextReviewTime IS NULL OR nextReviewTime <= :currentTime) THEN 1 ELSE 0 END) as toReview
        FROM vocabulary_book
    """)
    suspend fun getStatistics(currentTime: Long = System.currentTimeMillis()): VocabularyStatistics

    /**
     * 获取最近添加的N个单词
     */
    @Query("SELECT * FROM vocabulary_book ORDER BY addedTime DESC LIMIT :limit")
    suspend fun getRecentWords(limit: Int): List<VocabularyBookItem>
}

/**
 * 生词本统计
 */
data class VocabularyStatistics(
    val total: Int,
    val mastered: Int,
    val toReview: Int
) {
    val learning: Int get() = total - mastered
    val masteryRate: Float get() = if (total > 0) mastered.toFloat() / total else 0f
}
