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
import androidx.compose.ui.graphics.Color
import com.example.wordlearn.ui.viewmodel.LearningPlanViewModel
import com.example.wordlearn.ui.theme.ThemeManager
import java.time.DayOfWeek

@Composable
fun DailyGoalCard(viewModel: LearningPlanViewModel) {
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val isDarkMode by ThemeManager.isDarkMode
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "每日学习目标",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color.Black
            )
            
            // 新词数量设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "每日新词数量",
                    color = if (isDarkMode) Color.White else Color.Black
                )
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
                    suffix = { Text("词", color = if (isDarkMode) Color.White else Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = if (isDarkMode) Color.Gray else Color.Black,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                        focusedTextColor = if (isDarkMode) Color.White else Color.Black
                    )
                )
            }
            
            // 复习数量设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "每日复习数量",
                    color = if (isDarkMode) Color.White else Color.Black
                )
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
                    suffix = { Text("词", color = if (isDarkMode) Color.White else Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = if (isDarkMode) Color.Gray else Color.Black,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                        focusedTextColor = if (isDarkMode) Color.White else Color.Black
                    )
                )
            }
            
            // 学习时长设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "每日学习时长",
                    color = if (isDarkMode) Color.White else Color.Black
                )
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
                    suffix = { Text("分钟", color = if (isDarkMode) Color.White else Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = if (isDarkMode) Color.Gray else Color.Black,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                        focusedTextColor = if (isDarkMode) Color.White else Color.Black
                    )
                )
            }
            
            // 学习日选择
            Text(
                "学习日",
                style = MaterialTheme.typography.titleMedium,
                color = if (isDarkMode) Color.White else Color.Black
            )
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
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleActiveDay(day) },
                            label = { 
                                Text(
                                    dayNames[index],
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                           else if (isDarkMode) Color.White
                                           else Color.Black
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                containerColor = if (isDarkMode) Color(0xFF3D3D3D) else Color(0xFFF0F0F0)
                            )
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
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleActiveDay(day) },
                            label = { 
                                Text(
                                    dayNames[index + 4],
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                           else if (isDarkMode) Color.White
                                           else Color.Black
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                containerColor = if (isDarkMode) Color(0xFF3D3D3D) else Color(0xFFF0F0F0)
                            )
                        )
                    }
                    // 添加一个空的 Spacer 来保持对齐
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
} 