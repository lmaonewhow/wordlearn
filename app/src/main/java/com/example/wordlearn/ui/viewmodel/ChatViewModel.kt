package com.example.wordlearn.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.api.ChatService
import com.example.wordlearn.data.model.ChatInput
import com.example.wordlearn.data.model.ChatMessage
import com.example.wordlearn.data.model.ChatRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()

    fun sendMessage(content: String) {
        // 添加用户消息
        _messages.add(ChatMessage(content = content, isUser = true))

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _currentResponse.value = ""

                // 创建请求
                val request = ChatRequest(
                    input = ChatInput(prompt = content)
                )

                // 发送请求
                val response = ChatService.api.chat(
                    auth = "Bearer ${ChatService.getApiKey()}",
                    request = request
                )

                // 模拟流式输出
                response.output.text.forEachIndexed { index, char ->
                    _currentResponse.value += char
                    kotlinx.coroutines.delay(50) // 每个字符间隔 50ms
                }

                // 添加完整回复
                _messages.add(ChatMessage(
                    content = response.output.text,
                    isUser = false
                ))
                _currentResponse.value = ""

            } catch (e: Exception) {
                // 添加错误消息
                _messages.add(ChatMessage(
                    content = "抱歉，出现了一些问题：${e.message}",
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