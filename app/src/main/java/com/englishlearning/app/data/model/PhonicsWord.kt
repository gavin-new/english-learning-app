package com.englishlearning.app.data.model

/**
 * 自然拼读单词数据类
 * 扩展原有Word类，添加音节拆分信息
 */
data class PhonicsWord(
    val id: Long,
    val word: String,                    // 单词拼写
    val phonetic: String,                // 完整音标
    val meaning: String,                 // 中文释义
    val syllables: List<Syllable>,       // 音节拆分列表
    val example: String?,                // 例句
    val exampleTranslation: String?,     // 例句翻译
    val prefix: String? = null,          // 前缀
    val root: String? = null,            // 词根
    val suffix: String? = null,          // 后缀
    val rootMeaning: String? = null,     // 词根含义
    val level: Int = 1,                  // 难度等级
    val category: String? = null         // 分类（小学、中考等）
)
