package com.example.wordlearn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import java.util.*

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WordChainScreen(
    navController: NavController,
    viewModel: LearningViewModel,
    achievementViewModel: AchievementViewModel
) {
    val wordsStateFlow = remember { viewModel.getAllWords() }
    val words by wordsStateFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    
    // 游戏状态
    val userInput = remember { mutableStateOf("") }
    val chainWords = remember { mutableStateListOf<ChainWord>() }
    val timeLeft = remember { mutableStateOf(60) } // 60秒倒计时
    val gameActive = remember { mutableStateOf(false) }
    val gameOver = remember { mutableStateOf(false) }
    val score = remember { mutableStateOf(0) }
    val currentLetter = remember { mutableStateOf("") }
    val usedWords = remember { mutableStateListOf<String>() }
    val errorMessage = remember { mutableStateOf("") }
    val showError = remember { mutableStateOf(false) }
    
    // 记录游戏体验成就
    LaunchedEffect(Unit) {
        achievementViewModel.recordGamePlayed("word_chain")
    }
    
    // 获取所有单词的映射，便于快速查找
    val wordsMap = remember(words) {
        words.associateBy { it.word.lowercase() }
    }
    
    // 处理用户输入的单词
    fun processUserInput() {
        val input = userInput.value.trim().lowercase()
        
        // 验证输入
        if (input.isEmpty()) {
            return
        }
        
        // 验证是否以正确的字母开头
        if (currentLetter.value.isNotEmpty() && !input.startsWith(currentLetter.value)) {
            errorMessage.value = "单词必须以 '${currentLetter.value}' 开头"
            showError.value = true
            scope.launch {
                delay(2000)
                showError.value = false
            }
            return
        }
        
        // 验证是否已使用过
        if (usedWords.contains(input)) {
            errorMessage.value = "单词 '$input' 已经使用过了"
            showError.value = true
            scope.launch {
                delay(2000)
                showError.value = false
            }
            return
        }
        
        // 验证是否是有效单词
        if (!wordsMap.containsKey(input)) {
            errorMessage.value = "单词 '$input' 不在词库中"
            showError.value = true
            scope.launch {
                delay(2000)
                showError.value = false
            }
            return
        }
        
        // 添加到链中
        val word = wordsMap[input]
        if (word != null) {
            val lastLetter = input.last().toString()
            val points = input.length * 2 // 简单的得分规则：单词长度 * 2
            
            chainWords.add(ChainWord(
                word = input,
                meaning = word.meaning,
                points = points,
                lastLetter = lastLetter
            ))
            
            // 更新游戏状态
            usedWords.add(input)
            currentLetter.value = lastLetter
            score.value += points
            userInput.value = ""
            
            // 延长时间
            timeLeft.value += minOf(5, input.length) // 根据单词长度延长时间，最多5秒
            
            // 滚动到底部
            scope.launch {
                listState.animateScrollToItem(chainWords.size - 1)
            }
            
            // 记录单词链长度成就
            achievementViewModel.recordWordChainLength(chainWords.size)
        }
    }
    
    // 开始游戏
    fun startGame() {
        // 重置游戏状态
        chainWords.clear()
        usedWords.clear()
        score.value = 0
        timeLeft.value = 60
        gameOver.value = false
        errorMessage.value = ""
        showError.value = false
        
        // 选择一个随机的起始字母
        val alphabet = ('a'..'z').toList()
        currentLetter.value = alphabet[Random().nextInt(alphabet.size)].toString()
        
        // 启动游戏
        gameActive.value = true
        
        // 请求焦点
        scope.launch {
            delay(300)
            focusRequester.requestFocus()
        }
    }
    
    // 倒计时逻辑
    LaunchedEffect(gameActive.value) {
        if (gameActive.value) {
            while (timeLeft.value > 0) {
                delay(1000)
                timeLeft.value--
                
                if (timeLeft.value == 0) {
                    gameActive.value = false
                    gameOver.value = true
                    keyboardController?.hide()
                }
            }
        }
    }
    
    // 游戏结束时记录成就
    LaunchedEffect(gameOver.value) {
        if (gameOver.value && chainWords.isNotEmpty()) {
            // 确保记录最终的单词链长度
            achievementViewModel.recordWordChainLength(chainWords.size)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("单词接龙") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 重新开始按钮
                    IconButton(
                        onClick = { startGame() },
                        enabled = !gameActive.value || gameOver.value
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
            if (!gameActive.value && !gameOver.value) {
                // 游戏开始前的界面
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Loop,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "单词接龙游戏",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "游戏规则：\n" +
                        "1. 每轮需要输入一个以指定字母开头的单词\n" +
                        "2. 下一轮需要以上一个单词的最后一个字母开始\n" +
                        "3. 不能重复使用单词\n" +
                        "4. 单词长度越长，得分越高\n" +
                        "5. 每个有效单词会增加游戏时间",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { startGame() },
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
            } else if (gameActive.value) {
                // 游戏进行中
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 游戏状态栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 得分
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
                        
                        // 当前字母
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
                                    "开始字母",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    currentLetter.value.uppercase(),
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
                    
                    // 错误提示
                    AnimatedVisibility(
                        visible = showError.value,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = errorMessage.value,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // 接龙列表
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chainWords) { chainWord ->
                            ChainWordItem(chainWord)
                        }
                    }
                    
                    // 输入区域
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userInput.value,
                            onValueChange = { userInput.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = { Text("输入单词...") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    processUserInput()
                                }
                            ),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { processUserInput() },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "提交")
                        }
                    }
                }
            } else if (gameOver.value) {
                // 游戏结束界面
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
                                "最终得分: ${score.value}",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "接龙单词: ${chainWords.size}个",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (chainWords.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "单词链",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(chainWords) { chainWord ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = chainWord.word,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "+${chainWord.points}",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { startGame() },
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
fun ChainWordItem(chainWord: ChainWord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = chainWord.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = chainWord.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 显示得分和最后一个字母
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "+${chainWord.points}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chainWord.lastLetter.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

// 游戏数据类
data class ChainWord(
    val word: String,
    val meaning: String,
    val points: Int,
    val lastLetter: String
) 