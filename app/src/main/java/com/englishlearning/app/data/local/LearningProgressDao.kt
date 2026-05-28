package com.englishlearning.app.data.local

import androidx.room.*
import com.englishlearning.app.data.model.LearningProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningProgressDao {
    // 查询所有进度
    @Query("SELECT * FROM learning_progress")
    fun getAllProgress(): Flow<List<LearningProgress>>
    
    // 根据单词ID查询进度
    @Query("SELECT * FROM learning_progress WHERE wordId = :wordId")
    suspend fun getProgressByWordId(wordId: Long): LearningProgress?
    
    // 获取需要复习的单词（当前时间 > 下次复习时间）
    @Query("SELECT * FROM learning_progress WHERE nextReviewDate <= :currentTime")
    fun getWordsToReview(currentTime: Long): Flow<List<LearningProgress>>
    
    // 获取复习中的单词数
    @Query("SELECT COUNT(*) FROM learning_progress")
    suspend fun getReviewingCount(): Int
    
    // 插入或更新进度
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: LearningProgress)
    
    // 删除进度
    @Delete
    suspend fun deleteProgress(progress: LearningProgress)
    
    // 更新掌握程度
    @Query("UPDATE learning_progress SET masteryLevel = :level WHERE wordId = :wordId")
    suspend fun updateMasteryLevel(wordId: Long, level: Int)
    
    // 计算下次复习时间（艾宾浩斯算法）
    @Query("SELECT interval FROM learning_progress WHERE wordId = :wordId")
    suspend fun getCurrentInterval(wordId: Long): Int?
}
