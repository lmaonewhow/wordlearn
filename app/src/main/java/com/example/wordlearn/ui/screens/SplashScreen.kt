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
    
    // 读取视频和音效设置
    val sharedPrefs = remember { 
        context.getSharedPreferences("user_settings", Context.MODE_PRIVATE) 
    }
    val isVideoEnabled = remember { 
        sharedPrefs.getBoolean("video_enabled", true)
    }
    val isSoundEnabled = remember { 
        sharedPrefs.getBoolean("sound_enabled", true)
    }

    // 如果视频被禁用，直接跳转到主页
    LaunchedEffect(isVideoEnabled) {
        if (!isVideoEnabled) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // 只有在视频启用时才显示视频
    if (isVideoEnabled) {
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
                        
                        // 根据音效设置来设置音量
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        if (isSoundEnabled) {
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                                0
                            )
                        } else {
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                0,
                                0
                            )
                        }
                        
                        setVideoURI(Uri.parse("android.resource://${context.packageName}/raw/splash_video"))
                        setOnPreparedListener { mediaPlayer ->
                            // 根据音效设置来设置视频音量
                            mediaPlayer.setVolume(
                                if (isSoundEnabled) 1f else 0f,
                                if (isSoundEnabled) 1f else 0f
                            )
                            
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
} 