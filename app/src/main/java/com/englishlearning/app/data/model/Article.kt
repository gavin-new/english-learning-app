package com.englishlearning.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 文章实体类
 * 用于阅读理解模块
 */
@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                   // 文章标题
    val content: String,                  // 文章内容
    val level: Int = 1,                   // 难度等级 1-5
    val wordCount: Int,                   // 单词数
    val estimatedReadTime: Int,           // 预估阅读时间（分钟）
    val category: String,                // 分类（科技、生活、文化等）
    val isFavorite: Boolean = false,
    val lastReadAt: Long? = null,         // 上次阅读时间
    val createdAt: Long = System.currentTimeMillis()
)
