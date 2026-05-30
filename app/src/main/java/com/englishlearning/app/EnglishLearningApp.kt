package com.englishlearning.app

import android.app.Application
import com.englishlearning.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EnglishLearningApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 触发DB初始化 + 词汇数据自动加载
        appScope.launch {
            val db = AppDatabase.getDatabase(this@EnglishLearningApp)
            // DB Callback会自动调用VocabularyLoader.loadAllVocabulary()
            // 此处确保DB已创建
            db.wordDao().getWordCount()
        }
    }
}
