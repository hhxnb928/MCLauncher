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
import com.mclauncher.data.LocalVersion
import com.mclauncher.data.LoaderType
import com.mclauncher.ui.theme.*

@Composable
fun HomeScreen(onLaunchGame: (String) -> Unit) {
    var selectedVersion by remember { mutableStateOf<LocalVersion?>(null) }
    var launching by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var progressText by remember { mutableStateOf("准备就绪") }

    val versions = remember {
        listOf(
            LocalVersion("1.21.1", "release", installed = true),
            LocalVersion("1.20.6-Fabric", "release", installed = true, loader = LoaderType.FABRIC, loaderVersion = "0.16.5"),
            LocalVersion("1.20.1-Forge", "release", installed = true, loader = LoaderType.FORGE, loaderVersion = "47.4.0"),
        )
    }

    // 默认选第一个已安装的
    LaunchedEffect(Unit) {
        selectedVersion = versions.firstOrNull { it.installed }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MC_BLACK).padding(24.dp)
    ) {
        // ========== 顶部欢迎 ==========
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "欢迎回来，矿工！⛏️",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
                    .format(java.util.Date()),
                color = MC_TEXT_SECONDARY,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 版本选择卡片（大） ==========
        selectedVersion?.let { ver ->
            VersionBigCard(
                version = ver,
                isSelected = true,
                onClick = { /* 版本管理页面 */ }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 启动按钮 ==========
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    if (!launching && selectedVersion != null) {
                        launching = true
                        progressText = "正在下载依赖库..."

                        // 模拟启动进度
                        Thread {
                            for (i in 1..10) {
                                Thread.sleep(200)
                                progress = i / 10f
                                val steps = listOf(
                                    "正在下载依赖库...",
                                    "检查 Java 环境...",
                                    "加载游戏版本...",
                                    "初始化模组加载器...",
                                    "加载模组...",
                                    "构建类路径...",
                                    "准备渲染引擎...",
                                    "启动游戏进程...",
                                )
                                progressText = steps.getOrNull(i - 1) ?: "启动完成！"
                            }
                            Thread.sleep(500)
                            launching = false
                            progress = 0f
                            progressText = "准备就绪"

                            // 启动游戏 Activity
                            onLaunchGame(selectedVersion!!.id)
                        }.start()
                    }
                },
                enabled = !launching && selectedVersion != null,
                modifier = Modifier
                    .width(280.dp)
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (launching) Color(0xFF3A9121) else MC_GREEN,
                    disabledContainerColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (launching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (launching) "启动中..." else "启动游戏",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(280.dp).height(4.dp),
                color = MC_GREEN,
                trackColor = Color(0xFF333333)
            )

            Text(
                progressText,
                color = MC_TEXT_SECONDARY,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ========== 已安装版本快速切换 ==========
        Text(
            "🔄 快速切换版本",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(versions) { ver ->
                VersionQuickRow(
                    version = ver,
                    isSelected = selectedVersion?.id == ver.id,
                    onClick = { selectedVersion = ver }
                )
            }
        }
    }
}

// 大版本卡片
@Composable
fun VersionBigCard(version: LocalVersion, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 版本图标（渐变方块）
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        when (version.loader) {
                            LoaderType.FABRIC -> Color(0xFFDBD0B4)
                            LoaderType.FORGE -> Color(0xFFD57F2C)
                            LoaderType.NEOFORGE -> Color(0xFFC9B26B)
                            else -> MC_GREEN
                        },
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (version.loader) {
                        LoaderType.FABRIC -> "🧵"
                        LoaderType.FORGE -> "⚒"
                        LoaderType.NEOFORGE -> "🔨"
                        else -> "⛏"
                    },
                    fontSize = 36.sp
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Minecraft ${version.id}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        containerColor = when (version.loader) {
                            LoaderType.VANILLA -> MC_GREEN
                            LoaderType.FABRIC -> Color(0xFFDBD0B4)
                            LoaderType.FORGE -> Color(0xFFD57F2C)
                            else -> ACCENT_PURPLE
                        }
                    ) {
                        Text(
                            when (version.loader) {
                                LoaderType.VANILLA -> "原版"
                                LoaderType.FABRIC -> "Fabric"
                                LoaderType.FORGE -> "Forge"
                                LoaderType.NEOFORGE -> "NeoForge"
                                LoaderType.QUILT -> "Quilt"
                            },
                            fontSize = 11.sp
                        )
                    }

                    if (version.loaderVersion.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Loader ${version.loaderVersion}",
                            color = MC_TEXT_SECONDARY,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (version.installed) "✅ 已安装" else "⬇️ 未安装",
                    color = if (version.installed) MC_GREEN else ACCENT_ORANGE,
                    fontSize = 13.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MC_TEXT_SECONDARY,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// 快速切换行
@Composable
fun VersionQuickRow(version: LocalVersion, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color(0xFF2A2A2A) else Color(0xFF1E1E1E),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isSelected) MC_GREEN else Color(0xFF444444),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            version.id,
            color = Color.White,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            when (version.loader) {
                LoaderType.VANILLA -> "原版"
                LoaderType.FABRIC -> "Fabric"
                LoaderType.FORGE -> "Forge"
                else -> version.loader.name
            },
            color = MC_TEXT_SECONDARY,
            fontSize = 12.sp
        )
    }
}
