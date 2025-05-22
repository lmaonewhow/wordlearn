package com.example.wordlearn.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.model.UserProfile
import com.example.wordlearn.data.store.AppSettingsKeys
import com.example.wordlearn.data.store.settingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ToolViewModel : ViewModel() {
    private val _isProfileCompleted = MutableStateFlow(false)
    val isProfileCompleted: StateFlow<Boolean> = _isProfileCompleted.asStateFlow()

    private val TAG = "ToolViewModel"

    /**
     * 检查用户是否已完成问卷配置
     */
    fun checkProfileCompletion(context: Context) {
        Log.d(TAG, "开始检查用户配置状态")
        viewModelScope.launch {
            try {
                val preferences = context.settingsDataStore.data.first()
                val isCompleted = preferences[AppSettingsKeys.IS_PROFILE_COMPLETED] ?: false
                Log.d(TAG, "用户配置完成状态: $isCompleted")
                
                // 检查配置数据是否存在
                val profileJson = preferences[AppSettingsKeys.PROFILE_JSON]
                if (profileJson != null) {
                    try {
                        // 创建json实例时指定序列化模块
                        val json = Json { 
                            ignoreUnknownKeys = true
                            isLenient = true 
                        }
                        val profile = json.decodeFromString<UserProfile>(profileJson)
                        Log.d(TAG, "成功反序列化用户配置: $profile")
                    } catch (e: Exception) {
                        Log.e(TAG, "反序列化用户配置失败", e)
                    }
                }
                
                Log.d(TAG, "用户配置数据: ${profileJson?.take(50) ?: "无"}")
                
                _isProfileCompleted.value = isCompleted
                Log.d(TAG, "更新状态流完成状态为: $isCompleted")
            } catch (e: Exception) {
                // 如果发生异常，默认为未完成
                Log.e(TAG, "检查用户配置时发生异常", e)
                _isProfileCompleted.value = false
            }
        }
    }
} 