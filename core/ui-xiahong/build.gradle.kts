plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ui_xiahong"
    compileSdk = 34

    defaultConfig {
        minSdk = 33 // AGSL RuntimeShader 需要 Android 13+ (Tiramisu)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 保护对外 API，避免消费方 R8 混淆误删
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        // 着色器以字符串内联方式经 RuntimeShader(SHADER_SRC) 加载，无需 shaders 资源目录
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // 兼容 Kotlin 1.9.22
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // 仅依赖必要的 Compose 基础包
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.graphics:graphics-shapes:1.0.0-alpha05")
}
