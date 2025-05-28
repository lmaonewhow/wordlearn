package com.example.wordlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wordlearn.navigation.NavRoute
import com.example.wordlearn.ui.viewmodel.LearningViewModel
import com.example.wordlearn.ui.viewmodel.AchievementViewModel

data class GameInfo(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCenterScreen(
    navController: NavController,
    viewModel: LearningViewModel,
    achievementViewModel: AchievementViewModel
) {
    // 游戏列表
    val games = listOf(
        GameInfo(
            id = "wordMatch",
            title = "词义匹配",
            description = "连接单词与含义，训练词汇理解能力",
            icon = Icons.Default.Games,
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            route = NavRoute.WordMatch.route
        ),
        GameInfo(
            id = "fillBlanks",
            title = "单词填空",
            description = "根据提示填写单词，强化拼写记忆",
            icon = Icons.Default.Edit,
            backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
            route = NavRoute.FillBlanks.route
        ),
        GameInfo(
            id = "wordChain",
            title = "单词接龙",
            description = "用尾字母开始新词，扩展词汇量",
            icon = Icons.Default.Loop,
            backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
            route = NavRoute.WordChain.route
        ),
        GameInfo(
            id = "memoryChallenge",
            title = "速记挑战",
            description = "限时记忆单词列表，提升记忆力",
            icon = Icons.Default.Timer,
            backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            route = NavRoute.MemoryChallenge.route
        ),
        GameInfo(
            id = "challenge",
            title = "今日挑战",
            description = "今日五词匹配，测试今日学习成果",
            icon = Icons.Default.Today,
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            route = NavRoute.Challenge.Today
        ),
        GameInfo(
            id = "yesterdayChallenge",
            title = "昨日挑战",
            description = "昨日五词匹配，巩固昨日学习成果",
            icon = Icons.Default.History,
            backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
            route = NavRoute.Challenge.Yesterday
        ),
        GameInfo(
            id = "pronunciation",
            title = "发音训练",
            description = "即将推出! 练习单词发音",
            icon = Icons.Default.RecordVoiceOver,
            backgroundColor = Color.Gray.copy(alpha = 0.2f),
            route = ""
        ),
        GameInfo(
            id = "crossword",
            title = "单词填字",
            description = "即将推出! 单词填字游戏",
            icon = Icons.Default.GridOn,
            backgroundColor = Color.Gray.copy(alpha = 0.2f),
            route = ""
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("游戏中心") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                "趣味游戏",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                "通过游戏化的方式学习和巩固单词记忆",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(games) { game ->
                    GameCard(game) {
                        if (game.route.isNotEmpty()) {
                            navController.navigate(game.route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameCard(
    gameInfo: GameInfo,
    onClick: () -> Unit
) {
    val isDisabled = gameInfo.route.isEmpty()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clickable(enabled = !isDisabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDisabled) 1.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = gameInfo.backgroundColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    gameInfo.icon,
                    contentDescription = gameInfo.title,
                    modifier = Modifier.size(32.dp),
                    tint = if (isDisabled) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = gameInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDisabled) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = gameInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = if (isDisabled) 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.heightIn(min = 40.dp)
                )
            }
            
            if (isDisabled) {
                SuggestionChip(
                    onClick = { },
                    enabled = false,
                    label = {
                        Text("即将推出", style = MaterialTheme.typography.bodySmall)
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
} 