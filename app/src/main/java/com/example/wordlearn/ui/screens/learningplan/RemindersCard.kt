package com.example.wordlearn.ui.screens.learningplan

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wordlearn.data.LearningReminder
import com.example.wordlearn.ui.viewmodel.LearningPlanViewModel
import java.time.DayOfWeek
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RemindersCard(viewModel: LearningPlanViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    var showTimeDialog by remember { mutableStateOf(false) }
    var showDaysPicker by remember { mutableStateOf(false) }
    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    
    val orderedDays = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )
    
    val currentTime = reminders.firstOrNull()?.time ?: LocalTime.of(9, 0)
    val selectedDays = reminders.flatMap { it.days }.toSet()
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "学习提醒",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            
            // 时间选择卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimeDialog = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%02d:%02d", currentTime.hour, currentTime.minute),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Switch(
                        checked = reminders.any { it.isEnabled },
                        onCheckedChange = { enabled ->
                            reminders.forEach { reminder ->
                                viewModel.updateReminderEnabled(reminder.id, enabled)
                            }
                        }
                    )
                }
            }
            
            // 重复日期选择
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "重复",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        orderedDays.take(4).forEachIndexed { index, day ->
                            val isSelected = selectedDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = { 
                                    val newDays = if (isSelected) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                    reminders.forEach { reminder ->
                                        viewModel.updateReminderDays(reminder.id, newDays)
                                    }
                                    if (reminders.isEmpty()) {
                                        viewModel.addReminder(currentTime, setOf(day))
                                    }
                                },
                                label = { 
                                    Text(
                                        dayNames[index],
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // 第二行：周五到周日
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        orderedDays.takeLast(3).forEachIndexed { index, day ->
                            val isSelected = selectedDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = { 
                                    val newDays = if (isSelected) {
                                        selectedDays - day
            } else {
                                        selectedDays + day
                                    }
                reminders.forEach { reminder ->
                                        viewModel.updateReminderDays(reminder.id, newDays)
                                    }
                                    if (reminders.isEmpty()) {
                                        viewModel.addReminder(currentTime, setOf(day))
                                    }
                                },
                                label = { 
                                    Text(
                                        dayNames[index + 4],
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
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

    if (showTimeDialog) {
        TimePickerDialog(
            initialTime = currentTime,
            onDismiss = { showTimeDialog = false },
            onConfirm = { newTime ->
                if (reminders.isEmpty()) {
                    viewModel.addReminder(newTime, selectedDays.ifEmpty { setOf(DayOfWeek.MONDAY) })
                } else {
                    reminders.forEach { reminder ->
                        viewModel.updateReminderTime(reminder.id, newTime)
                    }
                }
                showTimeDialog = false
            }
        )
    }
}

@Composable
private fun ReminderItem(
    reminder: LearningReminder,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onDaysChange: (Set<DayOfWeek>) -> Unit
) {
    var showTimeDialog by remember { mutableStateOf(false) }
    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // 时间显示和选择
                Text(
                    text = String.format("%02d:%02d", reminder.time.hour, reminder.time.minute),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { showTimeDialog = true }
                )
                
                // 重复日期选择
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.values().forEachIndexed { index, day ->
                        val isSelected = reminder.days.contains(day)
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { 
                                val newDays = if (isSelected) {
                                    reminder.days - day
                                } else {
                                    reminder.days + day
                                }
                                onDaysChange(newDays)
                            },
                            label = { 
                                Text(
                                    dayNames[index],
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "删除提醒")
                }
            }
        }
    }

    if (showTimeDialog) {
        TimePickerDialog(
            initialTime = reminder.time,
            onDismiss = { showTimeDialog = false },
            onConfirm = { 
                onTimeChange(it)
                showTimeDialog = false
            }
        )
    }
}

@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime, Set<DayOfWeek>) -> Unit
) {
    var selectedTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var selectedDays by remember { mutableStateOf(DayOfWeek.values().toSet()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDaysPicker by remember { mutableStateOf(false) }
    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加提醒") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 时间选择
                Text(
                    text = String.format("提醒时间：%02d:%02d", selectedTime.hour, selectedTime.minute),
                    modifier = Modifier.clickable { showTimePicker = true }
                )
                
                // 重复日期选择
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDaysPicker = !showDaysPicker }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "重复",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (selectedDays.isEmpty()) "未选择" 
                                  else selectedDays.sortedBy { it.value }
                                       .joinToString("、") { dayNames[it.value - 1] },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 展开的日期选择器
                    if (showDaysPicker) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayOfWeek.values().forEachIndexed { index, day ->
                        val isSelected = selectedDays.contains(day)
                                FilterChip(
                            selected = isSelected,
                            onClick = { 
                                selectedDays = if (isSelected) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                                    label = { Text(dayNames[index]) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedTime, selectedDays) }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showTimePicker) {
        TimePickerDialog(
            initialTime = selectedTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { 
                selectedTime = it
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 小时选择
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("时")
                        Text(
                            text = selectedHour.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    if (selectedHour > 0) selectedHour-- 
                                    else selectedHour = 23
                                }
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "减少")
                            }
                            IconButton(
                                onClick = { 
                                    if (selectedHour < 23) selectedHour++ 
                                    else selectedHour = 0
                                }
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "增加")
                            }
                        }
                    }
                    
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    
                // 分钟选择
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("分")
                        Text(
                            text = selectedMinute.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    if (selectedMinute > 0) selectedMinute-- 
                                    else selectedMinute = 59
                                }
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "减少")
                            }
                            IconButton(
                                onClick = { 
                                    if (selectedMinute < 59) selectedMinute++ 
                                    else selectedMinute = 0
                                }
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "增加")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime.of(selectedHour, selectedMinute)) }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    format: (Int) -> String = { it.toString() }
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = { 
                if (value < range.last) onValueChange(value + 1)
            }
        ) {
            Icon(Icons.Default.KeyboardArrowUp, "增加")
        }
        
        Text(
            text = format(value),
            style = MaterialTheme.typography.titleLarge
        )
        
        IconButton(
            onClick = { 
                if (value > range.first) onValueChange(value - 1)
            }
        ) {
            Icon(Icons.Default.KeyboardArrowDown, "减少")
        }
    }
} 