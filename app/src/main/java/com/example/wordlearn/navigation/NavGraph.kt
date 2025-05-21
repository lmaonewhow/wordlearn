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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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
import com.example.wordlearn.ui.screens.FavoriteWordsScreen
import com.example.wordlearn.ui.screens.ErrorBookScreen

// ViewModels
import com.example.wordlearn.ui.viewmodel.LearningViewModel
import com.example.wordlearn.ui.viewmodel.HomeViewModel

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
    object Favorites : NavRoute("favorites")
    object ErrorBook : NavRoute("errorbook")
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
                Log.d("NavGraph", "【诊断】初始化LearningViewModel")
                viewModel.initialize(context)
            }
            
            // 获取当前选中的词汇书
            LaunchedEffect(selectedBookName.value) {
                val bookName = selectedBookName.value
                if (hasSelectedBook.value && bookName.isNotEmpty()) {
                    Log.d("NavGraph", "【诊断】正在加载词汇书：$bookName")
                    // 从 repository 获取词汇书
                    val availableBooks = viewModel.getAvailableBooks()
                    
                    Log.d("NavGraph", "【诊断】可用词汇书数量: ${availableBooks.size}")
                    Log.d("NavGraph", "【诊断】可用词汇书列表: ${availableBooks.joinToString(", ") { it.name }}")
                    
                    // 先尝试精确匹配
                    var book = availableBooks.find { it.name == bookName }
                    
                    // 如果精确匹配没找到，尝试部分匹配
                    if (book == null && availableBooks.isNotEmpty()) {
                        Log.d("NavGraph", "【诊断】未找到精确匹配词汇书，尝试部分匹配")
                        book = availableBooks.find { it.name.contains(bookName, ignoreCase = true) 
                                               || bookName.contains(it.name, ignoreCase = true) }
                    }
                    
                    if (book != null) {
                        Log.d("NavGraph", "【诊断】找到词汇书：${book.name}, 路径: ${book.filePath}")
                        
                        // 强制检查当前单词是否为null，如果是，确保加载词汇书
                        if (viewModel.currentWord.value == null) {
                            Log.d("NavGraph", "【诊断】当前单词为null，重新加载词汇书")
                            viewModel.loadVocabularyBook(book)
                        } else {
                            Log.d("NavGraph", "【诊断】当前单词不为null: ${viewModel.currentWord.value?.word}")
                        }
                    } else {
                        Log.e("NavGraph", "【诊断】未找到词汇书：$bookName")
                    }
                } else {
                    Log.d("NavGraph", "【诊断】没有选中的词汇书或词汇书名为空")
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

        // 收藏单词列表
        composable(NavRoute.Favorites.route) {
            FavoriteWordsScreen(
                onBackClick = { navController.navigateUp() }
            )
        }

        // 错题本
        composable(NavRoute.ErrorBook.route) {
            ErrorBookScreen(
                onBackClick = { navController.navigateUp() }
            )
        }

        // Other screens
        composable("detail/{word}") { backStackEntry ->
            DetailScreen(backStackEntry.arguments?.getString("word") ?: "")
        }
    }
}
