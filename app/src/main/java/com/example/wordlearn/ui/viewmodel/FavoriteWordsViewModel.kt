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

private const val TAG = "FavoriteWordsViewModel"

class FavoriteWordsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as App).vocabularyRepository
    
    // 收藏单词列表
    private val _favoriteWords = MutableStateFlow<List<Word>>(emptyList())
    val favoriteWords: StateFlow<List<Word>> = _favoriteWords.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * 加载收藏单词
     */
    fun loadFavoriteWords() {
        Log.d(TAG, "正在加载收藏单词")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val words = repository.getFavoriteWords()
                Log.d(TAG, "加载到${words.size}个收藏单词")
                _favoriteWords.value = words
            } catch (e: Exception) {
                Log.e(TAG, "加载收藏单词失败", e)
                _favoriteWords.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 切换收藏状态
     */
    fun toggleFavorite(wordId: Long, isFavorite: Boolean) {
        Log.d(TAG, "切换收藏状态：ID=$wordId, isFavorite=$isFavorite")
        viewModelScope.launch {
            try {
                repository.toggleFavorite(wordId, isFavorite)
                // 如果取消收藏，从列表中移除
                if (!isFavorite) {
                    _favoriteWords.value = _favoriteWords.value.filter { it.id != wordId }
                    Log.d(TAG, "已从收藏列表移除：ID=$wordId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "切换收藏状态失败", e)
            }
        }
    }
} 