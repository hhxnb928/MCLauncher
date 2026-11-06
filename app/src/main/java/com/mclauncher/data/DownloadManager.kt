package com.mclauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DownloadManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks
    
    private val taskMap = ConcurrentHashMap<String, DownloadTask>()
    
    // 添加下载任务
    suspend fun download(
        url: String,
        outputPath: String,
        name: String,
        description: String = "",
        onProgress: ((Float, Long, Long) -> Unit)? = null
    ): DownloadTask = withContext(Dispatchers.IO) {
        val task = DownloadTask(
            id = System.currentTimeMillis().toString(),
            name = name,
            url = url,
            outputPath = outputPath,
            description = description,
            status = DownloadTask.Status.RUNNING
        )
        taskMap[task.id] = task
        refresh()
        
        try {
            val file = File(outputPath)
            file.parentFile?.mkdirs()
            
            // 支持断点续传
            var downloaded = if (file.exists()) file.length() else 0L
            val requestBuilder = Request.Builder().url(url)
            if (downloaded > 0) requestBuilder.header("Range", "bytes=$downloaded-")
            
            client.newCall(requestBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful && !resp.isRedirect) {
                    task.status = DownloadTask.Status.ERROR
                    refresh()
                    throw Exception("下载失败: ${resp.code}")
                }
                
                // 获取总大小
                val contentLength = resp.header("Content-Length")?.toLongOrNull()
                    ?: resp.header("Content-Range")?.let { 
                        val parts = it.split("/")
                        if (parts.size == 2) parts[1].toLongOrNull() else null
                    } ?: 0L
                
                task.totalBytes = contentLength + downloaded
                task.downloadedBytes = downloaded
                
                resp.body?.byteStream()?.use { input ->
                    FileOutputStream(file, downloaded > 0).use { output ->
                        val buffer = ByteArray(8192)
                        var lastTime = System.currentTimeMillis()
                        var lastDownloaded = downloaded
                        
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            
                            // 更新进度（节流 200ms）
                            val now = System.currentTimeMillis()
                            if (now - lastTime > 200) {
                                task.downloadedBytes = downloaded
                                task.progress = if (task.totalBytes > 0) downloaded.toFloat() / task.totalBytes else 0f
                                task.speed = (downloaded - lastDownloaded) * 1000f / (now - lastTime)
                                lastTime = now
                                lastDownloaded = downloaded
                                onProgress?.invoke(task.progress, downloaded, task.totalBytes)
                                refresh()
                            }
                        }
                    }
                }
                
                task.progress = 1f
                task.downloadedBytes = file.length()
                task.status = DownloadTask.Status.COMPLETED
                refresh()
            }
        } catch (e: Exception) {
            task.status = DownloadTask.Status.ERROR
            task.description = e.message ?: "未知错误"
            refresh()
            throw e
        }
        
        task
    }
    
    private fun refresh() {
        _tasks.value = taskMap.values.toList().sortedByDescending { 
            when (it.status) {
                DownloadTask.Status.RUNNING -> 0
                DownloadTask.Status.PENDING -> 1
                else -> 2
            }
        }
    }
    
    fun removeTask(id: String) {
        taskMap.remove(id)
        refresh()
    }
    
    fun clearCompleted() {
        taskMap.entries.removeIf { it.value.status == DownloadTask.Status.COMPLETED }
        refresh()
    }
}
