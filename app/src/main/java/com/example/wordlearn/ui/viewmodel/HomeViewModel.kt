// HomeViewModel.kt
package com.example.wordapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.store.AppSettingsKeys
import com.example.wordlearn.data.store.settingsDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val dataStore = context.settingsDataStore

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
            dataStore.edit { it[AppSettingsKeys.SELECTED_BOOK_PROGRESS] = p } }
    }

    fun setUnit(unit: String) {
        viewModelScope.launch {
            dataStore.edit { it[AppSettingsKeys.SELECTED_BOOK_UNIT] = unit } }
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

    private val _newWords = MutableStateFlow(12)
    val newWords: StateFlow<Int> = _newWords

    private val _reviewWords = MutableStateFlow(6)
    val reviewWords: StateFlow<Int> = _reviewWords

    private val _isTaskCompleted = MutableStateFlow(false)
    val isTaskCompleted: StateFlow<Boolean> = _isTaskCompleted

    fun completeTask() {
        _isTaskCompleted.value = true
    }

    fun loadFromServer() {
        // TODO: 异步拉取数据，调用 .value = xxx 更新各 StateFlow
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

    // endregion
}
