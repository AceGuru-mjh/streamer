package com.example.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui_xiahong.ElectricSliderStep
import com.example.ui_xiahong.NeonConfig
import com.example.ui_xiahong.NeonElectricDiscreteSlider
import com.example.ui_xiahong.NeonFlow
import com.example.ui_xiahong.NeonIntensity

/** 赛博深空背景色。 */
private val CyberBg = Color(0xFF05060A)

/** UltraCode 专属电光青。 */
private val UltraCyan = Color(0xFF00FFFF)

/**
 * 档位 ↔ 全局特效强度映射。
 * 复用库 [NeonFlow] 内部既有渲染器（ParticleEngine / ArcRenderer / AGSL 液态），
 * 仅通过 [NeonConfig] 开关与数值调节强度，绝不重写特效。
 */
private fun stepToConfig(step: ElectricSliderStep): NeonConfig = when (step) {
    ElectricSliderStep.Low -> NeonConfig( // 仅微弱霓虹边框，关闭电弧/粒子/扭曲
        enableLiquid = false,
        enableParticles = false,
        enableArc = false,
        enableBokeh = false,
        enableSweep = false,
        enableBorder = true,
        enableSparks = false,
        enableMiniBlocks = false,
        borderColor = Color(0xFF2A3550),
        liquidIntensity = 0f,
        particleCount = 0,
        particleSpeed = 0f
    )
    ElectricSliderStep.Medium -> NeonConfig( // 少量粒子，无电弧/电花，画面简洁
        enableLiquid = false,
        enableParticles = true,
        enableArc = false,
        enableBokeh = false,
        enableSweep = true,
        enableBorder = true,
        enableSparks = false,
        enableMiniBlocks = false,
        primaryColor = Color(0xFF00C6FF),
        borderColor = Color(0xFF00C6FF),
        particleCount = 120,
        particleSpeed = 0.8f,
        liquidIntensity = 0f
    )
    ElectricSliderStep.High -> NeonConfig( // 低速粒子 + 律动方阵 + 白荧光电花，轻度霓虹发光
        enableLiquid = false,
        enableParticles = true,
        enableArc = true,
        enableBokeh = false,
        enableSweep = true,
        enableBorder = true,
        primaryColor = Color(0xFF00E5FF),
        arcColor = Color(0xFFFFFFFF),
        borderColor = Color(0xFF00E5FF),
        particleCount = 220,
        particleSpeed = 1.0f,
        liquidIntensity = 0.15f
    )
    ElectricSliderStep.XHigh -> NeonConfig( // 粒子增多 + 电流电弧 + 紫青未来感
        enableLiquid = false,
        enableParticles = true,
        enableArc = true,
        enableBokeh = true,
        enableSweep = true,
        enableBorder = true,
        primaryColor = Color(0xFF9B51E0),
        arcColor = Color(0xFF00F3FF),
        borderColor = Color(0xFF9B51E0),
        particleCount = 360,
        particleSpeed = 1.3f,
        liquidIntensity = 0.3f
    )
    ElectricSliderStep.Max -> NeonConfig( // 全部特效中等强度，霓虹品红 + 白电花
        enableLiquid = true,
        enableParticles = true,
        enableArc = true,
        enableBokeh = true,
        enableSweep = true,
        enableBorder = true,
        primaryColor = Color(0xFFB14BFF),
        arcColor = Color(0xFFFFFFFF),
        borderColor = Color(0xFFB14BFF),
        particleCount = 400,
        particleSpeed = 1.5f,
        liquidIntensity = 0.4f
    )
    ElectricSliderStep.UltraCode -> NeonConfig( // 专属高能模式：电光青 + 翻倍 + 高强度液态
        enableLiquid = true,
        enableParticles = true,
        enableArc = true,
        enableBokeh = true,
        enableSweep = true,
        enableBorder = true,
        primaryColor = UltraCyan,
        arcColor = UltraCyan,
        borderColor = UltraCyan,
        bokehColor = UltraCyan,
        sweepColor = UltraCyan,
        particleCount = 800, // 粒子数量翻倍
        particleSpeed = 3.0f, // 流动速度翻倍
        liquidIntensity = 0.7f // 高强度 AGSL 液态扭曲（适度降糊）
    )
}

/**
 * Model Intelligence Effort Tuner —— 极简赛博朋克档位调谐器。
 *
 * - 深色赛博背景 + 标题「Model Intelligence Effort Tuner」+ 中间离散霓虹滑块 + 下方实时档位名。
 * - 滑块为库组件 [NeonElectricDiscreteSlider]；背景特效为库 [NeonFlow]（复用 ParticleEngine/ArcRenderer/液态）。
 * - [NeonElectricDiscreteSlider.onValueChange] 实时把当前档位映射为全局 [NeonConfig] 并驱动背景强度。
 * - 标题/档位标签置于 [NeonFlow] 前景层，不参与液态扭曲，保持清晰可读。
 */
@Composable
fun ModelIntelligenceEffortTuner() {
    // 当前档位 与 全局特效配置（onValueChange 实时更新）
    var step by remember { mutableStateOf(ElectricSliderStep.High) }
    var config by remember { mutableStateOf(stepToConfig(step)) }

    val isUltra = step == ElectricSliderStep.UltraCode
    val accent = if (isUltra) UltraCyan else Color.White

    Box(modifier = Modifier.fillMaxSize().background(CyberBg)) {
        // 背景特效层：复用库 NeonFlow（内部即 ParticleEngine + ArcRenderer + AGSL 液态）
        NeonFlow(modifier = Modifier.fillMaxSize(), config = config, intensity = NeonIntensity.MEDIUM) {
            // 内容层留空：液态扭曲只作用于背景特效层，前景滑块/文字保持清晰
        }

        // 前景层（不参与液态扭曲）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 44.dp)
        ) {
            Text(
                text = "Model Intelligence Effort Tuner",
                color = accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )

            // 中间区域：滑块 + 实时档位名（垂直居中）
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeonElectricDiscreteSlider(
                    value = step,
                    onValueChange = { ns ->
                        step = ns
                        config = stepToConfig(ns) // 实时更新全局特效配置
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "CURRENT MODE",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    letterSpacing = 3.sp
                )
                Text(
                    text = step.label,
                    color = accent,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
private fun ModelIntelligenceEffortTunerPreview() {
    MaterialTheme {
        ModelIntelligenceEffortTuner()
    }
}
