plugins {
    id("com.android.application")
    kotlin("android") version "1.8.0"
}

val compileSdkVersion: String by project
val minSdkVersion: String by project
val targetSdkVersion: String by project
val packageName = "com.garmin.android.apps.connectiq.sample.comm"
val versionCode: String by project
val versionName: String by project

kotlin {
    jvmToolchain(17)
}

android {
    namespace = this@Build_gradle.packageName
    compileSdk = this@Build_gradle.compileSdkVersion.toInt()

    defaultConfig {
        applicationId = this@Build_gradle.packageName
        minSdk = minSdkVersion.toInt()
        targetSdk = targetSdkVersion.toInt()
        versionCode = this@Build_gradle.versionCode.toInt()
        versionName = this@Build_gradle.versionName
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.garmin.connectiq:ciq-companion-app-sdk:2.2.0@aar")
}