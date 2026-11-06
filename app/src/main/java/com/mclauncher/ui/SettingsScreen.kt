package com.mclauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.mclauncher.data.JreManager
import com.mclauncher.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()
    var memoryMB by remember { mutableStateOf(4096) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadCurrentFile by remember { mutableStateOf("") }
    var jre8Installed by remember { mutableStateOf(JreManager.isInstalled("jre8")) }
    var jre17Installed by remember { mutableStateOf(JreManager.isInstalled("jre17")) }

    Column(modifier = Modifier.fillMaxSize().background(MC_BLACK).padding(24.dp)) {
        Text("⚙️ 设置", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // ========== 内存设置 ==========
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🖥️ 内存分配", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("分配给 Minecraft 的最大内存", color = MC_TEXT_SECONDARY, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))

                val presets = listOf(2048 to "2GB", 4096 to "4GB", 6144 to "6GB", 8192 to "8GB")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (mb, label) ->
                        FilterChip(
                            selected = memoryMB == mb,
                            onClick = { memoryMB = mb },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = memoryMB.toFloat(),
                        onValueChange = { memoryMB = it.toInt() },
                        valueRange = 512f..16384f,
                        steps = 6,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("${memoryMB}MB", color = MC_GREEN, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ========== JRE 下载 ==========
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("☕ Java 运行环境", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Pojav 官方 OpenJDK Android port", color = MC_TEXT_SECONDARY, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("JRE 8", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("MC 1.16.5 及更早版本", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
                    }
                    if (jre8Installed) {
                        AssistChip(
                            onClick = { jre8Installed = JreManager.isInstalled("jre8") },
                            label = { Text("✅ 已安装", color = MC_GREEN) }
                        )
                    } else {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    downloading = true
                                    JreManager.download("jre8") { p, _, _ ->
                                        downloadProgress = p
                                    }
                                    jre8Installed = JreManager.isInstalled("jre8")
                                    downloading = false
                                }
                            },
                            enabled = !downloading,
                            colors = ButtonDefaults.buttonColors(containerColor = MC_GREEN)
                        ) { Text("下载") }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("JRE 17", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("MC 1.17+ (推荐)", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
                    }
                    if (jre17Installed) {
                        AssistChip(
                            onClick = { jre17Installed = JreManager.isInstalled("jre17") },
                            label = { Text("✅ 已安装", color = MC_GREEN) }
                        )
                    } else {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    downloading = true
                                    JreManager.download("jre17") { p, _, _ ->
                                        downloadProgress = p
                                    }
                                    jre17Installed = JreManager.isInstalled("jre17")
                                    downloading = false
                                }
                            },
                            enabled = !downloading,
                            colors = ButtonDefaults.buttonColors(containerColor = MC_GREEN)
                        ) { Text("下载") }
                    }
                }

                if (downloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MC_GREEN, trackColor = Color(0xFF2A2A2A)
                    )
                }
            }
        }

        // ========== 关于 ==========
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ℹ️ 关于", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("MCLauncher v1.0.0", color = MC_GREEN, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Android 上的 Minecraft Java 启动器", color = MC_TEXT_SECONDARY, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("底层技术:", color = MC_TEXT_SECONDARY, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("  • GL4ES (OpenGL → GLES 转译)", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
                Text("  • LWJGL3 Android port", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
                Text("  • OpenJDK Android port (Pojav)", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
                Text("  • PCL 风格 UI (Jetpack Compose)", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
            }
        }
    }
}
