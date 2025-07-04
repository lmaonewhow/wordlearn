package com.example.wordlearn.data.dao

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.example.wordlearn.data.AppDatabase
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.WordStatus
import java.time.LocalDate

private const val TAG = "VocabularyDaoImpl"
private const val BATCH_SIZE = 100 // 批量处理的大小

class VocabularyDaoImpl(
    private val database: AppDatabase
) : VocabularyDao {

    // 缓存所有单词，用于生成选项
    private var allWordsCache: List<Word>? = null
    private var lastCacheTime: Long = 0
    private val CACHE_DURATION = 5 * 60 * 1000 // 缓存有效期5分钟

    override suspend fun getAllWords(): List<Word> {
        // 检查缓存是否有效
        val now = System.currentTimeMillis()
        if (allWordsCache != null && (now - lastCacheTime) < CACHE_DURATION) {
            return allWordsCache!!
        }

        val words = mutableListOf<Word>()
        val db = database.readableDatabase
        
        val cursor = db.query(
            "words",
            null,
            null,
            null,
            null,
            null,
            null
        )
        
        cursor.use {
            while (it.moveToNext()) {
                words.add(cursor.toWord())
            }
        }
        
        // 更新缓存
        allWordsCache = words
        lastCacheTime = now
        
        Log.d(TAG, "从数据库获取到 ${words.size} 个单词")
        return words
    }

    override suspend fun insertWords(words: List<Word>) {
        val db = database.writableDatabase
        var successCount = 0
        
        db.beginTransaction()
        try {
            // 批量插入
            words.chunked(BATCH_SIZE).forEach { batch ->
                batch.forEach { word ->
                    val values = ContentValues().apply {
                        put("word", word.word)
                        put("meaning", word.meaning)
                        put("ukPhonetic", word.ukPhonetic)
                        put("usPhonetic", word.usPhonetic)
                        put("example", word.example)
                        put("status", word.status.name)
                        put("lastReviewDate", word.lastReviewDate?.toString())
                        put("nextReviewDate", word.nextReviewDate?.toString())
                        put("reviewCount", word.reviewCount)
                    }
                    if (db.insert("words", null, values) != -1L) {
                        successCount++
                    }
                }
            }
            
            db.setTransactionSuccessful()
            Log.i(TAG, "成功插入 $successCount 个单词")
            
            // 清除缓存，强制下次重新加载
            allWordsCache = null
            
        } catch (e: Exception) {
            Log.e(TAG, "插入单词时出错", e)
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun getWordsForReview(today: LocalDate): List<Word> {
        Log.d(TAG, "获取需要复习的单词，日期: $today")
        val words = mutableListOf<Word>()
        val db = database.readableDatabase
        
        // 只获取 NEEDS_REVIEW 状态且需要复习的单词
        val selection = "status = ? AND nextReviewDate <= ?"
        val selectionArgs = arrayOf(
            WordStatus.NEEDS_REVIEW.name,
            today.toString()
        )
        
        Log.d(TAG, "SQL查询条件: $selection")
        Log.d(TAG, "查询参数: ${selectionArgs.joinToString()}")
        
        val cursor = db.query(
            "words",
            null,
            selection,
            selectionArgs,
            null,
            null,
            "RANDOM()",
            "50"
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val word = cursor.toWord()
                Log.d(TAG, "找到需要复习的单词: ${word.word}, 状态: ${word.status}, 下次复习日期: ${word.nextReviewDate}")
                words.add(word)
            }
        }
        
        Log.d(TAG, "找到 ${words.size} 个需要复习的单词")
        return words
    }

    override suspend fun getTodayNewWords(count: Int): List<Word> {
        Log.d(TAG, "获取今日新单词，数量: $count")
        val words = mutableListOf<Word>()
        val db = database.readableDatabase
        
        // 获取状态为NEW的单词
        val selection = "status = ?"
        val selectionArgs = arrayOf(WordStatus.NEW.name)
        
        val cursor = db.query(
            "words",
            null,
            selection,
            selectionArgs,
            null,
            null,
            "RANDOM()",  // 随机排序
            count.toString()  // 限制数量
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val word = cursor.toWord()
                Log.d(TAG, "今日新单词: ${word.word}")
                words.add(word)
            }
        }
        
        Log.d(TAG, "获取到 ${words.size} 个今日新单词")
        return words
    }

    override suspend fun getTodayLearningWords(today: LocalDate): List<Word> {
        Log.d(TAG, "获取今日已学习单词，日期: $today")
        val words = mutableListOf<Word>()
        val db = database.readableDatabase
        
        // 获取今天学习过的单词 (状态为NEEDS_REVIEW且最后复习日期是今天)
        val selection = "status = ? AND lastReviewDate = ?"
        val selectionArgs = arrayOf(
            WordStatus.NEEDS_REVIEW.name,
            today.toString()
        )
        
        val cursor = db.query(
            "words",
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val word = cursor.toWord()
                Log.d(TAG, "今日学习单词: ${word.word}, 最后复习: ${word.lastReviewDate}")
                words.add(word)
            }
        }
        
        Log.d(TAG, "获取到 ${words.size} 个今日学习单词")
        return words
    }

    override suspend fun getPlannedReviewWords(
        today: LocalDate,
        intervalDays: List<Int>,
        limit: Int
    ): List<Word> {
        Log.d(TAG, "获取计划复习单词，日期: $today，间隔: $intervalDays，限制: $limit")
        val words = mutableListOf<Word>()
        val db = database.readableDatabase
        
        // 构建查询条件，获取复习次数对应间隔天数的单词
        var selection = "status = ? AND nextReviewDate <= ?"
        var selectionArgs = arrayOf(
            WordStatus.NEEDS_REVIEW.name,
            today.toString()
        )
        
        // 如果有指定间隔，添加复习次数条件
        if (intervalDays.isNotEmpty()) {
            // 构建复习次数条件：reviewCount IN (0, 1, 2, ...)
            val reviewCountCondition = intervalDays.indices.joinToString(", ")
            if (reviewCountCondition.isNotEmpty()) {
                selection += " AND reviewCount IN ($reviewCountCondition)"
            }
        }
        
        val cursor = db.query(
            "words",
            null,
            selection,
            selectionArgs,
            null,
            null,
            "nextReviewDate ASC", // 按复习日期升序
            limit.toString()
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val word = cursor.toWord()
                Log.d(TAG, "计划复习单词: ${word.word}, 复习次数: ${word.reviewCount}, 下次复习: ${word.nextReviewDate}")
                words.add(word)
            }
        }
        
        Log.d(TAG, "获取到 ${words.size} 个计划复习单词")
        return words
    }

    override suspend fun markWordAsLearning(
        wordId: Long,
        today: LocalDate,
        nextReviewDay: LocalDate
    ) {
        Log.d(TAG, "标记单词为学习中状态: wordId=$wordId, today=$today, nextReview=$nextReviewDay")
        val db = database.writableDatabase
        
        val values = ContentValues().apply {
            put("status", WordStatus.NEEDS_REVIEW.name)
            put("lastReviewDate", today.toString())
            put("nextReviewDate", nextReviewDay.toString())
            put("reviewCount", 0)
        }
        
        val updatedRows = db.update(
            "words",
            values,
            "id = ?",
            arrayOf(wordId.toString())
        )
        
        if (updatedRows > 0) {
            Log.d(TAG, "成功标记单词为学习中状态")
            // 状态更新后清除缓存
            allWordsCache = null
        } else {
            Log.w(TAG, "标记单词失败，可能单词ID不存在")
        }
    }

    override suspend fun updateNextReviewDate(
        wordId: Long,
        lastReviewDate: LocalDate,
        nextReviewDate: LocalDate,
        reviewCount: Int
    ): Boolean {
        Log.d(TAG, "更新下次复习日期: wordId=$wordId, lastReview=$lastReviewDate, nextReview=$nextReviewDate, reviewCount=$reviewCount")
        val db = database.writableDatabase
        
        val values = ContentValues().apply {
            put("lastReviewDate", lastReviewDate.toString())
            put("nextReviewDate", nextReviewDate.toString())
            put("reviewCount", reviewCount)
        }
        
        val updatedRows = db.update(
            "words",
            values,
            "id = ?",
            arrayOf(wordId.toString())
        )
        
        val success = updatedRows > 0
        
        if (success) {
            Log.d(TAG, "成功更新下次复习日期")
            // 状态更新后清除缓存
            allWordsCache = null
        } else {
            Log.w(TAG, "更新下次复习日期失败")
        }
        
        return success
    }

    override suspend fun getReviewCount(wordId: Long): Int {
        Log.d(TAG, "获取单词复习次数: wordId=$wordId")
        val db = database.readableDatabase
        
        val cursor = db.query(
            "words",
            arrayOf("reviewCount"),
            "id = ?",
            arrayOf(wordId.toString()),
            null,
            null,
            null
        )
        
        return cursor.use {
            if (it.moveToFirst()) {
                val count = it.getInt(0)
                Log.d(TAG, "单词复习次数: $count")
                count
            } else {
                Log.d(TAG, "未找到单词的复习次数记录")
                0
            }
        }
    }

    override suspend fun updateWordStatus(
        wordId: Long,
        status: WordStatus,
        lastReviewDate: LocalDate,
        nextReviewDate: LocalDate,
        reviewCount: Int
    ) {
        Log.d(TAG, "更新单词状态: wordId=$wordId, status=$status, lastReviewDate=$lastReviewDate, nextReviewDate=$nextReviewDate, reviewCount=$reviewCount")
        val db = database.writableDatabase
        
        val values = ContentValues().apply {
            put("status", status.name)
            put("lastReviewDate", lastReviewDate.toString())
            put("nextReviewDate", nextReviewDate.toString())
            put("reviewCount", reviewCount)
        }
        
        val updatedRows = db.update(
            "words",
            values,
            "id = ?",
            arrayOf(wordId.toString())
        )
        
        if (updatedRows > 0) {
            // 状态更新后清除缓存
            allWordsCache = null
        }
        
        Log.d(TAG, "更新了 $updatedRows 行记录")
    }

    override suspend fun getTodayNewWordsCount(today: LocalDate): Int {
        Log.d(TAG, "获取今天的新单词数量")
        val db = database.readableDatabase
        
        // 手动清除缓存，确保获取最新数据
        allWordsCache = null
        
        val cursor = db.query(
            "words",
            arrayOf("COUNT(*)"),
            "status = ? AND lastReviewDate IS NULL",
            arrayOf(WordStatus.NEW.name),
            null,
            null,
            null
        )
        
        return cursor.use {
            if (it.moveToFirst()) {
                val count = it.getInt(0)
                Log.d(TAG, "今天有 $count 个新单词")
                count
            } else {
                Log.d(TAG, "没有找到新单词")
                0
            }
        }
    }

    override suspend fun getTodayReviewWordsCount(today: LocalDate): Int {
        Log.d(TAG, "获取今天需要复习的单词数量")
        val db = database.readableDatabase
        
        // 手动清除缓存，确保获取最新数据
        allWordsCache = null
        
        // 修改查询条件：包含状态为NEEDS_REVIEW且nextReviewDate <= today的单词
        // 同时添加日志以便追踪
        val todayStr = today.toString()
        Log.d(TAG, "查询条件: status = NEEDS_REVIEW AND nextReviewDate <= $todayStr")
        
        val cursor = db.query(
            "words",
            arrayOf("COUNT(*)"),
            "status = ? AND nextReviewDate <= ?",
            arrayOf(WordStatus.NEEDS_REVIEW.name, todayStr),
            null,
            null,
            null
        )
        
        val count = cursor.use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else {
                0
            }
        }
        
        Log.d(TAG, "今天需要复习 $count 个单词")
        
        // 额外检查：打印几个最近添加到复习队列的单词详情
        if (count == 0) {
            try {
                Log.d(TAG, "当前需要复习的单词数为0，检查数据库中的单词状态")
                val debugCursor = db.query(
                    "words",
                    arrayOf("id", "word", "status", "nextReviewDate", "lastReviewDate"),
                    "status = ?",
                    arrayOf(WordStatus.NEEDS_REVIEW.name),
                    null,
                    null,
                    "id DESC",
                    "5"  // 只检查最近5个
                )
                
                debugCursor.use { cursor ->
                    if (cursor.moveToFirst()) {
                        do {
                            val id = cursor.getLong(0)
                            val word = cursor.getString(1)
                            val status = cursor.getString(2)
                            val nextReview = cursor.getString(3)
                            val lastReview = cursor.getString(4)
                            Log.d(TAG, "NEEDS_REVIEW状态单词: ID=$id, 单词=$word, 状态=$status, 下次复习=$nextReview, 最后复习=$lastReview")
                        } while (cursor.moveToNext())
                    } else {
                        Log.d(TAG, "数据库中没有NEEDS_REVIEW状态的单词")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "调试查询失败", e)
            }
        }
        
        return count
    }

    private fun Cursor.toWord(): Word {
        return Word(
            id = getLong(getColumnIndexOrThrow("id")),
            word = getString(getColumnIndexOrThrow("word")),
            meaning = getString(getColumnIndexOrThrow("meaning")),
            ukPhonetic = getString(getColumnIndexOrThrow("ukPhonetic")),
            usPhonetic = getString(getColumnIndexOrThrow("usPhonetic")),
            example = getString(getColumnIndexOrThrow("example")),
            status = WordStatus.valueOf(getString(getColumnIndexOrThrow("status"))),
            lastReviewDate = getString(getColumnIndexOrThrow("lastReviewDate"))?.let { LocalDate.parse(it) },
            nextReviewDate = getString(getColumnIndexOrThrow("nextReviewDate"))?.let { LocalDate.parse(it) },
            reviewCount = getInt(getColumnIndexOrThrow("reviewCount")),
            isFavorite = try {
                getInt(getColumnIndexOrThrow("isFavorite")) == 1
            } catch (e: Exception) {
                Log.w(TAG, "isFavorite列不存在，使用默认值false")
                false
            },
            errorCount = try {
                getInt(getColumnIndexOrThrow("errorCount"))
            } catch (e: Exception) {
                Log.w(TAG, "errorCount列不存在，使用默认值0")
                0
            }
        )
    }

    override suspend fun getWordById(wordId: Long): Word? {
        val db = database.readableDatabase
        
        val cursor = db.query(
            "words",
            null,
            "id = ?",
            arrayOf(wordId.toString()),
            null,
            null,
            null
        )
        
        return cursor.use {
            if (it.moveToFirst()) {
                cursor.toWord()
            } else {
                null
            }
        }
    }

    override suspend fun updateFavoriteStatus(wordId: Long, isFavorite: Boolean) {
        try {
            val db = database.writableDatabase
            
            val values = ContentValues().apply {
                put("isFavorite", if (isFavorite) 1 else 0)
            }
            
            val rowsAffected = db.update(
                "words",
                values,
                "id = ?",
                arrayOf(wordId.toString())
            )
            
            Log.i(TAG, "更新单词收藏状态: ID=$wordId, 收藏=$isFavorite, 结果=${rowsAffected > 0}")
            
            // 清除缓存
            allWordsCache = null
            
        } catch (e: Exception) {
            Log.e(TAG, "更新单词收藏状态时出错: ${e.message}", e)
        }
    }

    override suspend fun updateErrorCount(wordId: Long, errorCount: Int) {
        Log.d(TAG, "更新单词错误次数: wordId=$wordId, errorCount=$errorCount")
        try {
            val db = database.writableDatabase
            
            // 确保事务完整性
            db.beginTransaction()
            try {
                // 首先检查errorCount列是否存在
                val cursor = db.rawQuery("PRAGMA table_info(words)", null)
                val columnExists = cursor.use { c ->
                    var exists = false
                    while (c.moveToNext()) {
                        val columnName = c.getString(c.getColumnIndex("name"))
                        if (columnName == "errorCount") {
                            exists = true
                            break
                        }
                    }
                    exists
                }
                
                // 如果列不存在，添加列
                if (!columnExists) {
                    Log.w(TAG, "errorCount列不存在，正在添加")
                    db.execSQL("ALTER TABLE words ADD COLUMN errorCount INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_error ON words(errorCount)")
                }
                
                val values = ContentValues().apply {
                    put("errorCount", errorCount)
                }
                
                val updatedRows = db.update(
                    "words",
                    values,
                    "id = ?",
                    arrayOf(wordId.toString())
                )
                
                if (updatedRows > 0) {
                    Log.d(TAG, "成功更新单词错误次数: wordId=$wordId, errorCount=$errorCount")
                } else {
                    Log.w(TAG, "未找到要更新的单词: wordId=$wordId")
                }
                
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            
            // 状态更新后清除缓存
            allWordsCache = null
            
        } catch (e: Exception) {
            Log.e(TAG, "更新错误次数时出错: ${e.message}", e)
            // 创建索引以修复问题
            try {
                val db = database.writableDatabase
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_error ON words(errorCount)")
            } catch (indexEx: Exception) {
                Log.e(TAG, "创建错误索引失败", indexEx)
            }
        }
    }

    override suspend fun getFavoriteWords(): List<Word> {
        Log.d(TAG, "获取收藏单词列表")
        val words = mutableListOf<Word>()
        try {
            val db = database.writableDatabase
            
            // 首先检查isFavorite列是否存在
            val cursor = db.rawQuery("PRAGMA table_info(words)", null)
            val columnNames = mutableListOf<String>()
            cursor.use {
                while (it.moveToNext()) {
                    val columnName = it.getString(it.getColumnIndex("name"))
                    columnNames.add(columnName)
                }
            }
            
            // 如果列不存在，添加列
            if (!columnNames.contains("isFavorite")) {
                Log.w(TAG, "isFavorite列不存在，正在添加")
                db.execSQL("ALTER TABLE words ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                // 创建索引
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_favorite ON words(isFavorite)")
                
                // 如果刚添加列，直接返回空列表，因为还没有收藏的单词
                Log.d(TAG, "刚添加isFavorite列，暂无收藏单词")
                return emptyList()
            }
            
            // 查询收藏单词
            val queryCursor = db.query(
                "words",
                null,
                "isFavorite = ?",
                arrayOf("1"),
                null,
                null,
                null
            )
            
            queryCursor.use {
                while (it.moveToNext()) {
                    words.add(queryCursor.toWord())
                }
            }
            
            Log.d(TAG, "找到 ${words.size} 个收藏单词")
        } catch (e: Exception) {
            Log.e(TAG, "获取收藏单词列表失败: ${e.message}", e)
        }
        return words
    }

    override suspend fun getErrorWords(): List<Word> {
        Log.d(TAG, "获取错题列表")
        val words = mutableListOf<Word>()
        try {
            val db = database.writableDatabase
            
            // 首先检查errorCount列是否存在
            val cursor = db.rawQuery("PRAGMA table_info(words)", null)
            val columnNames = mutableListOf<String>()
            cursor.use {
                while (it.moveToNext()) {
                    val columnName = it.getString(it.getColumnIndex("name"))
                    columnNames.add(columnName)
                }
            }
            
            // 如果列不存在，添加列
            if (!columnNames.contains("errorCount")) {
                Log.w(TAG, "errorCount列不存在，正在添加")
                db.execSQL("ALTER TABLE words ADD COLUMN errorCount INTEGER NOT NULL DEFAULT 0")
                // 创建索引
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_words_error ON words(errorCount)")
                
                // 如果刚添加列，直接返回空列表，因为还没有错题
                Log.d(TAG, "刚添加errorCount列，暂无错题")
                return emptyList()
            }
            
            // 查询错题
            val queryCursor = db.query(
                "words",
                null,
                "errorCount > ?",
                arrayOf("0"),
                null,
                null,
                "errorCount DESC" // 按错误次数降序排序
            )
            
            queryCursor.use {
                while (it.moveToNext()) {
                    words.add(queryCursor.toWord())
                }
            }
            
            Log.d(TAG, "找到 ${words.size} 个错题")
        } catch (e: Exception) {
            Log.e(TAG, "获取错题列表失败: ${e.message}", e)
        }
        return words
    }

    override suspend fun getWordByText(wordText: String): Word? {
        try {
            val db = database.readableDatabase
            
            val cursor = db.query(
                "words",
                null,
                "word = ?",
                arrayOf(wordText),
                null,
                null,
                null,
                "1" // 限制只返回一条记录
            )
            
            return cursor.use { c ->
                if (c.moveToFirst()) {
                    c.toWord()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "根据文本查询单词失败: $wordText", e)
            return null
        }
    }

    override suspend fun resetAllWordsStatus() {
        try {
            val db = database.writableDatabase
            
            db.beginTransaction()
            try {
                // 更新所有非KNOWN状态的单词
                val values = ContentValues().apply {
                    put("status", WordStatus.NEW.name)
                    putNull("lastReviewDate")
                    putNull("nextReviewDate")
                    put("reviewCount", 0)
                }
                
                val updatedRows = db.update(
                    "words",
                    values,
                    "status != ?",
                    arrayOf(WordStatus.KNOWN.name)
                )
                
                Log.i(TAG, "已重置 $updatedRows 个单词的学习状态")
                
                db.setTransactionSuccessful()
                
                // 清除缓存
                allWordsCache = null
                
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "重置单词学习状态时出错: ${e.message}", e)
        }
    }

    override suspend fun clearLearningProgress() {
        try {
            val db = database.writableDatabase
            
            db.beginTransaction()
            try {
                // 重置所有非KNOWN状态单词的学习进度
                val values = ContentValues().apply {
                    put("status", WordStatus.NEW.name)
                    putNull("lastReviewDate")
                    putNull("nextReviewDate")
                    put("reviewCount", 0)
                }
                
                val updatedRows = db.update(
                    "words",
                    values,
                    "status != ?",
                    arrayOf(WordStatus.KNOWN.name)
                )
                
                Log.i(TAG, "已清理 $updatedRows 个单词的学习进度")
                
                db.setTransactionSuccessful()
                
                // 清除缓存
                allWordsCache = null
                
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理学习进度时出错: ${e.message}", e)
        }
    }

    override suspend fun updateLastModifiedTime(timestamp: Long) {
        try {
            val db = database.writableDatabase
            
            db.beginTransaction()
            try {
                // 检查lastModified列是否存在
                val cursor = db.rawQuery("PRAGMA table_info(words)", null)
                val columnExists = cursor.use { c ->
                    var exists = false
                    while (c.moveToNext()) {
                        val columnName = c.getString(c.getColumnIndex("name"))
                        if (columnName == "lastModified") {
                            exists = true
                            break
                        }
                    }
                    exists
                }
                
                // 如果列不存在，添加列
                if (!columnExists) {
                    Log.w(TAG, "lastModified列不存在，正在添加")
                    db.execSQL("ALTER TABLE words ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                }
                
                val values = ContentValues().apply {
                    put("lastModified", timestamp)
                }
                
                val updatedRows = db.update(
                    "words",
                    values,
                    null,  // 更新所有记录
                    null
                )
                
                Log.i(TAG, "已更新 $updatedRows 个单词的最后修改时间")
                
                db.setTransactionSuccessful()
                
                // 清除缓存
                allWordsCache = null
                
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新最后修改时间时出错: ${e.message}", e)
        }
    }

    override suspend fun clearAllWords() {
        try {
            val db = database.writableDatabase
            
            db.beginTransaction()
            try {
                // 删除所有单词数据
                val deletedRows = db.delete("words", null, null)
                
                Log.i(TAG, "已清空单词表，删除了 $deletedRows 条记录")
                
                db.setTransactionSuccessful()
                
                // 清除缓存
                allWordsCache = null
                
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "清空单词表时出错: ${e.message}", e)
        }
    }
} 