package com.example.wordlearn.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.wordapp.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 从 assets/m-word 读取 CSV 文件名（无扩展名）
suspend fun Context.loadWordbookNames(): List<String> = withContext(Dispatchers.IO) {
    assets.list("m-word")
        ?.filter { it.endsWith(".csv") }
        ?.map { it.removeSuffix(".csv") }
        ?: emptyList()
}

@Composable
fun WordbookSelectorScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var wordbookList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 加载 assets 中词书文件名
    LaunchedEffect(Unit) {
        wordbookList = context.loadWordbookNames()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "📚 选择词书",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (wordbookList.isEmpty()) {
            Text("没有找到任何词书文件", style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                wordbookList.forEach { name ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSelectedBookId(name)
                                viewModel.markFirstLaunchComplete()
                                navController.popBackStack()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFFF9F7FC)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击开始使用该词书",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
