package com.example.wordlearn.navigation

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

// Screens
import com.example.wordlearn.ui.screens.SplashScreen
import com.example.wordlearn.ui.screens.HomeScreen
import com.example.wordlearn.ui.screens.ToolScreen
import com.example.wordlearn.ui.screens.UserScreen
import com.example.wordlearn.ui.screens.ProfileScreen
import com.example.wordlearn.ui.screens.DetailScreen
import com.example.wordlearn.ui.screens.learningplan.LearningPlanScreen
import com.example.wordlearn.ui.screens.learning.LearningScreen
import com.example.wordlearn.ui.screens.challenge.ChallengeScreen
import com.example.wordlearn.ui.screens.review.ReviewScreen
import com.example.wordlearn.ui.screens.WordbookSelectorScreen

// ViewModels
import com.example.wordlearn.ui.viewmodel.LearningViewModel
import com.example.wordapp.viewmodel.HomeViewModel

// Models
import com.example.wordlearn.data.model.BookType
import com.example.wordlearn.data.model.VocabularyBook

// 导航路由
sealed class NavRoute(val route: String) {
    object Home : NavRoute("home")
    object Learning : NavRoute("learning")
    object LearningPlan : NavRoute("learning_plan")
    object Challenge : NavRoute("challenge") {
        // 子路由
        const val Today = "challenge/today"
        const val Yesterday = "challenge/yesterday"
    }
    object Review : NavRoute("review")
}

@Composable
fun NavGraph(navController: NavHostController, innerPadding: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Splash Screen
        composable("splash") {
            SplashScreen(navController)
        }

        // Main screens
        composable(BottomNavItem.Home.route) {
            HomeScreen(navController, innerPadding)
        }
        composable(BottomNavItem.Tool.route) {
            ToolScreen(
                innerPadding = innerPadding,
                navController = navController
            )
        }
        composable(BottomNavItem.User.route) {
            UserScreen()
        }

        // 学习页面
        composable("learning") {
            val viewModel: LearningViewModel = viewModel()
            val homeViewModel: HomeViewModel = viewModel()
            val selectedBookName = homeViewModel.selectedBookName.collectAsStateWithLifecycle()
            val hasSelectedBook = homeViewModel.hasSelectedBook.collectAsStateWithLifecycle()
            val context = LocalContext.current
            
            // 初始化 ViewModel
            LaunchedEffect(Unit) {
                viewModel.initialize(context)
            }
            
            // 获取当前选中的词汇书
            LaunchedEffect(selectedBookName.value) {
                val bookName = selectedBookName.value
                if (hasSelectedBook.value && bookName.isNotEmpty()) {
                    Log.d("NavGraph", "正在加载词汇书：$bookName")
                    // 从 repository 获取词汇书
                    val availableBooks = viewModel.getAvailableBooks()
                    val book = availableBooks.find { it.name == bookName }
                    
                    if (book != null) {
                        Log.d("NavGraph", "找到词汇书：${book.name}")
                        viewModel.loadVocabularyBook(book)
                    } else {
                        Log.e("NavGraph", "未找到词汇书：$bookName")
                    }
                } else {
                    Log.d("NavGraph", "没有选中的词汇书或词汇书名为空")
                }
            }
            
            LearningScreen(navController = navController)
        }

        // Wordbook selector screen
        composable("wordbookSelector") {
            WordbookSelectorScreen(navController)
        }

        // Profile configuration screen
        composable("profile") {
            ProfileScreen(
                onComplete = {
                    // 完成后返回工具页面
                    navController.navigate(BottomNavItem.Tool.route) {
                        // 清除回退栈中的 profile 页面
                        popUpTo("profile") { inclusive = true }
                    }
                }
            )
        }

        // Learning plan screen
        composable(NavRoute.LearningPlan.route) {
            LearningPlanScreen(
                onBackClick = { navController.navigateUp() }
            )
        }

        // Challenge screens
        composable(NavRoute.Challenge.Today) {
            ChallengeScreen(
                isToday = true,
                onBackClick = { navController.navigateUp() }
            )
        }
        composable(NavRoute.Challenge.Yesterday) {
            ChallengeScreen(
                isToday = false,
                onBackClick = { navController.navigateUp() }
            )
        }

        // Review screen
        composable(NavRoute.Review.route) {
            val context = LocalContext.current
            ReviewScreen(
                navController = navController,
                context = context
            )
        }

        // Other screens
        composable("detail/{word}") { backStackEntry ->
            DetailScreen(backStackEntry.arguments?.getString("word") ?: "")
        }
    }
}
