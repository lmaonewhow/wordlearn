package com.example.wordlearn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.wordlearn.ui.viewmodel.LearningViewModel
import com.example.wordlearn.ui.viewmodel.AchievementViewModel
import com.example.wordlearn.data.model.Word
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordMatchScreen(
    navController: NavController,
    viewModel: LearningViewModel,
    achievementViewModel: AchievementViewModel
) {
    val wordsStateFlow = remember { viewModel.getRandomWords(5) }
    val words by wordsStateFlow.collectAsState()
    val scope = rememberCoroutineScope()
    
    // 游戏状态
    val gameState = remember { mutableStateOf(GameState.PLAYING) }
    val score = remember { mutableStateOf(0) }
    val round = remember { mutableStateOf(1) }
    val timer = remember { mutableStateOf(60) } // 60秒倒计时
    
    // 左侧单词列表和右侧含义列表
    val wordItems = remember { mutableStateListOf<MatchItem>() }
    val meaningItems = remember { mutableStateListOf<MatchItem>() }
    
    // 当前选中的项
    val selectedWordItem = remember { mutableStateOf<MatchItem?>(null) }
    val selectedMeaningItem = remember { mutableStateOf<MatchItem?>(null) }
    
    // 连线信息
    val connections = remember { mutableStateListOf<Connection>() }
    
    // 记录游戏体验成就
    LaunchedEffect(Unit) {
        achievementViewModel.recordGamePlayed("word_match")
    }
    
    // 初始化游戏数据
    LaunchedEffect(words, round.value) {
        // 清空之前的数据
        wordItems.clear()
        meaningItems.clear()
        connections.clear()
        selectedWordItem.value = null
        selectedMeaningItem.value = null
        
        // 填充新数据
        if (words.isNotEmpty()) {
            val shuffledMeanings = words.map { it.meaning }.shuffled()
            
            words.forEachIndexed { index, word ->
                wordItems.add(MatchItem(index, word.word, ItemType.WORD, word.meaning))
            }
            
            shuffledMeanings.forEachIndexed { index, meaning ->
                meaningItems.add(MatchItem(index, meaning, ItemType.MEANING))
            }
        }
    }
    
    // 倒计时逻辑
    LaunchedEffect(gameState.value) {
        if (gameState.value == GameState.PLAYING) {
            while (timer.value > 0) {
                delay(1000)
                timer.value--
                
                // 时间到，游戏结束
                if (timer.value == 0) {
                    gameState.value = GameState.FINISHED
                }
            }
        }
    }
    
    // 检查匹配是否完成
    LaunchedEffect(connections.size) {
        if (connections.size == wordItems.size && gameState.value == GameState.PLAYING) {
            // 检查是否全部正确匹配
            val allCorrect = connections.all { con ->
                val word = wordItems.find { it.id == con.wordId }
                val meaning = meaningItems.find { it.id == con.meaningId }
                word?.correctMeaning == meaning?.content
            }
            
            if (allCorrect) {
                score.value += timer.value + (10 * connections.size)
                delay(500) // 给用户一点时间看到完成的连线
                
                // 记录成就
                achievementViewModel.recordWordMatchScore(score.value)
                
                // 进入下一轮
                round.value++
                timer.value = 60
                
                // 获取新的单词
                scope.launch {
                    val newWordsFlow = viewModel.getRandomWords(5)
                    // 使用临时变量存储新单词，而不是直接修改val变量
                    val newWordsList = newWordsFlow.value
                    if (newWordsList.isNotEmpty()) {
                        // 不直接修改words，而是通过LaunchedEffect监听round.value的变化来更新UI
                    } else {
                        gameState.value = GameState.FINISHED
                    }
                }
            }
        }
    }
    
    // 游戏结束时记录成就
    LaunchedEffect(gameState.value) {
        if (gameState.value == GameState.FINISHED) {
            achievementViewModel.recordWordMatchScore(score.value)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("词义匹配") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 重新开始按钮
                    IconButton(onClick = {
                        scope.launch {
                            round.value = 1
                            score.value = 0
                            timer.value = 60
                            gameState.value = GameState.PLAYING
                            // 通过LaunchedEffect监听round.value的变化来刷新游戏
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新开始")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 游戏状态栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分数
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "得分",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${score.value}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // 轮次
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "轮次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "${round.value}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                // 倒计时
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (timer.value > 10) 
                            MaterialTheme.colorScheme.tertiaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "时间",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (timer.value > 10) 
                                MaterialTheme.colorScheme.onTertiaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "${timer.value}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (timer.value > 10) 
                                MaterialTheme.colorScheme.onTertiaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // 游戏主区域
            if (gameState.value == GameState.PLAYING) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 60.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 左侧单词列表
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            wordItems.forEach { item ->
                                val isSelected = selectedWordItem.value?.id == item.id
                                val isConnected = connections.any { it.wordId == item.id }
                                
                                MatchItemCard(
                                    item = item,
                                    isSelected = isSelected,
                                    isConnected = isConnected,
                                    onClick = {
                                        if (!isConnected) {
                                            selectedWordItem.value = item
                                            
                                            // 如果已经选择了含义，尝试连接
                                            selectedMeaningItem.value?.let { meaning ->
                                                connections.add(Connection(item.id, meaning.id))
                                                selectedWordItem.value = null
                                                selectedMeaningItem.value = null
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        
                        // 连线区域
                        Box(
                            modifier = Modifier
                                .weight(0.1f)
                                .fillMaxHeight()
                        ) {
                            ConnectionLines(
                                wordItems = wordItems,
                                meaningItems = meaningItems,
                                connections = connections
                            )
                        }
                        
                        // 右侧含义列表
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            meaningItems.forEach { item ->
                                val isSelected = selectedMeaningItem.value?.id == item.id
                                val isConnected = connections.any { it.meaningId == item.id }
                                
                                MatchItemCard(
                                    item = item,
                                    isSelected = isSelected,
                                    isConnected = isConnected,
                                    onClick = {
                                        if (!isConnected) {
                                            selectedMeaningItem.value = item
                                            
                                            // 如果已经选择了单词，尝试连接
                                            selectedWordItem.value?.let { word ->
                                                connections.add(Connection(word.id, item.id))
                                                selectedWordItem.value = null
                                                selectedMeaningItem.value = null
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // 游戏结束状态
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.8f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "游戏结束",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "你的总得分: ${score.value}",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "完成轮次: ${round.value - 1}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        round.value = 1
                                        score.value = 0
                                        timer.value = 60
                                        gameState.value = GameState.PLAYING
                                        // 通过LaunchedEffect监听round.value的变化来刷新游戏
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text("再来一局")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text("返回")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchItemCard(
    item: MatchItem,
    isSelected: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isConnected -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val textColor = when {
        isConnected -> MaterialTheme.colorScheme.onPrimaryContainer
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(60.dp)
            .clickable(enabled = !isConnected, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected || isConnected) 4.dp else 1.dp
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = if (isConnected || isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun ConnectionLines(
    wordItems: List<MatchItem>,
    meaningItems: List<MatchItem>,
    connections: List<Connection>
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        connections.forEach { connection ->
            val wordIndex = wordItems.indexOfFirst { it.id == connection.wordId }
            val meaningIndex = meaningItems.indexOfFirst { it.id == connection.meaningId }
            
            if (wordIndex >= 0 && meaningIndex >= 0) {
                // 计算左右两边的高度位置
                val wordY = (size.height / (wordItems.size + 1)) * (wordIndex + 1)
                val meaningY = (size.height / (meaningItems.size + 1)) * (meaningIndex + 1)
                
                // 绘制连线
                val word = wordItems[wordIndex]
                val meaning = meaningItems[meaningIndex]
                val isCorrect = word.correctMeaning == meaning.content
                
                // 决定连线颜色
                val lineColor = if (isCorrect) Color.Green else Color.Red
                
                drawLine(
                    color = lineColor,
                    start = Offset(0f, wordY),
                    end = Offset(size.width, meaningY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// 游戏数据类
data class MatchItem(
    val id: Int,
    val content: String,
    val type: ItemType,
    val correctMeaning: String = ""
)

data class Connection(
    val wordId: Int,
    val meaningId: Int
)

enum class ItemType {
    WORD, MEANING
}

enum class GameState {
    PLAYING, FINISHED
} 