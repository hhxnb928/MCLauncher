package com.mclauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
fun SkinManagerScreen() {
    val skins = remember {
        listOf("Steve", "Alex", "Him", "Dream", "Technoblade", "自制")
    }
    var selected by remember { mutableStateOf("Steve") }

    Column(modifier = Modifier.fillMaxSize().background(MC_BLACK).padding(24.dp)) {
        Text(
            "👤 皮肤管理",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // 当前皮肤预览（大）
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑", fontSize = 72.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(selected, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("当前皮肤", color = MC_TEXT_SECONDARY, fontSize = 13.sp)
            }
        }

        // 皮肤选择
        Text("选择皮肤", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(skins) { skin ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                if (selected == skin) MC_GREEN.copy(alpha = 0.3f)
                                else Color(0xFF1E1E1E),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selected = skin },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected == skin) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MC_GREEN,
                                modifier = Modifier.size(24.dp).align(Alignment.TopEnd).padding(4.dp)
                            )
                        }
                        Text("🧑", fontSize = 40.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(skin, color = MC_TEXT_SECONDARY, fontSize = 12.sp)
                }
            }

            // 添加按钮
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = MC_TEXT_SECONDARY, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("添加", color = MC_TEXT_SECONDARY, fontSize = 12.sp)
                }
            }
        }
    }
}
