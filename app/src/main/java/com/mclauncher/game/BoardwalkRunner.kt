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
 * Boardwalk = 类 Wine 的 Windows API 转译层
 * Pojav 的核心组件，让 Android 上能跑 LWJGL
 */
class BoardwalkRunner(private val context: Context) {
    
    companion object {
        private const val TAG = "BoardwalkRunner"
    }
    
    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log
    
    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state
    
    enum class State { IDLE, STARTING, RUNNING, CRASHED, STOPPED }
    
    private var process: Process? = null
    
    suspend fun launch(config: LaunchConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            _state.value = State.STARTING
            appendLog("▶ 启动游戏: ${config.versionId}")
            appendLog("主类: ${config.mainClass}")
            appendLog("JVM 参数: ${config.jvmArgs.joinToString(" ")}")
            
            // ========== 构建 Boardwalk 命令行 ==========
            val boardwalkBin = File(context.applicationInfo.nativeLibraryDir, "libboardwalk.so")
            if (!boardwalkBin.exists()) {
                appendLog("❌ Boardwalk 二进制不存在！")
                _state.value = State.CRASHED
                return@withContext false
            }
            
            // 完整命令
            val cmd = mutableListOf<String>()
            cmd += boardwalkBin.absolutePath
            cmd += "--"
            cmd += config.mainClass
            cmd.addAll(config.jvmArgs)
            cmd += "-cp"; cmd += config.classpath
            cmd.addAll(config.gameArgs)
            
            appendLog("执行: ${cmd.joinToString(" ") { if (it.length > 100) "...(截断)" else it }}")
            
            // ========== 启动进程 ==========
            val builder = ProcessBuilder(cmd)
            builder.directory(MCApplication.mcDir())
            builder.redirectErrorStream(true)
            builder.environment().apply {
                put("LD_LIBRARY_PATH", context.applicationInfo.nativeLibraryDir)
                put("HOME", MCApplication.gameDir.absolutePath)
                put("MCLAUNCHER_VERSION", "1.0.0")
            }
            
            process = builder.start()
            
            // 读取输出
            Thread {
                process?.inputStream?.bufferedReader()?.useLines { lines ->
                    lines.forEach { line ->
                        appendLog(line)
                    }
                }
            }.start()
            
            // 等待启动就绪
            Thread.sleep(1000)
            _state.value = State.RUNNING
            appendLog("✅ 游戏进程已启动")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "启动失败", e)
            appendLog("❌ 启动失败: ${e.message}")
            _state.value = State.CRASHED
            false
        }
    }
    
    fun stop() {
        try {
            process?.destroy()
            process?.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            process?.destroyForcibly()
            process = null
            _state.value = State.STOPPED
            appendLog("⏹ 游戏进程已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止失败", e)
        }
    }
    
    fun isRunning(): Boolean = process?.isAlive == true
    
    private fun appendLog(msg: String) {
        _log.value = _log.value + msg
        Log.d(TAG, msg)
    }
}
