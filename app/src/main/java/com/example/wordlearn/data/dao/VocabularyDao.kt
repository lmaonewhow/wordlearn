package com.example.wordlearn.data.dao

import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.WordStatus
import java.time.LocalDate
import androidx.room.Dao
import androidx.room.Query

@Dao
interface VocabularyDao {
    suspend fun getAllWords(): List<Word>

    suspend fun insertWords(words: List<Word>)

    suspend fun getWordsForReview(today: LocalDate): List<Word>

    suspend fun getReviewCount(wordId: Long): Int

    suspend fun updateWordStatus(
        wordId: Long,
        status: WordStatus,
        lastReviewDate: LocalDate,
        nextReviewDate: LocalDate,
        reviewCount: Int
    )

    suspend fun getTodayNewWordsCount(today: LocalDate): Int

    suspend fun getTodayReviewWordsCount(today: LocalDate): Int
    
    // 获取单个单词
    suspend fun getWordById(wordId: Long): Word?
    
    // 获取单个单词（通过单词文本）
    suspend fun getWordByText(wordText: String): Word?
    
    // 更新单词收藏状态
    suspend fun updateFavoriteStatus(wordId: Long, isFavorite: Boolean)
    
    // 更新单词错误次数
    suspend fun updateErrorCount(wordId: Long, errorCount: Int)
    
    // 获取所有收藏单词
    suspend fun getFavoriteWords(): List<Word>
    
    // 获取所有错题（按错误次数降序排序）
    suspend fun getErrorWords(): List<Word>
    
    /**
     * 获取指定数量的新单词（状态为NEW）用于今日学习
     * @param count 需要获取的单词数量
     * @return 指定数量的新单词列表
     */
    suspend fun getTodayNewWords(count: Int): List<Word>
    
    /**
     * 获取今日学习的单词（当天标记为NEEDS_REVIEW且lastReviewDate是今天的单词）
     * @param today 今天的日期
     * @return 今日学习过的单词列表
     */
    suspend fun getTodayLearningWords(today: LocalDate): List<Word>
    
    /**
     * 根据间隔重复计划，获取需要复习的单词
     * @param today 今天的日期
     * @param intervalDays 间隔天数列表
     * @param limit 返回的单词数量上限
     * @return 需要复习的单词列表
     */
    suspend fun getPlannedReviewWords(
        today: LocalDate,
        intervalDays: List<Int>,
        limit: Int = 50
    ): List<Word>
    
    /**
     * 标记单词为学习中状态，设置下一次复习日期
     * @param wordId 单词ID
     * @param today 今天的日期
     * @param nextReviewDay 下次复习的日期
     */
    suspend fun markWordAsLearning(
        wordId: Long,
        today: LocalDate,
        nextReviewDay: LocalDate
    )
    
    /**
     * 更新单词的下一次复习日期
     * @param wordId 单词ID
     * @param lastReviewDate 最后复习日期
     * @param nextReviewDate 下次复习日期
     * @param reviewCount 已复习次数
     * @return 更新是否成功
     */
    suspend fun updateNextReviewDate(
        wordId: Long,
        lastReviewDate: LocalDate,
        nextReviewDate: LocalDate,
        reviewCount: Int
    ): Boolean

    /**
     * 重置所有单词的学习状态
     */
    @Query("""
        UPDATE words SET 
        status = 'NEW',
        lastReviewDate = NULL,
        nextReviewDate = NULL,
        reviewCount = 0
        WHERE status != 'KNOWN'
    """)
    suspend fun resetAllWordsStatus()

    /**
     * 清理当前学习进度
     */
    @Query("""
        UPDATE words SET 
        status = 'NEW',
        lastReviewDate = NULL,
        nextReviewDate = NULL,
        reviewCount = 0
        WHERE status != 'KNOWN'
    """)
    suspend fun clearLearningProgress()

    /**
     * 清空所有单词数据
     */
    @Query("DELETE FROM words")
    suspend fun clearAllWords()

    /**
     * 更新最后修改时间
     */
    @Query("UPDATE words SET lastModified = :timestamp")
    suspend fun updateLastModifiedTime(timestamp: Long = System.currentTimeMillis())
} 