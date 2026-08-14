package com.example.ui_xiahong.internal

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// 内部粒子模型
internal class Particle(
    var x: Float,
    var y: Float,
    var speed: Float,
    var size: Float,
    var alpha: Float
)

// 高性能粒子流系统
internal class ParticleEngine(count: Int) {
    private val particles = List(count) { createParticle(true) }

    private fun createParticle(randomX: Boolean = false): Particle {
        return Particle(
            x = if (randomX) Random.nextFloat() * 2000f else -50f,
            y = Random.nextFloat() * 1500f,
            speed = Random.nextFloat() * 8f + 2f,
            size = Random.nextFloat() * 4f + 2f,
            alpha = Random.nextFloat() * 0.5f + 0.2f
        )
    }

    // 作为成员函数（可访问 private particles）；发光通过原生 Paint + BlurMaskFilter 实现，
    // 依赖 API 29+ 硬件加速 Canvas（本库 minSdk 33 满足）。
    fun draw(drawScope: DrawScope, color: Color, speedMultiplier: Float) {
        val canvas = drawScope.drawContext.canvas.nativeCanvas
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 具有发光效果的原生 Paint（BlurMaskFilter 在硬件加速下生效）
        val glowPaint = Paint().apply {
            this.color = color.toArgb()
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        }

        particles.forEach { p ->
            p.x += p.speed * speedMultiplier
            if (p.x > width + 50f) {
                p.x = -50f
                p.y = Random.nextFloat() * height
            }

            // 带发光的粒子主体
            canvas.drawCircle(p.x, p.y, p.size, glowPaint)

            // 更亮的核心，增加层次感
            drawScope.drawCircle(
                color = Color.White.copy(alpha = p.alpha * 0.8f),
                radius = p.size * 0.4f,
                center = Offset(p.x, p.y)
            )
        }
    }
}

// 动态电弧：三层叠加 + 分段随机抖动 + 流动电光(PathMeasure) + 闪烁 + 随机分叉
internal class ArcRenderer {
    private val path = Path()
    private val segPath = Path()
    private val measure = PathMeasure()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun draw(drawScope: DrawScope, color: Color, time: Float) {
        val canvas = drawScope.drawContext.canvas.nativeCanvas
        val w = drawScope.size.width
        val h = drawScope.size.height

        // 全局闪烁：偶尔压暗，模拟电流的不稳定感
        val flicker = if (Random.nextFloat() < 0.12f) 0.35f else 1f

        // 三层基础电弧（叠加能量感）：粗而暗、细而亮
        drawElectricLayer(canvas, color.copy(alpha = 0.18f * flicker), time, h * 0.5f, w, 12f)
        drawElectricLayer(canvas, color.copy(alpha = 0.45f * flicker), time * 1.2f, h * 0.45f, w, 5f)
        drawElectricLayer(canvas, Color.White.copy(alpha = 0.30f * flicker), time * 1.5f, h * 0.55f, w, 2.5f)

        // 沿路径流动的高亮电光段
        drawFlowBolt(canvas, Color.White, time, h * 0.5f, w)

        // 随机分叉，增强“电流散射”意象
        if (Random.nextFloat() < 0.5f) {
            drawBranch(canvas, color.copy(alpha = 0.6f * flicker), time, h * 0.5f, w)
        }
    }

    // 构建一条带分段随机抖动的锯齿路径（复用内部 path 实例）
    private fun buildJitterPath(baseY: Float, time: Float, w: Float): Path {
        path.reset()
        path.moveTo(0f, baseY)
        val segments = 10
        for (i in 1..segments) {
            val x = (w / segments) * i
            val jitter = if (i < segments) (Random.nextFloat() - 0.5f) * 20f else 0f
            val y = baseY + sin(time + i) * 80f + jitter
            path.lineTo(x, y)
        }
        return path
    }

    private fun drawElectricLayer(
        canvas: Canvas,
        color: Color,
        time: Float,
        baseY: Float,
        w: Float,
        strokeWidth: Float
    ) {
        val p = buildJitterPath(baseY, time, w)
        paint.color = color.toArgb()
        paint.strokeWidth = strokeWidth
        paint.maskFilter = null
        canvas.drawPath(p, paint)
    }

    // 用 PathMeasure 在整条路径上截取一段“移动的高亮电光”，实现流动感
    private fun drawFlowBolt(canvas: Canvas, color: Color, time: Float, baseY: Float, w: Float) {
        val p = buildJitterPath(baseY, time, w)
        measure.setPath(p, false)
        val len = measure.length
        if (len <= 0f) return

        val segLen = len * 0.18f
        val head = (time * len * 0.6f) % len
        val end = head + segLen

        segPath.reset()
        if (end <= len) {
            measure.getSegment(head, end, segPath, true)
        } else {
            // 跨越末端时环绕到起点，保证电光连续
            measure.getSegment(head, len, segPath, true)
            val tail = Path()
            measure.getSegment(0f, end - len, tail, true)
            segPath.addPath(tail)
        }

        paint.color = color.toArgb()
        paint.strokeWidth = 3f
        paint.maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(segPath, paint)
        paint.maskFilter = null
    }

    // 从主路径上某点甩出一条随机短叉，模拟电弧分枝
    private fun drawBranch(canvas: Canvas, color: Color, time: Float, baseY: Float, w: Float) {
        val bx = Random.nextFloat() * w
        val by = baseY + sin(time + (bx / w) * 10f) * 80f
        val branch = Path()
        branch.moveTo(bx, by)
        branch.lineTo(
            bx + (Random.nextFloat() - 0.5f) * 120f,
            by + (Random.nextFloat() - 0.2f) * 160f
        )
        paint.color = color.toArgb()
        paint.strokeWidth = 2f
        paint.maskFilter = null
        canvas.drawPath(branch, paint)
    }
}
