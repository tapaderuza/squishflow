plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget()
    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
        }
    }
}

android {
    namespace = "com.alvaropassalacqua.squishflow"
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        applicationId = "com.alvaropassalacqua.squishflow"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = (findProperty("app.versionCode") as String).toInt()
        versionName = findProperty("app.versionName") as String
    }

    // The upload key lives outside the repository. With the four variables set
    // (see docs/MONETIZATION_LAUNCH.md) `bundleRelease` produces a Play-ready
    // AAB; without them the release build still assembles, signed with the
    // debug key, so R8 can be exercised on any machine.
    val keystorePath = System.getenv("SQUISHFLOW_KEYSTORE")
    if (keystorePath != null) {
        signingConfigs.create("release") {
            storeFile = file(keystorePath)
            storePassword = System.getenv("SQUISHFLOW_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("SQUISHFLOW_KEY_ALIAS")
            keyPassword = System.getenv("SQUISHFLOW_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystorePath != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        // AGP's bundled lint reads Kotlin 2.1 metadata and prints a page of
        // "incompatible version" errors against every Kotlin 2.3 class on a
        // release build. They are noise, not findings; lint still runs on debug.
        checkReleaseBuilds = false
    }
}
