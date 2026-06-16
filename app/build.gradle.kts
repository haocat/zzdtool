plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zzd.tool"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zzd.tool"
        minSdk = 27
        targetSdk = 36
        versionName = "1.0.0"
        versionCode = 1
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }
}

dependencies {
    // Xposed API (compileOnly，运行时由 LSPosed 提供)
    compileOnly("de.robv.android.xposed:api:82")

    // DexKit — 运行时搜索混淆后的类/方法
    implementation("org.luckypray:dexkit:2.2.0")

    // MMKV — 高性能跨进程 KV 存储
    implementation("com.tencent:mmkv:1.3.9")

    // AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
