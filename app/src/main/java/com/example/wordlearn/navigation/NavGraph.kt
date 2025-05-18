package com.example.wordlearn.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.wordlearn.ui.screens.*
import com.example.wordlearn.ui.screens.learningplan.LearningPlanScreen

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
        composable("learningPlan") {
            LearningPlanScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Other screens
        composable("detail/{word}") { backStackEntry ->
            DetailScreen(backStackEntry.arguments?.getString("word") ?: "")
        }
    }
}
