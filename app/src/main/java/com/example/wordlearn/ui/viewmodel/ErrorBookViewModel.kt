package com.example.wordlearn.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.App
import com.example.wordlearn.data.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ErrorBookViewModel"

class ErrorBookViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as App).vocabularyRepository
    
    // 错题列表
    private val _errorWords = MutableStateFlow<List<Word>>(emptyList())
    val errorWords: StateFlow<List<Word>> = _errorWords.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * 加载错题
     */
    fun loadErrorWords() {
        Log.d(TAG, "正在加载错题")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val words = repository.getErrorWords()
                Log.d(TAG, "加载到${words.size}个错题")
                _errorWords.value = words
            } catch (e: Exception) {
                Log.e(TAG, "加载错题失败", e)
                _errorWords.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 