package com.mclauncher.data

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class DownloadService : Service() {
    private val TAG = "DownloadService"
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DownloadService started (stub)")
        return START_NOT_STICKY
    }
}
