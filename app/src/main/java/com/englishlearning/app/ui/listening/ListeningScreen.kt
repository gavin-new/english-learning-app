package com.englishlearning.app.ui.listening

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.englishlearning.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningScreen() {
    var selectedLevel by remember { mutableIntStateOf(0) }
    val levels = listOf("全部", "初级", "中级", "高级")
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Secondary, SecondaryLight)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "🎧 听力练习",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "提升听力，从简单开始",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        // 难度选择
        ScrollableTabRow(
            selectedTabIndex = selectedLevel,
            containerColor = Surface,
            contentColor = Secondary,
            edgePadding = 16.dp
        ) {
            levels.forEachIndexed { index, level ->
                Tab(
                    selected = selectedLevel == index,
                    onClick = { selectedLevel = index },
                    text = { Text(level) }
                )
            }
        }
        
        // 听力材料列表
        ListeningMaterialsList()
    }
}

@Composable
fun ListeningMaterialsList() {
    val materials = remember {
        listOf(
            ListeningMaterialItem(
                title = "每日英语听力",
                description = "适合初学者的日常对话",
                duration = "5:30",
                level = "初级",
                progress = 0.7f
            ),
            ListeningMaterialItem(
                title = "英语新闻听力",
                description = "标准英语新闻播报",
                duration = "8:45",
                level = "中级",
                progress = 0.3f
            ),
            ListeningMaterialItem(
                title = "商务英语",
                description = "职场商务沟通",
                duration = "12:00",
                level = "高级",
                progress = 0f
            ),
            ListeningMaterialItem(
                title = "英语故事",
                description = "有趣的故事讲解",
                duration = "6:20",
                level = "初级",
                progress = 1f
            )
        )
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(materials) { material ->
            ListeningMaterialCard(material = material)
        }
    }
}

data class ListeningMaterialItem(
    val title: String,
    val description: String,
    val duration: String,
    val level: String,
    val progress: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningMaterialCard(material: ListeningMaterialItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = material.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
                
                // 播放按钮
                IconButton(
                    onClick = { /* 播放 */ },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Secondary)
                ) {
                    Icon(
                        imageVector = if (material.progress > 0f) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标签
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { },
                        label = { Text(material.level) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Secondary.copy(alpha = 0.1f),
                            labelColor = Secondary
                        )
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text(material.duration) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                
                // 进度
                if (material.progress > 0f) {
                    Text(
                        text = "${(material.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 进度条
            if (material.progress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = material.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Secondary,
                    trackColor = SurfaceVariant
                )
            }
        }
    }
}
