package com.example.app

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * 离散档位枚举
 */
enum class SliderStep(val stepValue: Float, val label: String) {
    Low(0f, "Low"),
    Medium(1f, "Medium"),
    High(2f, "High"),
    XHigh(3f, "XHigh"),
    Max(4f, "Max");

    companion object {
        fun fromValue(value: Float): SliderStep {
            val rounded = value.coerceIn(0f, 4f)
            return entries.minByOrNull { kotlin.math.abs(it.stepValue - rounded) } ?: Low
        }
    }
}

/**
 * 粒子内部数据结构（矩阵方块）
 */
private class SquareParticle(
    var normalizedX: Float, // 0.0 ~ 1.0 相对轨道位置
    var normalizedY: Float, // 0.0 ~ 1.0 相对轨道高度
    val sizePx: Float,      // 方块尺寸 (2-4dp 换算)
    val color: Color,       // 霓虹配色
    val speed: Float,       // 飘移速度
    val phase: Float,       // 缩放/透明度相位偏移
    val pulseFrequency: Float // 律动频率
)

private val NeonMatrixColors = listOf(
    Color(0xCC00F2FE), // 半透明青色
    Color(0xCC9B51E0), // 半透明紫罗兰
    Color(0xCCFF2A85)  // 半透明淡洋红
)

private val TrackDark = Color(0xFF12131C)
private val FillCyan = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
private val FillMax = listOf(Color(0xFF00F2FE), Color(0xFF9B51E0), Color(0xFFFF2A85))

/**
 * 高性能霓虹磁吸吸附滑块 (Neon Discrete Magnetic Slider + Ultracode 电流弧)
 *
 * - 保留「律动小方块」粒子与「离散磁吸物理引擎」。
 * - 新增刻度 notch ticks、双环 Thumb。
 * - 切换到 Max 档位时，叠加原生 Procedural 电流/闪电弧特效（矩阵方块 + 电弧 + Thumb 火花）。
 *
 * @param value 当前选中的档位
 * @param onValueChange 档位变化回调
 * @param modifier 外部样式修饰符
 * @param reduceMotion 是否启用减弱动画 (从系统设置获取)
 */
@Composable
fun NeonDiscreteSlider(
    value: SliderStep,
    onValueChange: (SliderStep) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 28.dp,
    thumbSize: Dp = 36.dp,
    reduceMotion: Boolean = false
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // 内部连续刻度值 (0.0f - 4.0f)
    val animatedValue = remember { Animatable(value.stepValue) }

    // 拖拽状态
    var isDragging by remember { mutableStateOf(false) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    // 当外部传入的 value 改变且未在拖拽中时，进行磁吸动画同步
    LaunchedEffect(value, isDragging) {
        if (!isDragging && animatedValue.targetValue != value.stepValue) {
            animatedValue.animateTo(
                targetValue = value.stepValue,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // 档位判定：是否触发了 Max 档位电流特效
    val isMaxStep = SliderStep.fromValue(animatedValue.value) == SliderStep.Max

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbSize),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. 底层滑块轨道 + 刻度 notch + 霓虹粒子 + 电流弧 Canvas
        val isAndroid12OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val blurRadius = if (isAndroid12OrAbove && isMaxStep) 2.dp else 0.dp

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.Center)
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
        ) {
            trackWidthPx = size.width
            val cornerRadiusPx = size.height / 2f
            val currentProgress = (animatedValue.value / 4f).coerceIn(0f, 1f)
            val fillWidthPx = size.width * currentProgress

            // 绘制深色底轨
            drawRoundRect(
                color = TrackDark,
                size = size,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )

            // 绘制当前激活部分的渐变填充
            if (fillWidthPx > 0f) {
                val activeGradient = Brush.horizontalGradient(
                    colors = if (isMaxStep) FillMax else FillCyan,
                    endX = fillWidthPx
                )

                // 激活轨道的裁剪路径 (圆角 Pill 形状)
                val trackPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(0f, 0f, fillWidthPx.coerceAtLeast(cornerRadiusPx * 2), size.height),
                            CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    )
                }

                clipPath(trackPath) {
                    drawRect(
                        brush = activeGradient,
                        size = Size(fillWidthPx, size.height)
                    )
                }
            }

            // 刻度 notch ticks：5 个固定档位刻度
            val tickCount = SliderStep.entries.size
            val tickColor = Color.White.copy(alpha = 0.25f)
            val tickActiveColor = Color.White.copy(alpha = 0.7f)
            val tickH = size.height * 0.34f
            for (i in 0 until tickCount) {
                val x = (i.toFloat() / (tickCount - 1)) * size.width
                val active = (currentProgress * (tickCount - 1)) >= i - 0.001f
                drawLine(
                    color = if (active) tickActiveColor else tickColor,
                    start = Offset(x, (size.height - tickH) / 2f),
                    end = Offset(x, (size.height + tickH) / 2f),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        // 2. 矩阵粒子 + 程序化电流弧 叠加层 (仅当 isMaxStep = true 时激活，非 Max 完全不绘制)
        if (isMaxStep) {
            NeonMaxOverlay(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .align(Alignment.Center),
                fillProgress = (animatedValue.value / 4f).coerceIn(0f, 1f),
                reduceMotion = reduceMotion
            )
        }

        // 3. 双环 Thumb 与手势处理
        val thumbPx = with(density) { thumbSize.toPx() }
        val availableWidth = (trackWidthPx - thumbPx).coerceAtLeast(1f)
        val thumbOffset = (animatedValue.value / 4f) * availableWidth

        val ringColor = if (isMaxStep) Color(0xFFFF2A85) else Color(0xFF00C6FF)
        val ringColor2 = if (isMaxStep) Color(0xFF9B51E0) else Color(0xFF00F2FE)

        Canvas(
            modifier = Modifier
                .size(thumbSize)
                .align(Alignment.CenterStart)
                .graphicsLayer { translationX = thumbOffset }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            // 拖拽松手，计算最近的离散档位并触发磁吸弹簧吸附
                            val targetStep = SliderStep.fromValue(animatedValue.value)
                            scope.launch {
                                animatedValue.animateTo(
                                    targetValue = targetStep.stepValue,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                onValueChange(targetStep)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val deltaStep = (dragAmount / availableWidth) * 4f
                            val newValue = (animatedValue.value + deltaStep).coerceIn(0f, 4f)
                            scope.launch {
                                animatedValue.snapTo(newValue)
                            }
                        }
                    )
                }
        ) {
            val r = size.minDimension / 2f
            // 外发光底色
            drawCircle(
                color = ringColor.copy(alpha = 0.35f),
                radius = r
            )
            // 外环
            drawCircle(
                color = ringColor,
                radius = r - 1.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
            // 白色核心
            drawCircle(
                color = Color.White,
                radius = (r - 4.dp.toPx())
            )
            // 内环
            drawCircle(
                color = ringColor2,
                radius = (r - 9.dp.toPx()).coerceAtLeast(2.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

/**
 * Max 档位特效叠加层：矩阵方块粒子 + 程序化电流/闪电弧 + Thumb 火花。
 * 仅在 isMaxStep 时挂载；withFrameNanos 帧循环只在 Max 时运行，非 Max 零开销。
 */
@Composable
private fun NeonMaxOverlay(
    fillProgress: Float,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val particleCount = 45
    val particles = remember {
        List(particleCount) {
            val sizeDp = Random.nextFloat() * 2f + 2f // 2dp - 4dp
            SquareParticle(
                normalizedX = Random.nextFloat(),
                normalizedY = Random.nextFloat(),
                sizePx = with(density) { sizeDp.dp.toPx() },
                color = NeonMatrixColors[Random.nextInt(NeonMatrixColors.size)],
                speed = Random.nextFloat() * 0.15f + 0.05f,
                phase = Random.nextFloat() * 2f * Math.PI.toFloat(),
                pulseFrequency = Random.nextFloat() * 2f + 1f
            )
        }
    }

    // Nanos 帧脉冲，驱使粒子与电流弧循环运动
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            while (true) {
                withFrameNanos { nanos ->
                    frameTimeNanos = nanos
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val trackWidth = size.width
        val trackHeight = size.height
        val cornerRadiusPx = trackHeight / 2f
        val activeWidth = trackWidth * fillProgress

        if (activeWidth <= 0f) return@Canvas

        val seconds = frameTimeNanos / 1_000_000_000f
        val flicker = if (reduceMotion) 1f else (0.6f + 0.4f * sin(seconds * 22f))

        // 裁剪路径：粒子与电弧严格限制在圆角轨道填充区内
        val clipPath = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(0f, 0f, activeWidth, trackHeight),
                    CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            )
        }

        clipPath(clipPath) {
            // (a) 矩阵方块粒子：从左向右漂移 + 周期律动
            particles.forEach { p ->
                if (!reduceMotion && frameTimeNanos > 0L) {
                    p.normalizedX += p.speed * 0.016f
                    if (p.normalizedX > 1.0f) p.normalizedX = 0.0f
                }
                val realX = p.normalizedX * activeWidth
                val realY = p.normalizedY * trackHeight
                val pulse = sin(seconds * p.pulseFrequency + p.phase)
                val scale = 0.7f + 0.3f * pulse
                val alpha = (0.45f + 0.25f * pulse).coerceIn(0.2f, 0.7f)
                val currentSize = p.sizePx * scale
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(realX - currentSize / 2f, realY - currentSize / 2f),
                    size = Size(currentSize, currentSize)
                )
            }

            // (b) 程序化电流/闪电弧：每帧用新种子生成抖动裂纹
            val seed = if (reduceMotion) 1337L else (frameTimeNanos / 16_000_000L)
            drawElectricArcs(activeWidth, trackHeight, seconds, flicker, seed)

            // (c) Thumb 火花：在拇指当前位置放射状电弧火花
            val thumbPx = 36.dp.toPx()
            val availableWidth = (trackWidth - thumbPx).coerceAtLeast(1f)
            val thumbOffsetX = fillProgress * availableWidth + thumbPx / 2f
            drawThumbElectricSparks(thumbOffsetX, trackHeight / 2f, seconds, flicker, seed)
        }
    }
}

/**
 * 程序化电流/闪电弧：在激活填充区生成一条带垂直抖动的主电弧 + 若干分叉放电枝。
 * 每帧使用不同 seed 重生成，形成持续闪烁、跳变的电流观感（原生 Canvas 绘制，零重组）。
 */
private fun DrawScope.drawElectricArcs(
    activeWidth: Float,
    trackHeight: Float,
    seconds: Float,
    flicker: Float,
    seed: Long
) {
    val rnd = Random(seed)
    val baseY = trackHeight / 2f

    fun buildJagged(startX: Float, endX: Float, amp: Float, steps: Int): android.graphics.Path {
        val path = android.graphics.Path()
        path.moveTo(startX, baseY)
        for (i in 1..steps) {
            val x = startX + (endX - startX) * i / steps
            val y = baseY + (rnd.nextFloat() - 0.5f) * amp
            path.lineTo(x, y)
        }
        return path
    }

    // 主电弧
    val mainPath = buildJagged(0f, activeWidth, trackHeight * 0.6f, 16)

    // 分叉放电枝
    val branchPaths = List(3) {
        val bx0 = activeWidth * rnd.nextFloat()
        val bx1 = bx0 + (rnd.nextFloat() - 0.5f) * activeWidth * 0.5f
        buildJagged(bx0, bx1.coerceIn(0f, activeWidth), trackHeight * 0.9f, 6)
    }

    val canvas = drawContext.canvas.nativeCanvas

    val glowPaint = Paint().apply {
        color = Color(0xFFFF2A85).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
        maskFilter = BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL)
        alpha = (200 * flicker).toInt().coerceIn(80, 255)
    }
    val corePaint = Paint().apply {
        color = Color.White.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
        alpha = (230 * flicker).toInt().coerceIn(100, 255)
    }

    canvas.drawPath(mainPath, glowPaint)
    canvas.drawPath(mainPath, corePaint)
    branchPaths.forEach {
        canvas.drawPath(it, glowPaint)
        canvas.drawPath(it, corePaint)
    }
}

/**
 * Thumb 火花：从拇指位置向四周放射 4~6 条短促抖动电流火花，带闪烁。
 */
private fun DrawScope.drawThumbElectricSparks(
    centerX: Float,
    centerY: Float,
    seconds: Float,
    flicker: Float,
    seed: Long
) {
    val rnd = Random(seed xor 0x9E3779B9L)
    val canvas = drawContext.canvas.nativeCanvas
    val sparkCount = 5
    val maxLen = 26f

    val glowPaint = Paint().apply {
        color = Color(0xFF00F2FE).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
        maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
        alpha = (220 * flicker).toInt().coerceIn(90, 255)
    }
    val corePaint = Paint().apply {
        color = Color.White.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        isAntiAlias = true
        alpha = (240 * flicker).toInt().coerceIn(110, 255)
    }

    for (i in 0 until sparkCount) {
        val angle = (i.toFloat() / sparkCount) * 2f * Math.PI.toFloat() + rnd.nextFloat() * 0.4f
        val len = maxLen * (0.6f + 0.4f * rnd.nextFloat())
        val midX = centerX + kotlin.math.cos(angle) * len * 0.5f + (rnd.nextFloat() - 0.5f) * 6f
        val midY = centerY + kotlin.math.sin(angle) * len * 0.5f + (rnd.nextFloat() - 0.5f) * 6f
        val endX = centerX + kotlin.math.cos(angle) * len
        val endY = centerY + kotlin.math.sin(angle) * len
        val spark = android.graphics.Path().apply {
            moveTo(centerX, centerY)
            lineTo(midX, midY)
            lineTo(endX, endY)
        }
        canvas.drawPath(spark, glowPaint)
        canvas.drawPath(spark, corePaint)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0C10)
@Composable
private fun NeonDiscreteSliderPreview() {
    androidx.compose.material3.MaterialTheme {
        var currentStep by remember { mutableStateOf(SliderStep.High) }
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xFF0B0C10))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NeonDiscreteSlider(value = currentStep, onValueChange = { currentStep = it })
        }
    }
}
