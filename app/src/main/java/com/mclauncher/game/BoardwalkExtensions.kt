package com.mclauncher.game

import android.view.KeyEvent
import android.util.Log

/**
 * 发送按键事件到游戏进程
 * （通过 Boardwalk 的输入通道）
 */
fun BoardwalkRunner.sendKeyEvent(keyCode: Int, action: Int) {
    try {
        // Boardwalk 通过 JNI 暴露按键接口
        // 这里用反射调 org.lwjgl.glfw.GLFW 的事件注入
        val keyName = when (keyCode) {
            KeyEvent.KEYCODE_A -> "A"
            KeyEvent.KEYCODE_W -> "W"
            KeyEvent.KEYCODE_S -> "S"
            KeyEvent.KEYCODE_D -> "D"
            KeyEvent.KEYCODE_SPACE -> "SPACE"
            KeyEvent.KEYCODE_SHIFT_LEFT -> "LSHIFT"
            KeyEvent.KEYCODE_CTRL_LEFT -> "LCTRL"
            KeyEvent.KEYCODE_ESCAPE -> "ESCAPE"
            KeyEvent.KEYCODE_TAB -> "TAB"
            KeyEvent.KEYCODE_E -> "E"
            KeyEvent.KEYCODE_F -> "F"
            KeyEvent.KEYCODE_Q -> "Q"
            KeyEvent.KEYCODE_1 -> "1"
            KeyEvent.KEYCODE_2 -> "2"
            KeyEvent.KEYCODE_3 -> "3"
            KeyEvent.KEYCODE_4 -> "4"
            KeyEvent.KEYCODE_5 -> "5"
            KeyEvent.KEYCODE_6 -> "6"
            KeyEvent.KEYCODE_7 -> "7"
            KeyEvent.KEYCODE_8 -> "8"
            KeyEvent.KEYCODE_9 -> "9"
            KeyEvent.KEYCODE_0 -> "0"
            KeyEvent.KEYCODE_DPAD_UP -> "UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
            KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "RIGHT"
            else -> keyCode.toString()
        }
        Log.d("Boardwalk", "sendKey: $keyName action=$action")
        // 实际实现需 JNI 调用 boardwalk_native_send_key
    } catch (e: Exception) {
        Log.e("Boardwalk", "sendKey 失败", e)
    }
}
