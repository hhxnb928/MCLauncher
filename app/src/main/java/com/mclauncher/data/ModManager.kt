package com.mclauncher.data

import com.mclauncher.MCApplication
import java.io.File

/**
 * 模组管理器: 扫描 mods 目录, 启用/禁用/卸载
 * 禁用方式: 重命名 .jar → .disabled
 */
object ModManager {

    private val modsDir: File get() = MCApplication.modsDir()

    data class ModFile(
        val file: File,
        val name: String,           // 文件名
        val enabled: Boolean,       // true = .jar, false = .disabled
        val sizeBytes: Long,
        val lastModified: Long
    )

    fun scan(): List<ModFile> {
        modsDir.mkdirs()
        val out = mutableListOf<ModFile>()
        modsDir.listFiles()?.forEach { f ->
            if (f.isFile && (f.name.endsWith(".jar") || f.name.endsWith(".disabled"))) {
                out += ModFile(
                    file = f,
                    name = f.name,
                    enabled = f.name.endsWith(".jar"),
                    sizeBytes = f.length(),
                    lastModified = f.lastModified()
                )
            }
        }
        return out.sortedByDescending { it.lastModified }
    }

    fun toggle(mod: ModFile): Boolean {
        val newName = if (mod.enabled) {
            mod.name.removeSuffix(".jar") + ".disabled"
        } else {
            mod.name.removeSuffix(".disabled") + ".jar"
        }
        val target = File(mod.file.parentFile, newName)
        return if (!target.exists()) mod.file.renameTo(target) else false
    }

    fun remove(mod: ModFile): Boolean = mod.file.delete()

    fun enabledCount() = scan().count { it.enabled }
}
