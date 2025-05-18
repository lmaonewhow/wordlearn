package com.example.wordlearn.data

// 词书数据类
data class WordBook(
    val id: String,
    val title: String,
    val description: String,
    val totalWords: Int,
    val type: WordBookType,
    val words: List<Word> = emptyList(),
    val progress: Float = 0f // 学习进度，0-1
)

// 词书类型
enum class WordBookType {
    FOUR_LEVEL,    // 四级
    SIX_LEVEL,     // 六级
    HIGH_SCHOOL    // 高中
}

// 单词数据类
data class Word(
    val id: String,
    val word: String,
    val phonetic: String,         // 音标
    val pronunciation: String,    // 发音音频URL
    val definitions: List<WordDefinition>,
    val examples: List<String>,   // 例句
    val synonyms: List<String> = emptyList(),    // 同义词
    val antonyms: List<String> = emptyList(),    // 反义词
    val difficulty: Int = 1,      // 难度等级 1-5
    val learningStatus: LearningStatus = LearningStatus.NOT_STARTED,
    val lastReviewTime: Long? = null,  // 上次复习时间
    val reviewCount: Int = 0,     // 复习次数
    val masteryLevel: Float = 0f  // 掌握程度 0-1
)

// 词义数据类
data class WordDefinition(
    val partOfSpeech: String,    // 词性
    val meaning: String,         // 释义
    val examples: List<String>   // 该释义下的例句
)

// 学习状态
enum class LearningStatus {
    NOT_STARTED,    // 未开始
    LEARNING,       // 学习中
    REVIEWING,      // 复习中
    MASTERED        // 已掌握
} 