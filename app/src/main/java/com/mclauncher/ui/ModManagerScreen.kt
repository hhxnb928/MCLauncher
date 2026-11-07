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
import com.mclauncher.data.ModManager
import com.mclauncher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModManagerScreen() {
    var mods by remember { mutableStateOf(ModManager.scan()) }

    Column(modifier = Modifier.fillMaxSize().background(MC_BLACK)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF141414)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📁 模组管理", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { mods = ModManager.scan() }) {
                    Icon(Icons.Default.Refresh, null, tint = MC_GREEN, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新", color = MC_GREEN)
                }
            }
        }

        if (mods.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, null, tint = MC_TEXT_SECONDARY, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("还没有模组", color = MC_TEXT_SECONDARY, fontSize = 14.sp)
                    Text("去 🛒 模组商店 下载", color = MC_TEXT_SECONDARY, fontSize = 12.sp)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Text("✅ 已启用: ${mods.count { it.enabled }} | ⛔ 已禁用: ${mods.count { !it.enabled }}",
                        color = MC_TEXT_SECONDARY, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)) }
                    items(mods) { mod ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (mod.enabled) Color(0xFF1E1E1E) else Color(0xFF2A1E1E)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).background(
                                        if (mod.enabled) MC_GREEN.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (mod.enabled) Icons.Default.Extension else Icons.Default.Block,
                                        null,
                                        tint = if (mod.enabled) MC_GREEN else Color.Red,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        mod.name, color = if (mod.enabled) Color.White else Color.Red,
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${(mod.sizeBytes / 1024f).toInt()} KB | ${if (mod.enabled) "✅ 启用" else "⛔ 禁用"}",
                                        color = MC_TEXT_SECONDARY, fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = {
                                    ModManager.toggle(mod)
                                    mods = ModManager.scan()
                                }) {
                                    Icon(
                                        if (mod.enabled) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        null,
                                        tint = if (mod.enabled) MC_YELLOW else MC_GREEN
                                    )
                                }
                                IconButton(onClick = {
                                    ModManager.remove(mod)
                                    mods = ModManager.scan()
                                }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
