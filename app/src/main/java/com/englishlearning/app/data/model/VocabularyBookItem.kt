package com.englishlearning.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * 生词本条目
 */
@Parcelize
@Entity(
    tableName = "vocabulary_book",
    indices = [Index(value = ["wordId"], unique = true)]
)
data class VocabularyBookItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val wordId: Long,                    // 关联的单词ID
    val word: String,                    // 单词文本（冗余存储，方便查询）
    val phonetic: String?,               // 音标
    val meaning: String,                 // 中文意思
    val category: String,                // 所属分类（grade1, cet4等）
    val addedTime: Long = System.currentTimeMillis(),
    val reviewCount: Int = 0,            // 复习次数
    val lastReviewTime: Long? = null,    // 上次复习时间
    val nextReviewTime: Long? = null,    // 下次复习时间（艾宾浩斯）
    val isMastered: Boolean = false,     // 是否已掌握
    val difficulty: Int = 0,             // 难度评级（0-5，用于调整复习间隔）
    val notes: String? = null            // 用户笔记
) : Parcelable {

    companion object {
        // 艾宾浩斯遗忘曲线复习间隔（天）
        val EBBINGHAUS_INTERVALS = listOf(
            1,      // 第1次复习：1天后
            2,      // 第2次复习：2天后
            4,      // 第3次复习：4天后
            7,      // 第4次复习：7天后
            15,     // 第5次复习：15天后
            30      // 第6次复习：30天后
        )

        /**
         * 计算下次复习时间
         */
        fun calculateNextReviewTime(reviewCount: Int, difficulty: Int = 0): Long {
            val baseInterval = if (reviewCount < EBBINGHAUS_INTERVALS.size) {
                EBBINGHAUS_INTERVALS[reviewCount]
            } else {
                EBBINGHAUS_INTERVALS.last() * (reviewCount - EBBINGHAUS_INTERVALS.size + 2)
            }

            // 根据难度调整间隔（难度越高，间隔越短）
            val adjustedInterval = when (difficulty) {
                0 -> baseInterval
                1 -> (baseInterval * 0.9).toInt()
                2 -> (baseInterval * 0.8).toInt()
                3 -> (baseInterval * 0.7).toInt()
                4 -> (baseInterval * 0.6).toInt()
                else -> (baseInterval * 0.5).toInt()
            }.coerceAtLeast(1)

            return System.currentTimeMillis() + adjustedInterval * 24 * 60 * 60 * 1000L
        }
    }

    /**
     * 获取复习阶段描述
     */
    fun getReviewStage(): String {
        return when {
            isMastered -> "已掌握"
            reviewCount == 0 -> "新词"
            reviewCount < EBBINGHAUS_INTERVALS.size -> "第${reviewCount + 1}轮复习"
            else -> "长期记忆"
        }
    }

    /**
     * 检查是否需要复习
     */
    fun needsReview(): Boolean {
        if (isMastered) return false
        val nextTime = nextReviewTime ?: return true
        return System.currentTimeMillis() >= nextTime
    }

    /**
     * 获取剩余复习天数
     */
    fun getDaysUntilReview(): Int {
        if (isMastered) return -1
        val nextTime = nextReviewTime ?: return 0
        val diff = nextTime - System.currentTimeMillis()
        return if (diff <= 0) 0 else (diff / (24 * 60 * 60 * 1000L)).toInt()
    }
}
