package com.hyunchul.unsplashfeed.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import com.hyunchul.unsplashfeed.data.repository.FavoriteRepository
import com.hyunchul.unsplashfeed.data.repository.PhotoRepository
import com.hyunchul.unsplashfeed.domain.error.PhotoLoadException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    private val favoriteRepository: FavoriteRepository,
): ViewModel() {

    private val photoId = savedStateHandle.toRoute<PhotoDetailDestination>().photoId

    private val _uiState = MutableStateFlow(PhotoDetailUiState())
    val uiState: StateFlow<PhotoDetailUiState> = _uiState.asStateFlow()

    private val messageChannel = Channel<String>()
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    private var photoJob: Job? = null
    private var favoriteJob: Job? = null

    init {
        loadPhoto()
        reloadFavoriteState()
    }

    fun loadPhoto() {
        if(photoJob?.isActive == true) return

        photoJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(content = PhotoDetailContent.Loading)
            }
            try {
                val photo = photoRepository.getPhoto(photoId)
                _uiState.update { state ->
                    state.copy(content = PhotoDetailContent.Success(photo))
                }
            } catch(e: CancellationException) {
                throw e
            } catch(e: PhotoLoadException) {
                _uiState.update { state ->
                    state.copy(content = PhotoDetailContent.Error(e))
                }
            }
        }
    }

    fun reloadFavoriteState() {
        if(favoriteJob?.isActive == true) return
        favoriteJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    favorite = state.favorite.copy(
                        loaded = false,
                        loadFailed = false,
                    )
                )
            }
            try {
                favoriteRepository.observeWithFiles()
                    .map { rows ->
                        rows.firstOrNull { it.favorite.photoId == photoId }
                    }
                    .distinctUntilChanged()
                    .collect { row ->
                        _uiState.update { state ->
                            state.copy(
                                favorite = PhotoDetailFavoriteState(
                                    loaded = true,
                                    isFavorite = row != null,
                                    requestId = row?.file?.requestId,
                                    fileState = row?.file?.state,
                                ),
                            )
                        }
                    }
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        favorite = state.favorite.copy(
                            loaded = false,
                            loadFailed = true,
                        )
                    )
                }
            }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        val photo = (state.content as? PhotoDetailContent.Success)?.photo ?: return
        if(!state.favorite.loaded) return

        viewModelScope.launch {
            try {
                favoriteRepository.toggleFavorite(photo)
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                messageChannel.send("좋아요 변경에 실패했습니다.")
            }
        }
    }

    fun retryDownload() {
        val favorite = _uiState.value.favorite
        if(!favorite.loaded || favorite.fileState != PhotoFileState.FAILED) return
        val requestId = favorite.requestId ?: return

        viewModelScope.launch {
            try {
                favoriteRepository.retryDownload(photoId, requestId)
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                messageChannel.send("사진 저장에 실패했습니다.")
            }
        }
    }
}