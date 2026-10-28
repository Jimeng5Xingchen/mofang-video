package com.mofangvideo.core.repo

import android.content.Context
import com.mofangvideo.core.backup.WebDavBackupRepository

/**
 * 备份仓库实现：包装 [WebDavBackupRepository]，统一 [Result] 语义。
 */
class BackupRepositoryImpl(
    private val context: Context,
    private val webDav: WebDavBackupRepository
) : BackupRepository {

    override fun serverUrl(): String = webDav.serverUrl()
    override fun username(): String = webDav.username()
    override fun backupPath(): String = webDav.backupPath()
    override fun isConfigured(): Boolean = webDav.isConfigured()
    override fun autoBackupEnabled(): Boolean = webDav.autoBackupEnabled()

    override fun setAutoBackup(enable: Boolean) = webDav.setAutoBackup(enable)

    override fun configure(url: String, user: String, password: String, path: String) =
        webDav.configure(url, user, password, path)

    override suspend fun backupToWebDav(): Result<Boolean> = try {
        val ok = webDav.uploadToWebDav(webDav.buildBackupJson())
        if (ok) Result.Ok(true) else Result.Err("上传失败，请检查 WebDAV 地址与账号")
    } catch (e: Exception) {
        Result.Err("备份失败: ${e.message}")
    }

    override suspend fun restoreFromWebDav(): Result<Boolean> {
        val json = webDav.downloadFromWebDav() ?: return Result.Err("云端无备份或未配置 WebDAV")
        return try {
            if (webDav.restoreFromJson(json)) Result.Ok(true) else Result.Err("备份文件解析失败")
        } catch (e: Exception) {
            Result.Err("恢复失败: ${e.message}")
        }
    }

    override suspend fun restoreFromWebDavFile(fileName: String): Result<Boolean> {
        val json = webDav.downloadFromWebDav(fileName) ?: return Result.Err("云端无该备份文件")
        return try {
            if (webDav.restoreFromJson(json)) Result.Ok(true) else Result.Err("备份文件解析失败")
        } catch (e: Exception) {
            Result.Err("恢复失败: ${e.message}")
        }
    }

    override suspend fun listWebDavFiles(): Result<List<RemoteBackupFile>> = try {
        val files = webDav.listRemoteFiles().map {
            RemoteBackupFile(
                name = it.name,
                sizeBytes = it.sizeBytes,
                modifiedAt = it.modifiedAt
            )
        }
        Result.Ok(files)
    } catch (e: Exception) {
        Result.Err("获取文件列表失败: ${e.message}")
    }

    override suspend fun testConnection(): Result<Boolean> = try {
        if (webDav.testConnection()) Result.Ok(true) else Result.Err("连接失败，请检查地址、账号与路径")
    } catch (e: Exception) {
        Result.Err("连接失败: ${e.message}")
    }

    override suspend fun backupToLocal(): Result<String> = try {
        val file = webDav.writeLocalBackup(context)
        Result.Ok(file.absolutePath)
    } catch (e: Exception) {
        Result.Err("本地备份失败: ${e.message}")
    }

    override suspend fun restoreFromLocal(): Result<Boolean> {
        val json = webDav.readLocalBackup(context) ?: return Result.Err("本地无备份文件")
        return try {
            if (webDav.restoreFromJson(json)) Result.Ok(true) else Result.Err("备份文件解析失败")
        } catch (e: Exception) {
            Result.Err("恢复失败: ${e.message}")
        }
    }

    override fun localBackupInfo(): LocalBackupInfo? = webDav.localBackupInfo(context)?.let {
        LocalBackupInfo(
            fileName = it.fileName,
            sizeBytes = it.sizeBytes,
            modifiedAt = it.modifiedAt
        )
    }

    override suspend fun probeWebDav(): Result<Boolean> = try {
        if (webDav.probeWebDav()) Result.Ok(true) else Result.Err("未找到备份文件")
    } catch (e: Exception) {
        Result.Err("连接失败: ${e.message}")
    }
}
