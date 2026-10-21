package com.hyunchul.unsplashfeed.feature.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PhotoDetailRoute(
    onBack: () -> Unit,
    viewModel: PhotoDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    PhotoDetailScreen(
        state = state,
        onBack = onBack,
        onRetryPhoto = viewModel::loadPhoto,
        onToggleFavorite = viewModel::toggleFavorite,
        onRetryFavoriteState = viewModel::reloadFavoriteState,
        onRetryDownload = viewModel::retryDownload,
        snackbarHostState = snackbarHostState,
    )
}