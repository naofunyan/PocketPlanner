# Step 1F: Navigation & App Shell Guide

Now that the backend is set up, it's time to build the "skeleton" of the app! We need a way for the user to navigate between the Login Screen, the Trips list, and their Profile.

We will use Jetpack Compose Navigation to build a Bottom Navigation bar and route the user around.

## 1. Define Type-Safe Routes
Instead of using messy strings for navigation (like `"login_screen"`), modern Compose allows us to use Kotlin Objects to guarantee we don't make typos.

1. Right-click on `pocketplanner` and create a new Package named `ui.navigation`.
2. Inside `ui.navigation`, create a Kotlin File named `Routes.kt`:

```kotlin
package com.example.pocketplanner.ui.navigation

import kotlinx.serialization.Serializable

// The screens our app has
@Serializable
object LoginRoute

@Serializable
object HomeRoute // Where we show the list of trips

@Serializable
object ProfileRoute
```
*(Note: If `@Serializable` is red, click it and press `Alt + Enter` to import `kotlinx.serialization.Serializable`. If it still doesn't resolve, we'll fix it in the next step!)*

## 2. Fix Serialization Dependency (If needed)
If `@Serializable` was red in the previous step, it's because we need to add the Kotlin Serialization plugin.

1. Open your `libs.versions.toml` file.
2. Under `[versions]`, add: `kotlinSerialization = "1.6.3"`
3. Under `[libraries]`, add: `kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinSerialization" }`
4. Under `[plugins]`, add: `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`

5. Open your `app/build.gradle.kts` file.
6. In the `plugins { }` block at the top, add: `alias(libs.plugins.kotlin.serialization)`
7. In the `dependencies { }` block at the bottom, add: `implementation(libs.kotlinx.serialization.json)`
8. **Click Sync Now!**

## 3. Create the Main App Shell
This is the screen that holds the Bottom Navigation Bar.

1. Inside `ui.navigation`, create a Kotlin File named `MainScreen.kt`:

```kotlin
package com.example.pocketplanner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pocketplanner.ui.auth.LoginScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    // We only want to show the bottom bar if the user is NOT on the login screen
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route?.contains("LoginRoute") == false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    // Home Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Trips") },
                        selected = currentDestination?.hierarchy?.any { it.route?.contains("HomeRoute") == true } == true,
                        onClick = {
                            navController.navigate(HomeRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    // Profile Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentDestination?.hierarchy?.any { it.route?.contains("ProfileRoute") == true } == true,
                        onClick = {
                            navController.navigate(ProfileRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // This NavHost is where the actual screens are swapped in and out
        NavHost(
            navController = navController,
            startDestination = LoginRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = {
                        // When login succeeds, go to Home and remove Login from the backstack
                        navController.navigate(HomeRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    }
                )
            }
            
            composable<HomeRoute> {
                // Placeholder for now! We will build this in Step 1G
                Text("Your Trips Will Appear Here!")
            }
            
            composable<ProfileRoute> {
                // Placeholder for now!
                Text("User Profile Settings")
            }
        }
    }
}
```

## 4. Hook it up to MainActivity
Finally, tell the app to launch this `MainScreen` when it opens!

1. Open `MainActivity.kt` (it's right inside the main `com.example.pocketplanner` package).
2. Delete whatever is inside the `setContent { ... }` block and replace it with this:

```kotlin
package com.example.pocketplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pocketplanner.ui.navigation.MainScreen
import com.example.pocketplanner.ui.theme.PocketPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

// Make sure you add this annotation so Hilt knows to inject things here!
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketPlannerTheme {
                MainScreen()
            }
        }
    }
}
```

---
Let me know when you've finished setting this up! If you click "Run App" on your emulator/phone, you should now see the actual Login Screen, and if you click "Login", it should take you to the Bottom Navigation screen!
