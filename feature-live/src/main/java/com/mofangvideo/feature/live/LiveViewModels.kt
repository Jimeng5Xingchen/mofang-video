package com.mofangvideo.feature.live

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mofangvideo.core.model.EpgItem
import com.mofangvideo.core.model.EpgResult
import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.core.model.LiveSourceConfig
import com.mofangvideo.core.repo.LiveChannelRepository
import com.mofangvideo.core.repo.LiveSourceRepository
import com.mofangvideo.core.repo.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 直播源管理 ViewModel：导入 / 启停 / 删除源，并暴露源列表。
 */
@HiltViewModel
class LiveSourceViewModel @Inject constructor(
    private val repo: LiveSourceRepository
) : ViewModel() {

    private val _sources = MutableLiveData<List<LiveSourceConfig>>(emptyList())
    val sources: LiveData<List<LiveSourceConfig>> = _sources

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        viewModelScope.launch { repo.listAll().collect { _sources.value = it } }
    }

    fun importFromFile(uri: String) {
        viewModelScope.launch {
            val name = uri.substringAfterLast('/').substringBeforeLast('.')
                .ifEmpty { "直播源" }
            val result = repo.importFromFile(uri, name)
            _message.value = when (result) {
                is Result.Ok -> result.value.name
                is Result.Err -> result.message
            }
        }
    }

    fun importFromUrl(url: String, name: String) {
        viewModelScope.launch {
            val result = repo.importFromUrl(url, name)
            _message.value = when (result) {
                is Result.Ok -> result.value.name
                is Result.Err -> result.message
            }
        }
    }

    fun toggle(sourceId: String, enabled: Boolean) =
        viewModelScope.launch { repo.toggle(sourceId, enabled) }

    fun delete(sourceId: String) = viewModelScope.launch { repo.delete(sourceId) }

    fun consumeMessage() {
        _message.value = null
    }
}

/**
 * 直播频道浏览 ViewModel：分组、频道列表、当前频道 EPG。
 */
@HiltViewModel
class LiveChannelViewModel @Inject constructor(
    private val repo: LiveChannelRepository
) : ViewModel() {

    private val _groups = MutableLiveData<List<String>>(emptyList())
    val groups: LiveData<List<String>> = _groups

    private val _channels = MutableLiveData<List<LiveChannel>>(emptyList())
    val channels: LiveData<List<LiveChannel>> = _channels

    private val _epg = MutableLiveData<List<EpgItem>>(emptyList())
    val epg: LiveData<List<EpgItem>> = _epg

    private val _selectedGroup = MutableLiveData(GROUP_ALL)
    val selectedGroup: LiveData<String> = _selectedGroup

    private val currentChannel = MutableLiveData<LiveChannel?>()

    fun loadGroups() {
        viewModelScope.launch { _groups.value = repo.groups() }
    }

    fun selectGroup(group: String) {
        _selectedGroup.value = group
        loadChannels(group)
    }

    fun loadChannels(group: String = _selectedGroup.value ?: GROUP_ALL) {
        viewModelScope.launch { _channels.value = repo.listByGroup(group) }
    }

    fun loadEpg(channel: LiveChannel) {
        currentChannel.value = channel
        _epg.value = emptyList()
        viewModelScope.launch { _epg.value = repo.epg(channel)?.items ?: emptyList() }
    }

    fun currentChannel(): LiveChannel? = currentChannel.value

    companion object {
        const val GROUP_ALL = "全部"
    }
}
