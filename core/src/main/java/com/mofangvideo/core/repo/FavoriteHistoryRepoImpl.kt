package com.mofangvideo.core.repo

import com.mofangvideo.core.db.FavoriteDao
import com.mofangvideo.core.db.FavoriteEntity
import com.mofangvideo.core.db.HistoryDao
import com.mofangvideo.core.db.PlayHistoryEntity
import com.mofangvideo.core.model.FavType
import com.mofangvideo.core.model.Favorite
import com.mofangvideo.core.model.PlayHistory

/**
 * 收藏仓库实现。
 *
 * 账号系统已移除，收藏数据全部保存在手机本地。
 * (type, refId) 唯一约束由 Room 保证，重复收藏不产生重复记录。
 */
class FavoriteRepositoryImpl(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override suspend fun add(type: FavType, refId: String, title: String, poster: String) {
        if (favoriteDao.isFavorited(type.name, refId)) return
        favoriteDao.insert(
            FavoriteEntity(
                type = type.name,
                refId = refId,
                title = title,
                poster = poster,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun remove(type: FavType, refId: String) {
        favoriteDao.delete(type.name, refId)
    }

    override suspend fun list(type: FavType?): List<Favorite> {
        val rows = if (type != null) {
            favoriteDao.listByType(type.name)
        } else {
            favoriteDao.list()
        }
        return rows.map { it.toFavorite() }
    }

    override suspend fun isFavorited(type: FavType, refId: String): Boolean =
        favoriteDao.isFavorited(type.name, refId)
}

/**
 * 播放历史仓库实现。
 *
 * 账号系统已移除，播放历史全部保存在手机本地。
 */
class HistoryRepositoryImpl(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override suspend fun record(type: FavType, refId: String, title: String, progressMs: Long, durationMs: Long) {
        historyDao.insert(
            PlayHistoryEntity(
                type = type.name,
                refId = refId,
                title = title,
                progressMs = progressMs,
                durationMs = durationMs,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun list(): List<PlayHistory> =
        historyDao.list().map { it.toHistory() }

    override suspend fun latest(type: FavType, refId: String): PlayHistory? {
        val row = historyDao.latest(refId) ?: return null
        // 续播条件：进度 > 0 且 < 90%
        if (row.progressMs <= 0) return null
        if (row.durationMs > 0 && row.progressMs >= row.durationMs * 0.9) return null
        return row.toHistory()
    }

    override suspend fun delete(id: Long) {
        historyDao.delete(id)
    }

    override suspend fun clearAll() {
        historyDao.clearAll()
    }
}

private fun FavoriteEntity.toFavorite() = Favorite(
    id = id,
    type = FavType.valueOf(type),
    refId = refId,
    title = title,
    poster = poster,
    createdAt = createdAt
)

private fun PlayHistoryEntity.toHistory() = PlayHistory(
    id = id,
    type = FavType.valueOf(type),
    refId = refId,
    title = title,
    progressMs = progressMs,
    durationMs = durationMs,
    playedAt = playedAt
)
