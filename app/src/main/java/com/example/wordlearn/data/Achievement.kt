package com.example.wordlearn.data

import java.time.LocalDateTime

/**
 * 成就类型枚举
 */
enum class AchievementType {
    FIRST_WORD,           // 第一次学习单词
    WORDS_LEARNED,        // 学习单词数量里程碑
    DAILY_STREAK,         // 连续学习天数
    REVIEW_COMPLETED,     // 完成复习
    GAME_MASTER,          // 游戏达人(玩过所有游戏)
    GAME_PLAYED,          // 玩过特定游戏
    ACCURACY_RATE,        // 准确率达标
    WORD_CHAIN_MASTER,    // 单词接龙高手
    MEMORY_MASTER,        // 速记挑战高手
    WORD_MATCH_MASTER,    // 词义匹配高手
    FILL_BLANKS_MASTER    // 单词填空高手
}

/**
 * 成就实体类
 *
 * @param id 成就唯一标识
 * @param type 成就类型
 * @param name 成就名称
 * @param description 成就描述
 * @param imageRes 成就图标资源ID(可选)
 * @param threshold 成就达成阈值(例如：学习10个单词)
 * @param unlockedAt 解锁时间(null表示未解锁)
 * @param progress 当前进度(0-1之间的浮点数)
 */
data class Achievement(
    val id: String,
    val type: AchievementType,
    val name: String,
    val description: String,
    val imageRes: Int? = null,
    val threshold: Int = 1,
    val unlockedAt: LocalDateTime? = null,
    val progress: Float = 0f
) {
    companion object {
        // 预定义成就列表
        fun getDefaultAchievements(): List<Achievement> = listOf(
            // 初次学习相关
            Achievement(
                id = "first_word",
                type = AchievementType.FIRST_WORD,
                name = "小试牛刀",
                description = "首次学习单词",
                threshold = 1
            ),
            
            // 学习进度相关
            Achievement(
                id = "learned_10",
                type = AchievementType.WORDS_LEARNED,
                name = "初学乍练",
                description = "学习10个单词",
                threshold = 10
            ),
            Achievement(
                id = "learned_50",
                type = AchievementType.WORDS_LEARNED,
                name = "稳步前进",
                description = "学习50个单词",
                threshold = 50
            ),
            Achievement(
                id = "learned_100",
                type = AchievementType.WORDS_LEARNED,
                name = "百词斩",
                description = "学习100个单词",
                threshold = 100
            ),
            Achievement(
                id = "learned_500",
                type = AchievementType.WORDS_LEARNED,
                name = "词汇大师",
                description = "学习500个单词",
                threshold = 500
            ),
            
            // 坚持学习相关
            Achievement(
                id = "streak_3",
                type = AchievementType.DAILY_STREAK,
                name = "初见成效",
                description = "连续学习3天",
                threshold = 3
            ),
            Achievement(
                id = "streak_7",
                type = AchievementType.DAILY_STREAK,
                name = "一周坚持",
                description = "连续学习7天",
                threshold = 7
            ),
            Achievement(
                id = "streak_30",
                type = AchievementType.DAILY_STREAK,
                name = "月月不断",
                description = "连续学习30天",
                threshold = 30
            ),
            
            // 复习相关
            Achievement(
                id = "review_first",
                type = AchievementType.REVIEW_COMPLETED,
                name = "温故知新",
                description = "首次完成复习",
                threshold = 1
            ),
            
            // 游戏相关
            Achievement(
                id = "game_master",
                type = AchievementType.GAME_MASTER,
                name = "游戏达人",
                description = "体验过所有游戏",
                threshold = 4  // 对应4种游戏
            ),
            Achievement(
                id = "word_match_played",
                type = AchievementType.GAME_PLAYED,
                name = "连线高手",
                description = "首次体验词义匹配游戏",
                threshold = 1
            ),
            Achievement(
                id = "fill_blanks_played",
                type = AchievementType.GAME_PLAYED,
                name = "填空专家",
                description = "首次体验单词填空游戏",
                threshold = 1
            ),
            Achievement(
                id = "word_chain_played",
                type = AchievementType.GAME_PLAYED,
                name = "接龙挑战者",
                description = "首次体验单词接龙游戏",
                threshold = 1
            ),
            Achievement(
                id = "memory_played",
                type = AchievementType.GAME_PLAYED,
                name = "记忆大师",
                description = "首次体验速记挑战游戏",
                threshold = 1
            ),
            
            // 游戏成就
            Achievement(
                id = "word_match_master",
                type = AchievementType.WORD_MATCH_MASTER,
                name = "匹配王者",
                description = "在词义匹配游戏中获得100分",
                threshold = 100
            ),
            Achievement(
                id = "fill_blanks_master",
                type = AchievementType.FILL_BLANKS_MASTER,
                name = "填空无敌",
                description = "在单词填空游戏中连续答对10个单词",
                threshold = 10
            ),
            Achievement(
                id = "word_chain_master",
                type = AchievementType.WORD_CHAIN_MASTER,
                name = "接龙传奇",
                description = "在单词接龙游戏中达成10个单词的接龙",
                threshold = 10
            ),
            Achievement(
                id = "memory_master",
                type = AchievementType.MEMORY_MASTER,
                name = "超强记忆",
                description = "在速记挑战中达到3级并保持80%以上的准确率",
                threshold = 3
            )
        )
    }
} 