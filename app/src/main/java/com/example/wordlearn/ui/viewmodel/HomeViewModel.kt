// HomeViewModel.kt
package com.example.wordlearn.ui.viewmodel
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.App
import com.example.wordlearn.data.store.AppSettingsKeys
import com.example.wordlearn.data.store.settingsDataStore
import com.example.wordlearn.data.repository.VocabularyRepository
import com.example.wordlearn.data.model.WordStatus
import com.example.wordlearn.data.model.VocabularyBook
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val dataStore = context.settingsDataStore
    private val vocabularyRepository = (application as App).vocabularyRepository
    
    // region —— 持久状态（DataStore）
    val isFirstLaunch: StateFlow<Boolean> = dataStore.data
        .map { it[AppSettingsKeys.IS_FIRST_LAUNCH] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hasSelectedBook: StateFlow<Boolean> = dataStore.data
        .map { it[AppSettingsKeys.SELECTED_BOOK_ID] != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedBookName: StateFlow<String> = dataStore.data
        .map { it[AppSettingsKeys.SELECTED_BOOK_ID] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val progress: StateFlow<Float> = dataStore.data
        .map { it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val currentUnit: StateFlow<String> = dataStore.data
        .map { it[AppSettingsKeys.SELECTED_BOOK_UNIT] ?: "Unit 1" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Unit 1")
        
    // 添加当前活跃词书ID
    private val _activeBookId = MutableStateFlow<String>("")
    val activeBookId: StateFlow<String> = _activeBookId

    fun markFirstLaunchComplete() {
        viewModelScope.launch {
            dataStore.edit { it[AppSettingsKeys.IS_FIRST_LAUNCH] = false }
        }
    }

    fun selectWordbook(bookName: String) {
        viewModelScope.launch {
            dataStore.edit {
                it[AppSettingsKeys.SELECTED_BOOK_ID] = bookName
                it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = 0f
                it[AppSettingsKeys.SELECTED_BOOK_UNIT] = "Unit 1"
                it[AppSettingsKeys.IS_FIRST_LAUNCH] = false
            }
            
            // 保存词书选择
            _activeBookId.value = bookName
            
            // 加载选定词书的数据
            loadSelectedWordbookData()
        }
    }
    
    // 加载活跃词书的数据
    fun loadSelectedWordbookData() {
        viewModelScope.launch {
            try {
                // 检查我们是否有选择的词书
                val selectedBookName = dataStore.data.first()[AppSettingsKeys.SELECTED_BOOK_ID] ?: ""
                
                if (selectedBookName.isNotEmpty()) {
                    Log.d("HomeViewModel", "加载词书数据: $selectedBookName")
                    _activeBookId.value = selectedBookName
                    
                    // 获取词书中的单词统计
                    val allWords = vocabularyRepository.getAllWords()
                    val today = LocalDate.now()
                    
                    // 统计各类单词
                    val totalCount = allWords.size
                    val learnedCount = allWords.count { it.status != WordStatus.NEW }
                    val newWordsCount = allWords.count { it.status == WordStatus.NEW }
                    val reviewWordsCount = allWords.count { 
                        it.status == WordStatus.NEEDS_REVIEW && 
                        it.nextReviewDate != null && 
                        !it.nextReviewDate.isAfter(today) 
                    }
                    
                    // 计算学习进度
                    val progressRate = if (totalCount > 0) {
                        learnedCount.toFloat() / totalCount.toFloat()
                    } else {
                        0f
                    }
                    
                    // 更新UI状态
                    _newWords.value = newWordsCount
                    _reviewWords.value = reviewWordsCount
                    _totalWords.value = totalCount
                    _learnedWords.value = learnedCount
                    
                    // 更新DataStore中的词书进度
                    dataStore.edit {
                        it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = progressRate
                    }
                    
                    Log.d("HomeViewModel", "词书数据加载完成 - 总单词数: $totalCount, 已学习: $learnedCount, 待学习: $newWordsCount, 待复习: $reviewWordsCount")
                } else {
                    Log.d("HomeViewModel", "没有找到活跃词书")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "加载词书数据失败", e)
            }
        }
    }

    fun setProgress(p: Float) {
        viewModelScope.launch {
            dataStore.edit { it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = p }
        }
    }

    fun setUnit(unit: String) {
        viewModelScope.launch {
            dataStore.edit { it[AppSettingsKeys.SELECTED_BOOK_UNIT] = unit }
        }
    }
    // endregion

    // region —— UI 状态（统一用 StateFlow）

    private val _username = MutableStateFlow("Alex")
    val username: StateFlow<String> = _username

    private val _rememberedWords = MutableStateFlow(28)
    val rememberedWords: StateFlow<Int> = _rememberedWords

    private val _forgottenWords = MutableStateFlow(6)
    val forgottenWords: StateFlow<Int> = _forgottenWords

    private val _todayReviewCount = MutableStateFlow(5)
    val todayReviewCount: StateFlow<Int> = _todayReviewCount

    // 使用MutableStateFlow而不是硬编码值
    private val _newWords = MutableStateFlow(0)
    val newWords: StateFlow<Int> = _newWords

    private val _reviewWords = MutableStateFlow(0)
    val reviewWords: StateFlow<Int> = _reviewWords

    // 添加总单词数和已学习单词数，默认值为0而非硬编码值
    private val _totalWords = MutableStateFlow(0)
    val totalWords: StateFlow<Int> = _totalWords
    
    private val _learnedWords = MutableStateFlow(0)
    val learnedWords: StateFlow<Int> = _learnedWords

    private val _isTaskCompleted = MutableStateFlow(false)
    val isTaskCompleted: StateFlow<Boolean> = _isTaskCompleted
    
    // 添加当前词书
    private val _currentVocabularyBook = MutableStateFlow<VocabularyBook?>(null)
    val currentVocabularyBook: StateFlow<VocabularyBook?> = _currentVocabularyBook

    init {
        // 把自身保存到App实例中，方便其他组件调用
        (application as App).homeViewModel = this
        
        // 初始化时加载真实数据
        loadTodayProgress()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadTodayProgress() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "开始加载今日任务数据...")
                
                // 获取当前选中的词书名称
                val selectedBookName = dataStore.data.first()[AppSettingsKeys.SELECTED_BOOK_ID]
                
                if (!selectedBookName.isNullOrEmpty()) {
                    _activeBookId.value = selectedBookName
                    
                    // 获取单词统计
                    val allWords = vocabularyRepository.getAllWords()
                    val today = LocalDate.now()
                    
                    // 获取学习计划目标和今日已学习单词数
                    val app = getApplication<App>()
                    val dailyGoal = app.getLearningPlanGoal() ?: 10
                    val todayLearned = app.getTodayLearnedCount() ?: 0
                    
                    // 统计各类单词
                    val totalCount = allWords.size
                    val learnedCount = allWords.count { it.status != WordStatus.NEW }
                    val newWordsCount = allWords.count { it.status == WordStatus.NEW }
                    val reviewWordsCount = allWords.count { 
                        it.status == WordStatus.NEEDS_REVIEW && 
                        it.nextReviewDate != null && 
                        !it.nextReviewDate.isAfter(today) 
                    }
                    
                    // 计算今日剩余可学习单词
                    val remainingToLearn = if (dailyGoal > todayLearned) {
                        // 还有剩余可学习单词
                        minOf(newWordsCount, dailyGoal - todayLearned)
                    } else {
                        // 今日目标已完成，在首页显示每日目标值，与WordbookCard逻辑保持一致
                        dailyGoal
                    }
                    
                    // 更新UI状态
                    _newWords.value = remainingToLearn
                    _reviewWords.value = reviewWordsCount
                    _totalWords.value = totalCount
                    _learnedWords.value = learnedCount
                    
                    // 计算学习进度
                    val progressRate = if (totalCount > 0) {
                        learnedCount.toFloat() / totalCount.toFloat()
                    } else {
                        0f
                    }
                    
                    // 更新DataStore中的词书进度
                    dataStore.edit {
                        it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = progressRate
                    }
                    
                    Log.d("HomeViewModel", "词书数据加载完成 - 总单词数: $totalCount, 已学习: $learnedCount, 待学习: $remainingToLearn, 待复习: $reviewWordsCount, 今日已学: $todayLearned")
                } else {
                    Log.d("HomeViewModel", "没有找到活跃词书")
                }
            } catch (e: Exception) {
                // 保留当前值，记录错误
                Log.e("HomeViewModel", "加载单词数据失败", e)
                e.printStackTrace()
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshAfterTaskReturn() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "==== 学习/复习任务返回，刷新数据 ====")
                
                // 获取单词统计
                val allWords = vocabularyRepository.getAllWords()
                val today = LocalDate.now()
                
                // 获取学习计划目标和今日已学习单词数
                val app = getApplication<App>()
                val dailyGoal = app.getLearningPlanGoal() ?: 10
                val todayLearned = app.getTodayLearnedCount() ?: 0
                
                // 统计各类单词
                val newWordsCount = allWords.count { it.status == WordStatus.NEW }
                val reviewWordsCount = allWords.count { 
                    it.status == WordStatus.NEEDS_REVIEW && 
                    it.nextReviewDate != null && 
                    !it.nextReviewDate.isAfter(today) 
                }
                
                // 计算今日剩余可学习单词
                val remainingToLearn = if (dailyGoal > todayLearned) {
                    // 还有剩余可学习单词
                    minOf(newWordsCount, dailyGoal - todayLearned)
                } else {
                    // 今日目标已完成，在首页显示每日目标值，与WordbookCard逻辑保持一致
                    dailyGoal
                }
                
                // 更新UI状态
                _newWords.value = remainingToLearn
                _reviewWords.value = reviewWordsCount
                
                Log.d("HomeViewModel", "刷新后 - 每日目标: $dailyGoal, 已学习: $todayLearned, 待学习: $remainingToLearn, 待复习: $reviewWordsCount")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "刷新任务数据失败", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadRealWordCounts() {
        // 为了向后兼容，保留这个方法，调用loadTodayProgress
        loadTodayProgress()
    }

    fun completeTask() {
        _isTaskCompleted.value = true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadFromServer() {
        // 加载真实数据
        loadTodayProgress()
    }

    fun Context.getWordbookNames(): List<String> {
        return assets.list("m-word")
            ?.filter { it.endsWith(".csv") }
            ?.map { it.removeSuffix(".csv") } // 去掉后缀显示更美观
            ?: emptyList()
    }

    fun setSelectedBookId(id: String) {
        viewModelScope.launch {
            dataStore.edit { it[AppSettingsKeys.SELECTED_BOOK_ID] = id }
        }
    }

    // 强制立即刷新数据的方法，供其他组件调用
    @RequiresApi(Build.VERSION_CODES.O)
    fun forceRefreshNow() {
        // 调用refreshAfterTaskReturn方法，避免重复代码
        refreshAfterTaskReturn()
    }

    // endregion
}
