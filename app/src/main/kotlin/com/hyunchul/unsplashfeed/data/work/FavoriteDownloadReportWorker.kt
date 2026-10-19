package com.hyunchul.unsplashfeed.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hyunchul.unsplashfeed.data.local.dao.FavoritePhotoDao
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import com.hyunchul.unsplashfeed.data.remote.download.PhotoDownloadReportHttpException
import com.hyunchul.unsplashfeed.data.remote.download.PhotoDownloadReporter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException

@HiltWorker
class FavoriteDownloadReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted parameters: WorkerParameters,
    private val dao: FavoritePhotoDao,
    private val reporter: PhotoDownloadReporter,
): CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        val photoId = inputData.getString(FavoriteWorkScheduler.KEY_PHOTO_ID)
            ?.takeIf { it.isNotBlank() } ?: return Result.failure()
        val requestId = inputData.getString(FavoriteWorkScheduler.KEY_REQUEST_ID)
            ?.takeIf { it.isNotBlank() } ?: return Result.failure()

        val request = try {
            dao.findFile(photoId)
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            currentCoroutineContext().ensureActive()
            return Result.retry()
        }

        currentCoroutineContext().ensureActive()
        if(
            request == null ||
            request.requestId != requestId ||
            request.state != PhotoFileState.PENDING
        ) {
            return Result.success()
        }

        val downloadLocation = request.downloadLocation?.takeIf { it.isNotBlank() }
            ?: return markFailed(photoId, requestId, "REPORT_LOCATION_MISSING")

        return try {
            reporter.report(downloadLocation)
            currentCoroutineContext().ensureActive()
            Result.success()
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            currentCoroutineContext().ensureActive()
            if(isRetryable(e) && runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                val code = when(e) {
                    is PhotoDownloadReportHttpException -> "REPORT_HTTP_${e.statusCode}"
                    is IOException -> "REPORT_IO"
                    is IllegalArgumentException -> "REPORT_LOCATION_INVALID"
                    else -> "REPORT_UNEXPECTED"
                }
                markFailed(photoId, requestId, code)
            }
        }
    }

    private suspend fun markFailed(
        photoId: String,
        requestId: String,
        code: String,
    ): Result {
        currentCoroutineContext().ensureActive()

        return try {
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

    private fun isRetryable(failure: Exception): Boolean = when(failure) {
        is PhotoDownloadReportHttpException -> failure.statusCode in setOf(408, 429) || failure.statusCode in 500..599
        is IOException -> true
        else -> false
    }

    private companion object {
        const val TAG = "FavoriteDownloadReport"
        const val MAX_RETRY_COUNT = 2
    }
}