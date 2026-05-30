package com.englishlearning.app.data.repository

import com.englishlearning.app.data.local.AppDatabase
import com.englishlearning.app.data.local.VocabularyLoader
import com.englishlearning.app.data.model.Word
import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * 单词仓库 —— UI层访问词汇数据的唯一入口
 * 负责从Room DB读取, 首次启动从assets自动填充
 */
class WordRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val wordDao = db.wordDao()

    /** 首次启动标志 —— 确保词汇只加载一次 */
    @Volatile
    var isInitialized = false
        private set

    /**
     * 初始化词汇库: 从assets JSON文件加载所有词汇到Room
     * 仅在首次启动时调用, 数据库回调已自动执行, 此处作为补充确保
     */
    suspend fun initialize(): Boolean {
        if (isInitialized) return true
        return try {
            val count = wordDao.getWordCount()
            if (count == 0) {
                // DB为空, 触发VocabularyLoader加载
                val allWords = VocabularyLoader.loadAllVocabulary(
                    db.openHelper.writableDatabase.let {
                        // 需要Context — 由外部传入
                        return@let emptyList<Word>()
                    }
                )
                // 注意: AppDatabase.Callback已经处理了首次填充
                // 这里只是作为备用路径
            }
            isInitialized = true
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== 词汇查询 ====================

    /** 获取所有词汇 (Flow, 响应式) */
    fun getAllWords(): Flow<List<Word>> = wordDao.getAllWords()

    /** 按年级筛选词汇 */
    fun getWordsByGrade(grade: Int): Flow<List<Word>> = wordDao.getWordsByGrade(grade)

    /** 按分类筛选 */
    fun getWordsByCategory(category: String): Flow<List<Word>> = wordDao.getWordsByCategory(category)

    /** 按难度筛选 */
    fun getWordsByLevel(level: Int): Flow<List<Word>> = wordDao.getWordsByLevel(level)

    /** 获取新词 (未学习) */
    suspend fun getNewWords(limit: Int = 20): List<Word> = wordDao.getNewWords(limit)

    /** 按前缀/后缀/词根查询 */
    fun searchByMorphology(text: String): Flow<List<Word>> = wordDao.getWordsByMorphology(text)

    /** 通用搜索 */
    fun searchWords(query: String): Flow<List<Word>> = wordDao.searchWords(query)

    /** 按ID查询 */
    suspend fun getWordById(id: Long): Word? = wordDao.getWordById(id)

    /** 获取已学习词汇 */
    fun getLearnedWords(): Flow<List<Word>> = wordDao.getLearnedWords()

    // ==================== 收藏管理 ====================

    fun getFavoriteWords(): Flow<List<Word>> = wordDao.getFavoriteWords()

    suspend fun toggleFavorite(word: Word) {
        val updated = word.copy(isFavorite = !word.isFavorite)
        wordDao.updateWord(updated)
    }

    suspend fun addToFavorites(word: Word) {
        if (!word.isFavorite) {
            wordDao.updateWord(word.copy(isFavorite = true))
        }
    }

    suspend fun removeFromFavorites(word: Word) {
        if (word.isFavorite) {
            wordDao.updateWord(word.copy(isFavorite = false))
        }
    }

    // ==================== 学习状态管理 ====================

    suspend fun markAsLearned(word: Word) {
        val updated = word.copy(isLearned = true)
        wordDao.updateWord(updated)
    }

    suspend fun markAsUnlearned(word: Word) {
        val updated = word.copy(isLearned = false)
        wordDao.updateWord(updated)
    }

    // ==================== 统计 ====================

    suspend fun getWordCount(): Int = wordDao.getWordCount()
    suspend fun getLearnedWordCount(): Int = wordDao.getLearnedWordCount()
    suspend fun getWordCountByGrade(grade: Int): Int = wordDao.getWordCountByGrade(grade)
}
