package com.mofangvideo.core.di

import android.content.Context
import com.mofangvideo.core.backup.WebDavBackupRepository
import com.mofangvideo.core.db.FavoriteDao
import com.mofangvideo.core.db.HistoryDao
import com.mofangvideo.core.db.LiveChannelDao
import com.mofangvideo.core.db.LiveSourceDao
import com.mofangvideo.core.db.MofangDatabase
import com.mofangvideo.core.db.VideoSourceDao
import com.mofangvideo.core.repo.BackupRepository
import com.mofangvideo.core.repo.BackupRepositoryImpl
import com.mofangvideo.core.repo.FavoriteRepository
import com.mofangvideo.core.repo.FavoriteRepositoryImpl
import com.mofangvideo.core.repo.HistoryRepository
import com.mofangvideo.core.repo.HistoryRepositoryImpl
import com.mofangvideo.core.repo.LiveChannelRepository
import com.mofangvideo.core.repo.LiveChannelRepositoryImpl
import com.mofangvideo.core.repo.LiveSourceRepository
import com.mofangvideo.core.repo.LiveSourceRepositoryImpl
import com.mofangvideo.core.repo.SettingsRepository
import com.mofangvideo.core.repo.SettingsRepositoryImpl
import com.mofangvideo.core.repo.VideoRepository
import com.mofangvideo.core.repo.VideoRepositoryImpl
import com.mofangvideo.core.repo.VideoSourceRepository
import com.mofangvideo.core.repo.VideoSourceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * 核心 DI 模块：数据库、DAO、Repository、协程 Scope。
 */
@InstallIn(SingletonComponent::class)
@Module
object CoreModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MofangDatabase =
        androidx.room.Room.databaseBuilder(context, MofangDatabase::class.java, MofangDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideIoScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    fun provideVideoSourceDao(db: MofangDatabase): VideoSourceDao = db.videoSourceDao()

    @Provides
    fun provideLiveSourceDao(db: MofangDatabase): LiveSourceDao = db.liveSourceDao()

    @Provides
    fun provideLiveChannelDao(db: MofangDatabase): LiveChannelDao = db.liveChannelDao()

    @Provides
    fun provideFavoriteDao(db: MofangDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: MofangDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideVideoSourceRepo(db: MofangDatabase, dao: VideoSourceDao): VideoSourceRepository =
        VideoSourceRepositoryImpl(db, dao)

    @Provides
    @Singleton
    fun provideVideoRepo(videoSourceDao: VideoSourceDao): VideoRepository =
        VideoRepositoryImpl(videoSourceDao)

    @Provides
    @Singleton
    fun provideLiveSourceRepo(
        db: MofangDatabase,
        liveSourceDao: LiveSourceDao,
        liveChannelDao: LiveChannelDao
    ): LiveSourceRepository =
        LiveSourceRepositoryImpl(db, liveSourceDao, liveChannelDao)

    @Provides
    @Singleton
    fun provideLiveChannelRepo(liveChannelDao: LiveChannelDao, liveSourceDao: LiveSourceDao): LiveChannelRepository =
        LiveChannelRepositoryImpl(liveChannelDao, liveSourceDao)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = com.mofangvideo.core.network.Net.client

    @Provides
    @Singleton
    fun provideWebDavBackupRepo(
        @ApplicationContext context: Context,
        db: MofangDatabase,
        okHttpClient: OkHttpClient
    ): WebDavBackupRepository {
        val prefs = context.getSharedPreferences("mofang_settings", Context.MODE_PRIVATE)
        return WebDavBackupRepository(
            prefs = prefs,
            videoSourceDao = db.videoSourceDao(),
            liveSourceDao = db.liveSourceDao(),
            liveChannelDao = db.liveChannelDao(),
            favoriteDao = db.favoriteDao(),
            historyDao = db.historyDao(),
            okHttpClient = okHttpClient
        )
    }

    @Provides
    @Singleton
    fun provideBackupRepo(
        @ApplicationContext context: Context,
        webDav: WebDavBackupRepository
    ): BackupRepository =
        BackupRepositoryImpl(context, webDav)

    @Provides
    @Singleton
    fun provideFavoriteRepo(favoriteDao: FavoriteDao): FavoriteRepository =
        FavoriteRepositoryImpl(favoriteDao)

    @Provides
    @Singleton
    fun provideHistoryRepo(historyDao: HistoryDao): HistoryRepository =
        HistoryRepositoryImpl(historyDao)

    @Provides
    @Singleton
    fun provideSettingsRepo(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepositoryImpl(context)
}
