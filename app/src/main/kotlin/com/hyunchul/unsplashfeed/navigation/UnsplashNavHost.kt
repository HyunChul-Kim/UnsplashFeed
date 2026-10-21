package com.hyunchul.unsplashfeed.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hyunchul.unsplashfeed.feature.detail.PhotoDetailDestination
import com.hyunchul.unsplashfeed.feature.detail.PhotoDetailRoute
import com.hyunchul.unsplashfeed.feature.favorites.FavoritesDestination
import com.hyunchul.unsplashfeed.feature.favorites.FavoritesRoute
import com.hyunchul.unsplashfeed.feature.feed.FeedDestination
import com.hyunchul.unsplashfeed.feature.feed.FeedRoute
import kotlinx.serialization.Serializable

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
                    navController.runIfCurrent(entry) {
                        navigate(FavoritesDestination) {
                            launchSingleTop = true
                        }
                    }
                },
                onPhotoClick = { photoId ->
                    navController.runIfCurrent(entry) {
                        navigate(PhotoDetailDestination(photoId)) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable<FavoritesDestination> { entry ->
            FavoritesRoute(
                onBack = {
                    navController.runIfCurrent(entry) {
                        popBackStack()
                    }
                }
            )
        }

        composable<PhotoDetailDestination> { entry ->
            PhotoDetailRoute(
                onBack = {
                    navController.runIfCurrent(entry) {
                        popBackStack()
                    }
                }
            )
        }
    }
}

private fun NavController.runIfCurrent(
    sourceEntry: NavBackStackEntry,
    action: NavController.() -> Unit,
) {
    if(currentBackStackEntry == sourceEntry) {
        action()
    }
}