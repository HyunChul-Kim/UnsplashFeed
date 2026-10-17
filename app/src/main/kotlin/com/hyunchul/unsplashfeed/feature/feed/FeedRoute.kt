package com.hyunchul.unsplashfeed.feature.feed

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.hyunchul.unsplashfeed.domain.model.FeedPhotoItem
import com.hyunchul.unsplashfeed.domain.model.Photo

@Composable
fun FeedRoute(
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val photos = viewModel.photos.collectAsLazyPagingItems()

    FeedScreen(
        photos = photos,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    photos: LazyPagingItems<FeedPhotoItem>,
) {
    val refreshState = photos.loadState.refresh
    val appendState = photos.loadState.append

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unsplash Explorer") }
            )
        }
    ) { innerPadding ->

        when {
            photos.itemCount == 0 &&
                    refreshState is LoadState.Loading -> {
                        FeedLoading(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    }

            photos.itemCount == 0 &&
                    refreshState is LoadState.Error -> {
                        FeedMessage(
                            message = "사진을 불러오지 못했습니다.",
                            onRetry = { photos.retry() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    }

            photos.itemCount == 0 -> {
                FeedMessage(
                    message = "표시할 사진이 없습니다.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if(refreshState is LoadState.Loading) {
                        item(
                            span = {
                                GridItemSpan(maxLineSpan)
                            },
                        ) {
                            FeedLoading(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                            )
                        }
                    }

                    if(refreshState is LoadState.Error) {
                        item(
                            span = {
                                GridItemSpan(maxLineSpan)
                            },
                        ) {
                            FeedMessage(
                                message = "사진을 새로고침하지 못했습니다.",
                                onRetry = { photos.retry() },
                            )
                        }
                    }

                    items(
                        count = photos.itemCount,
                        key = photos.itemKey { it.entryKey },
                    ) { index ->
                        photos[index]?.let { entry ->
                            PhotoCard(
                                photo = entry.photo,
                            )
                        }
                    }

                    when(appendState) {
                        is LoadState.Loading -> {
                            item(
                                span = {
                                    GridItemSpan(maxLineSpan)
                                },
                            ) {
                                FeedLoading(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                )
                            }
                        }

                        is LoadState.Error -> {
                            item(
                                span = {
                                    GridItemSpan(maxLineSpan)
                                },
                            ) {
                                FeedMessage(
                                    message = "추가 사진을 불러오지 못했습니다.",
                                    onRetry = { photos.retry() },
                                )
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(
    photo: Photo,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            AsyncImage(
                model = photo.thumbnailUrl,
                contentDescription = photo.altDescription
                    ?: photo.description
                    ?: "photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f),
                contentScale = ContentScale.Crop,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = photo.photographerName
                            .take(1)
                            .uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                    )

                    if(photo.photographerProfileUrl.isNotBlank()) {
                        AsyncImage(
                            model = photo.photographerProfileUrl,
                            contentDescription = "profile image",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = photo.photographerName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(4.dp))

                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "favorite icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FeedMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)

        if(onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("다시 시도")
            }
        }
    }
}
