package com.example.wordlearn.data.model

data class ChatRequest(
    val model: String = "qwen-turbo",
    val input: ChatInput,
    val parameters: ChatParameters = ChatParameters()
)

data class ChatInput(
    val prompt: String
)

data class ChatParameters(
    val temperature: Double = 0.8
)

data class ChatResponse(
    val output: ChatOutput,
    val usage: ChatUsage,
    val request_id: String
)

data class ChatOutput(
    val text: String,
    val finish_reason: String
)

data class ChatUsage(
    val input_tokens: Int,
    val output_tokens: Int,
    val total_tokens: Int
)

// 聊天消息模型
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) 