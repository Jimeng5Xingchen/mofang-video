// 版本与依赖统一管理
object Versions {
    const val minSdk = 21
    const val targetSdk = 34
    const val compileSdk = 34

    const val kotlin = "1.9.22"
    const val agp = "8.2.2"
    const val coroutines = "1.7.3"
    const val hilt = "2.50"
    const val room = "2.6.1"
    const val okhttp = "4.12.0"
    const val glide = "4.16.0"
    const val exoplayer = "1.2.1"
    const val material = "1.11.0"
    const val leanback = "1.0.0"
    const val appcompat = "1.6.1"
    const val constraintlayout = "2.1.4"
    const val lifecycle = "2.7.0"
    const val navigation = "2.7.6"
    const val recyclerview = "1.3.2"
    const val fragment = "1.6.2"
    const val coreKtx = "1.12.0"
}

object Deps {
    // 协程
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"

    // 依赖注入
    const val hiltAndroid = "com.google.dagger:hilt-android:${Versions.hilt}"
    const val hiltCompiler = "com.google.dagger:hilt-compiler:${Versions.hilt}"

    // 数据库
    const val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
    const val roomKtx = "androidx.room:room-ktx:${Versions.room}"
    const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"
    const val roomTesting = "androidx.room:room-testing:${Versions.room}"

    // 网络
    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    const val okhttpLogging = "com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}"
    const val okhttpMockwebserver = "com.squareup.okhttp3:mockwebserver:${Versions.okhttp}"

    // 图片与播放器
    const val glide = "com.github.bumptech.glide:glide:${Versions.glide}"
    const val exoplayer = "androidx.media3:media3-exoplayer:${Versions.exoplayer}"
    const val media3Ui = "androidx.media3:media3-ui:${Versions.exoplayer}"

    // UI
    const val material = "com.google.android.material:material:${Versions.material}"
    const val leanback = "androidx.leanback:leanback:${Versions.leanback}"
    const val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
    const val fragmentKtx = "androidx.fragment:fragment-ktx:${Versions.fragment}"
    const val constraintlayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintlayout}"
    const val recyclerview = "androidx.recyclerview:recyclerview:${Versions.recyclerview}"
    const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"

    // 架构组件
    const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
    const val lifecycleLiveData = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}"
    const val navigationFragment = "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}"
    const val navigationUi = "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"

    // 测试
    const val junit = "junit:junit:4.13.2"

    // JVM 单测中的 org.json 真实实现，替换 Android stub
    const val jsonTest = "org.json:json:20231013"
}
