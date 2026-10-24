plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.mofangvideo.feature.settings"
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
    api(project(":core"))
    api(project(":feature-video"))
    api(project(":feature-live"))
    implementation(Deps.hiltAndroid)
    ksp(Deps.hiltCompiler)
}
