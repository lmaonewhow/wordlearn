package com.example.wordlearn.ui.screens

import android.media.MediaPlayer
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordlearn.R

// 用户数据类
data class UserProfile(
    val name: String = "学习者",
    val avatar: Int = R.drawable.ic_launcher_foreground,
    val signature: String = "每天进步一点点",
    val learningGoal: String = "每天学习10个新单词",
    val totalLearningTime: Int = 120, // 分钟
    val totalWords: Int = 500,
    val continuousLearningDays: Int = 5,
    val achievements: List<Achievement> = listOf(
        Achievement("坚持不懈", "连续学习7天", true),
        Achievement("词汇达人", "掌握500个单词", true),
        Achievement("学习先锋", "每日目标达成30次", false)
    )
)

// 成就数据类
data class Achievement(
    val title: String,
    val description: String,
    val isUnlocked: Boolean
)

@Composable
fun UserScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_settings", Context.MODE_PRIVATE) }
    
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    // 从 SharedPreferences 读取设置状态
    var isDarkMode by remember { 
        mutableStateOf(sharedPrefs.getBoolean("dark_mode", false))
    }
    var isVideoEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("video_enabled", true))
    }
    var isSoundEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("sound_enabled", true))
    }

    // 播放音效：只在页面进入时播放一次，并且要检查是否启用了音效
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
        AchievementsCard(achievements = userProfile.achievements, isDarkMode = isDarkMode)

        Spacer(modifier = Modifier.height(16.dp))

        // 设置卡片
        SettingsCard(
            isDarkMode = isDarkMode,
            isVideoEnabled = isVideoEnabled,
            isSoundEnabled = isSoundEnabled,
            onDarkModeChange = { newValue ->
                isDarkMode = newValue
                sharedPrefs.edit().putBoolean("dark_mode", newValue).apply()
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "目标",
                    tint = if (isDarkMode) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "学习目标：${userProfile.learningGoal}",
                    color = if (isDarkMode) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun LearningStatsCard(userProfile: UserProfile, isDarkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
fun AchievementsCard(
    achievements: List<Achievement>,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "成就系统",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            achievements.forEach { achievement ->
                AchievementItem(
                    achievement = achievement,
                    isDarkMode = isDarkMode
                )
                if (achievement != achievements.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: Achievement,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "成就",
                tint = if (achievement.isUnlocked) 
                    MaterialTheme.colorScheme.primary 
                else 
                    if (isDarkMode) Color.DarkGray else Color.LightGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = achievement.title,
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = achievement.description,
                    color = if (isDarkMode) Color.Gray else Color.DarkGray,
                    fontSize = 14.sp
                )
            }
        }
        if (achievement.isUnlocked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已解锁",
                tint = MaterialTheme.colorScheme.primary
            )
        }
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
        modifier = Modifier.fillMaxWidth(),
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
            
            // 暗色模式设置
            SettingItem(
                icon = Icons.Default.Settings,
                title = "暗色模式",
                checked = isDarkMode,
                onCheckedChange = onDarkModeChange,
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 开场视频设置
            SettingItem(
                icon = Icons.Default.PlayArrow,
                title = "开场视频",
                checked = isVideoEnabled,
                onCheckedChange = onVideoEnabledChange,
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 音效设置
            SettingItem(
                icon = Icons.Default.Notifications,
                title = "音效",
                checked = isSoundEnabled,
                onCheckedChange = onSoundEnabledChange,
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDarkMode) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = if (isDarkMode) Color.White else Color.Black
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
