# Step 1D: Auth Dependency Injection & UI Guide

You've successfully created the `AuthRepository`, but Android doesn't know how to "build" it yet because it requires a `FirebaseAuth` instance to work. We use **Hilt** (Dependency Injection) to wire these pieces together automatically. Once wired, we will build the Login Screen!

## 1. Setup Hilt Dependency Injection
Hilt acts like a central factory that knows how to provide instances of classes to your app.

1. Right-click on `data` (inside `com.example.pocketplanner`) and create a new Package named `di` (short for Dependency Injection).
2. Inside `di`, create a Kotlin Object named `AppModule.kt`:

```kotlin
package com.example.pocketplanner.data.di

import com.example.pocketplanner.data.repository.AuthRepository
import com.example.pocketplanner.data.repository.AuthRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}
```

## 2. Initialize Hilt in your Application
Hilt requires a custom Application class to bootstrap itself when the app opens.

1. Right-click on `pocketplanner` (the root package) -> **New > Kotlin Class**.
2. Name it `PocketPlannerApp`.

```kotlin
package com.example.pocketplanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PocketPlannerApp : Application()
```

3. Open your `AndroidManifest.xml`. Inside the `<application>` tag, add `android:name=".PocketPlannerApp"` so Android knows to use this class:

```xml
    <application
            android:name=".PocketPlannerApp" 
            android:allowBackup="true"
            ...
```

## 3. Create the Auth ViewModel
The ViewModel connects your UI to the Repository.

1. Right-click on `pocketplanner` and create a Package named `ui.auth`.
2. Inside `ui.auth`, create a Kotlin Class named `AuthViewModel.kt`:

```kotlin
package com.example.pocketplanner.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(email, pass)
            if (result.isSuccess) {
                _uiState.value = AuthState.Success
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login Failed")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
```

## 4. Build the Login Screen UI
Finally, the fun part—the UI! We will use Jetpack Compose and our custom blue/orange theme.

1. Inside `ui.auth`, create a Kotlin File named `LoginScreen.kt`:

```kotlin
package com.example.pocketplanner.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // If login is successful, trigger the callback to navigate
    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to\nPocketPlanner",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = uiState !is AuthState.Loading
        ) {
            if (uiState is AuthState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Login")
            }
        }

        if (uiState is AuthState.Error) {
            Text(
                text = (uiState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
```

---
Let me know when you've finished these 4 steps! Give the project a build (Make Project) afterwards to ensure Hilt successfully wires everything together.
