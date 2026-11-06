package com.mclauncher.game

import android.content.Context
import android.util.Log
import com.mclauncher.MCApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * BoardwalkRunner — 用 Pojav 的 GL4ES + LWJGL Native 跑 MC
 *
 * 架构:
 *   1. 把 Minecraft main class 作为 JVM 入口
 *   2. LWJGL 通过 java.library.path 加载 liblwjgl.so / liblwjgl_opengl.so
 *   3. OpenGL 调用 → GL4ES (libgl4es_114.so) → OpenGL ES → GPU
 *   4. 音频走 libopenal.so, 字体走 libfreetype.so
 *
 * 注意: 当前阶段没有打包 JRE (OpenJDK), 所以只能显示日志界面
 */
class BoardwalkRunner(private val context: Context) {

    companion object {
        private const val TAG = "BoardwalkRunner"
    }

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    enum class State { IDLE, CHECKING, STARTING, RUNNING, CRASHED, STOPPED }

    private var process: Process? = null
    private var readerThread: Thread? = null

    private fun appendLog(msg: String) {
        val safe = try { msg } catch (e: Exception) { "(log error)" }
        _log.value = _log.value + safe
        Log.d(TAG, safe)
    }

    suspend fun launch(config: LaunchConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            _state.value = State.CHECKING
            appendLog("========================================")
            appendLog("🎮 MCLauncher Game Session")
            appendLog("========================================")
            appendLog("版本: ${config.versionId}")
            appendLog("主类: ${config.mainClass}")
            appendLog("")

            // 检查 native libs
            val nativeDir = context.applicationInfo.nativeLibraryDir
            val natives = listOf(
                "liblwjgl.so",
                "liblwjgl_opengl.so",
                "libgl4es_114.so",
                "libopenal.so",
                "libfreetype.so"
            )
            appendLog("📁 Native libs ($nativeDir):")
            for (n in natives) {
                val f = File(nativeDir, n)
                appendLog("  ${if (f.exists()) "✅" else "❌"} $n ${if (f.exists()) "(${(f.length()/1024).toInt()}KB)" else ""}")
            }
            appendLog("")

            // 检查 Java (Android 用 Zygote, 我们没法直接调 java 命令)
            // 真正的运行需要打包 OpenJDK Android port (100MB+)
            appendLog("⚠️ 当前阶段: 无法直接执行 Java main class")
            appendLog("   Android 没有原生 java 命令, 需要:")
            appendLog("   1. 打包 OpenJDK Android port (Pojav 的做法)")
            appendLog("   2. 或用 Termux 里的 java (用户自行安装)")
            appendLog("")

            // 检查游戏数据
            val mcDir = MCApplication.mcDir()
            val versionsDir = MCApplication.versionsDir()
            val versionDir = File(versionsDir, config.versionId)
            appendLog("📂 游戏数据:")
            appendLog("  .minecraft: ${mcDir.exists()} ${mcDir.absolutePath}")
            appendLog("  versions: ${versionDir.exists()} ${versionDir.absolutePath}")
            appendLog("  libraries: ${MCApplication.librariesDir().exists()}")
            appendLog("  assets: ${MCApplication.assetsDir().exists()}")
            appendLog("")

            if (!versionDir.exists()) {
                appendLog("❌ 版本 ${config.versionId} 未安装!")
                appendLog("   请先在 [版本管理] 里点击 [安装]")
                _state.value = State.CRASHED
                return@withContext false
            }

            _state.value = State.RUNNING
            appendLog("✅ Runner 就绪 (等待 JRE 集成)")
            true

        } catch (e: Exception) {
            Log.e(TAG, "启动异常", e)
            appendLog("❌ 异常: ${e.message}")
            e.stackTraceToString().lines().take(5).forEach { appendLog("  $it") }
            _state.value = State.CRASHED
            false
        }
    }

    fun stop() {
        try {
            process?.destroy()
            process = null
            readerThread?.interrupt()
            readerThread = null
            _state.value = State.STOPPED
            appendLog("⏹ 已停止")
        } catch (e: Exception) {
            Log.e(TAG, "stop", e)
        }
    }

    fun isRunning(): Boolean = _state.value == State.RUNNING
}
