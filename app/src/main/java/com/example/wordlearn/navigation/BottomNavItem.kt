package com.example.wordlearn.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "首页", Icons.Default.Home)
    object Tool : BottomNavItem("tool", "工具", Icons.Default.Build)
    object User : BottomNavItem("user", "我的", Icons.Default.Person)
    object Profile : BottomNavItem("profile", "配置", Icons.Default.Person)
}
