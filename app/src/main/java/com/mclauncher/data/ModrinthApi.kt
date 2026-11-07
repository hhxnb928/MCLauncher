package com.mclauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ModrinthMod(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val author: String?,
    val downloads: Int,
    val loader: List<String>,          // fabric/forge/neoforge/quilt
    val gameVersion: List<String>,     // 支持的 MC 版本
    val projectUrl: String,            // 主页
    val latestVersionUrl: String?      // 最新版下载
)

object ModrinthApi {
    private const val BASE = "https://api.modrinth.com/v2"
    private const val USER_AGENT = "MCLauncher/1.0.0 (Android)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun search(
        query: String = "",
        loader: String? = null,          // fabric / forge / neoforge
        gameVersion: String? = null,     // 1.21.1
        limit: Int = 20
    ): List<ModrinthMod> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder("$BASE/search?limit=$limit&query=${query.replace(" ", "+")}")
            // Facets 严格过滤
            val facets = mutableListOf<List<String>>()
            if (loader != null) facets.add(listOf("mod_loaders:$loader"))
            if (gameVersion != null) facets.add(listOf("versions:$gameVersion"))
            if (facets.isNotEmpty()) {
                // Modrinth facets: [["mod_loaders:fabric"],["versions:1.21.1"]]
                val facetsJson = facets.joinToString(",") { it.joinToString(",") { f -> "\"$f\"" } }
                urlBuilder.append("&facets=[[$facetsJson]]")
            }

            val req = Request.Builder()
                .url(urlBuilder.toString())
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val json = JSONObject(resp.body?.string() ?: "{}")
                val hits = json.optJSONArray("hits") ?: return@use emptyList()
                val result = mutableListOf<ModrinthMod>()
                for (i in 0 until hits.length()) {
                    val h = hits.getJSONObject(i)
                    result += ModrinthMod(
                        id = h.getString("project_id"),
                        slug = h.getString("slug"),
                        title = h.getString("title"),
                        description = h.optString("description", ""),
                        iconUrl = h.optString("icon_url").ifEmpty { null },
                        author = h.optString("author").ifEmpty { null },
                        downloads = h.optInt("downloads", 0),
                        loader = (h.optJSONArray("mod_loaders") ?: JSONArray()).let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        },
                        gameVersion = (h.optJSONArray("versions") ?: JSONArray()).let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        },
                        projectUrl = "https://modrinth.com/mod/${h.getString("slug")}",
                        latestVersionUrl = null
                    )
                }
                result
            }
        } catch (e: Exception) { emptyList() }
    }

    /** 获取模组详情 + 最新版下载 */
    suspend fun getMod(slug: String): ModrinthMod? = withContext(Dispatchers.IO) {
        try {
            // 项目信息
            val req1 = Request.Builder()
                .url("$BASE/project/$slug")
                .header("User-Agent", USER_AGENT)
                .build()
            val project = client.newCall(req1).execute().use {
                if (!it.isSuccessful) return@use null
                JSONObject(it.body?.string() ?: "{}")
            }

            // 最新版本
            val req2 = Request.Builder()
                .url("$BASE/project/$slug/version?featured=true")
                .header("User-Agent", USER_AGENT)
                .build()
            val latest = client.newCall(req2).execute().use {
                if (!it.isSuccessful) null
                else {
                    val arr = JSONArray(it.body?.string() ?: "[]")
                    if (arr.length() > 0) arr.getJSONObject(0) else null
                }
            }

            ModrinthMod(
                id = project.getString("id"),
                slug = slug,
                title = project.getString("title"),
                description = project.optString("description", ""),
                iconUrl = project.optString("icon_url").ifEmpty { null },
                author = null,
                downloads = project.optInt("downloads", 0),
                loader = (project.optJSONArray("loaders") ?: JSONArray()).let { (0 until it.length()).map { i -> it.getString(i) } },
                gameVersion = (project.optJSONArray("game_versions") ?: JSONArray()).let { (0 until it.length()).map { i -> it.getString(i) } },
                projectUrl = "https://modrinth.com/mod/$slug",
                latestVersionUrl = latest?.optJSONArray("files")?.optJSONObject(0)?.optString("url")
            )
        } catch (e: Exception) { null }
    }

    /** 下载模组文件 (校验 hash) */
    suspend fun downloadMod(mod: ModrinthMod, versionId: String, onProgress: (Float) -> Unit): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$BASE/project/${mod.slug}/version/$versionId")
                .header("User-Agent", USER_AGENT).build()
            val ver = client.newCall(req).execute().use {
                if (!it.isSuccessful) return@withContext null
                JSONObject(it.body?.string() ?: "{}")
            }
            val files = ver.optJSONArray("files") ?: return@withContext null
            val primaryFile = (0 until files.length()).firstOrNull { i ->
                files.getJSONObject(i).optBoolean("primary", false)
            } ?: 0
            val file = files.getJSONObject(primaryFile)
            val url = file.getString("url")
            val filename = file.getString("filename")
            val sha1 = file.optJSONObject("hashes")?.optString("sha1")

            val outDir = com.mclauncher.MCApplication.modsDir()
            val outFile = java.io.File(outDir, filename)
            outFile.parentFile?.mkdirs()

            DownloadManager.download(url, outFile.absolutePath, filename, "Mod ${mod.title}") { p, _, _ ->
                onProgress(p)
            }

            // 校验 sha1
            if (sha1 != null && outFile.exists()) {
                val actual = computeSha1(outFile)
                if (actual != sha1) {
                    outFile.delete()
                    return@withContext null
                }
            }
            outFile.absolutePath
        } catch (e: Exception) { null }
    }

    private fun computeSha1(file: java.io.File): String {
        val md = java.security.MessageDigest.getInstance("SHA-1")
        val buf = ByteArray(8192)
        java.io.FileInputStream(file).use { fis ->
            var n = fis.read(buf)
            while (n > 0) { md.update(buf, 0, n); n = fis.read(buf) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
