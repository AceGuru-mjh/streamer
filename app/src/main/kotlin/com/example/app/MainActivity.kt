package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
                // 半透明底 + 流光特效，便于观察叠加在内容上的流动/发光/电弧效果
                XiaHongDemoScreen()
            }
        }
    }
}

/**
 * 演示界面：用 XiaHongFlow 包裹一组进度条 UI，验证三层特效叠加在真实控件上的观感。
 * 直接运行此 app 模块即可在设备上看到流动、发光、电弧 + 进度条动画。
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
        DemoContent()
    }
}

@Composable
private fun DemoContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "霞红流光",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "XiaHong Flow · 进度演示",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))

            // 1. 循环进度条（快）：模拟高速下载
            ProgressCard(title = "下载中", subtitle = "高速通道") {
                LoopingLinearProgress(color = Color(0xFFE91E63), durationMillis = 1500)
            }

            // 2. 静态进度条：展示固定完成度
            ProgressCard(title = "已安装", subtitle = "78%") {
                LinearProgressIndicator(
                    progress = { 0.78f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4DD0E1),
                    trackColor = Color(0xFF4DD0E1).copy(alpha = 0.2f)
                )
            }

            // 3. 循环进度条（慢）：模拟稳定缓冲
            ProgressCard(title = "缓冲", subtitle = "稳定加载") {
                LoopingLinearProgress(color = Color(0xFFB388FF), durationMillis = 2600)
            }

            // 4. 圆环组合：不确定 + 确定循环
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularTile(label = "同步", color = Color(0xFF69F0AE)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = Color(0xFF69F0AE),
                        strokeWidth = 5.dp,
                        trackColor = Color(0xFF69F0AE).copy(alpha = 0.2f)
                    )
                }
                CircularTile(label = "上传", color = Color(0xFFFFD54F)) {
                    LoopingCircularProgress(color = Color(0xFFFFD54F), durationMillis = 1800)
                }
            }

            // 5. 反向循环进度条：模拟解压往返
            ProgressCard(title = "解压", subtitle = "往返循环") {
                LoopingLinearProgress(color = Color(0xFFFF8A65), durationMillis = 2000, reverse = true)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CircularTile(
    label: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LoopingLinearProgress(
    color: Color,
    durationMillis: Int,
    reverse: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "lp")
    val value = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = if (reverse) RepeatMode.Reverse else RepeatMode.Restart
        ),
        label = "lpv"
    )
    LinearProgressIndicator(
        progress = { value.value },
        modifier = Modifier.fillMaxWidth(),
        color = color,
        trackColor = color.copy(alpha = 0.2f)
    )
}

@Composable
private fun LoopingCircularProgress(color: Color, durationMillis: Int) {
    val transition = rememberInfiniteTransition(label = "cp")
    val value = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cpv"
    )
    CircularProgressIndicator(
        progress = { value.value },
        modifier = Modifier.size(56.dp),
        color = color,
        strokeWidth = 5.dp,
        trackColor = color.copy(alpha = 0.2f)
    )
}

/**
 * 仅用于 Android Studio 的 Compose 预览（演示模块内，库本身不写 @Preview）。
 * 无需启动模拟器即可在 IDE 中查看流光与进度条布局（预览为静态帧，动效需运行 app）。
 */
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
private fun XiaHongDemoScreenPreview() {
    MaterialTheme {
        XiaHongDemoScreen()
    }
}
