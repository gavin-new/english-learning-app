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
import com.englishlearning.app.ui.theme.*
import com.englishlearning.app.utils.PhonicsUtils

/**
 * 自然拼读学习界面
 * 展示音节拆分、发音教学、例句等
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonicsLearningScreen(
    word: String = "abandon",
    onBackClick: () -> Unit = {},
    onAddToFavorites: () -> Unit = {},
    onStartBackgroundPlay: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // 创建音频播放器
    val audioPlayer = remember { AudioPlayerManager(context) }
    
    // 解析单词
    val phonicsWord = remember(word) {
        createPhonicsWord(word)
    }
    
    // 播放状态
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingSyllable by remember { mutableStateOf<Int?>(null) }
    
    // 播放速度
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    
    // 例句展开状态
    var isExampleExpanded by remember { mutableStateOf(false) }
    
    // 释放资源
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自然拼读") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onAddToFavorites) {
                        Icon(Icons.Default.Star, contentDescription = "收藏")
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
            // 顶部单词展示区域
            WordHeaderSection(
                word = phonicsWord,
                isPlaying = isPlaying,
                onPlayClick = {
                    isPlaying = true
                    audioPlayer.speak(phonicsWord.word) {
                        isPlaying = false
                    }
                }
            )
            
            // 音节拆分区域
            SyllableSection(
                syllables = phonicsWord.syllables,
                currentPlayingIndex = currentPlayingSyllable,
                onSyllableClick = { index, syllable ->
                    currentPlayingSyllable = index
                    audioPlayer.speak(syllable.text) {
                        currentPlayingSyllable = null
                    }
                }
            )
            
            // 中文释义
            MeaningSection(meaning = phonicsWord.meaning)
            
            // 例句区域
            ExampleSection(
                example = phonicsWord.example,
                translation = phonicsWord.exampleTranslation,
                isExpanded = isExampleExpanded,
                onToggleExpand = { isExampleExpanded = !isExampleExpanded },
                onPlayClick = {
                    phonicsWord.example?.let {
                        audioPlayer.speak(it)
                    }
                }
            )
            
            // 播放控制区域
            PlaybackControlSection(
                speed = playbackSpeed,
                onSpeedChange = { newSpeed ->
                    playbackSpeed = newSpeed
                    audioPlayer.setSpeed(newSpeed)
                },
                onPlayAllClick = {
                    // 播放完整序列：单词 + 各音节 + 中文
                    val queue = mutableListOf<String>()
                    queue.add(phonicsWord.word)
                    phonicsWord.syllables.forEach { 
                        queue.add(it.text)
                        queue.add(it.meaning)
                    }
                    queue.add(phonicsWord.meaning)
                    
                    isPlaying = true
                    audioPlayer.playQueue(queue) {
                        isPlaying = false
                    }
                }
            )
            
            // 底部操作按钮
            ActionButtonsSection(
                onAddToFavorites = onAddToFavorites,
                onStartBackgroundPlay = onStartBackgroundPlay
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 单词头部展示区域
 */
@Composable
private fun WordHeaderSection(
    word: PhonicsWord,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Primary, PrimaryLight)
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 单词（大字）
            Text(
                text = word.word,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 音标
            Text(
                text = word.phonetic,
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 播放按钮
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = Primary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * 音节拆分区域
 */
@Composable
private fun SyllableSection(
    syllables: List<Syllable>,
    currentPlayingIndex: Int?,
    onSyllableClick: (Int, Syllable) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "音节拆分",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 分隔线
        HorizontalDivider(
            modifier = Modifier.padding(bottom = 16.dp),
            color = Primary.copy(alpha = 0.3f)
        )
        
        // 音节横向排列
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            syllables.forEachIndexed { index, syllable ->
                SyllableCard(
                    syllable = syllable,
                    isPlaying = currentPlayingIndex == index,
                    onClick = { onSyllableClick(index, syllable) }
                )
                
                // 音节之间的分隔符（最后一个不加）
                if (index < syllables.size - 1) {
                    Text(
                        text = "-",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 音节卡片
 */
@Composable
private fun SyllableCard(
    syllable: Syllable,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isPlaying) {
        Primary.copy(alpha = 0.2f)
    } else {
        Primary.copy(alpha = 0.1f)
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        // 音节文本
        Text(
            text = syllable.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 音标
        Text(
            text = syllable.phonetic,
            fontSize = 14.sp,
            color = OnSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 中文解释
        Text(
            text = syllable.meaning,
            fontSize = 16.sp,
            color = Secondary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 播放图标
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "播放",
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 中文释义区域
 */
@Composable
private fun MeaningSection(meaning: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = Primary.copy(alpha = 0.3f))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = meaning,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider(color = Primary.copy(alpha = 0.3f))
    }
}

/**
 * 例句区域
 */
@Composable
private fun ExampleSection(
    example: String?,
    translation: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPlayClick: () -> Unit
) {
    if (example == null) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📖 例句",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                
                Row {
                    IconButton(onClick = onPlayClick) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "播放例句",
                            tint = Primary
                        )
                    }
                    
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = Primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 例句内容
            Text(
                text = example,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic
            )
            
            // 翻译（展开时显示）
            if (isExpanded && translation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

/**
 * 播放控制区域
 */
@Composable
private fun PlaybackControlSection(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onPlayAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 播放全部按钮
            Button(
                onClick = onPlayAllClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("完整播放")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 速度控制
            Text(
                text = "播放速度",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 慢速
                FilterChip(
                    selected = speed == 0.8f,
                    onClick = { onSpeedChange(0.8f) },
                    label = { Text("🐢 慢速") }
                )
                
                // 正常
                FilterChip(
                    selected = speed == 1.0f,
                    onClick = { onSpeedChange(1.0f) },
                    label = { Text("正常") }
                )
                
                // 快速
                FilterChip(
                    selected = speed == 1.2f,
                    onClick = { onSpeedChange(1.2f) },
                    label = { Text("快速 🐇") }
                )
            }
        }
    }
}

/**
 * 底部操作按钮
 */
@Composable
private fun ActionButtonsSection(
    onAddToFavorites: () -> Unit,
    onStartBackgroundPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 加入生词本
        OutlinedButton(
            onClick = onAddToFavorites,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Star, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("加入生词本")
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 后台播放
        Button(
            onClick = onStartBackgroundPlay,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Secondary
            )
        ) {
            Icon(Icons.Default.Headphones, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("后台播放")
        }
    }
}

/**
 * 创建PhonicsWord对象（从普通单词）
 */
private fun createPhonicsWord(word: String): PhonicsWord {
    val syllables = PhonicsUtils.splitSyllables(word)
    val phonetic = syllables.joinToString("") { it.phonetic }
    
    // 示例数据，实际应从数据库获取
    return PhonicsWord(
        id = 1,
        word = word,
        phonetic = "/$phonetic/",
        meaning = when (word.lowercase()) {
            "abandon" -> "放弃；遗弃；放纵"
            "beautiful" -> "美丽的；美好的"
            "knowledge" -> "知识；学问"
            else -> "待添加释义"
        },
        syllables = syllables,
        example = when (word.lowercase()) {
            "abandon" -> "They had to abandon their car in the snow."
            "beautiful" -> "She has a beautiful smile."
            "knowledge" -> "Knowledge is power."
            else -> null
        },
        exampleTranslation = when (word.lowercase()) {
            "abandon" -> "他们不得不把汽车遗弃在雪地里。"
            "beautiful" -> "她有一个美丽的微笑。"
            "knowledge" -> "知识就是力量。"
            else -> null
        }
    )
}
