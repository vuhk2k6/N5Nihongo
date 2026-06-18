package com.vunv.n5nihongo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.vunv.n5nihongo.ui.flashcard.FlashcardLearningRoute
import com.vunv.n5nihongo.ui.home.HomeLearningRoute
import com.vunv.n5nihongo.ui.home.LessonDetailRoute
import com.vunv.n5nihongo.ui.auth.LoginRoute
import com.vunv.n5nihongo.ui.profile.ProfileRoute
import com.vunv.n5nihongo.ui.quiz.QuizRoute
import com.vunv.n5nihongo.ui.splash.SplashRoute
import com.vunv.n5nihongo.ui.navigation.mainEnterTransition
import com.vunv.n5nihongo.ui.navigation.mainExitTransition
import com.vunv.n5nihongo.ui.navigation.mainPopEnterTransition
import com.vunv.n5nihongo.ui.navigation.mainPopExitTransition
import com.vunv.n5nihongo.ui.navigation.rootEnterTransition
import com.vunv.n5nihongo.ui.navigation.rootExitTransition
import com.vunv.n5nihongo.ui.navigation.rootPopEnterTransition
import com.vunv.n5nihongo.ui.navigation.rootPopExitTransition
import com.vunv.n5nihongo.ui.theme.N5NihongoTheme
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vunv.n5nihongo.ui.theme.LightBackground
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SurfaceWhite
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.font.FontWeight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            N5NihongoTheme {
                AppRoot()
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = RootRoute.Splash.route,
        enterTransition = { rootEnterTransition() },
        exitTransition = { rootExitTransition() },
        popEnterTransition = { rootPopEnterTransition() },
        popExitTransition = { rootPopExitTransition() }
    ) {
        composable(RootRoute.Splash.route) {
            SplashRoute(
                onNavigateToLogin = {
                    navController.navigate(RootRoute.Login.route) {
                        popUpTo(RootRoute.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(RootRoute.Main.route) {
                        popUpTo(RootRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(RootRoute.Login.route) {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(RootRoute.Main.route) {
                        popUpTo(RootRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(RootRoute.Main.route) {
            MainBottomNavigationApp(
                onLogout = {
                    navController.navigate(RootRoute.Login.route) {
                        popUpTo(RootRoute.Main.route) { inclusive = true }
                    }
                },
                onNavigateToAi = {
                    navController.navigate(RootRoute.AiAssistant.route)
                },
                onNavigateToMockExam = {
                    navController.navigate("mockExam")
                }
            )
        }
        composable(RootRoute.AiAssistant.route) {
            com.vunv.n5nihongo.ui.ai.AiAssistantScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable("aiQuiz?prompt={prompt}") { backStackEntry ->
            val promptArg = backStackEntry.arguments?.getString("prompt") ?: "Kiểm tra tổng hợp"
            com.vunv.n5nihongo.ui.ai.AiQuizScreen(
                prompt = promptArg,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable("lessonDetail/{lessonId}") { backStackEntry ->
            val lessonIdArg = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull() ?: 1
            LessonDetailRoute(
                lessonId = lessonIdArg,
                onStartQuiz = { lessonId, selectedIds ->
                    val prompt = "trắc nghiệm từ vựng ngữ pháp chữ hán tiếng Nhật N5 Bài $lessonId"
                    navController.navigate("aiQuiz?prompt=$prompt")
                },
                onStartFlashcard = { lessonId, selectedIds ->
                    val selectedParam = selectedIds.joinToString(",")
                    navController.navigate("lessonFlashcard/$lessonId?selectedIds=$selectedParam")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("lessonQuiz/{lessonId}") { backStackEntry ->
            val lessonIdArg = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull() ?: 1
            QuizRoute(
                lessonId = lessonIdArg,
                selectedWordIds = emptyList(),
                onBack = { navController.popBackStack() }
            )
        }
        composable("lessonQuiz/{lessonId}?selectedIds={selectedIds}") { backStackEntry ->
            val lessonIdArg = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull() ?: 1
            val selectedIdsArg = backStackEntry.arguments?.getString("selectedIds").orEmpty()
            val selectedIds = selectedIdsArg
                .split(",")
                .mapNotNull { it.toIntOrNull() }
            QuizRoute(
                lessonId = lessonIdArg,
                selectedWordIds = selectedIds,
                onBack = { navController.popBackStack() }
            )
        }
        composable("lessonFlashcard/{lessonId}?selectedIds={selectedIds}") { backStackEntry ->
            val lessonIdArg = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull() ?: 1
            val selectedIdsArg = backStackEntry.arguments?.getString("selectedIds").orEmpty()
            val selectedIds = selectedIdsArg
                .split(",")
                .mapNotNull { it.toIntOrNull() }
            FlashcardLearningRoute(
                lessonId = lessonIdArg,
                selectedWordIds = selectedIds
            )
        }
        composable("alphabet/{typeId}") { backStackEntry ->
            val typeId = backStackEntry.arguments?.getString("typeId")?.toIntOrNull() ?: 1
            com.vunv.n5nihongo.ui.alphabet.AlphabetMasterRoute(
                typeId = typeId,
                onStartQuiz = { lessonId, selectedIds ->
                    if (selectedIds.isEmpty()) {
                        navController.navigate("lessonQuiz/$lessonId")
                    } else {
                        val selectedParam = selectedIds.joinToString(",")
                        navController.navigate("lessonQuiz/$lessonId?selectedIds=$selectedParam")
                    }
                },
                onStartFlashcard = { lessonId, selectedIds ->
                    val selectedParam = selectedIds.joinToString(",")
                    navController.navigate("lessonFlashcard/$lessonId?selectedIds=$selectedParam")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("numbersTime") {
            com.vunv.n5nihongo.ui.foundation.NumbersTimeRoute(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable("kanji") {
            com.vunv.n5nihongo.ui.kanji.KanjiListRoute(
                onKanjiClick = { kanjiId -> navController.navigate("kanjiDetail/$kanjiId") }
            )
        }
        composable("kanjiDetail/{kanjiId}") { backStackEntry ->
            val kanjiIdArg = backStackEntry.arguments?.getString("kanjiId")?.toIntOrNull() ?: -1
            com.vunv.n5nihongo.ui.kanji.KanjiDetailRoute(
                kanjiId = kanjiIdArg,
                onBack = { navController.popBackStack() }
            )
        }
        composable("leaderboard") {
            com.vunv.n5nihongo.ui.quiz.QuizSelectionScreen(
                onQuizSelected = { lessonId ->
                    navController.navigate("lessonQuiz/$lessonId?selectedIds=")
                },
                onAiQuizSelected = { lessonId ->
                    val prompt = if (lessonId == 3) {
                        "Trắc nghiệm số đếm, giờ giấc, ngày tháng tiếng Nhật N5"
                    } else {
                        "trắc nghiệm từ vựng ngữ pháp chữ hán tiếng Nhật N5 Bài $lessonId"
                    }
                    navController.navigate("aiQuiz?prompt=$prompt")
                },
                onStartMockExam = { navController.navigate("mockExam") }
            )
        }
        composable("mockExam") {
            com.vunv.n5nihongo.ui.quiz.MockExamScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
    }
}

@Composable
private fun MainBottomNavigationApp(
    onLogout: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToMockExam: () -> Unit
) {
    val navController = rememberNavController()
    val navItems = listOf(
        BottomNavItem(route = "aiAssistant", title = "AI Sensei", icon = Icons.Default.AutoAwesome),
        BottomNavItem(route = "home", title = "Lộ trình", icon = Icons.Filled.Home),
        BottomNavItem(route = "kanji", title = "Chữ Hán", icon = Icons.Filled.Edit),
        BottomNavItem(route = "leaderboard", title = "Luyện thi", icon = Icons.Filled.Star),
        BottomNavItem(route = "profile", title = "Hồ sơ", icon = Icons.Filled.Person)
    )

    Scaffold(
        containerColor = LightBackground,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                containerColor = SurfaceWhite.copy(alpha = 0.88f)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { item ->
                    val isSelected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == item.route } == true

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = MintPrimary,
                            selectedTextColor = MintPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        icon = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .height(3.dp)
                                        .fillMaxWidth(0.35f)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MintPrimary)
                                        .alpha(if (isSelected) 1f else 0f)
                                )
                            }
                        },
                        label = { Text(text = item.title, maxLines = 1) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "aiAssistant",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { mainEnterTransition() },
            exitTransition = { mainExitTransition() },
            popEnterTransition = { mainPopEnterTransition() },
            popExitTransition = { mainPopExitTransition() }
        ) {
            composable("aiAssistant") {
                com.vunv.n5nihongo.ui.ai.AiAssistantScreen(
                    showBackButton = false,
                    onBack = {},
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable("aiQuiz?prompt={prompt}") { backStackEntry ->
                val promptArg = backStackEntry.arguments?.getString("prompt") ?: "Kiểm tra tổng hợp"
                com.vunv.n5nihongo.ui.ai.AiQuizScreen(
                    prompt = promptArg,
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable("home") { HomeScreen(navController = navController) }
            composable("lessonDetail/{lessonId}") { backStackEntry ->
                val lessonIdArg = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull() ?: 1
                LessonDetailRoute(
                    lessonId = lessonIdArg,
                    onStartQuiz = { lessonId, selectedIds ->
                        val prompt = "trắc nghiệm từ vựng ngữ pháp chữ hán tiếng Nhật N5 Bài $lessonId"
                        navController.navigate("aiQuiz?prompt=$prompt")
                    },
                    onStartFlashcard = { lessonId, selectedIds ->
                        val selectedParam = selectedIds.joinToString(",")
                        navController.navigate("lessonFlashcard/$lessonId?selectedIds=$selectedParam")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("lessonQuiz/{lessonId}") { backStackEntry ->
                val lessonIdArg = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull() ?: 1
                QuizRoute(
                    lessonId = lessonIdArg,
                    selectedWordIds = emptyList(),
                    onBack = {
                        navController.navigate("aiAssistant") {
                            popUpTo("aiAssistant") { inclusive = false }
                        }
                    }
                )
            }
            composable("lessonQuiz/{lessonId}?selectedIds={selectedIds}") { backStackEntry ->
                val lessonIdArg = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull() ?: 1
                val selectedIdsArg = backStackEntry.arguments?.getString("selectedIds").orEmpty()
                val selectedIds = selectedIdsArg
                    .split(",")
                    .mapNotNull { it.toIntOrNull() }
                QuizRoute(
                    lessonId = lessonIdArg,
                    selectedWordIds = selectedIds,
                    onBack = {
                        navController.navigate("aiAssistant") {
                            popUpTo("aiAssistant") { inclusive = false }
                        }
                    }
                )
            }
            composable("lessonFlashcard/{lessonId}?selectedIds={selectedIds}") { backStackEntry ->
                val lessonIdArg = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull() ?: 1
                val selectedIdsArg = backStackEntry.arguments?.getString("selectedIds").orEmpty()
                val selectedIds = selectedIdsArg
                    .split(",")
                    .mapNotNull { it.toIntOrNull() }
                FlashcardLearningRoute(
                    lessonId = lessonIdArg,
                    selectedWordIds = selectedIds
                )
            }
            composable("alphabet/{typeId}") { backStackEntry ->
                val typeId = backStackEntry.arguments?.getString("typeId")?.toIntOrNull() ?: 1
                com.vunv.n5nihongo.ui.alphabet.AlphabetMasterRoute(
                    typeId = typeId,
                    onStartQuiz = { lessonId, selectedIds ->
                        if (selectedIds.isEmpty()) {
                            navController.navigate("lessonQuiz/$lessonId")
                        } else {
                            val selectedParam = selectedIds.joinToString(",")
                            navController.navigate("lessonQuiz/$lessonId?selectedIds=$selectedParam")
                        }
                    },
                    onStartFlashcard = { lessonId, selectedIds ->
                        val selectedParam = selectedIds.joinToString(",")
                        navController.navigate("lessonFlashcard/$lessonId?selectedIds=$selectedParam")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("numbersTime") {
                com.vunv.n5nihongo.ui.foundation.NumbersTimeRoute(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable("kanji") {
                com.vunv.n5nihongo.ui.kanji.KanjiListRoute(
                    onKanjiClick = { kanjiId -> navController.navigate("kanjiDetail/$kanjiId") }
                )
            }
            composable("kanjiDetail/{kanjiId}") { backStackEntry ->
                val kanjiIdArg = backStackEntry.arguments?.getString("kanjiId")?.toIntOrNull() ?: -1
                com.vunv.n5nihongo.ui.kanji.KanjiDetailRoute(
                    kanjiId = kanjiIdArg,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("leaderboard") { LeaderboardScreen(navController = navController, onStartMockExam = onNavigateToMockExam) }
            composable("profile") { ProfileScreen(onLogout = onLogout, onNavigateToAi = onNavigateToAi) }
        }
    }
}

@Composable
private fun HomeScreen(navController: androidx.navigation.NavHostController) {
    HomeLearningRoute(
        onLessonClick = { lessonId ->
            when (lessonId) {
                1, 2 -> navController.navigate("alphabet/$lessonId")
                3 -> navController.navigate("numbersTime")
                else -> navController.navigate("lessonDetail/$lessonId")
            }
        }
    )
}

@Composable
private fun LeaderboardScreen(
    navController: androidx.navigation.NavHostController,
    onStartMockExam: () -> Unit
) {
    com.vunv.n5nihongo.ui.quiz.QuizSelectionScreen(
        onQuizSelected = { lessonId ->
            navController.navigate("lessonQuiz/$lessonId?selectedIds=")
        },
        onAiQuizSelected = { lessonId ->
            val prompt = if (lessonId == 3) {
                "Trắc nghiệm số đếm, giờ giấc, ngày tháng tiếng Nhật N5"
            } else {
                "trắc nghiệm từ vựng ngữ pháp chữ hán tiếng Nhật N5 Bài $lessonId"
            }
            navController.navigate("aiQuiz?prompt=$prompt")
        },
        onStartMockExam = onStartMockExam
    )
}

@Composable
private fun ProfileScreen(onLogout: () -> Unit, onNavigateToAi: () -> Unit) {
    ProfileRoute(onLogoutSuccess = onLogout, onNavigateToAi = onNavigateToAi)
}

private sealed class RootRoute(val route: String) {
    data object Splash : RootRoute("splash")
    data object Login : RootRoute("login")
    data object Main : RootRoute("main")
    data object AiAssistant : RootRoute("aiAssistant")
}
