package com.mclauncher.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 单个按键按钮
 */
@Composable
fun KeyButton(
    text: String,
    keyCode: Int,
    modifier: Modifier = Modifier,
    size: Int = 60,
    onKeyDown: (Int) -> Unit,
    onKeyUp: (Int) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (pressed) Color(0x804DBD2D)
                else Color(0x80282828)
            )
            .pointerInput(keyCode) {
                detectDragGestures(
                    onDragStart = { pressed = true; onKeyDown(keyCode) },
                    onDragEnd = { pressed = false; onKeyUp(keyCode) },
                    onDragCancel = { pressed = false; onKeyUp(keyCode) }
                ) { _, _ -> }
            }
            .clickable { /* 已通过 drag 处理 */ },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = (size / 3).sp
        )
    }
}

/**
 * 虚拟摇杆（左手移动）
 */
@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Int = 140,
    onMove: (dx: Float, dy: Float) -> Unit
) {
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0x40282828))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true; offset = IntOffset(0, 0) },
                    onDragEnd = { dragging = false; offset = IntOffset.Zero; onMove(0f, 0f) },
                    onDragCancel = { dragging = false; offset = IntOffset.Zero },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val max = size / 2f
                        val nx = (offset.x + dragAmount.x).coerceIn(-max, max)
                        val ny = (offset.y + dragAmount.y).coerceIn(-max, max)
                        offset = IntOffset(nx.roundToInt(), ny.roundToInt())
                        onMove(nx.toFloat() / max, ny.toFloat() / max)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { offset }
                .size((size * 0.4).dp)
                .clip(CircleShape)
                .background(Color(0x804DBD2D))
        )
    }
}

/**
 * 触屏按键总悬浮层
 */
@Composable
fun TouchOverlay(
    onKeyEvent: (Int, Int) -> Unit,
    config: KeyboardConfig = KeyboardConfig.default()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 左摇杆（移动）
        VirtualJoystick(
            modifier = Modifier.align(Alignment.BottomStart).padding(32.dp),
            onMove = { dx, dy ->
                // 映射到 W A S D 键值
                // dx 正值 -> D, 负值 -> A
                // dy 正值 -> S, 负值 -> W
            }
        )
        
        // 右摇杆（视角旋转）
        VirtualJoystick(
            modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp),
            size = 100,
            onMove = { _, _ -> }
        )
        
        // 跳跃键（空格）
        KeyButton(
            text = "跳",
            keyCode = android.view.KeyEvent.KEYCODE_SPACE,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            onKeyDown = { onKeyEvent(it, 0) },
            onKeyUp = { onKeyEvent(it, 1) }
        )
        
        // 攻击/破坏（鼠标左键）
        KeyButton(
            text = "打",
            keyCode = -100, // 自定义：左键
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            onKeyDown = { onKeyEvent(it, 0) },
            onKeyUp = { onKeyEvent(it, 1) }
        )
        
        // 使用/交互（鼠标右键）
        KeyButton(
            text = "用",
            keyCode = -101, // 自定义：右键
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 86.dp),
            size = 50,
            onKeyDown = { onKeyEvent(it, 0) },
            onKeyUp = { onKeyEvent(it, 1) }
        )
        
        // 物品栏 1-9
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..9).forEach { num ->
                KeyButton(
                    text = num.toString(),
                    keyCode = android.view.KeyEvent.KEYCODE_0 + num,
                    size = 44,
                    onKeyDown = { onKeyEvent(it, 0) },
                    onKeyUp = { onKeyEvent(it, 1) }
                )
            }
        }
        
        // Esc
        KeyButton(
            text = "⨯",
            keyCode = android.view.KeyEvent.KEYCODE_ESCAPE,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            size = 50,
            onKeyDown = { onKeyEvent(it, 0) },
            onKeyUp = { onKeyEvent(it, 1) }
        )
        
        // 潜行（Shift）
        KeyButton(
            text = "Shift",
            keyCode = android.view.KeyEvent.KEYCODE_SHIFT_LEFT,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 160.dp, bottom = 20.dp),
            size = 55,
            onKeyDown = { onKeyEvent(it, 0) },
            onKeyUp = { onKeyEvent(it, 1) }
        )
    }
}

/**
 * 按键布局配置（可持久化）
 */
data class KeyboardConfig(
    val joystickLeftEnabled: Boolean = true,
    val joystickRightEnabled: Boolean = true,
    val showInventoryBar: Boolean = true,
    val jumpButtonPos: Pair<Float, Float> = 0.5f to 0.85f,
    val attackButtonPos: Pair<Float, Float> = 0.9f to 0.5f,
    val buttons: List<KeyMapping> = defaultKeyMappings()
) {
    companion object {
        fun default() = KeyboardConfig()
        fun defaultKeyMappings() = listOf(
            KeyMapping("W", android.view.KeyEvent.KEYCODE_W, pos = 0.1f to 0.7f),
            KeyMapping("A", android.view.KeyEvent.KEYCODE_A, pos = 0.05f to 0.75f),
            KeyMapping("S", android.view.KeyEvent.KEYCODE_S, pos = 0.15f to 0.75f),
            KeyMapping("D", android.view.KeyEvent.KEYCODE_D, pos = 0.2f to 0.7f),
            KeyMapping("E", android.view.KeyEvent.KEYCODE_E, pos = 0.8f to 0.6f),  // 打开背包
            KeyMapping("Q", android.view.KeyEvent.KEYCODE_Q, pos = 0.85f to 0.55f), // 丢弃
            KeyMapping("F", android.view.KeyEvent.KEYCODE_F, pos = 0.82f to 0.5f),  // 切换视角
            KeyMapping("空格", android.view.KeyEvent.KEYCODE_SPACE, pos = 0.5f to 0.85f),
            KeyMapping("Shift", android.view.KeyEvent.KEYCODE_SHIFT_LEFT, pos = 0.12f to 0.82f),
            KeyMapping("Ctrl", android.view.KeyEvent.KEYCODE_CTRL_LEFT, pos = 0.05f to 0.85f), // 疾跑
            KeyMapping("Esc", android.view.KeyEvent.KEYCODE_ESCAPE, pos = 0.95f to 0.05f),
            KeyMapping("Tab", android.view.KeyEvent.KEYCODE_TAB, pos = 0.9f to 0.05f),   // 物品搜索
        )
    }
}

data class KeyMapping(
    val label: String,
    val keyCode: Int,
    val pos: Pair<Float, Float>, // 相对坐标 0~1
    val size: Int = 56,
    val enabled: Boolean = true
)
