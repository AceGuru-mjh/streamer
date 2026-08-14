package com.example.ui_xiahong.internal

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * 库内特效层（均为纯 Canvas 绘制，与 API 版本无关，<33 也正常显示）：
 * - 景深光斑 Bokeh：缓慢漂移的柔光圆斑，增强空间纵深感；
 * - 流光扫光 Sweep：一道斜向高光带横向扫过，呼应「流光」主题；
 * - 霓虹边框 Border：发光圆角描边 + 中心向四周的暗角晕影。
 * 全部由 time/phase 驱动，在 Canvas 绘制回调中读取，不触发 Composable 重组。
 *
 * 注意：参数命名为 tint，避免与 android.graphics.Paint.color 属性产生命名歧义。
 */

// 景深光斑：若干柔光圆斑缓慢横向漂移 + 轻微纵向呼吸
internal fun DrawScope.drawBokeh(tint: Color, phase: Float, intensity: Float) {
    val w = size.width
    val h = size.height
    val minSide = minOf(w, h)
    val native = drawContext.canvas.nativeCanvas

    val glow = Paint().apply {
        color = tint.toArgb()
        isAntiAlias = true
        alpha = (120 * intensity.coerceIn(0f, 1.5f)).toInt().coerceIn(0, 255)
        maskFilter = BlurMaskFilter(minSide * 0.05f, BlurMaskFilter.Blur.NORMAL)
    }

    val orbs = 6
    val span = w + minSide * 0.2f
    for (i in 0 until orbs) {
        val seed = i * 1.37f
        val radius = minSide * 0.05f * (0.6f + 0.4f * ((i % 3) / 2f))
        val y = h * ((i + 0.5f) / orbs) + sin((phase + seed) * 2 * Math.PI.toFloat()) * h * 0.05f
        val pos = (phase * 0.25f + seed) % 1f
        val x = pos * span - radius
        native.drawCircle(x, y, radius, glow)
    }
}

// 流光扫光：斜向高光带横向扫过（phase ∈ [0,1) 循环）
internal fun DrawScope.drawSweep(tint: Color, phase: Float, intensity: Float) {
    val w = size.width
    val h = size.height
    val band = w * 0.16f
    val span = w + 2 * band
    val x = (phase % 1f) * span - band
    val peak = (0.10f * intensity.coerceIn(0f, 1.5f)).coerceIn(0f, 0.35f)

    rotate(18f, pivot = center) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, tint.copy(alpha = peak), Color.Transparent),
                startX = 0f,
                endX = band
            ),
            topLeft = Offset(x, -h),
            size = Size(band, h * 3f)
        )
    }
}

// 霓虹边框 + 暗角晕影（pulse ∈ [0,1] 控制呼吸辉光强度）
internal fun DrawScope.drawBorder(tint: Color, pulse: Float, intensity: Float) {
    val w = size.width
    val h = size.height
    val pad = 6.dp.toPx()
    val r = 22.dp.toPx()
    val strokeW = 2.5.dp.toPx()
    val native = drawContext.canvas.nativeCanvas

    val breathe = (0.5f + 0.5f * sin(pulse * 2 * Math.PI.toFloat()))
    val glowScale = (0.6f + 0.6f * breathe) * intensity.coerceIn(0f, 1.5f)

    // 外层发光描边
    val glow = Paint().apply {
        color = tint.toArgb()
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = strokeW
        maskFilter = BlurMaskFilter(strokeW * 2.2f * glowScale, BlurMaskFilter.Blur.NORMAL)
    }
    native.drawRoundRect(pad, pad, w - pad, h - pad, r, r, glow)

    // 内层清晰描边
    val crisp = Paint().apply {
        color = tint.copy(alpha = 0.85f).toArgb()
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = strokeW * 0.6f
    }
    native.drawRoundRect(pad, pad, w - pad, h - pad, r, r, crisp)

    // 暗角晕影（中心透明 → 四周压暗）
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
            center = center,
            radius = maxOf(w, h) * 0.62f
        ),
        size = size
    )
}
