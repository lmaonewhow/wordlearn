package com.example.wordlearn.ui.screens.learningplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordlearn.ui.viewmodel.LearningPlanViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPlanScreen(
    onBackClick: () -> Unit,
    viewModel: LearningPlanViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("学习计划", "学习记录", "成就")
    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 监听保存状态变化
    LaunchedEffect(saveState) {
        when (saveState) {
            is LearningPlanViewModel.SaveState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "设置已保存",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
            is LearningPlanViewModel.SaveState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (saveState as LearningPlanViewModel.SaveState.Error).message,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习计划") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 添加保存按钮
                    IconButton(
                        onClick = { viewModel.saveLearningPlan() },
                        enabled = saveState !is LearningPlanViewModel.SaveState.Saving
                    ) {
                        if (saveState is LearningPlanViewModel.SaveState.Saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "保存",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 标签栏
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)  // 添加权重，确保内容区域可以正确滚动
            ) {
                when (selectedTab) {
                    0 -> LearningPlanContent(viewModel)
                    1 -> LearningHistoryContent(viewModel)
                    2 -> AchievementsContent(viewModel)
                }
            }
        }
    }
}

@Composable
private fun LearningPlanContent(viewModel: LearningPlanViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 80.dp),  // 添加底部边距，避免被导航栏遮挡
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DailyGoalCard(viewModel)
        }
        
        item {
            ReviewSettingsCard(viewModel)
        }
        
        item {
            RemindersCard(viewModel)
        }
        
        item {
            DifficultySettingsCard(viewModel)
        }
        
        // 添加底部空白项，确保最后一个卡片完全可见
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

 