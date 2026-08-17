package com.example.ui_xiahong

import androidx.compose.ui.graphics.Color

/**
 * 流光强度档位：作为速度/密度/液态位移的统一倍率。
 * - [CALM]：0.5×，最克制；
 * - [MEDIUM]：1.0×，默认；
 * - [ULTRA]：2.0×，最强烈。
 */
enum class NeonIntensity(val multiplier: Float) {
    CALM(0.5f),
    MEDIUM(1.0f),
    ULTRA(2.0f)
}

/**
 * 霓虹流光库的总配置。所有颜色、密度、速度、开关均由此类参数化控制。
 *
 * 提示：默认开启 [enableLiquid]（AGSL 液态扭曲）会带来轻微流动质感，但也可能显得「糊」。
 * 若需要**清晰、锐利**的观感（如演示/控件叠加），建议显式传
 * `NeonConfig(enableLiquid = false)`，或直接使用更克制的 [NeonRhythmGrid] 律动方块层。
 */
data class NeonConfig(
    val primaryColor: Color = Color(0xFF00E5FF),   // 霓虹主色（冷霓虹青蓝，未来感）
    val arcColor: Color = Color(0xFFFFFFFF),       // 电流/电花颜色（霓虹白荧光）
    val particleCount: Int = 400,
    val particleSpeed: Float = 1.5f,
    val enableLiquid: Boolean = true,
    val enableParticles: Boolean = true,
    val enableArc: Boolean = true,
    val liquidIntensity: Float = 0.35f,            // 适度降糊，保留轻微流动质感
    // ── 新阶段特效开关（纯 Canvas，API<33 也生效）──
    val enableBokeh: Boolean = true,               // 景深光斑
    val enableSweep: Boolean = true,               // 流光扫光
    val enableBorder: Boolean = true,              // 霓虹边框 + 暗角
    val enableSparks: Boolean = true,              // 霓虹白荧光电花（替代背景贯穿电线）
    val enableMiniBlocks: Boolean = true,          // 背景网格呼吸方阵（律动迷你小方块）
    val miniBlockColor: Color = Color(0xFF00E5FF), // 小方块颜色（默认主色衍生）
    val bokehColor: Color = Color(0xFF00B3FF),     // 光斑颜色（冷霓虹衍生）
    val sweepColor: Color = Color.White,           // 扫光颜色
    val borderColor: Color = Color(0xFF00E5FF)     // 边框颜色（冷霓虹）
) {
    companion object {
        val Default = NeonConfig()
    }
}
