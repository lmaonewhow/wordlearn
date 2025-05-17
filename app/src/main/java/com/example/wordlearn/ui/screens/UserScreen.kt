package com.example.wordlearn.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.wordlearn.R


@Composable
fun UserScreen() {
    val context = LocalContext.current

    // 播放音效：只在进入此页面时执行一次
    LaunchedEffect(Unit) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.user)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()  // 播放完后释放资源
        }
    }

    // 你的界面内容
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("用户界面")
        // 其他 UI 元素
    }
}
