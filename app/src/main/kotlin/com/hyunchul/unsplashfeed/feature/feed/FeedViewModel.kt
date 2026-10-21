package com.hyunchul.unsplashfeed.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hyunchul.unsplashfeed.data.repository.FavoriteRepository
import com.hyunchul.unsplashfeed.data.repository.PhotoRepository
import com.hyunchul.unsplashfeed.domain.model.FeedPhotoItem
import com.hyunchul.unsplashfeed.domain.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    repository: PhotoRepository,
    private val favoriteRepository: FavoriteRepository,
): ViewModel() {
    val photos: Flow<PagingData<FeedPhotoItem>> = repository.getPagedPhotos()
            .cachedIn(viewModelScope)

    private val _favoriteState = MutableStateFlow(FeedFavoriteUiState())
    val favoriteState: StateFlow<FeedFavoriteUiState> = _favoriteState.asStateFlow()

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                favoriteRepository.observeWithFiles().collect { rows ->
                    _favoriteState.value = FeedFavoriteUiState(
                        loaded = true,
                        items = rows.associate { row ->
                            row.favorite.photoId to FeedFavoriteItem(row.file.requestId, row.file.state)
                        },
                    )
                }
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                _favoriteState.value = _favoriteState.value.copy(loaded = false)
                messageChannel.send("좋아요 상태를 읽지 못했습니다.")
            }
        }
    }

    fun toggleFavorite(
        photo: Photo
    ) {
        if(!_favoriteState.value.loaded) return
        viewModelScope.launch {
            try {
                favoriteRepository.toggleFavorite(photo)
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                messageChannel.send("좋아요에 실패했습니다.")
            }
        }
    }

}