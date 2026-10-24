package com.mofangvideo.feature.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mofangvideo.core.model.FavType
import com.mofangvideo.core.model.Favorite
import com.mofangvideo.core.model.PlayHistory
import com.mofangvideo.core.repo.FavoriteRepository
import com.mofangvideo.core.repo.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 收藏 ViewModel。
 */
@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val repo: FavoriteRepository
) : ViewModel() {

    private val _favorites = MutableLiveData<List<Favorite>>()
    val favorites: LiveData<List<Favorite>> = _favorites

    private val _videoFavs = MutableLiveData<List<Favorite>>()
    val videoFavs: LiveData<List<Favorite>> = _videoFavs

    private val _liveFavs = MutableLiveData<List<Favorite>>()
    val liveFavs: LiveData<List<Favorite>> = _liveFavs

    fun loadAll() {
        viewModelScope.launch {
            _favorites.value = repo.list()
            _videoFavs.value = repo.list(FavType.VIDEO)
            _liveFavs.value = repo.list(FavType.LIVE)
        }
    }

    fun add(type: FavType, refId: String, title: String, poster: String = "") {
        viewModelScope.launch {
            repo.add(type, refId, title, poster)
            loadAll()
        }
    }

    fun remove(type: FavType, refId: String) {
        viewModelScope.launch {
            repo.remove(type, refId)
            loadAll()
        }
    }

    fun isFavorited(type: FavType, refId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repo.isFavorited(type, refId)) }
    }
}

/**
 * 播放历史 ViewModel。
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: HistoryRepository
) : ViewModel() {

    private val _history = MutableLiveData<List<PlayHistory>>()
    val history: LiveData<List<PlayHistory>> = _history

    fun load() {
        viewModelScope.launch {
            _history.value = repo.list()
        }
    }

    fun record(type: FavType, refId: String, title: String, progressMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repo.record(type, refId, title, progressMs, durationMs)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repo.delete(id)
            load()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repo.clearAll()
            _history.value = emptyList()
        }
    }
}
