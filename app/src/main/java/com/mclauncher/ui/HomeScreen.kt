package com.mclauncher.ui

import android.content.Intent
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
import com.mclauncher.game.GameActivity
import com.mclauncher.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLaunchGame: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf(AccountManager.list()) }
    var activeAccount by remember { mutableStateOf(AccountManager.active()) }
    var versions by remember { mutableStateOf<List<LocalVersion>>(emptyList()) }
    var showCreateAccount by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }

    // 加载本地版本
    LaunchedEffect(Unit) {
        versions = loadLocalVersions()
    }

    Column(modifier = Modifier.fillMaxSize().background(MC_BLACK).padding(24.dp)) {
        Text("欢迎回来，矿工！⛏️", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // ========== 账户卡片 ==========
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = MC_GREEN, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("👤 账户", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { showCreateAccount = true }) {
                        Icon(Icons.Default.Add, null, tint = MC_GREEN, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建", color = MC_GREEN)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (accounts.isEmpty()) {
                    Text("暂无账户，点击新建创建离线账户", color = MC_TEXT_SECONDARY, fontSize = 13.sp)
                } else {
                    accounts.forEach { acc ->
                        val isActive = acc.name == activeAccount?.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { AccountManager.setActive(acc.name); activeAccount = AccountManager.active() }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                null,
                                tint = if (isActive) MC_GREEN else MC_TEXT_SECONDARY
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.name, color = Color.White, fontSize = 14.sp)
                                Text("${acc.uuid.take(8)}...", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
                            }
                            IconButton(onClick = { AccountManager.remove(acc.name); accounts = AccountManager.list() }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // ========== 版本卡片 ==========
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory2, null, tint = MC_GREEN, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📦 已安装版本", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (versions.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无版本，请先在 [版本管理] 安装", color = MC_TEXT_SECONDARY, fontSize = 13.sp)
                    }
                } else {
                    versions.forEach { v ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(MC_GREEN.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("MC", color = MC_GREEN, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(v.id, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("✅ 已安装", color = MC_GREEN, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // ========== JRE 状态 ==========
        val jre8Ok = JreManager.isInstalled("jre8")
        val jre17Ok = JreManager.isInstalled("jre17")
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("☕ Java 运行环境", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (jre8Ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null,
                        tint = if (jre8Ok) MC_GREEN else Color.Red, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JRE 8 (MC <1.17)", color = if (jre8Ok) Color.White else Color.Red, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (jre17Ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null,
                        tint = if (jre17Ok) MC_GREEN else Color.Red, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JRE 17 (MC ≥1.17)", color = if (jre17Ok) Color.White else Color.Red, fontSize = 13.sp)
                }
                if (!jre8Ok || !jre17Ok) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("💡 在 [设置] 页面下载 JRE", color = MC_TEXT_SECONDARY, fontSize = 11.sp)
                }
            }
        }

        // ========== 启动按钮 ==========
        val launchEnabled = versions.isNotEmpty() && activeAccount != null
        Button(
            onClick = {
                versions.firstOrNull()?.let { v ->
                    onLaunchGame(v.id)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = launchEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MC_GREEN,
                disabledContainerColor = Color(0xFF333333)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (launchEnabled) "🚀 启动游戏" else "需要安装版本 + 账户",
                color = if (launchEnabled) Color.White else MC_TEXT_SECONDARY, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // 新建账户对话框
        if (showCreateAccount) {
            AlertDialog(
                onDismissRequest = { showCreateAccount = false },
                title = { Text("创建离线账户") },
                text = {
                    Column {
                        Text("输入游戏名字 (无需 Mojang 账号)", color = MC_TEXT_SECONDARY, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newAccountName,
                            onValueChange = { newAccountName = it },
                            label = { Text("玩家名字") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newAccountName.isNotBlank()) {
                            AccountManager.addOffline(newAccountName.trim())
                            accounts = AccountManager.list()
                            activeAccount = AccountManager.active()
                        }
                        newAccountName = ""
                        showCreateAccount = false
                    }) { Text("创建") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateAccount = false }) { Text("取消") }
                }
            )
        }
    }
}

private suspend fun loadLocalVersions(): List<LocalVersion> = withContext(Dispatchers.IO) {
    try {
        val dir = com.mclauncher.MCApplication.versionsDir()
        dir.listFiles()?.filter { it.isDirectory }?.map { d ->
            LocalVersion(id = d.name, type = "release", installed = true)
        }?.toList() ?: emptyList()
    } catch (e: Exception) { emptyList() }
}
