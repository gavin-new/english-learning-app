package com.englishlearning.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.audio.AudioPlayerManager
import com.englishlearning.app.data.model.PhonicsWord
import com.englishlearning.app.data.model.Syllable
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.data.repository.WordRepository
import com.englishlearning.app.utils.PhonicsUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 自然拼读ViewModel
 * 管理音节拆分、单词发音教学
 */
class PhonicsViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)

    /** 当前学习的单词 */
    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord.asStateFlow()

    /** 解析后的拼读数据 */
    private val _phonicsWord = MutableStateFlow<PhonicsWord?>(null)
    val phonicsWord: StateFlow<PhonicsWord?> = _phonicsWord.asStateFlow()

    /** 词汇列表 (所有可学习的词汇) */
    private val _wordList = MutableStateFlow<List<Word>>(emptyList())
    val wordList: StateFlow<List<Word>> = _wordList.asStateFlow()

    /** 当前索引 */
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    /** 加载所有词汇 */
    fun loadAllWords(grade: Int = 0) {
        viewModelScope.launch {
            try {
                val words = wordRepo.getNewWords(500)
                _wordList.value = words
                if (words.isNotEmpty()) {
                    setCurrentWord(words[0], 0)
                }
            } catch (e: Exception) {
                // DB可能在初始化, 使用fallback
            }
        }
    }

    /** 设置当前单词 */
    fun setCurrentWord(word: Word, index: Int) {
        _currentWord.value = word
        _currentIndex.value = index
        // 用PhonicsUtils解析
        val syllables = PhonicsUtils.splitSyllables(word.word)
        _phonicsWord.value = PhonicsWord(
            id = word.id,
            word = word.word,
            phonetic = word.phonetic,
            meaning = word.meaning,
            syllables = syllables,
            example = word.example,
            exampleTranslation = word.exampleTranslation
        )
    }

    /** 下一个词 */
    fun nextWord() {
        val idx = _currentIndex.value + 1
        val words = _wordList.value
        if (idx < words.size) {
            setCurrentWord(words[idx], idx)
        }
    }

    /** 上一个词 */
    fun previousWord() {
        val idx = _currentIndex.value - 1
        val words = _wordList.value
        if (idx >= 0) {
            setCurrentWord(words[idx], idx)
        }
    }

    /** 获取音节拆分文本 */
    fun getSyllableText(): String {
        val pw = _phonicsWord.value ?: return ""
        return pw.syllables.joinToString(" - ") { it.text }
    }

    /** 获取用于播放的文本队列 */
    fun getPlayQueue(): List<String> {
        val pw = _phonicsWord.value ?: return emptyList()
        val queue = mutableListOf<String>()
        queue.add(pw.word)
        pw.syllables.forEach { syllable ->
            queue.add(syllable.text)
            if (syllable.meaning.isNotEmpty()) {
                queue.add(syllable.meaning)
            }
        }
        queue.add(pw.meaning)
        return queue
    }
}
