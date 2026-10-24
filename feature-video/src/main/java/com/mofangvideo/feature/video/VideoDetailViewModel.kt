package com.mofangvideo.feature.video

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mofangvideo.core.model.FavType
import com.mofangvideo.core.model.Video
import com.mofangvideo.core.repo.FavoriteRepository
import com.mofangvideo.core.repo.HistoryRepository
import com.mofangvideo.core.repo.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 收藏 / 历史统一使用的影视引用键。
 */
fun videoRef(sourceId: String, videoId: String): String = "${sourceId}_$videoId"

/**
 * 影视详情 ViewModel：加载详情、收藏状态维护、续播进度查询。
 */
@HiltViewModel
class VideoDetailViewModel @Inject constructor(
    private val videoRepo: VideoRepository,
    private val favoriteRepo: FavoriteRepository,
    private val historyRepo: HistoryRepository
) : ViewModel() {

    private val _video = MutableLiveData<Video?>()
    val video: LiveData<Video?> = _video

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _favorited = MutableLiveData(false)
    val favorited: LiveData<Boolean> = _favorited

    private var currentSourceId: String = ""

    fun load(sourceId: String, videoId: String) {
        currentSourceId = sourceId
        viewModelScope.launch {
            _loading.value = true
            try {
                val detail = videoRepo.detail(sourceId, videoId)
                _video.value = detail
                refreshFavorited()
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleFavorite() {
        val video = _video.value ?: return
        viewModelScope.launch {
            val refId = videoRef(currentSourceId, video.id)
            if (_favorited.value == true) {
                favoriteRepo.remove(FavType.VIDEO, refId)
            } else {
                favoriteRepo.add(FavType.VIDEO, refId, video.title, video.poster)
            }
            refreshFavorited()
        }
    }

    private suspend fun refreshFavorited() {
        val video = _video.value ?: return
        _favorited.value = favoriteRepo.isFavorited(FavType.VIDEO, videoRef(currentSourceId, video.id))
    }

    /** 续播进度（毫秒），没有历史或已播完返回 0。 */
    fun resumeMs(onResult: (Long) -> Unit) {
        viewModelScope.launch {
            val video = _video.value
            if (video == null) {
                onResult(0L)
                return@launch
            }
            val latest = historyRepo.latest(FavType.VIDEO, videoRef(currentSourceId, video.id))
            onResult(latest?.progressMs ?: 0L)
        }
    }
}
