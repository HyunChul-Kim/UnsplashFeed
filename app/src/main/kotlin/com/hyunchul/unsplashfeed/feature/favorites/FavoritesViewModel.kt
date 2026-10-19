package com.hyunchul.unsplashfeed.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyunchul.unsplashfeed.data.local.file.PhotoFileStore
import com.hyunchul.unsplashfeed.data.local.model.FavoritePhotoWithFile
import com.hyunchul.unsplashfeed.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: FavoriteRepository,
    private val fileStore: PhotoFileStore,
): ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    private var observationJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        observationJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, loadFailed = false)
        observationJob = viewModelScope.launch {
            try {
                repository.observeWithFiles().collect { rows ->
                    _uiState.value = FavoritesUiState(
                        isLoading = false,
                        items = rows.map { row ->
                            row.toUiItem()
                        },
                    )
                }
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, loadFailed = true)
            }
        }
    }

    fun removeFavorite(
        photoId: String,
    ) {
        if(_uiState.value.isLoading || _uiState.value.loadFailed) return
        viewModelScope.launch {
            try {
                repository.removeFavorite(photoId)
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                messageChannel.send("좋아요 해제에 실패했습니다.")
            }
        }
    }

    fun retryDownload(
        photoId: String,
        requestId: String,
    ) {
        if(_uiState.value.isLoading || _uiState.value.loadFailed) return
        viewModelScope.launch {
            try {
                repository.retryDownload(photoId, requestId)
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                messageChannel.send("사진 저장 요청에 실패했습니다.")
            }
        }
    }

    private fun FavoritePhotoWithFile.toUiItem(): FavoritePhotoUiItem =
        FavoritePhotoUiItem(
            photoId = favorite.photoId,
            requestId = file.requestId,
            photographerName = favorite.photographerName,
            imageDescription = favorite.altDescription ?: favorite.description,
            fileState = file.state,
            localImage = resolveFile(file.localImageRelativePath),
        )

    private fun resolveFile(
        relativePath: String?,
    ): File? {
        if(relativePath == null) return null
        return try {
            fileStore.fileFor(relativePath)
        } catch(e: IllegalArgumentException) {
            null
        }
    }
}