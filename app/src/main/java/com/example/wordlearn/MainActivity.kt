package com.example.wordlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.wordlearn.navigation.BottomBar
import com.example.wordlearn.navigation.NavGraph
import com.example.wordlearn.ui.theme.ThemeManager
import com.example.wordlearn.ui.theme.WordLearnTheme
import com.example.wordlearn.ui.components.AchievementUnlockedDialog
import com.example.wordlearn.ui.viewmodel.AchievementViewModel
import com.example.wordlearn.data.Achievement
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private val achievementViewModel: AchievementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化主题管理器
        ThemeManager.init(this)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 触发数据加载
        val app = application as App
        val homeViewModel = app.homeViewModel
        if (homeViewModel != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            homeViewModel.loadTodayProgress()
        }

        setContent {
            WordLearnTheme {
                val navController = rememberNavController()
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute != "splash" && currentRoute != "learning") {
                            BottomBar(navController) //控制是否显示底部栏
                        }
                    }
                ) { innerPadding ->
                    NavGraph(navController = navController, innerPadding = innerPadding)
                    
                    // 监听成就解锁并显示通知
                    val newlyUnlockedAchievement by achievementViewModel.newlyUnlockedAchievement.collectAsState()
                    
                    // 显示成就解锁对话框
                    var showAchievementDialog by remember { mutableStateOf(false) }
                    var currentAchievement by remember { mutableStateOf<Achievement?>(null) }
                    
                    // 监听新解锁的成就
                    LaunchedEffect(newlyUnlockedAchievement) {
                        newlyUnlockedAchievement?.let {
                            currentAchievement = it
                            showAchievementDialog = true
                            // 清除新解锁状态
                            achievementViewModel.clearNewlyUnlockedAchievement()
                        }
                    }
                    
                    // 成就解锁对话框
                    if (showAchievementDialog && currentAchievement != null) {
                        AchievementUnlockedDialog(
                            achievement = currentAchievement!!,
                            onDismiss = { showAchievementDialog = false }
                        )
                    }
                }
            }
        }
    }
}


