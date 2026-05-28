package com.englishlearning.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户学习统计数据
 * 用于首页统计展示
 */
@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey
    val date: String,                     // 日期格式：yyyy-MM-dd
    val wordsLearned: Int = 0,            // 今日学习单词数
    val wordsReviewed: Int = 0,           // 今日复习单词数
    val listeningTime: Int = 0,          // 今日听力时长（分钟）
    val readingTime: Int = 0,             // 今日阅读时长（分钟）
    val totalWordsLearned: Int = 0,       // 累计学习单词数
    val totalListeningTime: Int = 0,      // 累计听力时长
    val totalReadingTime: Int = 0,        // 累计阅读时长
    val streakDays: Int = 0,              // 连续学习天数
    val lastStudyDate: String? = null     // 最后学习日期
)
