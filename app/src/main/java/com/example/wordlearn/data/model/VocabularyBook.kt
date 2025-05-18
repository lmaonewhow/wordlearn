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