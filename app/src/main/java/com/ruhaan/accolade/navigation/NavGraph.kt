package com.ruhaan.accolade.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ruhaan.accolade.domain.model.MediaType
import com.ruhaan.accolade.presentation.category.AllCategoriesScreen
import com.ruhaan.accolade.presentation.category.CategoryScreen
import com.ruhaan.accolade.presentation.common.NavigationAnimations
import com.ruhaan.accolade.presentation.detail.DetailScreen
import com.ruhaan.accolade.presentation.detail.components.CastCrewScreen
import com.ruhaan.accolade.presentation.detail.components.CastCrewScreenType
import com.ruhaan.accolade.presentation.filmography.FilmographyScreen
import com.ruhaan.accolade.presentation.home.HomeScreen
import com.ruhaan.accolade.presentation.home.components.FloatingBottomBar
import com.ruhaan.accolade.presentation.schedule.ScheduleScreen
import kotlinx.coroutines.delay

private val bottomBarHiddenRoutes = listOf("detail", "castcrew", "filmography")
private const val BACK_PRESS_THRESHOLD = 2
private const val BAR_AUTO_HIDE_DELAY = 4_000L

@SuppressLint("RestrictedApi")
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route
    val isHiddenRoute = bottomBarHiddenRoutes.any { currentRoute?.startsWith(it) == true }

    // Counts consecutive back presses on hidden-route screens
    var backPressCount by remember { mutableIntStateOf(0) }
    var escapeBarVisible by remember { mutableStateOf(false) }

    val backStack by navController.currentBackStack.collectAsState()

    // Track stack size changes to detect back presses vs forward navigation
    var lastStackSize by remember { mutableIntStateOf(backStack.size) }

    LaunchedEffect(backStack.size) {
        val currentSize = backStack.size
        if (currentSize < lastStackSize && isHiddenRoute) {
            // User pressed back while on a hidden-route screen
            backPressCount++
            if (backPressCount >= BACK_PRESS_THRESHOLD) {
                escapeBarVisible = true
            }
        } else if (currentSize > lastStackSize) {
            // User navigated forward — reset everything
            backPressCount = 0
            escapeBarVisible = false
        }
        lastStackSize = currentSize
    }

    // Reset when leaving hidden routes (e.g. they eventually backed all the way home)
    LaunchedEffect(isHiddenRoute) {
        if (!isHiddenRoute) {
            backPressCount = 0
            escapeBarVisible = false
        }
    }

    // Auto-hide the escape bar after a few seconds
    LaunchedEffect(escapeBarVisible) {
        if (escapeBarVisible) {
            delay(BAR_AUTO_HIDE_DELAY)
            escapeBarVisible = false
            backPressCount = 0
        }
    }

    val showBottomBar = !isHiddenRoute || escapeBarVisible

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(
                route = "home",
                enterTransition = NavigationAnimations.mainScreenEnter(),
                exitTransition = NavigationAnimations.mainScreenExit(),
                popEnterTransition = NavigationAnimations.mainScreenPopEnter(),
                popExitTransition = NavigationAnimations.mainScreenPopExit(),
            ) { HomeScreen(navController = navController) }

            composable(
                route = "schedule",
                enterTransition = NavigationAnimations.mainScreenEnter(),
                exitTransition = NavigationAnimations.mainScreenExit(),
                popEnterTransition = NavigationAnimations.mainScreenPopEnter(),
                popExitTransition = NavigationAnimations.mainScreenPopExit(),
            ) { ScheduleScreen(navController = navController) }

            composable(
                route = "category",
                enterTransition = NavigationAnimations.mainScreenEnter(),
                exitTransition = NavigationAnimations.mainScreenExit(),
                popEnterTransition = NavigationAnimations.mainScreenPopEnter(),
                popExitTransition = NavigationAnimations.mainScreenPopExit(),
            ) { AllCategoriesScreen(navController = navController) }

            composable(
                route = "detail/{movieId}/{mediaType}",
                arguments = listOf(
                    navArgument("movieId") { type = NavType.IntType },
                    navArgument("mediaType") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
                val mediaType = MediaType.valueOf(
                    backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
                )
                DetailScreen(navController = navController, movieId = movieId, mediaType = mediaType)
            }

            composable(
                route = "castcrew/{movieId}/{mediaType}/{screenType}",
                arguments = listOf(
                    navArgument("movieId") { type = NavType.IntType },
                    navArgument("mediaType") { type = NavType.StringType },
                    navArgument("screenType") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getInt("movieId") ?: return@composable
                val mediaType = MediaType.valueOf(
                    backStackEntry.arguments?.getString("mediaType")?.uppercase() ?: return@composable
                )
                val screenType = CastCrewScreenType.valueOf(
                    backStackEntry.arguments?.getString("screenType")?.uppercase() ?: return@composable
                )
                CastCrewScreen(
                    navController = navController,
                    movieId = movieId,
                    mediaType = mediaType,
                    screenType = screenType,
                    onPersonClick = { personId -> navController.navigate("filmography/$personId") },
                )
            }

            composable("filmography/{personId}") { backStackEntry ->
                val personId = backStackEntry.arguments?.getString("personId")?.toInt()
                    ?: return@composable
                FilmographyScreen(navController = navController, personId = personId)
            }

            composable(
                route = "category/{genreId}/{genreName}",
                arguments = listOf(
                    navArgument("genreId") { type = NavType.IntType },
                    navArgument("genreName") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val genreId = backStackEntry.arguments?.getInt("genreId") ?: 0
                val genreName = backStackEntry.arguments?.getString("genreName") ?: "Category"
                CategoryScreen(navController = navController, genreId = genreId, genreName = genreName)
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FloatingBottomBar(navController = navController)
        }
    }
}