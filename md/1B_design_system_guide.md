# Step 1B: Design System & Theme Guide

Now that the build foundation is set up and backed up to GitHub, let's build the visual foundation of the app! We'll set up our colors, typography, and theme based on your custom blue and orange color palette.

Jetpack Compose handles UI theming using 3 main files inside your app package. Let's update the ones Android Studio generated for us.

## 1. Locate the `ui.theme` Package
In Android Studio, expand your project tree on the left:
`app` > `src` > `main` > `java` > `com` > `example` > `pocketplanner` > `ui` > `theme`

You should see 3 files already generated here: `Color.kt`, `Theme.kt`, and `Type.kt`. 

---

## 2. Setup the Colors (`Color.kt`)
Open the existing `Color.kt` file. Delete everything inside it and replace it with the following code. This sets up your exact Light and Dark mode colors.

```kotlin
package com.example.pocketplanner.ui.theme

import androidx.compose.ui.graphics.Color

// Light Mode Colors
val LightPrimary = Color(0xFF4496D8)
val LightSecondary = Color(0xFF77BEF0)
val LightTertiary = Color(0xFFE86A2D)
val LightError = Color(0xFFD32F2F)
val LightBackground = Color(0xFFF7FAFC)
val LightSurface = Color(0xFFFFFFFF)

// Dark Mode Colors
val DarkPrimary = Color(0xFF77BEF0)
val DarkSecondary = Color(0xFFA8D7FA)
val DarkTertiary = Color(0xFFFFAC7A)
val DarkError = Color(0xFFEF5350)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
```

---

## 3. Setup the Typography (`Type.kt`)
Open the existing `Type.kt` file. Replace its contents with this. 

*Note: For now, we will use default modern system fonts to keep things simple, but we've structured it so we can easily swap in Google Fonts (Inter and Outfit) later.*

```kotlin
package com.example.pocketplanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

---

## 4. Setup the Theme (`Theme.kt`)
Open the existing `Theme.kt` file. Replace its contents with this file, which maps your light and dark mode colors directly to Material 3's color scheme.

```kotlin
package com.example.pocketplanner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    error = DarkError,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    error = LightError,
    background = LightBackground,
    surface = LightSurface
)

@Composable
fun PocketPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false by default to ensure your custom colors are always used
    // instead of Android 12+ wallpaper colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## Next Steps
Once you've replaced the contents of these 3 files, our app has its core visual identity!

Let me know once you are done!
