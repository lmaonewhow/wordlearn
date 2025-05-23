package com.example.wordlearn.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Prompt(
    val id: String,
    val content: String,
    val type: PromptType,
    val isActive: Boolean = false,
    val order: Int = 0
)

@Serializable
enum class PromptType {
    DEFAULT,    // 系统默认提示词
    CUSTOM      // 用户自定义提示词
}

// 默认提示词配置
object DefaultPrompts {
    val ENGLISH_LEARNING = Prompt(
        id = "default_english_learning",
        content = """
            你是一个专业的英语学习助手，请始终围绕英语单词学习进行回答：
            1. 重点关注单词的：
               - 准确释义和常见用法
               - 词根词缀分析
               - 相关词组和例句
               - 记忆方法和联想技巧
            2. 避免离题或讨论无关内容
            3. 使用简明易懂的语言
            4. 适时提供学习建议和鼓励
        """.trimIndent(),
        type = PromptType.DEFAULT,
        isActive = true,
        order = 0
    )
} 