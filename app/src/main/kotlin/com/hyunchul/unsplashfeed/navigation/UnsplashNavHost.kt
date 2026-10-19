package com.hyunchul.unsplashfeed.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hyunchul.unsplashfeed.feature.favorites.FavoritesRoute
import com.hyunchul.unsplashfeed.feature.feed.FeedRoute
import kotlinx.serialization.Serializable

@Serializable
data object FeedDestination

@Serializable
data object FavoritesDestination

@Composable
fun UnsplashNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = FeedDestination,
        modifier = modifier,
    ) {
        composable<FeedDestination> {entry ->
            FeedRoute(
                onOpenFavorites = {
                    if(navController.currentBackStackEntry == entry) {
                        navController.navigate(FavoritesDestination) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable<FavoritesDestination> { entry ->
            FavoritesRoute(
                onBack = {
                    if(navController.currentBackStackEntry == entry) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}