package com.englishlearning.app.data.local

import androidx.room.*
import com.englishlearning.app.data.model.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    // 根据日期查询
    @Query("SELECT * FROM user_stats WHERE date = :date")
    suspend fun getStatsByDate(date: String): UserStats?
    
    // 获取今日统计
    @Query("SELECT * FROM user_stats WHERE date = :date")
    fun getTodayStats(date: String): Flow<UserStats?>
    
    // 获取本周统计
    @Query("SELECT * FROM user_stats ORDER BY date DESC LIMIT 7")
    fun getWeekStats(): Flow<List<UserStats>>
    
    // 插入或更新统计
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStats)
    
    // 更新今日学习数据
    @Query("""
        UPDATE user_stats SET 
        wordsLearned = wordsLearned + :wordsCount,
        totalWordsLearned = totalWordsLearned + :wordsCount,
        lastStudyDate = :date
        WHERE date = :date
    """)
    suspend fun updateTodayLearning(date: String, wordsCount: Int)
    
    // 更新连续学习天数
    @Query("UPDATE user_stats SET streakDays = :days WHERE date = :date")
    suspend fun updateStreakDays(date: String, days: Int)
}
