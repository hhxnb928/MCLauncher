package com.mclauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

enum class InstallStep {
    CACHING_MANIFEST, DOWNLOADING_CLIENT, DOWNLOADING_LIBRARIES,
    DOWNLOADING_ASSETS, DOWNLOADING_LOADER, COMPLETE, ERROR
}

data class InstallProgress(
    val step: InstallStep,
    val stepName: String,
    val stepProgress: Float = 0f,
    val overallProgress: Float = 0f,
    val message: String = ""
)

enum class LoaderChoice { NONE, FABRIC, FORGE, NEOFORGE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionManagerScreen() {
    val scope = rememberCoroutineScope()
    var versions by remember { mutableStateOf<List<MojangVersion>>(emptyList()) }
    var installed by remember { mutableStateOf<List<LocalVersion>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var installProgress by remember { mutableStateOf<InstallProgress?>(null) }
    var showLoaderPicker by remember { mutableStateOf<MojangVersion?>(null) }

    LaunchedEffect(Unit) { refreshAll { versions = it } }
    LaunchedEffect(Unit) { installed = loadInstalled() }

    Column(modifier = Modifier.fillMaxSize().background(MC_BLACK)) {
        // 顶部栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF141414),
            shape = RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📦 版本管理", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    scope.launch { refreshAll { versions = it }; installed = loadInstalled() }
                }) { Icon(Icons.Default.Refresh, null, tint = MC_GREEN) }
            }
        }

        // 安装进度
        installProgress?.let { p ->
            InstallProgressCard(p)
        }

        // 主区域 - 可滚动
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // === 已安装版本分组 ===
                item {
                    Text("✅ 已安装 (${installed.size})", color = MC_GREEN, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                }
                if (installed.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                        ) {
                            Text("  暂无已安装版本, 下方选一个安装 →", color = MC_TEXT_SECONDARY,
                                modifier = Modifier.padding(20.dp), fontSize = 13.sp)
                        }
                    }
                } else {
                    items(installed) { lv ->
                        InstalledVersionCard(
                            version = lv,
                            onUninstall = {
                                scope.launch(Dispatchers.IO) {
                                    uninstallVersion(lv)
                                    installed = loadInstalled()
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Text("📥 可安装版本 (Mojang)", color = MC_TEXT_SECONDARY, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                }

                if (versions.isEmpty() && !loading) {
                    item {
                        Text("加载中...", color = MC_TEXT_SECONDARY,
                            modifier = Modifier.padding(20.dp))
                    }
                }

                items(versions) { v ->
                    val isInstalled = installed.any { it.id == v.id }
                    MojangVersionCard(
                        version = v,
                        isInstalled = isInstalled,
                        installProgress = installProgress,
                        onInstall = {
                            showLoaderPicker = v
                        },
                        onUninstall = {
                            scope.launch(Dispatchers.IO) {
                                installed.firstOrNull { it.id == v.id }?.let { uninstallVersion(it) }
                                installed = loadInstalled()
                            }
                        }
                    )
                }
            }
        }
    }

    // 加载器选择对话框
    showLoaderPicker?.let { v ->
        LoaderPickerDialog(
            mcVersion = v.id,
            onConfirm = { loader ->
                showLoaderPicker = null
                scope.launch(Dispatchers.IO) {
                    installVersion(v.id, loader, onProgress = { installProgress = it })
                    installed = loadInstalled()
                    installProgress = null
                }
            },
            onDismiss = { showLoaderPicker = null }
        )
    }
}

// ========== 已安装版本卡片 (显示删除) ==========
@Composable
fun InstalledVersionCard(
    version: LocalVersion,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(MC_GREEN.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = MC_GREEN, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(version.id, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("✅ 已安装", color = MC_GREEN, fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = onUninstall,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("删除", fontSize = 13.sp)
            }
        }
    }
}

// ========== Mojang 版本卡片 ==========
@Composable
fun MojangVersionCard(
    version: MojangVersion,
    isInstalled: Boolean,
    installProgress: InstallProgress?,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val installing = installProgress != null &&
        installProgress.message.contains(version.id) ||
        installProgress?.step == InstallStep.DOWNLOADING_CLIENT

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInstalled) Color(0xFF222A22) else Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(
                    if (version.type == "release") MC_GREEN.copy(alpha = 0.2f)
                    else MC_YELLOW.copy(alpha = 0.2f),
                    RoundedCornerShape(10.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (version.type == "release") "R" else "S",
                    color = if (version.type == "release") MC_GREEN else MC_YELLOW,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(version.id, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (version.type == "release") "Release" else "Snapshot", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF2A2A2A))
                    )
                }
            }
            if (isInstalled) {
                OutlinedButton(
                    onClick = onUninstall,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除", fontSize = 13.sp)
                }
            } else if (installing) {
                CircularProgressIndicator(color = MC_GREEN, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            } else {
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = MC_GREEN),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("安装", fontSize = 13.sp)
                }
            }
        }
    }
}

// ========== 安装进度卡片 ==========
@Composable
fun InstallProgressCard(progress: InstallProgress) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MC_GREEN.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("📥 ${progress.stepName}", color = MC_GREEN, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.overallProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MC_GREEN, trackColor = Color(0xFF2A2A2A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${(progress.overallProgress * 100).toInt()}% | ${progress.message}",
                color = MC_TEXT_SECONDARY, fontSize = 11.sp
            )
        }
    }
}

// ========== 加载器选择对话框 ==========
@Composable
fun LoaderPickerDialog(
    mcVersion: String,
    onConfirm: (LoaderChoice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("安装 Minecraft $mcVersion") },
        text = {
            Column {
                Text("选择模组加载器 (或不选 = 原版)", color = MC_TEXT_SECONDARY, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                val options = listOf(
                    LoaderChoice.NONE to "原版 (Vanilla)",
                    LoaderChoice.FABRIC to "Fabric Loader (推荐)",
                    LoaderChoice.FORGE to "Forge",
                    LoaderChoice.NEOFORGE to "NeoForge"
                )
                options.forEach { (l, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onConfirm(l) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = false, onClick = { onConfirm(l) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ========== 辅助函数 ==========
suspend fun refreshAll(onDone: (List<MojangVersion>) -> Unit) {
    onDone(MojangApi.fetchManifest().versions.filter { it.type == "release" }.take(20))
}

suspend fun loadInstalled(): List<LocalVersion> = withContext(Dispatchers.IO) {
    try {
        val dir = com.mclauncher.MCApplication.versionsDir()
        dir.listFiles()?.filter { it.isDirectory }?.map { d ->
            val jsonFile = File(d, "${d.name}.json")
            val type = if (jsonFile.exists()) {
                try { JSONObject(jsonFile.readText()).optString("type", "custom") } catch (_: Exception) { "custom" }
            } else "custom"
            LocalVersion(id = d.name, type = type, installed = true)
        }?.sortedByDescending { it.id } ?: emptyList()
    } catch (e: Exception) { emptyList() }
}

fun uninstallVersion(version: LocalVersion) {
    val dir = File(com.mclauncher.MCApplication.versionsDir(), version.id)
    if (dir.exists()) dir.deleteRecursively()
}

suspend fun installVersion(
    versionId: String,
    loader: LoaderChoice,
    onProgress: (InstallProgress) -> Unit
) {
    val versionsDir = com.mclauncher.MCApplication.versionsDir()
    val versionDir = File(versionsDir, versionId)
    versionDir.mkdirs()

    try {
        // 1. 缓存版本 JSON
        onProgress(InstallProgress(InstallStep.CACHING_MANIFEST, "下载版本清单...", overallProgress = 0.02f, message = versionId))
        val jsonFile = MojangApi.cacheVersionJson(versionId)
        if (!jsonFile.exists()) {
            onProgress(InstallProgress(InstallStep.ERROR, "版本清单下载失败")); return
        }

        val json = JSONObject(jsonFile.readText())
        val clientUrl = json.optJSONObject("downloads")?.optJSONObject("client")?.optString("url") ?: ""
        val clientSize = json.optJSONObject("downloads")?.optJSONObject("client")?.optLong("size", 0) ?: 0
        val assetIndexId = json.optJSONObject("assetIndex")?.optString("id") ?: versionId
        val assetIndexUrl = json.optJSONObject("assetIndex")?.optString("url") ?: ""

        // 2. 下载客户端 jar
        if (clientUrl.isNotEmpty()) {
            onProgress(InstallProgress(InstallStep.DOWNLOADING_CLIENT, "下载游戏主包...", overallProgress = 0.1f, message = "${versionId}.jar"))
            val clientFile = File(versionDir, "$versionId.jar")
            if (!clientFile.exists()) {
                DownloadManager.download(
                    url = clientUrl, outputPath = clientFile.absolutePath,
                    name = "$versionId.jar", description = "Client"
                ) { p, _, _ ->
                    onProgress(InstallProgress(InstallStep.DOWNLOADING_CLIENT, "下载游戏主包...", stepProgress = p, overallProgress = 0.1f + p * 0.25f, message = "${(p*100).toInt()}%"))
                }
            }
        }

        // 3. 下载 libraries
        onProgress(InstallProgress(InstallStep.DOWNLOADING_LIBRARIES, "下载依赖库...", overallProgress = 0.35f))
        LibraryDownloader.downloadForVersion(json) { p, name ->
            onProgress(InstallProgress(InstallStep.DOWNLOADING_LIBRARIES, "下载依赖库... ($name)", stepProgress = p, overallProgress = 0.35f + p * 0.3f))
        }

        // 4. 下载 assets
        onProgress(InstallProgress(InstallStep.DOWNLOADING_ASSETS, "下载资源文件...", overallProgress = 0.65f))
        if (assetIndexUrl.isNotEmpty()) {
            val indexFile = File(com.mclauncher.MCApplication.assetsDir(), "indexes/$assetIndexId.json")
            indexFile.parentFile?.mkdirs()
            DownloadManager.download(assetIndexUrl, indexFile.absolutePath, "$assetIndexId.json", "Asset Index") { _, _, _ -> }

            try {
                val indexJson = JSONObject(indexFile.readText())
                val assets = indexJson.optJSONObject("objects")
                var count = 0; val total = assets?.length() ?: 1
                assets?.keys()?.forEach { name ->
                    val obj = assets.getJSONObject(name)
                    val hash = obj.getString("hash")
                    val prefix = hash.take(2)
                    val outFile = File(com.mclauncher.MCApplication.assetsDir(), "objects/$prefix/$hash")
                    if (!outFile.exists()) {
                        val url = "https://resources.download.minecraft.net/$prefix/$hash"
                        DownloadManager.download(url, outFile.absolutePath, name, "Asset") { _, _, _ -> }
                    }
                    count++
                    if (count % 20 == 0) {
                        val p = count.toFloat() / total
                        onProgress(InstallProgress(InstallStep.DOWNLOADING_ASSETS, "下载资源 $count/$total", stepProgress = p, overallProgress = 0.65f + p * 0.3f))
                    }
                }
            } catch (_: Exception) {}
        }

        // 5. 下载加载器
        if (loader != LoaderChoice.NONE) {
            onProgress(InstallProgress(InstallStep.DOWNLOADING_LOADER, "下载模组加载器...", overallProgress = 0.95f))
            val ldr = LoaderManager.Loader.valueOf(loader.name)
            LoaderManager.downloadLoader(ldr, "", versionId) { p, _, _ ->
                onProgress(InstallProgress(InstallStep.DOWNLOADING_LOADER, "下载加载器...", stepProgress = p, overallProgress = 0.95f))
            }
        }

        onProgress(InstallProgress(InstallStep.COMPLETE, "✅ 安装完成!", overallProgress = 1f, message = versionId))
    } catch (e: Exception) {
        onProgress(InstallProgress(InstallStep.ERROR, "安装失败: ${e.message}", overallProgress = 0f))
    }
}
