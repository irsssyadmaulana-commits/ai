plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val ks = file("../notme.jks")
android {
    namespace = "com.example.bubbleai"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.bubbleai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        if (ks.exists()) create("rel") {
            storeFile = ks
            storePassword = System.getenv("KS_PASS") ?: "bubbleai123"
            keyAlias = "notme"
            keyPassword = System.getenv("KS_PASS") ?: "bubbleai123"
        }
    }
    buildTypes {
        release {
            if (ks.exists()) signingConfig = signingConfigs.getByName("rel")
            isMinifyEnabled = false
        }
    }
}
