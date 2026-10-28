package com.mofangvideo.core.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WorkManager 依赖：向 [android.app.Application] 提供 Hilt 化的 [WorkerFactory]。
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkerFactory(factory: HiltWorkerFactory): WorkerFactory = factory
}
