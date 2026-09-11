plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Apple targets can only be compiled on a Mac, and Gradle otherwise fails the whole
// build on Windows while trying to run the Kotlin/Native compiler. Declaring them
// conditionally keeps `gradlew build` green on both machines; the iOS sources are
// still compiled on every macOS build and in CI.
val isMacOs: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

kotlin {
    // expect/actual objects are still flagged Beta and warn on every declaration.
    // The pattern is load-bearing here — it is how persistence, haptics and audio
    // differ per platform — so the warning is acknowledged rather than endured.
    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")

    androidTarget()
    if (isMacOs) {
        listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
            target.binaries.framework {
                baseName = "shared"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
            implementation("com.revenuecat.purchases:purchases-kmp-core:3.3.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        androidMain.dependencies {
            api("androidx.activity:activity-compose:1.10.1")
            api("androidx.appcompat:appcompat:1.7.1")
            api("androidx.core:core-ktx:1.15.0")
        }
        named { it.lowercase().startsWith("ios") }.configureEach {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }
}

compose.resources {
    packageOfResClass = "com.alvaropassalacqua.squishflow.generated.resources"
}

// The RevenueCat key for Android, in order of preference: the environment (CI),
// gradle.properties / local.properties (a developer machine), then the public
// Test Store key. The Test Store key only ever simulates purchases; a release
// build refuses to configure with it (see RevenueCatManager) rather than ship a
// store that cannot charge anyone.
val revenueCatAndroidKey: String = System.getenv("REVENUECAT_ANDROID_KEY")
    ?: (findProperty("revenuecat.android.key") as String?)
    ?: "test_RrClnxXHuDZzKrnNgwtGSiJgoak"

android {
    namespace = "com.alvaropassalacqua.squishflow.shared"
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
        buildConfigField("String", "REVENUECAT_KEY", "\"$revenueCatAndroidKey\"")
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
