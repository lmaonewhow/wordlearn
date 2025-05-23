package com.example.wordlearn.data.model

data class ChatRequest(
    val model: String = "qwen-turbo",
    val messages: List<ChatMessage> = emptyList(),
    val parameters: ChatParameters = ChatParameters()
)

data class ChatMessage(
    val role: String,
    val content: String,
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatParameters(
    val temperature: Double = 0.8,
    val top_p: Double = 0.8,
    val max_tokens: Int = 1500
)

data class ChatResponse(
    val id: String,
    val obj: String,
    val created: Long,
    val choices: List<Choice>,
    val usage: ChatUsage
)

data class Choice(
    val index: Int,
    val message: AssistantMessage,
    val finish_reason: String
)

data class AssistantMessage(
    val role: String,
    val content: String
)

data class ChatUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
) 