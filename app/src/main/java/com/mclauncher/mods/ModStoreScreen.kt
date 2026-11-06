package com.mclauncher.mods

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mclauncher.data.ModrinthMod
import com.mclauncher.data.ModrinthApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModStoreScreen(
    onInstall: (ModrinthMod) -> Unit,
    currentLoader: String = "fabric",
    currentGameVersion: String = "1.21.1"
) {
    var query by remember { mutableStateOf("") }
    var mods by remember { mutableStateOf<List<ModrinthMod>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("all") }
    
    val scope = rememberCoroutineScope()
    
    val categories = listOf(
        "all" to "全部",
        "technology" to "⚙️ 科技",
        "magic" to "✨ 魔法",
        "adventure" to "🗺️ 冒险",
        "food" to "🍔 食物",
        "automation" to "🤖 自动化",
        "utility" to "🔧 工具",
        "minecraft" to "🎮 原版",
    )
    
    LaunchedEffect(Unit) {
        loading = true
        try {
            mods = ModrinthApi.search(
                query = "",
                gameVersion = currentGameVersion,
                loader = currentLoader,
                limit = 30
            ).hits
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        // 搜索栏
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; scope.launch { /* 实时搜索 */ } },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("搜索模组...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4DBD2D),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color(0xFF4DBD2D)
            )
        )
        
        // 分类标签
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { (key, label) ->
                FilterChip(
                    selected = selectedCategory == key,
                    onClick = { selectedCategory = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4DBD2D),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        
        // 模组列表
        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = Color(0xFF4DBD2D)
            )
        }
        
        error?.let { err ->
            Text(
                "⚠️ $err",
                modifier = Modifier.padding(16.dp),
                color = Color.Red
            )
        }
        
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mods) { mod ->
                ModCard(mod = mod, onInstall = { onInstall(mod) })
            }
        }
    }
}

@Composable
fun ModCard(mod: ModrinthMod, onInstall: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标（用 emoji 占位或网络加载）
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF4DBD2D), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📦", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mod.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    buildString {
                        append("⬇️ ${formatDownloads(mod.downloads)}")
                        if (mod.follows > 0) append("   ⭐ ${mod.follows}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    mod.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0B0B0),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DBD2D))
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.height(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("下载")
            }
        }
    }
}

private fun formatDownloads(n: Long): String = when {
    n >= 1_000_000_000 -> "${n / 1_000_000_000}亿+"
    n >= 10_000_000 -> "${n / 10_000}万+"
    n >= 10_000 -> "${n / 1000}K+"
    else -> n.toString()
}
