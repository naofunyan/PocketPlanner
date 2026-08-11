# Step 1D: Firebase Auth Integration Guide

To sync our users' itineraries across devices, we need a backend. We are using **Firebase** for this. Before we write the Authentication code in Android Studio, we have to register our app with Google.

## 1. Create the Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/) in your browser.
2. Click **Create a project**.
3. Name it **PocketPlanner**.
4. You can disable Google Analytics for now to save time, then click **Create Project**.

---

## 2. Register the Android App
1. Once your project is ready, click the white **Android icon** in the center of the project overview page to add an Android app.
2. **Android package name**: You must type exactly `com.example.pocketplanner` (This is very important to match your `build.gradle.kts` file!).
3. App nickname: "PocketPlanner Android".
4. Click **Register app**.

---

## 3. Download the Configuration File
1. Click the blue **Download google-services.json** button.
2. Find the downloaded file on your computer.
3. In IntelliJ/Android Studio, switch the Project view on the left from "Android" to **"Project"** (using the dropdown at the very top of the left panel) so you can see the actual file folders.
4. Drag and drop the `google-services.json` file directly into your `app` folder (`PocketPlanner/app/`). 
5. Click **Next** through the rest of the Firebase console setup (we already added the Gradle dependencies in Step 1A). Click **Continue to console**.

---

## 4. Enable Authentication
1. On the left menu in the Firebase Console, click **Build > Authentication**.
2. Click **Get Started**.
3. Under "Sign-in providers", click **Email/Password**.
4. Enable the first toggle (Email/Password) and click **Save**.

---

## 5. Create the AuthRepository Code
Now back to your IDE! We need to create a repository that talks to Firebase.

1. Inside `com.example.pocketplanner.data`, create a new Directory called `repository`.
2. Inside `repository`, create a Kotlin Interface named `AuthRepository.kt`:

```kotlin
package com.example.pocketplanner.data.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<FirebaseUser?>
    
    suspend fun signUp(email: String, password: String): Result<FirebaseUser>
    suspend fun signIn(email: String, password: String): Result<FirebaseUser>
    suspend fun signOut()
}
```

3. Now, in the same `repository` folder, create a Kotlin Class named `AuthRepositoryImpl.kt` that implements this interface:

```kotlin
package com.example.pocketplanner.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    // Emits the current user immediately, and every time they log in/out
    override val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
```

---
Let me know when you've placed `google-services.json` in your app folder and created these two code files!
