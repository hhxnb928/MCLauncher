package com.mclauncher.data

import com.mclauncher.MCApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Pojav OpenJDK Android port 下载+解压
 * JRE 8 (mc<1.17) 和 JRE 17 (mc>=1.17)
 */
object JreManager {
    
    // 固定 URL — Pojav 官方 release
    private const val JRE8_URL = "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/jre8-40df388/jre8-arm64-20220811-release.tar.xz"
    private const val JRE17_URL = "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/jre17-ca01427/jre17-arm64-20220817-release.tar.xz"
    
    private val jresDir by lazy { File(MCApplication.gameDir, "jres").apply { mkdirs() } }
    
    fun jreDir(version: String): File = File(jresDir, version).apply { mkdirs() }
    
    /** JRE 可执行文件路径 */
    fun javaExecutable(version: String): File {
        val jre = jreDir(version)
        // 尝试各种可能的位置
        val candidates = listOf(
            "bin/java",      // 标准位置
            "java",          // 根目录
            "bin/art",       // ART 模式
            "bin/openjdk",
            "bin/javaw"
        )
        for (c in candidates) {
            val f = File(jre, c)
            if (f.exists()) return f
        }
        return File(jre, "bin/java") // 返回默认路径供检查
    }
    
    fun isInstalled(version: String): Boolean {
        return javaExecutable(version).exists() || File(jreDir(version), "bin").exists()
    }
    
    fun recommendJre(mcVersion: String): String {
        val parts = mcVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val major = parts.getOrNull(1) ?: 0
        return if (major >= 17) "jre17" else "jre8"
    }
    
    suspend fun download(version: String, onProgress: (Float, Long, Long) -> Unit): Boolean {
        val url = when (version) {
            "jre8" -> JRE8_URL
            "jre17" -> JRE17_URL
            else -> return false
        }
        val target = jreDir(version)
        val tmpFile = File(jresDir, "$version.tar.xz")
        
        if (isInstalled(version)) { onProgress(1f, 0, 1); return true }
        
        return try {
            onProgress(0f, 0, 1)
            
            // 下载
            DownloadManager.download(
                url = url,
                outputPath = tmpFile.absolutePath,
                name = "$version.tar.xz",
                description = "Java Runtime $version",
                onProgress = onProgress
            )
            
            // 解压 tar.xz
            onProgress(0.9f, 0, 1)
            extractTarXz(tmpFile, target)
            tmpFile.delete()
            
            val ok = isInstalled(version)
            if (ok) {
                // 确保可执行
                javaExecutable(version).setExecutable(true)
            }
            onProgress(1f, 0, 1)
            ok
        } catch (e: Exception) {
            false
        }
    }
    
    private fun extractTarXz(tarXz: File, targetDir: File) {
        // 尝试用系统命令 tar -xJf
        try {
            val proc = ProcessBuilder("tar", "-xJf", tarXz.absolutePath, "-C", targetDir.absolutePath)
                .redirectErrorStream(true).start()
            proc.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) {
            // 备选: 尝试 unxz
            try {
                val xzFile = File(tarXz.parentFile, tarXz.nameWithoutExtension)
                val proc1 = ProcessBuilder("unxz", "-c", tarXz.absolutePath)
                    .redirectErrorStream(true).redirectOutput(xzFile).start()
                proc1.waitFor()
                val proc2 = ProcessBuilder("tar", "-xf", xzFile.absolutePath, "-C", targetDir.absolutePath)
                    .redirectErrorStream(true).start()
                proc2.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)
                xzFile.delete()
            } catch (_: Exception) {
                // 没有系统 xz/tar, 手动解压 (简单提示)
                File(targetDir, "README.txt").writeText(
                    "请手动解压 $version.tar.xz 到本目录\n" +
                    "需要: tar -xJf jre$version.tar.xz -C .\n"
                )
            }
        }
    }
}
