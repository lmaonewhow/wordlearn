package com.example.wordlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.wordlearn.navigation.BottomBar
import com.example.wordlearn.navigation.NavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val navController = rememberNavController()
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

            Scaffold(
                bottomBar = {
                    // Only show bottom bar if not on splash screen and not on learning screen
                    if (currentRoute != "splash" && currentRoute != "learning") {
                        BottomBar(navController)
                    }
                }
            ) { innerPadding ->
                NavGraph(navController, innerPadding)
            }
        }
    }
}


