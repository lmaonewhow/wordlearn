package com.example.wordlearn.ui.screens.learningplan

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordlearn.ui.viewmodel.LearningPlanViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPlanConfigScreen(
    onComplete: () -> Unit,
    viewModel: LearningPlanViewModel
) {
    val scrollState = rememberScrollState()
    val configCompleted = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 监听配置完成状态
    LaunchedEffect(configCompleted.value) {
        if (configCompleted.value) {
            snackbarHostState.showSnackbar(
                message = "学习计划已设置，即将开始学习之旅...",
                duration = SnackbarDuration.Short
            )
            delay(1500) // 延迟1.5秒后回到主界面
            onComplete()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设置学习计划", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.saveLearningPlan() 
                        configCompleted.value = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 80.dp) // 增加底部间距确保不被导航栏遮挡
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "完成配置"
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "配置您的学习计划",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "请设置您的学习目标和复习计划，这将帮助您更有效地学习和记忆单词",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 每日学习目标设置
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "每日学习目标",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 复用DailyGoalCard的内容，但简化版
                        DailyNewWordsSlider(viewModel)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 复习设置
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "复习计划",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 简化版的复习设置
                        SimpleReviewSettings(viewModel)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 提醒设置
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "学习提醒",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 简化版的提醒设置
                        SimpleReminderSettings(viewModel)
                    }
                }
                
                // 为FAB和底部导航栏腾出足够空间
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
        
        // 添加一个大号保存按钮在底部，避免被导航栏遮挡
        Button(
            onClick = {
                viewModel.saveLearningPlan() 
                configCompleted.value = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 64.dp), // 避免被导航栏遮挡
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "保存学习计划",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun DailyNewWordsSlider(viewModel: LearningPlanViewModel) {
    val dailyNewWords by viewModel.dailyNewWords.collectAsState()
    
    Column {
        Text(
            text = "每天学习 $dailyNewWords 个新单词",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = dailyNewWords.toFloat(),
            onValueChange = { viewModel.updateDailyNewWords(it.toInt()) },
            valueRange = 5f..50f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("5", fontSize = 12.sp)
            Text("50", fontSize = 12.sp)
        }
    }
}

@Composable
private fun SimpleReviewSettings(viewModel: LearningPlanViewModel) {
    val useSpacedRepetition by viewModel.useSpacedRepetition.collectAsState()
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = useSpacedRepetition,
                onCheckedChange = { viewModel.updateUseSpacedRepetition(it) }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "启用间隔重复学习",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        if (useSpacedRepetition) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "系统将根据艾宾浩斯记忆曲线，在以下天数后提醒您复习：1天、2天、4天、7天、15天、30天",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "您可以随时进入复习页面手动复习单词",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimpleReminderSettings(viewModel: LearningPlanViewModel) {
    val enableReminders by viewModel.enableReminders.collectAsState()
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = enableReminders,
                onCheckedChange = { viewModel.updateEnableReminders(it) }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "每日学习提醒",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        if (enableReminders) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "系统将在每天固定时间提醒您学习，您可以在学习计划页面修改具体时间",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = true,
                    onCheckedChange = null,
                    enabled = false
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "默认提醒时间: 20:00",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 