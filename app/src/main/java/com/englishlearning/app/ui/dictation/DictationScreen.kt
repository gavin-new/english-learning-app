package com.englishlearning.app.ui.dictation

import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.englishlearning.app.audio.AudioPlayerManager
import com.englishlearning.app.data.model.DictationRecord
import com.englishlearning.app.data.model.VocabularyBookItem
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.ui.phonics.PhonicsLearningScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 听写练习界面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DictationScreen(
    words: List<Word>,
    audioPlayer: AudioPlayerManager,
    onComplete: (sessionId: String, correctCount: Int, totalCount: Int) -> Unit = { _, _, _ -> },
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // 状态管理
    var currentIndex by remember { mutableStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var dictationResults by remember { mutableStateOf<Map<Int, DictationResult>>(emptyMap()) }
    var isCompleted by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val currentWord = words.getOrNull(currentIndex)

    // 语音权限
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "需要录音权限才能使用语音输入", Toast.LENGTH_SHORT).show()
        }
    }

    // 初始化语音识别
    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    // 当前进度
    val progress = if (words.isNotEmpty()) (currentIndex + 1).toFloat() / words.size else 0f

    // 自动播放当前单词
    LaunchedEffect(currentIndex) {
        currentWord?.let { word ->
            delay(300)
            audioPlayer.speak(word.word)
        }
    }

    // 请求焦点
    LaunchedEffect(currentIndex, showResult) {
        if (!showResult) {
            focusRequester.requestFocus()
        }
    }

    // ═══ 辅助函数定义（必须在UI之前）═══
    fun checkAnswer() {
        currentWord?.let { word ->
            val correct = userInput.trim().equals(word.word, ignoreCase = true)
            isCorrect = correct
            showResult = true
            dictationResults = dictationResults + (currentIndex to DictationResult(
                word = word.word,
                userInput = userInput.trim(),
                isCorrect = correct
            ))
            keyboardController?.hide()
        }
    }

    fun nextWord() {
        if (currentIndex < words.size - 1) {
            currentIndex++
            userInput = ""
            showResult = false
            isCorrect = false
        } else {
            isCompleted = true
            val correctCount = dictationResults.count { it.value.isCorrect }
            onComplete(UUID.randomUUID().toString(), correctCount, words.size)
        }
    }

    fun toggleVoiceInput() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        } else {
            isListening = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("听写练习") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = "退出")
                    }
                },
                actions = {
                    Text(
                        text = "${currentIndex + 1}/${words.size}",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        }
    ) { padding ->
        if (isCompleted) {
            // 完成界面
            DictationCompleteScreen(
                results = dictationResults,
                totalCount = words.size,
                onReview = { /* 复习错误单词 */ },
                onExit = onExit,
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 进度条
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 播放控制区
                PlayControlSection(
                    onPlayWord = { currentWord?.let { audioPlayer.speak(it.word) } },
                    onPlaySlow = {
                        currentWord?.let {
                            audioPlayer.setSpeed(0.7f)
                            audioPlayer.speak(it.word) {
                                audioPlayer.setSpeed(1.0f)
                            }
                        }
                    },
                    onPlaySyllables = {
                        // 播放音节拆分
                        currentWord?.let { word ->
                            val syllables = com.englishlearning.app.utils.PhonicsUtils.splitSyllables(word.word)
                            val texts = listOf(word.word) + syllables.map { it.text }
                            audioPlayer.playQueue(texts)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(48.dp))

                // 输入区域
                if (!showResult) {
                    // 输入模式
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        label = { Text("输入你听到的单词") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                checkAnswer()
                            }
                        ),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 语音输入按钮
                    VoiceInputButton(
                        isListening = isListening,
                        hasPermission = hasRecordPermission,
                        onClick = {
                            if (!hasRecordPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                toggleVoiceInput()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 提交按钮
                    Button(
                        onClick = { checkAnswer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = userInput.isNotBlank()
                    ) {
                        Text("提交", fontSize = 18.sp)
                    }
                } else {
                    // 结果显示
                    ResultDisplay(
                        word = currentWord?.word ?: "",
                        userInput = userInput,
                        isCorrect = isCorrect,
                        onContinue = { nextWord() }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 底部提示
                Text(
                    text = "提示：可以多次点击播放按钮听清楚",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 播放单词发音
}

/**
 * 播放控制区域
 */
@Composable
private fun PlayControlSection(
    onPlayWord: () -> Unit,
    onPlaySlow: () -> Unit,
    onPlaySyllables: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PlayButton(
            icon = Icons.Default.VolumeUp,
            label = "播放",
            onClick = onPlayWord
        )

        PlayButton(
            icon = Icons.Default.SlowMotionVideo,
            label = "慢速",
            onClick = onPlaySlow
        )

        PlayButton(
            icon = Icons.Default.Segment,
            label = "音节",
            onClick = onPlaySyllables
        )
    }
}

/**
 * 播放按钮
 */
@Composable
private fun PlayButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 语音输入按钮
 */
@Composable
private fun VoiceInputButton(
    isListening: Boolean,
    hasPermission: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isListening -> MaterialTheme.colorScheme.errorContainer
        !hasPermission -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val iconColor = when {
        isListening -> MaterialTheme.colorScheme.error
        !hasPermission -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isListening) 3.dp else 2.dp,
                color = iconColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = if (isListening) "停止录音" else "语音输入",
            modifier = Modifier.size(36.dp),
            tint = iconColor
        )
    }

    if (isListening) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "正在听...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * 结果显示
 */
@Composable
private fun ResultDisplay(
    word: String,
    userInput: String,
    isCorrect: Boolean,
    onContinue: () -> Unit
) {
    val backgroundColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val borderColor = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE57373)
    val textColor = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 结果卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 正确/错误图标
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = if (isCorrect) "正确" else "错误",
                    modifier = Modifier.size(64.dp),
                    tint = textColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 结果文字
                Text(
                    text = if (isCorrect) "回答正确！" else "回答错误",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 正确答案
                Text(
                    text = "正确答案：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = word,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                if (!isCorrect) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "你的答案：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = userInput,
                        style = MaterialTheme.typography.headlineMedium,
                        color = textColor,
                        letterSpacing = 2.sp
                    )

                    // 显示差异
                    Spacer(modifier = Modifier.height(8.dp))
                    DiffDisplay(expected = word, actual = userInput)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 继续按钮
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("继续", fontSize = 18.sp)
        }
    }
}

/**
 * 差异显示
 */
@Composable
private fun DiffDisplay(expected: String, actual: String) {
    val maxLen = maxOf(expected.length, actual.length)

    Row {
        for (i in 0 until maxLen) {
            val expChar = expected.getOrNull(i)
            val actChar = actual.getOrNull(i)
            val isMatch = expChar?.equals(actChar ?: ' ', ignoreCase = true) == true

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isMatch) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp,
                        if (isMatch) Color(0xFF4CAF50) else Color(0xFFE57373),
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actChar?.toString() ?: "",
                    color = if (isMatch) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Bold
                )
            }

            if (i < maxLen - 1) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

/**
 * 听写结果
 */
data class DictationResult(
    val word: String,
    val userInput: String,
    val isCorrect: Boolean
)

/**
 * 完成界面
 */
@Composable
private fun DictationCompleteScreen(
    results: Map<Int, DictationResult>,
    totalCount: Int,
    onReview: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val correctCount = results.count { it.value.isCorrect }
    val accuracy = if (totalCount > 0) correctCount.toFloat() / totalCount else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
        .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "完成",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "听写完成！",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 统计卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$correctCount / $totalCount",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "正确率: ${(accuracy * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 错误单词列表
        val incorrectWords = results.filter { !it.value.isCorrect }.values.toList()
        if (incorrectWords.isNotEmpty()) {
            Text(
                text = "需要复习的单词：",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    incorrectWords.forEach { result ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = result.word,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "→ ${result.userInput}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (result != incorrectWords.last()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onReview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("复习错误单词")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("完成", fontSize = 18.sp)
        }
    }
}
