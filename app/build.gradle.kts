import java.util.Properties

/**
 * Signing material never lives in this repository - it is public. Values come
 * from keystore.properties (local, gitignored) or, in CI, from environment
 * variables fed by GitHub Actions secrets. When neither is present the build
 * still works: debug falls back to the SDK's own debug key, and release is
 * produced unsigned.
 */
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(property: String, env: String): String? =
    keystoreProperties.getProperty(property) ?: System.getenv(env)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.drehtuer.shotgun"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.drehtuer.shotgun"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            val store = signingValue("debug.storeFile", "DEBUG_KEYSTORE_FILE")
                ?.let { rootProject.file(it) }
            if (store?.exists() == true) {
                storeFile = store
                storePassword = signingValue("debug.storePassword", "DEBUG_KEYSTORE_PASSWORD")
                keyAlias = signingValue("debug.keyAlias", "DEBUG_KEY_ALIAS")
                keyPassword = signingValue("debug.keyPassword", "DEBUG_KEY_PASSWORD")
            }
        }
        create("release") {
            val store = signingValue("release.storeFile", "RELEASE_KEYSTORE_FILE")
                ?.let { rootProject.file(it) }
            if (store?.exists() == true) {
                storeFile = store
                storePassword = signingValue("release.storePassword", "RELEASE_KEYSTORE_PASSWORD")
                keyAlias = signingValue("release.keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("release.keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Carries full debug data: debuggable, unminified, symbols intact.
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")

            // Unit-test coverage only. enableAndroidTestCoverage would
            // instrument the APK itself, and this app is judged on touch and
            // countdown timing - a slowed build would misreport how it feels.
            enableUnitTestCoverage = true

            // No applicationIdSuffix on purpose: it would rename the package
            // for debug builds and break every documented adb command.
        }
        release {
            // Optimised and stripped: no debug symbols, no debuggable flag.
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            isJniDebuggable = false
            ndk { debugSymbolLevel = "NONE" }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Left unsigned when the release key is absent, which is the normal
            // case in CI - the release key is deliberately not on GitHub.
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile?.exists() == true }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    // Exported schemas are committed, so a migration can be written against a
    // real before-and-after rather than from memory.
    ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
