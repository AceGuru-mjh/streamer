package com.example.ui_xiahong.internal

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
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

// 霓虹白荧光电花层：替代原贯穿背景的「三条线」横向电流。
// 满屏稀疏散布的短命白荧光电花（强辉光核心 + 偶发十字电芒），呈现未来感电花意象。
internal class ArcRenderer {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // 电花粒子池（短命、随机重生）
    private data class Spark(
        var x: Float,
        var y: Float,
        var life: Float,   // 剩余寿命比例 [0,1]
        var maxLife: Float,
        var size: Float
    )

    private val sparks = mutableListOf<Spark>()
    private var lastTime = 0f

    // 白荧光核心（实心）
    private val corePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    // 白荧光辉光（强模糊）
    private val glowPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }
    // 十字电芒（细线）
    private val rayPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 1f
        strokeCap = Paint.Cap.ROUND
    }

    fun draw(drawScope: DrawScope, color: Color, time: Float, enableSparks: Boolean = true) {
        val canvas = drawScope.drawContext.canvas.nativeCanvas
        val w = drawScope.size.width
        val h = drawScope.size.height

        // 全局闪烁：偶尔压暗，模拟电流的不稳定感
        val flicker = if (Random.nextFloat() < 0.12f) 0.4f else 1f

        // 霓虹白荧光电花（替代原 3 条贯穿电线）
        if (enableSparks) {
            drawSparks(canvas, time, w, h)
        }

        // 偶发短分叉电枝，强化「电流散射」意象（非贯穿全宽的长直线）
        if (Random.nextFloat() < 0.5f) {
            drawBranch(canvas, color.copy(alpha = 0.55f * flicker), time, h * 0.5f, w)
        }
    }

    private fun drawSparks(canvas: Canvas, time: Float, w: Float, h: Float) {
        // 帧间隔（秒），上限避免卡顿后大跳变
        val dt = (time - lastTime).coerceIn(0f, 0.05f)
        lastTime = time

        // 按面积维持稀疏目标数量，避免铺满整屏
        val target = ((w * h) / 60000f).toInt().coerceIn(6, 36)
        while (sparks.size < target) {
            sparks.add(newSpark(w, h))
        }

        val iter = sparks.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            s.life -= dt / s.maxLife
            if (s.life <= 0f) {
                // 重生到新位置，保持持续闪烁
                s.x = Random.nextFloat() * w
                s.y = Random.nextFloat() * h
                s.life = 1f
                s.maxLife = Random.nextFloat() * 0.6f + 0.4f
                s.size = Random.nextFloat() * 3f + 2f
            }
            val b = s.life.coerceIn(0f, 1f)

            // 强辉光外圈（霓虹白荧光）
            glowPaint.color = Color.White.copy(alpha = b * 0.85f).toArgb()
            canvas.drawCircle(s.x, s.y, s.size * 1.7f, glowPaint)

            // 实心白核心
            corePaint.color = Color.White.copy(alpha = b).toArgb()
            canvas.drawCircle(s.x, s.y, s.size * 0.5f, corePaint)

            // 偶发十字电芒，强化「电花」观感
            if (Random.nextFloat() < 0.45f) {
                val arm = s.size * (1.2f + b * 1.8f)
                rayPaint.color = Color.White.copy(alpha = b * 0.6f).toArgb()
                canvas.drawLine(s.x - arm, s.y, s.x + arm, s.y, rayPaint)
                canvas.drawLine(s.x, s.y - arm, s.x, s.y + arm, rayPaint)
            }
        }
    }

    private fun newSpark(w: Float, h: Float) = Spark(
        x = Random.nextFloat() * w,
        y = Random.nextFloat() * h,
        life = 1f,
        maxLife = Random.nextFloat() * 0.6f + 0.4f,
        size = Random.nextFloat() * 3f + 2f
    )

    // 从随机点甩出一条短分叉电枝，模拟电弧分枝
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
