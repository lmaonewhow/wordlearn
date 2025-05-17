package com.example.wordlearn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.wordlearn.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: ChatViewModel = viewModel()
) {
    var inputText by remember { mutableStateOf("") }
    val coroutine = rememberCoroutineScope()
    val kb = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isLoading by viewModel.isLoading.collectAsState()
    val currentResponse by viewModel.currentResponse.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()
    ) {
        CenterAlignedTopAppBar(
            title = { Text("AI 单词助手") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFF2C3E50),
                titleContentColor = Color.White
            )
        )

        // 工具卡片列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 个性化词典配置卡片
            item {
                ElevatedCard(
                    onClick = { navController.navigate("profile") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "个性化词典配置",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "根据你的学习目标和兴趣定制专属词典",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 聊天消息
            items(viewModel.messages) { message ->
                ChatMessageItem(message.content, message.isUser)
            }

            // 显示正在输入的回复
            if (currentResponse.isNotEmpty()) {
                item {
                    ChatMessageItem(currentResponse, false, true)
                }
            }
        }

        // 输入区域
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("请输入你的问题…") },
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            coroutine.launch { kb?.hide() }
                        }
                    },
                    enabled = !isLoading && inputText.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "发送")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    content: String,
    isUser: Boolean,
    isTyping: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when {
                        isUser -> Color(0xFFDCF8C6)
                        isTyping -> Color(0xFFE8E8E8)
                        else -> Color.White
                    }
                )
                .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = if (isTyping) "$content▋" else content,
                fontSize = 16.sp
            )
        }
    }
}

