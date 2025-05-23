package com.example.wordlearn.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.wordlearn.App
import com.example.wordlearn.data.dao.VocabularyDao
import com.example.wordlearn.data.model.BookType
import com.example.wordlearn.data.model.VocabularyBook
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.WordStatus
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "VocabularyRepository"

class VocabularyRepository(
    private val context: Context,
    val vocabularyDao: VocabularyDao
) {
    // 缓存已加载的词书
    private val loadedBooks = mutableSetOf<String>()
    
    // 检查词书是否已加载到数据库
    suspend fun isBookLoadedInDatabase(filePath: String): Boolean {
        // 获取第一个单词作为采样
        val words = vocabularyDao.getAllWords()
        return words.isNotEmpty()
    }
    
    // 清除加载缓存
    fun clearLoadedBooksCache() {
        loadedBooks.clear()
        Log.d(TAG, "已清除词书加载缓存")
    }

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

    // New function to dynamically get books from assets
    suspend fun getAvailableBooksFromAssets(): List<VocabularyBook> = withContext(Dispatchers.IO) {
        try {
            val assetManager = context.assets
            val files = assetManager.list("m-word")
            val books = mutableListOf<VocabularyBook>()
            files?.forEach { fileName ->
                if (fileName.endsWith(".csv")) {
                    val bookName = fileName.removeSuffix(".csv")
                    books.add(
                        VocabularyBook(
                            id = bookName, // Using bookName as ID, ensure uniqueness or generate proper ID
                            name = bookName,
                            filePath = "m-word/$fileName",
                            totalWords = 0, // Placeholder, actual count upon full load
                            type = BookType.CSV // Assuming all in m-word are CSV for now
                        )
                    )
                }
                // TODO: Add logic for other BookTypes like .txt if they exist in m-word
            }
            Log.d(TAG, "Dynamically found ${books.size} books in assets/m-word: ${books.joinToString { it.name }}")
            books
        } catch (e: Exception) {
            Log.e(TAG, "Error listing books from assets/m-word", e)
            emptyList()
        }
    }

    // 从CSV文件读取单词
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loadWordsFromCsv(filePath: String): List<Word> {
        // 检查是否已经加载过这本词书，且数据库中有数据
        if (loadedBooks.contains(filePath) && isBookLoadedInDatabase(filePath)) {
            Log.d(TAG, "词书已加载过且数据库中有数据，跳过导入: $filePath")
            return vocabularyDao.getAllWords()
        }
        
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
                                    nextReviewDate = today,
                                    reviewCount = 0
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析CSV行时出错: $line", e)
                    }
                }
            }
            
            if (words.isNotEmpty()) {
                Log.d(TAG, "从CSV文件读取到 ${words.size} 个单词，设置复习日期为: $today")
                
                // 将单词插入数据库
                vocabularyDao.insertWords(words)
                Log.d(TAG, "成功将单词插入数据库")
                
                // 标记词书为已加载
                loadedBooks.add(filePath)
                
                // 从数据库获取最新的单词数据（包含收藏和错误信息）
                syncWordsWithDatabase(words)
            } else {
                Log.w(TAG, "CSV文件中没有有效的单词数据: $filePath")
                emptyList()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "加载CSV文件失败: $filePath", e)
            emptyList()
        }
    }

    // 从TXT文件读取单词
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loadWordsFromTxt(filePath: String): List<Word> {
        // 检查是否已经加载过这本词书，且数据库中有数据
        if (loadedBooks.contains(filePath) && isBookLoadedInDatabase(filePath)) {
            Log.d(TAG, "词书已加载过且数据库中有数据，跳过导入: $filePath")
            return vocabularyDao.getAllWords()
        }
        
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
                                nextReviewDate = today,
                                reviewCount = 0
                            )
                        )
                    }
                }
            }
            
            reader.close()
            
            if (words.isNotEmpty()) {
                // 将单词插入数据库
                vocabularyDao.insertWords(words)
                
                // 标记词书为已加载
                loadedBooks.add(filePath)
                
                // 从数据库获取最新的单词数据（包含收藏和错误信息）
                syncWordsWithDatabase(words)
            } else {
                Log.w(TAG, "TXT文件中没有有效的单词数据: $filePath")
                emptyList()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "加载TXT文件失败: $filePath", e)
            emptyList()
        }
    }
    
    // 从数据库同步单词的收藏和错误信息
    private suspend fun syncWordsWithDatabase(words: List<Word>): List<Word> {
        Log.d(TAG, "同步单词的收藏和错误信息")
        
        try {
            // 更新内存单词列表中的收藏和错误信息
            val updatedWords = words.map { word ->
                try {
                    // 通过文本直接查询数据库
                    val dbWord = vocabularyDao.getWordByText(word.word)
                    if (dbWord != null) {
                        // 更新单词的ID、收藏状态和错误次数
//                        Log.d(TAG, "找到单词 '${word.word}' 在数据库中: id=${dbWord.id}, 收藏=${dbWord.isFavorite}, 错误次数=${dbWord.errorCount}")
                        word.copy(
                            id = dbWord.id,
                            isFavorite = dbWord.isFavorite,
                            errorCount = dbWord.errorCount
                        )
                    } else {
                        word
                    }
                } catch (e: Exception) {
//                    Log.e(TAG, "查询单词 '${word.word}' 失败: ${e.message}")
                    word
                }
            }
            
//            Log.d(TAG, "同步完成，更新了 ${updatedWords.size} 个单词的收藏和错误信息")
            return updatedWords
        } catch (e: Exception) {
            Log.e(TAG, "同步单词信息失败: ${e.message}")
            return words
        }
    }

    /**
     * 获取需要复习的单词列表
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getWordsForReview(limit: Int): List<Word> {
        Log.d(TAG, "获取需要复习的单词，限制数量: $limit")
        val today = LocalDate.now()
        
        // 获取需要复习的单词(NEEDS_REVIEW状态)，这些是用户已学习过的单词
        var words = vocabularyDao.getWordsForReview(today)
            .shuffled() // 确保随机排序
            .take(limit)
        
        Log.d(TAG, "获取到${words.size}个需要复习的单词")
        
        // 如果没有需要复习的单词，返回空列表
        if (words.isEmpty()) {
            Log.d(TAG, "没有找到需要复习的单词")
        } else {
            Log.d(TAG, "最终返回 ${words.size} 个单词")
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
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getTodayNewWordsCount(): Int {
        return vocabularyDao.getTodayNewWordsCount(LocalDate.now())
    }

    /**
     * 获取今天需要复习的单词数量
     */
    @RequiresApi(Build.VERSION_CODES.O)
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
    @RequiresApi(Build.VERSION_CODES.O)
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

    /**
     * 更新学习统计数据（用于刷新首页显示）
     * 这个方法会触发数据更新，确保首页显示的待学习和待复习数字是最新的
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateLearningCounts() {
        // 使用自定义协程作用域而不是viewModelScope
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 刷新今日新单词数量
                val newWordsCount = getTodayNewWordsCount()
                Log.d(TAG, "刷新统计 - 今日待学习单词数: $newWordsCount")
                
                // 刷新今日复习数量
                val reviewWordsCount = getTodayReviewWordsCount()
                Log.d(TAG, "刷新统计 - 今日待复习单词数: $reviewWordsCount")
                
                // 直接通知HomeViewModel刷新
                // 这里使用主线程，因为ViewModel需要在主线程更新UI状态
                withContext(Dispatchers.Main) {
                    (context.applicationContext as? App)?.homeViewModel?.forceRefreshNow()
                    Log.d(TAG, "已通知HomeViewModel强制刷新数据")
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新学习统计数据失败", e)
            }
        }
    }
    
    /**
     * 收藏/取消收藏单词
     */
    suspend fun toggleFavorite(wordId: Long, isFavorite: Boolean) {
        try {
            Log.d(TAG, "正在${if (isFavorite) "收藏" else "取消收藏"}单词 ID: $wordId")
            vocabularyDao.updateFavoriteStatus(wordId, isFavorite)
            Log.d(TAG, "单词收藏状态已更新: $isFavorite")
        } catch (e: Exception) {
            Log.e(TAG, "更新单词收藏状态失败", e)
        }
    }
    
    /**
     * 增加单词的错误次数
     */
    suspend fun incrementErrorCount(wordId: Long) {
        try {
            Log.d(TAG, "正在增加单词错误次数, ID: $wordId")
            // 先获取当前错误次数
            val word = vocabularyDao.getWordById(wordId)
            if (word != null) {
                // 错误次数+1
                vocabularyDao.updateErrorCount(wordId, word.errorCount + 1)
                Log.d(TAG, "单词错误次数已更新: ${word.errorCount + 1}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新单词错误次数失败", e)
        }
    }
    
    /**
     * 获取收藏的单词列表
     */
    suspend fun getFavoriteWords(): List<Word> {
        try {
            Log.d(TAG, "正在获取收藏单词列表")
            val favoriteWords = vocabularyDao.getFavoriteWords()
            Log.d(TAG, "获取到${favoriteWords.size}个收藏单词")
            return favoriteWords
        } catch (e: Exception) {
            Log.e(TAG, "获取收藏单词列表失败", e)
            return emptyList()
        }
    }
    
    /**
     * 获取错题列表（按错误次数降序排序）
     */
    suspend fun getErrorWords(): List<Word> {
        try {
            Log.d(TAG, "正在获取错题列表")
            val errorWords = vocabularyDao.getErrorWords()
            Log.d(TAG, "获取到${errorWords.size}个错题")
            return errorWords
        } catch (e: Exception) {
            Log.e(TAG, "获取错题列表失败", e)
            return emptyList()
        }
    }

    /**
     * 清理数据库中的临时数据
     * 包括：
     * 1. 重置所有单词的学习状态
     * 2. 清理当前学习进度
     * 3. 保留收藏和错误记录
     */
    suspend fun clearTemporaryData() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始清理数据库临时数据")
                
                // 重置所有单词的学习状态
                vocabularyDao.resetAllWordsStatus()
                
                // 清理当前学习进度
                vocabularyDao.clearLearningProgress()
                
                // 更新最后修改时间
                vocabularyDao.updateLastModifiedTime()
                
                Log.d(TAG, "数据库临时数据清理完成")
            } catch (e: Exception) {
                Log.e(TAG, "清理数据库临时数据失败", e)
                throw e
            }
        }
    }
} 