package com.example.wordlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.wordapp.viewmodel.HomeViewModel
import com.example.wordlearn.ui.components.WordbookCard
import com.example.wordlearn.navigation.NavRoute

@Composable
fun HomeScreen(navController: NavController, innerPadding: PaddingValues, viewModel: HomeViewModel = viewModel()) {
    val username by viewModel.username.collectAsState()
    val remembered by viewModel.rememberedWords.collectAsState()
    val forgotten by viewModel.forgottenWords.collectAsState()
    val newWords by viewModel.newWords.collectAsState()
    val reviewWords by viewModel.reviewWords.collectAsState()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val hasSelectedBook by viewModel.hasSelectedBook.collectAsState()
    val bookName by viewModel.selectedBookName.collectAsState()
    val unitLabel by viewModel.currentUnit.collectAsState()
    val progress by viewModel.progress.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp)
    ) {
        item {
            // 顶部欢迎区
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像与昵称（可后续加头像）
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Hi, $username 👋",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "记忆 $remembered 词 ｜ 遗忘 $forgotten 词",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // 快捷功能按钮
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HomeTopAction(icon = Icons.Default.Face, label = "排行") {
                            navController.navigate("排行榜")
                        }
                        HomeTopAction(icon = Icons.Default.List, label = "任务") {
                            navController.navigate("任务")
                        }
                    }
                }
            }
        }

        item {
            // 单词本卡片
            WordbookCard(
                isFirstLaunch = isFirstLaunch,
                hasSelectedBook = hasSelectedBook,
                bookName = bookName,
                unitLabel = unitLabel,
                progress = progress,
                newWords = newWords,
                reviewWords = reviewWords,
                onSelectBookClick = { navController.navigate("wordbookSelector") },
                onStudyClick = { 
                    if (hasSelectedBook) {
                        navController.navigate("learning")
                    } else {
                        navController.navigate("wordbookSelector")
                    }
                },
                onReviewClick = { navController.navigate("review") }
            )
        }

        item {
            // 挑战区，合并为单卡片
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("🎲 挑战专区", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("五词匹配游戏，测测你的记忆力", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { navController.navigate(NavRoute.Challenge.Today) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("今日挑战")
                        }
                        OutlinedButton(
                            onClick = { navController.navigate(NavRoute.Challenge.Yesterday) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("昨日挑战")
                        }
                    }
                }
            }
        }

        item {
            // 功能区
            Text(
                text = "更多功能",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                FeatureGridSection(navController)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun HomeTopAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 13.sp)
    }
}

@Composable
fun FeatureGridSection(navController: NavController) {
    val features = listOf(
        Icons.Default.Star to "学习计划",
        Icons.Default.Favorite to "收藏本",
        Icons.Default.Person to "错题本",
        Icons.Default.Info to "学习详情",
        Icons.Default.DateRange to "助记共建",
        Icons.Default.Build to "敬请期待"
    )
    // 栅格布局3列，每行间距12dp，左右留白16dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .heightIn(max = 400.dp) // 必须明确指定高度
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(features) { (icon, title) ->
            FeatureCard(
                icon = icon,
                title = title,
                onClick = {
                    when (title) {
                        "学习计划" -> navController.navigate(NavRoute.LearningPlan.route)
                        // 其他功能的导航待实现
                    }
                }
            )
        }
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
