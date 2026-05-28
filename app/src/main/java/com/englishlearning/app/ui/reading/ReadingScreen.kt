package com.englishlearning.app.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.englishlearning.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen() {
    var selectedCategory by remember { mutableIntStateOf(0) }
    val categories = listOf("全部", "科技", "生活", "文化", "教育")
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Accent, AccentDark)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "📖 阅读理解",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击单词查看释义",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        // 分类选择
        ScrollableTabRow(
            selectedTabIndex = selectedCategory,
            containerColor = Surface,
            contentColor = Accent,
            edgePadding = 16.dp
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategory == index,
                    onClick = { selectedCategory = index },
                    text = { Text(category) }
                )
            }
        }
        
        // 文章列表
        ArticleList()
    }
}

@Composable
fun ArticleList() {
    val articles = remember {
        listOf(
            ArticleItem(
                title = "The Importance of Reading",
                summary = "Reading is one of the most important skills we can learn. It opens doors to new worlds and ideas.",
                level = "初级",
                wordCount = 156,
                readTime = 3,
                isFavorite = true
            ),
            ArticleItem(
                title = "Technology in Education",
                summary = "How technology is changing the way we learn and teach in modern classrooms.",
                level = "中级",
                wordCount = 289,
                readTime = 5,
                isFavorite = false
            ),
            ArticleItem(
                title = "Sustainable Living Tips",
                summary = "Simple changes we can make in our daily lives to help protect the environment.",
                level = "初级",
                wordCount = 198,
                readTime = 4,
                isFavorite = false
            ),
            ArticleItem(
                title = "The Future of AI",
                summary = "Exploring how artificial intelligence will shape our world in the coming decades.",
                level = "高级",
                wordCount = 423,
                readTime = 8,
                isFavorite = true
            )
        )
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(articles) { article ->
            ArticleCard(article = article)
        }
    }
}

data class ArticleItem(
    val title: String,
    val summary: String,
    val level: String,
    val wordCount: Int,
    val readTime: Int,
    val isFavorite: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleCard(article: ArticleItem) {
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
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = { /* 收藏 */ }) {
                    Icon(
                        imageVector = if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (article.isFavorite) Error else OnSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(article.level) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Accent.copy(alpha = 0.1f),
                        labelColor = Accent
                    )
                )
                AssistChip(
                    onClick = { },
                    label = { Text("${article.wordCount}词") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                AssistChip(
                    onClick = { },
                    label = { Text("${article.readTime}分钟") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { /* 开始阅读 */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始阅读")
            }
        }
    }
}
