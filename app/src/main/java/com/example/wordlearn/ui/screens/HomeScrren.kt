package com.example.wordlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.wordapp.viewmodel.HomeViewModel
import com.example.wordlearn.ui.components.WordbookCard

@Composable
fun HomeScreen(navController: NavController,innerPadding: PaddingValues, viewModel: HomeViewModel = viewModel()) {
    val username by viewModel.username.collectAsState()
    val remembered by viewModel.rememberedWords.collectAsState()
    val forgotten by viewModel.forgottenWords.collectAsState()
    val newWords by viewModel.newWords.collectAsState()
    val reviewWords by viewModel.reviewWords.collectAsState()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val hasSelectedBook by viewModel.hasSelectedBook.collectAsState()
    val bookName by viewModel.selectedBookName.collectAsState()
    val unitLabel by viewModel.currentUnit.collectAsState()
    val progress by viewModel.progress.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding( WindowInsets.statusBars.asPaddingValues())
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TopFeatureBar(navController = navController,  modifier = Modifier.widthIn(max = 225.dp)) // 控制左边实际宽度)

                Spacer(modifier = Modifier.width(12.dp)) // 可选：分隔左/右区域

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Hi, $username 👋", style = MaterialTheme.typography.headlineSmall)
                    Text("记忆 $remembered 词 ｜ 遗忘 $forgotten 词", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            WordbookCard(
                isFirstLaunch = isFirstLaunch,
                hasSelectedBook = hasSelectedBook,
                bookName = bookName,
                unitLabel = unitLabel,
                progress = progress,
                newWords = newWords,
                reviewWords = reviewWords,
                onSelectBookClick = { navController.navigate("wordbookSelector") },
                onStudyClick = { navController.navigate("memory") },
                onReviewClick = { navController.navigate("review") }
            )
        }

        item {
            ChallengeCard(navController, "今日挑战")
        }

        item {
            ChallengeCard(navController, "昨日挑战")
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding( 20.dp)
            ) {
                Text(
                    text = "更多功能",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                FeatureGridSection()
            }
        }


    }
}

@Composable
fun TopFeatureBar(navController: NavController, modifier: Modifier = Modifier) {
    val features = listOf(
        "排行榜" to Icons.Default.Face,
        "任务" to Icons.Default.List,
        "挑战" to Icons.Default.Favorite
    )

    LazyRow(
        modifier = modifier, // 外部传入宽度控制
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(features) { (label, icon) ->
            ElevatedButton(
                onClick = { navController.navigate(label) },
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ChallengeCard(navController: NavController, name: String) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🎲 $name", style = MaterialTheme.typography.titleMedium)
                Text("五词匹配游戏", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { navController.navigate("challenge") }) {
                Text("开始")
            }
        }
    }
}

@Composable
fun FeatureGridSection() {
    val features = listOf(
        Icons.Default.Star to "学习计划",
        Icons.Default.Favorite to "收藏本",
        Icons.Default.Person to "错题本",
        Icons.Default.Info to "学习详情",
        Icons.Default.DateRange to "助记共建",
        Icons.Default.Build to "暂未开发"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 3500.dp)
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(features) { (icon, title) ->
            FeatureCard(icon = icon, title = title, onClick = { /* TODO */ })
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

    }
}


@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
