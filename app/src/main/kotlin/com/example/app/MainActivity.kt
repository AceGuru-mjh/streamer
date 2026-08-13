package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui_xiahong.XiaHongConfig
import com.example.ui_xiahong.XiaHongFlow
import com.example.ui_xiahong.XiaHongIntensity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // 半透明 Surface 作为内容底，便于观察流光叠加在文字/卡片上的效果
                XiaHongDemoScreen()
            }
        }
    }
}

/**
 * 演示界面：用 XiaHongFlow 包裹一段普通 UI，验证三层特效叠加。
 * 直接运行此 app 模块即可在设备上看到流动、发光、电弧效果。
 */
@Composable
fun XiaHongDemoScreen() {
    XiaHongFlow(
        modifier = Modifier.fillMaxSize(),
        intensity = XiaHongIntensity.ULTRA,
        config = XiaHongConfig(
            primaryColor = Color(0xFFE91E63),
            particleCount = 600,
            enableLiquid = true
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "霞红流光",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "XiaHong Flow · 演示",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * 仅用于 Android Studio 的 Compose 预览（演示模块内，库本身不写 @Preview）。
 * 无需启动模拟器即可在 IDE 中查看流光效果（预览为静态帧，动效需运行 app）。
 */
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
private fun XiaHongDemoScreenPreview() {
    MaterialTheme {
        XiaHongDemoScreen()
    }
}
