package com.example.app

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui_xiahong.ElectricSliderStep
import com.example.ui_xiahong.NeonElectricDiscreteSlider
import com.example.ui_xiahong.NeonConfig
import com.example.ui_xiahong.NeonFlow
import com.example.ui_xiahong.NeonIntensity
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // 演示：档位调谐器 + 流光特效（库组件驱动）
                ModelIntelligenceEffortTuner()
            }
        }
    }
}

/**
 * 演示界面：用 NeonFlow 包裹一组精致的进度条 UI，验证三层特效叠加在真实控件上的观感。
 * 直接运行此 app 模块即可在设备上看到流动、发光、电弧 + 进度条动画。
 */
@Composable
fun NeonDemoScreen() {
    NeonFlow(
        modifier = Modifier.fillMaxSize(),
        intensity = NeonIntensity.ULTRA,
        config = NeonConfig(
            primaryColor = Color(0xFFE91E63),
            particleCount = 600,
            enableLiquid = true
        )
    ) {
        DemoContent()
    }
}

// ── 统一配色：霓虹流光暗色玻璃主题下的渐变光带 ──────────────────────────
private val RoseGlow = listOf(Color(0xFFFF8FA3), Color(0xFFFF2D55))
private val CyanGlow = listOf(Color(0xFF9BE7F0), Color(0xFF00B8D4))
private val VioletGlow = listOf(Color(0xFFD0B4FF), Color(0xFF7C4DFF))
private val AmberGlow = listOf(Color(0xFFFFE08A), Color(0xFFFFA000))
private val MintGlow = listOf(Color(0xFFC8F7D4), Color(0xFF00C853))
private val OrangeGlow = listOf(Color(0xFFFFC1A6), Color(0xFFFF5722))

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            EnterReveal(delayMs = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "霓虹流光",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 4.sp,
                        style = MaterialTheme.typography.displayMedium.copy(
                            shadow = Shadow(color = Color(0xFFFF2D55), blurRadius = 18f)
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Neon Flow · 特效演示",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        letterSpacing = 2.sp
                    )
                }
            }

            // 霓虹离散磁吸滑块：证明流光特效可叠加在「可交互自定义控件」之上，而非仅包裹静态进度条
            EnterReveal(delayMs = 80) {
                NeonSliderDemo()
            }

            EnterReveal(delayMs = 170) {
                ProgressCard(title = "下载中", gradient = RoseGlow) {
                    LoopingLinearProgress(gradient = RoseGlow, durationMillis = 1500)
                }
            }
            EnterReveal(delayMs = 180) {
                StaticLinearProgress(title = "已安装", gradient = CyanGlow, progress = 0.78f)
            }
            EnterReveal(delayMs = 270) {
                ProgressCard(title = "缓冲", gradient = VioletGlow) {
                    LoopingLinearProgress(gradient = VioletGlow, durationMillis = 2600)
                }
            }
            EnterReveal(delayMs = 360) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularTile(label = "同步", gradient = MintGlow) {
                        GlowCircularProgress(
                            progress = 0f,
                            gradient = MintGlow,
                            sizeDp = 72,
                            indeterminate = true
                        )
                    }
                    CircularTile(label = "上传", gradient = AmberGlow) {
                        LoopingCircularProgress(gradient = AmberGlow, durationMillis = 1800, sizeDp = 72)
                    }
                }
            }
            EnterReveal(delayMs = 450) {
                ProgressCard(title = "解压", gradient = OrangeGlow) {
                    LoopingLinearProgress(gradient = OrangeGlow, durationMillis = 2000, reverse = true)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** 入场揭示：淡入 + 轻微上移，形成错落有致的加载节奏。 */
@Composable
private fun EnterReveal(delayMs: Int, content: @Composable () -> Unit) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); visible.value = true }
    val alpha by animateFloatAsState(if (visible.value) 1f else 0f, tween(500))
    val offsetY by animateFloatAsState(if (visible.value) 0f else 18f, tween(500))
    Column(
        Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = offsetY
        }
    ) { content() }
}

@Composable
private fun ProgressCard(
    title: String,
    gradient: List<Color>,
    content: @Composable () -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp, 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                // 渐变发光指示点
                Canvas(Modifier.size(10.dp)) {
                    val glow = Paint().apply {
                        color = gradient.last().toArgb()
                        maskFilter = BlurMaskFilter(size.minDimension * 0.6f, BlurMaskFilter.Blur.NORMAL)
                    }
                    drawContext.canvas.nativeCanvas.drawCircle(
                        size.width / 2f, size.height / 2f, size.minDimension / 2f, glow
                    )
                    drawCircle(Brush.radialGradient(gradient), radius = size.minDimension / 2f)
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun StaticLinearProgress(
    title: String,
    gradient: List<Color>,
    progress: Float
) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp, 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.sp)
                Text(
                    "${(progress * 100).toInt()}%",
                    color = gradient.last(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            GlowLinearProgress(progress = progress, gradient = gradient)
        }
    }
}

@Composable
private fun CircularTile(
    label: String,
    gradient: List<Color>,
    content: @Composable () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier.padding(18.dp, 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
            Spacer(Modifier.height(10.dp))
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        shape = MaterialTheme.shapes.large
    ) {
        // 顶部高光描边，营造玻璃质感
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                    ),
                    size = Size(size.width, size.height.coerceAtMost(1.5.dp.toPx())),
                    cornerRadius = CornerRadius(0f)
                )
            }
            content()
        }
    }
}

/** 精致线性进度条：圆角轨道 + 渐变填充 + 发光头部。 */
@Composable
private fun GlowLinearProgress(
    progress: Float,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    trackAlpha: Float = 0.10f,
    heightDp: Float = 9f
) {
    val density = LocalDensity.current
    val hPx = with(density) { heightDp.dp.toPx() }
    Canvas(modifier.fillMaxWidth().height(heightDp.dp)) {
        val w = size.width
        val r = hPx / 2f
        // 轨道
        drawRoundRect(
            color = Color.White.copy(alpha = trackAlpha),
            size = size,
            cornerRadius = CornerRadius(r, r)
        )
        val p = progress.coerceIn(0f, 1f)
        if (p > 0.001f) {
            val fillW = w * p
            val cr = minOf(r, fillW / 2f)
            drawRoundRect(
                brush = Brush.horizontalGradient(gradient),
                size = Size(fillW, size.height),
                cornerRadius = CornerRadius(cr, cr)
            )
            // 发光头部
            val glowPaint = Paint().apply {
                color = gradient.last().toArgb()
                maskFilter = BlurMaskFilter(hPx * 1.8f, BlurMaskFilter.Blur.NORMAL)
            }
            drawContext.canvas.nativeCanvas.drawCircle(fillW, hPx / 2f, hPx * 0.75f, glowPaint)
        }
    }
}

/** 循环线性进度条（带实时百分比）。 */
@Composable
private fun LoopingLinearProgress(
    gradient: List<Color>,
    durationMillis: Int,
    reverse: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "lp")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = if (reverse) RepeatMode.Reverse else RepeatMode.Restart
        ),
        label = "lpv"
    )
    GlowLinearProgress(progress = value, gradient = gradient)
    Spacer(Modifier.height(8.dp))
    Text(
        "${(value * 100).toInt()}%",
        color = gradient.last(),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End
    )
}

/** 精致圆环进度条：扫描渐变弧 + 发光弧尖 + 中心百分比。 */
@Composable
private fun GlowCircularProgress(
    progress: Float,
    gradient: List<Color>,
    sizeDp: Int,
    indeterminate: Boolean = false,
    strokeDp: Float = 6f
) {
    val density = LocalDensity.current
    val sw = with(density) { strokeDp.dp.toPx() }
    val rotation = if (indeterminate) {
        val t = rememberInfiniteTransition(label = "rot")
        t.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            "rot"
        ).value
    } else 0f
    val p = progress.coerceIn(0f, 1f)
    Box(modifier = Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val min = minOf(size.width, size.height)
            val radius = (min - sw) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            // 轨道
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = center - Offset(radius, radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            val startAngle = 270f + rotation
            val sweep = if (indeterminate) 90f else 360f * p
            if (sweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(gradient, center),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = center - Offset(radius, radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
                // 发光弧尖
                val tipAngle = Math.toRadians((startAngle + sweep).toDouble())
                val tip = Offset(
                    center.x + radius * cos(tipAngle).toFloat(),
                    center.y + radius * sin(tipAngle).toFloat()
                )
                val glowPaint = Paint().apply {
                    color = gradient.last().toArgb()
                    maskFilter = BlurMaskFilter(sw * 1.8f, BlurMaskFilter.Blur.NORMAL)
                }
                drawContext.canvas.nativeCanvas.drawCircle(tip.x, tip.y, sw * 0.8f, glowPaint)
            }
        }
        if (!indeterminate) {
            Text(
                "${(p * 100).toInt()}%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

/** 循环圆环进度条（带实时百分比）。 */
@Composable
private fun LoopingCircularProgress(
    gradient: List<Color>,
    durationMillis: Int,
    sizeDp: Int
) {
    val transition = rememberInfiniteTransition(label = "cp")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cpv"
    )
    GlowCircularProgress(progress = value, gradient = gradient, sizeDp = sizeDp)
}

/**
 * 霓虹离散磁吸滑块演示：直接在 NeonFlow 内容层内使用 NeonDiscreteSlider，
 * 配合档位按钮与响应式标签，证明流光特效可叠加在「可交互自定义控件」之上。
 */
@Composable
private fun NeonSliderDemo() {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp, 14.dp)) {
            var currentStep by remember { mutableStateOf(ElectricSliderStep.High) }
            Text(
                text = "霓虹磁吸滑块 · 电流弧",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Current: ${currentStep.label}" +
                    if (currentStep == ElectricSliderStep.Max) " · 放电中" else "",
                color = if (currentStep == ElectricSliderStep.Max) Color(0xFFFF2A85) else Color(0xFF00C6FF),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            NeonElectricDiscreteSlider(
                value = currentStep,
                onValueChange = { currentStep = it }
            )
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ElectricSliderStep.entries.forEach { step ->
                    Button(
                        onClick = { currentStep = step },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == step) Color(0xFFFF2A85) else Color(0xFF1F222E)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(step.label, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/**
 * 仅用于 Android Studio 的 Compose 预览（演示模块内，库本身不写 @Preview）。
 * 无需启动模拟器即可在 IDE 中查看流光与进度条布局（预览为静态帧，动效需运行 app）。
 */
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
private fun NeonDemoScreenPreview() {
    MaterialTheme {
        NeonDemoScreen()
    }
}
