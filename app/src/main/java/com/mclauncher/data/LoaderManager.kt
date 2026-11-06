package com.mclauncher.data

import com.mclauncher.MCApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 自动下载 Fabric Loader / Forge / NeoForge
 */
object LoaderManager {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    
    enum class Loader { FABRIC, FORGE, NEOFORGE }
    
    data class LoaderVersion(val version: String, val mcVersion: String, val url: String)
    
    /** 查 Fabric Loader 版本 */
    suspend fun getFabricVersions(mcVersion: String): List<LoaderVersion> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://meta.fabricmc.net/v2/versions/loader/$mcVersion")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val arr = JSONArray(resp.body?.string() ?: "[]")
                val result = mutableListOf<LoaderVersion>()
                for (i in 0 until arr.length()) {
                    val loader = arr.getJSONObject(i).optJSONObject("loader") ?: continue
                    result += LoaderVersion(
                        version = loader.getString("version"),
                        mcVersion = mcVersion,
                        url = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/${loader.getString("version")}/fabric-loader-${loader.getString("version")}.jar"
                    )
                }
                result
            }
        } catch (e: Exception) { emptyList() }
    }
    
    /** 查 Forge 版本 */
    suspend fun getForgeVersions(mcVersion: String): List<LoaderVersion> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://files.minecraftforge.net/net/minecraftforge/forge/index_$mcVersion.html")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                // 简单正则提取版本号
                val html = resp.body?.string() ?: ""
                val regex = """forge/${mcVersion}/forge-${mcVersion}-([0-9.]+)-installer.jar""".toRegex()
                regex.findAll(html).map { m ->
                    LoaderVersion(
                        version = m.groupValues[1],
                        mcVersion = mcVersion,
                        url = "https://maven.minecraftforge.net/net/minecraftforge/forge/${mcVersion}-${m.groupValues[1]}/forge-${mcVersion}-${m.groupValues[1]}-installer.jar"
                    )
                }.toList()
            }
        } catch (e: Exception) { emptyList() }
    }
    
    /** 下载 Loader jar */
    suspend fun downloadLoader(loader: Loader, version: String, mcVersion: String,
                               onProgress: (Float, Long, Long) -> Unit): File? = withContext(Dispatchers.IO) {
        val versions = when (loader) {
            Loader.FABRIC -> getFabricVersions(mcVersion)
            Loader.FORGE -> getForgeVersions(mcVersion)
            Loader.NEOFORGE -> emptyList()
        }
        val match = versions.firstOrNull { it.version == version } ?: versions.firstOrNull() ?: return@withContext null
        
        val outFile = File(MCApplication.librariesDir(), match.url.substringAfterLast("/"))
        outFile.parentFile?.mkdirs()
        if (!outFile.exists()) {
            DownloadManager.download(match.url, outFile.absolutePath, match.url.substringAfterLast("/"), "Loader", onProgress)
        }
        outFile
    }
}
