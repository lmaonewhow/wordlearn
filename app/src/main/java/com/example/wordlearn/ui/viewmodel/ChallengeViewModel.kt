package com.example.wordlearn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ChallengeViewModel : ViewModel() {
    // 游戏状态
    private val _gameState = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val gameState: StateFlow<List<Pair<String, String>>> = _gameState.asStateFlow()

    // 选中的单词和释义
    private val _selectedWord = MutableStateFlow<String?>(null)
    val selectedWord: StateFlow<String?> = _selectedWord.asStateFlow()

    private val _selectedMeaning = MutableStateFlow<String?>(null)
    val selectedMeaning: StateFlow<String?> = _selectedMeaning.asStateFlow()

    // 得分
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    // 游戏是否结束
    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    // 已匹配的单词
    private val matchedWords = mutableSetOf<String>()

    fun initializeGame(isToday: Boolean) {
        viewModelScope.launch {
            // TODO: 从数据库或词库中获取单词数据
            // 这里先使用模拟数据
            val wordPairs = if (isToday) {
                getTodayWords()
            } else {
                getYesterdayWords()
            }
            _gameState.value = wordPairs
            resetGameState()
        }
    }

    private fun resetGameState() {
        _score.value = 0
        _selectedWord.value = null
        _selectedMeaning.value = null
        _isGameOver.value = false
        matchedWords.clear()
    }

    fun selectWord(word: String) {
        if (matchedWords.contains(word)) return
        _selectedWord.value = word
        checkMatch()
    }

    fun selectMeaning(meaning: String) {
        _selectedMeaning.value = meaning
        checkMatch()
    }

    private fun checkMatch() {
        val word = _selectedWord.value
        val meaning = _selectedMeaning.value

        if (word != null && meaning != null) {
            val isMatch = _gameState.value.any { 
                it.first == word && it.second == meaning 
            }

            if (isMatch) {
                // 匹配成功
                _score.value++
                matchedWords.add(word)
                
                // 清除选择
                _selectedWord.value = null
                _selectedMeaning.value = null

                // 检查游戏是否结束
                if (matchedWords.size == _gameState.value.size) {
                    _isGameOver.value = true
                }
            } else {
                // 匹配失败，清除选择
                _selectedWord.value = null
                _selectedMeaning.value = null
            }
        }
    }

    fun restartGame() {
        resetGameState()
    }

    // 模拟获取今日单词
    private fun getTodayWords(): List<Pair<String, String>> {
        return listOf(
            "apple" to "苹果",
            "banana" to "香蕉",
            "orange" to "橙子",
            "grape" to "葡萄",
            "peach" to "桃子"
        )
    }

    // 模拟获取昨日单词
    private fun getYesterdayWords(): List<Pair<String, String>> {
        return listOf(
            "cat" to "猫",
            "dog" to "狗",
            "bird" to "鸟",
            "fish" to "鱼",
            "rabbit" to "兔子"
        )
    }
} 