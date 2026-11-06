package com.mclauncher.game

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mclauncher.data.LocalVersion
import com.mclauncher.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GameActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var runner: BoardwalkRunner
    private var version: LocalVersion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val versionId = intent.getStringExtra("version_id") ?: run { finish(); return }
        version = LocalVersion(id = versionId, type = "release", installed = true)
        runner = BoardwalkRunner(this)

        setContent { MCLauncherTheme { GameScreen() } }

        scope.launch(Dispatchers.IO) {
            try {
                val config = GameSession().buildLaunchArgs(this@GameActivity, version!!)
                runner.launch(config)
            } catch (e: Exception) {
                android.util.Log.e("GameActivity", "启动失败", e)
            }
        }
    }

    @Composable
    fun GameScreen() {
        val log by runner.log.collectAsState()
        val state by runner.state.collectAsState()

        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
            // 顶栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF141414),
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { runner.stop(); finish() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Text("🎮 ${version?.id}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    StatusBadge(state = state)
                }
            }

            // 日志区域
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                LazyColumn {
                    items(log) { line ->
                        val color = when {
                            line.contains("❌") || line.contains("失败") || line.contains("crash", true) -> Color.Red
                            line.contains("✅") || line.contains("启动") || line.contains("运行") -> MC_GREEN
                            line.contains("⚠️") || line.contains("警告") -> MC_YELLOW
                            else -> MC_TEXT_SECONDARY
                        }
                        Text(
                            text = line,
                            color = color,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun StatusBadge(state: BoardwalkRunner.State) {
        val (text, color) = when (state) {
            BoardwalkRunner.State.IDLE -> "空闲" to Color.Gray
            BoardwalkRunner.State.CHECKING -> "检测中" to MC_YELLOW
            BoardwalkRunner.State.JRE_MISSING -> "JRE缺失" to Color.Red
            BoardwalkRunner.State.STARTING -> "启动中" to MC_YELLOW
            BoardwalkRunner.State.RUNNING -> "运行中" to MC_GREEN
            BoardwalkRunner.State.CRASHED -> "已崩溃" to Color.Red
            BoardwalkRunner.State.STOPPED -> "已停止" to Color.Gray
        }
        Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.2f)) {
            Text(" ● $text", color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }

    override fun onBackPressed() { runner.stop(); super.onBackPressed() }
    override fun onDestroy() { runner.stop(); super.onDestroy() }
}
