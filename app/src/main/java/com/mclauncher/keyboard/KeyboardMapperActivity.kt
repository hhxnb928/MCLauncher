package com.mclauncher.keyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mclauncher.ui.theme.MCLauncherTheme
import kotlin.math.roundToInt

class KeyboardMapperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MCLauncherTheme {
                KeyboardEditor()
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun KeyboardEditor() {
        var config by remember { mutableStateOf(KeyboardConfig.default()) }
        var selectedKey by remember { mutableStateOf<KeyMapping?>(null) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🎨 按键布局编辑器") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFF121212)
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // 编辑区域（灰色背景模拟游戏画面）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color(0xFF2A2A2A))
                        .onSizeChanged { }
                ) {
                    var sizeX by remember { mutableStateOf(0) }
                    var sizeY by remember { mutableStateOf(0) }
                    
                    Box(modifier = Modifier.fillMaxSize().onSizeChanged {
                        sizeX = it.width; sizeY = it.height
                    }) {
                        config.buttons.forEach { mapping ->
                            DraggableKey(
                                mapping = mapping,
                                isSelected = selectedKey == mapping,
                                containerSizeX = sizeX,
                                containerSizeY = sizeY,
                                onClick = { selectedKey = mapping },
                                onMoved = { dx, dy ->
                                    config = config.copy(
                                        buttons = config.buttons.map { 
                                            if (it.keyCode == mapping.keyCode) {
                                                it.copy(pos = mapping.pos.first + dx to mapping.pos.second + dy)
                                            } else it
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
                
                // 底部操作栏
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { config = KeyboardConfig.default() },
                        modifier = Modifier.weight(1f)
                    ) { Text("重置") }
                    Button(
                        onClick = { finish() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DBD2D))
                    ) { Text("保存") }
                }
            }
        }
    }
    
    @Composable
    fun DraggableKey(
        mapping: KeyMapping,
        isSelected: Boolean,
        containerSizeX: Int,
        containerSizeY: Int,
        onClick: () -> Unit,
        onMoved: (Float, Float) -> Unit
    ) {
        var offset by remember { mutableStateOf(IntOffset.Zero) }
        
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (mapping.pos.first * containerSizeX).roundToInt() - mapping.size / 2 + offset.x,
                        (mapping.pos.second * containerSizeY).roundToInt() - mapping.size / 2 + offset.y
                    )
                }
                .size(mapping.size.dp)
                .background(
                    if (isSelected) Color(0xFFFFD700)
                    else Color(0x804DBD2D),
                    RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offset = IntOffset(offset.x + dragAmount.x.roundToInt(), offset.y + dragAmount.y.roundToInt())
                            onMoved(dragAmount.x / containerSizeX, dragAmount.y / containerSizeY)
                        }
                    )
                }
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(mapping.label, color = Color.White)
        }
    }
}
