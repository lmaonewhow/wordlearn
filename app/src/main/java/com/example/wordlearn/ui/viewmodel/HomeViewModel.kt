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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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

    fun markFirstLaunchComplete() {
        viewModelScope.launch {
            dataStore.edit { it[AppSettingsKeys.IS_FIRST_LAUNCH] = false }
        }
    }

    fun selectWordbook(id: String) {
        viewModelScope.launch {
            dataStore.edit {
                it[AppSettingsKeys.SELECTED_BOOK_ID] = id
                it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = 0f
                it[AppSettingsKeys.SELECTED_BOOK_UNIT] = "Unit 1"
                it[AppSettingsKeys.IS_FIRST_LAUNCH] = false
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

    // 添加总单词数和已学习单词数
    private val _totalWords = MutableStateFlow(4078)
    val totalWords: StateFlow<Int> = _totalWords
    
    private val _learnedWords = MutableStateFlow(30)
    val learnedWords: StateFlow<Int> = _learnedWords

    private val _isTaskCompleted = MutableStateFlow(false)
    val isTaskCompleted: StateFlow<Boolean> = _isTaskCompleted

    init {
        // 把自身保存到App实例中，方便其他组件调用
        (application as App).homeViewModel = this
        
        // 初始化时加载真实数据
        loadRealWordCounts()
        
        // 每当UI可见时刷新数据
        viewModelScope.launch {
            while(true) {
                loadRealWordCounts()
                delay(5000) // 每5秒更新一次，提高刷新频率
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadRealWordCounts() {
        viewModelScope.launch {
            try {
                // 获取今日需要学习的新单词数量
                val todayNewWordsCount = vocabularyRepository.getTodayNewWordsCount()
                _newWords.value = todayNewWordsCount

                // 获取今日需要复习的单词数量
                val todayReviewWordsCount = vocabularyRepository.getTodayReviewWordsCount()
                _reviewWords.value = todayReviewWordsCount
                
                // 使用默认值，因为Repository中没有相应的方法
                // _totalWords 和 _learnedWords 已经在声明时初始化
            } catch (e: Exception) {
                // 保留当前值，记录错误
                e.printStackTrace()
            }
        }
    }

    fun completeTask() {
        _isTaskCompleted.value = true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadFromServer() {
        // 加载真实数据
        loadRealWordCounts()
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
        viewModelScope.launch {
            try {
                // 立即执行刷新
                loadRealWordCounts()
                
                // 打印确认日志
                val todayNewWordsCount = vocabularyRepository.getTodayNewWordsCount()
                val todayReviewWordsCount = vocabularyRepository.getTodayReviewWordsCount()
                
                Log.d("HomeViewModel", "强制刷新 - 待学习单词: $todayNewWordsCount, 待复习单词: $todayReviewWordsCount")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "强制刷新失败", e)
            }
        }
    }

    // endregion
}
