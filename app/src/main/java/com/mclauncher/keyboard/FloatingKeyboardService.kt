package com.mclauncher.keyboard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlin.math.roundToInt

/**
 * 通过无障碍服务注入手势事件（模拟键鼠）
 * 需要用户手动在系统设置里开启
 */
class FloatingKeyboardService : AccessibilityService() {
    
    companion object {
        const val ACTION_INJECT_KEY = "com.mclauncher.INJECT_KEY"
        const val ACTION_INJECT_TOUCH = "com.mclauncher.INJECT_TOUCH"
        const val KEY_CODE = "key_code"
        const val KEY_ACTION = "key_action" // 0=down, 1=up
        const val TOUCH_X = "touch_x"
        const val TOUCH_Y = "touch_y"
        const val TOUCH_ACTION = "touch_action"
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* no-op */ }
    override fun onInterrupt() { /* no-op */ }
    
    // 注入触控事件
    fun injectTouch(x: Float, y: Float, action: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 100L)
            val builder = GestureDescription.Builder().addStroke(stroke)
            dispatchGesture(builder.build(), null, null)
        }
    }
    
    // 模拟按住
    fun injectTouchDown(x: Float, y: Float) = injectTouch(x, y, 0)
    fun injectTouchMove(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { lineTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 200L)
            dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        }
    }
    fun injectTouchUp(x: Float, y: Float) = injectTouch(x, y, 1)
    
    // 滑动
    fun injectSwipe(fromX: Float, fromY: Float, toX: Float, toY: Float, duration: Long = 200) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(fromX, fromY)
                lineTo(toX, toY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
            dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        }
    }
    
    // 点击
    fun injectTap(x: Float, y: Float) = injectTouch(x, y, 0)
}
