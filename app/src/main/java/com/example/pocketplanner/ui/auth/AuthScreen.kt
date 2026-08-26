package com.example.pocketplanner.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketplanner.R
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    initialIsLoginMode: Boolean = true,
    onAuthSuccess: () -> Unit,
    onGuestLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var isLoginMode by remember { mutableStateOf(initialIsLoginMode) }
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { androidx.credentials.CredentialManager.create(context) }
    val webClientId = androidx.compose.ui.res.stringResource(id = R.string.default_web_client_id)

    // Trigger navigation on success
    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            onAuthSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
                )
            )
    ) {
        // Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Load the actual logo from drawable
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = stringResource(R.string.auth_logo_desc),
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(32.dp)) // Reduced spacing slightly to fit smaller screens better
            Text(
                text = if (isLoginMode) stringResource(R.string.auth_sign_in_title) else stringResource(R.string.auth_sign_up_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 36.sp),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isLoginMode) stringResource(R.string.auth_sign_in_subtitle) else stringResource(R.string.auth_sign_up_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // Bottom Sheet Content
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Takes up the remaining space dynamically so it never overlaps!
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                // Segmented Tab Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50)) // Fully rounded pill shape
                        .background(Color.Gray.copy(alpha = 0.1f)) // Clean neutral background
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TabButton(
                        text = stringResource(R.string.auth_tab_login),
                        isSelected = isLoginMode,
                        modifier = Modifier.weight(1f)
                    ) {
                        isLoginMode = true
                    }
                    TabButton(
                        text = stringResource(R.string.auth_tab_signup),
                        isSelected = !isLoginMode,
                        modifier = Modifier.weight(1f)
                    ) {
                        isLoginMode = false
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message
                if (uiState is AuthState.Error) {
                    Text(
                        text = (uiState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Dynamic Form Fields
                AnimatedVisibility(
                    visible = !isLoginMode,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    Column {
                        FormField(
                            label = stringResource(R.string.auth_field_username),
                            value = username,
                            onValueChange = { username = it }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                FormField(
                    label = stringResource(R.string.auth_field_email),
                    value = email,
                    onValueChange = { email = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                FormField(
                    label = stringResource(R.string.auth_field_password),
                    value = password,
                    onValueChange = { password = it },
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { passwordVisible = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Options Row (Forgot Password)
                AnimatedVisibility(visible = isLoginMode) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = stringResource(R.string.auth_forgot_password),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.resetPassword(email) { success, message ->
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action Button
                Button(
                    onClick = {
                        if (isLoginMode) {
                            viewModel.login(email, password)
                        } else {
                            viewModel.register(username, email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState !is AuthState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isLoginMode) stringResource(R.string.auth_tab_login) else stringResource(R.string.auth_tab_signup),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                    Text(
                        text = stringResource(R.string.auth_or_divider),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Google Button
                OutlinedButton(
                    onClick = {
                        val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(webClientId)
                            .build()
                        val request = androidx.credentials.GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                            
                        coroutineScope.launch {
                            try {
                                val result = credentialManager.getCredential(request = request, context = context)
                                val credential = result.credential
                                if (credential is androidx.credentials.CustomCredential && credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                                    viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AuthScreen", "Google Sign-In Error", e)
                                android.widget.Toast.makeText(context, context.getString(R.string.auth_google_error), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = stringResource(R.string.auth_google_logo_desc),
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified // To preserve the original Google colors
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.auth_continue_google),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guest Login
                TextButton(
                    onClick = onGuestLogin,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(R.string.auth_continue_guest),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.auth_guest_login_desc),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp)) // Extra padding at bottom
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
            color = if (isSelected) Color.White else Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: ((Boolean) -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password)

                    IconButton(onClick = { onPasswordVisibilityChange?.invoke(!passwordVisible) }) {
                        Icon(imageVector = image, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Gray,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
