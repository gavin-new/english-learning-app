package com.englishlearning.app.data.local

import android.content.Context
import com.englishlearning.app.data.model.Word
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

/**
 * 词汇数据加载器
 * 从assets目录加载JSON格式的词汇数据
 */
object VocabularyLoader {
    
    private val gson = Gson()
    
    /**
     * 加载指定年级的词汇
     * @param context 上下文
     * @param grade 年级（1-6表示小学1-6年级，7表示中考，8表示高考，9表示四级，10表示六级）
     */
    suspend fun loadVocabularyByGrade(context: Context, grade: Int): List<Word> = withContext(Dispatchers.IO) {
        try {
            val fileName = when (grade) {
                1 -> "vocabulary/grade1.json"
                2 -> "vocabulary/grade2.json"
                3 -> "vocabulary/grade3.json"
                4 -> "vocabulary/grade4.json"
                5 -> "vocabulary/grade5.json"
                6 -> "vocabulary/grade6.json"
                7 -> "vocabulary/middle_school.json"      // 中考词汇
                8 -> "vocabulary/high_school.json"        // 高考词汇
                9 -> "vocabulary/cet4.json"               // 四级词汇
                10 -> "vocabulary/cet6.json"              // 六级词汇
                else -> "vocabulary/grade1.json"
            }
            
            loadVocabularyFromAssets(context, fileName)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 加载所有词汇
     */
    suspend fun loadAllVocabulary(context: Context): List<Word> = withContext(Dispatchers.IO) {
        val allWords = mutableListOf<Word>()
        
        // 加载小学词汇
        for (grade in 1..6) {
            val words = loadVocabularyByGrade(context, grade)
            allWords.addAll(words)
        }
        
        // 加载中考词汇
        allWords.addAll(loadVocabularyByGrade(context, 7))
        
        // 加载高考词汇
        allWords.addAll(loadVocabularyByGrade(context, 8))
        
        // 加载四级词汇
        allWords.addAll(loadVocabularyByGrade(context, 9))
        
        // 加载六级词汇
        allWords.addAll(loadVocabularyByGrade(context, 10))
        
        allWords
    }
    
    /**
     * 从assets目录加载词汇JSON文件
     */
    private fun loadVocabularyFromAssets(context: Context, fileName: String): List<Word> {
        return try {
            val inputStream = context.assets.open(fileName)
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<WordData>>() {}.type
            val wordDataList: List<WordData> = gson.fromJson(reader, type)
            reader.close()
            inputStream.close()
            
            // 转换为Word对象
            wordDataList.mapIndexed { index, data ->
                Word(
                    id = index.toLong(),
                    word = data.word,
                    phonetic = data.phonetic,
                    meaning = data.meaning,
                    example = data.example,
                    exampleTranslation = data.exampleTranslation,
                    level = data.level,
                    category = data.category ?: "通用",
                    prefix = data.prefix,
                    suffix = data.suffix,
                    root = data.root,
                    rootMeaning = data.rootMeaning
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 词汇数据类（用于JSON解析）
     */
    data class WordData(
        val word: String,
        val phonetic: String,
        val meaning: String,
        val example: String,
        val exampleTranslation: String,
        val level: Int = 1,
        val category: String? = null,
        val grade: Int? = null,
        val prefix: String? = null,
        val suffix: String? = null,
        val root: String? = null,
        val rootMeaning: String? = null
    )
}
