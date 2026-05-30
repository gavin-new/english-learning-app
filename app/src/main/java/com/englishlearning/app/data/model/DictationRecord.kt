package com.englishlearning.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * 听写记录
 */
@Parcelize
@Entity(tableName = "dictation_records")
data class DictationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val wordId: Long,                    // 单词ID
    val word: String,                    // 单词文本
    val userInput: String,               // 用户输入
    val isCorrect: Boolean,              // 是否正确
    val inputMethod: InputMethod,        // 输入方式
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String                // 听写会话ID（一次听写练习的唯一标识）
) : Parcelable {

    enum class InputMethod {
        KEYBOARD,       // 键盘输入
        VOICE,          // 语音输入
        BOTH            // 键盘+语音组合
    }

    /**
     * 计算准确率（字符级别）
     */
    fun calculateAccuracy(): Float {
        if (word.isEmpty()) return 0f

        val correctChars = word.zip(userInput).count { (a, b) ->
            a.equals(b, ignoreCase = true)
        }
        return correctChars.toFloat() / word.length
    }

    /**
     * 获取错误位置
     */
    fun getErrorPositions(): List<Int> {
        val errors = mutableListOf<Int>()
        val maxLen = maxOf(word.length, userInput.length)

        for (i in 0 until maxLen) {
            val expected = word.getOrNull(i)
            val actual = userInput.getOrNull(i)
            if (expected?.equals(actual ?: ' ', ignoreCase = true) != true) {
                errors.add(i)
            }
        }
        return errors
    }
}

/**
 * 听写会话
 */
@Parcelize
@Entity(tableName = "dictation_sessions")
data class DictationSession(
    @PrimaryKey
    val id: String,                      // 会话ID
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val totalWords: Int = 0,             // 总单词数
    val correctCount: Int = 0,           // 正确数
    val category: String? = null,        // 单词分类
    val isCompleted: Boolean = false
) : Parcelable {

    /**
     * 计算准确率
     */
    fun getAccuracy(): Float {
        return if (totalWords > 0) correctCount.toFloat() / totalWords else 0f
    }

    /**
     * 获取时长（分钟）
     */
    fun getDurationMinutes(): Int {
        val end = endTime ?: System.currentTimeMillis()
        return ((end - startTime) / 60000).toInt()
    }
}
