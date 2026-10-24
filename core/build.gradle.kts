plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.mofangvideo.core"
    compileSdk = Versions.compileSdk

    defaultConfig {
        minSdk = Versions.minSdk
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    api(Deps.coreKtx)

    // 协程
    api(Deps.coroutinesAndroid)

    // Hilt
    api(Deps.hiltAndroid)
    ksp(Deps.hiltCompiler)

    // Room
    api(Deps.roomRuntime)
    api(Deps.roomKtx)
    ksp(Deps.roomCompiler)

    // 网络
    api(Deps.okhttp)
    api(Deps.okhttpLogging)

    // 播放器
    api(Deps.exoplayer)
    api(Deps.media3Ui)

    // 图片
    api(Deps.glide)

    // 基础 UI
    api(Deps.material)
    api(Deps.appcompat)
    api(Deps.fragmentKtx)
    api(Deps.constraintlayout)
    api(Deps.recyclerview)
    api(Deps.lifecycleViewModel)
    api(Deps.lifecycleLiveData)
    api(Deps.navigationFragment)
    api(Deps.navigationUi)

    // 测试
    testImplementation(Deps.junit)
    testImplementation(Deps.jsonTest)
    testImplementation(Deps.coroutinesTest)
    testImplementation(Deps.roomTesting)
    testImplementation(Deps.okhttpMockwebserver)
}
