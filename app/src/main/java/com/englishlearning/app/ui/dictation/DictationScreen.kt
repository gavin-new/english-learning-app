package com.englishlearning.app.ui.dictation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishlearning.app.audio.AudioPlayerManager
import com.englishlearning.app.ui.viewmodel.DictationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictationScreen(
    viewModel: DictationViewModel,
    audioPlayer: AudioPlayerManager,
    onExit: () -> Unit = {}
) {
    val currentWord by viewModel.currentWord.collectAsState()
    val userInput by viewModel.userInput.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val results by viewModel.results.collectAsState()
    val words by viewModel.words.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("听写练习") },
                navigationIcon = {
                    IconButton(onClick = onExit) { Icon(Icons.Default.Close, "退出") }
                },
                actions = {
                    Text("${currentIndex + 1}/${words.size}", modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleMedium)
                }
            )
        }
    ) { padding ->
        if (isCompleted) {
            DictationCompleteView(results = results.values.toList(), onExit = onExit)
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 进度
                LinearProgressIndicator(
                    progress = viewModel.progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(Modifier.height(32.dp))

                // 播放按钮
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PlayButton(Icons.Default.VolumeUp, "播放") { currentWord?.let { audioPlayer.speak(it.word) } }
                    PlayButton(Icons.Default.SlowMotionVideo, "慢速") {
                        currentWord?.let {
                            audioPlayer.setSpeed(0.7f)
                            audioPlayer.speak(it.word) { audioPlayer.setSpeed(1.0f) }
                        }
                    }
                    PlayButton(Icons.Default.Segment, "音节") {
                        currentWord?.let {
                            val syllables = com.englishlearning.app.utils.PhonicsUtils.splitSyllables(it.word)
                            audioPlayer.playQueue(listOf(it.word) + syllables.map { s -> s.text })
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))

                if (!showResult) {
                    // 输入区
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { viewModel.updateInput(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("输入你听到的单词") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); viewModel.checkAnswer() }),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center, letterSpacing = 2.sp)
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.checkAnswer() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = userInput.isNotBlank()
                    ) { Text("提交", fontSize = 18.sp) }
                } else {
                    // 结果显示
                    val word = currentWord?.word ?: ""
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(if (isCorrect) "回答正确！" else "回答错误",
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(24.dp))
                            Text("正确答案：", style = MaterialTheme.typography.bodyMedium)
                            Text(word, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            if (!isCorrect) {
                                Spacer(Modifier.height(16.dp))
                                Text("你的答案：${viewModel.userInput.value}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFC62828))
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.nextWord() }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (currentIndex < words.size - 1) "下一个" else "查看结果")
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Text("提示：可以多次点击播放按钮听清楚", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DictationCompleteView(
    results: List<com.englishlearning.app.ui.viewmodel.DictationResultItem>,
    onExit: () -> Unit
) {
    val correctCount = results.count { it.isCorrect }
    val accuracy = if (results.isNotEmpty()) correctCount.toFloat() / results.size else 0f

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(80.dp), tint = Color(0xFFFFC107))
        Spacer(Modifier.height(24.dp))
        Text("听写完成！", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("正确率: ${(accuracy * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall, color = Primary)
        Text("$correctCount / ${results.size}", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        // 错误列表
        val errors = results.filter { !it.isCorrect }
        if (errors.isNotEmpty()) {
            Text("错误单词:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            errors.forEach { err ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("正确: ${err.word}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("你的: ${err.userInput}", color = Color(0xFFC62828))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("返回") }
    }
}

@Composable
private fun PlayButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
