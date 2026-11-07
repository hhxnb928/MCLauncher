package com.mclauncher.mods

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mclauncher.data.ModrinthApi
import com.mclauncher.data.ModrinthMod
import com.mclauncher.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModStoreScreen() {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var loaderFilter by remember { mutableStateOf<String?>(null) }
    var versionFilter by remember { mutableStateOf<String?>(null) }
    var mods by remember { mutableStateOf<List<ModrinthMod>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf<Pair<String, Float>?>(null) }

    val load = {
        scope.launch(Dispatchers.IO) {
            loading = true; error = null
            mods = ModrinthApi.search(query = query, loader = loaderFilter, gameVersion = versionFilter)
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().background(MC_BLACK)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF141414)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🛒 模组商店 (Modrinth)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                // 搜索
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("搜索模组...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { load() }) { Icon(Icons.Default.Search, null, tint = MC_GREEN) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 过滤器
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(null to "全部", "fabric" to "Fabric", "forge" to "Forge", "neoforge" to "NeoForge").forEach { (ldr, label) ->
                        FilterChip(
                            selected = loaderFilter == ldr,
                            onClick = { loaderFilter = ldr; load() },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MC_GREEN)
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (loading) {
                    item { CircularProgressIndicator(modifier = Modifier.padding(20.dp)) }
                } else if (error != null) {
                    item { Text("❌ $error", color = Color.Red, modifier = Modifier.padding(20.dp)) }
                } else if (mods.isEmpty()) {
                    item { Text("未找到模组", color = MC_TEXT_SECONDARY, modifier = Modifier.padding(20.dp)) }
                }

                items(mods) { mod ->
                    ModCard(
                        mod = mod,
                        onDownload = {
                            scope.launch(Dispatchers.IO) {
                                downloadProgress = mod.title to 0f
                                val path = ModrinthApi.getMod(mod.slug)?.let { full ->
                                    full.latestVersionUrl?.let { url ->
                                        ModrinthApi.downloadMod(full, "", onProgress = { downloadProgress = mod.title to it })
                                    }
                                }
                                downloadProgress = null
                            }
                        },
                        downloading = downloadProgress?.first == mod.title,
                        progress = downloadProgress?.second ?: 0f
                    )
                }
            }
        }
    }
}

@Composable
fun ModCard(
    mod: ModrinthMod,
    onDownload: () -> Unit,
    downloading: Boolean,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(MC_YELLOW.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Extension, null, tint = MC_YELLOW, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mod.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(mod.description, color = MC_TEXT_SECONDARY, fontSize = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    mod.loader.forEach { l ->
                        AssistChip(
                            onClick = {},
                            label = { Text(l, fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MC_GREEN.copy(alpha = 0.2f))
                        )
                    }
                    Text("⬇️ ${(mod.downloads / 1000f).toInt()}K", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (downloading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                    Text("${(progress*100).toInt()}%", color = MC_GREEN, fontSize = 10.sp)
                }
            } else {
                FilledTonalButton(
                    onClick = onDownload,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MC_GREEN.copy(alpha = 0.2f), contentColor = MC_GREEN)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("下载", fontSize = 13.sp)
                }
            }
        }
    }
}
