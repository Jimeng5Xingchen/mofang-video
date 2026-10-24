package com.mofangvideo.core.repo

import android.content.Context
import com.mofangvideo.core.backup.WebDavBackupRepository
import com.mofangvideo.core.model.FavType

/**
 * 备份仓库实现：包装 [WebDavBackupRepository]，统一 [Result] 语义。
 */
class BackupRepositoryImpl(
    private val context: Context,
    private val webDav: WebDavBackupRepository
) : BackupRepository {

    override fun serverUrl(): String = webDav.serverUrl()
    override fun username(): String = webDav.username()
    override fun isConfigured(): Boolean = webDav.isConfigured()
    override fun autoBackupEnabled(): Boolean = webDav.autoBackupEnabled()

    override fun setAutoBackup(enable: Boolean) = webDav.setAutoBackup(enable)

    override fun configure(url: String, user: String, password: String) =
        webDav.configure(url, user, password)

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

    override suspend fun probeWebDav(): Result<Boolean> = try {
        if (webDav.probeWebDav()) Result.Ok(true) else Result.Err("未找到备份文件")
    } catch (e: Exception) {
        Result.Err("连接失败: ${e.message}")
    }
}
