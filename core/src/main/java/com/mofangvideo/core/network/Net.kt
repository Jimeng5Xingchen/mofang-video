package com.mofangvideo.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 网络层：OkHttp 客户端封装。
 *
 * 账号系统已移除，统一 HTTP 客户端主要服务：
 * - M3U 文件 / 影视源 JSON 下载（[downloadText]）
 * - EPG XML 下载
 * - WebDAV 备份上传/下载
 *
 * 超时统一 10s。
 */
object Net {

    private const val TIMEOUT_S = 10L

    val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    /**
     * 通用文本下载（M3U 文件、影视源 JSON、EPG 等）。
     */
    suspend fun downloadText(url: String): String {
        val request = okhttp3.Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: $url")
            return resp.body?.string() ?: throw IOException("empty body: $url")
        }
    }
}
