package com.example.wordlearn.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.model.*
import com.example.wordlearn.data.store.AppSettingsKeys
import com.example.wordlearn.data.store.settingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProfileViewModel : ViewModel() {
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _answers = MutableStateFlow<Map<String, Any>>(emptyMap())
    val answers: StateFlow<Map<String, Any>> = _answers.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    private val TAG = "ProfileViewModel"

    var currentProfile by mutableStateOf<UserProfile?>(null)
        private set
        
    // 添加提示弹窗显示状态
    var showIncompleteWarning by mutableStateOf(false)

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
            Log.d(TAG, "所有问题已回答完毕，标记为完成状态")
            _isComplete.value = true
            generateProfile()
        } else {
            Log.d(TAG, "问题未全部回答: ${_answers.value.keys} vs ${Questions.all.map { it.id }}")
        }
    }

    private fun generateProfile() {
        Log.d(TAG, "开始生成用户配置文件")
        val answers = _answers.value
        
        val learningGoal = when (answers["Q1"] as? String) {
            "考试" -> LearningGoal.EXAM
            "出国" -> LearningGoal.ABROAD
            "工作" -> LearningGoal.WORK
            else -> LearningGoal.INTEREST
        }

        val readingInterests = answers["Q2"]?.let { value ->
            (value as? List<*>)?.mapNotNull { interest ->
                when (interest as? String) {
                    "小说" -> ReadingInterest.NOVEL
                    "科技" -> ReadingInterest.TECH
                    "商业" -> ReadingInterest.BUSINESS
                    else -> ReadingInterest.GAME
                }
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

        Log.d(TAG, "用户配置文件生成完成: $currentProfile")
    }
    
    fun saveUserProfile(context: Context, profile: UserProfile) {
        Log.d(TAG, "开始保存用户配置: $profile")
        viewModelScope.launch {
            try {
                // 先获取当前配置状态
                val preferences = context.settingsDataStore.data.first()
                val currentStatus = preferences[AppSettingsKeys.IS_PROFILE_COMPLETED] ?: false
                Log.d(TAG, "当前配置完成状态: $currentStatus")
                
                // 创建json实例时指定序列化模块
                val json = Json { 
                    // 容忍未知键 - 在读取旧版本数据时有用
                    ignoreUnknownKeys = true
                    // 对类进行多态序列化
                    isLenient = true
                    // 允许缺少必要值的序列化对象
                    encodeDefaults = true 
                }
                
                // 保存用户配置到DataStore
                context.settingsDataStore.edit {
                    try {
                        val jsonString = json.encodeToString(profile)
                        Log.d(TAG, "序列化后的JSON: $jsonString")
                        it[AppSettingsKeys.PROFILE_JSON] = jsonString
                        it[AppSettingsKeys.IS_PROFILE_COMPLETED] = true
                        Log.d(TAG, "已保存配置JSON和完成状态")
                    } catch (e: Exception) {
                        Log.e(TAG, "序列化过程中发生错误", e)
                        throw e
                    }
                }
                
                // 再次读取以验证保存成功
                val updatedPreferences = context.settingsDataStore.data.first()
                val updatedStatus = updatedPreferences[AppSettingsKeys.IS_PROFILE_COMPLETED] ?: false
                val savedJson = updatedPreferences[AppSettingsKeys.PROFILE_JSON]
                
                Log.d(TAG, "保存后状态验证 - 完成状态: $updatedStatus, JSON存在: ${savedJson != null}")
                
                // 更新当前配置
                currentProfile = profile
                
                // 确保完成状态被设置
                _isComplete.value = true
                
                // 日志记录
                Log.d(TAG, "用户配置已成功保存")
            } catch (e: Exception) {
                Log.e(TAG, "保存用户配置失败", e)
            }
        }
    }

    fun resetQuestions() {
        _currentQuestionIndex.value = 0
        _answers.value = emptyMap()
        _isComplete.value = false
        currentProfile = null
    }
} 