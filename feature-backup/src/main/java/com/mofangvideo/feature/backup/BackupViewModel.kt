package com.mofangvideo.feature.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mofangvideo.core.repo.BackupRepository
import com.mofangvideo.core.repo.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * WebDAV 备份同步 ViewModel。
 *
 * 数据全部存放在手机本地，WebDAV 仅作为备份/迁移通道。
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepo: BackupRepository
) : ViewModel() {

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    private val _connected = MutableLiveData<Boolean>()
    val connected: LiveData<Boolean> = _connected

    val isConfigured: Boolean get() = backupRepo.isConfigured()
    val autoBackupEnabled: Boolean get() = backupRepo.autoBackupEnabled()
    val serverUrl: String get() = backupRepo.serverUrl()
    val username: String get() = backupRepo.username()

    fun setAutoBackup(enable: Boolean) = backupRepo.setAutoBackup(enable)

    fun backupToWebDav(serverUrl: String, username: String, password: String) {
        if (!validate(serverUrl, username, password)) return
        launchWithBusy("已备份到 WebDAV") {
            backupRepo.configure(serverUrl, username, password)
            backupRepo.backupToWebDav()
        }
    }

    fun restoreFromWebDav() {
        launchWithBusy("已从 WebDAV 恢复") {
            backupRepo.restoreFromWebDav()
        }
    }

    fun backupToLocal() {
        launchWithBusy("已备份到本地") {
            backupRepo.backupToLocal()
        }
    }

    fun restoreFromLocal() {
        launchWithBusy("已从本地备份恢复") {
            backupRepo.restoreFromLocal()
        }
    }

    fun probeWebDav(serverUrl: String, username: String, password: String) {
        if (!validate(serverUrl, username, password)) return
        viewModelScope.launch {
            _busy.value = true
            backupRepo.configure(serverUrl, username, password)
            val result = backupRepo.probeWebDav()
            _connected.value = result.isOk()
            _busy.value = false
        }
    }

    private fun validate(serverUrl: String, username: String, password: String): Boolean {
        val ok = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
        if (!ok) _status.value = "请先填写 WebDAV 地址、账号与密码"
        return ok
    }

    private fun launchWithBusy(successMessage: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _busy.value = true
            val result = block()
            _status.value = if (result.isOk()) successMessage else (result as Result.Err).message
            _busy.value = false
        }
    }
}
