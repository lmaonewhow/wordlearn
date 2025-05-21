package com.example.wordlearn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.ui.viewmodel.FavoriteWordsViewModel

/**
 * 收藏单词屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteWordsScreen(
    onBackClick: () -> Unit,
    viewModel: FavoriteWordsViewModel = viewModel()
) {
    val favoriteWords by viewModel.favoriteWords.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // 加载收藏单词
    LaunchedEffect(Unit) {
        viewModel.loadFavoriteWords()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                // 加载中状态
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (favoriteWords.isEmpty()) {
                // 空状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "还没有收藏的单词",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "学习时点击收藏按钮添加到收藏本",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 收藏列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favoriteWords) { word ->
                        FavoriteWordItem(
                            word = word,
                            onRemoveFavorite = { viewModel.toggleFavorite(word.id, false) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 收藏单词项
 */
@Composable
fun FavoriteWordItem(
    word: Word,
    onRemoveFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 单词和释义
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleLarge
                )
                if (word.ukPhonetic.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "/${word.ukPhonetic}/",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 删除按钮
            IconButton(
                onClick = onRemoveFavorite
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "取消收藏",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
} 