package com.example.wordlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordlearn.data.Word
import com.example.wordlearn.data.WordBook
import com.example.wordlearn.data.LearningStatus

@Composable
fun WordBookDetailScreen(
    wordBook: WordBook,
    onBackClick: () -> Unit,
    onStartLearning: (WordBook, LearningMode) -> Unit,
    onChangeWordBook: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("词汇列表", "学习模式", "学习记录")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部进度条和信息
        TopProgressSection(
            wordBook = wordBook,
            onChangeWordBook = onChangeWordBook
        )

        // 标签栏
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> WordListContent(wordBook.words)
            1 -> LearningModeContent(wordBook, onStartLearning)
            2 -> LearningHistoryContent(wordBook)
        }
    }
}

@Composable
private fun TopProgressSection(
    wordBook: WordBook,
    onChangeWordBook: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行：包含词书标题和更换按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wordBook.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // 更换词书按钮
                TextButton(
                    onClick = onChangeWordBook,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "更换词书",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("更换词书")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 统计信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "总词数",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${wordBook.totalWords}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "已掌握",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${(wordBook.progress * wordBook.totalWords).toInt()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "学习进度",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${(wordBook.progress * 100).toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { wordBook.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WordListContent(words: List<Word>) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(WordFilter.ALL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 搜索栏
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索单词") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索"
                )
            }
        )

        // 筛选选项
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            WordFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // 单词列表
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(words.filter {
                it.word.contains(searchQuery, ignoreCase = true) &&
                when (selectedFilter) {
                    WordFilter.ALL -> true
                    WordFilter.NOT_STARTED -> it.learningStatus == LearningStatus.NOT_STARTED
                    WordFilter.LEARNING -> it.learningStatus == LearningStatus.LEARNING
                    WordFilter.REVIEWING -> it.learningStatus == LearningStatus.REVIEWING
                    WordFilter.MASTERED -> it.learningStatus == LearningStatus.MASTERED
                }
            }) { word ->
                WordItem(word = word)
            }
        }
    }
}

@Composable
private fun WordItem(word: Word) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* 打开单词详情 */ },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = word.word,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = word.phonetic,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = word.definitions.firstOrNull()?.meaning ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            when (word.learningStatus) {
                LearningStatus.NOT_STARTED -> Button(
                    onClick = { /* 开始学习 */ }
                ) {
                    Text("学习")
                }
                LearningStatus.LEARNING,
                LearningStatus.REVIEWING -> Button(
                    onClick = { /* 继续学习 */ }
                ) {
                    Text("复习")
                }
                LearningStatus.MASTERED -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已掌握",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LearningModeContent(
    wordBook: WordBook,
    onStartLearning: (WordBook, LearningMode) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(LearningMode.values()) { mode ->
            LearningModeCard(
                mode = mode,
                onClick = { onStartLearning(wordBook, mode) }
            )
        }
    }
}

@Composable
private fun LearningModeCard(
    mode: LearningMode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = mode.title,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = mode.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = mode.description,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "开始学习",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LearningHistoryContent(wordBook: WordBook) {
    // TODO: 实现学习历史记录展示
}

enum class WordFilter(val label: String) {
    ALL("全部"),
    NOT_STARTED("未学习"),
    LEARNING("学习中"),
    REVIEWING("复习中"),
    MASTERED("已掌握")
}

enum class LearningMode(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    CARD(
        "卡片模式",
        "通过卡片正反面展示单词和释义",
        Icons.Filled.Menu
    ),
    LISTENING(
        "听力练习",
        "听发音选择或输入正确单词",
        Icons.Filled.PlayArrow
    ),
    SPELLING(
        "拼写模式",
        "根据释义和发音拼写单词",
        Icons.Filled.Edit
    ),
    TEST(
        "测试模式",
        "多种题型测试掌握程度",
        Icons.Filled.Star
    )
} 