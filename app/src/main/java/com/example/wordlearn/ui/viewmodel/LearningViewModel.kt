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

private const val TAG = "LearningViewModel"

// 词书缓存，保存已加载的词书内容
private object WordBookCache {
    // 缓存映射表，每个词书路径对应一个单词列表
    private val cache = mutableMapOf<String, List<Word>>()
    
    // 首屏单词缓存 - 每个词书只缓存前100个单词用于快速显示
    private val quickCache = mutableMapOf<String, List<Word>>()
    
    // 最后加载时间记录
    private val lastLoadTime = mutableMapOf<String, Long>()
    
    // 缓存锁，防止并发加载
    private val cacheLock = Any()
    
    // 正在加载的词书路径
    private val loadingBooks = mutableSetOf<String>()
    
    // 从缓存获取单词列表
    fun getWordList(filePath: String): List<Word>? {
        synchronized(cacheLock) {
            return cache[filePath]?.also {
                lastLoadTime[filePath] = System.currentTimeMillis()
                Log.d(TAG, "【缓存】命中完整缓存：$filePath")
            }
        }
    }
    
    // 获取快速缓存的单词（仅前N个单词）
    fun getQuickWordList(filePath: String, count: Int = 50): List<Word>? {
        synchronized(cacheLock) {
            // 尝试获取快速缓存
            quickCache[filePath]?.let { 
                lastLoadTime[filePath] = System.currentTimeMillis()
                Log.d(TAG, "【缓存】命中快速缓存：$filePath")
                return it 
            }
            
            // 如果没有快速缓存但有完整缓存，创建快速缓存
            return cache[filePath]?.take(count)?.also {
                quickCache[filePath] = it
                lastLoadTime[filePath] = System.currentTimeMillis()
                Log.d(TAG, "【缓存】从完整缓存创建快速缓存：$filePath")
            }
        }
    }
    
    // 保存单词列表到缓存
    fun cacheWordList(filePath: String, wordList: List<Word>) {
        synchronized(cacheLock) {
            if (loadingBooks.contains(filePath)) {
                Log.d(TAG, "【缓存】词书正在加载中，跳过缓存：$filePath")
                return
            }
            
            try {
                loadingBooks.add(filePath)
                cache[filePath] = wordList
                quickCache[filePath] = wordList.take(50)
                lastLoadTime[filePath] = System.currentTimeMillis()
                Log.d(TAG, "【缓存】已缓存词书：$filePath")
            } finally {
                loadingBooks.remove(filePath)
            }
        }
    }
    
    // 快速缓存单词列表
    fun quickCacheWordList(filePath: String, wordList: List<Word>, count: Int = 50) {
        synchronized(cacheLock) {
            if (loadingBooks.contains(filePath)) {
                Log.d(TAG, "【缓存】词书正在加载中，跳过快速缓存：$filePath")
                return
            }
            
            quickCache[filePath] = wordList.take(count)
            lastLoadTime[filePath] = System.currentTimeMillis()
            Log.d(TAG, "【缓存】已快速缓存词书：$filePath")
        }
    }
    
    // 清除缓存
    fun clearCache() {
        synchronized(cacheLock) {
            cache.clear()
            quickCache.clear()
            lastLoadTime.clear()
            loadingBooks.clear()
            Log.d(TAG, "【缓存】已清除所有缓存")
        }
    }
    
    // 清除30分钟未使用的缓存
    fun cleanupOldCache() {
        synchronized(cacheLock) {
            val now = System.currentTimeMillis()
            val expireTime = 30 * 60 * 1000 // 30分钟
            
            val expiredFiles = lastLoadTime.filter { (_, time) -> 
                now - time > expireTime 
            }.keys.toList()
            
            expiredFiles.forEach { file ->
                cache.remove(file)
                quickCache.remove(file)
                lastLoadTime.remove(file)
                Log.d(TAG, "【缓存】清理过期缓存：$file")
            }
        }
    }
    
    // 检查缓存是否存在
    fun hasCached(filePath: String): Boolean {
        synchronized(cacheLock) {
            return cache.containsKey(filePath)
        }
    }
    
    // 检查快速缓存是否存在
    fun hasQuickCached(filePath: String): Boolean {
        synchronized(cacheLock) {
            return quickCache.containsKey(filePath)
        }
    }
    
    // 检查词书是否正在加载
    fun isLoading(filePath: String): Boolean {
        synchronized(cacheLock) {
            return loadingBooks.contains(filePath)
        }
    }
}

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

    // 从 DataStore 读取持久化的进度
    private val _persistedProgress = dataStore.data
        .map { it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

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

    // 初始化状态
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // 待加载的词书
    private var pendingBook: VocabularyBook? = null

    init {
        // 加载学习计划
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
                // 设置默认每日目标
                if (_dailyGoal.value <= 0) {
                    _dailyGoal.value = 20
                    Log.d(TAG, "【初始化】设置默认每日目标: 20")
                }
                
                // 初始化SharedPreferences
                prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
                
                // 从持久化存储加载进度
                loadProgress()
                
                // 预加载当前词书
                _currentBook.value?.let { book ->
                    withContext(Dispatchers.IO) {
                        preloadWordBook(book)
                    }
                }
                
                // 初始化完成
                _isInitialized.value = true
                Log.d(TAG, "【初始化】完成 - 每日目标: ${_dailyGoal.value}, 当前进度: ${_progress.value}, 总单词数: ${_totalWords.value}")
                
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
    
    // 预加载词书内容到缓存
    private suspend fun preloadWordBook(book: VocabularyBook) {
        try {
            // 检查词书是否正在加载
            if (WordBookCache.isLoading(book.filePath)) {
                Log.d(TAG, "【预加载】词书正在加载中，跳过：${book.name}")
                return
            }
            
            // 检查数据库中是否已存在该词书
            val isInDatabase = repository.isBookLoadedInDatabase(book.filePath)
            
            // 如果缓存中已有该词书且数据库中也存在，直接使用缓存
            if (WordBookCache.hasCached(book.filePath) && isInDatabase) {
                Log.d(TAG, "【预加载】词书已在缓存和数据库中：${book.name}")
                wordList = WordBookCache.getWordList(book.filePath) ?: emptyList()
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
            
            // 保存到缓存
            WordBookCache.cacheWordList(book.filePath, loadedWordList)
            
            Log.d(TAG, "【预加载】完成，缓存了 ${loadedWordList.size} 个单词")
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
                    
                    // 检查上次更新时间，如果不是今天则重置今日学习数
                    val lastUpdate = prefs.getLong("last_update", 0)
                    if (!android.text.format.DateUtils.isToday(lastUpdate)) {
                        _todayLearned.value = 0
                        Log.d(TAG, "【进度】非同一天，重置今日学习数量")
                    }
                    
                    // 恢复未掌握单词列表
                    val unknownWordsString = prefs.getString("unknown_words", "")
                    if (!unknownWordsString.isNullOrEmpty()) {
                        unknownWords.clear()
                        unknownWords.addAll(unknownWordsString.split(",").mapNotNull { it.toIntOrNull() })
                    }
                    
                    Log.d(TAG, "【进度】已恢复：${savedBook.name}, 进度=${_progress.value}, 今日学习=${_todayLearned.value}")
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
                apply()
            }
            
            // 同步更新DataStore中的进度
            viewModelScope.launch {
                _currentBook.value?.let { book ->
                    if (_totalWords.value > 0) {
                        val progressFloat = _progress.value.toFloat() / _totalWords.value.toFloat()
                        dataStore.edit { 
                            it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = progressFloat
                        }
                    }
                }
            }
            
            Log.d(TAG, "【进度】已保存：进度=${_progress.value}, 今日学习=${_todayLearned.value}")
        } catch (e: Exception) {
            Log.e(TAG, "【进度】保存失败", e)
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

    // 加载词汇书 - 优化版本
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
                
                // 清理所有状态和缓存
                withContext(Dispatchers.IO) {
                    WordBookCache.clearCache()
                    repository.clearLoadedBooksCache()
                    repository.clearTemporaryData()
                }
                
                _progress.value = 0
                _todayLearned.value = 0
                unknownWords.clear()
                wordList = emptyList()
                _currentWord.value = null
                _totalWords.value = 0
                
                _currentBook.value = book
                
                // 等待数据库清理完成
                delay(100)
                
                // 两阶段加载策略
                var quickLoaded = false
                
                // 尝试从快速缓存加载
                if (WordBookCache.hasQuickCached(book.filePath)) {
                    Log.d(TAG, "【加载】使用快速缓存")
                    val cachedWords = WordBookCache.getQuickWordList(book.filePath)
                    if (!cachedWords.isNullOrEmpty()) {
                        wordList = cachedWords
                        quickLoaded = true
                        Log.d(TAG, "【加载】快速缓存加载成功，包含 ${wordList.size} 个单词")
                    }
                }
                
                if (!quickLoaded) {
                    withContext(Dispatchers.IO) {
                        if (WordBookCache.hasCached(book.filePath)) {
                            Log.d(TAG, "【加载】使用完整缓存")
                            wordList = WordBookCache.getWordList(book.filePath) ?: emptyList()
                        } else {
                            Log.d(TAG, "【加载】从文件加载")
                            wordList = when (book.type) {
                                BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                                BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                            }
                            WordBookCache.cacheWordList(book.filePath, wordList)
                        }
                    }
                }
                
                // 计算实际可学习的单词数
                val remainingToLearn = _dailyGoal.value - _todayLearned.value
                _totalWords.value = if (remainingToLearn <= 0) {
                    Log.d(TAG, "【加载】今日学习目标已完成，不再加载新单词")
                    0
                } else {
                    minOf(wordList.size, remainingToLearn)
                }
                
                Log.d(TAG, "【加载】词库实际单词数量: ${wordList.size}, 今日可学习: ${_totalWords.value}")
                
                if (wordList.isEmpty()) {
                    _learningState.value = LearningState.Error("词汇书为空，请选择其他词汇书")
                    return@launch
                }
                
                // 加载第一个单词
                loadNextWord()
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
                Log.d(TAG, "正在加载下一个单词，当前进度：${_progress.value}")
                
                Log.d(TAG, "【诊断】加载单词检查 - 当前进度: ${_progress.value}, 总单词数: ${_totalWords.value}, 词库大小: ${wordList.size}")
                
                if (_progress.value < _totalWords.value && _progress.value < wordList.size) {
                    val word = wordList[_progress.value]
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
                    Log.d(TAG, "【诊断】没有更多单词需要加载: 进度=${_progress.value}, 总单词数=${_totalWords.value}, 词库大小=${wordList.size}")
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
                
                // 检查是否在有效范围内
                if (_progress.value < _totalWords.value) {
                    // 保存当前进度
                    val currentProgress = _progress.value
                    
                    if (!isKnown) {
                        unknownWords.add(currentProgress)
                        val word = wordList[currentProgress]
                        if (word.id > 0) {
                            try {
                                repository.incrementErrorCount(word.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "记录错误失败: ${e.message}", e)
                            }
                        }
                    }
                    
                    // 使用新的进度更新方法
                    updateProgress(currentProgress + 1, _totalWords.value)
                    
                    // 获取当前单词并更新其状态
                    val word = wordList[currentProgress]
                    
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
                                reviewCount = 0
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "更新单词状态失败: ${e.message}", e)
                    }
                    
                    // 保存进度
                    saveProgress()
                    
                    // 加载下一个单词
                    if (currentProgress + 1 < _totalWords.value) {
                        loadNextWord()
                    } else {
                        _currentWord.value = null
                    }
                }
                _learningState.value = LearningState.Success
                
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
    fun getLearningStats(): Triple<Int, Int, Int> {
        return try {
            val total = _totalWords.value
            
            // 获取今日实际学习的新单词数量
            val learned = _todayLearned.value
            
            // 获取需要复习的单词数量
            viewModelScope.launch {
                try {
                    val reviewCount = repository.getTodayReviewWordsCount()
                    // 这里我们仅在日志中记录，因为这个方法不能是挂起函数
                    Log.d(TAG, "今天需要复习的单词数量: $reviewCount")
                } catch (e: Exception) {
                    Log.e(TAG, "获取待复习单词数量失败", e)
                }
            }
            
            // 用本地记录的不认识的单词数量
            val unknown = unknownWords.size
            
            Log.d(TAG, "学习统计 - 目标：$total，今日已学习：$learned，未掌握：$unknown")
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
                _todayLearned.value = 0  // 重置今日学习数量，让用户可以从头开始学习
                
                // 重新加载当前词汇书
                _currentBook.value?.let { book ->
                    // 从缓存加载单词或从文件加载
                    if (WordBookCache.hasCached(book.filePath)) {
                        Log.d(TAG, "【缓存】使用缓存中的词书内容：${book.filePath}")
                        wordList = WordBookCache.getWordList(book.filePath) ?: emptyList()
                    } else {
                        // 根据文件类型加载单词
                        Log.d(TAG, "【缓存】缓存未命中，从${book.type}文件加载单词：${book.filePath}")
                        wordList = when (book.type) {
                            BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                            BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                        }
                        // 保存到缓存
                        WordBookCache.cacheWordList(book.filePath, wordList)
                    }
                    
                    // 计算剩余需要学习的单词数
                    val remainingToLearn = _dailyGoal.value - _todayLearned.value
                    
                    // 修复：恢复每日目标限制，但确保至少有1个单词可以学习
                    val effectiveLimit = if (remainingToLearn <= 0) 1 else remainingToLearn
                    _totalWords.value = minOf(wordList.size, effectiveLimit)
                    
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
                    
                    // 保存重置后的状态
                    saveProgress()
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
                ignoreLearningDayCheck = true // 忽略学习日检查
                
                // 重新加载当前词汇书
                _currentBook.value?.let { book ->
                    // 从缓存加载单词或从文件加载
                    if (WordBookCache.hasCached(book.filePath)) {
                        Log.d(TAG, "【缓存】使用缓存中的词书内容：${book.filePath}")
                        wordList = WordBookCache.getWordList(book.filePath) ?: emptyList()
                    } else {
                        // 根据文件类型加载单词
                        Log.d(TAG, "【缓存】缓存未命中，从${book.type}文件加载单词：${book.filePath}")
                        wordList = when (book.type) {
                            BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                            BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                        }
                        // 保存到缓存
                        WordBookCache.cacheWordList(book.filePath, wordList)
                    }
                    
                    // 计算剩余需要学习的单词数
                    val remainingToLearn = _dailyGoal.value - _todayLearned.value
                    
                    // 修复：恢复每日目标限制，但确保至少有1个单词可以学习
                    val effectiveLimit = if (remainingToLearn <= 0) 1 else remainingToLearn
                    _totalWords.value = minOf(wordList.size, effectiveLimit)
                    
                    Log.d(TAG, "已重新加载${_totalWords.value}个单词，今日目标：${_dailyGoal.value}")
                    
                    if (wordList.isEmpty()) {
                        Log.e(TAG, "单词列表为空")
                        _learningState.value = LearningState.Error("词汇书为空，请选择其他词汇书")
                        return@launch
                    }
                    
                    // 加载第一个单词
                    loadNextWord()
                    _learningState.value = LearningState.Success
                    
                    // 保存重置后的状态
                    saveProgress()
                    
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
        // 清理所有状态，但保留缓存数据
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
                
                // 更新缓存中的单词
                _currentBook.value?.let { book ->
                    // 获取缓存的单词列表
                    val cachedList = WordBookCache.getWordList(book.filePath)
                    if (cachedList != null) {
                        // 查找并更新收藏单词
                        val updatedList = cachedList.map { word ->
                            if (word.id == wordId) {
                                word.copy(isFavorite = isFavorite)
                            } else {
                                word
                            }
                        }
                        // 更新缓存
                        WordBookCache.cacheWordList(book.filePath, updatedList)
                        Log.d(TAG, "已更新缓存中单词的收藏状态")
                        
                        // 如果是当前学习列表中的单词，也需要更新wordList
                        wordList = wordList.map { word ->
                            if (word.id == wordId) {
                                word.copy(isFavorite = isFavorite)
                            } else {
                                word
                            }
                        }
                        Log.d(TAG, "已更新当前学习列表中单词的收藏状态")
                    }
                }
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
                val currentWord = wordList.find { it.id == wordId }
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
                
                // 更新缓存中的单词
                _currentBook.value?.let { book ->
                    // 获取缓存的单词列表
                    val cachedList = WordBookCache.getWordList(book.filePath)
                    if (cachedList != null) {
                        // 查找并更新错题单词
                        val updatedList = cachedList.map { word ->
                            if (word.id == wordId) {
                                word.copy(errorCount = newErrorCount)
                            } else {
                                word
                            }
                        }
                        // 更新缓存
                        WordBookCache.cacheWordList(book.filePath, updatedList)
                        Log.d(TAG, "已更新缓存中单词的错误次数")
                        
                        // 如果是当前学习列表中的单词，也需要更新wordList
                        wordList = wordList.map { word ->
                            if (word.id == wordId) {
                                word.copy(errorCount = newErrorCount)
                            } else {
                                word
                            }
                        }
                        Log.d(TAG, "已更新当前学习列表中单词的错误次数")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "记录错误失败", e)
            }
        }
    }
} 