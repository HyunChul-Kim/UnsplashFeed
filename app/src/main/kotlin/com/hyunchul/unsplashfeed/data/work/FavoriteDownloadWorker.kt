package com.hyunchul.unsplashfeed.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hyunchul.unsplashfeed.data.download.FavoriteDownloadTask
import com.hyunchul.unsplashfeed.data.local.dao.FavoritePhotoDao
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import com.hyunchul.unsplashfeed.data.remote.download.PhotoDownloadHttpException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

@HiltWorker
class FavoriteDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted parameters: WorkerParameters,
    private val task: FavoriteDownloadTask,
    private val dao: FavoritePhotoDao,
    private val scheduler: FavoriteWorkScheduler,
): CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        val photoId = inputData.getString(FavoriteWorkScheduler.KEY_PHOTO_ID)
            ?.takeIf { it.isNotBlank() } ?: return Result.failure()
        val requestId = inputData.getString(FavoriteWorkScheduler.KEY_REQUEST_ID)
            ?.takeIf { it.isNotBlank() } ?: return Result.failure()

        return try {
            task.execute(photoId, requestId)
            Result.success()
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            currentCoroutineContext().ensureActive()
            handleFailure(photoId, requestId, e)
        } finally {
            withContext(NonCancellable) {
                try {
                    scheduler.enqueueCleanup()
                } catch(e: Exception) {
                    Log.e(TAG, "Cleanup scheduling failed: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    private suspend fun handleFailure(
        photoId: String,
        requestId: String,
        failure: Exception,
    ): Result {
        return try {
            val current = dao.findFile(photoId)
            if(
                current == null ||
                current.requestId != requestId ||
                current.state != PhotoFileState.PENDING
            ) {
                return Result.success()
            }

            if(isRetryable(failure) && runAttemptCount < MAX_RETRY_COUNT) {
                return Result.retry()
            }

            currentCoroutineContext().ensureActive()
            val code = when(failure) {
                is PhotoDownloadHttpException -> "HTTP_${failure.statusCode}"
                is IOException -> "IO"
                else -> "UNEXPECTED"
            }
            val updated = dao.markFailed(photoId, requestId, code)
            if(updated == 0) {
                Result.success()
            } else {
                Result.failure(workDataOf("failure_code" to code))
            }
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            currentCoroutineContext().ensureActive()
            Result.retry()
        }
    }

    private fun isRetryable(
        failure: Exception,
    ): Boolean = when(failure) {
        is PhotoDownloadHttpException -> failure.statusCode in setOf(408, 429) || failure.statusCode in 500..599
        is IOException -> true
        else -> false
    }

    private companion object {
        const val TAG = "FavoriteDownload"
        const val MAX_RETRY_COUNT = 2
    }
}