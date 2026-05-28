package com.englishlearning.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单词实体类
 * 包含单词的所有信息：单词、音标、释义、例句、音频等
 */
@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,                    // 单词拼写
    val phonetic: String,                 // 音标（如 /əˈbstrækʃən/）
    val meaning: String,                  // 释义
    val example: String,                 // 例句
    val exampleTranslation: String,      // 例句中文翻译
    val audioPath: String? = null,       // 音频文件路径（离线）
    val level: Int = 1,                  // 难度等级 1-5
    val category: String? = null,        // 分类（小学一年级、中考、四级等）
    val grade: Int? = null,              // 年级（1-6小学，7中考，8高考，9四级，10六级）
    val isLearned: Boolean = false,       // 是否已学习
    val isFavorite: Boolean = false,     // 是否收藏
    val prefix: String? = null,          // 前缀
    val suffix: String? = null,          // 后缀
    val root: String? = null,            // 词根
    val rootMeaning: String? = null,     // 词根释义
    val createdAt: Long = System.currentTimeMillis()
)
