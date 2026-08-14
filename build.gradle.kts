// 纯库工程根构建脚本：仅声明插件版本，供子模块 apply 时复用，不在此直接应用。
plugins {
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
