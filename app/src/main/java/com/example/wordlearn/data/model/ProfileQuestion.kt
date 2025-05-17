package com.example.wordlearn.data.model

sealed class ProfileQuestion<T>(
    val id: String,
    val question: String,
    val type: QuestionType
) {
    data class SingleChoice(
        val questionId: String,
        val questionText: String,
        val options: List<String>
    ) : ProfileQuestion<String>(questionId, questionText, QuestionType.SINGLE_CHOICE)

    data class MultiChoice(
        val questionId: String,
        val questionText: String,
        val options: List<String>
    ) : ProfileQuestion<List<String>>(questionId, questionText, QuestionType.MULTI_CHOICE)

    data class TextInput(
        val questionId: String,
        val questionText: String,
        val hint: String = ""
    ) : ProfileQuestion<String>(questionId, questionText, QuestionType.TEXT_INPUT)
}

enum class QuestionType {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    TEXT_INPUT
}

// 预定义问题列表
object Questions {
    val all = listOf(
        ProfileQuestion.SingleChoice(
            "Q1",
            "你学习英语的主要目标是什么？",
            listOf("考试", "出国", "工作", "兴趣")
        ),
        ProfileQuestion.MultiChoice(
            "Q2",
            "你喜欢阅读哪类内容？",
            listOf("小说", "科技", "商业", "游戏")
        ),
        ProfileQuestion.SingleChoice(
            "Q3",
            "你的英语水平大概在哪个阶段？",
            listOf("初级", "中级", "高级")
        ),
        ProfileQuestion.TextInput(
            "Q4",
            "输入你最近遇到的单词吧（可选）",
            "请输入单词，多个单词用空格分隔"
        ),
        ProfileQuestion.SingleChoice(
            "Q5",
            "你更喜欢哪种学习方式？",
            listOf("刷题", "AI 讲解", "对话练习")
        )
    )
} 