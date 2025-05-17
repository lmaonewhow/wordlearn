package com.example.wordlearn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WordbookCard(
    isFirstLaunch: Boolean,
    hasSelectedBook: Boolean,
    bookName: String,
    unitLabel: String,
    progress: Float,
    newWords: Int,
    reviewWords: Int,
    onSelectBookClick: () -> Unit,
    onStudyClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF9F7FC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isFirstLaunch -> {
                    Text("👋 欢迎使用 AI 词书助手", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("首次使用，请先选择一个词书开始学习", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onSelectBookClick,
                        modifier = Modifier.align(Alignment.Start),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63FF),
                            contentColor = Color.White
                        )
                    ) { Text("选择词书") }
                }

                !hasSelectedBook -> {
                    Text("📚 尚未选择词书", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("请先完成词书配置，以便开始学习", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onSelectBookClick,
                        modifier = Modifier.align(Alignment.Start),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("去选择") }
                }

                else -> {
                    Text("📘 当前词书：《$bookName》", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("$unitLabel ｜ 记忆进度 ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🆕 新学：$newWords 词", fontSize = 14.sp)
                        Text("🔁 待复习：$reviewWords 词", fontSize = 14.sp)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onStudyClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("开始学习")
                        }
                        OutlinedButton(
                            onClick = onReviewClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("去复习")
                        }
                    }
                }
            }
        }
    }
}
