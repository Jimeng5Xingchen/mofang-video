package com.mofangvideo.feature.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mofangvideo.core.repo.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 设置 ViewModel。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    private val _playbackCore = MutableLiveData("exo")
    val playbackCore: LiveData<String> = _playbackCore

    private val _autoResume = MutableLiveData(true)
    val autoResume: LiveData<Boolean> = _autoResume

    private val _defaultQuality = MutableLiveData("auto")
    val defaultQuality: LiveData<String> = _defaultQuality

    private val _themeMode = MutableLiveData("system")
    val themeMode: LiveData<String> = _themeMode

    init {
        viewModelScope.launch {
            _playbackCore.value = repo.playbackCore().first()
            _autoResume.value = repo.autoResume().first()
            _defaultQuality.value = repo.defaultQuality().first()
            _themeMode.value = repo.themeMode().first()
        }
    }

    fun setPlaybackCore(core: String) {
        _playbackCore.value = core
        viewModelScope.launch { repo.setPlaybackCore(core) }
    }

    fun setAutoResume(enable: Boolean) {
        _autoResume.value = enable
        viewModelScope.launch { repo.setAutoResume(enable) }
    }

    fun setDefaultQuality(quality: String) {
        _defaultQuality.value = quality
        viewModelScope.launch { repo.setDefaultQuality(quality) }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch { repo.setThemeMode(mode) }
    }
}
