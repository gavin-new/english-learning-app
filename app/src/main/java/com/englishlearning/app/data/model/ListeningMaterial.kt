package com.englishlearning.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 听力材料实体类
 */
@Entity(tableName = "listening_materials")
data class ListeningMaterial(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                    // 标题
    val description: String,              // 描述
    val audioPath: String,                // 音频文件路径
    val transcript: String,               // 原文 transcript
    val translation: String,              // 中文翻译
    val duration: Int,                    // 时长（秒）
    val level: Int = 1,                  // 难度等级 1-5
    val category: String,                 // 分类（日常、新闻、商务等）
    val isCompleted: Boolean = false,    // 是否已完成
    val lastPlayedAt: Long? = null,       // 上次播放时间
    val playCount: Int = 0,               // 播放次数
    val createdAt: Long = System.currentTimeMillis()
)
