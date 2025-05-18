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

@Composable
fun ReviewSettingsCard(viewModel: LearningPlanViewModel) {
    val reviewSettings by viewModel.reviewSettings.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "复习设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // 复习间隔设置
            Text(
                "复习间隔",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(1, 3, 7, 14, 30).forEach { days ->
                    val isSelected = reviewSettings.intervalDays.contains(days)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateIntervalDays(days) },
                        label = { 
                            Text(
                                "${days}天",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            
            // 正确率要求
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "最低正确率要求",
                    color = MaterialTheme.colorScheme.onSurface
                )
                var text by remember { mutableStateOf((reviewSettings.minCorrectRate * 100).toInt().toString()) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        // 只允许输入数字且不超过100
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toIntOrNull()?.let { it <= 100 } == true)) {
                            text = newValue
                            newValue.toIntOrNull()?.let { viewModel.updateMinCorrectRate(it / 100f) }
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    suffix = { Text("%", color = MaterialTheme.colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
} 