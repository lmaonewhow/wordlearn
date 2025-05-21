package com.example.wordlearn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordlearn.data.LearningPlanRepository
import kotlin.math.min

@Composable
fun WordbookCard(
    isFirstLaunch: Boolean,
    hasSelectedBook: Boolean,
    bookName: String,
    unitLabel: String,
    progress: Float,
    newWords: Int,
    reviewWords: Int,
    totalWords: Int,
    learnedWords: Int,
    onSelectBookClick: () -> Unit,
    onStudyClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    // 从LearningPlanViewModel获取用户设置的每日学习目标
    val context = LocalContext.current
    val learningPlanRepository = remember { LearningPlanRepository(context) }
    val learningPlanFlow = remember { learningPlanRepository.learningPlan }
    var dailyNewWordsTarget by remember { mutableStateOf(10) } // 默认值
    var dailyReviewWordsTarget by remember { mutableStateOf(20) } // 默认值
    
    // 监听学习计划变化，获取用户设置的目标
    LaunchedEffect(Unit) {
        learningPlanFlow.collect { plan ->
            plan?.let {
                dailyNewWordsTarget = it.dailyGoal.newWordsCount
                dailyReviewWordsTarget = it.dailyGoal.reviewWordsCount
            }
        }
    }
    
    // 记录上次值，用于检测变化
    val previousNewWords = remember { mutableStateOf(newWords) }
    val previousReviewWords = remember { mutableStateOf(reviewWords) }
    
    // 检测值变化并记录日志，帮助调试
    LaunchedEffect(newWords, reviewWords) {
        if (previousNewWords.value != newWords) {
            android.util.Log.d("WordbookCard", "待学习数量已更新: ${previousNewWords.value} -> $newWords")
            previousNewWords.value = newWords
        }
        if (previousReviewWords.value != reviewWords) {
            android.util.Log.d("WordbookCard", "待复习数量已更新: ${previousReviewWords.value} -> $reviewWords")
            previousReviewWords.value = reviewWords
        }
    }
    
    // 今日待学习的单词（每日目标与剩余单词数的较小值）
    val todayLearningWords by remember(newWords, totalWords, learnedWords, dailyNewWordsTarget) { 
        mutableStateOf(min(dailyNewWordsTarget, totalWords - learnedWords).coerceAtLeast(0))
    }
    
    // 今日待复习单词数（不超过设置的每日复习目标）
    val todayReviewWords by remember(reviewWords, dailyReviewWordsTarget) { 
        mutableStateOf(min(reviewWords, dailyReviewWordsTarget))
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isFirstLaunch -> {
                    Text("👋 欢迎使用 AI 词书助手", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("首次使用，请先选择一个词书开始学习", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onSelectBookClick,
                        modifier = Modifier.align(Alignment.Start),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63FF),
                            contentColor = Color.White
                        )
                    ) { Text("选择词书") }
                }

                !hasSelectedBook -> {
                    Text("📚 尚未选择词书", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("请先完成词书配置，以便开始学习", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onSelectBookClick,
                        modifier = Modifier.align(Alignment.Start),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("去选择") }
                }

                else -> {
                    // 顶部标题和更换按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bookName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        // 更换词书按钮
                        TextButton(
                            onClick = onSelectBookClick,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text("更换")
                        }
                    }
                    
                    // Unit信息和进度
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "$unitLabel | 记忆进度 ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 进度条
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF3F51B5),
                        trackColor = Color(0xFFE0E0E0)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 学习数据展示区
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 左侧"待学习"框 - 使用今日待学习数量
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$todayLearningWords",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp
                                ),
                                color = Color(0xFF3F51B5),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "待学习",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 右侧"待复习"框 - 使用今日待复习数量
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$todayReviewWords",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp
                                ),
                                color = Color(0xFF3F51B5),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "待复习",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 按钮区域
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 开始学习按钮
                        OutlinedButton(
                            onClick = onStudyClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF3F51B5))
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF3F51B5)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("开始学习", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        // 去复习按钮
                        OutlinedButton(
                            onClick = onReviewClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            enabled = todayReviewWords > 0,
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (todayReviewWords > 0) Color(0xFFE57373) else Color.Gray
                                )
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (todayReviewWords > 0) Color(0xFFE57373) else Color.Gray
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("去复习", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
} 