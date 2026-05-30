package com.englishlearning.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.englishlearning.app.audio.AudioPlayerManager
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.ui.theme.*
import com.englishlearning.app.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    audioPlayer: AudioPlayerManager,
    onBack: () -> Unit = {}
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val selectedWord by viewModel.selectedWord.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔍 智能查询") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // 搜索栏
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入单词查询...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search(); focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            // 词根快捷查询
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("un-", "re-", "-tion", "-ful").forEach { morph ->
                    AssistChip(
                        onClick = { viewModel.searchByMorphology(morph.replace("-", "")) },
                        label = { Text(morph) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (results.isEmpty() && query.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("输入单词或词根进行搜索", color = OnSurfaceVariant)
                }
            } else if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = OnSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("未找到相关单词", color = OnSurfaceVariant)
                    }
                }
            } else {
                Text("找到 ${results.size} 个结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results) { word ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            onClick = { viewModel.selectWord(word) }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(word.word, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(8.dp))
                                        Text(word.phonetic, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                    Text(word.meaning, style = MaterialTheme.typography.bodyMedium, color = Primary)
                                }
                                IconButton(onClick = { audioPlayer.speak(word.word) }) {
                                    Icon(Icons.Default.VolumeUp, "播放", tint = Primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 单词详情弹窗
    selectedWord?.let { word ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSelection() },
            title = {
                Column {
                    Text(word.word, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(word.phonetic, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
                }
            },
            text = {
                Column {
                    Text("📖 ${word.meaning}", style = MaterialTheme.typography.bodyLarge)
                    if (word.example.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("💬 ${word.example}", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearSelection() }) { Text("知道了") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.addToVocabularyBook(word) }) {
                    Text("加入生词本")
                }
            }
        )
    }
}
