package com.hyunchul.unsplashfeed.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import com.hyunchul.unsplashfeed.domain.model.Photo
import com.hyunchul.unsplashfeed.feature.feed.PhotoFavoriteButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    state: PhotoDetailUiState,
    onBack: () -> Unit,
    onRetryPhoto: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRetryFavoriteState: () -> Unit,
    onRetryDownload: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Photo Detail")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "뒤로 가기",
                        )
                    }
                },
                actions = {
                    if(state.content is PhotoDetailContent.Success) {
                        PhotoFavoriteButton(
                            isFavorite = state.favorite.isFavorite,
                            isPreparing = state.favorite.fileState == PhotoFileState.PENDING,
                            enabled = state.favorite.loaded,
                            onClick = onToggleFavorite,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when(val content = state.content) {
                PhotoDetailContent.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is PhotoDetailContent.Error -> {
                    DetailMessage(
                        message = "사진 정보를 불러오는데 실패했습니다.",
                        button = "다시 시도",
                        onClick = onRetryPhoto,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is PhotoDetailContent.Success -> {
                    val photo = content.photo
                    LazyColumn(
                        modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        item(key = "image") {
                            DetailImage(photo)
                        }
                        item(key = "photographer") {
                            DetailPhotographer(photo)
                        }
                        item(key = "description") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = photo.description?.takeIf { it.isNotBlank() }
                                        ?: photo.altDescription?.takeIf { it.isNotBlank() }
                                        ?: "등록된 설명이 없습니다."
                                )
                            }
                        }
                        item(key = "favorite_state") {
                            FavoriteStatus(
                                favorite = state.favorite,
                                onRetryLoad = onRetryFavoriteState,
                                onRetryDownload = onRetryDownload,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailImage(photo: Photo) {
    val width = photo.width
    val height = photo.height
    val aspectRatio = if(width > 0 && height > 0) {
        width.toFloat() / height
    } else {
        4f / 5f
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val displayWidth = with(LocalDensity.current) {
            maxWidth.roundToPx()
        }
        val url = remember(photo.rawUrl, displayWidth, width) {
            detailImageUrl(photo.rawUrl, displayWidth, width)
        }
        var retryAttempt by remember(url) {
            mutableIntStateOf(0)
        }
        key(url, retryAttempt) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = photo.altDescription ?: photo.description ?: "photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    DetailMessage(
                        message = "이미지를 불러오는데 실패했습니다.",
                        button = "이미지 불러오기",
                        onClick = { retryAttempt++ },
                        modifier = Modifier.fillMaxSize(),
                    )
                },
            )
        }
    }
}

@Composable
private fun DetailPhotographer(photo: Photo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(photo.photographerName.take(1).uppercase())
            photo.photographerProfileUrl.takeIf { it.isNotBlank() }?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = photo.photographerName,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun FavoriteStatus(
    favorite: PhotoDetailFavoriteState,
    onRetryLoad: () -> Unit,
    onRetryDownload: () -> Unit,
    modifier: Modifier,
) {
    when {
        favorite.loadFailed -> DetailMessage(
            message = "좋아요 상태를 불러오는데 실패했습니다.",
            button = "좋아요 상태 불러오기",
            onClick = onRetryLoad,
            modifier = modifier,
        )

        !favorite.loaded -> Text("좋아요 상태 확인 중", modifier)

        !favorite.isFavorite -> Unit

        favorite.fileState == PhotoFileState.FAILED -> DetailMessage(
            message = "사진 저장에 실패했습니다.",
            button = "사진 저장",
            onClick = onRetryDownload,
            modifier = modifier,
        )
    }
}

@Composable
private fun DetailMessage(
    message: String,
    button: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
        TextButton(onClick = onClick) {
            Text(button)
        }
    }
}