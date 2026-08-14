package com.example.ui_xiahong

import androidx.compose.ui.graphics.Color

enum class XiaHongIntensity(val multiplier: Float) {
    CALM(0.5f),
    MEDIUM(1.0f),
    ULTRA(2.0f)
}

data class XiaHongConfig(
    val primaryColor: Color = Color(0xFFBC8F8F),   // 霞红主色
    val arcColor: Color = Color(0xFFFF00FF),       // 电流/电弧颜色
    val particleCount: Int = 400,
    val particleSpeed: Float = 1.5f,
    val enableLiquid: Boolean = true,
    val enableParticles: Boolean = true,
    val enableArc: Boolean = true,
    val liquidIntensity: Float = 0.6f,
    // ── 新阶段特效开关（纯 Canvas，API<33 也生效）──
    val enableBokeh: Boolean = true,               // 景深光斑
    val enableSweep: Boolean = true,               // 流光扫光
    val enableBorder: Boolean = true,              // 霓虹边框 + 暗角
    val bokehColor: Color = Color(0xFFFF8FA3),     // 光斑颜色（默认霞红衍生）
    val sweepColor: Color = Color.White,           // 扫光颜色
    val borderColor: Color = Color(0xFFFF2D55)     // 边框颜色（默认霞红）
) {
    companion object {
        val Default = XiaHongConfig()
    }
}
