package com.example.wordlearn.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.time.DayOfWeek
import java.time.LocalTime


// 词典配置
@Serializable
data class DictionaryConfig(
    val learningGoal: String, // 学习目标
    val readingInterests: List<String>, // 阅读兴趣
    val proficiencyLevel: String, // 等级
    val customWords: List<String>, // 自己的word
    val learningStyle: String // 学习的风格
)


// 学习计划数据类
@Serializable
data class LearningPlan(
    val id: String,
    val userId: String,
    val wordBookId: String,
    val dailyGoal: DailyGoal,
    val reviewSettings: ReviewSettings,
    val reminders: List<LearningReminder>,
    val difficultyLevel: DifficultyLevel = DifficultyLevel.NORMAL,
    val isActive: Boolean = true
)

// 每日学习目标
@Serializable
data class DailyGoal(
    val newWordsCount: Int = 10,  // 每日新词数量
    val reviewWordsCount: Int = 20,  // 每日复习数量
    val learningTimeMinutes: Int = 30,  // 每日学习时长（分钟）
    @Serializable(with = DayOfWeekSetSerializer::class)
    val activeDays: Set<DayOfWeek> = DayOfWeek.values().toSet()  // 学习日
)

// 复习设置
@Serializable
data class ReviewSettings(
    val intervalDays: List<Int> = listOf(1, 3, 7, 14, 30),  // 复习间隔天数
    val minCorrectRate: Float = 0.8f,  // 最低正确率要求
    val autoAdjustDifficulty: Boolean = true  // 是否自动调整难度
)

// 学习提醒
@Serializable
data class LearningReminder(
    val id: String,
    @Serializable(with = LocalTimeSerializer::class)
    val time: LocalTime,  // 提醒时间
    val isEnabled: Boolean = true,
    @Serializable(with = DayOfWeekSetSerializer::class)
    val days: Set<DayOfWeek> = DayOfWeek.values().toSet()  // 提醒日期
)

// 难度等级
@Serializable
enum class DifficultyLevel {
    EASY,       // 简单
    NORMAL,     // 正常
    HARD,       // 困难
    ADAPTIVE    // 自适应（根据学习情况自动调整）
}

// DayOfWeek Set 序列化器
object DayOfWeekSetSerializer : kotlinx.serialization.KSerializer<Set<DayOfWeek>> {
    private val serializer = kotlinx.serialization.builtins.SetSerializer(DayOfWeekSerializer)
    override val descriptor = serializer.descriptor
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Set<DayOfWeek>) = serializer.serialize(encoder, value)
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Set<DayOfWeek> = serializer.deserialize(decoder)
}

// 学习记录
data class LearningRecord(
    val id: String,
    val userId: String,
    val wordBookId: String,
    val date: String,  // YYYY-MM-DD
    val newWordsLearned: Int = 0,
    val wordsReviewed: Int = 0,
    val correctRate: Float = 0f,
    val learningTimeMinutes: Int = 0,
    val completedGoals: Set<LearningGoal> = emptySet()
)

// 学习目标类型
enum class LearningGoal {
    DAILY_NEW_WORDS,      // 每日新词目标
    DAILY_REVIEW,         // 每日复习目标
    DAILY_TIME,          // 每日时长目标
    WEEKLY_COMPLETION,    // 每周完成度
    MONTHLY_COMPLETION    // 每月完成度
}

// 成就
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val type: AchievementType,
    val requiredValue: Int,
    val icon: String,
    val unlockedAt: String? = null  // 解锁时间，为null表示未解锁
)

// 成就类型
enum class AchievementType {
    WORDS_LEARNED,        // 学习单词总数
    DAILY_STREAK,        // 连续学习天数
    REVIEW_COMPLETED,    // 完成复习次数
    TIME_SPENT,         // 累计学习时长
    ACCURACY_RATE       // 正确率达标
} 