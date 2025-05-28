package com.example.wordlearn.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.wordlearn.App
import com.example.wordlearn.data.model.VocabularyBook
import com.example.wordlearn.data.repository.VocabularyRepository
import com.example.wordlearn.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

private const val TAG = "WordbookSelector"

@Composable
fun WordbookSelectorScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    Log.d(TAG, "WordbookSelectorScreen 开始组合")

    val context = LocalContext.current
    val app = context.applicationContext as App
    val vocabularyRepository = remember { VocabularyRepository(context, app.getAppVocabularyDao()) }
    var wordbookList by remember { mutableStateOf<List<VocabularyBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0f) }
    var importMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 添加生命周期效果，记录组件创建和销毁
    DisposableEffect(Unit) {
        Log.d(TAG, "WordbookSelectorScreen 已创建")
        onDispose {
            Log.d(TAG, "WordbookSelectorScreen 已销毁")
        }
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "开始加载词书列表")
        try {
            wordbookList = vocabularyRepository.getAvailableBooksFromAssets()
            Log.d(TAG, "成功加载词书列表，共 ${wordbookList.size} 本词书")
            wordbookList.forEachIndexed { index, book ->
                Log.d(TAG, "词书 #$index: 名称=${book.name}, 路径=${book.filePath}, 类型=${book.type}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载词书列表失败", e)
        } finally {
            isLoading = false
            Log.d(TAG, "词书加载状态已更新: isLoading=$isLoading")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "📚 选择词书",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .padding(bottom = 22.dp)
                .align(Alignment.CenterHorizontally)
        )

        if (isLoading) {
            Log.d(TAG, "显示加载中状态")
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (isImporting) {
            // 导入中动画
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { importProgress },
                        modifier = Modifier.size(60.dp)
                    )
                    Text(
                        text = importMessage,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (wordbookList.isEmpty()) {
            Log.d(TAG, "词书列表为空，显示空状态")
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "没有找到任何词书文件",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Log.d(TAG, "显示词书列表，共 ${wordbookList.size} 项")
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                wordbookList.forEach { book ->
                    ElevatedCard(
                        onClick = {
                            Log.d(TAG, "用户选择了词书: ${book.name}, 路径=${book.filePath}")
                            
                            // 显示导入中状态
                            isImporting = true
                            importMessage = "正在导入词书: ${book.name}"
                            importProgress = 0f
                            
                            scope.launch {
                                try {
                                    // 导入词书单词到数据库
                                    importProgress = 0.2f
                                    importMessage = "正在从资源文件加载单词..."
                                    
                                    val words = vocabularyRepository.importWordsFromBookFile(book)
                                    
                                    importProgress = 0.5f
                                    importMessage = "正在将 ${words.size} 个单词保存到数据库..."
                                    
                                    // 存储单词到数据库
                                    vocabularyRepository.storeWordsInDatabase(words)
                                    
                                    importProgress = 0.8f
                                    importMessage = "正在更新学习状态..."
                                    
                                    // 选择该词书作为当前词书
                                    viewModel.selectWordbook(book.name)
                                    
                                    importProgress = 1f
                                    importMessage = "导入完成，正在切换到学习计划..."
                                    
                                    // 导航到学习计划配置页面
                                    Log.d(TAG, "导航到学习计划配置页面")
                                    navController.navigate("learning_plan_config") {
                                        popUpTo("wordbook_selector") { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "导入词书失败", e)
                                    importMessage = "导入失败: ${e.message}"
                                    // 延迟后恢复到选择界面
                                    kotlinx.coroutines.delay(2000)
                                    isImporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 18.dp)
                                .heightIn(min = 54.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = book.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "点击选择并导入词书",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "选中",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    Log.d(TAG, "WordbookSelectorScreen 组合完成")
}