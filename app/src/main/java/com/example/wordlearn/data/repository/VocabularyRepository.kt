package com.example.wordlearn.data.repository

import android.content.Context
import android.util.Log
import com.example.wordlearn.data.dao.VocabularyDao
import com.example.wordlearn.data.model.BookType
import com.example.wordlearn.data.model.VocabularyBook
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.WordStatus
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

private const val TAG = "VocabularyRepository"

class VocabularyRepository(
    private val context: Context,
    private val vocabularyDao: VocabularyDao
) {
    
    // 获取所有可用的词汇书
    fun getAvailableBooks(): List<VocabularyBook> {
        return listOf(
            VocabularyBook(
                id = "book1",
                name = "20天背完四级核心词汇",
                filePath = "m-word/20天背完四级核心词汇.csv",
                totalWords = 1200,
                type = BookType.CSV
            ),
            VocabularyBook(
                id = "book2",
                name = "20天背完高考核心词汇",
                filePath = "m-word/20天背完高考核心词汇.csv",
                totalWords = 3500,
                type = BookType.CSV
            ),
            VocabularyBook(
                id = "book3",
                name = "24天突破高考大纲词汇3500主词",
                filePath = "m-word/24天突破高考大纲词汇3500主词.csv",
                totalWords = 3500,
                type = BookType.CSV
            )
        )
    }

    // 从CSV文件读取单词
    suspend fun loadWordsFromCsv(filePath: String): List<Word> {
        Log.d(TAG, "开始从CSV文件加载单词: $filePath")
        return try {
            val inputStream = context.assets.open(filePath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val words = mutableListOf<Word>()
            val today = LocalDate.now()
            
            // 跳过标题行
            reader.readLine()
            
            // 读取每一行
            reader.useLines { lines ->
                lines.forEach { line ->
                    try {
                        val columns = line.split(",").map { it.trim() }
                        if (columns.size >= 2) {
                            words.add(
                                Word(
                                    word = columns[0],
                                    meaning = columns[1],
                                    ukPhonetic = columns.getOrNull(2) ?: "",
                                    usPhonetic = columns.getOrNull(3) ?: "",
                                    example = columns.getOrNull(4) ?: "",
                                    status = WordStatus.NEW,
                                    lastReviewDate = null,
                                    nextReviewDate = today,  // 设置为当前日期，这样新单词会立即出现在复习列表中
                                    reviewCount = 0
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析CSV行时出错: $line", e)
                    }
                }
            }
            
            Log.d(TAG, "从CSV文件读取到 ${words.size} 个单词，设置复习日期为: $today")
            
            // 将单词插入数据库
            vocabularyDao.insertWords(words)
            Log.d(TAG, "成功将单词插入数据库")
            
            words
            
        } catch (e: Exception) {
            Log.e(TAG, "加载CSV文件失败: $filePath", e)
            emptyList()
        }
    }

    // 从TXT文件读取单词
    suspend fun loadWordsFromTxt(filePath: String): List<Word> {
        return try {
            val inputStream = context.assets.open(filePath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val words = mutableListOf<Word>()
            val today = LocalDate.now()
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { 
                    val parts = it.split(" - ")
                    if (parts.size >= 2) {
                        words.add(
                            Word(
                                word = parts[0].trim(),
                                meaning = parts[1].trim(),
                                status = WordStatus.NEW,
                                lastReviewDate = null,
                                nextReviewDate = today,  // 设置为当前日期，这样新单词会立即出现在复习列表中
                                reviewCount = 0
                            )
                        )
                    }
                }
            }
            
            reader.close()
            
            // 将单词插入数据库
            vocabularyDao.insertWords(words)
            
            words
            
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取需要复习的单词列表
     */
    suspend fun getWordsForReview(limit: Int): List<Word> {
        Log.d(TAG, "获取需要复习的单词，限制数量: $limit")
        val today = LocalDate.now()
        
        // 首先尝试获取需要复习的单词
        var words = vocabularyDao.getWordsForReview(today).take(limit)
        
        // 如果没有需要复习的单词，尝试获取新单词
        if (words.isEmpty()) {
            Log.d(TAG, "没有找到需要复习的单词，尝试获取新单词")
            words = vocabularyDao.getAllWords()
                .filter { it.status == WordStatus.NEW }
                .take(limit)
            
            if (words.isNotEmpty()) {
                Log.d(TAG, "找到 ${words.size} 个新单词可以学习")
            } else {
                Log.d(TAG, "没有找到任何可以学习的单词，请先导入词书")
            }
        }
        
        Log.d(TAG, "最终返回 ${words.size} 个单词")
        if (words.isNotEmpty()) {
            Log.d(TAG, "第一个单词: ${words.first().word}, 状态: ${words.first().status}, 下次复习日期: ${words.first().nextReviewDate}")
        }
        
        return words
    }

    /**
     * 获取所有单词（用于生成选择题选项）
     */
    suspend fun getAllWords(): List<Word> {
        Log.d(TAG, "获取所有单词")
        val words = vocabularyDao.getAllWords()
        Log.d(TAG, "总共有 ${words.size} 个单词")
        return words
    }

    /**
     * 更新单词的学习状态
     */
    suspend fun updateWordStatus(
        wordId: Long,
        status: WordStatus,
        lastReviewDate: LocalDate,
        nextReviewDate: LocalDate,
        reviewCount: Int
    ) {
        Log.d(TAG, "更新单词状态: wordId=$wordId, status=$status, lastReviewDate=$lastReviewDate, nextReviewDate=$nextReviewDate, reviewCount=$reviewCount")
        
        vocabularyDao.updateWordStatus(
            wordId = wordId,
            status = status,
            lastReviewDate = lastReviewDate,
            nextReviewDate = nextReviewDate,
            reviewCount = reviewCount
        )
        Log.d(TAG, "单词状态已更新: status=$status, nextReview=$nextReviewDate")
    }

    /**
     * 获取今天需要学习的新单词数量
     */
    suspend fun getTodayNewWordsCount(): Int {
        return vocabularyDao.getTodayNewWordsCount(LocalDate.now())
    }

    /**
     * 获取今天需要复习的单词数量
     */
    suspend fun getTodayReviewWordsCount(): Int {
        val count = vocabularyDao.getTodayReviewWordsCount(LocalDate.now())
        Log.d(TAG, "今天需要复习的单词数量: $count")
        return count
    }

    /**
     * 计算下次复习时间
     * 使用间隔复习算法：
     * - 第1次复习：1天后
     * - 第2次复习：2天后
     * - 第3次复习：4天后
     * - 第4次复习：7天后
     * - 第5次复习：15天后
     * - 第6次及以上：30天后
     */
    private fun calculateNextReviewDate(today: LocalDate, currentReviewCount: Int): LocalDate {
        return when (currentReviewCount) {
            0 -> today.plusDays(1)
            1 -> today.plusDays(2)
            2 -> today.plusDays(4)
            3 -> today.plusDays(7)
            4 -> today.plusDays(15)
            else -> today.plusDays(30)
        }
    }
} 