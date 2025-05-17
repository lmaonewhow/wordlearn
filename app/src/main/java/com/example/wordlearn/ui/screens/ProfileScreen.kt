package com.example.wordlearn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordlearn.data.model.Questions
import com.example.wordlearn.ui.components.QuestionCard
import com.example.wordlearn.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onComplete: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()
    val answers by viewModel.answers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp) // 为底部导航栏留出空间
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("个性化词典配置") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Progress Indicator
        LinearProgressIndicator(
            progress = (currentQuestionIndex + 1).toFloat() / Questions.all.size,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .weight(1f)
        ) {
            // Current Question
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    QuestionCard(
                        question = Questions.all[currentQuestionIndex],
                        onAnswer = { answer ->
                            viewModel.answerQuestion(Questions.all[currentQuestionIndex].id, answer)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Navigation Buttons - 放在内容区域底部
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back Button
                    OutlinedButton(
                        onClick = { viewModel.previousQuestion() },
                        enabled = currentQuestionIndex > 0
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("上一题")
                    }

                    // Next/Complete Button
                    Button(
                        onClick = {
                            if (isComplete) {
                                onComplete()
                            } else if (answers.containsKey(Questions.all[currentQuestionIndex].id)) {
                                viewModel.nextQuestion()
                            }
                        },
                        enabled = answers.containsKey(Questions.all[currentQuestionIndex].id)
                    ) {
                        Text(if (currentQuestionIndex == Questions.all.size - 1) "完成" else "下一题")
                        if (currentQuestionIndex < Questions.all.size - 1) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    // Show completion dialog
    if (isComplete && viewModel.currentProfile != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("配置完成") },
            text = {
                Column {
                    Text("已根据您的回答生成个性化词典配置：")
                    Text("学习目标：${viewModel.currentProfile?.learningGoal}")
                    Text("兴趣方向：${viewModel.currentProfile?.readingInterests?.joinToString()}")
                    Text("当前水平：${viewModel.currentProfile?.proficiencyLevel}")
                    Text("学习方式：${viewModel.currentProfile?.learningStyle}")
                }
            },
            confirmButton = {
                TextButton(onClick = onComplete) {
                    Text("开始学习")
                }
            }
        )
    }
} 