package com.example.pocketplanner.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface) // Solid white background
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Push the icon and text to the center vertically by adding an equal weight spacer above and below
        Spacer(modifier = Modifier.weight(1f))

        // Circular Icon Header
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Public,
                contentDescription = "Globe Icon",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Welcome to\nPocketPlanner",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "Your all-in-one travel companion for planning, tracking, and exploring your next big adventure.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // This pushes the buttons to the very bottom
        Spacer(modifier = Modifier.weight(1f)) 

        // Get Started Button
        Button(
            onClick = onNavigateToSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Reverted back to 56.dp
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                "Get Started", 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), // Reverted back to titleMedium
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward Arrow", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // I already have an account Button
        OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // Reverted back to 56.dp
        ) {
            Text(
                "I already have an account", 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Continue as Guest
        TextButton(onClick = onNavigateToHome) {
            Text(
                "CONTINUE AS GUEST",
                style = MaterialTheme.typography.labelMedium.copy( // Reduced back to labelMedium
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface 
            )
        }
    }
}
