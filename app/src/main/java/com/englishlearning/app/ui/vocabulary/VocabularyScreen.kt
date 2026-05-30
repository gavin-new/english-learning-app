package com.englishlearning.app.ui.vocabulary

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.englishlearning.app.audio.AudioPlayerManager
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.data.model.VocabularyBookItem
import com.englishlearning.app.ui.theme.*
import com.englishlearning.app.ui.viewmodel.VocabularyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    viewModel: VocabularyViewModel,
    audioPlayer: AudioPlayerManager,
    onWordClick: (Word) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(Primary, PrimaryLight)))
                .padding(24.dp)
        ) {
            Column {
                Text("📚 背单词", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("艾宾浩斯遗忘曲线复习", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = Surface, contentColor = Primary) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("学习") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; viewModel.loadReviewWords() }, text = { Text("复习") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("生词本") })
        }

        when (selectedTab) {
            0 -> LearningTabView(viewModel, audioPlayer, onWordClick)
            1 -> ReviewTabView(viewModel, audioPlayer, onWordClick)
            2 -> VocabBookTabView(viewModel, audioPlayer, onWordClick)
        }
    }
}

@Composable
private fun LearningTabView(
    viewModel: VocabularyViewModel,
    audioPlayer: AudioPlayerManager,
    onWordClick: (Word) -> Unit
) {
    val words by viewModel.todayWords.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTodayWords(30)
    }

    if (words.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.School, null, modifier = Modifier.size(64.dp), tint = OnSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text("正在加载词汇...", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.loadTodayWords(30) }) { Text("刷新") }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("今日新词 (${words.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(words) { word ->
            WordCard(
                word = word,
                onClick = { onWordClick(word) },
                onSpeak = { audioPlayer.speak(word.word) },
                onLearned = { viewModel.markCurrentAsLearned() }
            )
        }
    }
}

@Composable
private fun ReviewTabView(
    viewModel: VocabularyViewModel,
    audioPlayer: AudioPlayerManager,
    onWordClick: (Word) -> Unit
) {
    val words by viewModel.reviewWords.collectAsState()
    val idx by viewModel.reviewIndex.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadReviewWords()
    }

    if (words.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(80.dp), tint = Secondary)
                Spacer(Modifier.height(16.dp))
                Text("🎉 太棒了！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("暂无需复习的单词", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("待复习 (${words.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(words) { word ->
            WordCard(
                word = word,
                onClick = { onWordClick(word) },
                onSpeak = { audioPlayer.speak(word.word) },
                onLearned = {
                    viewModel.completeReview(word.id, masteryLevel = 3)
                }
            )
        }
    }
}

@Composable
private fun VocabBookTabView(
    viewModel: VocabularyViewModel,
    audioPlayer: AudioPlayerManager,
    onWordClick: (Word) -> Unit
) {
    val vocabBook by viewModel.vocabularyBook.collectAsState()

    if (vocabBook.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Favorite, null, modifier = Modifier.size(80.dp), tint = Error)
                Spacer(Modifier.height(16.dp))
                Text("📝 生词本", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("收藏不认识的单词，随时复习", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("生词本 (${vocabBook.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(vocabBook) { item ->
            VocabItemCard(
                item = item,
                onSpeak = { audioPlayer.speak(item.word) },
                onDelete = { viewModel.removeFromVocabularyBook(item.id) },
                onMastered = { viewModel.markVocabAsMastered(item.id) }
            )
        }
    }
}

@Composable
private fun WordCard(
    word: Word,
    onClick: () -> Unit,
    onSpeak: () -> Unit,
    onLearned: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(word.word, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(word.phonetic, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(word.meaning, style = MaterialTheme.typography.bodyMedium, color = Primary)
                Spacer(Modifier.height(2.dp))
                Text(word.example, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, fontStyle = FontStyle.Italic, maxLines = 1)
            }
            Column {
                IconButton(onClick = onSpeak) {
                    Icon(Icons.Default.VolumeUp, "播放", tint = Primary)
                }
                IconButton(onClick = onLearned) {
                    Icon(Icons.Default.Check, "已学", tint = Secondary)
                }
            }
        }
    }
}

@Composable
private fun VocabItemCard(
    item: VocabularyBookItem,
    onSpeak: () -> Unit,
    onDelete: () -> Unit,
    onMastered: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(item.meaning, style = MaterialTheme.typography.bodyMedium, color = Primary)
                Text("复习${item.reviewCount}次 · ${item.getReviewStage()}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            IconButton(onClick = onSpeak) { Icon(Icons.Default.VolumeUp, "播放") }
            if (!item.isMastered) {
                IconButton(onClick = onMastered) { Icon(Icons.Default.CheckCircle, "掌握", tint = Secondary) }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", tint = Error) }
        }
    }
}
