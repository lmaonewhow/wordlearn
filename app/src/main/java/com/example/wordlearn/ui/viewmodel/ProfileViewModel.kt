package com.example.wordlearn.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _answers = MutableStateFlow<Map<String, Any>>(emptyMap())
    val answers: StateFlow<Map<String, Any>> = _answers.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    var currentProfile by mutableStateOf<UserProfile?>(null)
        private set

    fun answerQuestion(questionId: String, answer: Any) {
        val newAnswers = _answers.value.toMutableMap()
        newAnswers[questionId] = answer
        _answers.value = newAnswers

        // 不再自动前进到下一题
        checkCompletion()
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < Questions.all.size - 1) {
            _currentQuestionIndex.value++
        } else {
            checkCompletion()
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value--
        }
    }

    private fun checkCompletion() {
        // 只有当所有问题都回答完才标记为完成
        if (Questions.all.all { question -> _answers.value.containsKey(question.id) }) {
            _isComplete.value = true
            generateProfile()
        }
    }

    private fun generateProfile() {
        val answers = _answers.value
        
        val learningGoal = when (answers["Q1"] as? String) {
            "考试" -> LearningGoal.EXAM
            "出国" -> LearningGoal.ABROAD
            "工作" -> LearningGoal.WORK
            else -> LearningGoal.INTEREST
        }

        val readingInterests = (answers["Q2"] as? List<String>)?.map { interest ->
            when (interest) {
                "小说" -> ReadingInterest.NOVEL
                "科技" -> ReadingInterest.TECH
                "商业" -> ReadingInterest.BUSINESS
                else -> ReadingInterest.GAME
            }
        } ?: emptyList()

        val proficiencyLevel = when (answers["Q3"] as? String) {
            "初级" -> ProficiencyLevel.BEGINNER
            "中级" -> ProficiencyLevel.INTERMEDIATE
            else -> ProficiencyLevel.ADVANCED
        }

        val customWords = (answers["Q4"] as? String)?.split(" ") ?: emptyList()

        val learningStyle = when (answers["Q5"] as? String) {
            "刷题" -> LearningStyle.PRACTICE
            "AI 讲解" -> LearningStyle.AI_EXPLAIN
            else -> LearningStyle.CONVERSATION
        }

        currentProfile = UserProfile(
            learningGoal = learningGoal,
            readingInterests = readingInterests,
            proficiencyLevel = proficiencyLevel,
            customWords = customWords,
            learningStyle = learningStyle
        )

        // TODO: Save to database
        viewModelScope.launch {
            // saveProfile(currentProfile!!)
        }
    }

    fun resetQuestions() {
        _currentQuestionIndex.value = 0
        _answers.value = emptyMap()
        _isComplete.value = false
        currentProfile = null
    }
} 