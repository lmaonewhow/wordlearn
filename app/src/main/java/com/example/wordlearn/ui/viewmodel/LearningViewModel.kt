package com.example.wordlearn.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.App
import com.example.wordlearn.data.model.BookType
import com.example.wordlearn.data.model.VocabularyBook
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.WordCard
import com.example.wordlearn.data.model.WordStatus
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import com.example.wordlearn.ui.viewmodel.AchievementViewModel

private const val TAG = "LearningViewModel"

// 学习状态
sealed class LearningState {
    object Loading : LearningState()
    object Success : LearningState()
    data class Error(val message: String) : LearningState()
}

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val dataStore = context.settingsDataStore
    private val repository = (application as App).vocabularyRepository
    private val learningPlanRepository = LearningPlanRepository(application)
    private val achievementViewModel: AchievementViewModel = AchievementViewModel(application)

    // 从 DataStore 读取持久化的进度
    private val _persistedProgress = dataStore.data
        .map { it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
        
    // 保存已经读取的DataStore值，方便同步访问
    private var _persistedProgressValue: Float = 0f

    // 学习状态
    private val _learningState = MutableStateFlow<LearningState>(LearningState.Success)
    val learningState: StateFlow<LearningState> = _learningState.asStateFlow()

    // 当前单词状态
    private val _currentWord = MutableStateFlow<WordCard?>(null)
    val currentWord: StateFlow<WordCard?> = _currentWord.asStateFlow()

    // 学习进度状态
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    // Getter方法用于直接访问progress的值
    val progressValue: Int
        get() = _progress.value

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

    // 今日待复习数量
    private val _todayReviewCount = MutableStateFlow(0)
    val todayReviewCount: StateFlow<Int> = _todayReviewCount.asStateFlow()

    // 单词列表 - 内部使用
    private var _wordList = mutableListOf<Word>()
    // 暴露给UI的单词列表
    private val _wordListFlow = MutableStateFlow<List<Word>>(emptyList())
    val wordList: StateFlow<List<Word>> = _wordListFlow.asStateFlow()

    // 未掌握的单词列表（索引）
    private val unknownWords = mutableSetOf<Int>()

    // 学习计划状态
    private val _learningPlan = MutableStateFlow<LearningPlan?>(null)
    val learningPlan: StateFlow<LearningPlan?> = _learningPlan.asStateFlow()

    // 是否忽略学习日检查
    private var ignoreLearningDayCheck = false

    // SharedPreferences 实例
    private lateinit var prefs: SharedPreferences

    // 初始化状态
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // 待加载的词书
    private var pendingBook: VocabularyBook? = null

    init {
        // 加载学习计划
        viewModelScope.launch {
            // 订阅进度变化
            _persistedProgress.collect { progress ->
                _persistedProgressValue = progress
            }
        }
        
        viewModelScope.launch {
            learningPlanRepository.learningPlan.collect { plan ->
                _learningPlan.value = plan
                plan?.dailyGoal?.newWordsCount?.let { count ->
                    _dailyGoal.value = count
                    Log.d(TAG, "【诊断】初始化 - 设置每日学习目标: $count")
                } ?: run {
                    // 如果没有设置计划，设置一个默认值
                    _dailyGoal.value = 20
                    Log.d(TAG, "【诊断】初始化 - 使用默认每日目标: 20")
                }
            }
        }
        
        // 初始加载复习数量
        viewModelScope.launch {
            try {
                val count = updateReviewCount()
                Log.d(TAG, "【初始化】初始加载复习数量: $count")
            } catch (e: Exception) {
                Log.e(TAG, "【初始化】加载复习数量失败", e)
            }
        }
    }

    // 初始化 SharedPreferences
    fun initialize(context: Context) {
        Log.d(TAG, "【初始化】开始初始化ViewModel")
        
        if (_isInitialized.value) {
            Log.d(TAG, "【初始化】ViewModel已经初始化，处理待加载词书")
            pendingBook?.let { book ->
                loadVocabularyBook(book)
                pendingBook = null
            }
            return
        }
        
        viewModelScope.launch {
            try {
                // 初始化SharedPreferences
                prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
                
                // 从持久化存储加载进度
                loadProgress()
                
                // 设置每日目标 - 从LearningPlanRepository获取而不是硬编码
                if (_dailyGoal.value <= 0) {
                    // 从Flow中收集最新值
                    val plan = learningPlanRepository.learningPlan.firstOrNull()
                    _dailyGoal.value = plan?.dailyGoal?.newWordsCount ?: 20
                    Log.d(TAG, "【初始化】设置每日目标: ${_dailyGoal.value}")
                }
                
                // 立即更新今日待复习数量（同步获取）
                val reviewCount = withContext(Dispatchers.IO) {
                    repository.getTodayReviewWordsCount()
                }
                _todayReviewCount.value = reviewCount
                Log.d(TAG, "【初始化】同步获取待复习数量: $reviewCount")
                
                // 预加载当前词书
                _currentBook.value?.let { book ->
                    withContext(Dispatchers.IO) {
                        preloadWordBook(book)
                    }
                    
                    // 从保存的进度开始加载单词
                    if (_progress.value > 0 && _progress.value < _wordList.size) {
                        loadNextWord()
                    }
                }
                
                // 初始化完成
                _isInitialized.value = true
                Log.d(TAG, "【初始化】完成 - 每日目标: ${_dailyGoal.value}, 当前进度: ${_progress.value}, 总单词数: ${_totalWords.value}, 待复习单词: ${_todayReviewCount.value}")
                
                // 处理待加载的词书
                pendingBook?.let { book ->
                    Log.d(TAG, "【初始化】处理待加载词书：${book.name}")
                    loadVocabularyBook(book)
                    pendingBook = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "【初始化】失败", e)
                _learningState.value = LearningState.Error("初始化失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }
    
    // 预加载词书内容
    private suspend fun preloadWordBook(book: VocabularyBook) {
        try {
            // 检查数据库中是否已存在该词书
            val isInDatabase = repository.isBookLoadedInDatabase(book.filePath)
            
            if (isInDatabase) {
                Log.d(TAG, "【预加载】词书已在数据库中：${book.name}")
                val loadedWords = repository.getAllWords()
                _wordList = ArrayList(loadedWords)
                _wordListFlow.value = _wordList
                return
            }
            
            Log.d(TAG, "【预加载】开始加载词书：${book.name}")
            
            // 根据文件类型加载单词
            val loadedWordList = withContext(Dispatchers.IO) {
                when (book.type) {
                    BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                    BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                }
            }
            
            _wordList = ArrayList(loadedWordList)
            _wordListFlow.value = _wordList
            Log.d(TAG, "【预加载】完成，加载了 ${loadedWordList.size} 个单词")
        } catch (e: Exception) {
            Log.e(TAG, "【预加载】失败", e)
            throw e
        }
    }

    // 加载保存的进度
    private fun loadProgress() {
        try {
            // 从 SharedPreferences 获取词书信息
            val savedBookName = prefs.getString("current_book", null)
            val savedBookPath = prefs.getString("current_book_path", null)
            
            // 如果有保存的词书信息，找到对应的词书对象
            if (!savedBookName.isNullOrEmpty() && !savedBookPath.isNullOrEmpty()) {
                val savedBook = repository.getAvailableBooks().find { 
                    it.name == savedBookName && it.filePath == savedBookPath 
                }
                
                // 只有当找到完全匹配的词书时才恢复状态
                if (savedBook != null) {
                    _currentBook.value = savedBook
                    _progress.value = prefs.getInt("progress", 0)
                    _todayLearned.value = prefs.getInt("today_learned", 0)
                    
                    // 加载保存的待复习单词数量
                    _todayReviewCount.value = prefs.getInt("today_review_count", 0)
                    
                    // 检查上次更新时间，如果不是今天则重置今日学习数
                    val lastUpdate = prefs.getLong("last_update", 0)
                    if (!android.text.format.DateUtils.isToday(lastUpdate)) {
                        _todayLearned.value = 0
                        Log.d(TAG, "【进度】非同一天，重置今日学习数量")
                        
                        // 不重置待复习数量，因为复习是跨天的
                        // 但需要刷新一下数据库中实际的复习数量
                        viewModelScope.launch {
                            try {
                                val count = updateReviewCount()
                                Log.d(TAG, "【进度】刷新待复习数量: $count")
                            } catch (e: Exception) {
                                Log.e(TAG, "【进度】刷新待复习数量失败", e)
                            }
                        }
                    }
                    
                    // 恢复未掌握单词列表
                    val unknownWordsString = prefs.getString("unknown_words", "")
                    if (!unknownWordsString.isNullOrEmpty()) {
                        unknownWords.clear()
                        unknownWords.addAll(unknownWordsString.split(",").mapNotNull { it.toIntOrNull() })
                    }
                    
                    Log.d(TAG, "【进度】已恢复：${savedBook.name}, 进度=${_progress.value}, 今日学习=${_todayLearned.value}, 待复习=${_todayReviewCount.value}")
                } else {
                    Log.d(TAG, "【进度】未找到匹配的词书，清理保存的进度")
                    clearSavedProgress()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "【进度】加载失败", e)
            clearSavedProgress()
        }
    }
    
    // 清理保存的进度
    private fun clearSavedProgress() {
        prefs.edit().clear().apply()
        _progress.value = 0
        _todayLearned.value = 0
        unknownWords.clear()
        _currentBook.value = null
        Log.d(TAG, "已清理所有保存的进度")
    }

    // 保存进度
    private fun saveProgress() {
        try {
            prefs.edit().apply {
                // 保存词书名称和路径
                putString("current_book", _currentBook.value?.name)
                putString("current_book_path", _currentBook.value?.filePath)
                putInt("progress", _progress.value)
                putInt("today_learned", _todayLearned.value)
                putString("unknown_words", unknownWords.joinToString(","))
                putLong("last_update", System.currentTimeMillis())
                // 保存今日需要复习的单词数
                putInt("today_review_count", _todayReviewCount.value)
                apply()
            }
            
            // 同步更新DataStore中的进度
            viewModelScope.launch {
                _currentBook.value?.let { book ->
                    if (_wordList.isNotEmpty()) {
                        val progressFloat = _progress.value.toFloat() / _wordList.size.toFloat()
                        dataStore.edit { 
                            it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = progressFloat
                        }
                        Log.d(TAG, "【进度】已更新DataStore中的词书总体进度: $progressFloat (${_progress.value}/${_wordList.size})")
                    }
                }
            }
            
            Log.d(TAG, "【进度】已保存：进度=${_progress.value}, 今日学习=${_todayLearned.value}, 待复习=${_todayReviewCount.value}")
        } catch (e: Exception) {
            Log.e(TAG, "【进度】保存失败", e)
        }
    }

    // 检查今天是否是学习日
    private suspend fun isLearningDay(): Boolean {
        if (ignoreLearningDayCheck) return true
        val today = LocalDate.now().dayOfWeek
        val plan = learningPlanRepository.learningPlan.firstOrNull()
        return plan?.dailyGoal?.activeDays?.contains(today) ?: true
    }

    // 检查是否达到每日学习目标
    private suspend fun hasReachedDailyGoal(): Boolean {
        val plan = learningPlanRepository.learningPlan.firstOrNull() ?: return false
        return _todayLearned.value >= plan.dailyGoal.newWordsCount
    }

    // 获取可用词汇书列表
    fun getAvailableBooks(): List<VocabularyBook> {
        Log.d(TAG, "正在获取可用词汇书")
        return repository.getAvailableBooks()
    }

    // 加载词汇书 - 直接加载不使用缓存
    fun loadVocabularyBook(book: VocabularyBook) {
        Log.d(TAG, "【加载】请求加载词汇书：${book.name}")
        
        if (!_isInitialized.value) {
            Log.d(TAG, "【加载】ViewModel未初始化，保存待加载词书")
            pendingBook = book
            return
        }
        
        viewModelScope.launch {
            try {
                _learningState.value = LearningState.Loading
                
                // 检查是否是同一本词书且有保存的进度
                val isSameBook = _currentBook.value?.filePath == book.filePath
                val savedProgress = _progress.value
                
                // 如果不是同一本词书或没有进度，则清理状态
                if (!isSameBook) {
                    Log.d(TAG, "【加载】切换词书或首次加载，清理状态")
                    repository.clearLoadedBooksCache()
                    repository.clearTemporaryData()
                    
                    _progress.value = 0
                    // 不重置今日学习数量，因为这是每日的统计
                    // _todayLearned.value = 0 
                    unknownWords.clear()
                    _wordList.clear()
                    _wordListFlow.value = _wordList
                    _currentWord.value = null
                    _totalWords.value = 0
                } else {
                    Log.d(TAG, "【加载】续读相同词书，保留进度 $savedProgress")
                }
                
                _currentBook.value = book
                
                // 等待数据库清理完成
                if (!isSameBook) {
                    delay(100)
                }
                
                // 直接加载词书，不使用缓存
                if (_wordList.isEmpty()) {
                    val loadedWords = withContext(Dispatchers.IO) {
                        when (book.type) {
                            BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                            BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                        }
                    }
                    _wordList = ArrayList(loadedWords)
                    _wordListFlow.value = _wordList
                }
                
                // 计算实际可学习的单词数
                val remainingToLearn = _dailyGoal.value - _todayLearned.value
                
                // 修改：如果今日学习目标已完成，不再加载额外单词
                val effectiveLimit = if (remainingToLearn <= 0) {
                    Log.d(TAG, "【加载】今日学习目标已完成(${_todayLearned.value}/${_dailyGoal.value})，不再加载新单词")
                    0
                } else {
                    // 否则加载剩余学习量
                    minOf(_wordList.size, remainingToLearn)
                }
                
                _totalWords.value = effectiveLimit
                
                // 检查是否有单词可学习
                if (_wordList.isEmpty()) {
                    Log.e(TAG, "【加载】单词列表为空")
                    _learningState.value = LearningState.Error("词汇书为空，请选择其他词汇书")
                    return@launch
                }
                
                Log.d(TAG, "【加载】已设置总单词数：${_totalWords.value}，实际词库大小：${_wordList.size}")
                
                // 加载第一个单词
                loadNextWord()
                
                // 更新今日待复习单词数
                val reviewCount = updateReviewCount()
                Log.d(TAG, "【加载】今日待复习单词数：$reviewCount")
                
                _learningState.value = LearningState.Success
                
                // 保存进度
                saveProgress()
                
            } catch (e: Exception) {
                Log.e(TAG, "【加载】加载词汇书失败", e)
                _learningState.value = LearningState.Error("加载词汇书失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    // 加载下一个单词
    private fun loadNextWord() {
        viewModelScope.launch {
            try {
                _learningState.value = LearningState.Loading
                val progressValue = _progress.value
                Log.d(TAG, "正在加载下一个单词，当前进度：$progressValue")
                
                Log.d(TAG, "【诊断】加载单词检查 - 当前进度: $progressValue, 总单词数: ${_totalWords.value}, 词库大小: ${_wordList.size}")
                
                // 修改条件：只要进度值在有效词库范围内就加载单词，无论总单词数如何
                if (progressValue < _wordList.size) {
                    val word = _wordList[progressValue]
                    Log.d(TAG, "正在加载单词：${word.word}, ID=${word.id}, isFavorite=${word.isFavorite}, errorCount=${word.errorCount}")
                    
                    _currentWord.value = WordCard(
                        id = word.id,  // 复制ID
                        word = word.word,
                        ukPhonetic = word.ukPhonetic.ifEmpty { "/" + word.word + "/" },
                        usPhonetic = word.usPhonetic.ifEmpty { "/" + word.word + "/" },
                        example = word.example.ifEmpty { "${word.word} - ${word.meaning}" },
                        definition = word.meaning,
                        isFavorite = word.isFavorite,  // 复制收藏状态
                        errorCount = word.errorCount   // 复制错误次数
                    )
                    
                    Log.d(TAG, "【诊断】成功加载单词: ${word.word}, ID=${word.id}, isFavorite=${word.isFavorite}")
                } else {
                    Log.d(TAG, "【诊断】没有更多单词需要加载: 进度=$progressValue, 总单词数=${_totalWords.value}, 词库大小=${_wordList.size}")
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

    // 更新进度时同步更新 DataStore
    private suspend fun updateProgress(progress: Int, total: Int) {
        if (total > 0) {
            val progressFloat = progress.toFloat() / total.toFloat()
            dataStore.edit { 
                it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = progressFloat
            }
            _progress.value = progress
        }
    }

    // 处理用户的选择
    fun handleWordChoice(isKnown: Boolean) {
        viewModelScope.launch {
            try {
                _learningState.value = LearningState.Loading
                
                // 更新今日学习数量
                _todayLearned.value = _todayLearned.value + 1
                
                // 记录学习单词成就
                achievementViewModel.recordWordLearned(1)
                
                // 检查是否在有效范围内
                val currentProgress = _progress.value
                if (currentProgress < _totalWords.value) {
                    // 保存当前进度
                    Log.d(TAG, "处理用户选择 - 当前进度: $currentProgress, 总单词数: ${_totalWords.value}")
                    
                    // 获取当前单词
                    val word = _wordList[currentProgress]
                    
                    if (!isKnown) {
                        unknownWords.add(currentProgress)
                        if (word.id > 0) {
                            try {
                                repository.incrementErrorCount(word.id)
                                // 只有在不认识时才添加到今日复习队列
                                addToTodayReview(word.id)
                                Log.d(TAG, "单词已添加到今日复习队列: ${word.word}")
                            } catch (e: Exception) {
                                Log.e(TAG, "记录错误失败: ${e.message}", e)
                            }
                        }
                    }
                    
                    // 计算下一次复习日期
                    val today = LocalDate.now()
                    val nextReviewInterval = if (isKnown) 1L else 1L
                    val nextReviewDate = today.plusDays(nextReviewInterval)
                    
                    try {
                        if (word.id > 0) {
                            repository.updateWordStatus(
                                wordId = word.id,
                                status = WordStatus.NEEDS_REVIEW,
                                lastReviewDate = today,
                                nextReviewDate = nextReviewDate,
                                reviewCount = if (isKnown) word.reviewCount + 1 else 0
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "更新单词状态失败: ${e.message}", e)
                    }
                    
                    // 使用新的进度更新方法
                    val newProgress = currentProgress + 1
                    updateProgress(newProgress, _totalWords.value)
                    
                    // 保存进度
                    saveProgress()
                    
                    // 加载下一个单词
                    if (newProgress < _totalWords.value) {
                        loadNextWord()
                    } else {
                        _currentWord.value = null
                    }
                }
                _learningState.value = LearningState.Success
                
                // 立即更新待复习单词数量
                try {
                    val reviewCount = updateReviewCount()
                    Log.d(TAG, "处理单词后更新待复习数量: $reviewCount")
                } catch (e: Exception) {
                    Log.e(TAG, "更新待复习数量失败", e)
                }
                
                // 刷新首页数据
                try {
                    repository.updateLearningCounts()
                    (getApplication<Application>() as? App)?.homeViewModel?.forceRefreshNow()
                } catch (e: Exception) {
                    Log.e(TAG, "刷新首页数据失败", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理用户选择时出错", e)
                _learningState.value = LearningState.Error("处理选择失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    // 获取学习统计
    suspend fun getLearningStats(): Triple<Int, Int, Int> {
        return try {
            val total = _totalWords.value
            
            // 获取今日实际学习的新单词数量
            val learned = _todayLearned.value
            
            // 用本地记录的不认识的单词数量
            val unknownLocal = unknownWords.size
            
            // 同步获取今日复习单词数量
            val reviewCount = withContext(Dispatchers.IO) {
                repository.getTodayReviewWordsCount()
            }
            
            // 计算总未掌握单词数（本地记录的 + 数据库中的复习单词）
            val totalUnknown = unknownLocal + reviewCount
            
            Log.d(TAG, "学习统计 - 目标：$total，今日已学习：$learned，" +
                    "未掌握（本地）：$unknownLocal，数据库复习：$reviewCount，总计：$totalUnknown")
            
            Triple(total, learned, totalUnknown) // 返回总未掌握单词数
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
                _todayLearned.value = 0  // 重置今日学习数量，让用户可以从头开始学习
                
                // 重新加载当前词汇书
                _currentBook.value?.let { book ->
                    // 直接从文件加载单词
                    val loadedWords = when (book.type) {
                        BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                        BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                    }
                    _wordList = ArrayList(loadedWords)
                    _wordListFlow.value = _wordList
                    
                    // 计算剩余需要学习的单词数（已重置todayLearned，所以这里应该总是等于dailyGoal）
                    val remainingToLearn = _dailyGoal.value - _todayLearned.value
                    
                    // 限制单词数量不超过词库大小和每日目标
                    _totalWords.value = minOf(_wordList.size, remainingToLearn)
                    
                    Log.d(TAG, "已加载${_totalWords.value}个单词，今日目标：${_dailyGoal.value}，已学习：${_todayLearned.value}")
                    
                    if (_wordList.isEmpty()) {
                        Log.e(TAG, "单词列表为空")
                        _learningState.value = LearningState.Error("词汇书为空，请选择其他词汇书")
                        return@launch
                    }
                    
                    // 加载第一个单词
                    loadNextWord()
                    _learningState.value = LearningState.Success
                    Log.d(TAG, "状态已更改为成功")
                    
                    // 保存重置后的状态
                    saveProgress()
                    
                    // 更新复习数量
                    viewModelScope.launch {
                        try {
                            val count = updateReviewCount()
                            Log.d(TAG, "【重置】更新复习数量: $count")
                        } catch (e: Exception) {
                            Log.e(TAG, "【重置】更新复习数量失败", e)
                        }
                    }
                    
                    Log.d(TAG, "进度已重置")
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
                
                // 获取当前词书总进度，以便稍后恢复
                val currentBookProgress = _progress.value
                val currentBook = _currentBook.value
                
                // 重置今日学习状态
                _todayLearned.value = 0  // 重置今日学习数量
                unknownWords.clear()
                ignoreLearningDayCheck = true // 忽略学习日检查
                
                // 重新加载当前词汇书
                _currentBook.value?.let { book ->
                    // 直接从文件加载单词
                    val loadedWords = when (book.type) {
                        BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                        BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                    }
                    _wordList = ArrayList(loadedWords)
                    _wordListFlow.value = _wordList
                    
                    // 计算剩余需要学习的单词数（已重置todayLearned，所以这里应该总是等于dailyGoal）
                    val remainingToLearn = _dailyGoal.value - _todayLearned.value
                    
                    // 限制单词数量不超过词库大小和每日目标
                    _totalWords.value = minOf(_wordList.size, remainingToLearn)
                    
                    Log.d(TAG, "已重新加载${_totalWords.value}个单词，今日目标：${_dailyGoal.value}")
                    
                    if (_wordList.isEmpty()) {
                        Log.e(TAG, "单词列表为空")
                        _learningState.value = LearningState.Error("词汇书为空，请选择其他词汇书")
                        return@launch
                    }
                    
                    // 恢复词书总体进度
                    if (currentBook == book && currentBookProgress > 0) {
                        // 如果是同一本书，保留原有词书总进度
                        _progress.value = currentBookProgress
                        Log.d(TAG, "保留词书总体进度: ${_progress.value}")
                    } else {
                        // 如果是不同书或没有进度，从头开始
                        _progress.value = 0
                    }
                    
                    // 加载第一个单词
                    loadNextWord()
                    _learningState.value = LearningState.Success
                    
                    // 保存重置后的状态
                    saveProgress()
                    
                    // 更新复习数量
                    viewModelScope.launch {
                        try {
                            val count = updateReviewCount()
                            Log.d(TAG, "【重置】更新复习数量: $count")
                        } catch (e: Exception) {
                            Log.e(TAG, "【重置】更新复习数量失败", e)
                        }
                    }
                    
                    Log.d(TAG, "进度已重置")
                } ?: run {
                    _learningState.value = LearningState.Error("请先选择词汇书")
                }
            } catch (e: Exception) {
                Log.e(TAG, "重置进度时出错", e)
                _learningState.value = LearningState.Error("重置进度失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 清理所有状态
        Log.d(TAG, "ViewModel已销毁，清理状态")
    }
    
    // 切换单词收藏状态
    fun toggleFavorite(wordId: Long, isFavorite: Boolean) {
        Log.d(TAG, "切换单词收藏状态: ID=$wordId, isFavorite=$isFavorite")
        viewModelScope.launch {
            try {
                // 更新数据库收藏状态
                repository.toggleFavorite(wordId, isFavorite)
                
                // 更新当前单词
                _currentWord.value?.let { wordCard ->
                    if (wordCard.id == wordId) {
                        _currentWord.value = wordCard.copy(isFavorite = isFavorite)
                        Log.d(TAG, "已更新当前单词收藏状态: ${_currentWord.value?.word}, isFavorite=$isFavorite")
                    }
                }
                
                // 更新词汇列表中的单词
                val updatedList = ArrayList<Word>()
                for (word in _wordList) {
                    if (word.id == wordId) {
                        updatedList.add(word.copy(isFavorite = isFavorite))
                    } else {
                        updatedList.add(word)
                    }
                }
                _wordList = updatedList
                _wordListFlow.value = _wordList
            } catch (e: Exception) {
                Log.e(TAG, "更新收藏状态失败", e)
            }
        }
    }

    // 当用户答错时记录错误
    fun recordError(wordId: Long) {
        Log.d(TAG, "记录错误: ID=$wordId")
        viewModelScope.launch {
            try {
                // 查找当前单词以获取错误次数
                val currentWord = _wordList.find { it.id == wordId }
                if (currentWord == null) {
                    Log.w(TAG, "未在当前列表中找到要记录错误的单词: ID=$wordId")
                }
                val newErrorCount = (currentWord?.errorCount ?: 0) + 1
                
                // 增加错误次数
                repository.incrementErrorCount(wordId)
                Log.d(TAG, "已记录单词错误: ID=$wordId, 新错误次数=$newErrorCount")
                
                // 更新当前单词
                _currentWord.value?.let { wordCard ->
                    if (wordCard.id == wordId) {
                        _currentWord.value = wordCard.copy(errorCount = newErrorCount)
                        Log.d(TAG, "已更新当前单词错误次数: ${_currentWord.value?.word}, errorCount=$newErrorCount")
                    }
                }
                
                // 更新词汇列表中的单词
                val updatedList = ArrayList<Word>()
                for (word in _wordList) {
                    if (word.id == wordId) {
                        updatedList.add(word.copy(errorCount = newErrorCount))
                    } else {
                        updatedList.add(word)
                    }
                }
                _wordList = updatedList
                _wordListFlow.value = _wordList
            } catch (e: Exception) {
                Log.e(TAG, "记录错误失败", e)
            }
        }
    }
    
    // 添加单词到今日复习队列
    fun addToTodayReview(wordId: Long) {
        Log.d(TAG, "添加单词到今日复习队列: ID=$wordId")
        viewModelScope.launch {
            try {
                // 获取当前日期
                val today = LocalDate.now()
                // 设置下次复习日期为今天（确保今天就会复习）
                val nextReviewDate = today
                
                // 查询单词的当前状态
                val word = repository.getWordById(wordId)
                if (word != null) {
                    // 打印详细日志
                    Log.d(TAG, "将单词添加到今日复习: ID=${word.id}, 单词=${word.word}, 当前状态=${word.status}")
                    Log.d(TAG, "当前日期: $today, 设置复习日期: $nextReviewDate")

                    // 更新单词状态为需要复习，并设置复习日期为今天
                    repository.updateWordStatus(
                        wordId = wordId,
                        status = WordStatus.NEEDS_REVIEW,
                        lastReviewDate = today,
                        nextReviewDate = nextReviewDate,
                        reviewCount = word.reviewCount.coerceAtLeast(0) // 保留原有复习次数
                    )
                    Log.d(TAG, "已成功添加单词到今日复习队列: ID=$wordId, 单词=${word.word}")
                    
                    // 手动验证更新
                    try {
                        val updatedWord = repository.getWordById(wordId)
                        updatedWord?.let {
                            Log.d(TAG, "验证更新后的单词状态: ID=${it.id}, 状态=${it.status}, 复习日期=${it.nextReviewDate}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "验证单词状态失败", e)
                    }
                } else {
                    Log.w(TAG, "未找到要添加到复习队列的单词: ID=$wordId")
                }
                
                // 手动延迟一小段时间后再刷新，确保数据库更新完成
                delay(100)
                
                // 更新今日待复习数量
                val reviewCount = updateReviewCount()
                
                // 刷新首页数据
                try {
                    // 手动获取今日复习数量进行验证
                    Log.d(TAG, "更新后的今日复习数量: $reviewCount")
                    
                    repository.updateLearningCounts()
                    (getApplication<Application>() as? App)?.homeViewModel?.forceRefreshNow()
                    Log.d(TAG, "已刷新首页数据")
                } catch (e: Exception) {
                    Log.e(TAG, "刷新首页数据失败", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "添加单词到今日复习队列失败", e)
            }
        }
    }

    // 更新今日待复习数量
    suspend fun updateReviewCount(): Int {
        return try {
            val count = withContext(Dispatchers.IO) {
                repository.getTodayReviewWordsCount()
            }
            _todayReviewCount.value = count
            Log.d(TAG, "更新今日待复习数量: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "更新今日待复习数量失败", e)
            _todayReviewCount.value // 返回当前值
        }
    }

    // 获取随机单词，用于游戏功能
    fun getRandomWords(count: Int): StateFlow<List<Word>> {
        val randomWords = MutableStateFlow<List<Word>>(emptyList())
        
        viewModelScope.launch {
            try {
                // 尝试从当前加载的词汇书获取
                val currentWords = _wordListFlow.value
                if (currentWords.isNotEmpty()) {
                    // 如果有足够的单词，就随机选择
                    if (currentWords.size >= count) {
                        randomWords.value = currentWords.shuffled().take(count)
                    } else {
                        // 如果单词不够，就全部返回
                        randomWords.value = currentWords.shuffled()
                    }
                } else {
                    // 如果当前没有加载词汇书，从存储库中获取所有单词
                    val allAvailableWords = repository.getAllWords()
                    if (allAvailableWords.isNotEmpty()) {
                        randomWords.value = allAvailableWords.shuffled().take(
                            minOf(count, allAvailableWords.size)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取随机单词失败: ${e.message}")
                // 出错时返回空列表
                randomWords.value = emptyList()
            }
        }
        
        return randomWords
    }

    // 获取所有已加载的单词，用于游戏功能
    fun getAllWords(): StateFlow<List<Word>> {
        val allWords = MutableStateFlow<List<Word>>(_wordListFlow.value)
        
        // 如果当前没有单词，尝试从存储库获取
        if (_wordListFlow.value.isEmpty()) {
            viewModelScope.launch {
                try {
                    val storedWords = repository.getAllWords()
                    allWords.value = storedWords
                } catch (e: Exception) {
                    Log.e(TAG, "获取所有单词失败: ${e.message}")
                }
            }
        }
        
        return allWords
    }
} 