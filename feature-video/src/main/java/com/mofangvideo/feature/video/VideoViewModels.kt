package com.mofangvideo.feature.video

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mofangvideo.core.model.Video
import com.mofangvideo.core.model.VideoSourceConfig
import com.mofangvideo.core.repo.Result
import com.mofangvideo.core.repo.VideoRepository
import com.mofangvideo.core.repo.VideoSourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 影视源管理 ViewModel：导入 / 启停 / 删除源，并暴露源列表。
 */
@HiltViewModel
class VideoSourceViewModel @Inject constructor(
    private val repo: VideoSourceRepository
) : ViewModel() {

    private val _sources = MutableLiveData<List<VideoSourceConfig>>(emptyList())
    val sources: LiveData<List<VideoSourceConfig>> = _sources

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        viewModelScope.launch { repo.listAll().collect { _sources.postValue(it) } }
    }

    fun importFromFile(uri: String) {
        launchImport("从文件导入影视源失败") { repo.importFromFile(uri) }
    }

    fun importFromUrl(url: String, name: String) {
        launchImport("从 URL 导入影视源失败") { repo.importFromUrl(url, name) }
    }

    fun toggle(sourceId: String, enabled: Boolean) {
        viewModelScope.launch { repo.toggle(sourceId, enabled) }
    }

    fun delete(sourceId: String) {
        viewModelScope.launch { repo.delete(sourceId) }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun launchImport(failPrefix: String, block: suspend () -> Result<VideoSourceConfig>) {
        viewModelScope.launch {
            when (val result = block()) {
                is Result.Ok -> _message.value = result.value.name
                is Result.Err -> _message.value = result.message.ifBlank { failPrefix }
            }
        }
    }
}

/**
 * 影视浏览 ViewModel：关键词搜索与结果状态。
 */
@HiltViewModel
class VideoViewModel @Inject constructor(
    private val repo: VideoRepository
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<Video>>(emptyList())
    val searchResults: LiveData<List<Video>> = _searchResults

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun search(keyword: String) {
        if (keyword.isBlank()) {
            clear()
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _searchResults.value = repo.search(keyword)
            } catch (e: Exception) {
                _error.value = e.message
                _searchResults.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun clear() {
        _searchResults.value = emptyList()
        _error.value = null
    }
}
