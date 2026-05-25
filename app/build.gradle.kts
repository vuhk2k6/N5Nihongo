import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.vunv.n5nihongo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vunv.n5nihongo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "").trim()
        val facebookAppId = localProperties.getProperty("FACEBOOK_APP_ID", "").trim()
        val facebookClientToken = localProperties.getProperty("FACEBOOK_CLIENT_TOKEN", "").trim()

        if (googleWebClientId.isNotEmpty()) {
            resValue("string", "default_web_client_id", googleWebClientId)
        }
        if (facebookAppId.isNotEmpty()) {
            resValue("string", "facebook_app_id", facebookAppId)
            resValue("string", "fb_login_protocol_scheme", "fb$facebookAppId")
        }
        if (facebookClientToken.isNotEmpty()) {
            resValue("string", "facebook_client_token", facebookClientToken)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xskip-metadata-version-check")
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {
    val room_version = "2.6.1"
    val firebase_bom = "34.13.0"

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.gson)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation("com.caverock:androidsvg:1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(libs.androidx.activity)

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Room database
    implementation("androidx.room:room-runtime:${room_version}")
    implementation("androidx.room:room-ktx:${room_version}")
    ksp("androidx.room:room-compiler:${room_version}")

    // Firebase Auth + Firestore
    implementation(platform("com.google.firebase:firebase-bom:${firebase_bom}"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-ai")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.facebook.android:facebook-login:17.0.2")
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}