package com.example.wordlearn.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector


/*
* 底部导航栏配置
* */
sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "首页", Icons.Default.Home) // 首页
    object Tool : BottomNavItem("tool", "助手", Icons.Default.Build) // AI对话
    object User : BottomNavItem("user", "我的", Icons.Default.Person) // 我的设置
    object Profile : BottomNavItem("profile", "配置", Icons.Default.Person) // 词典个人配置
}
