package com.example.wordlearn.ui.screens.learningplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wordlearn.data.DifficultyLevel
import com.example.wordlearn.ui.viewmodel.LearningPlanViewModel

@Composable
fun DifficultySettingsCard(viewModel: LearningPlanViewModel) {
    val selectedDifficulty by viewModel.difficultyLevel.collectAsState()
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "难度设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // 难度选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DifficultyLevel.values().forEach { level ->
                    FilterChip(
                        selected = level == selectedDifficulty,
                        onClick = { viewModel.updateDifficultyLevel(level) },
                        label = { 
                            Text(when(level) {
                                DifficultyLevel.EASY -> "简单"
                                DifficultyLevel.NORMAL -> "正常"
                                DifficultyLevel.HARD -> "困难"
                                DifficultyLevel.ADAPTIVE -> "自适应"
                            })
                        }
                    )
                }
            }
        }
    }
} 