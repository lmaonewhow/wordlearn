package com.example.wordlearn.data.model

/**
 * 词书实体类
 */
data class Wordbook(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val sourcePath: String = "",
    val type: WordbookType = WordbookType.DEFAULT,
    val totalWords: Int = 0,
    val newWordsCount: Int = 0,
    val reviewWordsCount: Int = 0,
    val learnedWordsCount: Int = 0,
    val isFavorite: Boolean = false,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * 词书类型
 */
enum class WordbookType {
    DEFAULT,    // 默认词书
    IMPORTED,   // 导入的词书
    CUSTOM,     // 自定义词书
    SYSTEM      // 系统预置词书
}

/**
 * 词书学习统计
 */
data class WordbookStats(
    val wordbookId: Long,
    val totalWords: Int,
    val newWordsCount: Int,
    val reviewWordsCount: Int,
    val learnedWordsCount: Int,
    val progress: Float
)

/**
 * 学习记录
 */
data class LearningRecord(
    val id: Long = 0,
    val wordId: Long,
    val wordbookId: Long,
    val learnDate: String,
    val isCorrect: Boolean = true,
    val reviewTime: Long = 0
) 