package com.example.wordlearn.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.wordlearn.ui.viewmodel.HomeViewModel
import com.example.wordlearn.ui.viewmodel.LearningPlanViewModel
import com.example.wordlearn.ui.viewmodel.LearningViewModel
import com.example.wordlearn.ui.components.WordbookCard
import com.example.wordlearn.navigation.NavRoute

@Composable
fun HomeScreen(
    navController: NavController, 
    innerPadding: PaddingValues, 
    viewModel: HomeViewModel = viewModel(),
    learningPlanViewModel: LearningPlanViewModel,
    learningViewModel: LearningViewModel
) {
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
    val totalWords by viewModel.totalWords.collectAsState()
    val learnedWords by viewModel.learnedWords.collectAsState()
    
    // 从LearningViewModel获取今日已学习单词数
    val todayLearned by learningViewModel.todayLearned.collectAsState()
    
    // 添加隐藏的刷新功能
    var tapCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    // 首次显示时自动刷新
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            viewModel.loadTodayProgress()
        }
    }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        tapCount++
                        if (tapCount >= 5) {
                            tapCount = 0
                            // 显示刷新提示
                            Toast.makeText(context, "正在刷新数据...", Toast.LENGTH_SHORT).show()
                            // 强制刷新数据
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                viewModel.loadTodayProgress()
                            }
                        }
                    },
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
                totalWords = totalWords,
                learnedWords = learnedWords,
                onSelectBookClick = { navController.navigate("wordbookSelector") },
                onStudyClick = { 
                    if (hasSelectedBook) {
                        navController.navigate("learning")
                    } else {
                        navController.navigate("wordbookSelector")
                    }
                },
                onReviewClick = { navController.navigate("review") },
                learningPlanViewModel = learningPlanViewModel,
                todayLearned = todayLearned
            )
        }

        item {
            // 挑战区，更新为多游戏卡片
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🎮 趣味记忆", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Text("更多", 
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable { navController.navigate("gameCenter") },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("玩游戏，轻松记单词", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    
                    // 游戏选择区
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(220.dp)
                    ) {
                        // 匹配游戏
                        item {
                            GameCard(
                                title = "词义匹配",
                                description = "连接单词与含义",
                                icon = Icons.Default.Games,
                                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                onClick = { navController.navigate(NavRoute.Challenge.Today) }
                            )
                        }
                        // 填空游戏
                        item {
                            GameCard(
                                title = "单词填空",
                                description = "根据提示填写单词",
                                icon = Icons.Default.Edit,
                                backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                onClick = { navController.navigate("fillInBlanks") }
                            )
                        }
                        // 单词接龙
                        item {
                            GameCard(
                                title = "单词接龙",
                                description = "用尾字母开始新词",
                                icon = Icons.Default.Loop,
                                backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                onClick = { navController.navigate("wordChain") }
                            )
                        }
                        // 速记挑战
                        item {
                            GameCard(
                                title = "速记挑战",
                                description = "限时记忆单词列表",
                                icon = Icons.Default.Timer,
                                backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                onClick = { navController.navigate("memoryChallenge") }
                            )
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
                        "收藏本" -> navController.navigate(NavRoute.Favorites.route)
                        "错题本" -> navController.navigate(NavRoute.ErrorBook.route)
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

@Composable
fun GameCard(
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.95f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
