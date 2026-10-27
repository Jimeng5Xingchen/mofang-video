plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.mofangvideo"
    compileSdk = Versions.compileSdk

    defaultConfig {
        applicationId = "com.mofangvideo"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = 2
        versionName = "1.1.0"
    }

    signingConfigs {
        if (Signing.isConfigured) {
            create("release") {
                storeFile = file(Signing.storeFile!!)
                storePassword = Signing.storePassword
                keyAlias = Signing.keyAlias
                keyPassword = Signing.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (Signing.isConfigured) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // 未提供签名信息时保持可构建（CI 中以 warning 提示），产物为未签名 APK。
                logger.warn(":app release signing not configured; APK will be unsigned.")
            }
        }
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
    implementation(project(":core"))
    implementation(project(":feature-video"))
    implementation(project(":feature-live"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-history"))
    implementation(project(":feature-backup"))

    implementation(Deps.hiltAndroid)
    implementation(Deps.navigationFragment)
    implementation(Deps.navigationUi)

    ksp(Deps.hiltCompiler)

    testImplementation(Deps.junit)
    androidTestImplementation(Deps.roomTesting)
}
