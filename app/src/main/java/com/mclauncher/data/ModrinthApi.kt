package com.mclauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ModrinthApi {
    private const val BASE = "https://api.modrinth.com/v2"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // 搜索模组
    suspend fun search(
        query: String,
        gameVersion: String? = null,
        loader: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): ModrinthSearchResponse = withContext(Dispatchers.IO) {
        val tags = mutableListOf<String>()
        gameVersion?.let { tags.add("versions:$it") }
        loader?.let { tags.add("categories:$it") }
        
        val url = buildString {
            append("$BASE/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}")
            append("&offset=$offset&limit=$limit")
            if (tags.isNotEmpty()) append("&facets=${tags.joinToString(separator = "],[", prefix = "[[\"", postfix = "\"]]")}")
        }
        
        val req = Request.Builder().url(url).header("User-Agent", "MCLauncher/1.0").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("Modrinth API 错误: ${resp.code}")
            json.decodeFromString(resp.body?.string() ?: "{}")
        }
    }
    
    // 获取模组详情（含文件）
    suspend fun getModVersions(slugOrId: String, loader: String? = null, gameVersion: String? = null): List<ModrinthVersion> = withContext(Dispatchers.IO) {
        val url = buildString {
            append("$BASE/project/$slugOrId/version")
            val params = mutableListOf<String>()
            loader?.let { params.add("loaders=[\"$it\"]") }
            gameVersion?.let { params.add("game_versions=[\"$it\"]") }
            if (params.isNotEmpty()) append("?${params.joinToString("&")}")
        }
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("获取版本失败: ${resp.code}")
            json.decodeFromString(resp.body?.string() ?: "[]")
        }
    }
    
    // 获取所有分类（游戏版本支持列表）
    suspend fun getGameVersions(): List<String> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$BASE/tag/game_version").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use emptyList()
            json.decodeFromString<List<Map<String, Any>>>(resp.body?.string() ?: "[]")
                .map { it["version"] as? String ?: "" }
                .filter { it.isNotBlank() }
        }
    }
    
    // 获取所有加载器类型
    suspend fun getLoaders(): List<String> = withContext(Dispatchers.IO) {
        listOf("fabric", "forge", "neoforge", "quilt", "liteloader")
    }
}
