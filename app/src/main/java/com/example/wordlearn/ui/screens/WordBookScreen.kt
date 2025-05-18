package com.example.wordlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordlearn.data.WordBook
import com.example.wordlearn.data.WordBookType

@Composable
fun WordBookScreen(
    onWordBookClick: (WordBook) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("我的词书", "选择词书")

    // 模拟词书数据
    val myWordBooks = remember {
        listOf(
            WordBook(
                id = "1",
                title = "20天背完四级核心词汇",
                description = "点击开始背单词",
                totalWords = 2000,
                type = WordBookType.FOUR_LEVEL,
                progress = 0.3f
            ),
            WordBook(
                id = "2",
                title = "20天背完高考核心词汇",
                description = "点击开始背单词",
                totalWords = 3500,
                type = WordBookType.HIGH_SCHOOL,
                progress = 0.5f
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部标签栏
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
            0 -> MyWordBooksContent(
                wordBooks = myWordBooks,
                onWordBookClick = onWordBookClick
            )
            1 -> AvailableWordBooksContent(
                onWordBookClick = onWordBookClick
            )
        }
    }
}

@Composable
private fun MyWordBooksContent(
    wordBooks: List<WordBook>,
    onWordBookClick: (WordBook) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(wordBooks) { wordBook ->
            WordBookCard(
                wordBook = wordBook,
                onClick = { onWordBookClick(wordBook) }
            )
        }
    }
}

@Composable
private fun AvailableWordBooksContent(
    onWordBookClick: (WordBook) -> Unit
) {
    // 模拟可选词书数据
    val availableWordBooks = remember {
        listOf(
            WordBook(
                id = "3",
                title = "24天突破高考大纲词汇3500",
                description = "点击开始背单词",
                totalWords = 3500,
                type = WordBookType.HIGH_SCHOOL
            ),
            WordBook(
                id = "4",
                title = "20天背完六级核心词汇",
                description = "点击开始背单词",
                totalWords = 2500,
                type = WordBookType.SIX_LEVEL
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(availableWordBooks) { wordBook ->
            WordBookCard(
                wordBook = wordBook,
                onClick = { onWordBookClick(wordBook) }
            )
        }
    }
}

@Composable
private fun WordBookCard(
    wordBook: WordBook,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = wordBook.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = wordBook.description,
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "总词数：${wordBook.totalWords}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                if (wordBook.progress > 0) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "学习进度：${(wordBook.progress * 100).toInt()}%",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { wordBook.progress },
                            modifier = Modifier.width(100.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "开始学习",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
} 