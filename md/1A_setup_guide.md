# Step 1A: Project Setup & Build System Guide

Welcome! Since you want to be hands-on to understand everything, we'll set up the project step-by-step together. 

## 1. Create the Project in Android Studio

First, let's bootstrap the project using Android Studio:
1. Open **Android Studio**.
2. Click **New Project**.
3. Select **Empty Activity** (this template is pre-configured for Jetpack Compose, which we're using for our UI).
4. Click **Next**.
5. Fill in the details:
   - **Name:** `PocketPlanner`
   - **Package name:** `com.pocketplanner`
   - **Save location:** `E:\PocketPlanner` (or your preferred subfolder within your workspace)
   - **Minimum SDK:** API 26 (Android 8.0) or higher (recommended for modern apps)
   - **Build configuration language:** `Kotlin DSL (build.gradle.kts)` (this is standard for modern Android apps)
6. Click **Finish** and wait for Android Studio to generate the project and finish its initial sync.

## 2. Configure Dependencies (Version Catalog)

Modern Android projects use a Version Catalog to manage dependencies centrally. Let's set it up.

Open the file `gradle/libs.versions.toml` in your project and replace its contents with the following. This adds the versions and libraries we need for Room, Hilt, Firebase, and Compose.

```toml
[versions]
agp = "8.3.0"
kotlin = "1.9.0"
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.4"
activityCompose = "1.9.1"
composeBom = "2024.06.00"
navigationCompose = "2.7.7"
hilt = "2.51.1"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
datastore = "1.1.1"
firebaseBom = "33.1.2"
playServicesLocation = "21.3.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

# Navigation
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Hilt (Dependency Injection)
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Room (Local Database)
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# DataStore (Preferences)
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth-ktx = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore-ktx = { group = "com.google.firebase", name = "firebase-firestore-ktx" }

# Location
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

> **Why this step?** The version catalog ensures we declare all our dependency versions in one place. If we need to update Compose or Room later, we only change it here.

## 3. Configure Project-Level Gradle

Next, open your project-level `build.gradle.kts` (this is the one in the root folder, not the `app` folder). Update it to apply the plugins we'll need for Hilt (Dependency Injection) and Google Services (Firebase).

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    
    // Add these plugins for Hilt and Google Services
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.services) apply false
}
```

## 4. Configure App-Level Gradle

Now open your app-level `build.gradle.kts` (located inside the `app/` folder). Update the `plugins` and `dependencies` blocks.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    
    // Apply Hilt and Kotlin KAPT (for generating boilerplate code)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.pocketplanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pocketplanner"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
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

    // Location Services
    implementation(libs.play.services.location)
}
```

> **Why this step?** Here we actually link the libraries we defined in our catalog to our app. We are also enabling KAPT (Kotlin Annotation Processing Tool) which is required for Hilt and Room to generate code.

## 5. Sync Project & Update AndroidManifest

1. Click the **"Sync Now"** button in Android Studio (usually a banner at the top) to download all these dependencies.
2. Open `app/src/main/AndroidManifest.xml` and add these permissions above the `<application>` tag, which we'll need for location tracking and internet access:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Add these permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.CAMERA" />

    <application>
        ...
```

---

Let me know once you've completed these steps or if you run into any build errors during the Gradle sync!
