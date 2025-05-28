package com.example.wordlearn.data.dao

import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.Wordbook
import java.time.LocalDate

interface WordbookDao {
    /**
     * 获取所有词书
     */
    suspend fun getAllWordbooks(): List<Wordbook>
    
    /**
     * 获取活跃的词书（当前正在使用的）
     */
    suspend fun getActiveWordbook(): Wordbook?
    
    /**
     * 创建新词书
     * @return 新创建的词书ID
     */
    suspend fun createWordbook(wordbook: Wordbook): Long
    
    /**
     * 更新词书信息
     */
    suspend fun updateWordbook(wordbook: Wordbook): Boolean
    
    /**
     * 删除词书
     */
    suspend fun deleteWordbook(wordbookId: Long): Boolean
    
    /**
     * 设置活跃的词书
     */
    suspend fun setActiveWordbook(wordbookId: Long): Boolean
    
    /**
     * 更新词书的统计数据
     */
    suspend fun updateWordbookStats(wordbookId: Long): Boolean
    
    /**
     * 批量导入单词到指定词书
     */
    suspend fun importWordsToWordbook(wordbookId: Long, words: List<Word>): Int
    
    /**
     * 获取词书中的所有单词
     */
    suspend fun getWordsInWordbook(wordbookId: Long): List<Word>
    
    /**
     * 获取词书中的待学习单词列表及数量
     * status = NEW
     */
    suspend fun getNewWordsInWordbook(wordbookId: Long): List<Word>
    
    /**
     * 获取词书中的待学习单词数量
     */
    suspend fun getNewWordsCountInWordbook(wordbookId: Long): Int
    
    /**
     * 查询某本词书中"今日新学"单词数量
     * status = NEW 且 lastReviewDate IS NULL
     */
    suspend fun getTodayNewLearnedWordsInWordbook(wordbookId: Long, today: LocalDate): List<Word>
    
    /**
     * 查询某本词书中"今日新学"单词数量
     */
    suspend fun getTodayNewLearnedWordsCountInWordbook(wordbookId: Long, today: LocalDate): Int
    
    /**
     * 查询某本词书中"待复习"单词列表
     * status = NEEDS_REVIEW 且 nextReviewDate <= today
     */
    suspend fun getReviewWordsInWordbook(wordbookId: Long, today: LocalDate): List<Word>
    
    /**
     * 查询某本词书中"待复习"单词数量
     */
    suspend fun getReviewWordsCountInWordbook(wordbookId: Long, today: LocalDate): Int
    
    /**
     * 查询某本词书中"今日已学习"单词列表
     * status = NEEDS_REVIEW 且 lastReviewDate = today
     */
    suspend fun getTodayLearnedWordsInWordbook(wordbookId: Long, today: LocalDate): List<Word>
    
    /**
     * 查询某本词书中"今日已学习"单词数量
     */
    suspend fun getTodayLearnedWordsCountInWordbook(wordbookId: Long, today: LocalDate): Int
    
    /**
     * 按词书批量清空单词状态（重置为NEW）
     */
    suspend fun resetWordsStatusInWordbook(wordbookId: Long): Int
    
    /**
     * 记录学习行为
     */
    suspend fun recordLearningActivity(
        wordId: Long, 
        wordbookId: Long, 
        learnDate: LocalDate, 
        isCorrect: Boolean
    ): Long
    
    /**
     * 获取词书的学习进度
     * @return Triple(已学习单词数, 总单词数, 学习进度百分比)
     */
    suspend fun getWordbookProgress(wordbookId: Long): Triple<Int, Int, Float>
} 