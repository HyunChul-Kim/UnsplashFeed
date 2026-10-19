package com.hyunchul.unsplashfeed

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hyunchul.unsplashfeed.data.repository.FavoriteRepository
import com.hyunchul.unsplashfeed.di.ApplicationScope
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class UnsplashFeedApplication: Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var favoriteRepository: Lazy<FavoriteRepository>
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                favoriteRepository.get().restorePendingWork()
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                Log.e("FavoriteRestore", "Restore failed: ${e.javaClass.simpleName}")
            }
        }
    }
}