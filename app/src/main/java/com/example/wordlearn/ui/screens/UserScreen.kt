package com.example.wordlearn.ui.screens

import android.media.MediaPlayer
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordlearn.R
import com.example.wordlearn.ui.theme.ThemeManager
import com.example.wordlearn.ui.viewmodel.AchievementViewModel
import com.example.wordlearn.data.Achievement
import com.example.wordlearn.ui.components.AchievementCard
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// 用户数据类
data class UserProfile(
    val name: String = "学习者",
    val avatar: Int = R.drawable.ic_launcher_foreground,
    val signature: String = "每天进步一点点",
    val learningGoal: String = "每天学习10个新单词",
    val totalLearningTime: Int = 120, // 分钟
    val totalWords: Int = 500,
    val continuousLearningDays: Int = 5
)

@Composable
fun UserScreen(
    navController: NavController = rememberNavController(),
    viewModel: AchievementViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_settings", Context.MODE_PRIVATE) }
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    // 使用 ThemeManager 的暗色模式状态
    val isDarkMode by ThemeManager.isDarkMode
    var isVideoEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("video_enabled", true))
    }
    var isSoundEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("sound_enabled", true))
    }

    // 获取成就数据
    val achievements by viewModel.achievements.collectAsState()
    val unlockedAchievements by viewModel.unlockedAchievements.collectAsState()
    val inProgressAchievements by viewModel.inProgressAchievements.collectAsState()

    // 播放音效
    LaunchedEffect(Unit) {
        if (isSoundEnabled) {
            val mediaPlayer = MediaPlayer.create(context, R.raw.user)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                it.release()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF2F2F7))
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 个人信息卡片
        UserProfileCard(
            userProfile = userProfile,
            onEditClick = { showEditDialog = true },
            isDarkMode = isDarkMode
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 学习统计卡片
        LearningStatsCard(userProfile = userProfile, isDarkMode = isDarkMode)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 成就展示卡片
        NewAchievementsCard(
            achievements = achievements,
            unlockedAchievements = unlockedAchievements,
            inProgressAchievements = inProgressAchievements,
            onShowAllClick = {
                // 导航到全部成就页面
                navController.navigate(com.example.wordlearn.navigation.NavRoute.AllAchievements.route)
            },
            isDarkMode = isDarkMode
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 设置卡片
        SettingsCard(
            isDarkMode = isDarkMode,
            isVideoEnabled = isVideoEnabled,
            isSoundEnabled = isSoundEnabled,
            onDarkModeChange = { newValue ->
                // 使用 ThemeManager 切换主题
                ThemeManager.setDarkMode(context, newValue)
            },
            onVideoEnabledChange = { newValue ->
                isVideoEnabled = newValue
                sharedPrefs.edit().putBoolean("video_enabled", newValue).apply()
            },
            onSoundEnabledChange = { newValue ->
                isSoundEnabled = newValue
                sharedPrefs.edit().putBoolean("sound_enabled", newValue).apply()
            }
        )
    }

    // 编辑个人信息对话框
    if (showEditDialog) {
        EditProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProfile ->
                userProfile = updatedProfile
                showEditDialog = false
            }
        )
    }
}

@Composable
fun UserProfileCard(
    userProfile: UserProfile,
    onEditClick: () -> Unit,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像和编辑按钮
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = userProfile.avatar),
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 用户名
            Text(
                text = userProfile.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color.Black
            )

            // 个性签名
            Text(
                text = userProfile.signature,
                fontSize = 14.sp,
                color = if (isDarkMode) Color.LightGray else Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 学习目标
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "学习目标",
                    tint = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "目标: ${userProfile.learningGoal}",
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun LearningStatsCard(
    userProfile: UserProfile,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "学习统计",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Info,
                    value = "${userProfile.totalLearningTime}分钟",
                    label = "总学习时长",
                    isDarkMode = isDarkMode
                )
                StatItem(
                    icon = Icons.Default.Person,
                    value = "${userProfile.totalWords}词",
                    label = "已掌握词汇",
                    isDarkMode = isDarkMode
                )
                StatItem(
                    icon = Icons.Default.Check,
                    value = "${userProfile.continuousLearningDays}天",
                    label = "连续学习",
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
fun NewAchievementsCard(
    achievements: List<Achievement>,
    unlockedAchievements: List<Achievement>,
    inProgressAchievements: List<Achievement>,
    onShowAllClick: () -> Unit,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题和查看全部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的成就",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                
                TextButton(onClick = onShowAllClick) {
                    Text(
                        text = "查看全部",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // 成就统计
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AchievementStatItem(
                    count = achievements.size,
                    label = "总成就",
                    isDarkMode = isDarkMode
                )
                AchievementStatItem(
                    count = unlockedAchievements.size,
                    label = "已解锁",
                    isDarkMode = isDarkMode
                )
                AchievementStatItem(
                    count = inProgressAchievements.size,
                    label = "进行中",
                    isDarkMode = isDarkMode
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 最近解锁的成就
            if (unlockedAchievements.isNotEmpty()) {
                Text(
                    text = "最近解锁的成就",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(unlockedAchievements.take(3)) { achievement ->
                        AchievementItemCard(achievement = achievement, isDarkMode = isDarkMode)
                    }
                }
            } else {
                // 没有解锁的成就时显示提示
                EmptyAchievementState(isDarkMode = isDarkMode)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 进行中的成就
            if (inProgressAchievements.isNotEmpty()) {
                Text(
                    text = "正在进行的成就",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                inProgressAchievements.take(2).forEach { achievement ->
                    AchievementProgressItem(
                        achievement = achievement,
                        isDarkMode = isDarkMode
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AchievementStatItem(
    count: Int,
    label: String,
    isDarkMode: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isDarkMode) Color.LightGray else Color.Gray
        )
    }
}

@Composable
fun AchievementItemCard(
    achievement: Achievement,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = achievement.name,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isDarkMode) 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) 
                else 
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = achievement.description,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (isDarkMode) Color.LightGray else Color.DarkGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // 更安全地处理unlockedAt
            val unlockedText = if (achievement.unlockedAt != null) {
                try {
                    "解锁于: ${achievement.unlockedAt.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))}"
                } catch (e: Exception) {
                    "已解锁"
                }
            } else {
                "已解锁"
            }
            
            Text(
                text = unlockedText,
                fontSize = 10.sp,
                color = if (isDarkMode) Color.LightGray else Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AchievementProgressItem(
    achievement: Achievement,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) 
                Color(0xFF353535) 
            else 
                Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = if (isDarkMode) Color.LightGray else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = achievement.name,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    text = achievement.description,
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.LightGray else Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { achievement.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = if (isDarkMode) 
                        Color(0xFF555555) 
                    else 
                        Color(0xFFDDDDDD)
                )
                Text(
                    text = "${(achievement.progress * 100).toInt()}% 完成",
                    fontSize = 10.sp,
                    color = if (isDarkMode) Color.LightGray else Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun EmptyAchievementState(isDarkMode: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = if (isDarkMode) Color.Gray else Color.LightGray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "暂无已解锁的成就",
                color = if (isDarkMode) Color.Gray else Color.DarkGray,
                fontSize = 14.sp
            )
            Text(
                text = "继续努力学习解锁更多成就吧！",
                color = if (isDarkMode) Color.Gray else Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    isDarkMode: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDarkMode) Color.White else Color.Black
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color.Black
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isDarkMode) Color.LightGray else Color.Gray
        )
    }
}

@Composable
fun SettingsCard(
    isDarkMode: Boolean,
    isVideoEnabled: Boolean,
    isSoundEnabled: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onVideoEnabledChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // 深色模式设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "深色模式",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "深色模式",
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange
                )
            }
            
            // 背景视频设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "背景视频",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "背景视频",
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                }
                Switch(
                    checked = isVideoEnabled,
                    onCheckedChange = onVideoEnabledChange
                )
            }
            
            // 音效设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "音效",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "音效",
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                }
                Switch(
                    checked = isSoundEnabled,
                    onCheckedChange = onSoundEnabledChange
                )
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var signature by remember { mutableStateOf(userProfile.signature) }
    var learningGoal by remember { mutableStateOf(userProfile.learningGoal) }
    var showAvatarOptions by remember { mutableStateOf(false) }

    // 预设头像列表
    val avatarOptions = listOf(
        R.drawable.ic_launcher_foreground,
        // 这里可以添加更多预设头像
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑个人信息") },
        text = {
            Column {
                // 头像选择区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = userProfile.avatar),
                        contentDescription = "当前头像",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable { showAvatarOptions = true },
                        contentScale = ContentScale.Crop
                    )
                }

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = signature,
                    onValueChange = { signature = it },
                    label = { Text("个性签名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = learningGoal,
                    onValueChange = { learningGoal = it },
                    label = { Text("学习目标") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(userProfile.copy(
                        name = name,
                        signature = signature,
                        learningGoal = learningGoal
                    ))
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 头像选择对话框
    if (showAvatarOptions) {
        AlertDialog(
            onDismissRequest = { showAvatarOptions = false },
            title = { Text("选择头像") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(avatarOptions) { avatarId ->
                        Image(
                            painter = painterResource(id = avatarId),
                            contentDescription = "头像选项",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onSave(userProfile.copy(avatar = avatarId))
                                    showAvatarOptions = false
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarOptions = false }) {
                    Text("取消")
                }
            }
        )
    }
}
