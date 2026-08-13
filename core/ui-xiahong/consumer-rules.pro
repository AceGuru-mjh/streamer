# 保留对外 API，避免外部 App 开启 R8 / 混淆时误删 Composable 接口与配置类
-keep class com.example.ui_xiahong.** { *; }
