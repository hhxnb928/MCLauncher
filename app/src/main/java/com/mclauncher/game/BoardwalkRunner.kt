package com.mclauncher.game

import android.content.Context
import android.util.Log
import com.mclauncher.MCApplication
import com.mclauncher.data.JreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * 真实游戏启动器
 *
 * 1. 检查 JRE (JRE 8 for mc<1.17, JRE 17 for mc>=1.17)
 * 2. 构建 Java 命令行
 * 3. exec java 进程
 * 4. 环境变量 LD_LIBRARY_PATH = 我们的 native libs + GL4ES + OpenAL
 *
 * 关键环境变量:
 *   LD_LIBRARY_PATH -> libgl4es_114.so + liblwjgl_*.so + libopenal.so
 *   LIBGL_ALWAYS_SOFTWARE=0 -> 让 GL4ES 走硬件 GLES
 */
class BoardwalkRunner(private val context: Context) {

    companion object { private const val TAG = "MCLauncher" }

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    enum class State { IDLE, CHECKING, JRE_MISSING, STARTING, RUNNING, CRASHED, STOPPED }

    private var process: Process? = null
    private var readerThread: Thread? = null

    private fun appendLog(msg: String) {
        _log.value = _log.value + msg
        Log.d(TAG, msg)
    }

    suspend fun launch(config: LaunchConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            _state.value = State.CHECKING
            appendLog("╔══════════════════════════════════╗")
            appendLog("║  MCLauncher - Game Session       ║")
            appendLog("╚══════════════════════════════════╝")
            appendLog("")

            // 1. 检查版本文件
            val versionDir = File(MCApplication.versionsDir(), config.versionId)
            val versionJson = File(versionDir, "${config.versionId}.json")
            val versionJar = File(versionDir, "${config.versionId}.jar")
            appendLog("📦 版本: ${config.versionId}")
            appendLog("  JSON: ${if (versionJson.exists()) "✅" else "❌"}")
            appendLog("  JAR: ${if (versionJar.exists()) "✅" else "❌"}")
            if (!versionJson.exists() || !versionJar.exists()) {
                appendLog("❌ 版本未安装! 请先在[版本管理]安装")
                _state.value = State.CRASHED; return@withContext false
            }
            appendLog("")

            // 2. 检查 JRE
            val jreVer = JreManager.recommendJre(config.versionId)
            appendLog("☕ JRE: $jreVer")
            appendLog("  已安装: ${if (JreManager.isInstalled(jreVer)) "✅" else "❌"}")

            if (!JreManager.isInstalled(jreVer)) {
                appendLog("")
                appendLog("⚠️ JRE 未安装! 这是启动游戏的关键!")
                appendLog("   请在 [设置] 页面下载 Java Runtime")
                _state.value = State.JRE_MISSING; return@withContext false
            }

            val javaBin = File(JreManager.jreDir(jreVer), "bin/java")
            if (!javaBin.exists()) {
                // 尝试其他路径
                val candidates = listOf("bin/java", "java", "bin/art", "lib/server/libjvm.so")
                for (c in candidates) {
                    if (File(JreManager.jreDir(jreVer), c).exists()) {
                        appendLog("  找到: $c")
                        break
                    }
                }
            }
            appendLog("")

            // 3. 检查 Native libs
            val nativeDir = context.applicationInfo.nativeLibraryDir
            val keyLibs = listOf("liblwjgl.so", "liblwjgl_opengl.so", "libgl4es_114.so", "libopenal.so")
            appendLog("🔧 Native libs ($nativeDir):")
            for (n in keyLibs) {
                val f = File(nativeDir, n)
                appendLog("  ${if (f.exists()) "✅" else "❌"} $n")
            }
            appendLog("")

            // 4. 构建 Java 命令
            _state.value = State.STARTING
            appendLog("▶ 构建命令行...")
            appendLog("  主类: ${config.mainClass}")
            appendLog("  classpath 长度: ${config.classpath.length} chars")

            val javaCmd = mutableListOf<String>()
            javaCmd += javaBin.absolutePath
            javaCmd += config.jvmArgs
            javaCmd += "-cp"; javaCmd += config.classpath
            javaCmd += config.mainClass
            javaCmd += config.gameArgs

            appendLog("  参数: ${javaCmd.size} 个")
            appendLog("")

            // 5. 启动进程
            appendLog("🚀 启动 Java 进程...")
            val builder = ProcessBuilder(javaCmd)
            builder.directory(MCApplication.mcDir())
            builder.redirectErrorStream(true)
            builder.environment().apply {
                put("HOME", MCApplication.gameDir.absolutePath)
                put("LD_LIBRARY_PATH", nativeDir + ":" + File(nativeDir).parentFile?.absolutePath)
                put("LIBGL_ALWAYS_SOFTWARE", "0")
                put("MESA_LOADER_DRIVER_OVERRIDE", "virpipe")
                put("ANDROID_DATA", "/data")
                put("EXTERNAL_STORAGE", "/sdcard")
                put("JAVA_HOME", JreManager.jreDir(jreVer).absolutePath)
                put("PATH", "${JreManager.jreDir(jreVer).absolutePath}/bin:/system/bin")
            }

            process = builder.start()

            readerThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process!!.inputStream)).useLines { lines ->
                        lines.forEach { line -> appendLog(line) }
                    }
                } catch (_: Exception) {}
            }
            readerThread?.start()

            // 等待 Java 启动
            Thread.sleep(2000)
            if (process?.isAlive == true) {
                _state.value = State.RUNNING
                appendLog("✅ 游戏进程已启动 (PID ${process?.pid()})")
                appendLog("")
                appendLog("════════════════════════════════")
                appendLog("  游戏正在运行...")
                appendLog("  返回键可停止")
                appendLog("════════════════════════════════")
            } else {
                val exit = process?.waitFor()
                appendLog("❌ Java 进程退出码: $exit")
                appendLog("   (查看上方日志找原因)")
                _state.value = State.CRASHED
            }
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
            process?.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            process?.destroyForcibly()
            process = null
            readerThread?.interrupt()
            _state.value = State.STOPPED
            appendLog("⏹ 游戏进程已停止")
        } catch (_: Exception) {}
    }

    fun isRunning() = _state.value == State.RUNNING
}
