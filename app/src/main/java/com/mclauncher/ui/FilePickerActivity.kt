package com.mclauncher.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log

class FilePickerActivity : Activity() {
    private val TAG = "FilePickerActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "FilePickerActivity called (stub)")
        finish()
    }
}
