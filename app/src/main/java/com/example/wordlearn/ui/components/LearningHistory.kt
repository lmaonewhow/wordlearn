package com.example.wordlearn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wordlearn.data.LearningGoal
import com.example.wordlearn.data.LearningRecord

@Composable
fun LearningHistoryList(
    records: List<LearningRecord>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(records) { record ->
            LearningHistoryCard(record = record)
        }
    }
}

@Composable
fun LearningHistoryCard(
    record: LearningRecord,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 日期和完成情况
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // 完成目标图标
                if (record.completedGoals.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "目标达成",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 学习数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LearningStatItem(
                    icon = Icons.Default.Add,
                    label = "新学单词",
                    value = "${record.newWordsLearned}词"
                )
                LearningStatItem(
                    icon = Icons.Default.Done,
                    label = "复习单词",
                    value = "${record.wordsReviewed}词"
                )
                LearningStatItem(
                    icon = Icons.Default.Star,
                    label = "学习时长",
                    value = "${record.learningTimeMinutes}分钟"
                )
            }
            
            // 正确率
            LinearProgressIndicator(
                progress = { record.correctRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "正确率：${(record.correctRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            
            // 完成的目标
            if (record.completedGoals.isNotEmpty()) {
                Text(
                    text = "完成目标",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    record.completedGoals.forEach { goal ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    when (goal) {
                                        LearningGoal.DAILY_NEW_WORDS -> "新词目标"
                                        LearningGoal.DAILY_REVIEW -> "复习目标"
                                        LearningGoal.DAILY_TIME -> "时长目标"
                                        LearningGoal.WEEKLY_COMPLETION -> "周目标"
                                        LearningGoal.MONTHLY_COMPLETION -> "月目标"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
} 