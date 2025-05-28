package com.example.wordlearn.ui.screens.review

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.wordlearn.App
import com.example.wordlearn.ui.viewmodel.ReviewViewModel
import com.example.wordlearn.ui.viewmodel.ReviewViewModelFactory
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.LearningPlanRepository
import android.widget.Toast
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.util.Log

private const val TAG = "ReviewScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    context: Context = LocalContext.current,
    viewModel: ReviewViewModel = viewModel(
        factory = ReviewViewModelFactory(
            vocabularyRepository = (context.applicationContext as App).vocabularyRepository,
            learningPlanRepository = LearningPlanRepository(context)
        )
    )
) {
    // 状态
    val currentWord by viewModel.currentWord.collectAsState(initial = null)
    val options by viewModel.options.collectAsState(initial = emptyList())
    val isAnswered by viewModel.isAnswered.collectAsState(initial = false)
    val selectedAnswer by viewModel.selectedAnswer.collectAsState(initial = "")
    val progress by viewModel.progress.collectAsState(initial = 0)
    val totalWords by viewModel.totalWords.collectAsState(initial = 0)
    val isEnglishToChinese by viewModel.isEnglishToChinese.collectAsState(initial = true)
    val question by viewModel.question.collectAsState(initial = "")
    val isReviewComplete by viewModel.isReviewComplete.collectAsState(initial = false)
    val message by viewModel.message.collectAsState(initial = null)
    val shouldPlayPronunciation by viewModel.shouldPlayPronunciation.collectAsState(initial = null)

    // 创建MediaPlayer实例
    val mediaPlayer = remember { 
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    // 创建播放函数
    val playPronunciation = remember(mediaPlayer) {
        { word: String ->
            try {
                // 检查网络连接
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo == null || !networkInfo.isConnected) {
                    Log.e(TAG, "无网络连接")
                    return@remember
                }

                // 重置MediaPlayer
                mediaPlayer.reset()
                
                // 构建URL（使用美式发音 type=0）
                val url = "http://dict.youdao.com/dictvoice?type=0&audio=$word"
                Log.d(TAG, "正在播放音频：$url")
                
                mediaPlayer.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    
                    // 设置数据源并播放
                    setDataSource(url)
                    
                    setOnPreparedListener { player ->
                        Log.d(TAG, "音频准备完成，开始播放")
                        player.start()
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "播放音频失败：what=$what, extra=$extra")
                        reset()
                        true
                    }
                    
                    setOnCompletionListener {
                        Log.d(TAG, "音频播放完成")
                        it.reset()
                    }
                    
                    // 异步准备
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "设置音频源时出错", e)
                mediaPlayer.reset()
            }
        }
    }

    // 监听是否需要播放发音
    LaunchedEffect(shouldPlayPronunciation) {
        shouldPlayPronunciation?.let { word ->
            playPronunciation(word)
        }
    }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
                Log.d(TAG, "MediaPlayer资源已释放")
            } catch (e: Exception) {
                Log.e(TAG, "释放MediaPlayer时出错", e)
            }
        }
    }

    // 显示完成提示并自动返回
    LaunchedEffect(isReviewComplete) {
        if (isReviewComplete) {
            // 显示提示
            Toast.makeText(context, message ?: "复习完成", Toast.LENGTH_SHORT).show()
            
            // 刷新主页数据
            val app = context.applicationContext as App
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.homeViewModel?.refreshAfterTaskReturn()
                Log.d(TAG, "复习完成，已触发主页数据刷新")
            }
            
            // 延迟一秒后返回
            kotlinx.coroutines.delay(1000)
            navController.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "复习",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "进度: $progress/$totalWords",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        // 返回前刷新主页数据
                        val app = context.applicationContext as App
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            app.homeViewModel?.refreshAfterTaskReturn()
                        }
                        navController.navigateUp() 
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (totalWords == 0) {
            // 显示空状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "暂无需要复习的单词",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "可能的原因：\n1. 还未选择词书\n2. 所有单词都已经复习完成\n3. 今天的复习任务已完成",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        // 返回前刷新主页数据
                        val app = context.applicationContext as App
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            app.homeViewModel?.refreshAfterTaskReturn()
                        }
                        navController.navigateUp() 
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("返回首页")
                }
            }
        } else {
            // 复习界面内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .padding(bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 单词卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        currentWord?.let { word ->
                            // 显示问题（根据当前模式显示英文或中文）
                            Text(
                                text = question,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 如果是英译中模式且有音标，显示音标
                            if (isEnglishToChinese && (word.ukPhonetic.isNotEmpty() || word.usPhonetic.isNotEmpty())) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    if (word.ukPhonetic.isNotEmpty()) {
                                        Text(
                                            text = "UK: [${word.ukPhonetic}]",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (word.usPhonetic.isNotEmpty()) {
                                        Text(
                                            text = "US: [${word.usPhonetic}]",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // 如果已回答，显示正确答案和例句
                            if (isAnswered) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isEnglishToChinese) word.meaning else word.word,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // 如果有例句，显示例句
                                if (word.example.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = word.example,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 选项列表
                Column(
                    modifier = Modifier.weight(0.5f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { option ->
                        val isCorrect = isCorrect(option, currentWord, isEnglishToChinese)
                        val isSelected = option == selectedAnswer
                        
                        Button(
                            onClick = { 
                                if (!isAnswered) {
                                    viewModel.checkAnswer(option)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    !isAnswered -> MaterialTheme.colorScheme.surface
                                    isCorrect -> MaterialTheme.colorScheme.primaryContainer
                                    isSelected -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                contentColor = when {
                                    !isAnswered -> MaterialTheme.colorScheme.onSurface
                                    isCorrect -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isSelected -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
                
                // 下一个按钮
                NextButton(
                    isVisible = isAnswered && !isCorrect(selectedAnswer, currentWord, isEnglishToChinese),
                    onClick = { viewModel.nextWord() }
                )
            }
        }
    }
}

@Composable
private fun isCorrect(
    selectedAnswer: String,
    currentWord: Word?,
    isEnglishToChinese: Boolean
): Boolean {
    if (currentWord == null) return false
    return if (isEnglishToChinese) {
        selectedAnswer == currentWord.meaning
    } else {
        selectedAnswer == currentWord.word
    }
}

// 下一个按钮（仅在回答错误时显示）
@Composable
fun NextButton(
    isVisible: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("下一个")
        }
    }
}