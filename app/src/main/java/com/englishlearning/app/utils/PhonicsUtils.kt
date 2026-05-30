package com.englishlearning.app.utils

import com.englishlearning.app.data.model.Syllable

/**
 * 自然拼读工具类
 * 提供音节拆分、音标生成等功能
 */
object PhonicsUtils {

    // 元音字母
    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u', 'y')

    // 辅音组合（不拆分）
    private val CONSONANT_BLENDS = setOf(
        "th", "sh", "ch", "ph", "ck", "wh", "qu",
        "tr", "dr", "br", "cr", "gr", "pr", "fr",
        "bl", "fl", "pl", "cl", "gl", "sl",
        "sc", "sk", "sm", "sn", "sp", "st", "sw",
        "tw", "dw", "gw", "kw"
    )

    // 元音组合（不拆分）
    private val VOWEL_TEAMS = setOf(
        "ai", "ay", "ee", "ea", "oa", "oe", "oo", "ou", "ow",
        "ar", "er", "ir", "or", "ur", "igh", "oi", "oy",
        "au", "aw", "ew", "ui", "ue"
    )

    // 简单音节到音标的映射（常用规则）
    private val SYLLABLE_TO_PHONETIC = mapOf(
        // 常见音节映射
        "a" to "ə", "an" to "ən", "and" to "ənd",
        "ban" to "bæn", "can" to "kæn", "man" to "mæn",
        "don" to "dɒn", "ton" to "tɒn", "son" to "sʌn",
        "ment" to "mənt", "ness" to "nəs",
        "tion" to "ʃən", "sion" to "ʒən",
        "ing" to "ɪŋ", "ed" to "d", "er" to "ə"
    )

    // 音节中文解释映射
    private val SYLLABLE_TO_MEANING = mapOf(
        "a" to "呃", "an" to "安", "and" to "安的",
        "ban" to "班", "can" to "看", "man" to "曼",
        "don" to "东", "ton" to "通", "son" to "森",
        "ment" to "门特", "ness" to "内斯",
        "tion" to "逊", "sion" to "忍",
        "ing" to "英", "ed" to "的", "er" to "尔"
    )

    /**
     * 按照自然拼读规则拆分音节
     * 口诀：
     * 1. 先找元音定音节，词尾哑e不算数
     * 2. 一辅归后不拆分，前开后闭读准音
     * 3. 两辅分家两边站，各归前后不相连
     * 4. 辅音组合算整体，拆分千万别分离
     * 5. r在中间分两边，前音重读按r规
     * 6. 词尾le前断，成音节单独算
     */
    fun splitSyllables(word: String): List<Syllable> {
        if (word.length <= 3) {
            // 短单词作为单音节
            return listOf(createSyllable(word))
        }

        val lowerWord = word.lowercase()
        val syllables = mutableListOf<String>()
        var i = 0

        while (i < lowerWord.length) {
            // 找元音位置
            val vowelStart = findNextVowel(lowerWord, i)
            if (vowelStart == -1) {
                // 没有更多元音，剩余部分归最后一个音节
                if (i < lowerWord.length) {
                    if (syllables.isNotEmpty()) {
                        syllables[syllables.size - 1] = syllables.last() + lowerWord.substring(i)
                    } else {
                        syllables.add(lowerWord.substring(i))
                    }
                }
                break
            }

            // 找元音结束位置（处理元音组合）
            val vowelEnd = findVowelEnd(lowerWord, vowelStart)

            // 找下一个元音
            val nextVowel = findNextVowel(lowerWord, vowelEnd + 1)

            if (nextVowel == -1) {
                // 没有下一个元音，剩余部分作为一个音节
                syllables.add(lowerWord.substring(i))
                break
            }

            // 计算两个元音之间的辅音
            val consonantsBetween = nextVowel - vowelEnd - 1

            val splitPoint = when {
                // 一辅归后
                consonantsBetween == 1 -> vowelEnd + 1
                // 两辅分家
                consonantsBetween >= 2 -> {
                    // 检查是否是辅音组合
                    val firstConsonant = lowerWord.substring(vowelEnd + 1, vowelEnd + 3)
                    if (CONSONANT_BLENDS.contains(firstConsonant)) {
                        // 辅音组合算整体，归后
                        vowelEnd + 2
                    } else {
                        // 两辅分家，各归前后
                        vowelEnd + 1
                    }
                }
                else -> vowelEnd + 1
            }

            syllables.add(lowerWord.substring(i, splitPoint))
            i = splitPoint
        }

        // 处理特殊情况
        val processedSyllables = processSpecialCases(syllables)

        return processedSyllables.map { createSyllable(it) }
    }

    /**
     * 找下一个元音位置
     */
    private fun findNextVowel(word: String, start: Int): Int {
        var i = start
        while (i < word.length) {
            if (VOWELS.contains(word[i])) {
                return i
            }
            i++
        }
        return -1
    }

    /**
     * 找元音结束位置（处理元音组合）
     */
    private fun findVowelEnd(word: String, start: Int): Int {
        if (start >= word.length - 1) return start

        // 检查是否是元音组合
        val twoChars = word.substring(start, minOf(start + 2, word.length))
        if (VOWEL_TEAMS.contains(twoChars)) {
            return start + 1
        }

        return start
    }

    /**
     * 处理特殊情况
     */
    private fun processSpecialCases(syllables: List<String>): List<String> {
        val result = mutableListOf<String>()

        for (i in syllables.indices) {
            var syllable = syllables[i]

            // 处理词尾le（成音节）
            if (i == syllables.size - 1 && syllable.length > 2 &&
                syllable.endsWith("le") && !isVowel(syllable[syllable.length - 3])) {
                // le单独成音节
                if (syllable.length > 2) {
                    result.add(syllable.substring(0, syllable.length - 2))
                    result.add(syllable.substring(syllable.length - 2))
                    continue
                }
            }

            result.add(syllable)
        }

        return result
    }

    /**
     * 判断是否是元音
     */
    private fun isVowel(char: Char): Boolean {
        return VOWELS.contains(char.lowercaseChar())
    }

    /**
     * 创建音节对象
     */
    private fun createSyllable(text: String): Syllable {
        val lowerText = text.lowercase()
        val phonetic = SYLLABLE_TO_PHONETIC[lowerText] ?: generatePhonetic(lowerText)
        val meaning = SYLLABLE_TO_MEANING[lowerText] ?: generateMeaning(lowerText)
        val tip = generatePronunciationTip(lowerText)

        return Syllable(
            text = text,
            phonetic = phonetic,
            meaning = meaning,
            pronunciationTip = tip
        )
    }

    /**
     * 生成音标（简化规则）
     */
    private fun generatePhonetic(syllable: String): String {
        // 这里使用简化规则，实际应用需要更复杂的规则或查表
        return when {
            syllable.length == 1 -> {
                when (syllable) {
                    "a" -> "ə"
                    "e" -> "ɪ"
                    "i" -> "ɪ"
                    "o" -> "ɒ"
                    "u" -> "ʌ"
                    else -> syllable
                }
            }
            else -> "/${syllable}/"
        }
    }

    /**
     * 生成中文解释
     */
    private fun generateMeaning(syllable: String): String {
        // 根据音节特点生成近似中文
        return when {
            syllable.contains("a") -> "啊"
            syllable.contains("e") -> "呃"
            syllable.contains("i") -> "衣"
            syllable.contains("o") -> "哦"
            syllable.contains("u") -> "乌"
            else -> syllable
        }
    }

    /**
     * 生成发音技巧
     */
    private fun generatePronunciationTip(syllable: String): String {
        return when {
            syllable.length == 1 -> "短促发音"
            syllable.contains("th") -> "咬舌发音"
            syllable.contains("sh") -> "翘舌发音"
            syllable.contains("ch") -> "卷舌发音"
            syllable.endsWith("ng") -> "鼻音结尾"
            else -> "自然发音"
        }
    }

    /**
     * 获取完整单词的音标
     */
    fun getWordPhonetic(word: String): String {
        val syllables = splitSyllables(word)
        return syllables.joinToString("") { it.phonetic }
    }

    /**
     * 测试音节拆分
     */
    fun testSplit(word: String): String {
        val syllables = splitSyllables(word)
        return syllables.joinToString(" - ") { "${it.text}(${it.phonetic})" }
    }
}
