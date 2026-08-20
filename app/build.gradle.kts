import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Apply Hilt and Kotlin KAPT (for generating boilerplate code)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}


val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (!localPropertiesFile.exists()) {
    localPropertiesFile.createNewFile()
}
localProperties.load(localPropertiesFile.inputStream())
val vertexApiKey = localProperties.getProperty("VERTEX_API_KEY") ?: System.getenv("VERTEX_API_KEY") ?: ""
val unsplashApiKey = localProperties.getProperty("UNSPLASH_API_KEY") ?: System.getenv("UNSPLASH_API_KEY") ?: ""
val mapboxAccessToken = localProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: System.getenv("MAPBOX_ACCESS_TOKEN") ?: ""
val foursquareApiKey = localProperties.getProperty("FOURSQUARE_API_KEY") ?: System.getenv("FOURSQUARE_API_KEY") ?: ""

android {
    namespace = "com.example.pocketplanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pocketplanner"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "VERTEX_API_KEY", "\"$vertexApiKey\"")
        buildConfigField("String", "UNSPLASH_API_KEY", "\"$unsplashApiKey\"")
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxAccessToken\"")
        buildConfigField("String", "FOURSQUARE_API_KEY", "\"$foursquareApiKey\"")
        buildConfigField("String", "GOOGLE_PLACES_API_KEY", "\"${localProperties.getProperty("GOOGLE_PLACES_API_KEY") ?: System.getenv("GOOGLE_PLACES_API_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/INDEX.LIST"
            )
        }
    }
}

dependencies {
    // Core & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.coil.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room (Local Database)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // DataStore (Preferences)
    implementation(libs.androidx.datastore.preferences)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation("com.google.firebase:firebase-functions-ktx")

    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Location Services
    implementation(libs.play.services.location)

    // Mapbox
    implementation("com.mapbox.maps:android:11.2.0")
    implementation("com.mapbox.extension:maps-compose:11.2.0")

    // ML Kit for Language ID & Translation
    implementation("com.google.mlkit:language-id:17.0.6")
    implementation("com.google.mlkit:translate:17.0.3")

    // WorkManager for background syncing
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // Testing (fixes KAPT @Test annotation errors)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation(libs.kotlinx.serialization.json)
    
    // OkHttp for WebSockets (Gemini Live API)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Accompanist for Permissions in Compose
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // CameraX for Live Video Feed
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    
    // Fix for ListenableFuture resolution in CameraX
    implementation("com.google.guava:guava:32.1.3-android")
}