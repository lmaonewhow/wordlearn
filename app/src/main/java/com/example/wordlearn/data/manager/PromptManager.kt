package com.example.wordlearn.data.manager

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.wordlearn.data.model.*
import com.example.wordlearn.data.store.AppSettingsKeys
import com.example.wordlearn.data.store.settingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PromptManager(private val context: Context) {
    private val TAG = "PromptManager"
    private var currentPrompt: Prompt = DefaultPrompts.ENGLISH_LEARNING
    private var userProfile: UserProfile? = null

    suspend fun initialize() {
        Log.d(TAG, "初始化提示词管理器")
        loadUserProfile()
        loadPrompt()
    }

    private suspend fun loadUserProfile() {
        try {
            val preferences: Preferences = context.settingsDataStore.data.first()
            val profileJson = preferences[AppSettingsKeys.PROFILE_JSON]
            
            if (!profileJson.isNullOrEmpty()) {
                Log.d(TAG, "检测到用户配置文件")
                try {
                    val json = Json { 
                        ignoreUnknownKeys = true 
                        isLenient = true
                    }
                    userProfile = json.decodeFromString<UserProfile>(profileJson)
                    Log.d(TAG, "成功加载用户配置文件")
                } catch (e: Exception) {
                    Log.e(TAG, "解析用户配置文件失败", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载用户配置文件失败", e)
        }
    }

    private suspend fun loadPrompt() {
        try {
            val preferences: Preferences = context.settingsDataStore.data.first()
            val customPromptJson = preferences[AppSettingsKeys.PROFILE_JSON]
            
            if (!customPromptJson.isNullOrEmpty()) {
                Log.d(TAG, "检测到自定义提示词配置")
                try {
                    val json = Json { 
                        ignoreUnknownKeys = true 
                        isLenient = true
                    }
                    val customPrompt = json.decodeFromString<Prompt>(customPromptJson)
                    if (customPrompt.isActive) {
                        Log.d(TAG, "加载自定义提示词：${customPrompt.id}")
                        currentPrompt = customPrompt
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析自定义提示词失败", e)
                    updatePromptWithProfile()
                }
            } else {
                Log.d(TAG, "未检测到自定义提示词，使用基于用户配置的提示词")
                updatePromptWithProfile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载提示词失败", e)
            updatePromptWithProfile()
        }
    }

    private fun updatePromptWithProfile() {
        val profile = userProfile
        if (profile != null) {
            Log.d(TAG, "根据用户配置更新提示词")
            
            val proficiencyDesc = when (profile.proficiencyLevel) {
                ProficiencyLevel.BEGINNER -> "初级水平，需要更多基础解释和简单例句"
                ProficiencyLevel.INTERMEDIATE -> "中级水平，可以理解较复杂的用法和例句"
                ProficiencyLevel.ADVANCED -> "高级水平，需要深入的词义辨析和地道用法"
            }
            
            val learningGoalDesc = when (profile.learningGoal) {
                LearningGoal.EXAM -> "考试备考，注重词汇考点和常见题型"
                LearningGoal.ABROAD -> "出国需求，侧重日常交际和学术用语"
                LearningGoal.WORK -> "工作需求，关注商务和职场用语"
                LearningGoal.INTEREST -> "兴趣学习，灵活多样的学习内容"
            }
            
            val interestsDesc = if (profile.readingInterests.isNotEmpty()) {
                "特别关注" + profile.readingInterests.joinToString("、") { 
                    when (it) {
                        ReadingInterest.NOVEL -> "文学小说"
                        ReadingInterest.TECH -> "科技"
                        ReadingInterest.BUSINESS -> "商业"
                        ReadingInterest.GAME -> "游戏"
                    }
                } + "相关领域的词汇"
            } else "涵盖多个领域的通用词汇"
            
            val learningStyleDesc = when (profile.learningStyle) {
                LearningStyle.PRACTICE -> "通过大量练习来加深记忆"
                LearningStyle.AI_EXPLAIN -> "需要详细的AI讲解和分析"
                LearningStyle.CONVERSATION -> "偏好对话式的学习方式"
            }
            
            currentPrompt = Prompt(
                id = "profile_based_prompt",
                content = """
                    你是一个专业的英语学习助手，请基于以下用户画像提供针对性的单词学习指导：
                    
                    用户特征：
                    - 英语水平：$proficiencyDesc
                    - 学习目标：$learningGoalDesc
                    - 兴趣方向：$interestsDesc
                    - 学习偏好：$learningStyleDesc
                    
                    回答要求：
                    1. 根据用户水平调整解释深度和难度
                    2. 结合学习目标提供相关场景和例句
                    3. 基于兴趣领域选择贴近用户的例子
                    4. 按照用户偏好的学习方式组织内容
                    5. 保持简明扼要，突出重点
                    6. 适时给予鼓励和学习建议
                """.trimIndent(),
                type = PromptType.DEFAULT,
                isActive = true,
                order = 0
            )
            
            Log.d(TAG, "已生成基于用户配置的提示词")
        } else {
            Log.d(TAG, "未找到用户配置，使用默认提示词")
            currentPrompt = DefaultPrompts.ENGLISH_LEARNING
        }
    }


    fun getCurrentPrompt(): Prompt {
        return currentPrompt
    }

    fun getDefaultPrompt(): Prompt {
        return DefaultPrompts.ENGLISH_LEARNING
    }
    
    suspend fun updateWithProfile(profile: UserProfile) {
        Log.d(TAG, "收到用户配置更新")
        userProfile = profile
        updatePromptWithProfile()
    }
} 