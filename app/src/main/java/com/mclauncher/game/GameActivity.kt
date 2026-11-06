package com.mclauncher.game

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mclauncher.data.LocalVersion
import com.mclauncher.keyboard.TouchOverlay
import com.mclauncher.ui.theme.MCLauncherTheme
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
        
        // 全屏无状态栏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // 从 Intent 读取版本信息
        val versionId = intent.getStringExtra("version_id") ?: run {
            finish()
            return
        }
        version = LocalVersion(
            id = versionId,
            type = "release",
            installed = true
        )
        
        runner = BoardwalkRunner(this)
        
        setContent {
            MCLauncherTheme {
                GameScreen()
            }
        }
        
        // 启动游戏
        scope.launch(Dispatchers.IO) {
            val config = GameSession().buildLaunchArgs(
                this@GameActivity,
                version!!
            )
            runner.launch(config)
        }
    }
    
    @Composable
    fun GameScreen() {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // 游戏渲染视图（SurfaceView 或 Boardwalk 的 GL 视图）
            Box(modifier = Modifier.fillMaxSize()) {
                // 按键悬浮层
                TouchOverlay(onKeyEvent = { keyCode, action ->
                    // 发送按键事件到 Boardwalk
                    runner.sendKeyEvent(keyCode, action)
                })
            }
        }
    }
    
    override fun onBackPressed() {
        if (runner.isRunning()) {
            // 二次确认
            runner.stop()
        }
        super.onBackPressed()
    }
    
    override fun onDestroy() {
        runner.stop()
        super.onDestroy()
    }
}
