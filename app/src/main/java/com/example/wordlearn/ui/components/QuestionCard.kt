package com.example.wordlearn.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordlearn.data.model.ProfileQuestion
import com.example.wordlearn.data.model.QuestionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionCard(
    question: ProfileQuestion<*>,
    onAnswer: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var selectedOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var textInput by remember { mutableStateOf("") }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (question.type) {
                QuestionType.SINGLE_CHOICE -> {
                    (question as ProfileQuestion.SingleChoice).options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption == option,
                                onClick = {
                                    selectedOption = option
                                    onAnswer(option)
                                }
                            )
                            Text(
                                text = option,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                QuestionType.MULTI_CHOICE -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items((question as ProfileQuestion.MultiChoice).options) { option ->
                            FilterChip(
                                selected = selectedOptions.contains(option),
                                onClick = {
                                    selectedOptions = if (selectedOptions.contains(option)) {
                                        selectedOptions - option
                                    } else {
                                        selectedOptions + option
                                    }
                                    onAnswer(selectedOptions)
                                },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                QuestionType.TEXT_INPUT -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { 
                            textInput = it
                            onAnswer(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text((question as ProfileQuestion.TextInput).hint) }
                    )
                }
            }
        }
    }
} 