package com.mclauncher.game

import android.content.Context
import com.mclauncher.MCApplication
import com.mclauncher.data.AccountManager
import com.mclauncher.data.LocalVersion
import com.mclauncher.data.LoaderType
import org.json.JSONObject
import java.io.File
import java.util.UUID

class GameSession {

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
            throw IllegalStateException("版本 ${version.id} 未安装! 请先在[版本管理]点击安装")
        }
        val json = JSONObject(versionJsonFile.readText())

        val mainClass = when (version.loader) {
            LoaderType.VANILLA -> json.optString("mainClass", "net.minecraft.client.main.Main")
            LoaderType.FABRIC -> "net.fabricmc.loader.impl.launch.knot.KnotClient"
            LoaderType.FORGE, LoaderType.NEOFORGE -> json.optString("mainClass", "cpw.mods.modlauncher.Launcher")
            LoaderType.QUILT -> "org.quiltmc.loader.impl.launch.knot.KnotClient"
        }

        val jvmArgs = mutableListOf<String>()
        jvmArgs += "-Xms${(memoryMB / 4).coerceAtLeast(256)}m"
        jvmArgs += "-Xmx${memoryMB}m"
        jvmArgs += "-XX:+UseG1GC"
        jvmArgs += "-XX:+ParallelRefProcEnabled"
        jvmArgs += "-XX:MaxGCPauseMillis=200"
        jvmArgs += "-XX:+UnlockExperimentalVMOptions"
        jvmArgs += "-XX:+DisableExplicitGC"
        jvmArgs += "-XX:G1NewSizePercent=30"
        jvmArgs += "-XX:G1MaxNewSizePercent=40"
        jvmArgs += "-XX:G1HeapRegionSize=8M"
        jvmArgs += "-XX:G1ReservePercent=20"
        jvmArgs += "-XX:G1HeapWastePercent=5"
        jvmArgs += "-XX:G1MixedGCCountTarget=4"
        jvmArgs += "-XX:InitiatingHeapOccupancyPercent=15"
        jvmArgs += "-XX:G1MixedGCLiveThresholdPercent=90"
        jvmArgs += "-XX:G1RSetUpdatingPauseTimePercent=5"
        jvmArgs += "-XX:SurvivorRatio=32"
        jvmArgs += "-XX:+PerfDisableSharedMem"
        jvmArgs += "-XX:MaxTenuringThreshold=1"

        val nativesDir = context.applicationInfo.nativeLibraryDir
        jvmArgs += "-Djava.library.path=$nativesDir:${MCApplication.nativesDir()}"
        jvmArgs += "-Dorg.lwjgl.librarypath=${MCApplication.nativesDir()}"
        jvmArgs += "-Dorg.lwjgl.opengl.egl.display=$nativesDir/libEGL.so"
        jvmArgs += "-Dorg.lwjgl.opengl.gles2.display=$nativesDir/libGLESv2.so"
        jvmArgs += "-Dminecraft.launcher.brand=MCLauncher"
        jvmArgs += "-Dminecraft.launcher.version=1.0.0-Android"
        jvmArgs += "-Djava.net.preferIPv4Stack=true"
        jvmArgs += "-Dos.name=Linux"
        jvmArgs += "-Dos.arch=aarch64"
        jvmArgs += "-Dfile.encoding=UTF-8"
        jvmArgs += customJvmArgs

        val classpath = buildClasspath(json, version)
        val gameArgs = buildGameArgs(version)

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
                val rules = lib.optJSONArray("rules")
                if (rules != null) {
                    var allowed = true
                    for (j in 0 until rules.length()) {
                        val rule = rules.getJSONObject(j)
                        val action = rule.optString("action", "allow")
                        val os = rule.optJSONObject("os")
                        val name = os?.optString("name")
                        if (action == "disallow" && (name == null || name == "linux")) allowed = false
                        if (action == "allow" && name != null && name != "linux") allowed = false
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

        // Loader jar
        val loaderJar = File(librariesDir, "fabric-loader-*.jar")
        if (loaderJar.exists()) paths += loaderJar.absolutePath

        return paths.joinToString(File.pathSeparator)
    }

    private fun buildGameArgs(version: LocalVersion): List<String> {
        val account = AccountManager.active()
        val name = account?.name ?: "Player"
        val uuid = account?.uuid ?: UUID.randomUUID().toString()
        val token = account?.offlineAccessToken ?: ""
        val mcDir = MCApplication.mcDir().absolutePath

        return listOf(
            "--username", name,
            "--version", version.id,
            "--gameDir", mcDir,
            "--assetsDir", MCApplication.assetsDir().absolutePath,
            "--assetIndex", version.id,
            "--accessToken", token,
            "--uuid", uuid,
            "--userType", "offline",
            "--versionType", "MCLauncher",
            "--width", "1280",
            "--height", "720"
        )
    }
}

data class LaunchConfig(
    val versionId: String,
    val mainClass: String,
    val jvmArgs: List<String>,
    val classpath: String,
    val gameArgs: List<String>
)
