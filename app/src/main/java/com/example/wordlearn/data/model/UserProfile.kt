package com.example.wordlearn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val learningGoal: LearningGoal = LearningGoal.EXAM,
    val readingInterests: List<ReadingInterest> = emptyList(),
    val proficiencyLevel: ProficiencyLevel = ProficiencyLevel.BEGINNER,
    val customWords: List<String> = emptyList(),
    val learningStyle: LearningStyle = LearningStyle.PRACTICE,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
enum class LearningGoal {
    EXAM, ABROAD, WORK, INTEREST
}

@Serializable
enum class ReadingInterest {
    NOVEL, TECH, BUSINESS, GAME
}

@Serializable
enum class ProficiencyLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}

@Serializable
enum class LearningStyle {
    PRACTICE, AI_EXPLAIN, CONVERSATION
}

// 词典配置
@Serializable
data class DictionaryConfig(
    val id: Long = 0,
    val name: String,
    val type: DictionaryType,
    val userProfileId: Long,
    val words: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class DictionaryType {
    EXAM_PREP, AI_GENERATED, CUSTOM
} 