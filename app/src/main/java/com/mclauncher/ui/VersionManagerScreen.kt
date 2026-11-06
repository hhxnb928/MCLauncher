package com.mclauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mclauncher.data.*
import com.mclauncher.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

enum class InstallStep {
    IDLE, CACHING_MANIFEST, DOWNLOADING_CLIENT, DOWNLOADING_LIBS,
    DOWNLOADING_ASSETS, DONE, ERROR
}

data class InstallProgress(
    val step: InstallStep = InstallStep.IDLE,
    val stepName: String = "",
    val stepProgress: Float = 0f,
    val overallProgress: Float = 0f,
    val speedBytesPerSec: Float = 0f,
    val currentFile: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionManagerScreen() {
    var manifest by remember { mutableStateOf<MojangVersionManifest?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tabIndex by remember { mutableStateOf(0) }
    var installingId by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(InstallProgress()) }
    val scope = rememberCoroutineScope()
    val tabs = listOf("Release", "Snapshot", "旧版")

    LaunchedEffect(Unit) {
        loading = true
        try { manifest = MojangApi.fetchManifest() }
        catch (e: Exception) { error = e.message }
        finally { loading = false }
    }

    Column(modifier = Modifier.fillMaxSize().background(MC_BLACK).padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📦 版本管理", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                scope.launch {
                    loading = true; manifest = null
                    try { manifest = MojangApi.fetchManifest() }
                    catch (e: Exception) { error = e.message }
                    finally { loading = false }
                }
            }) { Icon(Icons.Default.Refresh, null, tint = MC_TEXT_SECONDARY) }
        }

        TabRow(selectedTabIndex = tabIndex, containerColor = Color(0xFF1E1E1E), contentColor = MC_GREEN) {
            tabs.forEachIndexed { idx, label ->
                Tab(selected = tabIndex == idx, onClick = { tabIndex = idx }, text = { Text(label) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MC_GREEN)
            }
            return@Column
        }
        error?.let {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("⚠️ $it", color = Color.Red)
            }
            return@Column
        }

        if (installingId != null) {
            InstallProgressCard(progress = progress)
            Spacer(modifier = Modifier.height(16.dp))
        }

        manifest?.let { mf ->
            val filtered = mf.versions.filter { v ->
                when (tabIndex) { 0 -> v.type == "release"; 1 -> v.type == "snapshot"
                    else -> v.type != "release" && v.type != "snapshot" }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("共 ${filtered.size} 个版本", color = MC_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                items(filtered.take(50)) { v ->
                    VersionRow(
                        version = v,
                        isLatest = v.id == mf.latest.release || v.id == mf.latest.snapshot,
                        isInstalling = installingId == v.id,
                        installProgress = if (installingId == v.id) progress else null,
                        onInstall = {
                            installingId = v.id
                            progress = InstallProgress()
                            scope.launch {
                                installVersion(v.id, onProgress = { p ->
                                    withContext(Dispatchers.Main) { progress = p }
                                })
                                withContext(Dispatchers.Main) {
                                    installingId = null
                                    progress = InstallProgress()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InstallProgressCard(progress: InstallProgress) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A1E))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MC_GREEN, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(progress.stepName.ifEmpty { "正在安装..." }, color = MC_GREEN, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (progress.currentFile.isNotEmpty()) Text(progress.currentFile, color = MC_TEXT_SECONDARY, fontSize = 11.sp, maxLines = 1)
                }
                Text("${(progress.overallProgress * 100).toInt()}%", color = MC_GREEN, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress.overallProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp), color = MC_GREEN, trackColor = Color(0xFF2A2A2A))
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(progress.stepName, color = MC_TEXT_SECONDARY, fontSize = 11.sp)
                Spacer(modifier = Modifier.weight(1f))
                val kb = progress.speedBytesPerSec / 1024f
                if (kb > 10) Text("${kb.toInt()} KB/s", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun VersionRow(version: MojangVersion, isLatest: Boolean, isInstalling: Boolean,
               installProgress: InstallProgress?, onInstall: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(version.id, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (isLatest) { Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = Color(0xFFFFD700)) { Text("最新", fontSize = 10.sp, color = Color.Black) } }
                }
                Text("📅 ${version.releaseTime.substringBefore('T')}", color = MC_TEXT_SECONDARY, fontSize = 12.sp)
            }
            if (isInstalling && installProgress != null) {
                Button(onClick = {}, enabled = false, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MC_GREEN, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${(installProgress.overallProgress * 100).toInt()}%", fontSize = 12.sp)
                }
            } else {
                Button(onClick = onInstall, colors = ButtonDefaults.buttonColors(containerColor = MC_GREEN), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("安装", fontSize = 13.sp)
                }
            }
        }
    }
}

private suspend fun installVersion(versionId: String, onProgress: (InstallProgress) -> Unit) {
    try {
        onProgress(InstallProgress(step = InstallStep.CACHING_MANIFEST, stepName = "下载版本清单..."))
        val versionJsonStr = MojangApi.cacheVersionJson(versionId) ?: run {
            onProgress(InstallProgress(step = InstallStep.ERROR, stepName = "版本清单下载失败")); return }
        val versionJson = JSONObject(versionJsonStr)

        onProgress(InstallProgress(step = InstallStep.DOWNLOADING_CLIENT, stepName = "下载游戏主包...", overallProgress = 0.05f))
        val clientUrl = versionJson.optJSONObject("downloads")?.optJSONObject("client")?.optString("url")
        if (clientUrl != null) {
            val clientFile = com.mclauncher.MCApplication.versionsDir().resolve("$versionId/$versionId.jar")
            clientFile.parentFile?.mkdirs()
            if (!clientFile.exists()) {
                DownloadManager.download(url = clientUrl, outputPath = clientFile.absolutePath,
                    name = "$versionId.jar", description = "游戏主包",
                    onProgress = { p, _, _ -> onProgress(InstallProgress(step = InstallStep.DOWNLOADING_CLIENT,
                        stepName = "下载游戏主包", stepProgress = p, overallProgress = 0.05f + p * 0.15f)) })
            }
        }

        onProgress(InstallProgress(step = InstallStep.DOWNLOADING_LIBS, stepName = "下载依赖库...", overallProgress = 0.2f))
        val libs = versionJson.optJSONArray("libraries")
        if (libs != null) {
            val totalLibs = libs.length()
            for (i in 0 until totalLibs) {
                val lib = libs.getJSONObject(i)
                val artifact = lib.optJSONObject("downloads")?.optJSONObject("artifact") ?: continue
                val url = artifact.optString("url") ?: continue
                val path = artifact.optString("path") ?: continue
                val rules = lib.optJSONArray("rules")
                if (rules != null) {
                    var disallowed = false
                    for (j in 0 until rules.length()) {
                        if (rules.getJSONObject(j).optString("action") == "disallow") {
                            val os = rules.getJSONObject(j).optJSONObject("os")
                            if (os == null || os.optString("name") == "linux") disallowed = true
                        }
                    }
                    if (disallowed) continue
                }
                val outFile = com.mclauncher.MCApplication.librariesDir().resolve(path)
                if (outFile.exists() && outFile.length() > 0) continue
                try {
                    DownloadManager.download(url = url, outputPath = outFile.absolutePath,
                        name = path.substringAfterLast("/"), description = "库 ${i+1}/$totalLibs",
                        onProgress = { p, _, _ -> onProgress(InstallProgress(step = InstallStep.DOWNLOADING_LIBS,
                            stepName = "下载依赖库 (${i+1}/$totalLibs)", stepProgress = (i + p) / totalLibs,
                            overallProgress = 0.2f + (i + p) / totalLibs * 0.5f, currentFile = path.substringAfterLast("/"))) })
                } catch (_: Exception) {}
            }
        }

        onProgress(InstallProgress(step = InstallStep.DOWNLOADING_ASSETS, stepName = "下载资源文件...", overallProgress = 0.7f))
        val assetIndex = versionJson.optJSONObject("assetIndex")
        if (assetIndex != null) {
            val assetsDir = com.mclauncher.MCApplication.assetsDir()
            val idxFile = assetsDir.resolve("indexes/$versionId.json")
            idxFile.parentFile?.mkdirs()
            if (!idxFile.exists()) {
                DownloadManager.download(url = assetIndex.optString("url"), outputPath = idxFile.absolutePath,
                    name = "$versionId.json", description = "资源索引")
            }
            val objects = JSONObject(idxFile.readText()).optJSONObject("objects")
            if (objects != null) {
                val keyList = mutableListOf<String>()
                val it = objects.keys()
                while (it.hasNext()) keyList.add(it.next())
                val totalAssets = keyList.size
                var done = 0
                for (key in keyList) {
                    val hash = objects.getJSONObject(key).optString("hash")
                    val objFile = assetsDir.resolve("objects/${hash.substring(0, 2)}/$hash")
                    if (!objFile.exists()) {
                        objFile.parentFile?.mkdirs()
                        try { DownloadManager.download(
                            url = "https://resources.download.minecraft.net/${hash.substring(0, 2)}/$hash",
                            outputPath = objFile.absolutePath, name = key, description = "资源 $done/$totalAssets"
                        ) } catch (_: Exception) {}
                    }
                    done++
                    if (done % 30 == 0) onProgress(InstallProgress(step = InstallStep.DOWNLOADING_ASSETS,
                        stepName = "下载资源 ($done/$totalAssets)", stepProgress = done.toFloat() / totalAssets,
                        overallProgress = 0.7f + done.toFloat() / totalAssets * 0.25f, currentFile = key))
                }
            }
        }
        onProgress(InstallProgress(step = InstallStep.DONE, stepName = "✅ 安装完成!", overallProgress = 1f))
    } catch (e: Exception) {
        onProgress(InstallProgress(step = InstallStep.ERROR, stepName = "❌ 错误: ${e.message}"))
    }
}
