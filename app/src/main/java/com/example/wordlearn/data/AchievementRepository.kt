package com.example.wordlearn.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * 成就系统管理类
 */
class AchievementRepository(private val context: Context) {

    companion object {
        private const val PREF_NAME = "achievement_prefs"
        private const val KEY_ACHIEVEMENTS = "achievements"
        private const val KEY_GAME_PLAYS = "game_plays"
        private const val TAG = "AchievementRepository"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // 当前成就列表流
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements
    
    // 游戏体验记录 (游戏ID -> 是否体验过)
    private val gamePlayRecord = mutableMapOf<String, Boolean>()
    
    init {
        // 加载已有成就或初始化默认成就
        loadAchievements()
        loadGamePlays()
    }
    
    /**
     * 加载已保存的成就
     */
    private fun loadAchievements() {
        try {
            val achievementsJson = prefs.getString(KEY_ACHIEVEMENTS, null)
            if (achievementsJson != null) {
                val type: Type = object : TypeToken<List<Achievement>>() {}.type
                val loadedAchievements: List<Achievement> = gson.fromJson(achievementsJson, type)
                _achievements.value = loadedAchievements
                Log.d(TAG, "已加载${loadedAchievements.size}个成就")
            } else {
                // 首次运行，初始化默认成就
                _achievements.value = Achievement.getDefaultAchievements()
                saveAchievements()
                Log.d(TAG, "初始化默认成就列表")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载成就失败: ${e.message}")
            // 出错时使用默认成就
            _achievements.value = Achievement.getDefaultAchievements()
            saveAchievements()
        }
    }
    
    /**
     * 加载游戏体验记录
     */
    private fun loadGamePlays() {
        try {
            val gamePlayJson = prefs.getString(KEY_GAME_PLAYS, null)
            if (gamePlayJson != null) {
                val type: Type = object : TypeToken<Map<String, Boolean>>() {}.type
                val loadedGamePlays: Map<String, Boolean> = gson.fromJson(gamePlayJson, type)
                gamePlayRecord.putAll(loadedGamePlays)
                Log.d(TAG, "已加载游戏记录: $gamePlayRecord")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载游戏记录失败: ${e.message}")
        }
    }
    
    /**
     * 保存成就列表
     */
    private fun saveAchievements() {
        try {
            val achievementsJson = gson.toJson(_achievements.value)
            prefs.edit().putString(KEY_ACHIEVEMENTS, achievementsJson).apply()
            Log.d(TAG, "成就已保存")
        } catch (e: Exception) {
            Log.e(TAG, "保存成就失败: ${e.message}")
        }
    }
    
    /**
     * 保存游戏体验记录
     */
    private fun saveGamePlays() {
        try {
            val gamePlayJson = gson.toJson(gamePlayRecord)
            prefs.edit().putString(KEY_GAME_PLAYS, gamePlayJson).apply()
            Log.d(TAG, "游戏记录已保存")
        } catch (e: Exception) {
            Log.e(TAG, "保存游戏记录失败: ${e.message}")
        }
    }
    
    /**
     * 解锁特定成就
     */
    fun unlockAchievement(achievementId: String) {
        val currentAchievements = _achievements.value.toMutableList()
        val achievementIndex = currentAchievements.indexOfFirst { it.id == achievementId }
        
        if (achievementIndex != -1) {
            val achievement = currentAchievements[achievementIndex]
            // 只有未解锁的成就才能解锁
            if (achievement.unlockedAt == null) {
                val updatedAchievement = achievement.copy(
                    unlockedAt = LocalDateTime.now(),
                    progress = 1.0f
                )
                currentAchievements[achievementIndex] = updatedAchievement
                _achievements.value = currentAchievements
                saveAchievements()
                
                Log.d(TAG, "成就已解锁: ${achievement.name}")
            }
        }
    }
    
    /**
     * 更新成就进度
     */
    fun updateProgress(achievementId: String, currentProgress: Int) {
        val currentAchievements = _achievements.value.toMutableList()
        val achievementIndex = currentAchievements.indexOfFirst { it.id == achievementId }
        
        if (achievementIndex != -1) {
            val achievement = currentAchievements[achievementIndex]
            // 只更新未解锁的成就
            if (achievement.unlockedAt == null) {
                val normalizedProgress = currentProgress.toFloat() / achievement.threshold
                val updatedProgress = normalizedProgress.coerceIn(0f, 1f)
                
                // 如果达到阈值，解锁成就
                if (currentProgress >= achievement.threshold) {
                    val updatedAchievement = achievement.copy(
                        unlockedAt = LocalDateTime.now(),
                        progress = 1.0f
                    )
                    currentAchievements[achievementIndex] = updatedAchievement
                    Log.d(TAG, "成就已解锁: ${achievement.name}")
                } else {
                    // 否则只更新进度
                    val updatedAchievement = achievement.copy(progress = updatedProgress)
                    currentAchievements[achievementIndex] = updatedAchievement
                    Log.d(TAG, "成就进度更新: ${achievement.name}, $updatedProgress")
                }
                
                _achievements.value = currentAchievements
                saveAchievements()
            }
        }
    }
    
    /**
     * 记录学习的单词数量
     */
    fun recordWordLearned(count: Int = 1) {
        Log.d(TAG, "记录学习单词：$count 个，当前总学习数：${getTotalLearnedWords()}")
        
        // 首次学习单词成就
        if (count > 0) {
            // 检查该成就是否已解锁
            val firstWordAchievement = _achievements.value.find { it.id == "first_word" }
            if (firstWordAchievement?.unlockedAt == null) {
                Log.d(TAG, "尝试解锁首次学习成就")
                unlockAchievement("first_word")
            } else {
                Log.d(TAG, "首次学习成就已解锁于：${firstWordAchievement.unlockedAt}")
            }
        }
        
        // 更新学习数量里程碑成就
        val wordsLearnedAchievements = _achievements.value.filter { 
            it.type == AchievementType.WORDS_LEARNED && it.unlockedAt == null 
        }
        
        // 获取总学习单词数
        val totalLearned = getTotalLearnedWords() + count
        
        // 更新每个相关成就
        wordsLearnedAchievements.forEach { achievement ->
            updateProgress(achievement.id, totalLearned)
        }
        
        // 保存新的总数
        prefs.edit().putInt("total_words_learned", totalLearned).apply()
        Log.d(TAG, "更新后总学习单词数：$totalLearned")
    }
    
    /**
     * 获取已学习的总单词数
     */
    private fun getTotalLearnedWords(): Int {
        return prefs.getInt("total_words_learned", 0)
    }
    
    /**
     * 记录连续学习天数
     */
    fun recordDailyStreak(streakDays: Int) {
        val streakAchievements = _achievements.value.filter {
            it.type == AchievementType.DAILY_STREAK && it.unlockedAt == null
        }
        
        streakAchievements.forEach { achievement ->
            updateProgress(achievement.id, streakDays)
        }
    }
    
    /**
     * 记录首次完成复习
     */
    fun recordReviewCompleted() {
        unlockAchievement("review_first")
    }
    
    /**
     * 记录游戏体验
     */
    fun recordGamePlayed(gameId: String) {
        // 如果是第一次玩这个游戏
        if (gamePlayRecord[gameId] != true) {
            gamePlayRecord[gameId] = true
            saveGamePlays()
            
            // 解锁特定游戏成就
            when (gameId) {
                "word_match" -> unlockAchievement("word_match_played")
                "fill_blanks" -> unlockAchievement("fill_blanks_played")
                "word_chain" -> unlockAchievement("word_chain_played")
                "memory_challenge" -> unlockAchievement("memory_played")
            }
            
            // 检查是否解锁游戏达人成就
            checkGameMasterAchievement()
        }
    }
    
    /**
     * 检查是否解锁游戏达人成就
     */
    private fun checkGameMasterAchievement() {
        // 判断所有游戏是否都玩过
        val allGamesPlayed = listOf("word_match", "fill_blanks", "word_chain", "memory_challenge")
            .all { gamePlayRecord[it] == true }
            
        if (allGamesPlayed) {
            unlockAchievement("game_master")
        }
    }
    
    /**
     * 记录词义匹配游戏分数
     */
    fun recordWordMatchScore(score: Int) {
        val achievement = _achievements.value.find { it.id == "word_match_master" }
        achievement?.let {
            updateProgress(it.id, score)
        }
    }
    
    /**
     * 记录单词填空连续正确数
     */
    fun recordFillBlanksStreak(streak: Int) {
        val achievement = _achievements.value.find { it.id == "fill_blanks_master" }
        achievement?.let {
            updateProgress(it.id, streak)
        }
    }
    
    /**
     * 记录单词接龙数量
     */
    fun recordWordChainLength(chainLength: Int) {
        val achievement = _achievements.value.find { it.id == "word_chain_master" }
        achievement?.let {
            updateProgress(it.id, chainLength)
        }
    }
    
    /**
     * 记录速记挑战级别和准确率
     */
    fun recordMemoryChallengeResult(level: Int, accuracy: Float) {
        // 只有当准确率>=80%时才记录关卡
        if (accuracy >= 0.8f) {
            val achievement = _achievements.value.find { it.id == "memory_master" }
            achievement?.let {
                updateProgress(it.id, level)
            }
        }
    }
} 