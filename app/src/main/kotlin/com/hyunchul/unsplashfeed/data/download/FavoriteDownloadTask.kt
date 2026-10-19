package com.hyunchul.unsplashfeed.data.download

import com.hyunchul.unsplashfeed.data.local.dao.FavoritePhotoDao
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import com.hyunchul.unsplashfeed.data.local.file.PhotoFileStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.OutputStream

class FavoriteDownloadTask(
    private val dao: FavoritePhotoDao,
    private val fileStore: PhotoFileStore,
    private val downloadImage: suspend (String, OutputStream) -> Unit,
) {

    suspend fun execute(
        photoId: String,
        requestId: String,
    ): FavoriteDownloadResult {
        require(photoId.isNotBlank())
        require(requestId.isNotBlank())
        currentCoroutineContext().ensureActive()

        val request = dao.findFile(photoId)
        if(
            request == null ||
            request.requestId != requestId ||
            request.state != PhotoFileState.PENDING
        ) {
           currentCoroutineContext().ensureActive()
           return FavoriteDownloadResult.SKIPPED
        }

        val imageUrl = checkNotNull(request.imageUrl?.takeIf { it.isNotBlank() })
        var completedPath: String? = null

        try {
            val path = fileStore.save { output ->
                downloadImage(imageUrl, output)
            }
            completedPath = path
            currentCoroutineContext().ensureActive()

            val published = withContext(NonCancellable) {
                dao.publishDownloadedFile(photoId, requestId, path)
            }
            currentCoroutineContext().ensureActive()

            return if(published) {
                FavoriteDownloadResult.PUBLISHED
            } else {
                FavoriteDownloadResult.DISCARDED
            }
        } catch(failure: Throwable) {
            completedPath?.let { path ->
                try {
                    withContext(NonCancellable) {
                        dao.queueUnreferencedFile(path)
                    }
                } catch(cleanupFailure: Throwable) {
                    if(cleanupFailure !== failure) {
                        failure.addSuppressed(cleanupFailure)
                    }
                }
            }

            throw failure
        }
    }
}

enum class FavoriteDownloadResult {
    PUBLISHED,
    SKIPPED,
    DISCARDED,
}