plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
}
val signingValues=listOf("RELEASE_KEYSTORE_PATH","RELEASE_STORE_PASSWORD","RELEASE_KEY_ALIAS","RELEASE_KEY_PASSWORD").map {System.getenv(it).orEmpty()}
val hasReleaseSigning=signingValues.all {it.isNotBlank()}
check(signingValues.all {it.isBlank()} || hasReleaseSigning) {"Release signing requires all four owner-controlled signing variables."}
android {
    namespace = "com.pritam.liquidchat"
    compileSdk = 36
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = "com.pritam.liquidchat"
        minSdk = 29
        targetSdk = 36
        buildConfigField("String","FCM_APP_ID", "\"" + (System.getenv("FCM_APP_ID") ?: "") + "\"")
        buildConfigField("String","FCM_PROJECT_ID", "\"" + (System.getenv("FCM_PROJECT_ID") ?: "") + "\"")
        buildConfigField("String","FCM_API_KEY", "\"" + (System.getenv("FCM_API_KEY") ?: "") + "\"")
        buildConfigField("String","FCM_SENDER_ID", "\"" + (System.getenv("FCM_SENDER_ID") ?: "") + "\"")
        buildConfigField("boolean","ENABLE_EXPERIMENTAL_CALLS",if(System.getenv("ENABLE_EXPERIMENTAL_CALLS")=="true") "true" else "false")
        versionCode = 10
        versionName = "1.0.0-rc3"
    }
    signingConfigs {
        // CI prepares/caches this exact development key. AGP defaults may use a
        // different Android user directory; never infer key continuity from cache configuration.
        System.getenv("LIQUID_DEBUG_KEYSTORE_PATH")?.takeIf {it.isNotBlank()}?.let {path->
            getByName("debug") {storeFile=file(path)}
        }
        if(hasReleaseSigning)create("upload") {
            storeFile=file(signingValues[0]);storePassword=signingValues[1]
            keyAlias=signingValues[2];keyPassword=signingValues[3]
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled=true
            isShrinkResources=true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")
            if(hasReleaseSigning)signingConfig=signingConfigs.getByName("upload")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    implementation("dev.chrisbanes.haze:haze:1.5.4")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("com.google.firebase:firebase-messaging:24.1.1")
    implementation("io.github.webrtc-sdk:android:150.7871.01")
    // Local 720p/30-second story export and authenticated MP4 playback (no paid media API).
    implementation("androidx.media3:media3-transformer:1.11.1")
    implementation("androidx.media3:media3-effect:1.11.1")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    testImplementation("junit:junit:4.13.2")
}

// The signed-candidate workflow must pass this task; debug signing is never reused.
tasks.register("verifyReleaseSigning") {
    doLast {
        check(hasReleaseSigning && file(signingValues[0]).isFile) {"Owner upload signing credentials are required. No signed release was produced."}
    }
}
