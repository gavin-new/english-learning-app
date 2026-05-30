package com.englishlearning.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

/**
 * 离线英汉词典仓库
 * 从assets加载精选词典, 支持单词查询
 * 
 * 词典格式: JSON数组 [{word, phonetic, meanings: [...], examples: [...]}]
 * 词典来源: 精选15000常用词条, 覆盖K12+CET范围
 */
class DictionaryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private var dictionary: Map<String, DictEntry> = emptyMap()
    private var isLoaded = false

    /**
     * 词典条目
     */
    data class DictEntry(
        val word: String,
        val phonetic: String = "",
        val meanings: List<String> = emptyList(),  // 中文释义列表
        val examples: List<DictExample> = emptyList(), // 例句
        val pos: String = ""  // 词性
    )

    data class DictExample(
        val en: String,
        val zh: String
    )

    /**
     * 加载词典到内存 (首次查询时自动调用)
     */
    suspend fun ensureLoaded(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true
        try {
            // 尝试从assets加载合并词典
            val dictList = loadDictFromAssets("dictionary/ecdict_mini.json")
            if (dictList.isNotEmpty()) {
                dictionary = dictList.associateBy { it.word.lowercase() }
                isLoaded = true
                return@withContext true
            }
            false
        } catch (e: Exception) {
            // 词典文件不存在 — 降级使用词汇库查词
            false
        }
    }

    /**
     * 查询单词释义
     * @return DictEntry? 词典条目, null表示未找到
     */
    suspend fun lookup(word: String): DictEntry? = withContext(Dispatchers.IO) {
        if (!isLoaded) ensureLoaded()
        dictionary[word.lowercase().trim()]
    }

    /**
     * 批量查询
     */
    suspend fun lookupBatch(words: List<String>): Map<String, DictEntry> = withContext(Dispatchers.IO) {
        if (!isLoaded) ensureLoaded()
        words.mapNotNull { word ->
            val entry = dictionary[word.lowercase().trim()]
            if (entry != null) word.lowercase() to entry else null
        }.toMap()
    }

    /**
     * 模糊搜索 — 前缀匹配
     */
    suspend fun searchByPrefix(prefix: String, limit: Int = 20): List<DictEntry> = withContext(Dispatchers.IO) {
        if (!isLoaded) ensureLoaded()
        val lower = prefix.lowercase().trim()
        dictionary.entries
            .filter { it.key.startsWith(lower) }
            .sortedBy { it.key.length }
            .take(limit)
            .map { it.value }
    }

    /**
     * 全文搜索 — 在释义中搜索
     */
    suspend fun searchInMeanings(query: String, limit: Int = 20): List<DictEntry> = withContext(Dispatchers.IO) {
        if (!isLoaded) ensureLoaded()
        val lower = query.lowercase().trim()
        dictionary.values
            .filter { entry ->
                entry.meanings.any { it.contains(lower) } ||
                entry.word.contains(lower)
            }
            .take(limit)
    }

    /**
     * 获取词典大小
     */
    fun getDictSize(): Int = dictionary.size

    /**
     * 是否已加载
     */
    fun isDictLoaded(): Boolean = isLoaded

    // ==================== 内部方法 ====================

    private fun loadDictFromAssets(fileName: String): List<DictEntry> {
        return try {
            val inputStream = appContext.assets.open(fileName)
            val reader = InputStreamReader(inputStream, "UTF-8")
            val type = object : TypeToken<List<DictEntryJson>>() {}.type
            val rawList: List<DictEntryJson> = gson.fromJson(reader, type)
            reader.close()
            inputStream.close()
            rawList.map { it.toDictEntry() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * JSON解析用的中间类
     */
    data class DictEntryJson(
        val word: String = "",
        val phonetic: String = "",
        val translation: String = "",  // 用分号分隔的多条释义
        val pos: String = "",
        val example_en: String = "",
        val example_zh: String = ""
    ) {
        fun toDictEntry(): DictEntry {
            return DictEntry(
                word = word,
                phonetic = phonetic,
                meanings = translation.split("；", ";").map { it.trim() }.filter { it.isNotEmpty() },
                examples = if (example_en.isNotBlank()) {
                    listOf(DictExample(example_en, example_zh))
                } else {
                    emptyList()
                },
                pos = pos
            )
        }
    }
}
