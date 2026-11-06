package com.mclauncher.data

import com.mclauncher.MCApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class LibraryDownloader(
    private val versionJson: String,
    private val scope: CoroutineScope
) {
    fun downloadAll(onProgress: (String, Float) -> Unit, onDone: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject(versionJson)
                val libraries = json.optJSONArray("libraries") ?: return@launch
                val total = libraries.length()
                var done = 0
                
                for (i in 0 until total) {
                    val lib = libraries.getJSONObject(i)
                    val downloads = lib.optJSONObject("downloads") ?: continue
                    val artifact = downloads.optJSONObject("artifact") ?: continue
                    val url = artifact.getString("url")
                    val path = artifact.getString("path")
                    
                    val outFile = File(MCApplication.librariesDir(), path)
                    if (outFile.exists() && outFile.length() > 0) {
                        done++
                        onProgress("跳过已有: ${path.substringAfterLast("/")}", done.toFloat() / total)
                        continue
                    }
                    
                    try {
                        DownloadManager.download(
                            url = url,
                            outputPath = outFile.absolutePath,
                            name = path.substringAfterLast("/"),
                            description = "下载库 ${i+1}/$total"
                        )
                        done++
                        onProgress("下载 ${i+1}/$total: ${path.substringAfterLast("/")}", done.toFloat() / total)
                    } catch (e: Exception) {
                        // 跳过失败的库继续
                        done++
                        onProgress("失败: ${e.message}", done.toFloat() / total)
                    }
                }
                onDone()
            } catch (e: Exception) {
                onProgress("错误: ${e.message}", 0f)
            }
        }
    }
}
