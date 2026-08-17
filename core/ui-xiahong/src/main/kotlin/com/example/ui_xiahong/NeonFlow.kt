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
import com.example.ui_xiahong.internal.MiniBlockEngine
import com.example.ui_xiahong.internal.ParticleEngine
import com.example.ui_xiahong.internal.drawBokeh
import com.example.ui_xiahong.internal.drawBorder
import com.example.ui_xiahong.internal.drawSweep

// 只有 API 33+ 支持 AGSL
private const val SHADER_SRC = """
    uniform shader content;
    uniform float time;
    uniform float intensity;
    vec4 main(vec2 fragCoord) {
        float speed = time * 1.5;
        vec2 uv = fragCoord;
        float dispX = sin(uv.y * 0.02 + speed) * 6.0 * intensity;
        float dispY = cos(uv.x * 0.02 + speed * 0.8) * 4.0 * intensity;
        return content.eval(fragCoord + vec2(dispX, dispY));
    }
"""

/**
 * 霓虹流光对外唯一入口：用特效层包裹任意子内容。
 *
 * 视觉由多层特效协同构成：背景光斑 → 律动方块([NeonConfig.enableMiniBlocks]) →
 * 发光粒子奔流 → 霓虹白荧光电花/电流([NeonConfig.enableArc] + [NeonConfig.enableSparks]) →
 * 流光扫光 → 霓虹边框+暗角，最上层叠加 [content]。
 * 液态扭曲([NeonConfig.enableLiquid])为可选层，关闭即为锐利清晰观感。
 *
 * 所有层均受 [NeonConfig] 开关控制，可自由组合。若只需要克制、清晰的律动纹理，
 * 可直接用更轻量的 [NeonRhythmGrid]，不必引入整层流光。
 *
 * @param modifier 外层 Box 修饰符
 * @param config 特效配置（颜色/开关/强度）
 * @param intensity 流光强度档位（速度/密度/位移倍率）
 * @param content 被特效包裹的子内容（不参与液态扭曲，保持清晰）
 */
@Composable
fun NeonFlow(
    modifier: Modifier = Modifier,
    config: NeonConfig = NeonConfig.Default,
    intensity: NeonIntensity = NeonIntensity.MEDIUM,
    content: @Composable () -> Unit
) {
    // 1. 初始化引擎（remember 保证状态在重组中保留）
    val particleEngine = remember(config.particleCount) {
        ParticleEngine(config.particleCount)
    }
    val arcRenderer = remember { ArcRenderer() }
    val miniBlockEngine = remember { MiniBlockEngine() }

    // 2. 动画时间轴：用 InfiniteTransition 在绘制阶段驱动，不触发 Composable 重组
    //    time 仅被 graphicsLayer / Canvas 的绘制回调读取（draw 阶段），消费量不会重组本函数。
    val transition = rememberInfiniteTransition(label = "neon-time")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1_000_000f, // 足够大，等效持续递增（约 1 单位/秒，避免重启跳变）
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon-time"
    )
    // 新阶段特效的相位（独立周期，均由同一 InfiniteTransition 驱动，零重组）
    val sweepPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon-sweep"
    )
    val bokehPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon-bokeh"
    )
    val borderPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon-border"
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
        // 背景特效层：景深光斑（最底，营造纵深）
        if (config.enableBokeh) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawBokeh(config.bokehColor, bokehPhase, intensity.multiplier)
            }
        }

        // 背景律动方阵：网格呼吸迷你小方块（赛博数据矩阵氛围）
        if (config.enableMiniBlocks) {
            Canvas(modifier = Modifier.matchParentSize()) {
                miniBlockEngine.draw(this, config.miniBlockColor, time)
            }
        }

        // 中层特效：粒子奔流
        Canvas(modifier = Modifier.matchParentSize()) {
            if (config.enableParticles) {
                particleEngine.draw(this, config.primaryColor, config.particleSpeed * intensity.multiplier)
            }
            if (config.enableArc) {
                arcRenderer.draw(this, config.arcColor, time, config.enableSparks)
            }
        }

        // 用户内容层
        content()

        // 叠加特效层：流光扫光 + 霓虹边框（最上，覆盖在内容之上）
        if (config.enableSweep) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawSweep(config.sweepColor, sweepPhase, intensity.multiplier)
            }
        }
        if (config.enableBorder) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawBorder(config.borderColor, borderPulse, intensity.multiplier)
            }
        }
    }
}
