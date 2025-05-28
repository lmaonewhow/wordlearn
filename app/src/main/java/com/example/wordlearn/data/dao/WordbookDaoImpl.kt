package com.example.wordlearn.data.dao

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.example.wordlearn.data.AppDatabase
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.Wordbook
import com.example.wordlearn.data.model.WordbookType
import com.example.wordlearn.data.model.WordStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "WordbookDaoImpl"
private const val BATCH_SIZE = 100 // 批量处理的大小

class WordbookDaoImpl(
    private val database: AppDatabase
) : WordbookDao {

    // 缓存词书列表，用于减少数据库访问
    private var wordbooksCache: List<Wordbook>? = null
    private var lastCacheTime: Long = 0
    private val CACHE_DURATION = 5 * 60 * 1000 // 缓存有效期5分钟

    // 将Cursor转换为Wordbook对象
    private fun Cursor.toWordbook(): Wordbook {
        return Wordbook(
            id = getLong(getColumnIndexOrThrow("id")),
            name = getString(getColumnIndexOrThrow("name")),
            description = getString(getColumnIndexOrThrow("description") ?: 0).orEmpty(),
            sourcePath = getString(getColumnIndexOrThrow("source_path") ?: 0).orEmpty(),
            type = WordbookType.valueOf(getString(getColumnIndexOrThrow("type"))),
            totalWords = getInt(getColumnIndexOrThrow("total_words")),
            newWordsCount = getInt(getColumnIndexOrThrow("new_words_count")),
            reviewWordsCount = getInt(getColumnIndexOrThrow("review_words_count")),
            learnedWordsCount = getInt(getColumnIndexOrThrow("learned_words_count")),
            isFavorite = getInt(getColumnIndexOrThrow("is_favorite")) == 1,
            isActive = getInt(getColumnIndexOrThrow("is_active")) == 1,
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
            lastUpdated = getLong(getColumnIndexOrThrow("last_updated"))
        )
    }

    override suspend fun getAllWordbooks(): List<Wordbook> {
        // 检查缓存是否有效
        val now = System.currentTimeMillis()
        if (wordbooksCache != null && (now - lastCacheTime) < CACHE_DURATION) {
            return wordbooksCache!!
        }

        val wordbooks = mutableListOf<Wordbook>()
        val db = database.readableDatabase
        
        val cursor = db.query(
            "wordbooks",
            null,
            null,
            null,
            null,
            null,
            "name ASC" // 按名称排序
        )
        
        cursor.use {
            while (it.moveToNext()) {
                wordbooks.add(it.toWordbook())
            }
        }
        
        // 更新缓存
        wordbooksCache = wordbooks
        lastCacheTime = now
        
        Log.d(TAG, "从数据库获取到 ${wordbooks.size} 本词书")
        return wordbooks
    }

    override suspend fun getActiveWordbook(): Wordbook? {
        val db = database.readableDatabase
        
        val cursor = db.query(
            "wordbooks",
            null,
            "is_active = 1",
            null,
            null,
            null,
            null,
            "1" // 限制为1条结果
        )
        
        return cursor.use {
            if (it.moveToFirst()) {
                it.toWordbook()
            } else {
                null
            }
        }
    }

    override suspend fun createWordbook(wordbook: Wordbook): Long {
        val db = database.writableDatabase
        
        val values = ContentValues().apply {
            put("name", wordbook.name)
            put("description", wordbook.description)
            put("source_path", wordbook.sourcePath)
            put("type", wordbook.type.name)
            put("total_words", wordbook.totalWords)
            put("new_words_count", wordbook.newWordsCount)
            put("review_words_count", wordbook.reviewWordsCount)
            put("learned_words_count", wordbook.learnedWordsCount)
            put("is_favorite", if (wordbook.isFavorite) 1 else 0)
            put("is_active", if (wordbook.isActive) 1 else 0)
            put("created_at", wordbook.createdAt)
            put("last_updated", System.currentTimeMillis())
        }
        
        val id = db.insert("wordbooks", null, values)
        
        // 如果该词书是活跃的，确保其他词书不是活跃状态
        if (wordbook.isActive && id != -1L) {
            db.execSQL(
                "UPDATE wordbooks SET is_active = 0 WHERE id != ?",
                arrayOf(id.toString())
            )
        }
        
        // 清除缓存
        wordbooksCache = null
        
        Log.d(TAG, "创建词书：${wordbook.name}, ID=$id")
        return id
    }

    override suspend fun updateWordbook(wordbook: Wordbook): Boolean {
        val db = database.writableDatabase
        
        val values = ContentValues().apply {
            put("name", wordbook.name)
            put("description", wordbook.description)
            put("source_path", wordbook.sourcePath)
            put("type", wordbook.type.name)
            put("total_words", wordbook.totalWords)
            put("new_words_count", wordbook.newWordsCount)
            put("review_words_count", wordbook.reviewWordsCount)
            put("learned_words_count", wordbook.learnedWordsCount)
            put("is_favorite", if (wordbook.isFavorite) 1 else 0)
            put("is_active", if (wordbook.isActive) 1 else 0)
            put("last_updated", System.currentTimeMillis())
        }
        
        val rowsAffected = db.update(
            "wordbooks",
            values,
            "id = ?",
            arrayOf(wordbook.id.toString())
        )
        
        // 如果该词书是活跃的，确保其他词书不是活跃状态
        if (wordbook.isActive && rowsAffected > 0) {
            db.execSQL(
                "UPDATE wordbooks SET is_active = 0 WHERE id != ?",
                arrayOf(wordbook.id.toString())
            )
        }
        
        // 清除缓存
        wordbooksCache = null
        
        Log.d(TAG, "更新词书：${wordbook.name}, ID=${wordbook.id}, 结果=${rowsAffected > 0}")
        return rowsAffected > 0
    }

    override suspend fun deleteWordbook(wordbookId: Long): Boolean {
        val db = database.writableDatabase
        
        // 先删除该词书中的所有单词和学习记录（通过外键约束自动删除）
        val rowsAffected = db.delete(
            "wordbooks",
            "id = ?",
            arrayOf(wordbookId.toString())
        )
        
        // 清除缓存
        wordbooksCache = null
        
        Log.d(TAG, "删除词书：ID=$wordbookId, 结果=${rowsAffected > 0}")
        return rowsAffected > 0
    }

    override suspend fun setActiveWordbook(wordbookId: Long): Boolean {
        val db = database.writableDatabase
        
        db.beginTransaction()
        try {
            // 先将所有词书设为非活跃
            db.execSQL("UPDATE wordbooks SET is_active = 0")
            
            // 再将指定词书设为活跃
            val values = ContentValues().apply {
                put("is_active", 1)
            }
            
            val rowsAffected = db.update(
                "wordbooks",
                values,
                "id = ?",
                arrayOf(wordbookId.toString())
            )
            
            db.setTransactionSuccessful()
            
            // 清除缓存
            wordbooksCache = null
            
            Log.d(TAG, "设置词书活跃状态：ID=$wordbookId, 结果=${rowsAffected > 0}")
            return rowsAffected > 0
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun updateWordbookStats(wordbookId: Long): Boolean {
        val db = database.writableDatabase
        
        db.beginTransaction()
        try {
            // 获取词书中所有单词的数量
            val totalWordsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM words WHERE wordbook_id = ?",
                arrayOf(wordbookId.toString())
            )
            
            val totalWords = totalWordsCursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            
            // 获取新单词数量
            val newWordsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM words WHERE wordbook_id = ? AND status = ?",
                arrayOf(wordbookId.toString(), WordStatus.NEW.name)
            )
            
            val newWordsCount = newWordsCursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            
            // 获取待复习单词数量
            val today = LocalDate.now().toString()
            val reviewWordsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM words WHERE wordbook_id = ? AND status = ? AND nextReviewDate <= ?",
                arrayOf(wordbookId.toString(), WordStatus.NEEDS_REVIEW.name, today)
            )
            
            val reviewWordsCount = reviewWordsCursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            
            // 获取已学习单词数量
            val learnedWordsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM words WHERE wordbook_id = ? AND status != ?",
                arrayOf(wordbookId.toString(), WordStatus.NEW.name)
            )
            
            val learnedWordsCount = learnedWordsCursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            
            // 更新词书统计信息
            val values = ContentValues().apply {
                put("total_words", totalWords)
                put("new_words_count", newWordsCount)
                put("review_words_count", reviewWordsCount)
                put("learned_words_count", learnedWordsCount)
                put("last_updated", System.currentTimeMillis())
            }
            
            val rowsAffected = db.update(
                "wordbooks",
                values,
                "id = ?",
                arrayOf(wordbookId.toString())
            )
            
            db.setTransactionSuccessful()
            
            // 清除缓存
            wordbooksCache = null
            
            Log.d(TAG, "更新词书统计信息：ID=$wordbookId, 总单词=$totalWords, 新单词=$newWordsCount, 待复习=$reviewWordsCount, 已学习=$learnedWordsCount")
            return rowsAffected > 0
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun importWordsToWordbook(wordbookId: Long, words: List<Word>): Int {
        val db = database.writableDatabase
        var successCount = 0
        
        db.beginTransaction()
        try {
            // 批量插入
            words.chunked(BATCH_SIZE).forEach { batch ->
                batch.forEach { word ->
                    val values = ContentValues().apply {
                        put("wordbook_id", wordbookId)
                        put("word", word.word)
                        put("meaning", word.meaning)
                        put("ukPhonetic", word.ukPhonetic)
                        put("usPhonetic", word.usPhonetic)
                        put("example", word.example)
                        put("status", word.status.name)
                        put("lastReviewDate", word.lastReviewDate?.toString())
                        put("nextReviewDate", word.nextReviewDate?.toString())
                        put("reviewCount", word.reviewCount)
                        put("isFavorite", if (word.isFavorite) 1 else 0)
                        put("errorCount", word.errorCount)
                        put("lastModified", System.currentTimeMillis())
                    }
                    if (db.insert("words", null, values) != -1L) {
                        successCount++
                    }
                }
            }
            
            // 更新词书统计信息
            if (successCount > 0) {
                val totalWordsCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM words WHERE wordbook_id = ?",
                    arrayOf(wordbookId.toString())
                )
                
                val totalWords = totalWordsCursor.use {
                    if (it.moveToFirst()) it.getInt(0) else 0
                }
                
                val values = ContentValues().apply {
                    put("total_words", totalWords)
                    put("new_words_count", totalWords) // 导入的单词默认都是新单词
                    put("last_updated", System.currentTimeMillis())
                }
                
                db.update(
                    "wordbooks",
                    values,
                    "id = ?",
                    arrayOf(wordbookId.toString())
                )
            }
            
            db.setTransactionSuccessful()
            Log.i(TAG, "成功导入 $successCount 个单词到词书 ID=$wordbookId")
            
        } catch (e: Exception) {
            Log.e(TAG, "导入单词时出错", e)
        } finally {
            db.endTransaction()
        }
        
        return successCount
    }

    override suspend fun getWordsInWordbook(wordbookId: Long): List<Word> {
        val db = database.readableDatabase
        val words = mutableListOf<Word>()
        
        val cursor = db.query(
            "words",
            null,
            "wordbook_id = ?",
            arrayOf(wordbookId.toString()),
            null,
            null,
            null
        )
        
        cursor.use {
            while (it.moveToNext()) {
                words.add(cursorToWord(it))
            }
        }
        
        Log.d(TAG, "获取词书中所有单词：ID=$wordbookId, 单词数量=${words.size}")
        return words
    }

    // 辅助方法：将Cursor转换为Word对象
    private fun cursorToWord(cursor: Cursor): Word {
        val lastReviewDateStr = cursor.getString(cursor.getColumnIndexOrThrow("lastReviewDate"))
        val nextReviewDateStr = cursor.getString(cursor.getColumnIndexOrThrow("nextReviewDate"))
        
        val lastReviewDate = if (lastReviewDateStr.isNullOrEmpty()) null 
                           else LocalDate.parse(lastReviewDateStr)
        
        val nextReviewDate = if (nextReviewDateStr.isNullOrEmpty()) null 
                           else LocalDate.parse(nextReviewDateStr)
        
        return Word(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            word = cursor.getString(cursor.getColumnIndexOrThrow("word")),
            meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning")),
            ukPhonetic = cursor.getString(cursor.getColumnIndexOrThrow("ukPhonetic") ?: 0).orEmpty(),
            usPhonetic = cursor.getString(cursor.getColumnIndexOrThrow("usPhonetic") ?: 0).orEmpty(),
            example = cursor.getString(cursor.getColumnIndexOrThrow("example") ?: 0).orEmpty(),
            status = WordStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
            lastReviewDate = lastReviewDate,
            nextReviewDate = nextReviewDate,
            reviewCount = cursor.getInt(cursor.getColumnIndexOrThrow("reviewCount")),
            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite")) == 1,
            errorCount = cursor.getInt(cursor.getColumnIndexOrThrow("errorCount"))
        )
    }

    override suspend fun getNewWordsInWordbook(wordbookId: Long): List<Word> {
        val db = database.readableDatabase
        val words = mutableListOf<Word>()
        
        val cursor = db.query(
            "words",
            null,
            "wordbook_id = ? AND status = ?",
            arrayOf(wordbookId.toString(), WordStatus.NEW.name),
            null,
            null,
            null
        )
        
        cursor.use {
            while (it.moveToNext()) {
                words.add(cursorToWord(it))
            }
        }
        
        Log.d(TAG, "获取词书中待学习单词：ID=$wordbookId, 单词数量=${words.size}")
        return words
    }

    override suspend fun getNewWordsCountInWordbook(wordbookId: Long): Int {
        val db = database.readableDatabase
        
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM words WHERE wordbook_id = ? AND status = ?",
            arrayOf(wordbookId.toString(), WordStatus.NEW.name)
        )
        
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    override suspend fun getTodayNewLearnedWordsInWordbook(wordbookId: Long, today: LocalDate): List<Word> {
        val db = database.readableDatabase
        val words = mutableListOf<Word>()
        
        val todayStr = today.toString()
        
        val cursor = db.query(
            "words",
            null,
            "wordbook_id = ? AND status = ? AND lastReviewDate IS NULL",
            arrayOf(wordbookId.toString(), WordStatus.NEW.name),
            null,
            null,
            null
        )
        
        cursor.use {
            while (it.moveToNext()) {
                words.add(cursorToWord(it))
            }
        }
        
        Log.d(TAG, "获取词书中今日新学单词：ID=$wordbookId, 日期=$today, 单词数量=${words.size}")
        return words
    }

    override suspend fun getTodayNewLearnedWordsCountInWordbook(wordbookId: Long, today: LocalDate): Int {
        val db = database.readableDatabase
        
        val todayStr = today.toString()
        
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM words WHERE wordbook_id = ? AND status = ? AND lastReviewDate IS NULL",
            arrayOf(wordbookId.toString(), WordStatus.NEW.name)
        )
        
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    override suspend fun getReviewWordsInWordbook(wordbookId: Long, today: LocalDate): List<Word> {
        val db = database.readableDatabase
        val words = mutableListOf<Word>()
        
        val todayStr = today.toString()
        
        val cursor = db.query(
            "words",
            null,
            "wordbook_id = ? AND status = ? AND nextReviewDate <= ?",
            arrayOf(wordbookId.toString(), WordStatus.NEEDS_REVIEW.name, todayStr),
            null,
            null,
            null
        )
        
        cursor.use {
            while (it.moveToNext()) {
                words.add(cursorToWord(it))
            }
        }
        
        Log.d(TAG, "获取词书中待复习单词：ID=$wordbookId, 日期=$today, 单词数量=${words.size}")
        return words
    }

    override suspend fun getReviewWordsCountInWordbook(wordbookId: Long, today: LocalDate): Int {
        val db = database.readableDatabase
        
        val todayStr = today.toString()
        
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM words WHERE wordbook_id = ? AND status = ? AND nextReviewDate <= ?",
            arrayOf(wordbookId.toString(), WordStatus.NEEDS_REVIEW.name, todayStr)
        )
        
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    override suspend fun getTodayLearnedWordsInWordbook(wordbookId: Long, today: LocalDate): List<Word> {
        val db = database.readableDatabase
        val words = mutableListOf<Word>()
        
        val todayStr = today.toString()
        
        val cursor = db.query(
            "words",
            null,
            "wordbook_id = ? AND status = ? AND lastReviewDate = ?",
            arrayOf(wordbookId.toString(), WordStatus.NEEDS_REVIEW.name, todayStr),
            null,
            null,
            null
        )
        
        cursor.use {
            while (it.moveToNext()) {
                words.add(cursorToWord(it))
            }
        }
        
        Log.d(TAG, "获取词书中今日已学习单词：ID=$wordbookId, 日期=$today, 单词数量=${words.size}")
        return words
    }

    override suspend fun getTodayLearnedWordsCountInWordbook(wordbookId: Long, today: LocalDate): Int {
        val db = database.readableDatabase
        
        val todayStr = today.toString()
        
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM words WHERE wordbook_id = ? AND status = ? AND lastReviewDate = ?",
            arrayOf(wordbookId.toString(), WordStatus.NEEDS_REVIEW.name, todayStr)
        )
        
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    override suspend fun resetWordsStatusInWordbook(wordbookId: Long): Int {
        val db = database.writableDatabase
        
        db.beginTransaction()
        try {
            // 更新该词书中所有非KNOWN状态的单词
            val values = ContentValues().apply {
                put("status", WordStatus.NEW.name)
                putNull("lastReviewDate")
                putNull("nextReviewDate")
                put("reviewCount", 0)
                put("lastModified", System.currentTimeMillis())
            }
            
            val updatedRows = db.update(
                "words",
                values,
                "wordbook_id = ? AND status != ?",
                arrayOf(wordbookId.toString(), WordStatus.KNOWN.name)
            )
            
            // 更新词书统计信息
            val wordbookValues = ContentValues().apply {
                put("new_words_count", updatedRows)
                put("review_words_count", 0)
                put("last_updated", System.currentTimeMillis())
            }
            
            db.update(
                "wordbooks",
                wordbookValues,
                "id = ?",
                arrayOf(wordbookId.toString())
            )
            
            db.setTransactionSuccessful()
            
            Log.i(TAG, "已重置词书中 $updatedRows 个单词的学习状态：ID=$wordbookId")
            
            return updatedRows
            
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun recordLearningActivity(
        wordId: Long,
        wordbookId: Long,
        learnDate: LocalDate,
        isCorrect: Boolean
    ): Long {
        val db = database.writableDatabase
        
        val values = ContentValues().apply {
            put("word_id", wordId)
            put("wordbook_id", wordbookId)
            put("learn_date", learnDate.toString())
            put("is_correct", if (isCorrect) 1 else 0)
            put("review_time", System.currentTimeMillis())
        }
        
        val id = db.insert("learning_records", null, values)
        
        Log.d(TAG, "记录学习行为：单词ID=$wordId, 词书ID=$wordbookId, 日期=$learnDate, 正确=${isCorrect}, 记录ID=$id")
        return id
    }

    override suspend fun getWordbookProgress(wordbookId: Long): Triple<Int, Int, Float> {
        val db = database.readableDatabase
        
        // 获取总单词数
        val totalWordsCursor = db.rawQuery(
            "SELECT COUNT(*) FROM words WHERE wordbook_id = ?",
            arrayOf(wordbookId.toString())
        )
        
        val totalWords = totalWordsCursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        
        // 获取已学习单词数
        val learnedWordsCursor = db.rawQuery(
            "SELECT COUNT(*) FROM words WHERE wordbook_id = ? AND status != ?",
            arrayOf(wordbookId.toString(), WordStatus.NEW.name)
        )
        
        val learnedWords = learnedWordsCursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        
        // 计算学习进度
        val progress = if (totalWords > 0) {
            learnedWords.toFloat() / totalWords.toFloat()
        } else {
            0f
        }
        
        Log.d(TAG, "获取词书学习进度：ID=$wordbookId, 已学习=$learnedWords, 总单词=$totalWords, 进度=$progress")
        return Triple(learnedWords, totalWords, progress)
    }
} 