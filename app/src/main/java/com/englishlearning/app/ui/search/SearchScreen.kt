package com.englishlearning.app.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(SearchMode.TEXT) }
    var recognizedText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var searchResults by remember { mutableStateOf<List<Word>>(emptyList()) }
    var showWordDetail by remember { mutableStateOf<Word?>(null) }
    
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    // 语音识别Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 开始语音识别（简化版，实际需要 SpeechRecognizer）
            isListening = true
        }
    }
    
    // 相机Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            capturedImage = it
            // 使用ML Kit进行文字识别
            recognizeTextFromImage(it)
        }
    }
    
    // 权限检查
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "🔍 智能查询",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "支持字母、语音、拍照、词根查询",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 搜索模式选择
        SearchModeSelector(
            selectedMode = searchMode,
            onModeSelected = { searchMode = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 搜索输入区域
        when (searchMode) {
            SearchMode.TEXT -> {
                // 文字输入
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入单词查询...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            performSearch(searchQuery, searchResults) { results ->
                                searchResults = results
                            }
                            focusManager.clearFocus()
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            SearchMode.VOICE -> {
                // 语音输入
                VoiceSearchSection(
                    isListening = isListening,
                    recognizedText = recognizedText,
                    onStartListening = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            isListening = true
                        } else {
                            speechLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopListening = {
                        isListening = false
                        if (recognizedText.isNotEmpty()) {
                            searchQuery = recognizedText
                            performSearch(recognizedText, searchResults) { results ->
                                searchResults = results
                            }
                        }
                    },
                    onClear = {
                        isListening = false
                        recognizedText = ""
                    }
                )
            }
            
            SearchMode.CAMERA -> {
                // 拍照识别
                CameraSearchSection(
                    capturedImage = capturedImage,
                    recognizedText = recognizedText,
                    onCapture = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            cameraLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onClear = {
                        capturedImage = null
                        recognizedText = ""
                    }
                )
            }
            
            SearchMode.MORPHOLOGY -> {
                // 词根查询
                MorphologySearchSection(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        performMorphologySearch(searchQuery, searchResults) { results ->
                            searchResults = results
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 搜索结果
        if (searchResults.isNotEmpty()) {
            Text(
                text = "找到 ${searchResults.size} 个结果",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { word ->
                    SearchResultItem(
                        word = word,
                        onClick = { showWordDetail = word }
                    )
                }
            }
        } else if (searchQuery.isNotEmpty() || recognizedText.isNotEmpty()) {
            // 无结果提示
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "未找到相关单词",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = "尝试其他搜索方式",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
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

enum class SearchMode {
    TEXT,      // 文字查询
    VOICE,     // 语音查询
    CAMERA,    // 拍照查询
    MORPHOLOGY // 词根查询
}

@Composable
fun SearchModeSelector(
    selectedMode: SearchMode,
    onModeSelected: (SearchMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchModeChip(
            icon = Icons.Default.TextFields,
            label = "字母",
            isSelected = selectedMode == SearchMode.TEXT,
            onClick = { onModeSelected(SearchMode.TEXT) },
            modifier = Modifier.weight(1f)
        )
        SearchModeChip(
            icon = Icons.Default.Mic,
            label = "语音",
            isSelected = selectedMode == SearchMode.VOICE,
            onClick = { onModeSelected(SearchMode.VOICE) },
            modifier = Modifier.weight(1f)
        )
        SearchModeChip(
            icon = Icons.Default.CameraAlt,
            label = "拍照",
            isSelected = selectedMode == SearchMode.CAMERA,
            onClick = { onModeSelected(SearchMode.CAMERA) },
            modifier = Modifier.weight(1f)
        )
        SearchModeChip(
            icon = Icons.Default.AccountTree,
            label = "词根",
            isSelected = selectedMode == SearchMode.MORPHOLOGY,
            onClick = { onModeSelected(SearchMode.MORPHOLOGY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchModeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        },
        modifier = modifier.height(64.dp)
    )
}

@Composable
fun VoiceSearchSection(
    isListening: Boolean,
    recognizedText: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 录音按钮
        IconButton(
            onClick = if (isListening) onStopListening else onStartListening,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isListening) Error else Primary)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "录音",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isListening) "🎙️ 正在录音..." else "点击开始录音",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isListening) Error else OnSurfaceVariant
        )
        
        if (recognizedText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "识别结果：$recognizedText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClear) {
                Text("清除")
            }
        }
    }
}

@Composable
fun CameraSearchSection(
    capturedImage: Bitmap?,
    recognizedText: String,
    onCapture: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 拍照按钮
        IconButton(
            onClick = onCapture,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Accent)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "拍照",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "📷 点击拍照识别文字",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant
        )
        
        if (capturedImage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.size(200.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box {
                    androidx.compose.foundation.Image(
                        bitmap = capturedImage.asImageBitmap(),
                        contentDescription = "拍摄的照片",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        if (recognizedText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "识别结果：$recognizedText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClear) {
                Text("清除")
            }
        }
    }
}

@Composable
fun MorphologySearchSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Column {
        Text(
            text = "💡 输入前缀、后缀或词根进行查询",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // 常用词根示例
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MorphologyExampleChip(text = "un-", onClick = { onQueryChange("un") })
            MorphologyExampleChip(text = "-tion", onClick = { onQueryChange("tion") })
            MorphologyExampleChip(text = "re-", onClick = { onQueryChange("re") })
            MorphologyExampleChip(text = "-ful", onClick = { onQueryChange("ful") })
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("输入词根/前缀/后缀...") },
            leadingIcon = {
                Icon(Icons.Default.AccountTree, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onSearch() }
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "示例：un-（不）, re-（再）, -tion（名词后缀）, -ful（形容词后缀）",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

@Composable
fun MorphologyExampleChip(text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Primary.copy(alpha = 0.1f),
            labelColor = Primary
        )
    )
}

@Composable
fun SearchResultItem(
    word: Word,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = word.phonetic,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary
                )
                if (word.prefix != null || word.root != null || word.suffix != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        word.prefix?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrefixColor
                            )
                        }
                        word.root?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = RootColor
                            )
                        }
                        word.suffix?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = SuffixColor
                            )
                        }
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = OnSurfaceVariant
            )
        }
    }
}

// 模拟搜索功能（实际应用中连接数据库）
private fun performSearch(
    query: String,
    currentResults: List<Word>,
    onResults: (List<Word>) -> Unit
) {
    // 简化示例，实际需要从数据库查询
    val sampleResults = listOf(
        Word(
            id = 1,
            word = "unhappy",
            phonetic = "/ʌnˈhæpi/",
            meaning = "adj. 不快乐的",
            example = "She looked unhappy.",
            exampleTranslation = "她看起来不高兴。",
            level = 1,
            prefix = "un-",
            root = "happ",
            suffix = "-y",
            rootMeaning = "运气好运"
        ),
        Word(
            id = 2,
            word = "unable",
            phonetic = "/ʌnˈeɪbəl/",
            meaning = "adj. 不会的；不能的",
            example = "I am unable to attend the meeting.",
            exampleTranslation = "我无法参加会议。",
            level = 1,
            prefix = "un-",
            root = "able",
            suffix = null,
            rootMeaning = "能够"
        )
    )
    
    if (query.isNotEmpty()) {
        onResults(sampleResults.filter {
            it.word.contains(query, ignoreCase = true) ||
            it.meaning.contains(query, ignoreCase = true)
        })
    }
}

private fun performMorphologySearch(
    query: String,
    currentResults: List<Word>,
    onResults: (List<Word>) -> Unit
) {
    val sampleMorphologyResults = listOf(
        Word(
            id = 3,
            word = "beautiful",
            phonetic = "/ˈbjuːtɪfʊl/",
            meaning = "adj. 美丽的",
            example = "What a beautiful day!",
            exampleTranslation = "多么美好的一天！",
            level = 1,
            prefix = null,
            root = "beaut",
            suffix = "-ful",
            rootMeaning = "美"
        ),
        Word(
            id = 4,
            word = "wonderful",
            phonetic = "/ˈwʌndərfʊl/",
            meaning = "adj. 精彩的；奇妙的",
            example = "The show was wonderful.",
            exampleTranslation = "演出很精彩。",
            level = 2,
            prefix = null,
            root = "wonder",
            suffix = "-ful",
            rootMeaning = "惊奇"
        ),
        Word(
            id = 5,
            word = "careful",
            phonetic = "/ˈkeərfʊl/",
            meaning = "adj. 小心的",
            example = "Be careful!",
            exampleTranslation = "小心！",
            level = 1,
            prefix = null,
            root = "care",
            suffix = "-ful",
            rootMeaning = "关心"
        )
    )
    
    if (query.isNotEmpty()) {
        onResults(sampleMorphologyResults.filter {
            it.prefix?.contains(query, ignoreCase = true) == true ||
            it.suffix?.contains(query, ignoreCase = true) == true ||
            it.root?.contains(query, ignoreCase = true) == true
        })
    }
}

// OCR文字识别
private fun recognizeTextFromImage(bitmap: Bitmap) {
    // 注意：这需要在后台线程执行
    // 实际应用中需要实现完整的OCR逻辑
    try {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
        val image = InputImage.fromBitmap(bitmap, 0)
        
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // 处理识别的文本
                val recognizedText = visionText.text
                // 更新UI
            }
            .addOnFailureListener { e ->
                // 处理错误
            }
    } catch (e: Exception) {
        // 处理异常
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
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
