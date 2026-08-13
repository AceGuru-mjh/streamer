# 霞红·流光 (XiaHong Flow) UI 库

基于 Jetpack Compose + AGSL 的纯 Android UI 库（非 App）。对外仅暴露一个 Composable
`XiaHongFlow`，为包裹的内容叠加三层特效：**液态扭曲基底 → 霞红发光粒子奔流 → 动态电流/电弧**。

> 进度：第 1–6 轮已全部落地（工程骨架 + 内联 AGSL 液态层 + 粒子/电弧引擎 + 发光打磨 + 发布配置）。

## 模块结构

```
streamer/
├── settings.gradle.kts              # 根工程，引入 :core:ui-xiahong 与 :app（demo）
├── build.gradle.kts                 # 统一 AGP 8.2.2 / Kotlin 1.9.22 插件版本
├── gradle.properties
├── app/                             # 本地验证用演示模块（com.android.application，不发布）
│   └── src/main/.../MainActivity.kt # 调用 XiaHongFlow 的演示界面
└── core/ui-xiahong/                 # 库模块 (com.android.library) —— 对外发布单元
    ├── build.gradle.kts             # 含 consumerProguardFiles("consumer-rules.pro")
    ├── consumer-rules.pro           # 保护对外 API 不被混淆
    └── src/main/
        ├── AndroidManifest.xml      # 无启动入口
        └── kotlin/com/example/ui_xiahong/
            ├── XiaHongFlow.kt        # 对外唯一入口 + 内联 SHADER_SRC + 降级
            ├── XiaHongConfig.kt      # 配置类 + XiaHongIntensity 枚举
            └── internal/
                └── Renderers.kt      # ParticleEngine + ArcRenderer（internal）
```

> 注意：`:app` 仅用于本地跑效果/截图验证，**它是 application 不会被 maven 发布**；对外发布的只有 `:core:ui-xiahong`。


## 版本约束

- `minSdk = 33`：AGSL `RuntimeShader` 需 Android 13+（已做 <33 自动降级，仅跳过液态扭曲）。
- `compileSdk = 34`，Kotlin 1.9.22 匹配 Compose 编译器扩展 1.5.1，Compose BOM 2024.02.00。
- 发光效果依赖 `BlurMaskFilter`，需 API 29+ 硬件加速 Canvas —— `minSdk 33` 已满足。

## 构建说明（库本身）

本仓库不含 Gradle Wrapper 的 `gradle-wrapper.jar`（体积大且需联网下载）。本地构建前：

1. 安装 Gradle 8.2+ 或 Android Studio（自带 Gradle）。
2. 在项目根目录生成 wrapper（如本地已装 Gradle）：
   ```bash
   gradle wrapper --gradle-version 8.2
   ```
3. 构建库 AAR：
   ```bash
   ./gradlew :core:ui-xiahong:assembleRelease
   ```
   或发布到本地 maven：
   ```bash
   ./gradlew :core:ui-xiahong:publishToMavenLocal
   ```

## 接入外部 App

本仓库的**对外发布单元是 `:core:ui-xiahong`（库）**；根目录下的 `:app` 只是本地验证用的演示模块，不会被发布。在消费方自己的 App 中引入本库：

1. `settings.gradle.kts` 引入本库模块（路径按你的工程调整）：
   ```kotlin
   include(":core:ui-xiahong")   // 或 implementation 远程 AAR / maven 坐标
   ```
2. `app/build.gradle.kts` 添加依赖：
   ```kotlin
   dependencies {
       implementation(project(":core:ui-xiahong"))
   }
   ```

## 使用示例

```kotlin
// App 界面的某个 Screen
@Composable
fun PremiumDashboard() {
    // 霞红流光库作为最底层容器，包裹任意正常 UI
    XiaHongFlow(
        modifier = Modifier.fillMaxSize(),
        intensity = XiaHongIntensity.MEDIUM,
        config = XiaHongConfig(
            primaryColor = Color(0xFFE91E63), // 更鲜艳的粉红
            particleCount = 500,
            enableLiquid = true
        )
    ) {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Text("尊享会员界面", color = Color.White,
                     style = MaterialTheme.typography.displayMedium)
            }
            items(10) { index ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("数据卡片 $index", modifier = Modifier.padding(16.dp), color = Color.White)
                }
            }
        }
    }
}
```

## 发布前 Checklist

- [x] **混淆**：`consumer-rules.pro` 已通过 `consumerProguardFiles` 接入，保护对外 API。
- [x] **API 兼容**：`XiaHongFlow` 内部用 `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)` 判断，
      未加 `@RequiresApi`，<33 自动跳过 AGSL 扭曲并保留粒子/电弧，不会崩溃。
- [x] **内存**：`ParticleEngine` / `ArcRenderer` 均在 `remember` 中创建；`LaunchedEffect` 的
      `while (isActive)` 无限循环在协程作用域取消时自动停止，无泄漏。

## 已知可调点（后续打磨）

- [x] **液态层零重组**：`time` 改由 `rememberInfiniteTransition` 驱动，仅在 `graphicsLayer` / `Canvas` 的
      绘制回调中读取，不再触发 `XiaHongFlow` 重组（已落地）。
- [x] **电弧升级**：`ArcRenderer` 现用 `PathMeasure` 截取沿路径移动的高亮电光段（流动感）、随机 `alpha`
      闪烁、随机分叉，三层叠加（已落地）。
- [ ] **粒子辉光方案**：当前用 `BlurMaskFilter`（GPU 单 draw，minSdk 33 已满足硬件加速）。若改为每粒子
      径向渐变 `Brush.radialGradient`，需每帧分配 shader，800+ 粒子下 GC 压力更大，暂不做。
