package com.mclauncher.data

import com.mclauncher.MCApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object MojangApi {
    private const val MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    private const val ASSET_INDEX_URL = "https://resources.download.minecraft.net/"
    private const val DOWNLOAD_URL = "https://piston-download.mojang.com/"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    
    // 获取所有可用版本
    suspend fun fetchManifest(): MojangVersionManifest = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(MANIFEST_URL).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("Manifest 下载失败: ${resp.code}")
            resp.body?.string()?.let { json.decodeFromString(it) }
                ?: throw Exception("Manifest 为空")
        }
    }
    
    // 获取单个版本的元数据 JSON
    suspend fun fetchVersionJson(versionId: String): String = withContext(Dispatchers.IO) {
        val manifest = fetchManifest()
        val v = manifest.versions.find { it.id == versionId }
            ?: throw Exception("版本 $versionId 不存在")
        val req = Request.Builder().url(v.url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("版本 JSON 下载失败: ${resp.code}")
            resp.body?.string() ?: throw Exception("空响应")
        }
    }
    
    // 缓存版本 JSON 到本地
    suspend fun cacheVersionJson(versionId: String): File = withContext(Dispatchers.IO) {
        val versionsDir = MCApplication.versionsDir()
        val versionDir = File(versionsDir, versionId).apply { if (!exists()) mkdirs() }
        val jsonFile = File(versionDir, "$versionId.json")
        if (!jsonFile.exists()) {
            jsonFile.writeText(fetchVersionJson(versionId))
        }
        jsonFile
    }
}
