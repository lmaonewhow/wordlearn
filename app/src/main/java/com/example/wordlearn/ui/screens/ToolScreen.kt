package com.example.wordlearn.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.wordlearn.navigation.BottomNavItem
import com.example.wordlearn.ui.viewmodel.ChatViewModel
import com.example.wordlearn.ui.viewmodel.ToolViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ToolScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: ChatViewModel = viewModel(),
    toolViewModel: ToolViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages = viewModel.messages
    val isLoading by viewModel.isLoading.collectAsState()
    val currentResponse by viewModel.currentResponse.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // 初始化 ChatViewModel
    LaunchedEffect(Unit) {
        Log.d(TAG, "初始化ChatViewModel")
        viewModel.initialize(context)
    }

    // 状态收集
    val isProfileCompleted by toolViewModel.isProfileCompleted.collectAsState()
    
    // 使用一个状态跟踪配置检查是否已完成
    var configCheckDone by remember { mutableStateOf(false) }
    
    // 检查用户是否已完成配置
    LaunchedEffect(key1 = Unit) {
        Log.d(TAG, "启动ToolScreen，开始检查用户配置")
        toolViewModel.checkProfileCompletion(context)
        // 给一点时间让状态流更新
        delay(300)
        configCheckDone = true
    }
    
    // 如果未完成配置，导航到配置页面，但仅当检查完成后才执行
    LaunchedEffect(configCheckDone, isProfileCompleted) {
        Log.d(TAG, "配置检查完成: $configCheckDone, 配置状态: $isProfileCompleted")
        
        if (configCheckDone && !isProfileCompleted) {
            Log.d(TAG, "用户未完成配置，准备导航到配置页面")
            
            // 移动到协程内，确保在主线程上执行
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            Log.d(TAG, "当前路由: $currentRoute")
            
            // 防止在配置页面重复导航
            if (currentRoute != BottomNavItem.Profile.route) {
                Log.d(TAG, "执行导航到配置页面: ${BottomNavItem.Profile.route}")
                navController.navigate(BottomNavItem.Profile.route) {
                    // 保存当前导航状态，设置为通过popUpTo实现，等配置完成后仍能返回到原页面
                    popUpTo(BottomNavItem.Tool.route) { inclusive = true }
                }
            }
        } else if (configCheckDone && isProfileCompleted) {
            Log.d(TAG, "用户已完成配置，不需要导航")
        }
    }

    // 渐变背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFf5f7fa),
                        Color(0xFFc3cfe2)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // 紧凑自定义 AppBar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))
                        ),
                        shape = RectangleShape
                    )
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "AI 单词助手",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
            }

            // 内容区域
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    // 个性化词典配置卡片
                    item {
                        ElevatedCard(
                            onClick = { navController.navigate("profile") },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color(0xFF4CA1AF),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "个性化词典配置",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "根据你的学习目标和兴趣定制专属词典",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // 聊天消息
                    items(messages) { message ->
                        this@Column.AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut()
                        ) {
                            ChatMessageItem(
                                content = message.content,
                                isUser = message.isUser
                            )
                        }
                    }

                    // 显示正在输入的回复
                    if (currentResponse.isNotEmpty()) {
                        item {
                            this@Column.AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut()
                            ) {
                                ChatMessageItem(
                                    content = currentResponse,
                                    isUser = false,
                                    isTyping = true
                                )
                            }
                        }
                    }
                }
            }

            // 输入区域
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        placeholder = { Text("请输入你的问题…") },
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CA1AF),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                scope.launch { keyboardController?.hide() }
                            }
                        },
                        enabled = !isLoading && inputText.isNotBlank(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送", tint = Color(0xFF4CA1AF))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun ChatMessageItem(
    content: String,
    isUser: Boolean,
    isTyping: Boolean = false,
    showAvatar: Boolean = true
) {
    val bubbleShape = RoundedCornerShape(14.dp)

    val avatar: @Composable () -> Unit = {
        if (isUser) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(Color(0xFF4CA1AF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "用户头像",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        } else {
            Box(
                Modifier
                    .size(36.dp)
                    .background(Color(0xFFB6F0C6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "AI头像",
                    tint = Color(0xFF2C3E50),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }

    val bubbleModifier = Modifier
        .background(
            brush = if (isUser) {
                Brush.horizontalGradient(listOf(Color(0xFFB6F0C6), Color(0xFF9BC9FF)))
            } else {
                SolidColor(if (isTyping) Color(0xFFEDEDED) else Color.White)
            },
            shape = bubbleShape
        )
        .border(
            width = 1.dp,
            color = if (isUser) Color(0xFF8FD3F4) else Color(0xFFDFE6E9),
            shape = bubbleShape
        )
        .padding(horizontal = 18.dp, vertical = 12.dp)
        .widthIn(max = 270.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            if (showAvatar) avatar() else Spacer(Modifier.width(36.dp))
            Spacer(Modifier.width(8.dp))
        }

        Box(modifier = bubbleModifier) {
            Text(
                text = if (isTyping) "$content▋" else content,
                fontSize = 16.sp,
                color = if (isUser) Color(0xFF253858) else Color(0xFF222222),
                softWrap = true,
                lineHeight = 20.sp
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            avatar()
        }
    }
}
