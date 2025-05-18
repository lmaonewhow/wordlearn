package com.example.wordlearn.data.dao

import androidx.room.*
import com.example.wordlearn.data.model.WordCard
import com.example.wordlearn.data.model.WordStatus
import java.time.LocalDate

@Dao
interface WordDao {
    @Query("""
        SELECT * FROM words 
        WHERE status = :status 
        AND nextReviewDate <= :today
        ORDER BY RANDOM()
    """)
    suspend fun getWordsForReview(today: LocalDate): List<WordCard>

    @Query("SELECT * FROM words")
    fun getAllWords(): List<WordCard>

    @Query("SELECT reviewCount FROM words WHERE id = :wordId")
    suspend fun getReviewCount(wordId: Long): Int

    @Query("""
        UPDATE words 
        SET status = :status,
            lastReviewDate = :lastReviewDate,
            nextReviewDate = :nextReviewDate,
            reviewCount = :reviewCount
        WHERE id = :wordId
    """)
    suspend fun updateWordStatus(
        wordId: Long,
        status: WordStatus,
        lastReviewDate: LocalDate,
        nextReviewDate: LocalDate,
        reviewCount: Int
    )

    @Query("""
        SELECT COUNT(*) FROM words 
        WHERE status = 'NEW' 
        AND lastReviewDate IS NULL
    """)
    suspend fun getTodayNewWordsCount(today: LocalDate): Int

    @Query("""
        SELECT COUNT(*) FROM words 
        WHERE status = 'NEEDS_REVIEW'
        AND nextReviewDate <= :today
    """)
    suspend fun getTodayReviewWordsCount(today: LocalDate): Int
} 