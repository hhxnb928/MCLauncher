package com.mclauncher.game

import android.content.Context
import com.mclauncher.MCApplication
import com.mclauncher.data.LocalVersion
import com.mclauncher.data.LoaderType
import org.json.JSONObject
import java.io.File
import java.util.UUID

class GameSession {
    
    // ========== 启动参数构建 ==========
    fun buildLaunchArgs(
        context: Context,
        version: LocalVersion,
        memoryMB: Int = 4096,
        customJvmArgs: List<String> = emptyList()
    ): LaunchConfig {
        val versionsDir = MCApplication.versionsDir()
        val versionDir = File(versionsDir, version.id)
        val versionJsonFile = File(versionDir, "${version.id}.json")
        if (!versionJsonFile.exists()) {
            throw IllegalStateException("版本 JSON 不存在: ${version.id}")
        }
        val json = JSONObject(versionJsonFile.readText())
        
        // ========== 主类名 ==========
        val mainClass = when (version.loader) {
            LoaderType.VANILLA -> json.optString("mainClass", "net.minecraft.client.main.Main")
            LoaderType.FABRIC -> "net.fabricmc.loader.impl.launch.knot.KnotClient"
            LoaderType.FORGE, LoaderType.NEOFORGE -> json.optString("mainClass", "cpw.mods.modlauncher.Launcher")
            LoaderType.QUILT -> "org.quiltmc.loader.impl.launch.knot.KnotClient"
        }
        
        // ========== JVM 参数 ==========
        val jvmArgs = mutableListOf<String>()
        
        // 内存
        jvmArgs += "-Xms${memoryMB}m"
        jvmArgs += "-Xmx${memoryMB}m"
        
        // Boardwalk 转译必需参数
        jvmArgs += "-XX:+UseZGC"
        jvmArgs += "-XX:+ZGenerational"
        jvmArgs += "-XX:+UseCompressedOops"
        jvmArgs += "-XX:MaxRAM=${memoryMB}m"
        
        // LWJGL 相关系统属性
        val nativesDir = MCApplication.nativesDir()
        jvmArgs += "-Djava.library.path=${nativesDir.absolutePath}"
        jvmArgs += "-Dorg.lwjgl.librarypath=${nativesDir.absolutePath}"
        
        // Minecraft 必需的系统属性
        jvmArgs += "-Dminecraft.launcher.brand=MCLauncher"
        jvmArgs += "-Dminecraft.launcher.version=1.0.0"
        jvmArgs += "-Dminecraft.client.jar=${File(versionDir, "${version.id}.jar").absolutePath}"
        jvmArgs += "-Djava.net.preferIPv4Stack=true"
        
        // 自定义参数
        jvmArgs += customJvmArgs
        
        // ========== 类路径 ==========
        val classpath = buildClasspath(json, version)
        
        // ========== 游戏参数 ==========
        val gameArgs = buildGameArgs(json, version)
        
        return LaunchConfig(
            versionId = version.id,
            mainClass = mainClass,
            jvmArgs = jvmArgs,
            classpath = classpath,
            gameArgs = gameArgs
        )
    }
    
    private fun buildClasspath(versionJson: JSONObject, version: LocalVersion): String {
        val paths = mutableListOf<String>()
        val versionsDir = MCApplication.versionsDir()
        val librariesDir = MCApplication.librariesDir()
        
        // MC 主 jar
        val versionJar = File(File(versionsDir, version.id), "${version.id}.jar")
        if (versionJar.exists()) paths += versionJar.absolutePath
        
        // 依赖库
        val libraries = versionJson.optJSONArray("libraries")
        if (libraries != null) {
            for (i in 0 until libraries.length()) {
                val lib = libraries.getJSONObject(i)
                
                // 跳过被排除的库
                val rules = lib.optJSONArray("rules")
                if (rules != null) {
                    var allowed = true
                    for (j in 0 until rules.length()) {
                        val rule = rules.getJSONObject(j)
                        val action = rule.optString("action", "allow")
                        val os = rule.optJSONObject("os")
                        val name = os?.optString("name")
                        if (action == "disallow" && (name == null || name == "linux")) {
                            allowed = false
                        }
                    }
                    if (!allowed) continue
                }
                
                val artifact = lib.optJSONObject("downloads")
                    ?.optJSONObject("artifact") ?: continue
                val path = artifact.optString("path") ?: continue
                val file = File(librariesDir, path)
                if (file.exists()) paths += file.absolutePath
            }
        }
        
        // 模组加载器 jar
        when (version.loader) {
            LoaderType.FABRIC -> {
                val fabricJar = File(librariesDir, "net/fabricmc/fabric-loader/${version.loaderVersion}/fabric-loader-${version.loaderVersion}.jar")
                if (fabricJar.exists()) paths += fabricJar.absolutePath
            }
            LoaderType.FORGE, LoaderType.NEOFORGE -> {
                // Forge jar 在版本目录下
                val forgeJar = File(File(versionsDir, version.id), "forge-${version.loaderVersion}.jar")
                if (forgeJar.exists()) paths += forgeJar.absolutePath
            }
            else -> {}
        }
        
        return paths.joinToString(File.pathSeparator)
    }
    
    private fun buildGameArgs(versionJson: JSONObject, version: LocalVersion): List<String> {
        val args = mutableListOf<String>()
        val mcDir = MCApplication.mcDir().absolutePath
        
        // 基础参数
        args += "--username"; args += "Player"
        args += "--version"; args += version.id
        args += "--gameDir"; args += mcDir
        args += "--assetsDir"; args += MCApplication.assetsDir().absolutePath
        args += "--assetIndex"; args += version.id
        args += "--accessToken"; args += ""
        args += "--uuid"; args += UUID.randomUUID().toString()
        args += "--userType"; args += "mojang"
        
        // 窗口设置
        args += "--width"; args += "1280"
        args += "--height"; args += "720"
        
        return args
    }
}

data class LaunchConfig(
    val versionId: String,
    val mainClass: String,
    val jvmArgs: List<String>,
    val classpath: String,
    val gameArgs: List<String>
)
