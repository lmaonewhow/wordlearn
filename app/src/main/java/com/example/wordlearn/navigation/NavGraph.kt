package com.example.wordlearn.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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
import com.example.wordlearn.ui.screens.learningplan.LearningPlanConfigScreen
import com.example.wordlearn.ui.screens.learning.LearningScreen
import com.example.wordlearn.ui.screens.challenge.ChallengeScreen
import com.example.wordlearn.ui.screens.review.ReviewScreen
import com.example.wordlearn.ui.screens.WordbookSelectorScreen
import com.example.wordlearn.ui.screens.FavoriteWordsScreen
import com.example.wordlearn.ui.screens.ErrorBookScreen
import com.example.wordlearn.ui.screens.WordMatchScreen
import com.example.wordlearn.ui.screens.FillBlanksScreen
import com.example.wordlearn.ui.screens.WordChainScreen
import com.example.wordlearn.ui.screens.MemoryChallengeScreen
import com.example.wordlearn.ui.screens.GameCenterScreen
import com.example.wordlearn.ui.screens.AllAchievementsScreen

// ViewModels
import com.example.wordlearn.ui.viewmodel.LearningViewModel
import com.example.wordlearn.ui.viewmodel.HomeViewModel
import com.example.wordlearn.ui.viewmodel.LearningPlanViewModel
import com.example.wordlearn.ui.viewmodel.AchievementViewModel

// Models
import com.example.wordlearn.data.model.BookType
import com.example.wordlearn.data.model.VocabularyBook

/*
*
* 导航路由配置
* */
sealed class NavRoute(val route: String) {
    object Detail :NavRoute("detail/{word}")
    object Learning : NavRoute("learning")
    object LearningPlan : NavRoute("learning_plan")
    object LearningPlanConfig : NavRoute("learning_plan_config")
    object Challenge : NavRoute("challenge") {
        // 子路由
        const val Today = "challenge/today"
        const val Yesterday = "challenge/yesterday"
    }
    object Review : NavRoute("review")
    object Favorites : NavRoute("favorites")
    object ErrorBook : NavRoute("errorbook")
    object WordbookSelector :NavRoute("wordbookSelector")
    
    // 新增游戏路由
    object WordMatch : NavRoute("wordMatch")
    object FillBlanks : NavRoute("fillInBlanks")
    object WordChain : NavRoute("wordChain")
    object MemoryChallenge : NavRoute("memoryChallenge")
    object GameCenter : NavRoute("gameCenter")
    
    // 成就系统路由
    object AllAchievements : NavRoute("allAchievements")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(navController: NavHostController, innerPadding: PaddingValues) {
    // 创建共享的ViewModel实例
    val homeViewModel: HomeViewModel = viewModel()
    val learningViewModel: LearningViewModel = viewModel()
    val learningPlanViewModel: LearningPlanViewModel = viewModel()
    val achievementViewModel: AchievementViewModel = viewModel()
    
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
            HomeScreen(navController, innerPadding, homeViewModel, learningPlanViewModel, learningViewModel)
        }
        composable(BottomNavItem.Tool.route) {
            ToolScreen(
                innerPadding = innerPadding,
                navController = navController
            )
        }
        composable(BottomNavItem.User.route) {
            UserScreen(
                navController = navController,
                viewModel = achievementViewModel
            )
        }

        // 学习页面
        composable(NavRoute.Learning.route) {
            val selectedBookName = homeViewModel.selectedBookName.collectAsStateWithLifecycle()
            val hasSelectedBook = homeViewModel.hasSelectedBook.collectAsStateWithLifecycle()
            val context = LocalContext.current
            
            // 初始化 ViewModel
            LaunchedEffect(Unit) {
                Log.d("NavGraph", "【诊断】初始化LearningViewModel")
                learningViewModel.initialize(context)
            }
            
            // 获取当前选中的词汇书
            LaunchedEffect(selectedBookName.value) {
                val bookName = selectedBookName.value
                if (hasSelectedBook.value && bookName.isNotEmpty()) {
                    Log.d("NavGraph", "【诊断】正在加载词汇书：$bookName")
                    // 从 repository 获取词汇书
                    val availableBooks = learningViewModel.getAvailableBooks()
                    
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
                        if (learningViewModel.currentWord.value == null) {
                            Log.d("NavGraph", "【诊断】当前单词为null，重新加载词汇书")
                            learningViewModel.loadVocabularyBook(book)
                        } else {
                            Log.d("NavGraph", "【诊断】当前单词不为null: ${learningViewModel.currentWord.value?.word}")
                        }
                    } else {
                        Log.e("NavGraph", "【诊断】未找到词汇书：$bookName")
                    }
                } else {
                    Log.d("NavGraph", "【诊断】没有选中的词汇书或词汇书名为空")
                }
            }
            
            LearningScreen(navController = navController, viewModel = learningViewModel)
        }

        // Wordbook selector screen
        composable(NavRoute.WordbookSelector.route) {
            WordbookSelectorScreen(navController, homeViewModel)
        }

        // 学习计划配置页面（词书选择后跳转到这里）
        composable(NavRoute.LearningPlanConfig.route) {
            val selectedBookName = homeViewModel.selectedBookName.collectAsState()
            
            // 加载该词书的相关信息
            LaunchedEffect(selectedBookName.value) {
                // 使用共享的learningPlanViewModel初始化词书相关的学习计划
                learningPlanViewModel.initializeWithBook(selectedBookName.value)
            }
            
            LearningPlanConfigScreen(
                onComplete = {
                    // 配置完成后标记首次启动已完成
                    homeViewModel.markFirstLaunchComplete()
                    // 配置完成后导航到主页
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo(NavRoute.LearningPlanConfig.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                viewModel = learningPlanViewModel
            )
        }

        // Profile configuration screen
        composable(BottomNavItem.Profile.route) {
            Log.d("NavGraph", "进入ProfileScreen页面")
            ProfileScreen(
                onComplete = {
                    // 完成后返回工具页面
                    Log.d("NavGraph", "ProfileScreen完成，准备导航回ToolScreen")
                    
                    // 先清除当前回退栈并导航到首页，然后再导航到工具页面
                    // 这样可以保证工具页面有一个干净的回退栈
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true  // 移除起始页面
                        }
                    }
                    
                    // 确保首页导航完成后再导航到工具页面
                    navController.navigate(BottomNavItem.Tool.route) {
                        launchSingleTop = true  // 避免多次创建相同页面
                        Log.d("NavGraph", "导航到工具页面: ${BottomNavItem.Tool.route}")
                    }
                    
                    Log.d("NavGraph", "导航完成，当前路由栈: ${navController.currentBackStackEntry?.destination?.route}")
                }
            )
        }

        // Learning plan screen
        composable(NavRoute.LearningPlan.route) {
            LearningPlanScreen(
                onBackClick = { navController.navigateUp() },
                viewModel = learningPlanViewModel
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

        // 词义匹配游戏
        composable(NavRoute.WordMatch.route) {
            WordMatchScreen(
                navController = navController,
                viewModel = learningViewModel,
                achievementViewModel = achievementViewModel
            )
        }
        
        // 单词填空游戏
        composable(NavRoute.FillBlanks.route) {
            FillBlanksScreen(
                navController = navController,
                viewModel = learningViewModel,
                achievementViewModel = achievementViewModel
            )
        }
        
        // 单词接龙游戏
        composable(NavRoute.WordChain.route) {
            WordChainScreen(
                navController = navController,
                viewModel = learningViewModel,
                achievementViewModel = achievementViewModel
            )
        }
        
        // 速记挑战游戏
        composable(NavRoute.MemoryChallenge.route) {
            MemoryChallengeScreen(
                navController = navController,
                viewModel = learningViewModel,
                achievementViewModel = achievementViewModel
            )
        }
        
        // 游戏中心
        composable(NavRoute.GameCenter.route) {
            GameCenterScreen(
                navController = navController,
                viewModel = learningViewModel,
                achievementViewModel = achievementViewModel
            )
        }

        // 成就详情页面
        composable(NavRoute.AllAchievements.route) {
            AllAchievementsScreen(
                navController = navController
            )
        }

        // Other screens
        composable(NavRoute.Detail.route) { backStackEntry ->
            DetailScreen(backStackEntry.arguments?.getString("word") ?: "")
        }
    }
}
