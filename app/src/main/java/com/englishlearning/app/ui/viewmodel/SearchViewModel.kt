package com.englishlearning.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.data.repository.DictationRepository
import com.englishlearning.app.data.repository.DictionaryRepository
import com.englishlearning.app.data.repository.WordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 搜索ViewModel
 * 支持文字/语音/词根查询
 */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)
    val dictionaryRepo = DictionaryRepository(application)
    val dictationRepo = DictationRepository(application)

    /** 搜索关键词 */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** 搜索结果 */
    private val _results = MutableStateFlow<List<Word>>(emptyList())
    val results: StateFlow<List<Word>> = _results.asStateFlow()

    /** 词典查询结果 */
    private val _dictResult = MutableStateFlow<DictionaryRepository.DictEntry?>(null)
    val dictResult: StateFlow<DictionaryRepository.DictEntry?> = _dictResult.asStateFlow()

    /** 是否正在搜索 */
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /** 选中的单词详情 */
    private val _selectedWord = MutableStateFlow<Word?>(null)
    val selectedWord: StateFlow<Word?> = _selectedWord.asStateFlow()

    // ==================== 搜索方法 ====================

    fun updateQuery(q: String) {
        _query.value = q
    }

    /** 执行搜索 (从词汇库) */
    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return

        _isSearching.value = true
        viewModelScope.launch {
            try {
                // 1. 搜索词汇库
                wordRepo.searchWords(q).collect { words ->
                    _results.value = words
                    _isSearching.value = false
                }
                // 2. 同时查词典
                val dictEntry = dictionaryRepo.lookup(q)
                _dictResult.value = dictEntry
            } catch (e: Exception) {
                _isSearching.value = false
            }
        }
    }

    /** 按词根/前缀/后缀查询 */
    fun searchByMorphology(text: String) {
        _query.value = text
        viewModelScope.launch {
            try {
                wordRepo.searchByMorphology(text).collect { words ->
                    _results.value = words
                }
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    /** 查看单词详情 */
    fun selectWord(word: Word) {
        _selectedWord.value = word
    }

    fun clearSelection() {
        _selectedWord.value = null
    }

    /** 清空搜索 */
    fun clearSearch() {
        _query.value = ""
        _results.value = emptyList()
        _dictResult.value = null
        _selectedWord.value = null
    }

    // ==================== 快捷操作 ====================

    fun toggleFavorite(word: Word) {
        viewModelScope.launch {
            wordRepo.toggleFavorite(word)
        }
    }

    fun addToVocabularyBook(word: Word) {
        viewModelScope.launch {
            dictationRepo.addToVocabularyBook(
                wordId = word.id,
                word = word.word,
                meaning = word.meaning,
                category = word.category ?: "搜索",
                phonetic = word.phonetic
            )
        }
    }

    fun isInVocabulary(wordId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val inVocab = dictationRepo.isInVocabulary(wordId)
            callback(inVocab)
        }
    }
}
