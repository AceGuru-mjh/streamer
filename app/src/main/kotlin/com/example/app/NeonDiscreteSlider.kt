package com.example.app

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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
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
 * 粒子内部数据结构
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

/**
 * 高性能霓虹磁吸吸附滑块 (Neon Discrete Magnetic Slider)
 *
 * @param value 当前选中的档位
 * @param onValueChange 档位变化回调（在磁吸吸附后或手动切换时触发）
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

    // 档位判定：是否触发了 Max 档位特效
    val isMaxStep = SliderStep.fromValue(animatedValue.value) == SliderStep.Max

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbSize),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. 底层滑块轨道与霓虹粒子特效 Canvas
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
                color = Color(0xFF12131C),
                size = size,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )

            // 绘制当前激活部分的渐变填充
            if (fillWidthPx > 0f) {
                val activeGradient = Brush.horizontalGradient(
                    colors = if (isMaxStep) listOf(
                        Color(0xFF00F2FE),
                        Color(0xFF9B51E0),
                        Color(0xFFFF2A85)
                    ) else listOf(
                        Color(0xFF00C6FF),
                        Color(0xFF0072FF)
                    ),
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
        }

        // 2. 粒子的渲染层 (仅当 isMaxStep = true 时激活，非 Max 状态销毁不占资源)
        if (isMaxStep) {
            NeonParticleOverlay(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .align(Alignment.Center),
                fillProgress = (animatedValue.value / 4f).coerceIn(0f, 1f),
                reduceMotion = reduceMotion
            )
        }

        // 3. 拖拽滑块 Thumb 与手势处理
        val thumbPx = with(density) { thumbSize.toPx() }
        val availableWidth = (trackWidthPx - thumbPx).coerceAtLeast(1f)
        val thumbOffset = (animatedValue.value / 4f) * availableWidth

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
            // Thumb 外发光底色
            drawCircle(
                color = if (isMaxStep) Color(0xFFFF2A85).copy(alpha = 0.4f) else Color(0xFF0072FF).copy(alpha = 0.3f),
                radius = size.minDimension / 2f
            )
            // Thumb 核心白色滑块
            drawCircle(
                color = Color.White,
                radius = (size.minDimension / 2f) - 4.dp.toPx()
            )
        }
    }
}

/**
 * 粒子特效叠加层：负责 35-45 个超迷你正方形粒子的横向漂移与 Alpha/Scale 律动
 */
@Composable
private fun NeonParticleOverlay(
    fillProgress: Float,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // 固定 40 个粒子 (符合 35-45 性能要求)
    val particleCount = 40
    val particles = remember {
        val neonColors = listOf(
            Color(0xCC00F2FE), // 半透明青色
            Color(0xCC9B51E0), // 半透明紫罗兰
            Color(0xCCFF2A85)  // 半透明淡洋红
        )
        List(particleCount) {
            val sizeDp = Random.nextFloat() * 2f + 2f // 2dp - 4dp
            SquareParticle(
                normalizedX = Random.nextFloat(),
                normalizedY = Random.nextFloat(),
                sizePx = with(density) { sizeDp.dp.toPx() },
                color = neonColors[Random.nextInt(neonColors.size)],
                speed = Random.nextFloat() * 0.15f + 0.05f, // 飘移速度
                phase = Random.nextFloat() * 2f * Math.PI.toFloat(),
                pulseFrequency = Random.nextFloat() * 2f + 1f
            )
        }
    }

    // Nanos 帧脉冲，驱使粒子循环运动
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

        // 裁剪路径：确保粒子严格被限制在圆角轨道填充区内
        val clipPath = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(0f, 0f, activeWidth, trackHeight),
                    CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            )
        }

        val seconds = frameTimeNanos / 1_000_000_000f

        clipPath(clipPath) {
            particles.forEach { p ->
                // 1. 横向位置更新 (从左到右漂移)
                if (!reduceMotion && frameTimeNanos > 0L) {
                    p.normalizedX += p.speed * 0.016f // 简易 delta 步进
                    if (p.normalizedX > 1.0f) {
                        p.normalizedX = 0.0f // 循环回归左边
                    }
                }

                // 计算粒子实际坐标 (横向基于 activeWidth 缩放)
                val realX = p.normalizedX * activeWidth
                val realY = p.normalizedY * trackHeight

                // 2. 周期律动 (0.4~1.0 缩放, 0.2~0.7 淡入淡出)
                val pulse = sin(seconds * p.pulseFrequency + p.phase)
                val scale = 0.7f + 0.3f * pulse        // 0.4 ~ 1.0
                val alpha = (0.45f + 0.25f * pulse).coerceIn(0.2f, 0.7f) // 0.2 ~ 0.7

                val currentSize = p.sizePx * scale

                // 3. 绘制超迷你**正方形**粒子
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(realX - currentSize / 2f, realY - currentSize / 2f),
                    size = Size(currentSize, currentSize)
                )
            }
        }
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
