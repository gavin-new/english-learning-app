package com.englishlearning.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.englishlearning.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Word::class,
        Article::class,
        LearningProgress::class,
        UserStats::class,
        ListeningMaterial::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun learningProgressDao(): LearningProgressDao
    abstract fun userStatsDao(): UserStatsDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "english_learning_db"
                )
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
    
    // 数据库创建时的回调，预填充数据
    private class DatabaseCallback(
        private val context: Context
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.wordDao(), database.userStatsDao(), context)
                }
            }
        }
        
        suspend fun populateDatabase(
            wordDao: WordDao, 
            userStatsDao: UserStatsDao,
            context: Context
        ) {
            // 从assets加载所有词汇数据
            val allWords = VocabularyLoader.loadAllVocabulary(context)
            if (allWords.isNotEmpty()) {
                wordDao.insertWords(allWords)
            }
            
            // 创建今日统计数据
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            userStatsDao.insertOrUpdate(UserStats(date = today))
        }
    }
}
