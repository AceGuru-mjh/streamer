package com.example.ui_xiahong.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 律动小方块引擎：满屏按网格排布的小方块，亮度/大小随 `sin(time * pulseFreq + seed)` 呼吸。
 * 纯 Canvas 绘制，不触发 Compose 重组；方块始终**锐利无模糊**。
 *
 * 视觉参数集中在构造期，便于 NeonFlow（背景氛围）与 NeonRhythmGrid（清晰装饰）复用同一引擎、
 * 却呈现不同气质。默认值偏向「低调背景氛围」，NeonRhythmGrid 会传入更鲜明的参数。
 *
 * @param cols 列数
 * @param rows 行数
 * @param gapFraction 单元格内留白比例 [0,1)：方块边长 = 单元格短边 * (1 - gapFraction)，越大越疏朗
 * @param cornerRadiusFraction 圆角占方块边长的比例 [0,1]，0=纯方块、1=圆形
 * @param minAlpha 呼吸最低透明度
 * @param maxAlpha 呼吸最高透明度
 * @param minScale 呼吸最小缩放（0=完全消失，制造闪烁感）
 * @param maxScale 呼吸最大缩放
 */
internal class MiniBlockEngine(
    private val cols: Int = 18,
    private val rows: Int = 30,
    private val gapFraction: Float = 0.35f,
    private val cornerRadiusFraction: Float = 0.25f,
    private val minAlpha: Float = 0.05f,
    private val maxAlpha: Float = 0.30f,
    private val minScale: Float = 0.35f,
    private val maxScale: Float = 1.0f
) {
    // 每个方块带独立相位与频率，避免整齐划一的呆板感
    private val blocks = List(cols * rows) { idx ->
        Block(
            col = idx % cols,
            row = idx / cols,
            seed = Random.nextFloat() * 6.2832f,
            pulseFreq = Random.nextFloat() * 1.4f + 0.6f
        )
    }

    fun draw(drawScope: DrawScope, color: Color, time: Float) {
        val w = drawScope.size.width
        val h = drawScope.size.height
        if (w <= 0f || h <= 0f) return

        val cellW = w / cols
        val cellH = h / rows
        val baseSize = min(cellW, cellH) * (1f - gapFraction)

        blocks.forEach { b ->
            val cx = (b.col + 0.5f) * cellW
            val cy = (b.row + 0.5f) * cellH

            // 归一化呼吸量 [0,1]
            val pulse = (sin(time * b.pulseFreq + b.seed) + 1f) * 0.5f
            val scale = minScale + (maxScale - minScale) * pulse
            val alpha = (minAlpha + (maxAlpha - minAlpha) * pulse).coerceIn(0f, 1f)
            val size = baseSize * scale
            if (size <= 0.5f) return@forEach

            drawScope.drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(cx - size / 2f, cy - size / 2f),
                size = Size(size, size),
                cornerRadius = CornerRadius(size * cornerRadiusFraction, size * cornerRadiusFraction)
            )
        }
    }

    private data class Block(val col: Int, val row: Int, val seed: Float, val pulseFreq: Float)
}
