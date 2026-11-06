package com.mclauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mclauncher.ui.theme.*

@Composable
fun KeyboardScreen(onOpenEditor: () -> Unit) {
    val presets = remember {
        listOf(
            "Pojav 默认布局" to "12 个按键",
            "Mobile 精简版" to "8 个按键",
            "DS4 手柄风格" to "摇杆 + 10 键",
            "Xbox 手柄风格" to "摇杆 + 10 键",
            "个人自定义" to "打开编辑器",
        )
    }
    var selected by remember { mutableStateOf("Pojav 默认布局") }

    Column(modifier = Modifier.fillMaxSize().background(MC_BLACK).padding(24.dp)) {
        Text(
            "⌨️ 按键布局",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // 启用开关
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TouchApp, null, tint = MC_GREEN, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("启用虚拟按键", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("游戏内显示触屏按键", color = MC_TEXT_SECONDARY, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                var enabled by remember { mutableStateOf(true) }
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
        }

        // 预设列表
        Text("选择布局预设", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (name, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected == name) Color(0xFF2A2A2A) else Color(0xFF1E1E1E),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selected = name
                            if (name == "个人自定义") onOpenEditor()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected == name,
                        onClick = { selected = name },
                        colors = RadioButtonDefaults.colors(selectedColor = MC_GREEN)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(desc, color = MC_TEXT_SECONDARY, fontSize = 12.sp)
                    }
                    if (name == "个人自定义") {
                        Text("编辑", color = ACCENT_BLUE, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 底部按钮
        Button(
            onClick = onOpenEditor,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MC_GREEN),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🎨 打开布局编辑器", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
