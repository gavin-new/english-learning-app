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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showWordDetail by remember { mutableStateOf<Word?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Primary, PrimaryLight)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "📚 背单词",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "艾宾浩斯遗忘曲线复习",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        // Tab选择
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = Primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("学习") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("复习") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("生词本") }
            )
        }
        
        // 内容区域
        when (selectedTab) {
            0 -> LearningTab(onWordClick = { showWordDetail = it })
            1 -> ReviewTab(onWordClick = { showWordDetail = it })
            2 -> FavoriteTab(onWordClick = { showWordDetail = it })
        }
    }
    
    // 单词详情弹窗
    showWordDetail?.let { word ->
        WordDetailDialog(
            word = word,
            onDismiss = { showWordDetail = null }
        )
    }
}

@Composable
fun LearningTab(onWordClick: (Word) -> Unit) {
    // 示例单词数据
    val sampleWords = remember {
        listOf(
            Word(
                id = 1,
                word = "abandon",
                phonetic = "/əˈbændən/",
                meaning = "v. 放弃；遗弃",
                example = "They had to abandon their car in the snow.",
                exampleTranslation = "他们不得不把汽车遗弃在雪地里。",
                level = 2,
                prefix = "ab-",
                root = "bandon",
                suffix = null,
                rootMeaning = "控制"
            ),
            Word(
                id = 2,
                word = "beautiful",
                phonetic = "/ˈbjuːtɪfʊl/",
                meaning = "adj. 美丽的；美好的",
                example = "She has a beautiful smile.",
                exampleTranslation = "她有一个美丽的微笑。",
                level = 1,
                prefix = null,
                root = "beaut",
                suffix = "-ful",
                rootMeaning = "美"
            ),
            Word(
                id = 3,
                word = "knowledge",
                phonetic = "/ˈnɒlɪdʒ/",
                meaning = "n. 知识；学问",
                example = "Knowledge is power.",
                exampleTranslation = "知识就是力量。",
                level = 1,
                prefix = null,
                root = "know",
                suffix = "-ledge",
                rootMeaning = "知道"
            )
        )
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "今日新词",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(sampleWords) { word ->
            WordCard(word = word, onClick = { onWordClick(word) })
        }
    }
}

@Composable
fun ReviewTab(onWordClick: (Word) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Secondary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "🎉 太棒了！",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "今日复习任务已完成",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant
        )
    }
}

@Composable
fun FavoriteTab(onWordClick: (Word) -> Unit) {
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
            tint = Error,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "📝 生词本",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "收藏不认识的单词，随时复习",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordCard(word: Word, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 单词信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = word.phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.example,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1
                )
            }
            
            // 收藏按钮
            IconButton(onClick = { /* 收藏 */ }) {
                Icon(
                    imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (word.isFavorite) Error else OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WordDetailDialog(word: Word, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = word.phonetic,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "📖 释义",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "💬 例句",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
                Text(
                    text = word.example,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = word.exampleTranslation,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                
                // 词根词缀
                if (word.prefix != null || word.root != null || word.suffix != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "🔍 词根词缀分析",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        word.prefix?.let {
                            MorphologyChip(text = it, label = "前缀", color = PrefixColor)
                        }
                        word.root?.let {
                            MorphologyChip(text = it, label = "词根", color = RootColor)
                        }
                        word.suffix?.let {
                            MorphologyChip(text = it, label = "后缀", color = SuffixColor)
                        }
                    }
                    
                    word.rootMeaning?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "词根含义：$it",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("知道了")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { /* 加入生词本 */ }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("加入生词本")
            }
        }
    )
}

@Composable
fun MorphologyChip(text: String, label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}
