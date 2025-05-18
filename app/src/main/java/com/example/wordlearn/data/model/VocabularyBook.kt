package com.example.wordlearn.data.model

// 词汇书数据类
data class VocabularyBook(
    val id: String,
    val name: String,
    val filePath: String,
    val totalWords: Int,
    val type: BookType
)

// 词汇书类型
enum class BookType {
    CSV,
    TXT
}

// 单词数据类
data class Word(
    val word: String,
    val ukPhonetic: String = "", // 英式音标
    val usPhonetic: String = "", // 美式音标
    val meaning: String = "",    // 中文含义
    val example: String = ""     // 例句
) 