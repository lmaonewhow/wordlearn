package com.example.wordlearn.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.api.ChatService
import com.example.wordlearn.data.manager.PromptManager
import com.example.wordlearn.data.model.ChatMessage
import com.example.wordlearn.data.model.ChatRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

class ChatViewModel : ViewModel() {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()

    private lateinit var promptManager: PromptManager
    private val TAG = "ChatViewModel"

    fun initialize(context: Context) {
        Log.d(TAG, "初始化ChatViewModel")
        promptManager = PromptManager(context)
        viewModelScope.launch {
            promptManager.initialize()
        }
    }

    fun sendMessage(content: String) {
        // 添加用户消息
        val userMessage = ChatMessage(
            role = "user",
            content = content,
            isUser = true
        )
        _messages.add(userMessage)

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _currentResponse.value = ""

                // 获取当前活动的提示词
                val currentPrompt = promptManager.getCurrentPrompt()
                Log.d(TAG, "使用提示词：${currentPrompt.id}")

                // 创建系统提示消息
                val systemMessage = ChatMessage(
                    role = "system",
                    content = currentPrompt.content,
                    isUser = false
                )

                // 创建消息列表
                val messageList = mutableListOf(systemMessage)
                messageList.addAll(_messages.filter { it.isUser }) // 只添加用户消息

                // 创建请求
                val request = ChatRequest(
                    messages = messageList
                )

                // 发送请求
                val response = ChatService.api.chat(
                    auth = "Bearer ${ChatService.getApiKey()}",
                    request = request
                )

                // 获取助手的回复
                val assistantMessage = response.choices.firstOrNull()?.message
                if (assistantMessage != null) {
                    // 模拟流式输出
                    assistantMessage.content.forEach { char ->
                        _currentResponse.value += char
                        kotlinx.coroutines.delay(50) // 每个字符间隔 50ms
                    }

                    // 添加完整回复
                    _messages.add(ChatMessage(
                        role = "assistant",
                        content = assistantMessage.content,
                        isUser = false
                    ))
                    _currentResponse.value = ""
                } else {
                    throw Exception("未收到有效的回复")
                }

            } catch (e: Exception) {
                Log.e(TAG, "发送消息失败", e)
                val errorMessage = when (e) {
                    is SocketTimeoutException -> "网络请求超时，请检查网络连接后重试"
                    is UnknownHostException -> "无法连接到服务器，请检查网络连接"
                    is HttpException -> when (e.code()) {
                        401 -> "API密钥无效，请联系管理员"
                        403 -> "没有访问权限，请联系管理员"
                        429 -> "请求过于频繁，请稍后再试"
                        500 -> "服务器内部错误，请稍后再试"
                        else -> "网络请求失败（${e.code()}），请稍后重试"
                    }
                    else -> "发生错误：${e.message}"
                }
                
                // 添加错误消息
                _messages.add(ChatMessage(
                    role = "assistant",
                    content = errorMessage,
                    isUser = false
                ))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _messages.clear()
    }
} 