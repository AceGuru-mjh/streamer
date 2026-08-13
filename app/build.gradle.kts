// ⚠️ 注意：本 app 模块仅用于本地验证「霞红·流光」库的效果，不参与库发布。
// 库本身仍是 :core:ui-xiahong（com.android.library）。
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 33 // 必须与库的 minSdk 对齐（AGSL 要求）
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // 依赖本地库模块
    implementation(project(":core:ui-xiahong"))

    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // setContent 扩展（仅 demo 需要，库本身不依赖）
    implementation("androidx.activity:activity-compose")
    // Compose 预览工具（仅 demo 需要，库本身不依赖、不写 @Preview）
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
