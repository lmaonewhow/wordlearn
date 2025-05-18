package com.example.wordlearn.data.dao

import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.WordStatus
import java.time.LocalDate

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
} 