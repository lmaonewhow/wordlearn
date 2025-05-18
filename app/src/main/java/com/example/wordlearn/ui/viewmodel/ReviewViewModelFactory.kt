package com.example.wordlearn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordlearn.data.repository.VocabularyRepository
import com.example.wordlearn.data.LearningPlanRepository

class ReviewViewModelFactory(
    private val vocabularyRepository: VocabularyRepository,
    private val learningPlanRepository: LearningPlanRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            return ReviewViewModel(vocabularyRepository, learningPlanRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 