package com.example.wordlearn.ui.screens.learning

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import com.example.wordlearn.R
import com.example.wordlearn.ui.viewmodel.LearningViewModel
import com.example.wordlearn.ui.viewmodel.LearningState
import android.util.Log
import com.example.wordlearn.navigation.NavRoute
import com.example.wordlearn.data.model.WordCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.content.SharedPreferences

private const val TAG = "LearningScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    navController: NavController? = null,
    viewModel: LearningViewModel = viewModel()
) {
    Log.d(TAG, "学习界面开始组合")
    
    // 获取状态
    val currentWord by viewModel.currentWord.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val totalWords by viewModel.totalWords.collectAsStateWithLifecycle()
    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
    val learningState by viewModel.learningState.collectAsStateWithLifecycle()
    
    // 获取音效设置
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_settings", Context.MODE_PRIVATE) }
    val isSoundEnabled = remember { 
        mutableStateOf(sharedPrefs.getBoolean("sound_enabled", true))
    }
    
    Log.d(TAG, "当前状态 - 进度：$progress/$totalWords，状态：$learningState，单词：${currentWord?.word}")
    
    // 动画相关状态
    val offsetX = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(1f) }
    val cardRotation = remember { Animatable(0f) }
    val cardScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // 创建音效播放器
    val soundEffectPlayer = remember { 
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
        { word: String, isUK: Boolean ->
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
                
                // 构建URL（type=0为美式发音，type=1为英式发音）
                val url = "http://dict.youdao.com/dictvoice?type=${if (isUK) 1 else 0}&audio=$word"
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
                        when (what) {
                            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> Log.e(TAG, "服务器错误")
                            MediaPlayer.MEDIA_ERROR_IO -> Log.e(TAG, "IO错误")
                            MediaPlayer.MEDIA_ERROR_MALFORMED -> Log.e(TAG, "数据格式错误")
                            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> Log.e(TAG, "不支持的格式")
                            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> Log.e(TAG, "连接超时")
                        }
                        reset()
                        true
                    }
                    
                    setOnCompletionListener {
                        Log.d(TAG, "音频播放完成")
                        it.reset()
                    }
                    
                    // 设置超时
                    setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                            // 如果缓冲时间过长，可以取消播放
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (isPlaying.not()) {
                                    Log.w(TAG, "缓冲超时，取消播放")
                                    reset()
                                }
                            }, 10000) // 10秒超时
    }
                        true
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

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
                
                if (soundEffectPlayer.isPlaying) {
                    soundEffectPlayer.stop()
                }
                soundEffectPlayer.release()
                
                Log.d(TAG, "所有MediaPlayer资源已释放")
            } catch (e: Exception) {
                Log.e(TAG, "释放MediaPlayer时出错", e)
            }
        }
    }

    // 播放音效的函数
    val playSoundEffect = remember(soundEffectPlayer) {
        { isKnown: Boolean ->
            try {
                // 检查音效是否启用
                if (!isSoundEnabled.value) {
                    Log.d(TAG, "音效已禁用，跳过播放")
                    return@remember
                }

                soundEffectPlayer.apply {
                    reset()
                    // 根据滑动方向播放不同音效
                    val soundFile = if (isKnown) {
                        // 向右滑（认识）播放niubi.mp3
                        context.resources.openRawResourceFd(R.raw.niubi)
                    } else {
                        // 向左滑（不认识）播放ngm.mp3
                        context.resources.openRawResourceFd(R.raw.ngm)
                    }
                    setDataSource(soundFile.fileDescriptor, soundFile.startOffset, soundFile.length)
                    soundFile.close()
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放音效时出错", e)
            }
        }
    }

    // 监听音效设置变化
    LaunchedEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "sound_enabled") {
                isSoundEnabled.value = prefs.getBoolean(key, true)
                Log.d(TAG, "音效设置已更改：${isSoundEnabled.value}")
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
    }

    // 对话框状态
    var showNonLearningDayDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 处理错误状态
    LaunchedEffect(learningState) {
        if (learningState is LearningState.Error) {
            Log.e(TAG, "显示错误：${(learningState as LearningState.Error).message}")
            snackbarHostState.showSnackbar(
                message = (learningState as LearningState.Error).message,
                duration = SnackbarDuration.Short
            )
        }
        when (learningState) {
            is LearningState.Error -> {
                val message = (learningState as LearningState.Error).message
                if (message.contains("不是学习日")) {
                    showNonLearningDayDialog = true
                    errorMessage = message
                }
            }
            else -> {}
        }
    }

    // 处理滑动手势
    val handleSwipe: (Boolean) -> Unit = { isKnown ->
        Log.d(TAG, "处理滑动：${if (isKnown) "认识" else "不认识"}")
        // 播放音效
        playSoundEffect(isKnown)
        viewModel.handleWordChoice(isKnown)
        scope.launch {
            // 重置动画
            offsetX.animateTo(0f, spring())
            cardRotation.animateTo(0f, spring())
            cardScale.animateTo(1f, spring())
            cardAlpha.animateTo(1f, spring())
            Log.d(TAG, "动画重置完成")
        }
    }

    // 非学习日对话框
    if (showNonLearningDayDialog) {
        AlertDialog(
            onDismissRequest = { showNonLearningDayDialog = false },
            title = { Text("提示") },
            text = { 
                Text("今天不是设定的学习日，是否仍要继续学习？\n\n坚持学习是好事，但也要注意劳逸结合哦！") 
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showNonLearningDayDialog = false
                        viewModel.overrideLearningDayCheck()
                    }
                ) {
                    Text("继续学习")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showNonLearningDayDialog = false
                        navController?.navigateUp()
                    }
                ) {
                    Text("休息一下")
                }
            }
        )
    }

    // 获取学习进度数据
    val learningStats = viewModel.getLearningStats()
    val totalProgress = progress.toFloat() / totalWords.coerceAtLeast(1)  // 修正进度计算
    val newWordsLearned = learningStats.second
    val reviewNeeded = learningStats.third

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = currentBook?.name ?: "学习",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "进度: ${(totalProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 显示详细进度
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "新学: $newWordsLearned",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "待复习: $reviewNeeded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 进度卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                // 进度条
                LinearProgressIndicator(
                            progress = totalProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 进度详情
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "新学单词",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$newWordsLearned",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "待复习",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$reviewNeeded",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // 单词卡片区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (learningState) {
                        LearningState.Loading -> {
                            // 加载状态
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "正在加载...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        LearningState.Success -> {
                            when {
                                progress >= totalWords -> {
                                    // 学习完成状态
                                    LearningCompletionContent(viewModel)
                                }
                                currentWord == null -> {
                                    // 空状态
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "暂无单词",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                else -> {
                                    // 显示单词卡片
                                    currentWord?.let { word ->
                                    WordCardContent(
                                            word = word,
                                        offsetX = offsetX,
                                        cardAlpha = cardAlpha,
                                        cardRotation = cardRotation,
                                        cardScale = cardScale,
                                            onSwipe = handleSwipe,
                                            playPronunciation = playPronunciation,
                                            context = context
                                    )
                                    }
                                }
                            }
                        }
                        is LearningState.Error -> {
                            // 错误状态显示在Snackbar中，这里可以显示重试按钮
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "加载失败",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { 
                                        currentBook?.let { viewModel.loadVocabularyBook(it) }
                                    }
                                ) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                }

                // 底部提示
                AnimatedVisibility(
                    visible = learningState is LearningState.Success && currentWord != null && progress < totalWords,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                offsetX.value > 50f -> "👉 认识"
                                offsetX.value < -50f -> "不认识 👈"
                                else -> "左右滑动或点击按钮进行选择"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    Log.d(TAG, "学习界面组合开始")
}

@Composable
private fun WordCardContent(
    word: WordCard,
    offsetX: Animatable<Float, AnimationVector1D>,
    cardAlpha: Animatable<Float, AnimationVector1D>,
    cardRotation: Animatable<Float, AnimationVector1D>,
    cardScale: Animatable<Float, AnimationVector1D>,
    onSwipe: (Boolean) -> Unit,
    playPronunciation: (String, Boolean) -> Unit,
    context: Context
) {
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .offset(offsetX.value.dp, 0.dp)
            .graphicsLayer {
                rotationZ = cardRotation.value
                scaleX = cardScale.value
                scaleY = cardScale.value
                alpha = cardAlpha.value
            }
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX.value > 100f) {
                            onSwipe(true)
                        } else if (offsetX.value < -100f) {
                            onSwipe(false)
                        } else {
                            scope.launch {
                                offsetX.animateTo(0f, spring())
                                cardRotation.animateTo(0f, spring())
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f, spring())
                            cardRotation.animateTo(0f, spring())
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                            cardRotation.snapTo(offsetX.value * 0.05f)
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 单词显示
            Text(
                text = word.word,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // 音标显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 美式发音按钮
                PhoneticButton(
                    label = "美式发音",
                    phonetic = word.usPhonetic,
                    onClick = { playPronunciation(word.word, false) }
                )
                // 英式发音按钮
                PhoneticButton(
                    label = "英式发音",
                    phonetic = word.ukPhonetic,
                    onClick = { playPronunciation(word.word, true) }
                )
            }
            
            // 例句显示
            Text(
                text = word.example,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun LearningCompletionContent(viewModel: LearningViewModel) {
    val stats = viewModel.getLearningStats()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "完成",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "本轮学习完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "共学习 ${stats.second} 个单词",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "其中 ${stats.third} 个需要加强记忆",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.resetProgress() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("重新开始")
        }
    }
}

@Composable
private fun PhoneticButton(
    label: String,
    phonetic: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = phonetic,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "播放发音",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
} 