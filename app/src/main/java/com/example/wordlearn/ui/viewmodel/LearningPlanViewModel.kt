package com.example.wordlearn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class LearningPlanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LearningPlanRepository(application)

    // 学习计划状态
    private val _learningPlan = MutableStateFlow<LearningPlan?>(null)
    val learningPlan: StateFlow<LearningPlan?> = _learningPlan.asStateFlow()

    // 每日目标状态
    private val _dailyGoal = MutableStateFlow(
        DailyGoal(
            newWordsCount = 10,
            reviewWordsCount = 20,
            learningTimeMinutes = 30,
            activeDays = emptySet() // 初始化为空集合
        )
    )
    val dailyGoal: StateFlow<DailyGoal> = _dailyGoal.asStateFlow()

    // 复习设置状态
    private val _reviewSettings = MutableStateFlow(
        ReviewSettings(
            intervalDays = listOf(1),  // 默认选中第一个间隔
            minCorrectRate = 0.8f,
            autoAdjustDifficulty = true
        )
    )
    val reviewSettings: StateFlow<ReviewSettings> = _reviewSettings.asStateFlow()

    // 提醒设置状态
    private val _reminders = MutableStateFlow<List<LearningReminder>>(emptyList())
    val reminders: StateFlow<List<LearningReminder>> = _reminders.asStateFlow()

    // 难度等级状态
    private val _difficultyLevel = MutableStateFlow(DifficultyLevel.NORMAL)
    val difficultyLevel: StateFlow<DifficultyLevel> = _difficultyLevel.asStateFlow()

    // 学习记录状态
    private val _learningRecords = MutableStateFlow<List<LearningRecord>>(emptyList())
    val learningRecords: StateFlow<List<LearningRecord>> = _learningRecords.asStateFlow()

    // 成就状态
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    // 保存状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.None)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    init {
        // 初始化时加载保存的数据
        loadLearningPlan()
    }

    // 更新每日新词数量
    fun updateNewWordsCount(count: Int) {
        viewModelScope.launch {
            _dailyGoal.value = _dailyGoal.value.copy(newWordsCount = count)
            saveLearningPlan()
        }
    }

    // 更新每日复习数量
    fun updateReviewWordsCount(count: Int) {
        viewModelScope.launch {
            _dailyGoal.value = _dailyGoal.value.copy(reviewWordsCount = count)
            saveLearningPlan()
        }
    }

    // 更新每日学习时长
    fun updateLearningTimeMinutes(minutes: Int) {
        viewModelScope.launch {
            _dailyGoal.value = _dailyGoal.value.copy(learningTimeMinutes = minutes)
            saveLearningPlan()
        }
    }

    // 切换学习日
    fun toggleActiveDay(day: DayOfWeek) {
        viewModelScope.launch {
            val currentDays = _dailyGoal.value.activeDays
            val newDays = if (currentDays.contains(day)) {
                currentDays - day
            } else {
                currentDays + day
            }
            _dailyGoal.value = _dailyGoal.value.copy(activeDays = newDays)
            saveLearningPlan()
        }
    }

    // 更新复习间隔
    fun updateIntervalDays(interval: Int) {
        viewModelScope.launch {
            _reviewSettings.value = _reviewSettings.value.copy(
                intervalDays = listOf(interval)
            )
            saveLearningPlan()
        }
    }

    // 更新最低正确率
    fun updateMinCorrectRate(rate: Float) {
        viewModelScope.launch {
            _reviewSettings.value = _reviewSettings.value.copy(minCorrectRate = rate)
            saveLearningPlan()
        }
    }

    // 添加提醒
    fun addReminder(time: LocalTime, days: Set<DayOfWeek>) {
        viewModelScope.launch {
            // 检查是否已存在相同时间的提醒
            val existingReminder = _reminders.value.find { it.time == time }
            if (existingReminder != null) {
                // 如果存在相同时间的提醒，更新其日期设置
                updateReminderDays(existingReminder.id, existingReminder.days + days)
            } else {
                // 如果不存在，添加新提醒
                val newReminder = LearningReminder(
                    id = System.currentTimeMillis().toString(),
                    time = time,
                    isEnabled = true,
                    days = days
                )
                _reminders.value = (_reminders.value + newReminder)
                    .sortedBy { it.time } // 按时间排序
                saveLearningPlan()
            }
        }
    }

    // 删除提醒
    fun removeReminder(reminderId: String) {
        viewModelScope.launch {
            _reminders.value = _reminders.value.filter { it.id != reminderId }
            saveLearningPlan()
        }
    }

    // 更新提醒状态
    fun updateReminderEnabled(reminderId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            _reminders.value = _reminders.value.map { reminder ->
                if (reminder.id == reminderId) reminder.copy(isEnabled = isEnabled)
                else reminder
            }
            saveLearningPlan()
        }
    }

    // 更新提醒时间
    fun updateReminderTime(reminderId: String, time: LocalTime) {
        viewModelScope.launch {
            _reminders.value = _reminders.value.map { reminder ->
                if (reminder.id == reminderId) reminder.copy(time = time)
                else reminder
            }
            saveLearningPlan()
        }
    }

    // 更新提醒日期
    fun updateReminderDays(reminderId: String, days: Set<DayOfWeek>) {
        viewModelScope.launch {
            _reminders.value = _reminders.value.map { reminder ->
                if (reminder.id == reminderId) reminder.copy(days = days)
                else reminder
            }
            saveLearningPlan()
        }
    }

    // 更新难度等级
    fun updateDifficultyLevel(level: DifficultyLevel) {
        viewModelScope.launch {
            _difficultyLevel.value = level
            saveLearningPlan()
        }
    }

    // 保存学习计划
    fun saveLearningPlan() {
        viewModelScope.launch {
            try {
                _saveState.value = SaveState.Saving
                val plan = LearningPlan(
                    id = _learningPlan.value?.id ?: "default",
                    userId = "current_user", // TODO: 从用户系统获取
                    wordBookId = "current_book", // TODO: 从词书系统获取
                    dailyGoal = _dailyGoal.value,
                    reviewSettings = _reviewSettings.value,
                    reminders = _reminders.value,
                    difficultyLevel = _difficultyLevel.value
                )
                _learningPlan.value = plan
                // 保存到 DataStore
                repository.saveLearningPlan(plan)
                _saveState.value = SaveState.Success
                // 3秒后重置状态
                kotlinx.coroutines.delay(3000)
                _saveState.value = SaveState.None
            } catch (e: Exception) {
                e.printStackTrace()
                _saveState.value = SaveState.Error(e.message ?: "保存失败")
                // 3秒后重置状态
                kotlinx.coroutines.delay(3000)
                _saveState.value = SaveState.None
            }
        }
    }

    // 加载学习计划
    private fun loadLearningPlan() {
        viewModelScope.launch {
            repository.learningPlan.collect { plan ->
                plan?.let {
                    _learningPlan.value = it
                    _dailyGoal.value = it.dailyGoal
                    _reviewSettings.value = it.reviewSettings
                    _reminders.value = it.reminders
                    _difficultyLevel.value = it.difficultyLevel
                }
            }
        }
    }

    // 加载学习记录
    fun loadLearningRecords() {
        viewModelScope.launch {
            // TODO: 从本地数据库或远程服务器加载学习记录
        }
    }

    // 加载成就
    fun loadAchievements() {
        viewModelScope.launch {
            // TODO: 从本地数据库或远程服务器加载成就
        }
    }

    // 保存状态枚举
    sealed class SaveState {
        object None : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }
} 