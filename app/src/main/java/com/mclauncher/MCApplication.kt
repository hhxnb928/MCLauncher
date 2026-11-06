package com.mclauncher

import android.app.Application
import android.os.Environment
import java.io.File

class MCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        // 游戏数据目录：/sdcard/MCLauncher/.minecraft/
        gameDir = File(Environment.getExternalStorageDirectory(), "MCLauncher").apply {
            if (!exists()) mkdirs()
        }
    }
    
    companion object {
        lateinit var instance: MCApplication
        lateinit var gameDir: File
        
        fun mcDir(): File = File(gameDir, ".minecraft").apply { if (!exists()) mkdirs() }
        fun versionsDir(): File = File(mcDir(), "versions").apply { if (!exists()) mkdirs() }
        fun modsDir(): File = File(mcDir(), "mods").apply { if (!exists()) mkdirs() }
        fun resourcePacksDir(): File = File(mcDir(), "resourcepacks").apply { if (!exists()) mkdirs() }
        fun shaderPacksDir(): File = File(mcDir(), "shaderpacks").apply { if (!exists()) mkdirs() }
        fun librariesDir(): File = File(mcDir(), "libraries").apply { if (!exists()) mkdirs() }
        fun assetsDir(): File = File(mcDir(), "assets").apply { if (!exists()) mkdirs() }
        fun downloadsDir(): File = File(gameDir, "downloads").apply { if (!exists()) mkdirs() }
        fun nativesDir(): File = File(gameDir, "natives").apply { if (!exists()) mkdirs() }
    }
}
