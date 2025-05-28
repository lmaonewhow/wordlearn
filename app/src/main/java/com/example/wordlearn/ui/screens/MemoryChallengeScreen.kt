package com.example.wordlearn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

enum class MemoryGameState {
    READY,       // 准备开始
    MEMORIZING,  // 记忆阶段
    RECALLING,   // 回忆阶段
    RESULT       // 结果展示
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryChallengeScreen(
    navController: NavController,
    viewModel: LearningViewModel,
    achievementViewModel: AchievementViewModel
) {
    val scope = rememberCoroutineScope()
    
    // 游戏状态
    val gameState = remember { mutableStateOf(MemoryGameState.READY) }
    val wordsStateFlow = remember { viewModel.getRandomWords(20) }
    val words by wordsStateFlow.collectAsState()
    val memoryWords = remember { mutableStateListOf<MemoryWord>() }
    val selectedWords = remember { mutableStateListOf<MemoryWord>() }
    val roundTime = remember { mutableStateOf(30) } // 记忆时间，单位秒
    val timeLeft = remember { mutableStateOf(30) }
    val level = remember { mutableStateOf(1) } // 游戏级别
    val wordsToMemorize = remember { mutableStateOf(5) } // 初始需要记忆的单词数
    
    // 记录游戏体验成就
    LaunchedEffect(Unit) {
        achievementViewModel.recordGamePlayed("memory_challenge")
    }
    
    // 初始化游戏数据
    LaunchedEffect(words, level.value) {
        // 清空之前的数据
        memoryWords.clear()
        selectedWords.clear()
        
        // 根据级别设置记忆时间和单词数量
        wordsToMemorize.value = 5 + (level.value - 1) * 2 // 每提升一级增加2个单词
        roundTime.value = maxOf(15, 30 - (level.value - 1) * 3) // 每提升一级减少3秒，最少15秒
        timeLeft.value = roundTime.value
        
        // 填充数据
        if (words.isNotEmpty()) {
            // 从随机单词中选择指定数量
            val shuffledWords = words.shuffled().take(wordsToMemorize.value)
            shuffledWords.forEach { word ->
                memoryWords.add(MemoryWord(word.word, word.meaning))
            }
        }
    }
    
    // 倒计时逻辑
    LaunchedEffect(gameState.value) {
        if (gameState.value == MemoryGameState.MEMORIZING) {
            timeLeft.value = roundTime.value
            while (timeLeft.value > 0) {
                delay(1000)
                timeLeft.value--
                
                if (timeLeft.value == 0) {
                    // 时间到，进入回忆阶段
                    gameState.value = MemoryGameState.RECALLING
                    
                    // 创建测试用的选项列表（包含正确答案和干扰项）
                    val correctWords = memoryWords.toList()
                    val allOtherWords = words.filter { word -> 
                        !correctWords.any { it.word == word.word } 
                    }.shuffled()
                    
                    // 生成干扰选项，数量是正确答案的两倍
                    val distractors = allOtherWords.take(correctWords.size * 2).map { 
                        MemoryWord(it.word, it.meaning, false) 
                    }
                    
                    // 合并所有选项并打乱顺序
                    val allOptions = (correctWords + distractors).shuffled()
                    
                    // 清空并重新填充选项
                    memoryWords.clear()
                    memoryWords.addAll(allOptions)
                }
            }
        }
    }
    
    // 检查选择是否完成
    LaunchedEffect(selectedWords.size) {
        if (gameState.value == MemoryGameState.RECALLING && selectedWords.size == wordsToMemorize.value) {
            // 延迟一会儿，让用户看到最后一个选择
            delay(500)
            gameState.value = MemoryGameState.RESULT
        }
    }
    
    // 游戏结果记录成就
    LaunchedEffect(gameState.value) {
        if (gameState.value == MemoryGameState.RESULT) {
            // 计算正确率
            val correctCount = selectedWords.count { it.isCorrect }
            val accuracy = (correctCount.toFloat() / wordsToMemorize.value)
            
            // 记录成就
            achievementViewModel.recordMemoryChallengeResult(level.value, accuracy)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("速记挑战") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 重新开始按钮
                    IconButton(
                        onClick = {
                            gameState.value = MemoryGameState.READY
                        }
                    ) {
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
            when (gameState.value) {
                MemoryGameState.READY -> {
                    // 游戏开始前的准备界面
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "速记挑战",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "游戏规则：\n" +
                            "1. 在限定时间内记忆屏幕上显示的单词\n" +
                            "2. 时间结束后，从混合的单词中找出你刚才看到的单词\n" +
                            "3. 随着级别提升，需要记忆的单词数量会增加，时间会减少\n" +
                            "4. 挑战你的记忆极限！",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 难度选择
                        Text(
                            "选择难度级别",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 1..5) {
                                val isSelected = level.value == i
                                Card(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { level.value = i },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    shape = CircleShape,
                                    border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$i",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) 
                                                MaterialTheme.colorScheme.onPrimary 
                                            else 
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { gameState.value = MemoryGameState.MEMORIZING },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(56.dp)
                        ) {
                            Text(
                                "开始游戏",
                                fontSize = 18.sp
                            )
                        }
                    }
                }
                
                MemoryGameState.MEMORIZING -> {
                    // 记忆阶段
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 倒计时和关卡信息
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 关卡
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
                                        "级别",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "${level.value}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            // 要记忆的单词数
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
                                        "单词数",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "${wordsToMemorize.value}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            
                            // 倒计时
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (timeLeft.value > 10) 
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
                                        color = if (timeLeft.value > 10) 
                                            MaterialTheme.colorScheme.onTertiaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "${timeLeft.value}s",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (timeLeft.value > 10) 
                                            MaterialTheme.colorScheme.onTertiaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        
                        // 记忆提示
                        Text(
                            "记住以下单词！",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        // 单词列表
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(memoryWords.filter { it.isCorrect }) { memoryWord ->
                                MemoryWordCard(memoryWord)
                            }
                        }
                    }
                }
                
                MemoryGameState.RECALLING -> {
                    // 回忆阶段
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 选择提示
                        Text(
                            "选出你刚才看到的 ${wordsToMemorize.value} 个单词",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        // 显示已选择的单词数量
                        Text(
                            "已选择: ${selectedWords.size}/${wordsToMemorize.value}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // 单词网格
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(memoryWords) { memoryWord ->
                                val isSelected = selectedWords.contains(memoryWord)
                                RecallWordCard(
                                    memoryWord = memoryWord,
                                    isSelected = isSelected,
                                    onSelect = {
                                        if (isSelected) {
                                            selectedWords.remove(memoryWord)
                                        } else {
                                            if (selectedWords.size < wordsToMemorize.value) {
                                                selectedWords.add(memoryWord)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                MemoryGameState.RESULT -> {
                    // 结果展示
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 计算正确率
                                val correctCount = selectedWords.count { it.isCorrect }
                                val accuracy = (correctCount.toFloat() / wordsToMemorize.value) * 100
                                
                                Text(
                                    "记忆测试结果",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // 展示准确率
                                Text(
                                    "准确率: ${accuracy.toInt()}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = when {
                                        accuracy >= 80 -> Color.Green
                                        accuracy >= 60 -> Color(0xFFFFA000) // 橙色
                                        else -> Color.Red
                                    }
                                )
                                
                                Text(
                                    "正确识别: $correctCount/${wordsToMemorize.value}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // 显示结果明细
                                Text(
                                    "详细结果",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 正确选择的单词
                                if (selectedWords.any { it.isCorrect }) {
                                    Text(
                                        "正确识别的单词:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Green
                                    )
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        selectedWords.filter { it.isCorrect }.forEach { word ->
                                            ResultWordItem(
                                                word = word.word, 
                                                meaning = word.meaning,
                                                isCorrect = true
                                            )
                                        }
                                    }
                                }
                                
                                // 错误选择的单词
                                if (selectedWords.any { !it.isCorrect }) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "错误选择的单词:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Red
                                    )
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        selectedWords.filter { !it.isCorrect }.forEach { word ->
                                            ResultWordItem(
                                                word = word.word, 
                                                meaning = word.meaning,
                                                isCorrect = false
                                            )
                                        }
                                    }
                                }
                                
                                // 遗漏的单词
                                val missedWords = memoryWords.filter { 
                                    it.isCorrect && !selectedWords.contains(it) 
                                }
                                
                                if (missedWords.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "遗漏的单词:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFFA000) // 橙色
                                    )
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        missedWords.forEach { word ->
                                            ResultWordItem(
                                                word = word.word, 
                                                meaning = word.meaning,
                                                isCorrect = null
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // 操作按钮
                                if (accuracy >= 80 && level.value < 5) {
                                    Button(
                                        onClick = { 
                                            level.value++
                                            gameState.value = MemoryGameState.READY
                                        },
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ) {
                                        Text("挑战下一级")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                
                                Button(
                                    onClick = { gameState.value = MemoryGameState.READY },
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
}

@Composable
fun MemoryWordCard(memoryWord: MemoryWord) {
    Card(
        modifier = Modifier.fillMaxWidth(0.8f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = memoryWord.word,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = memoryWord.meaning,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecallWordCard(
    memoryWord: MemoryWord,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = memoryWord.word,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = memoryWord.meaning,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ResultWordItem(
    word: String,
    meaning: String,
    isCorrect: Boolean?
) {
    val backgroundColor = when(isCorrect) {
        true -> MaterialTheme.colorScheme.primaryContainer
        false -> MaterialTheme.colorScheme.errorContainer
        null -> MaterialTheme.colorScheme.secondaryContainer
    }
    
    val textColor = when(isCorrect) {
        true -> MaterialTheme.colorScheme.onPrimaryContainer
        false -> MaterialTheme.colorScheme.onErrorContainer
        null -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when(isCorrect) {
                true -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                false -> Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                null -> Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = word,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

// 记忆单词数据类
data class MemoryWord(
    val word: String,
    val meaning: String,
    val isCorrect: Boolean = true
) 