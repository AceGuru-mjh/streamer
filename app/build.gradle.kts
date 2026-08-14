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
        versionCode = 3
        versionName = "1.2"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // 兼容 Kotlin 1.9.24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 签名配置：优先读取 CI/本地注入的环境变量；缺省时回退到占位值。
    // 本地可用 `gradlew :app:assembleRelease` 前先 export 这些变量（见 README）。
    val keystoreFile = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotEmpty() }
    val keystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
    val keyAlias = System.getenv("KEY_ALIAS") ?: "xiahong"
    val keyPassword = System.getenv("KEY_PASSWORD") ?: "android"

    signingConfigs {
        create("release") {
            // 若未提供 keystore 文件，则跳过签名（例如纯本地调试构建）
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
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
