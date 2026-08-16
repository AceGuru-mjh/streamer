package com.example.ui_xiahong.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 背景「网格呼吸方阵」——满屏律动迷你小方块。
 *
 * 按网格排布的小方块，亮度/大小随 `sin(time * pulseFreq + seed)` 呼吸起伏，
 * 形成类似数据矩阵的赛博感。纯 Canvas 绘制，不触发 Composable 重组。
 * 整体 alpha 偏低，作为背景氛围层，不抢前景内容。
 */
internal class MiniBlockEngine(private val cols: Int = 18, private val rows: Int = 30) {
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
        val baseSize = min(cellW, cellH) * 0.42f

        blocks.forEach { b ->
            val cx = (b.col + 0.5f) * cellW
            val cy = (b.row + 0.5f) * cellH

            // 归一化呼吸量 [0,1]
            val pulse = (sin(time * b.pulseFreq + b.seed) + 1f) * 0.5f
            val scale = 0.35f + 0.65f * pulse
            val alpha = (0.05f + 0.20f * pulse).coerceIn(0f, 0.32f)
            val size = baseSize * scale
            if (size <= 0f) return@forEach

            drawScope.drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(cx - size / 2f, cy - size / 2f),
                size = Size(size, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.25f, size * 0.25f)
            )
        }
    }

    private data class Block(val col: Int, val row: Int, val seed: Float, val pulseFreq: Float)
}
