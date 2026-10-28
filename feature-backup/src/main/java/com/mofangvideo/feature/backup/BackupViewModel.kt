package com.mofangvideo.feature.backup

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mofangvideo.core.backup.BackupScheduler
import com.mofangvideo.core.repo.BackupRepository
import com.mofangvideo.core.repo.LocalBackupInfo
import com.mofangvideo.core.repo.RemoteBackupFile
import com.mofangvideo.core.repo.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * WebDAV 备份同步 ViewModel。
 *
 * 数据全部存放在手机本地，WebDAV 仅作为备份/迁移通道。支持：
 * - 连接检测（testConnection）
 * - 保存配置 + 立即备份
 * - 定时备份（WorkManager 周期任务，开关由 [setAutoBackup] 调度）
 * - 远程文件列表（PROPFIND）+ 指定文件恢复
 * - 本地备份文件信息
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val backupRepo: BackupRepository
) : ViewModel() {

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    private val _connected = MutableLiveData<Boolean?>()
    val connected: LiveData<Boolean?> = _connected

    private val _remoteFiles = MutableLiveData<List<RemoteBackupFile>>()
    val remoteFiles: LiveData<List<RemoteBackupFile>> = _remoteFiles

    private val _localInfo = MutableLiveData<LocalBackupInfo?>()
    val localInfo: LiveData<LocalBackupInfo?> = _localInfo

    val serverUrl: String get() = backupRepo.serverUrl()
    val username: String get() = backupRepo.username()
    val backupPath: String get() = backupRepo.backupPath()
    val autoBackupEnabled: Boolean get() = backupRepo.autoBackupEnabled()

    init {
        refreshLocalInfo()
    }

    fun testConnection(serverUrl: String, username: String, password: String, path: String) {
        if (!validate(serverUrl, username, password)) return
        viewModelScope.launch {
            _busy.value = true
            backupRepo.configure(serverUrl, username, password, path)
            val result = backupRepo.testConnection()
            _connected.value = result.isOk()
            _status.value = if (result.isOk()) "连接成功" else (result as Result.Err).message
            _busy.value = false
        }
    }

    fun save(serverUrl: String, username: String, password: String, path: String) {
        if (!validate(serverUrl, username, password)) return
        viewModelScope.launch {
            _busy.value = true
            backupRepo.configure(serverUrl, username, password, path)
            _status.value = "配置已保存"
            _busy.value = false
        }
    }

    fun saveAndBackup(serverUrl: String, username: String, password: String, path: String) {
        if (!validate(serverUrl, username, password)) return
        launchWithBusy("已保存配置并备份到 WebDAV") {
            backupRepo.configure(serverUrl, username, password, path)
            backupRepo.backupToWebDav()
        }
    }

    /** 用已保存配置直接备份到 WebDAV（TV 端无文本输入场景）。 */
    fun backupToWebDavNow() {
        if (!backupRepo.isConfigured()) {
            _status.value = "请先在手机端配置 WebDAV 地址、账号与密码"
            return
        }
        launchWithBusy("已备份到 WebDAV") {
            backupRepo.backupToWebDav()
        }
    }

    fun setAutoBackup(enable: Boolean) {
        backupRepo.setAutoBackup(enable)
        if (enable) BackupScheduler.schedule(appContext)
        else BackupScheduler.cancel(appContext)
    }

    fun listRemoteFiles() {
        viewModelScope.launch {
            _busy.value = true
            val result = backupRepo.listWebDavFiles()
            if (result is Result.Ok) {
                _remoteFiles.value = result.value
                _status.value = if (result.value.isEmpty()) "远程无备份文件" else "已加载 ${result.value.size} 个远程备份"
            } else {
                _status.value = (result as Result.Err).message
                _remoteFiles.value = emptyList()
            }
            _busy.value = false
        }
    }

    fun restoreFromRemoteFile(fileName: String) {
        viewModelScope.launch {
            _busy.value = true
            val result = backupRepo.restoreFromWebDavFile(fileName)
            _status.value = if (result.isOk()) "已从远程恢复：$fileName" else (result as Result.Err).message
            if (result.isOk()) refreshLocalInfo()
            _busy.value = false
        }
    }

    fun restoreFromWebDav() {
        viewModelScope.launch {
            _busy.value = true
            val result = backupRepo.restoreFromWebDav()
            _status.value = if (result.isOk()) "已从 WebDAV 恢复" else (result as Result.Err).message
            if (result.isOk()) refreshLocalInfo()
            _busy.value = false
        }
    }

    fun backupToLocal() {
        viewModelScope.launch {
            _busy.value = true
            val result = backupRepo.backupToLocal()
            _status.value = if (result.isOk()) "已备份到本地：${(result as Result.Ok).value}" else (result as Result.Err).message
            if (result.isOk()) refreshLocalInfo()
            _busy.value = false
        }
    }

    fun restoreFromLocal() {
        viewModelScope.launch {
            _busy.value = true
            val result = backupRepo.restoreFromLocal()
            _status.value = if (result.isOk()) "已从本地备份恢复" else (result as Result.Err).message
            _busy.value = false
        }
    }

    private fun refreshLocalInfo() {
        _localInfo.postValue(backupRepo.localBackupInfo())
    }

    private fun validate(serverUrl: String, username: String, password: String): Boolean {
        val ok = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
        if (!ok) _status.value = "请先填写 WebDAV 地址、账号与密码"
        return ok
    }

    private fun launchWithBusy(successMessage: String?, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _busy.value = true
            val result = block()
            if (successMessage != null) {
                _status.value = if (result.isOk()) successMessage else (result as Result.Err).message
            }
            _busy.value = false
        }
    }
}
