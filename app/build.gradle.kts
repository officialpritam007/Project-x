plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.pritam.liquidchat"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pritam.liquidchat"
        minSdk = 29
        targetSdk = 35
        buildConfigField("String","FCM_APP_ID", "\"" + (System.getenv("FCM_APP_ID") ?: "") + "\"")
        buildConfigField("String","FCM_PROJECT_ID", "\"" + (System.getenv("FCM_PROJECT_ID") ?: "") + "\"")
        buildConfigField("String","FCM_API_KEY", "\"" + (System.getenv("FCM_API_KEY") ?: "") + "\"")
        buildConfigField("String","FCM_SENDER_ID", "\"" + (System.getenv("FCM_SENDER_ID") ?: "") + "\"")
        versionCode = 1
        versionName = "0.1.0"
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
    implementation("com.google.firebase:firebase-messaging:24.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    testImplementation("junit:junit:4.13.2")
}
