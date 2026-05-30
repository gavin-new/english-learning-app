package com.englishlearning.app.data.repository

import com.englishlearning.app.data.local.AppDatabase
import com.englishlearning.app.data.model.LearningProgress
import com.englishlearning.app.data.model.UserStats
import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 学习进度仓库
 * 管理艾宾浩斯复习计划、用户统计、学习记录
 */
class ProgressRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val progressDao = db.learningProgressDao()
    private val statsDao = db.userStatsDao()

    // ==================== 学习进度 ====================

    /** 获取所有进度 */
    fun getAllProgress(): Flow<List<LearningProgress>> = progressDao.getAllProgress()

    /** 按单词ID查进度 */
    suspend fun getProgressByWordId(wordId: Long): LearningProgress? =
        progressDao.getProgressByWordId(wordId)

    /** 获取需要复习的单词进度 */
    fun getWordsToReview(): Flow<List<LearningProgress>> =
        progressDao.getWordsToReview(System.currentTimeMillis())

    /** 获取复习中单词数 */
    suspend fun getReviewingCount(): Int = progressDao.getReviewingCount()

    /**
     * 记录一次学习 (单词学完时调用)
     * 创建或更新艾宾浩斯进度, 自动计算下次复习时间
     */
    suspend fun recordLearning(
        wordId: Long,
        masteryLevel: Int = 1  // 初始掌握程度 0-5
    ) {
        val existing = progressDao.getProgressByWordId(wordId)
        if (existing != null) {
            // 已存在进度, 更新复习次数
            val newCount = existing.reviewCount + 1
            val newInterval = calculateEbbinghausInterval(newCount)
            val progress = existing.copy(
                reviewCount = newCount,
                masteryLevel = masteryLevel.coerceIn(0, 5),
                lastReviewDate = System.currentTimeMillis(),
                nextReviewDate = System.currentTimeMillis() + newInterval * 24 * 60 * 60 * 1000L,
                interval = newInterval
            )
            progressDao.insertOrUpdate(progress)
        } else {
            // 首次学习, 创建新进度
            val progress = LearningProgress(
                wordId = wordId,
                reviewCount = 1,
                masteryLevel = masteryLevel.coerceIn(0, 5),
                lastReviewDate = System.currentTimeMillis(),
                nextReviewDate = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000L, // 1天后首次复习
                interval = 1
            )
            progressDao.insertOrUpdate(progress)
        }
    }

    /** 更新掌握程度 */
    suspend fun updateMastery(wordId: Long, level: Int) {
        progressDao.updateMasteryLevel(wordId, level.coerceIn(0, 5))
    }

    /** 删除进度 */
    suspend fun deleteProgress(progress: LearningProgress) {
        progressDao.deleteProgress(progress)
    }

    // ==================== 用户统计 ====================

    /** 获取今日统计 */
    fun getTodayStats(): Flow<UserStats?> = statsDao.getTodayStats(todayDate())

    /** 获取本周统计 */
    fun getWeekStats(): Flow<List<UserStats>> = statsDao.getWeekStats()

    /** 获取今日统计(同步) */
    suspend fun getTodayStatsSync(): UserStats? = statsDao.getStatsByDate(todayDate())

    /**
     * 初始化今日统计 (首次访问时调用)
     */
    suspend fun ensureTodayStats() {
        val today = todayDate()
        val existing = statsDao.getStatsByDate(today)
        if (existing == null) {
            // 获取昨天的统计, 计算连续天数
            val yesterday = yesterdayDate()
            val yesterdayStats = statsDao.getStatsByDate(yesterday)
            val streak = if (yesterdayStats != null) {
                yesterdayStats.streakDays + 1
            } else {
                1
            }
            statsDao.insertOrUpdate(
                UserStats(
                    date = today,
                    streakDays = streak,
                    lastStudyDate = today
                )
            )
        }
    }

    /**
     * 更新今日学习数据
     */
    suspend fun updateTodayLearning(wordCount: Int = 1) {
        ensureTodayStats()
        statsDao.updateTodayLearning(todayDate(), wordCount)
    }

    /**
     * 获取首页统计数据
     */
    suspend fun getHomeStats(): HomeStats {
        val today = statsDao.getStatsByDate(todayDate())
        val totalLearned = progressDao.getReviewingCount()
        return HomeStats(
            streakDays = today?.streakDays ?: 0,
            wordsLearned = today?.wordsLearned ?: 0,
            totalWordsLearned = today?.totalWordsLearned ?: 0,
            listeningTime = today?.listeningTime ?: 0,
            readingTime = today?.readingTime ?: 0,
            wordsToReview = totalLearned
        )
    }

    // ==================== 工具方法 ====================

    private fun todayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun yesterdayDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    private fun calculateEbbinghausInterval(reviewCount: Int): Int {
        val intervals = listOf(1, 3, 7, 14, 30, 90)
        return if (reviewCount in 1..intervals.size) {
            intervals[reviewCount - 1]
        } else {
            intervals.last()
        }
    }

    companion object {
        // 艾宾浩斯复习间隔(天)
        val EBBINGHAUS_INTERVALS = listOf(1, 3, 7, 14, 30, 90)
    }
}

/**
 * 首页统计数据
 */
data class HomeStats(
    val streakDays: Int,
    val wordsLearned: Int,
    val totalWordsLearned: Int,
    val listeningTime: Int,
    val readingTime: Int,
    val wordsToReview: Int
)
