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
            Log.d(TAG, "使用缓存的单词列表")
            return allWordsCache!!
        }

        Log.d(TAG, "从数据库获取所有单词")
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
        Log.d(TAG, "开始批量插入 ${words.size} 个单词到数据库")
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
            Log.d(TAG, "成功插入 $successCount 个单词")
            
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
        
        val cursor = db.query(
            "words",
            arrayOf("COUNT(*)"),
            "status = ? AND nextReviewDate <= ?",
            arrayOf(WordStatus.NEEDS_REVIEW.name, today.toString()),
            null,
            null,
            null
        )
        
        return cursor.use {
            if (it.moveToFirst()) {
                val count = it.getInt(0)
                Log.d(TAG, "今天需要复习 $count 个单词")
                count
            } else {
                Log.d(TAG, "没有找到需要复习的单词")
                0
            }
        }
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
            reviewCount = getInt(getColumnIndexOrThrow("reviewCount"))
        )
    }
} 