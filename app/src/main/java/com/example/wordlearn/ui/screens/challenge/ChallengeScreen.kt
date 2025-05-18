package com.example.wordlearn.ui.screens.challenge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordlearn.ui.viewmodel.ChallengeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    isToday: Boolean,
    onBackClick: () -> Unit,
    viewModel: ChallengeViewModel = viewModel()
) {
    // 状态
    val gameState by viewModel.gameState.collectAsState()
    val selectedWord by viewModel.selectedWord.collectAsState()
    val selectedMeaning by viewModel.selectedMeaning.collectAsState()
    val score by viewModel.score.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()

    // 初始化游戏
    LaunchedEffect(Unit) {
        viewModel.initializeGame(isToday)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isToday) "今日挑战" else "昨日挑战") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isGameOver) {
                // 游戏结束界面
                GameOverContent(
                    score = score,
                    totalWords = gameState.size,
                    onPlayAgain = { viewModel.restartGame() }
                )
            } else {
                // 游戏界面
                GameContent(
                    gameState = gameState,
                    selectedWord = selectedWord,
                    selectedMeaning = selectedMeaning,
                    onWordSelected = { word -> viewModel.selectWord(word) },
                    onMeaningSelected = { meaning -> viewModel.selectMeaning(meaning) },
                    viewModel = viewModel
                )
            }

            // 分数显示
            Text(
                text = "得分: $score",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun GameContent(
    gameState: List<Pair<String, String>>,
    selectedWord: String?,
    selectedMeaning: String?,
    onWordSelected: (String) -> Unit,
    onMeaningSelected: (String) -> Unit,
    viewModel: ChallengeViewModel = viewModel()
) {
    // 获取已匹配的单词对
    val matchedPairs by viewModel.matchedPairs.collectAsState()
    
    // 记住释义的随机顺序
    val shuffledMeanings = remember(gameState) {
        gameState.map { it.second }.shuffled()
    }

    // 获取主题颜色
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.surfaceTint
    )

    // 为每个匹配对生成一个独特的颜色
    val pairColors = matchedPairs.mapIndexed { index, pair ->
        pair to colors[index % colors.size]
    }.toMap()
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 单词列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gameState.map { it.first }) { word ->
                val matchedPair = matchedPairs.find { it.first == word }
                WordCard(
                    word = word,
                    isSelected = word == selectedWord,
                    isMatched = matchedPair != null,
                    matchedColor = matchedPair?.let { pairColors[it] },
                    onClick = { onWordSelected(word) }
                )
            }
        }

        // 释义列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shuffledMeanings) { meaning ->
                val matchedPair = matchedPairs.find { it.second == meaning }
                MeaningCard(
                    meaning = meaning,
                    isSelected = meaning == selectedMeaning,
                    isMatched = matchedPair != null,
                    matchedColor = matchedPair?.let { pairColors[it] },
                    onClick = { onMeaningSelected(meaning) }
                )
            }
        }
    }
}

@Composable
private fun WordCard(
    word: String,
    isSelected: Boolean,
    isMatched: Boolean,
    matchedColor: Color? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isMatched, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isMatched && matchedColor != null -> matchedColor.copy(alpha = 0.2f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = when {
                isMatched && matchedColor != null -> matchedColor
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected || isMatched) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun MeaningCard(
    meaning: String,
    isSelected: Boolean,
    isMatched: Boolean,
    matchedColor: Color? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isMatched, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isMatched && matchedColor != null -> matchedColor.copy(alpha = 0.2f)
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Text(
            text = meaning,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = when {
                isMatched && matchedColor != null -> matchedColor
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected || isMatched) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun GameOverContent(
    score: Int,
    totalWords: Int,
    onPlayAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "挑战完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "得分: $score / $totalWords",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("再来一次")
        }
    }
} 