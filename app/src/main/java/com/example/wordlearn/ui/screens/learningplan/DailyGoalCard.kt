package com.example.wordlearn.ui.screens.learningplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.wordlearn.ui.viewmodel.LearningPlanViewModel
import java.time.DayOfWeek

@Composable
fun DailyGoalCard(viewModel: LearningPlanViewModel) {
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "每日学习目标",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // 新词数量设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("每日新词数量")
                var text by remember { mutableStateOf(dailyGoal.newWordsCount.toString()) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        // 只允许输入数字
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            text = newValue
                            newValue.toIntOrNull()?.let { viewModel.updateNewWordsCount(it) }
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    suffix = { Text("词") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            // 复习数量设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("每日复习数量")
                var text by remember { mutableStateOf(dailyGoal.reviewWordsCount.toString()) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        // 只允许输入数字
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            text = newValue
                            newValue.toIntOrNull()?.let { viewModel.updateReviewWordsCount(it) }
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    suffix = { Text("词") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            // 学习时长设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("每日学习时长")
                var text by remember { mutableStateOf(dailyGoal.learningTimeMinutes.toString()) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        // 只允许输入数字
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            text = newValue
                            newValue.toIntOrNull()?.let { viewModel.updateLearningTimeMinutes(it) }
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    suffix = { Text("分钟") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            // 学习日选择
            Text("学习日", style = MaterialTheme.typography.titleMedium)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 第一行：周一到周四
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayOfWeek.values().take(4).forEachIndexed { index, day ->
                        val isSelected = dailyGoal.activeDays.contains(day)
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleActiveDay(day) },
                            label = { 
                                Text(dayNames[index])
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // 第二行：周五到周日
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayOfWeek.values().takeLast(3).forEachIndexed { index, day ->
                        val isSelected = dailyGoal.activeDays.contains(day)
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleActiveDay(day) },
                            label = { 
                                Text(dayNames[index + 4])
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 添加一个空的 Spacer 来保持对齐
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
} 