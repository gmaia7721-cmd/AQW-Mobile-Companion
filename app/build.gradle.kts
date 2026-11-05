plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aqw.companion"
    compileSdk = 35

    signingConfigs {
        create("stable") {
            storeFile = file(System.getenv("AQW_KEYSTORE_PATH") ?: "missing-keystore.jks")
            storePassword = System.getenv("AQW_KEYSTORE_PASSWORD")
            keyAlias = "aqwcompanion"
            keyPassword = System.getenv("AQW_KEYSTORE_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.aqw.companion"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "1.3"
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("stable")
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

dependencies {
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("org.jsoup:jsoup:1.18.3")
}
