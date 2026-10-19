package com.hyunchul.unsplashfeed.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hyunchul.unsplashfeed.data.download.PendingPhotoFileCleaner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@HiltWorker
class FavoriteFileCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted parameters: WorkerParameters,
    private val cleaner: PendingPhotoFileCleaner,
): CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        return try {
            cleaner.clean()
            Result.success()
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            currentCoroutineContext().ensureActive()
            if(runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.success(workDataOf("has_deferred_cleanup" to true))
            }
        }
    }
}