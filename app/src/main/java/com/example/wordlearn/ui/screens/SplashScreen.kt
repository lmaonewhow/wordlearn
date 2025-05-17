package com.example.wordlearn.ui.screens

import android.content.Context
import android.net.Uri
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.navigation.NavController
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonDefaults

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SplashScreen(navController: NavController) {
    var isVideoCompleted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 确保在组件销毁时释放资源
    DisposableEffect(Unit) {
        onDispose {
            // 清理资源
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video player
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    // 设置音频流类型
                    setAudioFocusRequest(AudioManager.AUDIOFOCUS_GAIN)
                    
                    // 设置音量
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                        0
                    )
                    
                    setVideoURI(Uri.parse("android.resource://${context.packageName}/raw/splash_video"))
                    setOnPreparedListener { mediaPlayer ->
                        // 设置音量
                        mediaPlayer.setVolume(1f, 1f)
                        
                        // 设置视频尺寸以填充屏幕
                        val videoRatio = mediaPlayer.videoWidth.toFloat() / mediaPlayer.videoHeight.toFloat()
                        val screenRatio = width.toFloat() / height.toFloat()
                        val scale = if (videoRatio > screenRatio) {
                            height.toFloat() / mediaPlayer.videoHeight.toFloat()
                        } else {
                            width.toFloat() / mediaPlayer.videoWidth.toFloat()
                        }
                        
                        layoutParams.width = (mediaPlayer.videoWidth * scale).toInt()
                        layoutParams.height = (mediaPlayer.videoHeight * scale).toInt()
                        requestLayout()
                    }
                    setOnCompletionListener {
                        isVideoCompleted = true
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                    start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Skip button
        TextButton(
            onClick = {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 16.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color.White
            )
        ) {
            Text(
                "跳过",
                color = Color.White
            )
        }
    }
} 