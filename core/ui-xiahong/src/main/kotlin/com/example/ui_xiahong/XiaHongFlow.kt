package com.example.ui_xiahong

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui_xiahong.internal.ArcRenderer
import com.example.ui_xiahong.internal.ParticleEngine

// 只有 API 33+ 支持 AGSL
private const val SHADER_SRC = """
    uniform shader content;
    uniform float time;
    uniform float intensity;
    vec4 main(vec2 fragCoord) {
        float speed = time * 1.5;
        vec2 uv = fragCoord;
        float dispX = sin(uv.y * 0.02 + speed) * 15.0 * intensity;
        float dispY = cos(uv.x * 0.02 + speed * 0.8) * 10.0 * intensity;
        return content.eval(fragCoord + vec2(dispX, dispY));
    }
"""

@Composable
fun XiaHongFlow(
    modifier: Modifier = Modifier,
    config: XiaHongConfig = XiaHongConfig.Default,
    intensity: XiaHongIntensity = XiaHongIntensity.MEDIUM,
    content: @Composable () -> Unit
) {
    // 1. 初始化引擎（remember 保证状态在重组中保留）
    val particleEngine = remember(config.particleCount) {
        ParticleEngine(config.particleCount)
    }
    val arcRenderer = remember { ArcRenderer() }

    // 2. 动画时间轴：用 InfiniteTransition 在绘制阶段驱动，不触发 Composable 重组
    //    time 仅被 graphicsLayer / Canvas 的绘制回调读取（draw 阶段），消费量不会重组本函数。
    val transition = rememberInfiniteTransition(label = "xiahong-time")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1_000_000f, // 足够大，等效持续递增（约 1 单位/秒，避免重启跳变）
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "xiahong-time"
    )

    // 3. AGSL Shader 处理（API 33+ 可用，否则为 null 自动降级）
    val shader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember { RuntimeShader(SHADER_SRC) }
    } else null
    val liquidRenderEffect = if (shader != null) {
        remember(shader) {
            RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        }
    } else null

    Box(
        modifier = modifier
            .graphicsLayer {
                if (config.enableLiquid && liquidRenderEffect != null && shader != null) {
                    shader.setFloatUniform("time", time)
                    shader.setFloatUniform("intensity", config.liquidIntensity * intensity.multiplier)
                    renderEffect = liquidRenderEffect
                }
            }
    ) {
        // 背景特效层
        Canvas(modifier = Modifier.matchParentSize()) {
            if (config.enableParticles) {
                particleEngine.draw(this, config.primaryColor, config.particleSpeed * intensity.multiplier)
            }
            if (config.enableArc) {
                arcRenderer.draw(this, config.arcColor, time)
            }
        }

        // 用户内容层
        content()
    }
}
