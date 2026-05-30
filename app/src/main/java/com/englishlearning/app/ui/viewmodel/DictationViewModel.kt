package com.englishlearning.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.model.DictationRecord
import com.englishlearning.app.data.model.DictationSession
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.data.repository.DictationRepository
import com.englishlearning.app.data.repository.WordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 听写ViewModel
 * 管理听写流程: 选词 → 播放 → 输入 → 判断 → 记录
 */
class DictationViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)
    val dictationRepo = DictationRepository(application)

    // ==================== 听写状态 ====================

    /** 本次听写词汇列表 */
    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow()

    /** 当前索引 */
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    /** 当前单词 */
    val currentWord: StateFlow<Word?> = combine(_words, _currentIndex) { words, idx ->
        if (idx in words.indices) words[idx] else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    /** 用户输入 */
    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput.asStateFlow()

    /** 是否显示结果 */
    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult.asStateFlow()

    /** 是否正确 */
    private val _isCorrect = MutableStateFlow(false)
    val isCorrect: StateFlow<Boolean> = _isCorrect.asStateFlow()

    /** 听写结果记录 */
    private val _results = MutableStateFlow<Map<Int, DictationResultItem>>(emptyMap())
    val results: StateFlow<Map<Int, DictationResultItem>> = _results.asStateFlow()

    /** 是否完成 */
    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    /** 当前会话 */
    private var currentSession: DictationSession? = null

    // ==================== 历史记录 ====================

    private val _recentSessions = MutableStateFlow<List<DictationSession>>(emptyList())
    val recentSessions: StateFlow<List<DictationSession>> = _recentSessions.asStateFlow()

    /** 听写进度 */
    val progress: Float
        get() {
            val total = _words.value.size
            return if (total > 0) (_currentIndex.value + 1).toFloat() / total else 0f
        }

    // ==================== 方法 ====================

    /** 开始新一轮听写 */
    fun startNewDictation(wordCount: Int = 10) {
        viewModelScope.launch {
            try {
                val allWords = wordRepo.getNewWords(wordCount)
                if (allWords.isEmpty()) return@launch

                _words.value = allWords
                _currentIndex.value = 0
                _userInput.value = ""
                _showResult.value = false
                _isCorrect.value = false
                _results.value = emptyMap()
                _isCompleted.value = false

                // 创建会话
                currentSession = dictationRepo.startNewSession(
                    totalWords = allWords.size,
                    category = "随机"
                )
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    /** 更新输入 */
    fun updateInput(input: String) {
        _userInput.value = input
    }

    /** 检查答案 */
    fun checkAnswer() {
        val word = currentWord.value ?: return
        val input = _userInput.value.trim()
        val correct = input.equals(word.word, ignoreCase = true)

        _isCorrect.value = correct
        _showResult.value = true

        // 记录结果
        val resultMap = _results.value.toMutableMap()
        resultMap[_currentIndex.value] = DictationResultItem(
            word = word.word,
            userInput = input,
            isCorrect = correct
        )
        _results.value = resultMap

        // 保存到数据库
        viewModelScope.launch {
            currentSession?.let {
                dictationRepo.insertRecord(
                    DictationRecord(
                        wordId = word.id,
                        word = word.word,
                        userInput = input,
                        isCorrect = correct,
                        inputMethod = DictationRecord.InputMethod.KEYBOARD,
                        sessionId = it.id
                    )
                )
            }
        }
    }

    /** 下一个词 */
    fun nextWord() {
        if (_currentIndex.value < _words.value.size - 1) {
            _currentIndex.value = _currentIndex.value + 1
            _userInput.value = ""
            _showResult.value = false
        } else {
            // 全部完成
            _isCompleted.value = true
            viewModelScope.launch {
                currentSession?.let {
                    dictationRepo.completeSession(it.id)
                }
            }
        }
    }

    /** 获取正确数量 */
    fun getCorrectCount(): Int {
        return _results.value.count { it.value.isCorrect }
    }

    /** 获取准确率 */
    fun getAccuracy(): Float {
        val total = _results.value.size
        if (total == 0) return 0f
        return getCorrectCount().toFloat() / total
    }

    /** 加载历史会话 */
    fun loadRecentSessions() {
        viewModelScope.launch {
            try {
                _recentSessions.value = dictationRepo.getRecentSessions(10)
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }
}

/** 听写结果项 */
data class DictationResultItem(
    val word: String,
    val userInput: String,
    val isCorrect: Boolean
)
