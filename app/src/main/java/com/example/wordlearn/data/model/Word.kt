package com.example.wordlearn.data.model

import java.time.LocalDate

data class Word(
    val id: Long = 0,
    val word: String,
    val meaning: String,
    val ukPhonetic: String = "",
    val usPhonetic: String = "",
    val example: String = "",
    val status: WordStatus = WordStatus.NEW,
    val lastReviewDate: LocalDate? = null,
    val nextReviewDate: LocalDate? = null,
    val reviewCount: Int = 0
)

enum class WordStatus {
    NEW,           // 新单词，未学习
    LEARNING,      // 正在学习中
    NEEDS_REVIEW,  // 需要复习
    KNOWN         // 已掌握
} 