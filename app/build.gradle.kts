import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// API keys live in the git-ignored local.properties, never in source.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun secret(name: String) = "\"${localProps.getProperty(name, "")}\""

android {
    namespace = "com.matt.flashcard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.matt.flashcard"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DEEPL_API_KEY", secret("DEEPL_API_KEY"))
        buildConfigField("String", "ELEVENLABS_API_KEY", secret("ELEVENLABS_API_KEY"))
        buildConfigField("String", "ANTHROPIC_API_KEY", secret("ANTHROPIC_API_KEY"))
    }

    buildTypes {
        // Release builds never carry the developer's keys; learners enter their own in Settings.
        release {
            buildConfigField("String", "DEEPL_API_KEY", "\"\"")
            buildConfigField("String", "ELEVENLABS_API_KEY", "\"\"")
            buildConfigField("String", "ANTHROPIC_API_KEY", "\"\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    testImplementation("junit:junit:4.13.2")
    // Android's own org.json, so unit tests can parse JSON on a computer.
    testImplementation("com.vaadin.external.google:android-json:0.0.20131108.vaadin1")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.media3:media3-session:1.11.1")
}
