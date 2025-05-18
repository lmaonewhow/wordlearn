package com.example.wordlearn.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordlearn.R

@Composable
fun UserScreen() {
    val context = LocalContext.current

    // 播放音效：只在页面进入时播放一次
    LaunchedEffect(Unit) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.user)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }

    // 用户信息界面
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF2F2F7)), // 背景色
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 用户头像
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "用户头像",
            modifier = Modifier
                .size(100.dp)
                .padding(8.dp),
            contentScale = ContentScale.Crop
        )

        // 昵称
        Text(
            text = "你好，学习者！",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 功能卡片示例
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("今日打卡", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("你已经连续学习 5 天，继续保持！", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("我的词书", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("当前词书：四级核心词汇", color = Color.Gray)
            }
        }
    }
}
