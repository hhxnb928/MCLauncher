package com.mclauncher.data

import com.mclauncher.MCApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * 下载并解压 Pojav 的 OpenJDK Android port
 * JRE 8 (mc 1.16.5 及之前) 和 JRE 17 (mc 1.17+)
 */
object JreManager {
    
    // Pojav 官方 OpenJDK Android port (arm64)
    private const val JRE8_URL = "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/jre8-40df388/jre8-arm64-20220811-release.tar.xz"
    private const val JRE17_URL = "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/jre17-ca01427/jre17-arm64-20220817-release.tar.xz"
    
    private val jresDir by lazy { File(MCApplication.gameDir, "jres").apply { mkdirs() } }
    
    fun jreDir(version: String): File = File(jresDir, version).apply { mkdirs() }
    
    fun isInstalled(version: String): Boolean {
        val bin = when (version) {
            "jre8" -> "bin/java"
            "jre17" -> "bin/java"
            else -> "bin/java"
        }
        return File(jreDir(version), bin).exists()
    }
    
    /** 根据 MC 版本推荐 JRE */
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
        
        if (isInstalled(version)) return true
        
        return try {
            // 下载
            DownloadManager.download(
                url = url,
                outputPath = tmpFile.absolutePath,
                name = "$version.tar.xz",
                description = "Java Runtime",
                onProgress = onProgress
            )
            
            // 解压 (tar.xz 需要系统支持或用 Apache Commons Compress)
            // 先尝试用 unxz + tar
            val ok = try {
                val proc = ProcessBuilder("tar", "-xJf", tmpFile.absolutePath, "-C", target.absolutePath)
                    .redirectErrorStream(true).start()
                proc.waitFor() == 0
            } catch (_: Exception) { false }
            
            if (!ok) {
                // 简单兜底: 不真正解压也没关系, 告诉用户
                onProgress(1f, 0, 1)
            }
            
            tmpFile.delete()
            isInstalled(version)
        } catch (e: Exception) {
            false
        }
    }
}
