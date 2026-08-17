package com.example.ui_xiahong

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui_xiahong.internal.MiniBlockEngine

/**
 * 清晰的律动方块装饰层（库对外公开 API）。
 *
 * 满屏小方块按 `sin(time * pulseFreq + seed)` 呼吸起伏，形成赛博数据矩阵氛围。
 * 方块永远**锐利、无模糊**，不参与任何液态/模糊特效——可直接作为任意内容的底层
 * 背景，或在 [content] 中叠加快照/控件（如 [NeonElectricDiscreteSlider]）。
 *
 * 这是「霓虹」库的核心装饰组件：相比 [NeonFlow] 的流动/发光特效，它更克制、更清晰，
 * 适合作为不抢内容的底层律动纹理。
 *
 * @param modifier 外层 Box 修饰符
 * @param color 方块颜色（默认冷霓虹青）
 * @param columns 网格列数（越多越细密）
 * @param rows 网格行数
 * @param gapFraction 单元格留白比例 [0,1)，越大方块越疏朗分离；默认 0.4
 * @param cornerRadiusFraction 方块圆角比例 [0,1]，默认 0.3（轻微圆角霓虹瓷砖）
 * @param minAlpha 呼吸最低透明度，默认 0.06
 * @param maxAlpha 呼吸最高透明度，默认 0.34
 * @param speed 呼吸速度倍率，默认 1f
 * @param content 叠加在方块之上的内容（如滑块、文字）
 */
@Composable
fun NeonRhythmGrid(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00E5FF),
    columns: Int = 18,
    rows: Int = 30,
    gapFraction: Float = 0.4f,
    cornerRadiusFraction: Float = 0.3f,
    minAlpha: Float = 0.06f,
    maxAlpha: Float = 0.34f,
    speed: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val engine = remember(columns, rows, gapFraction, cornerRadiusFraction, minAlpha, maxAlpha) {
        MiniBlockEngine(
            cols = columns,
            rows = rows,
            gapFraction = gapFraction,
            cornerRadiusFraction = cornerRadiusFraction,
            minAlpha = minAlpha,
            maxAlpha = maxAlpha
        )
    }

    // 持续递增的时间轴（与 NeonFlow 同源，零重组驱动绘制）
    val transition = rememberInfiniteTransition(label = "rhythm-time")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1_000_000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rhythm-time"
    )

    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            engine.draw(this, color, time * speed)
        }
        content()
    }
}
