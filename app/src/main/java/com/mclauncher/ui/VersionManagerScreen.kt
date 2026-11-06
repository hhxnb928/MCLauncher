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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionManagerScreen() {
    var manifest by remember { mutableStateOf<MojangVersionManifest?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tabIndex by remember { mutableStateOf(0) }
    var installingId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val tabs = listOf("Release", "Snapshot", "旧版")

    LaunchedEffect(Unit) {
        loading = true
        try {
            manifest = MojangApi.fetchManifest()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MC_BLACK).padding(24.dp)) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📦 版本管理",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                scope.launch {
                    loading = true
                    manifest = null
                    try { manifest = MojangApi.fetchManifest() }
                    catch (e: Exception) { error = e.message }
                    finally { loading = false }
                }
            }) {
                Icon(Icons.Default.Refresh, null, tint = MC_TEXT_SECONDARY)
            }
        }

        // Tab 切换
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color(0xFF1E1E1E),
            contentColor = MC_GREEN
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = tabIndex == idx,
                    onClick = { tabIndex = idx },
                    text = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 加载状态
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

        manifest?.let { mf ->
            val filtered = mf.versions.filter { v ->
                when (tabIndex) {
                    0 -> v.type == "release"
                    1 -> v.type == "snapshot"
                    else -> v.type != "release" && v.type != "snapshot"
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        "共 ${filtered.size} 个版本",
                        color = MC_TEXT_SECONDARY,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(filtered.take(50)) { v ->
                    VersionRow(
                        version = v,
                        isLatest = v.id == mf.latest.release || v.id == mf.latest.snapshot,
                        isInstalling = installingId == v.id,
                        onInstall = {
                            installingId = v.id
                            // 模拟下载
                            scope.launch {
                                try {
                                    MojangApi.cacheVersionJson(v.id)
                                    // 下载 libraries...
                                } catch (e: Exception) {}
                                installingId = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VersionRow(
    version: MojangVersion,
    isLatest: Boolean,
    isInstalling: Boolean,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        version.id,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isLatest) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = Color(0xFFFFD700)) {
                            Text("最新", fontSize = 10.sp, color = Color.Black)
                        }
                    }
                }
                Text(
                    "📅 ${version.releaseTime.substringBefore('T')}",
                    color = MC_TEXT_SECONDARY,
                    fontSize = 12.sp
                )
            }

            if (isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MC_GREEN,
                    strokeWidth = 2.dp
                )
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
