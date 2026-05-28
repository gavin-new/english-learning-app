package com.englishlearning.app.data.local

import androidx.room.*
import com.englishlearning.app.data.model.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    // 查询所有单词
    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    fun getAllWords(): Flow<List<Word>>
    
    // 根据ID查询
    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordById(id: Long): Word?
    
    // 根据单词拼写搜索
    @Query("SELECT * FROM words WHERE word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%'")
    fun searchWords(query: String): Flow<List<Word>>
    
    // 根据难度筛选
    @Query("SELECT * FROM words WHERE level = :level")
    fun getWordsByLevel(level: Int): Flow<List<Word>>
    
    // 根据年级筛选
    @Query("SELECT * FROM words WHERE grade = :grade")
    fun getWordsByGrade(grade: Int): Flow<List<Word>>
    
    // 根据分类筛选
    @Query("SELECT * FROM words WHERE category = :category")
    fun getWordsByCategory(category: String): Flow<List<Word>>
    
    // 获取收藏的单词
    @Query("SELECT * FROM words WHERE isFavorite = 1")
    fun getFavoriteWords(): Flow<List<Word>>
    
    // 获取待复习的单词
    @Query("SELECT * FROM words WHERE isLearned = 1")
    fun getLearnedWords(): Flow<List<Word>>
    
    // 获取未学习的单词
    @Query("SELECT * FROM words WHERE isLearned = 0 LIMIT :limit")
    suspend fun getNewWords(limit: Int): List<Word>
    
    // 根据前缀/后缀/词根查询
    @Query("SELECT * FROM words WHERE prefix LIKE '%' || :morphology || '%' OR suffix LIKE '%' || :morphology || '%' OR root LIKE '%' || :morphology || '%'")
    fun getWordsByMorphology(morphology: String): Flow<List<Word>>
    
    // 插入单词
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word): Long
    
    // 批量插入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<Word>)
    
    // 更新单词
    @Update
    suspend fun updateWord(word: Word)
    
    // 删除单词
    @Delete
    suspend fun deleteWord(word: Word)
    
    // 获取单词总数
    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int
    
    // 获取已学习单词数
    @Query("SELECT COUNT(*) FROM words WHERE isLearned = 1")
    suspend fun getLearnedWordCount(): Int
    
    // 获取各年级单词数量
    @Query("SELECT COUNT(*) FROM words WHERE grade = :grade")
    suspend fun getWordCountByGrade(grade: Int): Int
    
    // 清空所有单词
    @Query("DELETE FROM words")
    suspend fun deleteAllWords()
}
