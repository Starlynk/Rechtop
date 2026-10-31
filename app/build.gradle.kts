plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "nl.rechtopzitten"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.rechtopzitten"
        minSdk = 29
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    // Fixed key, so every GitHub build can be installed over the previous one.
    signingConfigs {
        getByName("debug") {
            storeFile = file("rechtop.keystore")
            storePassword = "rechtop123"
            keyAlias = "rechtop"
            keyPassword = "rechtop123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
