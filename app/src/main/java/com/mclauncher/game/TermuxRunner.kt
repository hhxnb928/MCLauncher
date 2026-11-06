package com.mclauncher.game

import android.content.Context
import android.util.Log
import com.mclauncher.MCApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * TermuxRunner — 用 Termux 的 Java + Mesa Turnip (Android 原生 GPU 加速) 跑 MC
 *
 * 原理:
 *   1. 启动 Termux 的 bash (通过 exec)
 *   2. 设置 LD_LIBRARY_PATH / LIBGL_ALWAYS_SOFTWARE=0 (GPU)
 *   3. 用 Termux 里的 Java 跑 Minecraft main class
 *
 * GPU 加速: Mesa Turnip Vulkan 驱动 → LWJGL 的 Vulkan 后端
 * 或:       ANGLE (Vulkan on GLES) → LWJGL 的 OpenGL ES 后端
 */
class TermuxRunner(private val context: Context) {

    companion object {
        private const val TAG = "TermuxRunner"

        // Termux 路径常量
        private val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
        private val TERMUX_BASH = "$TERMUX_PREFIX/bin/bash"
        private val TERMUX_JAVA = "$TERMUX_PREFIX/bin/java"
    }

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    enum class State { IDLE, CHECKING_ENV, STARTING, RUNNING, CRASHED, STOPPED }

    private var process: Process? = null
    private var readerThread: Thread? = null

    // ========== 环境检测 ==========
    fun checkEnvironment(): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        result["termux_installed"] = File("/data/data/com.termux").exists()
        result["bash_exists"] = File(TERMUX_BASH).exists()
        result["java_exists"] = File(TERMUX_JAVA).exists()
        result["mesa_exists"] = File("$TERMUX_PREFIX/lib/libvulkan.so").exists() ||
                                File("$TERMUX_PREFIX/lib/libEGL.so").exists()
        return result
    }

    suspend fun launch(config: LaunchConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            _state.value = State.CHECKING_ENV
            appendLog("🔍 检测运行环境...")

            val env = checkEnvironment()
            env.forEach { (k, v) -> appendLog("  $k: ${if (v) "✅" else "❌"}") }

            if (!env["termux_installed"]!!) {
                appendLog("❌ Termux 未安装! 请先安装 Termux from F-Droid")
                appendLog("   https://f-droid.org/en/packages/com.termux/")
                _state.value = State.CRASHED
                return@withContext false
            }

            if (!env["java_exists"]!!) {
                appendLog("❌ Termux 里没有 Java! 请运行: pkg install openjdk-17")
                _state.value = State.CRASHED
                return@withContext false
            }

            _state.value = State.STARTING
            appendLog("▶ 启动: ${config.versionId}")

            // ========== 构建命令 ==========
            val jvmArgs = mutableListOf<String>()
            jvmArgs += "-Xms${config.jvmArgs.find { it.startsWith("-Xms") }?.substring(4) ?: "512m"}"
            jvmArgs += "-Xmx${config.jvmArgs.find { it.startsWith("-Xmx") }?.substring(4) ?: "2048m"}"

            // LWJGL 3 需要的关键属性 (让它走 OpenGL ES / Vulkan)
            jvmArgs += "-Djava.library.path=${MCApplication.nativesDir().absolutePath}:$TERMUX_PREFIX/lib"
            jvmArgs += "-Dorg.lwjgl.librarypath=${MCApplication.nativesDir().absolutePath}"
            jvmArgs += "-Dorg.lwjgl.opengl.egl.display=$TERMUX_PREFIX/lib/libEGL.so"
            jvmArgs += "-Dorg.lwjgl.opengl.gles2.display=$TERMUX_PREFIX/lib/libGLESv2.so"
            jvmArgs += "-Dminecraft.launcher.brand=MCLauncher"
            jvmArgs += "-Dminecraft.launcher.version=2.0.0-Termux"
            jvmArgs += "-Djava.net.preferIPv4Stack=true"
            jvmArgs += "-Dos.name=Linux"
            jvmArgs += "-Dos.arch=aarch64"

            // 构建 shell 命令 (Termux 里跑)
            val shellCmd = buildString {
                append("export PATH=$TERMUX_PREFIX/bin:\$PATH && ")
                append("export LD_LIBRARY_PATH=$TERMUX_PREFIX/lib:")
                append("${MCApplication.nativesDir().absolutePath}:")
                append("\$LD_LIBRARY_PATH && ")
                // 强制 GPU 加速 (Termux 里 Mesa Turnip 使用 Android 原生 GPU 驱动)
                append("export LIBGL_ALWAYS_SOFTWARE=0 && ")
                append("export GALLIUM_DRIVER=virpipe && ")
                append("export VULKAN_SDK=$TERMUX_PREFIX/lib && ")
                append("export ANDROID_DATA=/data && ")
                append("export EXTERNAL_STORAGE=/sdcard && ")
                append("cd ${MCApplication.mcDir().absolutePath} && ")
                append("$TERMUX_JAVA ")
                append(jvmArgs.joinToString(" "))
                append(" -cp '${config.classpath}' ")
                append(config.mainClass)
                append(" ${config.gameArgs.joinToString(" ")}")
            }

            appendLog("执行: $shellCmd")

            // ========== 启动 Termux bash ==========
            val builder = ProcessBuilder(TERMUX_BASH, "-c", shellCmd)
            builder.directory(MCApplication.mcDir())
            builder.redirectErrorStream(true)
            builder.environment().apply {
                put("HOME", MCApplication.gameDir.absolutePath)
                put("PATH", "$TERMUX_PREFIX/bin:/system/bin:/system/xbin")
                put("LD_LIBRARY_PATH", "$TERMUX_PREFIX/lib:/system/lib64")
            }

            process = builder.start()

            // 读取输出
            readerThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process!!.inputStream)).useLines { lines ->
                        lines.forEach { line -> appendLog(line) }
                    }
                } catch (e: Exception) { Log.w(TAG, "reader died", e) }
            }
            readerThread?.start()

            // 等启动
            Thread.sleep(1500)
            if (process?.isAlive == true) {
                _state.value = State.RUNNING
                appendLog("✅ 游戏进程已启动 (PID ${process?.pid()})")
            } else {
                appendLog("❌ 进程瞬间退出!")
                _state.value = State.CRASHED
            }
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
            appendLog("⏹ 进程已停止")
        } catch (e: Exception) { Log.e(TAG, "stop", e) }
    }

    fun isRunning() = process?.isAlive == true
    private fun appendLog(msg: String) {
        _log.value = _log.value + msg
        Log.d(TAG, msg)
    }
}

/** 轻量 Runner — 不依赖 Termux，直接用系统工具 */
class NativeRunner(private val context: Context) {
    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log
    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state
    enum class State { IDLE, STARTING, RUNNING, CRASHED, STOPPED }

    private var process: Process? = null

    suspend fun launch(config: LaunchConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            _state.value = State.STARTING
            appendLog("▶ NativeRunner (无 Termux)")
            appendLog("  注意: 纯 Native 无法跑 MC Java, 建议安装 Termux")
            _state.value = State.CRASHED
            false
        } catch (e: Exception) { false }
    }

    fun stop() { process?.destroy(); process = null; _state.value = State.STOPPED }
    fun isRunning() = process?.isAlive == true
    private fun appendLog(msg: String) { _log.value = _log.value + msg; Log.d("NativeRunner", msg) }
}
