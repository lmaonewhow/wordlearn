package com.example.wordlearn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalTime

// 创建 DataStore 实例
private val Context.learningPlanDataStore: DataStore<Preferences> by preferencesDataStore(name = "learning_plan")

// 配置 Json 序列化器
private val json = Json {
    ignoreUnknownKeys = true // 忽略未知字段
    encodeDefaults = true // 编码默认值
    isLenient = true // 宽松解析
}

class LearningPlanRepository(private val context: Context) {
    // 定义 keys
    private object PreferencesKeys {
        val DAILY_GOAL = stringPreferencesKey("daily_goal")
        val REVIEW_SETTINGS = stringPreferencesKey("review_settings")
        val REMINDERS = stringPreferencesKey("reminders")
        val DIFFICULTY_LEVEL = stringPreferencesKey("difficulty_level")
    }

    // 保存学习计划
    suspend fun saveLearningPlan(learningPlan: LearningPlan) {
        context.learningPlanDataStore.edit { preferences ->
            // 将对象转换为 JSON 字符串
            preferences[PreferencesKeys.DAILY_GOAL] = json.encodeToString(learningPlan.dailyGoal)
            preferences[PreferencesKeys.REVIEW_SETTINGS] = json.encodeToString(learningPlan.reviewSettings)
            preferences[PreferencesKeys.REMINDERS] = json.encodeToString(learningPlan.reminders)
            preferences[PreferencesKeys.DIFFICULTY_LEVEL] = json.encodeToString(learningPlan.difficultyLevel)
        }
    }

    // 获取学习计划
    val learningPlan: Flow<LearningPlan?> = context.learningPlanDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            try {
                // 从 JSON 字符串解析对象
                val dailyGoalJson = preferences[PreferencesKeys.DAILY_GOAL]
                val reviewSettingsJson = preferences[PreferencesKeys.REVIEW_SETTINGS]
                val remindersJson = preferences[PreferencesKeys.REMINDERS]
                val difficultyLevelJson = preferences[PreferencesKeys.DIFFICULTY_LEVEL]

                if (dailyGoalJson != null && reviewSettingsJson != null && 
                    remindersJson != null && difficultyLevelJson != null) {
                    LearningPlan(
                        id = "default",
                        userId = "current_user",
                        wordBookId = "current_book",
                        dailyGoal = json.decodeFromString(dailyGoalJson),
                        reviewSettings = json.decodeFromString(reviewSettingsJson),
                        reminders = json.decodeFromString(remindersJson),
                        difficultyLevel = json.decodeFromString(difficultyLevelJson)
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
} 