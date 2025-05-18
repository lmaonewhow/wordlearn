package com.example.wordlearn.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.model.BookType
import com.example.wordlearn.data.model.VocabularyBook
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.WordCard
import com.example.wordlearn.data.repository.VocabularyRepository
import com.example.wordlearn.data.LearningPlan
import com.example.wordlearn.data.LearningPlanRepository
import com.example.wordlearn.data.store.AppSettingsKeys
import com.example.wordlearn.data.store.settingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.DayOfWeek
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit

private const val TAG = "LearningViewModel"

// 学习状态
sealed class LearningState {
    object Loading : LearningState()
    object Success : LearningState()
    data class Error(val message: String) : LearningState()
}

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VocabularyRepository(application)
    private val learningPlanRepository = LearningPlanRepository(application)

    // 学习状态
    private val _learningState = MutableStateFlow<LearningState>(LearningState.Success)
    val learningState: StateFlow<LearningState> = _learningState.asStateFlow()

    // 当前单词状态
    private val _currentWord = MutableStateFlow<WordCard?>(null)
    val currentWord: StateFlow<WordCard?> = _currentWord.asStateFlow()

    // 学习进度状态
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    // 总单词数
    private val _totalWords = MutableStateFlow(0)
    val totalWords: StateFlow<Int> = _totalWords.asStateFlow()

    // 当前词汇书
    private val _currentBook = MutableStateFlow<VocabularyBook?>(null)
    val currentBook: StateFlow<VocabularyBook?> = _currentBook.asStateFlow()

    // 今日学习目标
    private val _dailyGoal = MutableStateFlow(0)
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    // 今日已学习数量
    private val _todayLearned = MutableStateFlow(0)
    val todayLearned: StateFlow<Int> = _todayLearned.asStateFlow()

    // 单词列表
    private var wordList = listOf<Word>()

    // 未掌握的单词列表（索引）
    private val unknownWords = mutableSetOf<Int>()

    // 学习计划状态
    private val _learningPlan = MutableStateFlow<LearningPlan?>(null)
    val learningPlan: StateFlow<LearningPlan?> = _learningPlan.asStateFlow()

    // 是否忽略学习日检查
    private var ignoreLearningDayCheck = false

    // SharedPreferences 实例
    private lateinit var prefs: SharedPreferences

    init {
        // 加载学习计划
        viewModelScope.launch {
            learningPlanRepository.learningPlan.collect { plan ->
                _learningPlan.value = plan
                plan?.dailyGoal?.newWordsCount?.let { count ->
                    _dailyGoal.value = count
                }
            }
        }
    }

    // 初始化 SharedPreferences
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
        loadProgress()
    }

    // 加载保存的进度
    private fun loadProgress() {
        // 从 SharedPreferences 获取词书名称
        val savedBookName = prefs.getString("current_book", null)
        // 如果有保存的词书名称，找到对应的词书对象
        if (savedBookName != null) {
            _currentBook.value = repository.getAvailableBooks().find { it.name == savedBookName }
        }
        _progress.value = prefs.getInt("progress", 0)
        _todayLearned.value = prefs.getInt("today_learned", 0)
        val unknownWordsString = prefs.getString("unknown_words", "")
        if (unknownWordsString?.isNotEmpty() == true) {
            unknownWords.clear()
            unknownWords.addAll(unknownWordsString.split(",").map { it.toInt() })
        }
    }

    // 保存进度
    private fun saveProgress() {
        prefs.edit().apply {
            // 保存词书名称而不是整个对象
            putString("current_book", _currentBook.value?.name)
            putInt("progress", _progress.value)
            putInt("today_learned", _todayLearned.value)
            putString("unknown_words", unknownWords.joinToString(","))
            apply()
        }
    }

    // 检查今天是否是学习日
    private fun isLearningDay(): Boolean {
        if (ignoreLearningDayCheck) return true
        val today = LocalDate.now().dayOfWeek
        return _learningPlan.value?.dailyGoal?.activeDays?.contains(today) ?: true
    }

    // 检查是否达到每日学习目标
    private fun hasReachedDailyGoal(): Boolean {
        val plan = _learningPlan.value ?: return false
        return _todayLearned.value >= plan.dailyGoal.newWordsCount
    }

    // 获取可用词汇书列表
    fun getAvailableBooks(): List<VocabularyBook> {
        Log.d(TAG, "正在获取可用词汇书")
        return repository.getAvailableBooks()
    }

    // 加载词汇书
    fun loadVocabularyBook(book: VocabularyBook) {
        Log.d(TAG, "正在加载词汇书：${book.name}")
        viewModelScope.launch {
            try {
                _learningState.value = LearningState.Loading
                Log.d(TAG, "状态已更改为加载中")
                
                // 检查今天是否是学习日
                if (!isLearningDay()) {
                    _learningState.value = LearningState.Error("今天不是学习日哦，休息一下吧！")
                    return@launch
                }
                
                // 检查是否达到每日目标
                if (hasReachedDailyGoal()) {
                    _learningState.value = LearningState.Error("今天的学习目标已经完成啦！\n目标：${_dailyGoal.value}词\n已学：${_todayLearned.value}词")
                    return@launch
                }
                
                _currentBook.value = book
                _progress.value = 0
                
                // 根据文件类型加载单词
                Log.d(TAG, "正在从${book.type}文件加载单词：${book.filePath}")
                wordList = when (book.type) {
                    BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                    BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                }
                
                // 计算剩余需要学习的单词数
                val remainingToLearn = _dailyGoal.value - _todayLearned.value
                
                // 如果词汇书中的单词数量少于剩余需要学习的数量，使用全部单词
                _totalWords.value = minOf(wordList.size, remainingToLearn)
                
                Log.d(TAG, "已加载${_totalWords.value}个单词，今日目标：${_dailyGoal.value}，已学习：${_todayLearned.value}")
                
                if (wordList.isEmpty()) {
                    Log.e(TAG, "单词列表为空")
                    _learningState.value = LearningState.Error("词汇书为空，请选择其他词汇书")
                    return@launch
                }
                
                // 加载第一个单词
                loadNextWord()
                _learningState.value = LearningState.Success
                Log.d(TAG, "状态已更改为成功")
                saveProgress()
            } catch (e: Exception) {
                Log.e(TAG, "加载词汇书时出错", e)
                _currentWord.value = null
                _totalWords.value = 0
                wordList = emptyList()
                _learningState.value = LearningState.Error("加载词汇书失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    // 加载下一个单词
    private fun loadNextWord() {
        viewModelScope.launch {
            try {
                _learningState.value = LearningState.Loading
                Log.d(TAG, "正在加载下一个单词，当前进度：${_progress.value}")
                
                if (_progress.value < wordList.size) {
                    val word = wordList[_progress.value]
                    Log.d(TAG, "正在加载单词：${word.word}")
                    
                    _currentWord.value = WordCard(
                        word = word.word,
                        ukPhonetic = word.ukPhonetic.ifEmpty { "/" + word.word + "/" },
                        usPhonetic = word.usPhonetic.ifEmpty { "/" + word.word + "/" },
                        example = word.example.ifEmpty { "${word.word} - ${word.meaning}" }
                    )
                } else {
                    Log.d(TAG, "没有更多单词需要加载")
                    // 学习完成，设置为null
                    _currentWord.value = null
                }
                _learningState.value = LearningState.Success
                Log.d(TAG, "状态已更改为成功")
            } catch (e: Exception) {
                Log.e(TAG, "加载下一个单词时出错", e)
                _currentWord.value = null
                _learningState.value = LearningState.Error("加载单词失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    // 处理用户的选择
    fun handleWordChoice(isKnown: Boolean) {
        Log.d(TAG, "处理用户选择：${if (isKnown) "认识" else "不认识"}")
        viewModelScope.launch {
            try {
                _learningState.value = LearningState.Loading
                
                // 更新今日学习数量
                _todayLearned.value = _todayLearned.value + 1
                
                // 检查是否在有效范围内
                if (_progress.value < _totalWords.value) {
                    // 保存当前进度
                    val currentProgress = _progress.value
                    
                    // 如果不认识，加入未掌握列表
                    if (!isKnown) {
                        unknownWords.add(currentProgress)
                    }
                    
                    // 更新进度
                    _progress.value = currentProgress + 1
                    
                    // 计算并保存总体进度
                    val totalProgress = _progress.value.toFloat() / _totalWords.value.toFloat()
                    getApplication<Application>().settingsDataStore.edit { 
                        it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = totalProgress
                    }
                    
                    // 保存进度
                    saveProgress()
                    
                    // 检查是否达到每日目标
                    if (hasReachedDailyGoal()) {
                        _learningState.value = LearningState.Success
                        _currentWord.value = null
                        return@launch
                    }
                    
                    // 如果还有下一个单词，加载它
                    if (currentProgress + 1 < _totalWords.value) {
                        val nextWord = wordList[currentProgress + 1]
                        _currentWord.value = WordCard(
                            word = nextWord.word,
                            ukPhonetic = nextWord.ukPhonetic.ifEmpty { "/" + nextWord.word + "/" },
                            usPhonetic = nextWord.usPhonetic.ifEmpty { "/" + nextWord.word + "/" },
                            example = nextWord.example.ifEmpty { "${nextWord.word} - ${nextWord.meaning}" }
                        )
                    } else {
                        // 学习完成
                        _currentWord.value = null
                    }
                }
                _learningState.value = LearningState.Success
            } catch (e: Exception) {
                Log.e(TAG, "处理用户选择时出错", e)
                _learningState.value = LearningState.Error("处理选择失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    // 获取学习统计
    fun getLearningStats(): Triple<Int, Int, Int> {
        return try {
            val total = _totalWords.value
            val learned = _todayLearned.value
            val unknown = unknownWords.size
            Log.d(TAG, "学习统计 - 目标：$total，已学习：$learned，未掌握：$unknown")
            Triple(total, learned, unknown)
        } catch (e: Exception) {
            Log.e(TAG, "获取学习统计时出错", e)
            Triple(0, 0, 0)
        }
    }

    // 允许用户在非学习日继续学习
    fun overrideLearningDayCheck() {
        viewModelScope.launch {
            try {
                _learningState.value = LearningState.Loading
                
                // 重置状态
                ignoreLearningDayCheck = true
                _progress.value = 0
                unknownWords.clear()
                
                // 重新加载当前词汇书
                _currentBook.value?.let { book ->
                    // 根据文件类型加载单词
                    Log.d(TAG, "正在从${book.type}文件加载单词：${book.filePath}")
                    wordList = when (book.type) {
                        BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                        BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                    }
                    
                    // 计算剩余需要学习的单词数
                    val remainingToLearn = _dailyGoal.value - _todayLearned.value
                    
                    // 如果词汇书中的单词数量少于剩余需要学习的数量，使用全部单词
                    _totalWords.value = minOf(wordList.size, remainingToLearn)
                    
                    Log.d(TAG, "已加载${_totalWords.value}个单词，今日目标：${_dailyGoal.value}，已学习：${_todayLearned.value}")
                    
                    if (wordList.isEmpty()) {
                        Log.e(TAG, "单词列表为空")
                        _learningState.value = LearningState.Error("词汇书为空，请选择其他词汇书")
                        return@launch
                    }
                    
                    // 加载第一个单词
                    loadNextWord()
                    _learningState.value = LearningState.Success
                    Log.d(TAG, "状态已更改为成功")
                } ?: run {
                    _learningState.value = LearningState.Error("请先选择词汇书")
                }
            } catch (e: Exception) {
                Log.e(TAG, "重置进度时出错", e)
                _learningState.value = LearningState.Error("重置进度失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    // 重置学习进度
    fun resetProgress() {
        Log.d(TAG, "正在重置进度")
        viewModelScope.launch {
            try {
                _learningState.value = LearningState.Loading
                
                // 重置所有状态
                _progress.value = 0
                _todayLearned.value = 0  // 重置今日学习数量
                unknownWords.clear()
                ignoreLearningDayCheck = false
                
                // 重新加载当前词汇书
                _currentBook.value?.let { book ->
                    // 根据文件类型加载单词
                    Log.d(TAG, "正在从${book.type}文件加载单词：${book.filePath}")
                    wordList = when (book.type) {
                        BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                        BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                    }
                    
                    // 计算需要学习的单词数
                    val remainingToLearn = _dailyGoal.value - _todayLearned.value
                    
                    // 如果词汇书中的单词数量少于需要学习的数量，使用全部单词
                    _totalWords.value = minOf(wordList.size, remainingToLearn)
                    
                    Log.d(TAG, "已重新加载${_totalWords.value}个单词，今日目标：${_dailyGoal.value}")
                    
                    if (wordList.isEmpty()) {
                        Log.e(TAG, "单词列表为空")
                        _learningState.value = LearningState.Error("词汇书为空，请选择其他词汇书")
                        return@launch
                    }
                    
                    // 加载第一个单词
                    if (_totalWords.value > 0) {
                        val firstWord = wordList[0]
                        _currentWord.value = WordCard(
                            word = firstWord.word,
                            ukPhonetic = firstWord.ukPhonetic.ifEmpty { "/" + firstWord.word + "/" },
                            usPhonetic = firstWord.usPhonetic.ifEmpty { "/" + firstWord.word + "/" },
                            example = firstWord.example.ifEmpty { "${firstWord.word} - ${firstWord.meaning}" }
                        )
                        _learningState.value = LearningState.Success
                        Log.d(TAG, "已加载第一个单词：${firstWord.word}")
                    } else {
                        _currentWord.value = null
                        _learningState.value = LearningState.Error("今日学习任务已完成！")
                    }
                } ?: run {
                    _learningState.value = LearningState.Error("请先选择词汇书")
                }
            } catch (e: Exception) {
                Log.e(TAG, "重置进度时出错", e)
                _currentWord.value = null
                _totalWords.value = 0
                wordList = emptyList()
                _learningState.value = LearningState.Error("重置进度失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }
} 