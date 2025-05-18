package com.example.wordlearn.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.wordlearn.data.dao.VocabularyDao
import com.example.wordlearn.data.dao.VocabularyDaoImpl

/**
 * 应用数据库类
 */
class AppDatabase private constructor(context: Context) : 
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // 获取 DAO
    fun vocabularyDao(): VocabularyDao {
        return VocabularyDaoImpl(this)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建单词表
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS words (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                word TEXT NOT NULL,
                meaning TEXT NOT NULL,
                ukPhonetic TEXT,
                usPhonetic TEXT,
                example TEXT,
                status TEXT NOT NULL,
                lastReviewDate TEXT,
                nextReviewDate TEXT,
                reviewCount INTEGER NOT NULL DEFAULT 0
            )
        """)
        
        // 创建索引以优化查询性能
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_status ON words(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_next_review ON words(nextReviewDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_status_next_review ON words(status, nextReviewDate)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 简单的升级策略：删除旧表，创建新表
        db.execSQL("DROP TABLE IF EXISTS words")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "word_database"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
} 