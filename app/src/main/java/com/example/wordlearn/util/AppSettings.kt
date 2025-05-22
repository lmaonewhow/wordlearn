package com.example.wordlearn.data.store

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.floatPreferencesKey

// 定义偏好键
object AppSettingsKeys {
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val SELECTED_BOOK_ID = stringPreferencesKey("selected_book_id")
    val SELECTED_BOOK_PROGRESS = floatPreferencesKey("selected_book_progress")
    val SELECTED_BOOK_UNIT = stringPreferencesKey("selected_book_unit")
    // 是否完成配置
    val IS_PROFILE_COMPLETED = booleanPreferencesKey("is_profile_completed")
    // 个人配置数据
    val PROFILE_JSON = stringPreferencesKey("user_profile_json")
}

// 扩展属性，用于在 Context 上访问 DataStore 实例
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
