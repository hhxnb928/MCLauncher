package com.mclauncher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.core.content.ContextCompat
import com.mclauncher.game.GameActivity
import com.mclauncher.keyboard.KeyboardMapperActivity
import com.mclauncher.mods.ModStoreScreen
import com.mclauncher.ui.ModManagerScreen
import com.mclauncher.ui.theme.*

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 处理权限结果 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求必要权限
        checkAndRequestPermissions()

        setContent {
            MCLauncherTheme {
                LauncherScaffold(
                    onLaunchGame = { versionId ->
                        startActivity(Intent(this, GameActivity::class.java).apply {
                            putExtra("version_id", versionId)
                        })
                    },
                    onOpenKeyboardMapper = {
                        startActivity(Intent(this, KeyboardMapperActivity::class.java))
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val perms = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            perms += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        val needGrant = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needGrant.isNotEmpty()) {
            requestPermissionLauncher.launch(needGrant.toTypedArray())
        }

        // Android 11+ 需要 MANAGE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }
}

// ========== 页面枚举 ==========
enum class Page(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("启动", Icons.Default.Home),
    VERSIONS("版本", Icons.Default.Inventory2),
    MODS_STORE("🛒 模组商店", Icons.Default.Extension),
    MOD_MANAGE("📁 模组管理", Icons.Default.Folder),
    SKINS("皮肤", Icons.Default.Person),
    KEYBOARD("按键", Icons.Default.Keyboard),
    SETTINGS("设置", Icons.Default.Settings)
}

// ========== 主 Scaffold ==========
@Composable
fun LauncherScaffold(
    onLaunchGame: (String) -> Unit,
    onOpenKeyboardMapper: () -> Unit
) {
    var currentPage by remember { mutableStateOf(Page.HOME) }

    Row(modifier = Modifier.fillMaxSize().background(MC_BLACK)) {
        // ========== 左侧边栏（PCL 风格） ==========
        Sidebar(
            currentPage = currentPage,
            onPageSelected = { currentPage = it }
        )

        // ========== 主内容区 ==========
        Box(modifier = Modifier.fillMaxSize().background(MC_BLACK)) {
            when (currentPage) {
                Page.HOME -> HomeScreen(onLaunchGame = onLaunchGame)
                Page.VERSIONS -> VersionManagerScreen()
                Page.MODS_STORE -> ModStoreScreen()
                Page.MOD_MANAGE -> ModManagerScreen()
                Page.SKINS -> SkinManagerScreen()
                Page.KEYBOARD -> KeyboardScreen(onOpenEditor = onOpenKeyboardMapper)
                Page.SETTINGS -> SettingsScreen()
            }
        }
    }
}

// ========== 侧边栏 ==========
@Composable
fun Sidebar(currentPage: Page, onPageSelected: (Page) -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(Color(0xFF141414))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MC_GREEN, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("⛏", color = Color.White, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 导航按钮
        Page.values().forEach { page ->
            SidebarButton(
                icon = page.icon,
                label = page.label,
                selected = currentPage == page,
                onClick = { onPageSelected(page) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // 版本信息
        Text(
            "v1.0.0",
            color = MC_TEXT_SECONDARY,
            fontSize = 10.sp
        )
    }
}

@Composable
fun SidebarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MC_GREEN else MC_TEXT_SECONDARY,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            color = if (selected) MC_GREEN else MC_TEXT_SECONDARY,
            fontSize = 11.sp
        )
    }
}
