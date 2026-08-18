package tech.salev.optimum.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import tech.salev.optimum.ui.screens.AnalyticsEvaluationScreen
import tech.salev.optimum.ui.screens.CategoryManagerScreen
import tech.salev.optimum.ui.screens.HomeScreen
import tech.salev.optimum.ui.screens.OnboardingScreen
import tech.salev.optimum.ui.screens.SettingsScreen
import tech.salev.optimum.ui.viewmodel.OptimumViewModel
import tech.salev.optimum.ui.viewmodel.SettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import tech.salev.optimum.ui.viewmodel.ProfileViewModel

@Composable
fun MainAppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: OptimumViewModel
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val categoryManagerViewModel: tech.salev.optimum.ui.viewmodel.CategoryManagerViewModel = hiltViewModel()
    val homeViewModel: tech.salev.optimum.ui.viewmodel.HomeViewModel = hiltViewModel()
    val evaluationViewModel: tech.salev.optimum.ui.viewmodel.EvaluationViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    // Observe onboarding state — drives start destination
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

    // 4 Primary Bottom Bar Tabs: Çizelge, Kategoriler, Analiz, Ayarlar
    val items = listOf(
        Screen.Home,
        Screen.Categories,
        Screen.Analytics,
        Screen.Settings
    )

    // While DataStore hasn't emitted yet, default is 'true' (no flash of onboarding)
    val startDest: Any = if (isOnboardingCompleted) Screen.Home else Screen.Onboarding

    Scaffold(
        bottomBar = {
            // Only show bottom bar when not on onboarding
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isOnboarding = currentDestination?.hasRoute<Screen.Onboarding>() == true
            if (!isOnboarding) {
                NavigationBar {
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hasRoute(screen::class) == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(screen) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable<Screen.Onboarding> {
                OnboardingScreen(
                    onFinish = {
                        viewModel.completeOnboarding()
                        navController.navigate(Screen.Home) {
                            popUpTo<Screen.Onboarding> { inclusive = true }
                        }
                    }
                )
            }
            composable<Screen.Home> {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    optimumViewModel = viewModel,
                    categoryManagerViewModel = categoryManagerViewModel,
                    onNavigateToCategories = {
                        navController.navigate(Screen.Categories) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable<Screen.Categories> {
                CategoryManagerScreen(viewModel = viewModel, categoryManagerViewModel = categoryManagerViewModel)
            }
            composable<Screen.Analytics> {
                AnalyticsEvaluationScreen(optimumViewModel = viewModel)
            }
            composable<Screen.Settings> {
                SettingsScreen(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    evaluationViewModel = evaluationViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}
