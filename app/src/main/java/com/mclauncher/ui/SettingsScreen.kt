package com.mclauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.mclauncher.ui.theme.*

@Composable
fun SettingsScreen() {
    var memoryMB by remember { mutableIntStateOf(4096) }
    var fullscreen by remember { mutableStateOf(true) }
    var autoUpdate by remember { mutableStateOf(true) }
    var theme by remember { mutableStateOf("MC 绿色") }
    var autoClose by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(MC_BLACK)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "⚙️ 设置",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // ========== Java 设置 ==========
        Section(title = "☕ Java 运行环境") {
            SettingRow(
                title = "Java 内存分配",
                desc = "分配给 Minecraft 的最大堆内存"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2048, 4096, 8192, 16384).forEach { mb ->
                        FilterChip(
                            selected = memoryMB == mb,
                            onClick = { memoryMB = mb },
                            label = { Text("${mb/1024}G") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MC_GREEN
                            )
                        )
                    }
                }
            }

            SettingRow(
                title = "使用系统 JDK",
                desc = "优先使用 Termux 里的 JDK"
            ) {
                var useSystem by remember { mutableStateOf(false) }
                Switch(checked = useSystem, onCheckedChange = { useSystem = it })
            }

            SettingRow(
                title = "自定义 JVM 参数",
                desc = "如 -XX:+UseZGC"
            ) {
                Button(onClick = { /* 打开输入框 */ }) { Text("编辑") }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ========== 游戏设置 ==========
        Section(title = "🎮 游戏") {
            SettingRow(title = "全屏启动", desc = "进入游戏时自动全屏") {
                Switch(checked = fullscreen, onCheckedChange = { fullscreen = it })
            }
            SettingRow(title = "自动检查更新", desc = "启动时检测新版本") {
                Switch(checked = autoUpdate, onCheckedChange = { autoUpdate = it })
            }
            SettingRow(title = "启动后关闭启动器", desc = "节省后台内存") {
                Switch(checked = autoClose, onCheckedChange = { autoClose = it })
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ========== 外观 ==========
        Section(title = "🎨 外观") {
            SettingRow(title = "主题色", desc = "选择启动器颜色") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MC 绿色", "海洋蓝", "紫罗兰", "暮色橙", "暗夜").forEach { t ->
                        FilterChip(
                            selected = theme == t,
                            onClick = { theme = t },
                            label = { Text(t) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MC_GREEN)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ========== 高级 ==========
        Section(title = "🛠️ 高级") {
            SettingRow(title = "游戏目录", desc = "/sdcard/MCLauncher/.minecraft") {
                Button(onClick = { }) { Text("📂 打开") }
            }
            SettingRow(title = "清理缓存", desc = "删除下载的临时文件") {
                Button(onClick = { }) { Text("🧹 清理") }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// 设置分组卡片
@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// 设置行
@Composable
fun SettingRow(title: String, desc: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text(desc, color = MC_TEXT_SECONDARY, fontSize = 12.sp)
        }
        control()
    }
}
