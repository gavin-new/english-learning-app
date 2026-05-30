package com.englishlearning.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.data.model.VocabularyBookItem
import com.englishlearning.app.data.repository.DictationRepository
import com.englishlearning.app.data.repository.ProgressRepository
import com.englishlearning.app.data.repository.WordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 词汇学习ViewModel
 * 管理背单词、复习、生词本三大功能
 */
class VocabularyViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)
    val progressRepo = ProgressRepository(application)
    val dictationRepo = DictationRepository(application)

    // ==================== 学习模式 ====================

    /** 当前选中的年级 (0=全部, 1-6=小学, 7=中考, 8=高考, 9=CET4, 10=CET6, 11-18=沪教版3-6年级) */
    private val _selectedGrade = MutableStateFlow(0)
    val selectedGrade: StateFlow<Int> = _selectedGrade.asStateFlow()

    /** 当前学习词汇列表 */
    private val _currentWords = MutableStateFlow<List<Word>>(emptyList())
    val currentWords: StateFlow<List<Word>> = _currentWords.asStateFlow()

    /** 当前词汇索引 */
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    /** 今日新词列表 */
    private val _todayWords = MutableStateFlow<List<Word>>(emptyList())
    val todayWords: StateFlow<List<Word>> = _todayWords.asStateFlow()

    // ==================== 复习模式 ====================

    /** 待复习词汇 */
    private val _reviewWords = MutableStateFlow<List<Word>>(emptyList())
    val reviewWords: StateFlow<List<Word>> = _reviewWords.asStateFlow()

    /** 复习进度 */
    private val _reviewIndex = MutableStateFlow(0)
    val reviewIndex: StateFlow<Int> = _reviewIndex.asStateFlow()

    // ==================== 生词本 ====================

    private val _vocabularyBook = MutableStateFlow<List<VocabularyBookItem>>(emptyList())
    val vocabularyBook: StateFlow<List<VocabularyBookItem>> = _vocabularyBook.asStateFlow()

    /** 生词本中待复习数量 */
    private val _vocabReviewCount = MutableStateFlow(0)
    val vocabReviewCount: StateFlow<Int> = _vocabReviewCount.asStateFlow()

    init {
        loadTodayWords()
        observeVocabularyBook()
        observeReviewCount()
    }

    // ==================== 学习方法 ====================

    /** 加载今日新词 */
    fun loadTodayWords(limit: Int = 20) {
        viewModelScope.launch {
            try {
                val words = wordRepo.getNewWords(limit)
                _todayWords.value = words
                if (words.isNotEmpty()) {
                    _currentWords.value = words
                    _currentIndex.value = 0
                }
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    /** 切换年级 */
    fun selectGrade(grade: Int) {
        _selectedGrade.value = grade
        viewModelScope.launch {
            try {
                val words = if (grade == 0) {
                    wordRepo.getNewWords(20)
                } else {
                    // 沪教版年级映射: 11=三年上, 12=三年下, 13=四年上, ... 18=六年下
                    val mappedGrade = when (grade) {
                        in 11..18 -> grade  // 沪教版直接映射
                        else -> grade       // 标准年级映射
                    }
                    wordRepo.getNewWords(20) // 暂时统一处理
                }
                _currentWords.value = words
                _currentIndex.value = 0
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    /** 下一个词 */
    fun nextWord() {
        val idx = _currentIndex.value + 1
        if (idx < _currentWords.value.size) {
            _currentIndex.value = idx
        }
    }

    /** 上一个词 */
    fun previousWord() {
        val idx = _currentIndex.value - 1
        if (idx >= 0) {
            _currentIndex.value = idx
        }
    }

    /** 获取当前词 */
    fun getCurrentWord(): Word? {
        val words = _currentWords.value
        val idx = _currentIndex.value
        return if (idx in words.indices) words[idx] else null
    }

    /** 标记当前词为已学 */
    fun markCurrentAsLearned() {
        viewModelScope.launch {
            val word = getCurrentWord() ?: return@launch
            wordRepo.markAsLearned(word)
            progressRepo.recordLearning(word.id)
            progressRepo.updateTodayLearning(1)
        }
    }

    /** 切换收藏 */
    fun toggleFavorite(word: Word) {
        viewModelScope.launch {
            wordRepo.toggleFavorite(word)
        }
    }

    // ==================== 复习方法 ====================

    fun loadReviewWords() {
        viewModelScope.launch {
            try {
                // 从学习进度中获取待复习词汇
                val progressList = progressRepo.getWordsToReview().first()
                val reviewIds = progressList.map { it.wordId }
                // 通过ID获取单词详情 (简化处理)
                val words = reviewIds.mapNotNull { wordRepo.getWordById(it) }
                _reviewWords.value = words
                _reviewIndex.value = 0
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    fun nextReviewWord() {
        val idx = _reviewIndex.value + 1
        if (idx < _reviewWords.value.size) {
            _reviewIndex.value = idx
        }
    }

    fun getCurrentReviewWord(): Word? {
        val words = _reviewWords.value
        val idx = _reviewIndex.value
        return if (idx in words.indices) words[idx] else null
    }

    fun completeReview(wordId: Long, masteryLevel: Int) {
        viewModelScope.launch {
            progressRepo.recordLearning(wordId, masteryLevel)
            nextReviewWord()
        }
    }

    // ==================== 生词本方法 ====================

    private fun observeVocabularyBook() {
        viewModelScope.launch {
            dictationRepo.getVocabularyBook().collect { items ->
                _vocabularyBook.value = items
            }
        }
    }

    private fun observeReviewCount() {
        viewModelScope.launch {
            dictationRepo.getReviewCount().collect { count ->
                _vocabReviewCount.value = count
            }
        }
    }

    fun addToVocabularyBook(word: Word) {
        viewModelScope.launch {
            dictationRepo.addToVocabularyBook(
                wordId = word.id,
                word = word.word,
                meaning = word.meaning,
                category = word.category ?: "通用",
                phonetic = word.phonetic
            )
        }
    }

    fun removeFromVocabularyBook(id: Long) {
        viewModelScope.launch {
            dictationRepo.removeFromVocabularyBook(id)
        }
    }

    fun markVocabAsMastered(id: Long) {
        viewModelScope.launch {
            dictationRepo.markAsMastered(id)
        }
    }

    fun reviewVocabItem(id: Long, reviewCount: Int, difficulty: Int = 0) {
        viewModelScope.launch {
            dictationRepo.updateReview(id, reviewCount, difficulty)
        }
    }
}
