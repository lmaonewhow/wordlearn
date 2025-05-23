package com.example.wordlearn.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.wordlearn.data.dao.VocabularyDao
import com.example.wordlearn.data.dao.VocabularyDaoImpl

private const val TAG = "AppDatabase"

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
        Log.d(TAG, "创建数据库表")
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
                reviewCount INTEGER NOT NULL DEFAULT 0,
                isFavorite INTEGER NOT NULL DEFAULT 0,
                errorCount INTEGER NOT NULL DEFAULT 0,
                lastModified INTEGER NOT NULL DEFAULT 0
            )
        """)
        
        // 创建索引以优化查询性能
        createIndices(db)
        
        Log.d(TAG, "数据库表创建完成")
    }
    
    // 创建所有索引
    private fun createIndices(db: SQLiteDatabase) {
        Log.d(TAG, "创建数据库索引")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_status ON words(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_next_review ON words(nextReviewDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_status_next_review ON words(status, nextReviewDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_favorite ON words(isFavorite)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_error ON words(errorCount)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_last_modified ON words(lastModified)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "数据库升级: $oldVersion -> $newVersion")
        
        try {
            if (oldVersion < 2) {
                // 检查isFavorite列是否存在
                val cursor = db.rawQuery("PRAGMA table_info(words)", null)
                val columnNames = mutableListOf<String>()
                cursor.use {
                    while (it.moveToNext()) {
                        val columnName = it.getString(it.getColumnIndex("name"))
                        columnNames.add(columnName)
                    }
                }
                
                // 添加缺失的列
                if (!columnNames.contains("isFavorite")) {
                    Log.i(TAG, "添加isFavorite列")
                    db.execSQL("ALTER TABLE words ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                }
                
                if (!columnNames.contains("errorCount")) {
                    Log.i(TAG, "添加errorCount列")
                    db.execSQL("ALTER TABLE words ADD COLUMN errorCount INTEGER NOT NULL DEFAULT 0")
                }
                
                // 确保索引存在
                createIndices(db)
            }
            
            // 确保所有数据列都存在，如果有问题，重建表
            ensureTableStructure(db)
            
        } catch (e: Exception) {
            Log.e(TAG, "数据库升级失败", e)
            // 如果升级失败，备份数据并重建表
            recreateTable(db)
        }
    }
    
    // 确保表结构完整
    private fun ensureTableStructure(db: SQLiteDatabase) {
        try {
            Log.d(TAG, "检查数据库表结构")
            // 检查表结构
            val cursor = db.rawQuery("PRAGMA table_info(words)", null)
            val columnNames = mutableListOf<String>()
            cursor.use {
                while (it.moveToNext()) {
                    val columnName = it.getString(it.getColumnIndex("name"))
                    columnNames.add(columnName)
                }
            }
            
            // 必须包含的列
            val requiredColumns = listOf(
                "id", "word", "meaning", "status", 
                "lastReviewDate", "nextReviewDate", "reviewCount",
                "isFavorite", "errorCount"
            )
            
            // 检查是否缺少必需的列
            val missingColumns = requiredColumns.filter { !columnNames.contains(it) }
            if (missingColumns.isNotEmpty()) {
                Log.w(TAG, "数据库缺少列: ${missingColumns.joinToString()}")
                recreateTable(db)
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查表结构失败", e)
            recreateTable(db)
        }
    }
    
    // 重建表 - 备份数据，删除旧表，创建新表，恢复数据
    private fun recreateTable(db: SQLiteDatabase) {
        Log.w(TAG, "重建words表")
        
        try {
            // 1. 创建临时表
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS words_backup (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    word TEXT NOT NULL,
                    meaning TEXT NOT NULL,
                    ukPhonetic TEXT,
                    usPhonetic TEXT,
                    example TEXT,
                    status TEXT NOT NULL,
                    lastReviewDate TEXT,
                    nextReviewDate TEXT,
                    reviewCount INTEGER NOT NULL DEFAULT 0,
                    isFavorite INTEGER NOT NULL DEFAULT 0,
                    errorCount INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            // 2. 复制数据
            try {
                db.execSQL("""
                    INSERT INTO words_backup(word, meaning, ukPhonetic, usPhonetic, example, status, 
                    lastReviewDate, nextReviewDate, reviewCount)
                    SELECT word, meaning, ukPhonetic, usPhonetic, example, status, 
                    lastReviewDate, nextReviewDate, reviewCount FROM words
                """)
                Log.d(TAG, "成功备份words表数据")
            } catch (e: Exception) {
                Log.e(TAG, "备份数据失败", e)
            }
            
            // 3. 删除旧表
            db.execSQL("DROP TABLE IF EXISTS words")
            
            // 4. 创建新表
            onCreate(db)
            
            // 5. 恢复数据
            try {
                db.execSQL("""
                    INSERT INTO words(word, meaning, ukPhonetic, usPhonetic, example, status, 
                    lastReviewDate, nextReviewDate, reviewCount, isFavorite, errorCount)
                    SELECT word, meaning, ukPhonetic, usPhonetic, example, status, 
                    lastReviewDate, nextReviewDate, reviewCount, 0, 0 FROM words_backup
                """)
                Log.d(TAG, "成功恢复数据到words表")
            } catch (e: Exception) {
                Log.e(TAG, "恢复数据失败", e)
            }
            
            // 6. 删除临时表
            db.execSQL("DROP TABLE IF EXISTS words_backup")
            
            Log.i(TAG, "words表重建完成")
        } catch (e: Exception) {
            Log.e(TAG, "重建表失败", e)
            // 如果完全失败，则创建新的空表
            db.execSQL("DROP TABLE IF EXISTS words")
            onCreate(db)
        }
    }

    companion object {
        private const val DATABASE_NAME = "word_database"
        private const val DATABASE_VERSION = 4

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