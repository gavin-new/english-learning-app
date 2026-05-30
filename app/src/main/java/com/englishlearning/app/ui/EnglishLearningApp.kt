package com.englishlearning.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.englishlearning.app.audio.AudioPlayerManager
import com.englishlearning.app.ui.dictation.DictationScreen
import com.englishlearning.app.ui.home.HomeScreen
import com.englishlearning.app.ui.listening.ListeningScreen
import com.englishlearning.app.ui.phonics.PhonicsLearningScreen
import com.englishlearning.app.ui.reading.ReadingScreen
import com.englishlearning.app.ui.search.SearchScreen
import com.englishlearning.app.ui.vocabulary.VocabularyScreen
import com.englishlearning.app.ui.viewmodel.AppViewModel
import com.englishlearning.app.utils.PhonicsUtils

// 导航路由定义
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "首页", Icons.Filled.Home, Icons.Outlined.Home)
    object Vocabulary : Screen("vocabulary", "背单词", Icons.Filled.Book, Icons.Outlined.Book)
    object Phonics : Screen("phonics", "拼读", Icons.Filled.Spellcheck, Icons.Outlined.Spellcheck)
    object Listening : Screen("listening", "听力", Icons.Filled.Headphones, Icons.Outlined.Headphones)
    object Reading : Screen("reading", "阅读", Icons.Filled.Article, Icons.Outlined.Article)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishLearningApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 全局ViewModel
    val appVM: AppViewModel = viewModel()

    val items = listOf(
        Screen.Home,
        Screen.Vocabulary,
        Screen.Phonics,
        Screen.Listening,
        Screen.Reading
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 首页
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = appVM.homeVM,
                    onNavigateToVocabulary = { navController.navigate(Screen.Vocabulary.route) },
                    onNavigateToListening = { navController.navigate(Screen.Listening.route) },
                    onNavigateToReading = { navController.navigate(Screen.Reading.route) },
                    onNavigateToPhonics = { navController.navigate(Screen.Phonics.route) }
                )
            }

            // 背单词
            composable(Screen.Vocabulary.route) {
                VocabularyScreen(
                    viewModel = appVM.vocabVM,
                    audioPlayer = remember { AudioPlayerManager(context) },
                    onWordClick = { word ->
                        // 跳转到拼读页面
                        appVM.phonicsVM.loadAllWords()
                        appVM.phonicsVM.setCurrentWord(word, 0)
                        navController.navigate(Screen.Phonics.route)
                    }
                )
            }

            // 自然拼读
            composable(Screen.Phonics.route) {
                PhonicsLearningScreen(
                    viewModel = appVM.phonicsVM,
                    audioPlayer = remember { AudioPlayerManager(context) },
                    onStartDictation = {
                        // 启动听写
                        appVM.dictationVM.startNewDictation(10)
                        navController.navigate("dictation")
                    }
                )
            }

            // 听力
            composable(Screen.Listening.route) {
                ListeningScreen()
            }

            // 阅读
            composable(Screen.Reading.route) {
                ReadingScreen()
            }

            // 听写 (独立路由, 不在底部导航)
            composable("dictation") {
                DictationScreen(
                    viewModel = appVM.dictationVM,
                    audioPlayer = remember { AudioPlayerManager(context) },
                    onExit = { navController.popBackStack() }
                )
            }

            // 搜索 (独立路由)
            composable("search") {
                SearchScreen(
                    viewModel = appVM.searchVM,
                    audioPlayer = remember { AudioPlayerManager(context) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
