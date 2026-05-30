package com.englishlearning.app.ui.vocabulary

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishlearning.app.data.model.VocabularyBookItem
import com.englishlearning.app.data.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 生词本主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyBookScreen(
    vocabularyFlow: Flow<List<VocabularyBookItem>> = emptyFlow(),
    reviewCountFlow: Flow<Int> = emptyFlow(),
    statistics: VocabularyStatistics = VocabularyStatistics(0, 0, 0),
    onWordClick: (VocabularyBookItem) -> Unit = {},
    onAddToVocabulary: (Word) -> Unit = {},
    onRemoveFromVocabulary: (Long) -> Unit = {},
    onStartReview: () -> Unit = {},
    onNavigateToDictation: () -> Unit = {},
    onSearch: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val tabs = listOf("全部", "待复习", "已掌握")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                onSearch(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("搜索单词...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清除")
                                    }
                                }
                            }
                        )
                    } else {
                        Text("生词本")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearching) "关闭搜索" else "搜索"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (statistics.toReview > 0) {
                ExtendedFloatingActionButton(
                    onClick = onStartReview,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("开始复习 (${statistics.toReview})") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 统计卡片
            StatisticsCard(statistics = statistics)

            // Tab选择
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // 单词列表
            // 注意：实际实现中应该根据selectedTab过滤数据
            // 这里简化处理
        }
    }
}

/**
 * 统计卡片
 */
@Composable
private fun StatisticsCard(statistics: VocabularyStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "学习进度",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = statistics.total.toString(),
                    label = "总单词",
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    value = statistics.toReview.toString(),
                    label = "待复习",
                    color = MaterialTheme.colorScheme.error
                )
                StatItem(
                    value = statistics.mastered.toString(),
                    label = "已掌握",
                    color = Color(0xFF4CAF50)
                )
            }

            if (statistics.total > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                // 掌握率进度条
                LinearProgressIndicator(
                    progress = statistics.masteryRate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "掌握率: ${(statistics.masteryRate * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * 生词本单词项
 */
@Composable
fun VocabularyBookItemCard(
    item: VocabularyBookItem,
    onClick: () -> Unit = {},
    onRemove: () -> Unit = {},
    onMarkMastered: () -> Unit = {}
) {
    val reviewStage = item.getReviewStage()
    val daysUntilReview = item.getDaysUntilReview()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 单词和音标
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.word,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (!item.phonetic.isNullOrBlank()) {
                        Text(
                            text = item.phonetic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 复习状态标签
                ReviewStatusBadge(
                    stage = reviewStage,
                    daysUntil = daysUntilReview,
                    isMastered = item.isMastered
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 中文意思
            Text(
                text = item.meaning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 底部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 复习次数和下次复习时间
                Column {
                    Text(
                        text = "已复习 ${item.reviewCount} 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!item.isMastered && item.nextReviewTime != null) {
                        val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
                        Text(
                            text = "下次复习: ${dateFormat.format(Date(item.nextReviewTime))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 操作按钮
                Row {
                    if (!item.isMastered) {
                        TextButton(onClick = onMarkMastered) {
                            Text("标记掌握")
                        }
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * 复习状态标签
 */
@Composable
private fun ReviewStatusBadge(stage: String, daysUntil: Int, isMastered: Boolean) {
    val backgroundColor = when {
        isMastered -> Color(0xFFE8F5E9)
        daysUntil == 0 -> Color(0xFFFFEBEE)
        daysUntil in 1..2 -> Color(0xFFFFF3E0)
        else -> Color(0xFFE3F2FD)
    }

    val textColor = when {
        isMastered -> Color(0xFF2E7D32)
        daysUntil == 0 -> Color(0xFFC62828)
        daysUntil in 1..2 -> Color(0xFFEF6C00)
        else -> Color(0xFF1565C0)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = stage,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 复习模式选择对话框
 */
@Composable
fun ReviewModeDialog(
    reviewCount: Int,
    onDismiss: () -> Unit,
    onSelectMode: (ReviewMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择复习模式") },
        text = {
            Column {
                Text("今天有 $reviewCount 个单词需要复习")
                Spacer(modifier = Modifier.height(16.dp))

                ReviewModeOption(
                    title = "快速复习",
                    description = "浏览单词，自我检测",
                    icon = Icons.Default.Visibility,
                    onClick = { onSelectMode(ReviewMode.QUICK) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ReviewModeOption(
                    title = "听写模式",
                    description = "听音写词，检验拼写",
                    icon = Icons.Default.Edit,
                    onClick = { onSelectMode(ReviewMode.DICTATION) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ReviewModeOption(
                    title = "拼写测试",
                    description = "看义写词，完整检验",
                    icon = Icons.Default.Spellcheck,
                    onClick = { onSelectMode(ReviewMode.SPELLING) }
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
 * 复习模式选项
 */
@Composable
private fun ReviewModeOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
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
 * 复习模式
 */
enum class ReviewMode {
    QUICK,      // 快速浏览
    DICTATION,  // 听写
    SPELLING    // 拼写测试
}

/**
 * 生词本统计
 */
data class VocabularyStatistics(
    val total: Int,
    val mastered: Int,
    val toReview: Int
) {
    val learning: Int get() = total - mastered
    val masteryRate: Float get() = if (total > 0) mastered.toFloat() / total else 0f
}
