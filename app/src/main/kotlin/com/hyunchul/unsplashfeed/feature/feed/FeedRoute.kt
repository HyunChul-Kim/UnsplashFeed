package com.hyunchul.unsplashfeed.feature.feed

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun FeedRoute(
    onOpenFavorites: () -> Unit,
    onPhotoClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val photos = viewModel.photos.collectAsLazyPagingItems()
    val favoriteState by viewModel.favoriteState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    FeedScreen(
        photos = photos,
        favoriteState = favoriteState,
        onFavoriteClick = viewModel::toggleFavorite,
        snackbarHostState = snackbarHostState,
        onOpenFavorites = onOpenFavorites,
        onPhotoClick = onPhotoClick,
    )
}
