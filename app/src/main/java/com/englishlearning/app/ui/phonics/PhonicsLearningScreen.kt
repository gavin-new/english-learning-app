package com.englishlearning.app.ui.phonics

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishlearning.app.audio.AudioPlayerManager
import com.englishlearning.app.data.model.PhonicsWord
import com.englishlearning.app.data.model.Syllable
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.ui.theme.*
import com.englishlearning.app.ui.viewmodel.PhonicsViewModel
import com.englishlearning.app.utils.PhonicsUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonicsLearningScreen(
    viewModel: PhonicsViewModel,
    audioPlayer: AudioPlayerManager,
    onStartDictation: () -> Unit = {}
) {
    val currentWord by viewModel.currentWord.collectAsState()
    val phonicsWord by viewModel.phonicsWord.collectAsState()
    val wordList by viewModel.wordList.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var isExampleExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (wordList.isEmpty()) {
            viewModel.loadAllWords()
        }
    }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    val word = currentWord
    val pw = phonicsWord

    if (word == null || pw == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("加载中...", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { viewModel.loadAllWords() }) { Text("重试") }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自然拼读") },
                actions = {
                    // 搜索
                    IconButton(onClick = { onStartDictation() }) {
                        Icon(Icons.Default.Edit, "听写")
                    }
                    // 加入生词本
                    IconButton(onClick = {
                        // 通过ViewModel添加入生词本
                    }) {
                        Icon(Icons.Default.Star, "收藏")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 单词展示
            WordHeaderSection(
                word = pw,
                isPlaying = isPlaying,
                onPlayClick = {
                    isPlaying = true
                    audioPlayer.speak(pw.word) { isPlaying = false }
                }
            )

            // 音节拆分
            SyllableSection(
                syllables = pw.syllables,
                onSyllableClick = { _, syllable ->
                    audioPlayer.speak(syllable.text)
                }
            )

            // 中文释义
            MeaningSection(meaning = pw.meaning)

            // 例句
            if (pw.example != null) {
                ExampleSection(
                    example = pw.example,
                    translation = pw.exampleTranslation,
                    isExpanded = isExampleExpanded,
                    onToggleExpand = { isExampleExpanded = !isExampleExpanded },
                    onPlayClick = { audioPlayer.speak(pw.example) }
                )
            }

            // 播放全部
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = {
                            isPlaying = true
                            val queue = viewModel.getPlayQueue()
                            audioPlayer.playQueue(queue) { isPlaying = false }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("完整播放")
                    }
                }
            }

            // 上下翻页
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.previousWord() },
                    enabled = currentIndex > 0
                ) { Text("上一个") }

                Text(
                    "${currentIndex + 1} / ${wordList.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                OutlinedButton(
                    onClick = { viewModel.nextWord() },
                    enabled = currentIndex < wordList.size - 1
                ) { Text("下一个") }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// Reuse the existing sub-composables from the original file
@Composable
private fun WordHeaderSection(word: PhonicsWord, isPlaying: Boolean, onPlayClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(brush = Brush.verticalGradient(listOf(Primary, PrimaryLight)))
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(word.word, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(word.phonetic, fontSize = 24.sp, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            FilledIconButton(onClick = onPlayClick, modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Primary)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "暂停" else "播放", modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun SyllableSection(syllables: List<Syllable>, onSyllableClick: (Int, Syllable) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("音节拆分", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = Primary.copy(alpha = 0.3f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            syllables.forEachIndexed { index, syllable ->
                SyllableCard(syllable = syllable, onClick = { onSyllableClick(index, syllable) })
                if (index < syllables.size - 1) {
                    Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SyllableCard(syllable: Syllable, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Primary.copy(alpha = 0.1f)).clickable(onClick = onClick).padding(12.dp)
    ) {
        Text(syllable.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
        Spacer(Modifier.height(4.dp))
        Text(syllable.phonetic, fontSize = 14.sp, color = OnSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(syllable.meaning, fontSize = 16.sp, color = Secondary)
        Spacer(Modifier.height(4.dp))
        Icon(Icons.Default.VolumeUp, "播放", tint = Primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MeaningSection(meaning: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(color = Primary.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text(meaning, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = OnSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Primary.copy(alpha = 0.3f))
    }
}

@Composable
private fun ExampleSection(example: String, translation: String?, isExpanded: Boolean, onToggleExpand: () -> Unit, onPlayClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SurfaceVariant.copy(alpha = 0.5f))) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📖 例句", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Primary)
                Row {
                    IconButton(onClick = onPlayClick) { Icon(Icons.Default.VolumeUp, "播放例句", tint = Primary) }
                    IconButton(onClick = onToggleExpand) { Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (isExpanded) "收起" else "展开", tint = Primary) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(example, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            if (isExpanded && translation != null) {
                Spacer(Modifier.height(8.dp))
                Text(translation, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        }
    }
}
