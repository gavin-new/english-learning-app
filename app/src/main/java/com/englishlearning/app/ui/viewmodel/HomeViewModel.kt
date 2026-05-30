package com.englishlearning.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.repository.DictationRepository
import com.englishlearning.app.data.repository.DictionaryRepository
import com.englishlearning.app.data.repository.HomeStats
import com.englishlearning.app.data.repository.ProgressRepository
import com.englishlearning.app.data.repository.WordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 首页ViewModel
 * 提供首页统计数据和快捷操作
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)
    val progressRepo = ProgressRepository(application)
    val dictationRepo = DictationRepository(application)
    val dictionaryRepo = DictionaryRepository(application)

    // ==================== 首页统计 ====================

    private val _homeStats = MutableStateFlow(HomeStats(0, 0, 0, 0, 0, 0))
    val homeStats: StateFlow<HomeStats> = _homeStats.asStateFlow()

    private val _todayWordCount = MutableStateFlow(0)
    val todayWordCount: StateFlow<Int> = _todayWordCount.asStateFlow()

    private val _totalLearnedCount = MutableStateFlow(0)
    val totalLearnedCount: StateFlow<Int> = _totalLearnedCount.asStateFlow()

    // ==================== 今日复习 ====================

    private val _reviewWordCount = MutableStateFlow(0)
    val reviewWordCount: StateFlow<Int> = _reviewWordCount.asStateFlow()

    private val _vocabBookCount = MutableStateFlow(0)
    val vocabBookCount: StateFlow<Int> = _vocabBookCount.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            try {
                // 确保今日统计存在
                progressRepo.ensureTodayStats()

                // 加载首页数据
                val stats = progressRepo.getHomeStats()
                _homeStats.value = stats

                // 今日单词数
                _todayWordCount.value = stats.wordsLearned

                // 累计单词数
                _totalLearnedCount.value = progressRepo.getReviewingCount()

                // 待复习数
                _reviewWordCount.value = stats.wordsToReview
            } catch (e: Exception) {
                // 静默失败
            }
        }

        // 监听生词本数量
        viewModelScope.launch {
            dictationRepo.getReviewCount().collect { count ->
                _vocabBookCount.value = count
            }
        }
    }

    // ==================== 今日目标 ====================

    // 默认每日目标: 20词
    private val dailyGoal = 20

    fun getDailyGoal(): Int = dailyGoal

    fun getDailyProgress(): Float {
        val learned = _todayWordCount.value
        return if (dailyGoal > 0) {
            (learned.toFloat() / dailyGoal).coerceIn(0f, 1f)
        } else 0f
    }

    fun getDailyProgressText(): String {
        return "${_todayWordCount.value}/$dailyGoal 词"
    }
}
