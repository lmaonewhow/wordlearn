package com.example.wordlearn.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

// 主题管理器
object ThemeManager {
    // 暗色模式状态
    private val _isDarkMode = mutableStateOf(false)
    val isDarkMode: MutableState<Boolean> = _isDarkMode

    // 初始化主题
    fun init(context: Context) {
        val sharedPrefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        _isDarkMode.value = sharedPrefs.getBoolean("dark_mode", false)
    }

    // 切换主题
    fun toggleTheme(context: Context) {
        _isDarkMode.value = !_isDarkMode.value
        // 保存设置
        context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("dark_mode", _isDarkMode.value)
            .apply()
    }

    // 设置主题
    fun setDarkMode(context: Context, isDark: Boolean) {
        _isDarkMode.value = isDark
        // 保存设置
        context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("dark_mode", isDark)
            .apply()
    }
}

// 创建一个 CompositionLocal 来提供主题状态
val LocalThemeManager = staticCompositionLocalOf { ThemeManager } 