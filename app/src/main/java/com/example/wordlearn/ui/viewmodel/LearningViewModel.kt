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

private const val TAG = "LearningViewModel"

// 词书缓存，保存已加载的词书内容
private object WordBookCache {
    // 缓存映射表，每个词书路径对应一个单词列表
    private val cache = mutableMapOf<String, List<Word>>()
    
    // 首屏单词缓存 - 每个词书只缓存前100个单词用于快速显示
    private val quickCache = mutableMapOf<String, List<Word>>()
    
    // 最后加载时间记录
    private val lastLoadTime = mutableMapOf<String, Long>()
    
    // 从缓存获取单词列表
    fun getWordList(filePath: String): List<Word>? {
        return cache[filePath]?.also {
            // 更新最后访问时间
            lastLoadTime[filePath] = System.currentTimeMillis()
        }
    }
    
    // 获取快速缓存的单词（仅前N个单词）
    fun getQuickWordList(filePath: String, count: Int = 50): List<Word>? {
        // 尝试获取快速缓存
        quickCache[filePath]?.let { 
            lastLoadTime[filePath] = System.currentTimeMillis()
            return it 
        }
        
        // 如果没有快速缓存但有完整缓存，创建快速缓存
        return cache[filePath]?.take(count)?.also {
            quickCache[filePath] = it
            lastLoadTime[filePath] = System.currentTimeMillis()
            Log.d(TAG, "【缓存】从完整缓存创建快速缓存 $filePath，包含 ${it.size} 个单词")
        }
    }
    
    // 保存单词列表到缓存
    fun cacheWordList(filePath: String, wordList: List<Word>) {
        cache[filePath] = wordList
        // 同时保存快速缓存（只取前50个以提高性能）
        quickCache[filePath] = wordList.take(50)
        lastLoadTime[filePath] = System.currentTimeMillis()
        Log.d(TAG, "【缓存】已缓存词书 $filePath，包含 ${wordList.size} 个单词，快速缓存 ${quickCache[filePath]?.size} 个单词")
    }
    
    // 清除缓存
    fun clearCache() {
        cache.clear()
        quickCache.clear()
        lastLoadTime.clear()
        Log.d(TAG, "【缓存】已清除所有词书缓存")
    }
    
    // 清除30分钟未使用的缓存
    fun cleanupOldCache() {
        val now = System.currentTimeMillis()
        val expireTime = 30 * 60 * 1000 // 30分钟
        
        val expiredFiles = lastLoadTime.filter { (_, time) -> 
            now - time > expireTime 
        }.keys.toList()
        
        expiredFiles.forEach { file ->
            cache.remove(file)
            quickCache.remove(file)
            lastLoadTime.remove(file)
            Log.d(TAG, "【缓存】清理过期缓存: $file")
        }
    }
    
    // 检查缓存是否存在
    fun hasCached(filePath: String): Boolean {
        return cache.containsKey(filePath)
    }
    
    // 检查快速缓存是否存在
    fun hasQuickCached(filePath: String): Boolean {
        return quickCache.containsKey(filePath)
    }
}

// 学习状态
sealed class LearningState {
    object Loading : LearningState()
    object Success : LearningState()
    data class Error(val message: String) : LearningState()
}

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as App).vocabularyRepository
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
        Log.d(TAG, "【诊断】初始化ViewModel开始")
        
        // 默认每日目标
        if (_dailyGoal.value <= 0) {
            _dailyGoal.value = 20
            Log.d(TAG, "【诊断】初始化 - 设置默认每日目标: 20")
        }
        
        prefs = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
        loadProgress()
        
        // 确保总单词数至少为1，避免立即进入完成状态
        if (_totalWords.value <= 0) {
            _totalWords.value = 1
            Log.d(TAG, "【诊断】初始化 - 强制设置总单词数为1以避免空学习")
        }
        
        // 新增：如果进度大于总单词数，重置进度为0
        if (_progress.value >= _totalWords.value) {
            Log.d(TAG, "【诊断】初始化 - 检测到进度(${_progress.value})大于或等于总单词数(${_totalWords.value})，重置进度为0")
            _progress.value = 0
            // 保存重置后的进度
            saveProgress()
        }
        
        // 预加载当前词书内容
        _currentBook.value?.let { book ->
            viewModelScope.launch {
                preloadWordBook(book)
            }
        }
        
        Log.d(TAG, "【诊断】初始化完成 - 每日目标: ${_dailyGoal.value}, 当前进度: ${_progress.value}, 总单词数: ${_totalWords.value}")
    }
    
    // 预加载词书内容到缓存
    private suspend fun preloadWordBook(book: VocabularyBook) {
        try {
            // 如果缓存中已有该词书，直接使用缓存
            if (WordBookCache.hasCached(book.filePath)) {
                Log.d(TAG, "【缓存】发现词书缓存 ${book.name}，无需重新加载")
                wordList = WordBookCache.getWordList(book.filePath) ?: emptyList()
                return
            }
            
            Log.d(TAG, "【缓存】预加载词书 ${book.name} (${book.filePath})")
            
            // 根据文件类型加载单词
            val loadedWordList = when (book.type) {
                BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
            }
            
            // 保存到缓存
            WordBookCache.cacheWordList(book.filePath, loadedWordList)
            
            Log.d(TAG, "【缓存】词书 ${book.name} 预加载完成，缓存了 ${loadedWordList.size} 个单词")
        } catch (e: Exception) {
            Log.e(TAG, "【缓存】预加载词书时出错", e)
        }
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

    // 加载词汇书 - 优化版本
    fun loadVocabularyBook(book: VocabularyBook) {
        Log.d(TAG, "正在加载词汇书：${book.name}")
        viewModelScope.launch {
            try {
                _learningState.value = LearningState.Loading
                
                // 重置进度
                _progress.value = 0
                _currentBook.value = book
                
                // 性能优化策略：两阶段加载
                // 1. 首先尝试快速加载少量单词以立即显示界面
                // 2. 然后在后台加载剩余单词
                
                // 清理过期缓存以释放内存
                WordBookCache.cleanupOldCache()
                
                // 标记是否已快速加载
                var quickLoaded = false
                
                // 尝试从快速缓存加载
                if (WordBookCache.hasQuickCached(book.filePath)) {
                    Log.d(TAG, "【加速】使用快速缓存立即显示首批单词")
                    // 从快速缓存加载少量单词立即显示
                    val quickWords = WordBookCache.getQuickWordList(book.filePath) ?: emptyList()
                    if (quickWords.isNotEmpty()) {
                        wordList = quickWords
                        quickLoaded = true
                        
                        // 计算显示数量
                        val remainingToLearn = _dailyGoal.value - _todayLearned.value
                        val effectiveLimit = if (remainingToLearn <= 0) 1 else remainingToLearn
                        _totalWords.value = minOf(wordList.size, effectiveLimit)
                        
                        // 立即加载第一个单词
                        loadNextWord()
                        _learningState.value = LearningState.Success
                        Log.d(TAG, "【加速】已快速加载${wordList.size}个单词并显示第一个")
                        
                        // 异步加载全部单词
                        launch(Dispatchers.IO) {
                            Log.d(TAG, "【后台】开始在后台加载完整词书")
                            loadFullWordBook(book)
                        }
                    }
                } 
                
                // 如果没有快速缓存，直接正常加载
                if (!quickLoaded) {
                    Log.d(TAG, "【加载】没有快速缓存，进行普通加载")
                    // 先尝试从预先缓存加载，减轻主线程负担
                    if (WordBookCache.hasCached(book.filePath)) {
                        val cachedWords = WordBookCache.getWordList(book.filePath) ?: emptyList()
                        if (cachedWords.isNotEmpty()) {
                            wordList = cachedWords
                            
                            // 计算显示数量
                            val remainingToLearn = _dailyGoal.value - _todayLearned.value
                            val effectiveLimit = if (remainingToLearn <= 0) 1 else remainingToLearn
                            _totalWords.value = minOf(wordList.size, effectiveLimit)
                            
                            // 立即加载第一个单词
                            loadNextWord()
                            _learningState.value = LearningState.Success
                            Log.d(TAG, "【加载】已从缓存加载${wordList.size}个单词并显示")
                            
                            return@launch
                        }
                    }
                    
                    // 如果前面的方法都没有找到缓存，才从磁盘加载
                    loadFullWordBook(book)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载词汇书时出错", e)
                _currentWord.value = null
                _totalWords.value = 0
                wordList = emptyList()
                _learningState.value = LearningState.Error("加载词汇书失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    // 加载完整词书内容（可在后台运行）
    private suspend fun loadFullWordBook(book: VocabularyBook) {
        try {
            // 从缓存加载单词或从文件加载
            wordList = if (WordBookCache.hasCached(book.filePath)) {
                Log.d(TAG, "【缓存】使用缓存中的词书内容：${book.filePath}")
                val cachedWordList = WordBookCache.getWordList(book.filePath) ?: emptyList()
                
                // 从内存同步单词状态，不查询数据库以提高性能
                cachedWordList
            } else {
                // 根据文件类型加载单词
                Log.d(TAG, "【缓存】缓存未命中，从${book.type}文件加载单词：${book.filePath}")
                val newWordList = when (book.type) {
                    BookType.CSV -> repository.loadWordsFromCsv(book.filePath)
                    BookType.TXT -> repository.loadWordsFromTxt(book.filePath)
                }
                // 保存到缓存
                WordBookCache.cacheWordList(book.filePath, newWordList)
                newWordList
            }
            
            Log.d(TAG, "【诊断】词库实际单词数量: ${wordList.size}")
            
            // 计算剩余需要学习的单词数
            val remainingToLearn = _dailyGoal.value - _todayLearned.value
            
            // 修复：恢复每日目标限制，但确保至少有1个单词可以学习
            val effectiveLimit = if (remainingToLearn <= 0) 1 else remainingToLearn
            _totalWords.value = minOf(wordList.size, effectiveLimit)
            
            Log.d(TAG, "已加载${_totalWords.value}个单词，今日目标：${_dailyGoal.value}，已学习：${_todayLearned.value}")
            
            // 如果单词列表为空，显示错误
            if (wordList.isEmpty()) {
                Log.e(TAG, "单词列表为空")
                _learningState.value = LearningState.Error("词汇书为空，请选择其他词汇书")
                return
            }
            
            // 如果当前没有显示单词，加载第一个单词
            if (_currentWord.value == null) {
                loadNextWord()
                _learningState.value = LearningState.Success
            }
            
            // 保存进度
            saveProgress()
        } catch (e: Exception) {
            Log.e(TAG, "后台加载词汇书时出错", e)
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
                    
                    // 如果不认识，加入未掌握列表并记录错误
                    if (!isKnown) {
                        unknownWords.add(currentProgress)
                        // 获取当前单词
                        val word = wordList[currentProgress]
                        if (word.id > 0) {
                            try {
                                // 记录错误
                                repository.incrementErrorCount(word.id)
                                Log.d(TAG, "成功记录错误：单词=${word.word}, ID=${word.id}")
                            } catch (e: Exception) {
                                Log.e(TAG, "记录错误失败: ${e.message}", e)
                            }
                        }
                    }
                    
                    // 更新进度
                    _progress.value = currentProgress + 1
                    
                    // 计算并保存总体进度
                    val totalProgress = _progress.value.toFloat() / _totalWords.value.toFloat()
                    getApplication<Application>().settingsDataStore.edit { 
                        it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = totalProgress
                    }
                    
                    // 获取当前单词并更新其状态
                    val word = wordList[currentProgress]
                    
                    // 计算下一次复习日期（根据是否认识来确定间隔）
                    val today = LocalDate.now()
                    val nextReviewInterval = if (isKnown) 1L else 1L // 如果认识，1天后复习；如果不认识，当天再复习
                    val nextReviewDate = today.plusDays(nextReviewInterval)
                    
                    // 更新数据库中单词状态
                    try {
                        // 确保单词有效的ID
                        if (word.id > 0) {
                            // 更新状态为NEEDS_REVIEW，这样单词就会出现在复习列表中
                            repository.updateWordStatus(
                                wordId = word.id,
                                status = WordStatus.NEEDS_REVIEW,
                                lastReviewDate = today,
                                nextReviewDate = nextReviewDate,
                                reviewCount = 0 // 新学习的单词，复习次数为0
                            )
                            Log.d(TAG, "单词[${word.word}]状态已更新为NEEDS_REVIEW，下次复习日期: $nextReviewDate")
                        } else {
                            Log.e(TAG, "无法更新单词状态：单词ID无效 (${word.id})")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "更新单词状态失败: ${e.message}", e)
                    }
                    
                    // 保存进度
                    saveProgress()
                    
                    // 如果还有下一个单词，加载它
                    if (currentProgress + 1 < _totalWords.value) {
                        val nextWord = wordList[currentProgress + 1]
                        _currentWord.value = WordCard(
                            id = nextWord.id,  // 复制ID
                            word = nextWord.word,
                            ukPhonetic = nextWord.ukPhonetic.ifEmpty { "/" + nextWord.word + "/" },
                            usPhonetic = nextWord.usPhonetic.ifEmpty { "/" + nextWord.word + "/" },
                            example = nextWord.example.ifEmpty { "${nextWord.word} - ${nextWord.meaning}" },
                            definition = nextWord.meaning,
                            isFavorite = nextWord.isFavorite,  // 复制收藏状态
                            errorCount = nextWord.errorCount   // 复制错误次数
                        )
                        Log.d(TAG, "已加载下一个单词：${nextWord.word}, ID=${nextWord.id}, isFavorite=${nextWord.isFavorite}")
                    } else {
                        // 学习完成
                        _currentWord.value = null
                    }
                }
                _learningState.value = LearningState.Success
                
                // 刷新首页数据，确保待学习和待复习数字更新
                try {
                    // 请求更新首页统计数据
                    repository.updateLearningCounts()
                    
                    // 额外确保首页立即刷新数据
                    (getApplication<Application>() as? App)?.homeViewModel?.forceRefreshNow()
                    
                    Log.d(TAG, "已请求刷新首页学习统计数据")
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