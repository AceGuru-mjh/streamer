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
 * @param color 方块颜色（默认冷霓虹青）
 * @param columns 网格列数
 * @param rows 网格行数
 * @param speed 呼吸速度倍率
 * @param content 叠加在方块之上的内容（如滑块、文字）
 */
@Composable
fun NeonRhythmGrid(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00E5FF),
    columns: Int = 18,
    rows: Int = 30,
    speed: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val engine = remember(columns, rows) { MiniBlockEngine(columns, rows) }

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
