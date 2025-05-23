package com.example.wordlearn.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordlearn.data.model.Questions
import com.example.wordlearn.data.store.settingsDataStore
import com.example.wordlearn.ui.components.QuestionCard
import com.example.wordlearn.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.first

private const val TAG = "ProfileScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onComplete: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()
    val answers by viewModel.answers.collectAsState()

    // 初始化 ViewModel
    LaunchedEffect(Unit) {
        Log.d(TAG, "初始化ProfileViewModel")
        viewModel.initialize(context)
    }

    // 当问卷完成时，保存用户配置并跳转
    LaunchedEffect(isComplete) {
        if (isComplete && viewModel.currentProfile != null) {
            Log.d(TAG, "问卷完成，开始保存用户配置")
            // 保存用户配置
            viewModel.saveUserProfile(context, viewModel.currentProfile!!)
            
            // 验证保存是否成功
            try {
                val preferences = context.settingsDataStore.data.first()
                val isProfileCompleted = preferences[com.example.wordlearn.data.store.AppSettingsKeys.IS_PROFILE_COMPLETED] ?: false
                val profileJson = preferences[com.example.wordlearn.data.store.AppSettingsKeys.PROFILE_JSON]
                
                Log.d(TAG, "验证保存结果 - 完成状态: $isProfileCompleted, 配置JSON存在: ${profileJson != null}")
                
                if (isProfileCompleted) {
                    Log.d(TAG, "保存成功，准备跳转")
                    // 跳转到助手页面
                    onComplete()
                } else {
                    Log.e(TAG, "保存失败，完成状态为false")
                }
            } catch (e: Exception) {
                Log.e(TAG, "验证保存结果时发生异常", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "个性化词典配置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    // 修改返回按钮的处理逻辑，只有当问卷已完成时才直接返回
                    IconButton(onClick = { 
                        if (isComplete && viewModel.currentProfile != null) {
                            onComplete()
                        } else {
                            // 如果未完成，提示用户
                            viewModel.showIncompleteWarning = true
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 80.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Progress Indicator
            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1).toFloat() / Questions.all.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // 问题区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                AnimatedContent(
                    targetState = currentQuestionIndex,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) + slideInHorizontally() togetherWith
                                fadeOut(animationSpec = tween(300)) + slideOutHorizontally()
                    }
                ) { targetIndex ->
                    QuestionCard(
                        question = Questions.all[targetIndex],
                        onAnswer = { answer ->
                            viewModel.answerQuestion(Questions.all[targetIndex].id, answer)
                        }
                    )
                }
            }

            // 按钮卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.previousQuestion() },
                        enabled = currentQuestionIndex > 0,
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("上一题")
                    }
                    Button(
                        onClick = {
                            if (isComplete) onComplete()
                            else if (answers.containsKey(Questions.all[currentQuestionIndex].id)) viewModel.nextQuestion()
                        },
                        enabled = answers.containsKey(Questions.all[currentQuestionIndex].id),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(if (currentQuestionIndex == Questions.all.size - 1) "完成" else "下一题")
                        if (currentQuestionIndex < Questions.all.size - 1) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "下一步",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
        // 完成弹窗
        if (isComplete && viewModel.currentProfile != null) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("配置完成") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("已根据您的回答生成个性化词典配置：", style = MaterialTheme.typography.bodyLarge)
                        Divider()
                        Text("• 学习目标：${viewModel.currentProfile?.learningGoal}")
                        Text("• 兴趣方向：${viewModel.currentProfile?.readingInterests?.joinToString()}")
                        Text("• 当前水平：${viewModel.currentProfile?.proficiencyLevel}")
                        Text("• 学习方式：${viewModel.currentProfile?.learningStyle}")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onComplete,
                        shape = RoundedCornerShape(50)
                    ) { Text("开始学习") }
                }
            )
        }
        
        // 未完成提示弹窗
        if (viewModel.showIncompleteWarning) {
            AlertDialog(
                onDismissRequest = { viewModel.showIncompleteWarning = false },
                title = { Text("提示") },
                text = { Text("请完成所有问题后再继续。您的个性化词典配置还未完成。") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.showIncompleteWarning = false },
                        shape = RoundedCornerShape(50)
                    ) { Text("继续完成") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { 
                            viewModel.showIncompleteWarning = false
                            onComplete() 
                        },
                        shape = RoundedCornerShape(50)
                    ) { Text("仍然离开") }
                }
            )
        }
    }
}
