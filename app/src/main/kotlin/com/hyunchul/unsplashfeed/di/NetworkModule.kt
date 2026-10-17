package com.hyunchul.unsplashfeed.di

import com.hyunchul.unsplashfeed.BuildConfig
import com.hyunchul.unsplashfeed.data.remote.createUnsplashClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return createUnsplashClient(
            accessKey = BuildConfig.UNSPLASH_ACCESS_KEY,
        )
    }
}