package com.englishlearning.app.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishlearning.app.audio.WordAudioService
import com.englishlearning.app.data.model.Word
import kotlinx.coroutines.flow.StateFlow

/**
 * 后台音频播放设置界面
 * 用于选择教材、设置播放模式、调整播放速度
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundAudioScreen(
    categories: List<WordCategory> = emptyList(),
    currentPlayMode: WordAudioService.PlayMode = WordAudioService.PlayMode.WORD_SYLLABLE_MEANING,
    currentSpeed: Float = 1.0f,
    isPlaying: Boolean = false,
    onCategorySelected: (String) -> Unit = {},
    onPlayModeChanged: (WordAudioService.PlayMode) -> Unit = {},
    onSpeedChanged: (Float) -> Unit = {},
    onStartPlayback: () -> Unit = {},
    onStopPlayback: () -> Unit = {},
    onNavigateToPlayer: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showModeSelector by remember { mutableStateOf(false) }
    var showSpeedSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("后台播放设置") },
                navigationIcon = {
                    IconButton(onClick = { /* 返回 */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 当前播放状态卡片
            if (isPlaying) {
                PlayingStatusCard(
                    onNavigateToPlayer = onNavigateToPlayer,
                    onStop = onStopPlayback
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 教材选择区域
            Text(
                text = "选择教材",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    CategoryCard(
                        category = category,
                        isSelected = selectedCategory == category.id,
                        onClick = {
                            selectedCategory = category.id
                            onCategorySelected(category.id)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 播放设置
            Text(
                text = "播放设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 播放模式选择
            PlayModeSelector(
                currentMode = currentPlayMode,
                onModeSelected = { showModeSelector = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 播放速度选择
            SpeedSelector(
                currentSpeed = currentSpeed,
                onSpeedSelected = { showSpeedSelector = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 开始/停止播放按钮
            if (isPlaying) {
                Button(
                    onClick = onStopPlayback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("停止播放", fontSize = 18.sp)
                }
            } else {
                Button(
                    onClick = onStartPlayback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedCategory != null
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始后台播放", fontSize = 18.sp)
                }
            }
        }
    }

    // 播放模式选择对话框
    if (showModeSelector) {
        PlayModeDialog(
            currentMode = currentPlayMode,
            onDismiss = { showModeSelector = false },
            onModeSelected = {
                onPlayModeChanged(it)
                showModeSelector = false
            }
        )
    }

    // 播放速度选择对话框
    if (showSpeedSelector) {
        SpeedDialog(
            currentSpeed = currentSpeed,
            onDismiss = { showSpeedSelector = false },
            onSpeedSelected = {
                onSpeedChanged(it)
                showSpeedSelector = false
            }
        )
    }
}

/**
 * 正在播放状态卡片
 */
@Composable
private fun PlayingStatusCard(
    onNavigateToPlayer: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToPlayer),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 动画播放图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "正在后台播放",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "点击可查看播放详情",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "停止",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 教材分类卡片
 */
@Composable
private fun CategoryCard(
    category: WordCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(category.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${category.wordCount} 个单词",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 播放模式选择器
 */
@Composable
private fun PlayModeSelector(
    currentMode: WordAudioService.PlayMode,
    onModeSelected: () -> Unit
) {
    val modeText = when (currentMode) {
        WordAudioService.PlayMode.WORD_ONLY -> "仅播放单词"
        WordAudioService.PlayMode.WORD_SYLLABLE -> "单词 + 音节拆分"
        WordAudioService.PlayMode.WORD_SYLLABLE_MEANING -> "单词 + 音节 + 中文"
        WordAudioService.PlayMode.FULL_DETAIL -> "完整模式（含例句）"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onModeSelected)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "播放模式",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = modeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 播放速度选择器
 */
@Composable
private fun SpeedSelector(
    currentSpeed: Float,
    onSpeedSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSpeedSelected)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "播放速度",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${currentSpeed}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 速度指示条
            LinearProgressIndicator(
                progress = (currentSpeed - 0.5f) / 1.5f,
                modifier = Modifier.width(60.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 播放模式选择对话框
 */
@Composable
private fun PlayModeDialog(
    currentMode: WordAudioService.PlayMode,
    onDismiss: () -> Unit,
    onModeSelected: (WordAudioService.PlayMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择播放模式") },
        text = {
            Column {
                PlayModeOption(
                    title = "仅播放单词",
                    description = "只朗读单词本身",
                    isSelected = currentMode == WordAudioService.PlayMode.WORD_ONLY,
                    onClick = { onModeSelected(WordAudioService.PlayMode.WORD_ONLY) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PlayModeOption(
                    title = "单词 + 音节拆分",
                    description = "朗读单词后拆分音节",
                    isSelected = currentMode == WordAudioService.PlayMode.WORD_SYLLABLE,
                    onClick = { onModeSelected(WordAudioService.PlayMode.WORD_SYLLABLE) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PlayModeOption(
                    title = "单词 + 音节 + 中文",
                    description = "完整朗读单词、音节和中文意思",
                    isSelected = currentMode == WordAudioService.PlayMode.WORD_SYLLABLE_MEANING,
                    onClick = { onModeSelected(WordAudioService.PlayMode.WORD_SYLLABLE_MEANING) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PlayModeOption(
                    title = "完整模式",
                    description = "包含单词、音节、中文和例句",
                    isSelected = currentMode == WordAudioService.PlayMode.FULL_DETAIL,
                    onClick = { onModeSelected(WordAudioService.PlayMode.FULL_DETAIL) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 播放模式选项
 */
@Composable
private fun PlayModeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 播放速度选择对话框
 */
@Composable
private fun SpeedDialog(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.7f, 0.8f, 1.0f, 1.2f, 1.5f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择播放速度") },
        text = {
            Column {
                speeds.forEach { speed ->
                    SpeedOption(
                        speed = speed,
                        isSelected = currentSpeed == speed,
                        onClick = { onSpeedSelected(speed) }
                    )

                    if (speed != speeds.last()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 速度选项
 */
@Composable
private fun SpeedOption(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when (speed) {
        0.5f -> "0.5x - 很慢"
        0.7f -> "0.7x - 慢速"
        0.8f -> "0.8x - 稍慢"
        1.0f -> "1.0x - 正常"
        1.2f -> "1.2x - 稍快"
        1.5f -> "1.5x - 快速"
        2.0f -> "2.0x - 很快"
        else -> "${speed}x"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 单词分类
 */
data class WordCategory(
    val id: String,
    val name: String,
    val wordCount: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
