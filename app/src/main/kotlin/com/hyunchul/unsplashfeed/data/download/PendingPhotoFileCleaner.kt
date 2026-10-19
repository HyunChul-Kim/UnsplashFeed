package com.hyunchul.unsplashfeed.data.download

import com.hyunchul.unsplashfeed.data.local.dao.FavoritePhotoDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class PendingPhotoFileCleaner(
    private val dao: FavoritePhotoDao,
    private val deleteFile: suspend (String) -> Unit,
) {

    suspend fun clean(): Int {
        currentCoroutineContext().ensureActive()
        val pending = dao.pendingFileDeletions()
        var cleared = 0
        var firstFailure: Exception? = null

        for(entry in pending) {
            currentCoroutineContext().ensureActive()

            try {
                check(!dao.isReferenced(entry.relativePath))
                deleteFile(entry.relativePath)
                currentCoroutineContext().ensureActive()

                cleared += dao.acknowledgeFileDeleted(entry.relativePath)
            } catch(e: CancellationException) {
                throw e
            } catch(failure: Exception) {
                currentCoroutineContext().ensureActive()

                if(firstFailure == null) {
                    firstFailure = failure
                } else {
                    firstFailure.addSuppressed(failure)
                }
            }
        }

        currentCoroutineContext().ensureActive()
        firstFailure?.let { throw it }
        return cleared
    }
}