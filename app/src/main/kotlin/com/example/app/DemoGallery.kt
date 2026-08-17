package com.example.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui_xiahong.ElectricSliderStep
import com.example.ui_xiahong.NeonConfig
import com.example.ui_xiahong.NeonElectricDiscreteSlider
import com.example.ui_xiahong.NeonFlow
import com.example.ui_xiahong.NeonIntensity
import com.example.ui_xiahong.NeonRhythmGrid
import kotlinx.coroutines.delay

/** 深空赛博背景色。 */
private val CyberBg = Color(0xFF05060A)

private val NeonCyan = Color(0xFF00E5FF)
private val NeonMagenta = Color(0xFFFF2D9B)
private val NeonViolet = Color(0xFF9B6BFF)
private val NeonMint = Color(0xFF36F1A6)

/** 各演示的底部署名。 */
private val DemoCaptions = mapOf(
    1 to "律动方阵 · Cyan", 2 to "律动方阵 · Violet", 3 to "律动方阵 · Mint",
    4 to "方阵 + 滑块", 5 to "滑块 · 默认", 6 to "滑块 · 电流",
    7 to "滑块 · 放电", 8 to "流光 · 锐利粒子", 9 to "流光 · 边框",
    10 to "流光 · 全开"
)

private data class DemoEntry(
    val id: Int,
    val title: String,
    val subtitle: String,
    val accent: Color
)

/**
 * 演示画廊入口：首页列出 10 个「利用 ui-xiahong 库模块」的演示，点击进入对应演示。
 * 所有演示均**不使用糊的全屏液态背景**，以清晰的律动方块 / 锐利特效 / 滑块为主。
 */
@Composable
fun DemoGallery() {
    var selectedId by remember { mutableStateOf<Int?>(null) }

    Box(Modifier.fillMaxSize().background(CyberBg)) {
        Box(Modifier.fillMaxSize()) {
            when (selectedId) {
                null -> DemoHome(onSelect = { selectedId = it })
                1 -> DemoRhythm(NeonCyan, "律动方阵 · Cyan")
                2 -> DemoRhythm(NeonViolet, "律动方阵 · Violet")
                3 -> DemoRhythm(NeonMint, "律动方阵 · Mint")
                4 -> DemoGridPlusSlider(NeonCyan)
                5 -> DemoSliderOnly(NeonCyan)
                6 -> DemoSliderStep(ElectricSliderStep.High, NeonCyan)
                7 -> DemoSliderStep(ElectricSliderStep.Max, NeonMagenta)
                8 -> DemoFlowSharp(NeonCyan)
                9 -> DemoFlowFrame(NeonCyan)
                10 -> DemoFlowFull(NeonCyan)
                else -> DemoHome(onSelect = { selectedId = it })
            }
        }
        if (selectedId != null) {
            DemoTopBar(
                onBack = { selectedId = null },
                modifier = Modifier.align(Alignment.TopStart)
            )
            DemoCaptions[selectedId]?.let { caption ->
                Text(
                    text = caption,
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp)
                )
            }
        }
    }
}

/** 错落入场：淡入 + 轻微上移，按 index 递增延迟。 */
@Composable
private fun StaggerItem(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(45L * index); shown = true }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(440, easing = LinearEasing))
    val ty by animateFloatAsState(if (shown) 0f else 16f, tween(440, easing = LinearEasing))
    Column(Modifier.graphicsLayer { this.alpha = alpha; translationY = ty }) { content() }
}

@Composable
private fun DemoTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(14.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹ 返回", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TitlePill(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    }
}

@Composable
private fun DemoHome(onSelect: (Int) -> Unit) {
    val items = listOf(
        DemoEntry(1, "律动方阵", "NeonRhythmGrid 满屏锐利呼吸方块 · Cyan", NeonCyan),
        DemoEntry(2, "律动方阵", "NeonRhythmGrid · Violet 主题", NeonViolet),
        DemoEntry(3, "律动方阵", "NeonRhythmGrid · Mint 主题", NeonMint),
        DemoEntry(4, "方阵 + 滑块", "Grid 背景叠加磁吸滑块", NeonCyan),
        DemoEntry(5, "滑块 · 默认", "NeonElectricDiscreteSlider 原样", NeonCyan),
        DemoEntry(6, "滑块 · 电流", "High 档内置青色电弧", NeonCyan),
        DemoEntry(7, "滑块 · 放电", "Max 档品红放电 + 白电花", NeonMagenta),
        DemoEntry(8, "流光 · 锐利粒子", "NeonFlow 液态关闭，仅粒子+电花", NeonCyan),
        DemoEntry(9, "流光 · 边框", "NeonFlow 仅边框 + 扫光", NeonCyan),
        DemoEntry(10, "流光 · 全开", "NeonFlow 全特效（液态关闭=锐利）", NeonCyan)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StaggerItem(0) {
            Column {
                Text("UI PREVIEW", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.height(6.dp))
                Text("霓虹组件演示画廊", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text("10 个利用 ui-xiahong 库模块的清晰特效演示", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(
                            listOf(NeonCyan, NeonCyan.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                )
            }
        }
        items.forEachIndexed { i, item ->
            StaggerItem(i + 1) {
                DemoCard(index = i + 1, item = item, onClick = { onSelect(item.id) })
            }
        }
    }
}

@Composable
private fun DemoCard(index: Int, item: DemoEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(item.accent.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("%02d".format(index), color = item.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(3.dp))
                Text(item.subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Text("›", color = Color.White.copy(alpha = 0.35f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── 10 个演示 ───────────────────────────────────────────────────────────

@Composable
private fun DemoRhythm(color: Color, title: String) {
    NeonRhythmGrid(modifier = Modifier.fillMaxSize(), color = color) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TitlePill(title, color)
        }
    }
}

@Composable
private fun DemoGridPlusSlider(color: Color) {
    NeonRhythmGrid(modifier = Modifier.fillMaxSize(), color = color) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            var step by remember { mutableStateOf(ElectricSliderStep.Medium) }
            NeonElectricDiscreteSlider(
                value = step,
                onValueChange = { step = it },
                modifier = Modifier.fillMaxWidth(0.82f)
            )
        }
    }
}

@Composable
private fun DemoSliderOnly(accent: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TitlePill("NeonElectricDiscreteSlider", accent)
            Spacer(Modifier.height(24.dp))
            var step by remember { mutableStateOf(ElectricSliderStep.Medium) }
            NeonElectricDiscreteSlider(
                value = step,
                onValueChange = { step = it },
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    }
}

@Composable
private fun DemoSliderStep(step: ElectricSliderStep, accent: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TitlePill(step.label, accent)
            Spacer(Modifier.height(20.dp))
            var s by remember { mutableStateOf(step) }
            NeonElectricDiscreteSlider(
                value = s,
                onValueChange = { s = it },
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    }
}

@Composable
private fun DemoFlowSharp(accent: Color) {
    NeonFlow(
        modifier = Modifier.fillMaxSize(),
        intensity = NeonIntensity.MEDIUM,
        config = NeonConfig(
            enableLiquid = false,
            enableParticles = true,
            enableArc = true,
            enableBokeh = false,
            enableSweep = false,
            enableBorder = true,
            enableSparks = true,
            enableMiniBlocks = false,
            primaryColor = accent,
            arcColor = Color.White,
            borderColor = accent
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TitlePill("流光 · 锐利粒子", accent)
                Spacer(Modifier.height(22.dp))
                var step by remember { mutableStateOf(ElectricSliderStep.XHigh) }
                NeonElectricDiscreteSlider(
                    value = step,
                    onValueChange = { step = it },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}

@Composable
private fun DemoFlowFrame(accent: Color) {
    NeonFlow(
        modifier = Modifier.fillMaxSize(),
        intensity = NeonIntensity.MEDIUM,
        config = NeonConfig(
            enableLiquid = false,
            enableParticles = false,
            enableArc = false,
            enableBokeh = false,
            enableSweep = true,
            enableBorder = true,
            enableSparks = false,
            enableMiniBlocks = false,
            sweepColor = accent,
            borderColor = accent
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TitlePill("流光 · 仅边框 + 扫光", accent)
        }
    }
}

@Composable
private fun DemoFlowFull(accent: Color) {
    NeonFlow(
        modifier = Modifier.fillMaxSize(),
        intensity = NeonIntensity.MEDIUM,
        config = NeonConfig(
            enableLiquid = false,
            enableParticles = true,
            enableArc = true,
            enableBokeh = true,
            enableSweep = true,
            enableBorder = true,
            enableSparks = true,
            enableMiniBlocks = true,
            primaryColor = accent,
            arcColor = Color.White,
            borderColor = accent,
            bokehColor = accent,
            sweepColor = accent,
            miniBlockColor = accent
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TitlePill("流光 · 全开", accent)
                Spacer(Modifier.height(22.dp))
                var step by remember { mutableStateOf(ElectricSliderStep.Max) }
                NeonElectricDiscreteSlider(
                    value = step,
                    onValueChange = { step = it },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}
