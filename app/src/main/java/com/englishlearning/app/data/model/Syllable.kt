package com.englishlearning.app.data.model

/**
 * 音节数据类
 * 用于自然拼读音节拆分
 */
data class Syllable(
    val text: String,           // 音节文本（如："ban"）
    val phonetic: String,       // 音标（如："/bæn/"）
    val meaning: String,        // 中文解释（如："班"）
    val pronunciationTip: String // 发音技巧（如："嘴巴张大"）
)
