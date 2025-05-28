package com.example.wordlearn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.wordlearn.ui.viewmodel.LearningViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.ui.viewmodel.AchievementViewModel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FillBlanksScreen(
    navController: NavController,
    viewModel: LearningViewModel,
    achievementViewModel: AchievementViewModel
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // 游戏状态
    val wordsStateFlow = remember { viewModel.getRandomWords(10) }
    val words by wordsStateFlow.collectAsState()
    val currentWordIndex = remember { mutableStateOf(0) }
    val userInput = remember { mutableStateOf("") }
    val showAnswer = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val score = remember { mutableStateOf(0) }
    val mistakes = remember { mutableStateOf(0) }
    val gameCompleted = remember { mutableStateOf(false) }
    val showHint = remember { mutableStateOf(false) }
    
    // 当前提示的字母索引
    val hintIndex = remember { mutableStateOf(0) }
    
    // 连续正确答题数
    val correctStreak = remember { mutableStateOf(0) }
    
    // 记录游戏体验成就
    LaunchedEffect(Unit) {
        achievementViewModel.recordGamePlayed("fill_blanks")
    }
    
    val currentWord = if (words.isNotEmpty() && currentWordIndex.value < words.size) {
        words[currentWordIndex.value]
    } else null
    
    // 生成带有空白的句子
    val sentenceWithBlanks = currentWord?.let { word ->
        val sentence = word.example ?: "This is ${word.word}."
        sentence.replace(word.word, "_______")
    } ?: ""
    
    // 处理检查答案
    fun checkAnswer() {
        if (currentWord == null) return
        
        if (userInput.value.trim().equals(currentWord.word, ignoreCase = true)) {
            // 正确答案
            score.value++
            correctStreak.value++
            
            // 记录连续正确成就
            achievementViewModel.recordFillBlanksStreak(correctStreak.value)
            
            userInput.value = ""
            showAnswer.value = false
            showHint.value = false
            hintIndex.value = 0
            
            // 移动到下一个单词
            if (currentWordIndex.value < words.size - 1) {
                currentWordIndex.value++
            } else {
                // 游戏完成
                gameCompleted.value = true
            }
        } else {
            // 错误答案
            mistakes.value++
            correctStreak.value = 0 // 重置连续正确
            userInput.value = ""
        }
    }
    
    // 游戏结束时记录成就
    LaunchedEffect(gameCompleted.value) {
        if (gameCompleted.value) {
            // 确保记录最终的连续正确数
            achievementViewModel.recordFillBlanksStreak(correctStreak.value)
        }
    }
    
    // 显示下一个提示字母
    fun showNextHint() {
        if (currentWord == null) return
        
        if (hintIndex.value < currentWord.word.length) {
            val currentInput = userInput.value
            val wordToGuess = currentWord.word
            
            // 确保用户输入至少与当前提示字母一样长
            val newInput = if (currentInput.length <= hintIndex.value) {
                currentInput.padEnd(hintIndex.value + 1, '_')
            } else {
                currentInput
            }
            
            // 替换当前提示位置的字母
            val charArray = newInput.toCharArray()
            if (hintIndex.value < charArray.size) {
                charArray[hintIndex.value] = wordToGuess[hintIndex.value]
            }
            
            userInput.value = String(charArray)
            hintIndex.value++
            
            // 如果已经显示了所有字母，就直接显示答案
            if (hintIndex.value >= wordToGuess.length) {
                showAnswer.value = true
            }
        }
    }
    
    // 重置游戏
    fun resetGame() {
        currentWordIndex.value = 0
        userInput.value = ""
        showAnswer.value = false
        score.value = 0
        mistakes.value = 0
        gameCompleted.value = false
        showHint.value = false
        hintIndex.value = 0
    }
    
    // 自动聚焦到输入框
    LaunchedEffect(currentWordIndex.value) {
        delay(300) // 短暂延迟确保UI已更新
        focusRequester.requestFocus()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("单词填空") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 重新开始按钮
                    IconButton(onClick = { resetGame() }) {
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
            if (!gameCompleted.value) {
                // 游戏进行中
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 游戏状态栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 进度
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
                                    "进度",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "${currentWordIndex.value + 1}/${words.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        // 得分
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
                                    "得分",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "${score.value}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        
                        // 错误
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "错误",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "${mistakes.value}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    // 单词定义
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "单词含义",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentWord?.meaning ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // 例句区域
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "例句",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = sentenceWithBlanks,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp
                            )
                        }
                    }
                    
                    // 输入区域
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 输入框
                        BasicTextField(
                            value = userInput.value,
                            onValueChange = { 
                                userInput.value = it
                                showHint.value = false 
                            },
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    checkAnswer()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(56.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .focusRequester(focusRequester)
                        ) { innerTextField ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                innerTextField()
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 操作按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 提示按钮
                            Button(
                                onClick = { showNextHint() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = "提示")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("提示")
                            }
                            
                            // 检查按钮
                            Button(
                                onClick = { checkAnswer() },
                                enabled = userInput.value.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "检查")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("检查")
                            }
                            
                            // 跳过按钮
                            Button(
                                onClick = { 
                                    showAnswer.value = true
                                    mistakes.value++
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "跳过")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("跳过")
                            }
                        }
                    }
                    
                    // 显示答案
                    AnimatedVisibility(
                        visible = showAnswer.value,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "正确答案",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentWord?.word ?: "",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        userInput.value = ""
                                        showAnswer.value = false
                                        showHint.value = false
                                        hintIndex.value = 0
                                        
                                        // 移动到下一个单词
                                        if (currentWordIndex.value < words.size - 1) {
                                            currentWordIndex.value++
                                        } else {
                                            // 游戏完成
                                            gameCompleted.value = true
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "下一个")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("下一个")
                                }
                            }
                        }
                    }
                }
            } else {
                // 游戏完成状态
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
                                "游戏完成！",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "你的得分: ${score.value}/${words.size}",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "错误次数: ${mistakes.value}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            val accuracy = if (score.value + mistakes.value > 0) {
                                (score.value.toFloat() / (score.value + mistakes.value) * 100).toInt()
                            } else 0
                            
                            Text(
                                "正确率: ${accuracy}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = when {
                                    accuracy >= 80 -> Color.Green
                                    accuracy >= 60 -> Color(0xFFFFA000) // 橙色
                                    else -> Color.Red
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { resetGame() },
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