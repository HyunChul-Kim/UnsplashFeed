package com.hyunchul.unsplashfeed.data.repository

import com.hyunchul.unsplashfeed.data.local.dao.FavoritePhotoDao
import com.hyunchul.unsplashfeed.data.local.entity.FavoritePhotoEntity
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import com.hyunchul.unsplashfeed.data.local.model.FavoritePhotoWithFile
import com.hyunchul.unsplashfeed.data.work.FavoriteWorkScheduler
import com.hyunchul.unsplashfeed.di.ApplicationScope
import com.hyunchul.unsplashfeed.domain.model.Photo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val dao: FavoritePhotoDao,
    private val scheduler: FavoriteWorkScheduler,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {

    private val mutex = Mutex()

    fun observeWithFiles(): Flow<List<FavoritePhotoWithFile>> =
        dao.observeWithFiles().distinctUntilChanged()

    fun observeReadyPhotos(): Flow<List<FavoritePhotoWithFile>> =
        dao.observeReadyPhotos().distinctUntilChanged()

    suspend fun toggleFavorite(
        photo: Photo,
    ): FavoriteMutationResult = command {
        val changed = dao.toggleFavorite(
            favorite = FavoritePhotoEntity(
                photoId = photo.id,
                photographerName = photo.photographerName,
                photographerProfileImageUrl = photo.photographerProfileUrl,
                description = photo.description,
                altDescription = photo.altDescription,
                createdAt = System.currentTimeMillis(),
            ),
            imageUrl = photo.regularUrl,
            newRequestId = UUID.randomUUID().toString(),
            downloadLocation = photo.downloadLocation,
        )

        var scheduled = true
        changed.requestIdToCancel?.let { requestId ->
            if(!scheduleSafely { scheduler.cancelDownload(requestId) }) {
                scheduled = false
            }
        }
        changed.requestToEnqueue?.let { request ->
            if(!scheduleSafely { scheduler.enqueueDownload(request) }) {
                scheduled = false
            }
        }
        if(!changed.isFavorite && !scheduleSafely { scheduler.enqueueCleanup() }) {
            scheduled = false
        }

        FavoriteMutationResult(
            isFavorite = changed.isFavorite,
            workScheduled = scheduled,
        )
    }

    suspend fun removeFavorite(
        photoId: String,
    ): FavoriteMutationResult = command {
        require(photoId.isNotBlank())

        val previous = dao.removeFavorite(photoId)
        var scheduled = true

        previous?.requestId?.let { requestId ->
            if(!scheduleSafely { scheduler.cancelDownload(requestId) }) {
                scheduled = false
            }
        }

        if(!scheduleSafely { scheduler.enqueueCleanup() }) {
            scheduled = false
        }

        FavoriteMutationResult(
            isFavorite = false,
            workScheduled = scheduled,
        )
    }

    suspend fun retryDownload(
        photoId: String,
        expectedRequestId: String,
    ): Boolean = command {
        val next = dao.retryFailed(
            photoId = photoId,
            expectedRequestId = expectedRequestId,
            newRequestId = UUID.randomUUID().toString(),
        ) ?: return@command true

        scheduleSafely { scheduler.enqueueDownload(next) }
    }

    suspend fun restorePendingWork() {
        val pending = dao.pendingDownloads()

        for((photoId, requestId) in pending) {
            command {
                val current = dao.findFile(photoId)
                if(current?.requestId == requestId && current.state == PhotoFileState.PENDING) {
                    scheduleSafely {
                        scheduler.enqueueDownload(current)
                    }
                }
            }
        }

        scheduleSafely { scheduler.enqueueCleanup() }
    }

    private suspend fun <T> command(
        block: suspend () -> T
    ): T = applicationScope.async(start = CoroutineStart.UNDISPATCHED) {
        mutex.withLock { block() }
    }.await()

    private suspend fun scheduleSafely(
        block: suspend () -> Unit
    ): Boolean = try {
        block()
        true
    } catch(e: CancellationException) {
        throw e
    } catch(e: Exception) {
        false
    }
}

data class FavoriteMutationResult(
    val isFavorite: Boolean,
    val workScheduled: Boolean,
)