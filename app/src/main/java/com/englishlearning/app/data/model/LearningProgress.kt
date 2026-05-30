package com.englishlearning.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 学习进度实体类
 * 使用艾宾浩斯遗忘曲线算法
 */
@Entity(tableName = "learning_progress")
data class LearningProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val wordId: Long,                     // 关联的单词ID
    val nextReviewDate: Long,             // 下次复习时间戳
    val reviewCount: Int = 0,             // 复习次数
    val masteryLevel: Int = 0,            // 掌握程度 0-5
    val lastReviewDate: Long? = null,     // 上次复习时间
    val easeFactor: Float = 2.5f,         // 难度因子
    val interval: Int = 1                // 间隔天数
) {
    // 艾宾浩斯复习间隔（天）
    // 第1次复习: 1天后
    // 第2次复习: 3天后
    // 第3次复习: 7天后
    // 第4次复习: 14天后
    // 第5次复习: 30天后
    // 第6次复习: 90天后（永久记住）
    
    companion object {
        val REVIEW_INTERVALS = listOf(1, 3, 7, 14, 30, 90)
    }
}
