package com.englishlearning.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.data.repository.WordRepository

/**
 * 年级/教材映射
 * 沪教版3-6年级: grade 11-18 映射到 category
 */
object GradeMapping {
    /** 年级名称映射 */
    val gradeNames = mapOf(
        0 to "全部",
        1 to "小学一年级", 2 to "小学二年级",
        3 to "小学三年级", 4 to "小学四年级",
        5 to "小学五年级", 6 to "小学六年级",
        7 to "中考词汇", 8 to "高考词汇",
        9 to "四级词汇", 10 to "六级词汇"
    )

    /** 沪教版教材映射 (grade → category) */
    val textbookMapping = mapOf(
        // 标准年级
        1 to "小学一年级", 2 to "小学二年级",
        3 to "小学三年级", 4 to "小学四年级",
        5 to "小学五年级", 6 to "小学六年级",
        7 to "中考词汇", 8 to "高考词汇",
        9 to "四级词汇", 10 to "六级词汇"
    )

    fun getGradeName(grade: Int): String = gradeNames[grade] ?: "年级$grade"
    fun getAllGrades(): List<Pair<Int, String>> = gradeNames.toList().sortedBy { it.first }
}

/**
 * 全局应用ViewModel — 提供所有子ViewModel的访问入口
 * 在Application中初始化一次, 通过CompositionLocal提供给UI
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    val homeVM = HomeViewModel(application)
    val vocabVM = VocabularyViewModel(application)
    val phonicsVM = PhonicsViewModel(application)
    val dictationVM = DictationViewModel(application)
    val searchVM = SearchViewModel(application)

    // ==================== 全局初始化 ====================

    init {
        viewModelScope.launch {
            // 初始化词汇数据 (首次启动从assets加载)
            homeVM.wordRepo.initialize()
            // 确保今日统计存在
            homeVM.progressRepo.ensureTodayStats()
            // 加载词典 (异步)
            homeVM.dictionaryRepo.ensureLoaded()
        }
    }
}
