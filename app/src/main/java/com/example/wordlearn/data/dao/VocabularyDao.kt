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
     * 更新最后修改时间
     */
    @Query("UPDATE vocabulary SET last_modified = :timestamp")
    suspend fun updateLastModifiedTime(timestamp: Long = System.currentTimeMillis())
} 