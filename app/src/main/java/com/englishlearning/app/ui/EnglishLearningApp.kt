package com.englishlearning.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.englishlearning.app.ui.home.HomeScreen
import com.englishlearning.app.ui.vocabulary.VocabularyScreen
import com.englishlearning.app.ui.listening.ListeningScreen
import com.englishlearning.app.ui.reading.ReadingScreen
import com.englishlearning.app.ui.search.SearchScreen

// 导航项目
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "首页", Icons.Filled.Home, Icons.Outlined.Home)
    object Vocabulary : Screen("vocabulary", "背单词", Icons.Filled.Book, Icons.Outlined.Book)
    object Listening : Screen("listening", "听力", Icons.Filled.Headphones, Icons.Outlined.Headphones)
    object Reading : Screen("reading", "阅读", Icons.Filled.Article, Icons.Outlined.Article)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishLearningApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 权限结果处理
    }
    
    // 请求必要的权限
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }
    
    val items = listOf(
        Screen.Home,
        Screen.Vocabulary,
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
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Vocabulary.route) { VocabularyScreen() }
            composable(Screen.Listening.route) { ListeningScreen() }
            composable(Screen.Reading.route) { ReadingScreen() }
        }
    }
}
