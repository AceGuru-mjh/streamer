package com.example.ui_xiahong

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 离散档位枚举（对外 API，供消费方指定当前档位）。
 * 注意：[UltraCode] 为「突破上限」专属高能档，应与普通档位以分割线区分。
 */
enum class ElectricSliderStep(val stepValue: Float, val label: String) {
    Low(0f, "LOW"),
    Medium(1f, "MED"),
    High(2f, "HIGH"),
    XHigh(3f, "XHIGH"),
    Max(4f, "MAX"),
    UltraCode(5f, "ULTRACODE");

    companion object {
        /** 最高档位的数值，用作量程归一化分母。 */
        val MAX_VALUE: Float = entries.last().stepValue

        fun fromValue(value: Float): ElectricSliderStep {
            val rounded = value.coerceIn(0f, MAX_VALUE)
            return entries.minByOrNull { abs(it.stepValue - rounded) } ?: Low
        }
    }
}

/** 律动小方块粒子模型（文件内私有，不进入库 ABI）。 */
private class SquareParticle(
    var normX: Float,
    var normY: Float,
    val sizePx: Float,
    val color: Color,
    val speed: Float,
    val phase: Float,
    val pulseFreq: Float
)

/** 电流折线通道配置（文件内私有）。 */
private class ElectricArcConfig(
    val yRatio: Float,        // 相对高度基准 (0.2 ~ 0.8)
    val amplitudePx: Float,   // 振幅折线偏差
    val segmentLengthPx: Float, // 节点破裂段长
    val color: Color,         // 电流颜色
    val glowColor: Color      // 外围发光层颜色
)

/**
 * 带有「律动小方块」与「横向能量电流」的霓虹离散磁吸滑块（库组件）。
 *
 * - 离散磁吸物理引擎（Animatable 弹簧吸附），档位列表 0..5。
 * - [ElectricSliderStep.Max] / [ElectricSliderStep.UltraCode] 时叠加原生 Procedural 电流/闪电弧特效
 *   （三条平行放电通道 + Thumb 边缘放射火花），并共享同一 [clipPath] 严格不溢出圆角轨道。
 * - [ElectricSliderStep.UltraCode] 专属高能模式：电光青主题、粒子数量/速度翻倍、电弧频繁分叉持续闪烁、
 *   霓虹边框呼吸脉冲、吸附进入瞬间触发一次电弧爆发动画。
 * - 切离高能档位时，帧动画回调与算法同步终止，维持 0 额外 CPU/GPU 开销。
 *
 * @param value 当前档位
 * @param onValueChange 档位变化回调（磁吸吸附后或手动切换时触发）
 * @param modifier 外部样式修饰符
 * @param reduceMotion 是否启用减弱动画（从系统设置获取）
 */
@Composable
fun NeonElectricDiscreteSlider(
    value: ElectricSliderStep,
    onValueChange: (ElectricSliderStep) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 26.dp,
    thumbSize: Dp = 34.dp,
    reduceMotion: Boolean = false
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val maxStep = ElectricSliderStep.MAX_VALUE

    // 内部驱动值 (0.0f ~ maxStep)
    val animatedValue = remember { Animatable(value.stepValue) }
    var isDragging by remember { mutableStateOf(false) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    // 档位同步与磁吸
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

    val currentStep = ElectricSliderStep.fromValue(animatedValue.value)
    val isMaxStep = currentStep == ElectricSliderStep.Max
    val isUltraStep = currentStep == ElectricSliderStep.UltraCode
    val isHighEffect = isMaxStep || isUltraStep // Max / UltraCode 开启滑块内电弧与粒子

    // 1. 预先分配粒子；UltraCode 翻倍数量并翻倍漂移速度
    val particleCount = if (isUltraStep) 80 else 40
    val particles = remember(isUltraStep) {
        val neonPalette = listOf(
            Color(0xFF00F3FF), // 电光青
            Color(0xFF8A2BE2), // 霓虹紫
            Color(0xFFFF007A)  // 淡洋红
        )
        List(particleCount) {
            val sizeDp = Random.nextFloat() * 2f + 2f // 2~4dp 正方形
            val speedMul = if (isUltraStep) 2f else 1f
            SquareParticle(
                normX = Random.nextFloat(),
                normY = Random.nextFloat(),
                sizePx = with(density) { sizeDp.dp.toPx() },
                color = neonPalette[Random.nextInt(neonPalette.size)],
                speed = (Random.nextFloat() * 0.12f + 0.04f) * speedMul,
                phase = Random.nextFloat() * 6.28f,
                pulseFreq = Random.nextFloat() * 2f + 1f
            )
        }
    }

    // 2. 配置 3 条穿梭在轨道内部的电流通道
    val arcConfigs = remember {
        listOf(
            ElectricArcConfig(
                yRatio = 0.25f,
                amplitudePx = with(density) { 3.dp.toPx() },
                segmentLengthPx = with(density) { 12.dp.toPx() },
                color = Color(0xFFFFFFFF),
                glowColor = Color(0xFF00F3FF)
            ),
            ElectricArcConfig(
                yRatio = 0.50f,
                amplitudePx = with(density) { 4.dp.toPx() },
                segmentLengthPx = with(density) { 16.dp.toPx() },
                color = Color(0xFFE0AAFF),
                glowColor = Color(0xFF8A2BE2)
            ),
            ElectricArcConfig(
                yRatio = 0.75f,
                amplitudePx = with(density) { 3.dp.toPx() },
                segmentLengthPx = with(density) { 10.dp.toPx() },
                color = Color(0xFFFFFFFF),
                glowColor = Color(0xFFFF007A)
            )
        )
    }

    // 3. 帧驱动时钟（仅在高能档位且未开启 Reduce Motion 时激活；其余档位 0 开销）
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isHighEffect, reduceMotion) {
        if (isHighEffect && !reduceMotion) {
            while (true) {
                withFrameNanos { nanos -> frameTimeNanos = nanos }
            }
        }
    }

    // 4. 霓虹边框呼吸脉冲（仅 UltraCode 使用，infiniteTransition 开销极低）
    val breathTransition = rememberInfiniteTransition(label = "ultra-breath")
    val breath by breathTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "ultra-breath"
    )

    // 5. 进入 UltraCode 瞬间的电弧爆发动画（一次性）
    val burstAnim = remember { Animatable(0f) }
    LaunchedEffect(isUltraStep) {
        if (isUltraStep) {
            burstAnim.snapTo(0f)
            burstAnim.animateTo(1f, tween(650))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbSize),
        contentAlignment = Alignment.CenterStart
    ) {
        // Android 12+ 模糊降级
        val isAndroid12OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val blurModifier = if (isAndroid12OrAbove && isHighEffect) Modifier.blur(3.dp) else Modifier

        // --- 单 Canvas 级联绘制 (底轨 + 渐变填充 + 律动小方块 + 电流折线 + 节点刻度 + 爆发) ---
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.Center)
                .then(blurModifier)
        ) {
            trackWidthPx = size.width
            val cornerRadius = size.height / 2f
            val currentProgress = (animatedValue.value / maxStep).coerceIn(0f, 1f)
            val fillWidth = size.width * currentProgress
            val thumbPx = with(density) { thumbSize.toPx() }

            // 1. 深色 Pill 底轨
            drawRoundRect(
                color = Color(0xFF0D0E15),
                size = size,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
            drawRoundRect(
                color = Color(0xFF1E202E),
                size = size,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. 渐变 Fill 轨道
            if (fillWidth > 0f) {
                val fillColors = when {
                    isUltraStep -> listOf(Color(0xFF00FFFF), Color(0xFF00B3FF), Color(0xFF8A2BE2))
                    isMaxStep -> listOf(Color(0xFF00F3FF), Color(0xFF8A2BE2), Color(0xFFFF007A))
                    else -> listOf(Color(0xFF0052D4), Color(0xFF4364F7), Color(0xFF6FB1FC))
                }
                val activeGradient = Brush.horizontalGradient(colors = fillColors, endX = fillWidth)

                val trackClipPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(0f, 0f, fillWidth.coerceAtLeast(cornerRadius * 2), size.height),
                            CornerRadius(cornerRadius, cornerRadius)
                        )
                    )
                }

                // **严格裁剪在轨道边界内**
                clipPath(trackClipPath) {
                    drawRect(brush = activeGradient, size = Size(fillWidth, size.height))

                    // 高能档位：渲染粒子与电流特效
                    if (isHighEffect) {
                        val seconds = frameTimeNanos / 1_000_000_000f

                        // A. 律动正方形小方块 (2~4dp)
                        particles.forEach { p ->
                            if (!reduceMotion && frameTimeNanos > 0L) {
                                p.normX += p.speed * 0.016f
                                if (p.normX > 1.0f) p.normX = 0.0f
                            }
                            val px = p.normX * fillWidth
                            val py = p.normY * size.height
                            val pulse = sin(seconds * p.pulseFreq + p.phase)
                            val scale = 0.7f + 0.3f * pulse
                            val alpha = (0.45f + 0.25f * pulse).coerceIn(0.2f, 0.7f)
                            val drawSize = p.sizePx * scale
                            drawRect(
                                color = p.color.copy(alpha = alpha),
                                topLeft = Offset(px - drawSize / 2f, py - drawSize / 2f),
                                size = Size(drawSize, drawSize)
                            )
                        }

                        // B. 横向流动电弧电流 (Electric Arcs)
                        if (!reduceMotion && frameTimeNanos > 0L) {
                            drawElectricArcs(
                                fillWidth = fillWidth,
                                trackHeight = size.height,
                                configs = arcConfigs,
                                frameTimeNanos = frameTimeNanos,
                                isUltra = isUltraStep
                            )
                        }
                    }
                }
            }

            // 3. 刻度卡槽 (Notch Ticks)
            val usableWidth = size.width - size.height
            for (i in 0..maxStep.toInt()) {
                val tickX = (size.height / 2f) + (i / maxStep) * usableWidth
                val isPassed = animatedValue.value >= i
                val tickColor = if (isPassed) Color.White.copy(alpha = 0.9f) else Color(0xFF2C2F45)
                drawCircle(
                    color = tickColor,
                    radius = 2.dp.toPx(),
                    center = Offset(tickX, size.height / 2f)
                )
            }

            // 4. UltraCode 分割线：在 Max(4) 与 UltraCode(5) 之间以电光青高亮分隔，代表「突破上限」
            if (isUltraStep || animatedValue.value >= maxStep - 1f) {
                val dividerX = (size.height / 2f) + ((maxStep - 1f + 0.5f) / maxStep) * usableWidth
                drawLine(
                    color = Color(0xFF00FFFF).copy(alpha = 0.85f),
                    start = Offset(dividerX, size.height * 0.18f),
                    end = Offset(dividerX, size.height * 0.82f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 5. 进入 UltraCode 的弧形爆发（在轨道 Canvas 上绘制，避免被 Thumb 裁剪）
            if (isUltraStep && burstAnim.value < 1f) {
                val cx = (animatedValue.value / maxStep) * (size.width - thumbPx) + thumbPx / 2f
                val cy = size.height / 2f
                val t = burstAnim.value
                val ringR = (thumbPx / 2f) * (1f + t * 2.4f)
                drawCircle(
                    color = Color(0xFF00FFFF).copy(alpha = (1f - t) * 0.8f),
                    radius = ringR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = Color(0xFF00FFFF).copy(alpha = (1f - t) * 0.5f),
                    radius = ringR * 0.66f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // --- 6. Thumb 滑块与高能放电火花 ---
        val thumbPx2 = with(density) { thumbSize.toPx() }
        val availableWidth = (trackWidthPx - thumbPx2).coerceAtLeast(1f)
        val thumbOffset = (animatedValue.value / maxStep) * availableWidth

        val accentColor = when {
            isUltraStep -> Color(0xFF00FFFF)
            isMaxStep -> Color(0xFFFF007A)
            else -> Color(0xFF00F3FF)
        }

        Box(
            modifier = Modifier
                .size(thumbSize)
                .align(Alignment.CenterStart)
                .graphicsLayer { translationX = thumbOffset }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            val targetStep = ElectricSliderStep.fromValue(animatedValue.value)
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
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = (dragAmount / availableWidth) * maxStep
                            val newValue = (animatedValue.value + delta).coerceIn(0f, maxStep)
                            scope.launch { animatedValue.snapTo(newValue) }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 2f

                // 高能档位：Thumb 周围电火花闪烁
                if (isHighEffect && !reduceMotion && frameTimeNanos > 0L) {
                    drawThumbElectricSparks(center, baseRadius, frameTimeNanos)
                }

                // Thumb 外发光圈
                drawCircle(
                    color = accentColor.copy(alpha = if (isUltraStep) 0.4f else 0.28f),
                    radius = baseRadius
                )
                // 暗黑底圆
                drawCircle(color = Color(0xFF090A0F), radius = baseRadius - 2.dp.toPx())
                // 极客霓虹边框（UltraCode 时呼吸脉冲）
                val ringAlpha = if (isUltraStep) breath else 1f
                drawCircle(
                    color = accentColor.copy(alpha = ringAlpha),
                    radius = baseRadius - 2.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // 内部电光核心
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
            }
        }
    }
}

/**
 * 绘制高频抖动的程序化电流/闪电 Path（文件内私有）。
 * [isUltra] 时刷新更快、追加随机分叉枝，呈现「频繁分叉 + 持续闪烁」的高能观感。
 */
private fun DrawScope.drawElectricArcs(
    fillWidth: Float,
    trackHeight: Float,
    configs: List<ElectricArcConfig>,
    frameTimeNanos: Long,
    isUltra: Boolean
) {
    val seedBase = if (isUltra) {
        (frameTimeNanos / 20_000_000L).toInt() // UltraCode：每 20ms 重塑电弧 → 更频繁闪烁
    } else {
        (frameTimeNanos / 50_000_000L).toInt() // 普通：每 50ms
    }

    configs.forEachIndexed { index, config ->
        val arcPath = Path()
        val baseY = trackHeight * config.yRatio
        var currentX = 0f

        arcPath.moveTo(0f, baseY)

        val rng = Random(seedBase + index * 100)
        while (currentX < fillWidth) {
            currentX += config.segmentLengthPx * (0.8f + rng.nextFloat() * 0.4f)
            val clampedX = currentX.coerceAtMost(fillWidth)
            val offsetY = (rng.nextFloat() - 0.5f) * 2f * config.amplitudePx
            val clampedY = (baseY + offsetY).coerceIn(2f, trackHeight - 2f)
            arcPath.lineTo(clampedX, clampedY)
        }

        // 高频频闪 Alpha（UltraCode 维持更亮、更连续）
        val flickerAlpha = if (isUltra) 0.9f else if (rng.nextBoolean()) 0.85f else 0.35f

        // 发电流发光外边
        drawPath(
            path = arcPath,
            color = config.glowColor.copy(alpha = flickerAlpha * 0.5f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // 电流核心白光
        drawPath(
            path = arcPath,
            color = config.color.copy(alpha = flickerAlpha),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // UltraCode：额外随机分叉放电枝，强化「突破上限」意象
    if (isUltra) {
        val brng = Random(seedBase + 777)
        repeat(4) {
            val bx = brng.nextFloat() * fillWidth
            val by = trackHeight * (0.2f + brng.nextFloat() * 0.6f)
            val branch = Path().apply {
                moveTo(bx, by)
                lineTo(
                    bx + (brng.nextFloat() - 0.5f) * 60f,
                    by + (brng.nextFloat() - 0.5f) * trackHeight * 0.6f
                )
            }
            drawPath(
                path = branch,
                color = Color(0xFF00FFFF).copy(alpha = 0.7f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * 绘制 Thumb 边缘的放射性电火花（文件内私有）。
 */
private fun DrawScope.drawThumbElectricSparks(
    center: Offset,
    baseRadius: Float,
    frameTimeNanos: Long
) {
    val rng = Random((frameTimeNanos / 40_000_000L).toInt()) // 快速闪烁
    val sparkCount = 5

    for (i in 0 until sparkCount) {
        val angle = rng.nextFloat() * 2f * Math.PI.toFloat()
        val innerR = baseRadius - 2.dp.toPx()
        val sparkLength = 4.dp.toPx() + rng.nextFloat() * 6.dp.toPx()
        val outerR = innerR + sparkLength

        val startX = center.x + innerR * cos(angle)
        val startY = center.y + innerR * sin(angle)

        // 弯折电火花
        val midAngle = angle + (rng.nextFloat() - 0.5f) * 0.4f
        val midR = innerR + sparkLength * 0.5f
        val midX = center.x + midR * cos(midAngle)
        val midY = center.y + midR * sin(midAngle)

        val endX = center.x + outerR * cos(angle)
        val endY = center.y + outerR * sin(angle)

        val sparkPath = Path().apply {
            moveTo(startX, startY)
            lineTo(midX, midY)
            lineTo(endX, endY)
        }

        drawPath(
            path = sparkPath,
            color = if (i % 2 == 0) Color(0xFF00F3FF) else Color(0xFFFF007A),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
