package com.mofangvideo.core.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.mofangvideo.core.repo.BackupRepository
import java.util.concurrent.TimeUnit

/**
 * 定时备份 Worker：开启自动备份后按周期把本地数据上传到 WebDAV。
 *
 * 由 [BackupScheduler] 调度；执行前检查 [BackupRepository.autoBackupEnabled]，
 * 关闭自动备份时不执行并取消自身。
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val backupRepo: BackupRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!backupRepo.autoBackupEnabled()) return Result.success()
        return when (backupRepo.backupToWebDav()) {
            is com.mofangvideo.core.repo.Result.Ok -> Result.success()
            is com.mofangvideo.core.repo.Result.Err -> Result.retry()
        }
    }
}

/**
 * 定时备份调度：周期 12 小时，开启自动备份后调度，关闭时取消。
 */
object BackupScheduler {

    private const val WORK_NAME = "periodic_backup"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
