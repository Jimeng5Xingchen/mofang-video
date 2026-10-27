package com.mofangvideo.feature.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mofangvideo.core.repo.SettingsRepository
import com.mofangvideo.core.update.AppUpdate
import com.mofangvideo.core.update.DownloadState
import com.mofangvideo.core.update.UpdateChannel
import com.mofangvideo.core.update.UpdateRepository
import com.mofangvideo.core.update.UpdateInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 检查更新状态。
 */
sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class Latest(val version: String) : UpdateUiState()
    data class Available(val update: AppUpdate) : UpdateUiState()
    data class Downloading(val percent: Int) : UpdateUiState()
    data class ReadyToInstall(val update: AppUpdate, val file: File) : UpdateUiState()
    data class Failed(val message: String) : UpdateUiState()
}

/**
 * 检查更新 ViewModel：渠道选择、GitHub Releases 检查、自动下载、安装引导。
 *
 * 手机端与 TV 端共用此逻辑：TV 端经 HiltViewModel 获取后由 D-pad 驱动。
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepo: UpdateRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableLiveData<UpdateUiState>(UpdateUiState.Idle)
    val state: LiveData<UpdateUiState> = _state

    private val _channel = MutableLiveData(UpdateChannel.STABLE)
    val channel: LiveData<UpdateChannel> = _channel

    private val _channelLabel = MutableLiveData("")
    val channelLabel: LiveData<String> = _channelLabel

    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = settingsRepo.updateChannel().first()
            setChannel(UpdateChannel.fromPrefs(stored), persist = false)
        }
    }

    /** 切换渠道（持久化后重置状态）。 */
    fun setChannel(channel: UpdateChannel) = setChannel(channel, persist = true)

    private fun setChannel(channel: UpdateChannel, persist: Boolean) {
        _channel.value = channel
        if (persist) {
            viewModelScope.launch { settingsRepo.setUpdateChannel(channel.name) }
        }
        _channelLabel.value = when (channel) {
            UpdateChannel.STABLE -> "正式版"
            UpdateChannel.BETA -> "Beta 测试版"
        }
        if (_state.value is UpdateUiState.Failed) _state.value = UpdateUiState.Idle
    }

    /** 执行检查；发现新版本后按需求自动开始下载。 */
    fun check(currentVersion: String) {
        if (_state.value is UpdateUiState.Checking) return
        downloadJob?.cancel()
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            val channel = _channel.value ?: UpdateChannel.STABLE
            when (val result = updateRepo.checkUpdate(channel, currentVersion)) {
                is com.mofangvideo.core.repo.Result.Ok -> {
                    val update = result.value
                    if (update == null) {
                        _state.value = UpdateUiState.Latest(currentVersion)
                    } else {
                        _state.value = UpdateUiState.Available(update)
                        startDownload(update)
                    }
                }
                is com.mofangvideo.core.repo.Result.Err ->
                    _state.value = UpdateUiState.Failed(result.message)
            }
        }
    }

    fun startDownload(update: AppUpdate) {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            updateRepo.downloadApk(update)
                .onEach { state ->
                    _state.value = when (state) {
                        is DownloadState.Progress -> UpdateUiState.Downloading(state.percent)
                        is DownloadState.Done -> UpdateUiState.ReadyToInstall(update, state.file)
                        is DownloadState.Failed -> UpdateUiState.Failed(state.message)
                    }
                }
                .collect { }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.value = UpdateUiState.Idle
    }

    fun retry(currentVersion: String) = check(currentVersion)

    /** 触发系统安装器；未授权时返回 false 由界面引导授权。 */
    fun install(context: Context, file: File): Boolean {
        if (!UpdateInstaller.canInstallUnknownSources(context)) return false
        return try {
            UpdateInstaller.installApk(context, file)
            true
        } catch (e: Exception) {
            _state.value = UpdateUiState.Failed("无法调起安装器：${e.message ?: "未知错误"}")
            false
        }
    }
}
