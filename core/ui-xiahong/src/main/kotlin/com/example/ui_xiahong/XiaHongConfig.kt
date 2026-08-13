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
    val liquidIntensity: Float = 0.6f
) {
    companion object {
        val Default = XiaHongConfig()
    }
}
