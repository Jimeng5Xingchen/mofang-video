package com.mofangvideo.core.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import org.json.JSONObject

/**
 * 影视源配置实体。
 * params / fields / mappings 序列化为 JSON 字符串存储。
 */
@Entity(tableName = "video_source_config")
data class VideoSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val api: String,
    val params: String = "{}",
    val fields: String = "{}",
    val mappings: String = "{}",
    val enabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L
) {
    companion object {
        fun fromJson(obj: JSONObject): VideoSourceEntity = VideoSourceEntity(
            id = obj.getString("id"),
            name = obj.getString("name"),
            type = obj.optString("type", "SCRAPER"),
            api = obj.getString("api"),
            params = obj.optString("params", "{}"),
            fields = obj.optString("fields", "{}"),
            mappings = obj.optString("mappings", "{}"),
            enabled = obj.optBoolean("enabled", true),
            createdAt = obj.optLong("createdAt", 0L)
        )
    }
}

/**
 * M3U 直播源配置实体。
 */
@Entity(tableName = "live_source_config")
data class LiveSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    @ColumnInfo(name = "channel_count") val channelCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L
) {
    companion object {
        fun fromJson(obj: JSONObject): LiveSourceEntity = LiveSourceEntity(
            id = obj.getString("id"),
            name = obj.getString("name"),
            url = obj.getString("url"),
            enabled = obj.optBoolean("enabled", true),
            channelCount = obj.optInt("channelCount", 0),
            createdAt = obj.optLong("createdAt", 0L)
        )
    }
}

/**
 * 直播频道实体，归属某直播源。
 */
@Entity(
    tableName = "live_channel",
    primaryKeys = ["id", "source_id"],
    foreignKeys = [
        ForeignKey(
            entity = LiveSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("source_id")]
)
data class LiveChannelEntity(
    val id: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val name: String,
    @ColumnInfo(name = "group_name") val groupName: String = "",
    val logo: String = "",
    val url: String,
    @ColumnInfo(name = "tvg_id") val tvgId: String = "",
    @ColumnInfo(name = "epg_url") val epgUrl: String? = null
) {
    companion object {
        fun fromJson(obj: JSONObject): LiveChannelEntity = LiveChannelEntity(
            id = obj.getString("id"),
            sourceId = obj.getString("sourceId"),
            name = obj.getString("name"),
            groupName = obj.optString("groupName", ""),
            logo = obj.optString("logo", ""),
            url = obj.getString("url"),
            tvgId = obj.optString("tvgId", ""),
            epgUrl = if (obj.isNull("epgUrl")) null else obj.optString("epgUrl")
        )
    }
}

/**
 * 收藏记录，账号已移除，数据仅存本地。
 * (type, refId) 唯一约束由 [Index] 保证。
 */
@Entity(
    tableName = "favorite",
    indices = [Index(value = ["type", "ref_id"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    @ColumnInfo(name = "ref_id") val refId: String,
    val title: String,
    val poster: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L
) {
    companion object {
        fun fromJson(obj: JSONObject): FavoriteEntity = FavoriteEntity(
            id = obj.optLong("id", 0L),
            type = obj.getString("type"),
            refId = obj.getString("refId"),
            title = obj.getString("title"),
            poster = obj.optString("poster", ""),
            createdAt = obj.optLong("createdAt", 0L)
        )
    }
}

/**
 * 播放历史，账号已移除，数据仅存本地。
 */
@Entity(
    tableName = "play_history",
    indices = [Index("ref_id"), Index("played_at")]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    @ColumnInfo(name = "ref_id") val refId: String,
    val title: String,
    @ColumnInfo(name = "progress_ms") val progressMs: Long = 0L,
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 0L,
    @ColumnInfo(name = "played_at") val playedAt: Long = 0L
) {
    companion object {
        fun fromJson(obj: JSONObject): PlayHistoryEntity = PlayHistoryEntity(
            id = obj.optLong("id", 0L),
            type = obj.getString("type"),
            refId = obj.getString("refId"),
            title = obj.getString("title"),
            progressMs = obj.optLong("progressMs", 0L),
            durationMs = obj.optLong("durationMs", 0L),
            playedAt = obj.optLong("playedAt", 0L)
        )
    }
}

/**
 * Room 数据库，版本 1。
 */
@Database(
    entities = [
        VideoSourceEntity::class,
        LiveSourceEntity::class,
        LiveChannelEntity::class,
        FavoriteEntity::class,
        PlayHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MofangDatabase : RoomDatabase() {

    abstract fun videoSourceDao(): VideoSourceDao
    abstract fun liveSourceDao(): LiveSourceDao
    abstract fun liveChannelDao(): LiveChannelDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "mofang_video.db"
    }
}

/**
 * 影视源 DAO。
 */
@Dao
interface VideoSourceDao {
    @Query("SELECT * FROM video_source_config")
    suspend fun listAll(): List<VideoSourceEntity>

    @Query("SELECT * FROM video_source_config WHERE enabled = 1")
    suspend fun listEnabled(): List<VideoSourceEntity>

    @Query("SELECT * FROM video_source_config WHERE id = :id")
    suspend fun findById(id: String): VideoSourceEntity?

    @Insert
    suspend fun upsert(entity: VideoSourceEntity)

    @Query("UPDATE video_source_config SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM video_source_config WHERE id = :id")
    suspend fun delete(id: String)
}

/**
 * 直播源 DAO。
 */
@Dao
interface LiveSourceDao {
    @Query("SELECT * FROM live_source_config")
    suspend fun listAll(): List<LiveSourceEntity>

    @Query("SELECT * FROM live_source_config WHERE enabled = 1")
    suspend fun listEnabled(): List<LiveSourceEntity>

    @Query("SELECT * FROM live_source_config WHERE id = :id")
    suspend fun findById(id: String): LiveSourceEntity?

    @Insert
    suspend fun upsert(entity: LiveSourceEntity)

    @Query("UPDATE live_source_config SET enabled = :enabled, channel_count = :count WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, count: Int)

    @Query("DELETE FROM live_source_config WHERE id = :id")
    suspend fun delete(id: String)
}

/**
 * 直播频道 DAO。
 */
@Dao
interface LiveChannelDao {
    @Query("SELECT * FROM live_channel WHERE source_id = :sourceId")
    suspend fun listBySource(sourceId: String): List<LiveChannelEntity>

    @Query("SELECT DISTINCT group_name FROM live_channel")
    suspend fun groups(): List<String>

    @Query("SELECT * FROM live_channel WHERE group_name = :group")
    suspend fun listByGroup(group: String): List<LiveChannelEntity>

    @Query("SELECT * FROM live_channel WHERE tvg_id = :tvgId")
    suspend fun findByTvgId(tvgId: String): List<LiveChannelEntity>

    @Insert
    suspend fun insertAll(channels: List<LiveChannelEntity>)

    @Query("DELETE FROM live_channel WHERE source_id = :sourceId")
    suspend fun deleteBySource(sourceId: String)
}

/**
 * 收藏 DAO。
 */
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite ORDER BY created_at DESC")
    suspend fun list(): List<FavoriteEntity>

    @Query("SELECT * FROM favorite WHERE type = :type ORDER BY created_at DESC")
    suspend fun listByType(type: String): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE type = :type AND ref_id = :refId)")
    suspend fun isFavorited(type: String, refId: String): Boolean

    @Insert
    suspend fun insert(entity: FavoriteEntity): Long

    @Query("DELETE FROM favorite WHERE type = :type AND ref_id = :refId")
    suspend fun delete(type: String, refId: String)

    @Query("DELETE FROM favorite WHERE ref_id = :refId")
    suspend fun deleteByRef(refId: String)
}

/**
 * 播放历史 DAO。
 */
@Dao
interface HistoryDao {
    @Query("SELECT * FROM play_history ORDER BY played_at DESC")
    suspend fun list(): List<PlayHistoryEntity>

    @Query("SELECT * FROM play_history WHERE ref_id = :refId ORDER BY played_at DESC LIMIT 1")
    suspend fun latest(refId: String): PlayHistoryEntity?

    @Insert
    suspend fun insert(entity: PlayHistoryEntity): Long

    @Query("DELETE FROM play_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM play_history")
    suspend fun clearAll()

    @Query("DELETE FROM play_history WHERE ref_id = :refId")
    suspend fun deleteByRef(refId: String)
}
