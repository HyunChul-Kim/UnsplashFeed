package com.hyunchul.unsplashfeed.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hyunchul.unsplashfeed.data.repository.PhotoRepository
import com.hyunchul.unsplashfeed.domain.model.FeedPhotoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    repository: PhotoRepository,
): ViewModel() {
    val photos: Flow<PagingData<FeedPhotoItem>> = repository.getPagedPhotos()
            .cachedIn(viewModelScope)
}