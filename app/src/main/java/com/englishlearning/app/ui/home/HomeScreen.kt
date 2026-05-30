package com.englishlearning.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.englishlearning.app.ui.theme.*
import com.englishlearning.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToVocabulary: () -> Unit = {},
    onNavigateToListening: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToPhonics: () -> Unit = {}
) {
    val stats by viewModel.homeStats.collectAsState()
    val todayCount by viewModel.todayWordCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部问候
        WelcomeCard()

        // 今日目标
        TodayGoalCard(
            learned = todayCount,
            goal = viewModel.getDailyGoal()
        )

        // 学习模块
        Text(
            text = "学习模块",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleCard("背单词", Icons.Default.Book, Primary, "${stats.wordsLearned}", "词", Modifier.weight(1f), onNavigateToVocabulary)
            ModuleCard("拼读学习", Icons.Default.Spellcheck, Secondary, "学习", "发音", Modifier.weight(1f), onNavigateToPhonics)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleCard("听力练习", Icons.Default.Headphones, Accent, "${stats.listeningTime}", "分钟", Modifier.weight(1f), onNavigateToListening)
            ModuleCard("阅读理解", Icons.Default.Article, PrimaryLight, "${stats.readingTime}", "分钟", Modifier.weight(1f), onNavigateToReading)
        }

        // 学习统计
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "学习统计",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        StatsCard(
            streakDays = stats.streakDays,
            totalLearned = stats.totalWordsLearned,
            studyTime = stats.listeningTime + stats.readingTime
        )

        // 待复习提醒
        if (stats.wordsToReview > 0) {
            ReviewReminderCard(
                reviewCount = stats.wordsToReview,
                onReview = onNavigateToVocabulary
            )
        }

        // 刷新按钮
        TextButton(
            onClick = { viewModel.loadHomeData() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("刷新数据")
        }
    }
}

@Composable
private fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                .padding(24.dp)
        ) {
            Column {
                Text("🌟 加油学习！", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("坚持学习，成就更好的自己", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
private fun TodayGoalCard(learned: Int, goal: Int) {
    val progress = if (goal > 0) (learned.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📊 今日目标", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Secondary, trackColor = SurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("已完成 $learned/$goal 词", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Secondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleCard(title: String, icon: ImageVector, color: Color, count: String, unit: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(count, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(streakDays: Int, totalLearned: Int, studyTime: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(Icons.Default.LocalFireDepartment, "$streakDays", "连续天数", Accent)
            StatItem(Icons.Default.School, "$totalLearned", "已学单词", Primary)
            StatItem(Icons.Default.Timer, "$studyTime", "学习时长(分)", Secondary)
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    }
}

@Composable
private fun ReviewReminderCard(reviewCount: Int, onReview: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Accent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("📝 待复习提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("你有 $reviewCount 个单词需要复习", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
            Button(onClick = onReview, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("去复习") }
        }
    }
}
