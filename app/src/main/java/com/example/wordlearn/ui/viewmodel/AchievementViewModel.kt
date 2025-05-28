package com.example.wordlearn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.Achievement
import com.example.wordlearn.data.AchievementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 成就系统ViewModel
 */
class AchievementViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AchievementRepository(application.applicationContext)
    
    // 所有成就流
    val achievements = repository.achievements
    
    // 已解锁成就流
    private val _unlockedAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val unlockedAchievements: StateFlow<List<Achievement>> = _unlockedAchievements
    
    // 未解锁但有进度的成就流
    private val _inProgressAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val inProgressAchievements: StateFlow<List<Achievement>> = _inProgressAchievements
    
    // 新解锁的成就流
    private val _newlyUnlockedAchievement = MutableStateFlow<Achievement?>(null)
    val newlyUnlockedAchievement: StateFlow<Achievement?> = _newlyUnlockedAchievement
    
    // 保存上一次的已解锁成就列表，用于检测新解锁
    private var previousUnlocked = listOf<Achievement>()
    
    init {
        viewModelScope.launch {
            // 监听成就变化，更新已解锁和进行中的成就
            achievements.collect { allAchievements ->
                val unlocked = allAchievements.filter { it.unlockedAt != null }
                _unlockedAchievements.value = unlocked
                _inProgressAchievements.value = allAchievements.filter { 
                    it.unlockedAt == null && it.progress > 0f 
                }
                
                // 检测新解锁的成就
                if (previousUnlocked.isNotEmpty()) {
                    val newUnlocked = unlocked.filter { achievement ->
                        !previousUnlocked.any { it.id == achievement.id }
                    }
                    
                    if (newUnlocked.isNotEmpty()) {
                        _newlyUnlockedAchievement.value = newUnlocked.first()
                    }
                }
                
                // 更新上一次的已解锁列表
                previousUnlocked = unlocked
            }
        }
    }
    
    /**
     * 清除新解锁的成就通知
     */
    fun clearNewlyUnlockedAchievement() {
        _newlyUnlockedAchievement.value = null
    }
    
    /**
     * 记录学习单词
     */
    fun recordWordLearned(count: Int = 1) {
        repository.recordWordLearned(count)
    }
    
    /**
     * 记录连续学习天数
     */
    fun recordDailyStreak(days: Int) {
        repository.recordDailyStreak(days)
    }
    
    /**
     * 记录完成复习
     */
    fun recordReviewCompleted() {
        repository.recordReviewCompleted()
    }
    
    /**
     * 记录玩过游戏
     */
    fun recordGamePlayed(gameId: String) {
        repository.recordGamePlayed(gameId)
    }
    
    /**
     * 记录词义匹配游戏分数
     */
    fun recordWordMatchScore(score: Int) {
        repository.recordWordMatchScore(score)
    }
    
    /**
     * 记录单词填空连续正确数
     */
    fun recordFillBlanksStreak(streak: Int) {
        repository.recordFillBlanksStreak(streak)
    }
    
    /**
     * 记录单词接龙长度
     */
    fun recordWordChainLength(chainLength: Int) {
        repository.recordWordChainLength(chainLength)
    }
    
    /**
     * 记录速记挑战结果
     */
    fun recordMemoryChallengeResult(level: Int, accuracy: Float) {
        repository.recordMemoryChallengeResult(level, accuracy)
    }
} 