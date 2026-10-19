package com.hyunchul.unsplashfeed.feature.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onRemoveFavorite: (photoId: String) -> Unit,
    onRetryDownload: (photoId: String, requestId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readyItems = remember(state.items) {
        state.items.filter { it.canDisplayLocalImage }
    }
    val actionsEnabled = !state.isLoading && !state.loadFailed

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Favorite Photos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)

        when {
            state.isLoading && state.items.isEmpty() -> {
                Box(
                    modifier = contentModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.loadFailed && state.items.isEmpty() -> {
                FavoritesMessage(
                    message = "좋아요 목록을 읽어오는데 실패했습니다.",
                    modifier = contentModifier,
                    onRetry = onReload,
                )
            }

            state.items.isEmpty() -> {
                FavoritesMessage(
                    message = "좋아요한 사진이 없습니다.",
                    modifier = contentModifier,
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    modifier = contentModifier,
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if(state.loadFailed) {
                        item(
                            key = "favorites:load-error",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            FavoritesMessage(
                                message = "좋아요 목록을 읽어오는데 실패했습니다.",
                                modifier = Modifier.fillMaxWidth(),
                                onRetry = onReload,
                            )
                        }
                    }

                    if(state.isLoading) {
                        item(
                            key = "favorites:loading",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    if(readyItems.isEmpty()) {
                        item(
                            key = "favorites:no-ready",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            FavoritesMessage(
                                message = "다운로드 완료된 사진이 없습니다.",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        items(
                            items = readyItems,
                            key = { "favorites:${it.photoId}" },
                            contentType = { "local-photo" },
                        ) { item ->
                            LocalFavoriteCard(
                                item = item,
                                enabled = actionsEnabled,
                                onRemove = { onRemoveFavorite(item.photoId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalFavoriteCard(
    item: FavoritePhotoUiItem,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    var imageFailed by remember(item.localImage) {
        mutableStateOf(false)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = item.localImage,
                    contentDescription = item.imageDescription ?: "다운로드한 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onLoading = { imageFailed = false },
                    onSuccess = { imageFailed = false },
                    onError = { imageFailed = true },
                )

                if(imageFailed) {
                    Text(
                        text = "사진 불러오기 오류",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            PhotographerRow(item.photographerName, enabled, onRemove)
        }
    }
}

@Composable
private fun FilePreparationRow(
    item: FavoritePhotoUiItem,
    enabled: Boolean,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            PhotographerRow(item.photographerName, enabled, onRemove)

            val message = when(item.fileState) {
                PhotoFileState.PENDING -> "다운로드 준비 중"
                PhotoFileState.FAILED -> "다운로드 실패"
                PhotoFileState.READY -> "사진 경로 확인 실패"
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            )

            if(item.fileState == PhotoFileState.FAILED) {
                TextButton(
                    onClick = onRetry,
                    enabled = enabled,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                ) {
                    Text("다운로드 다시 시도")
                }
            }
        }
    }
}

@Composable
private fun PhotographerRow(
    name: String,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.trim().take(1).ifBlank { "?" },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "좋아요 해제",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FavoritesMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center)
        if(onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                Text("다시 시도")
            }
        }
    }
}