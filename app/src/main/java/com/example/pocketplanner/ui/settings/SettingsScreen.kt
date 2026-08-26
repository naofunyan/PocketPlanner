package com.example.pocketplanner.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.pocketplanner.R
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.painter.Painter
import com.example.pocketplanner.ui.navigation.MedicalInfoRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit = {},
    onNavigateToFallDetection: () -> Unit = {},
    onNavigateToMedicalInfo: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val user = FirebaseAuth.getInstance().currentUser
    val userName = user?.displayName?.takeIf { it.isNotBlank() } ?: "naofunyan"
    val userEmail = user?.email ?: stringResource(id = R.string.settings_not_logged_in)

    val safeUserName = userName.replace(" ", "+")

    val uploadedAvatarUrl by viewModel.avatarUrl.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val displayUrl = uploadedAvatarUrl
        ?: "https://ui-avatars.com/api/?name=${safeUserName}&background=4496D8&color=fff&size=200"

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatar(uri)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateNotifications(true)
        } else {
            Toast.makeText(context, context.getString(R.string.settings_permission_notification), Toast.LENGTH_LONG).show()
            viewModel.updateNotifications(false)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateLocationAccess(true)
        } else {
            Toast.makeText(context, context.getString(R.string.settings_permission_location), Toast.LENGTH_LONG).show()
            viewModel.updateLocationAccess(false)
        }
    }

    // Observe persistent DataStore values
    val fallDetectionEnabled by viewModel.fallDetectionEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val locationAccessEnabled by viewModel.locationAccessEnabled.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, notificationsEnabled, locationAccessEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Sync notification permission
                val hasNotificationPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true
                if (notificationsEnabled && !hasNotificationPerm) {
                    viewModel.updateNotifications(false)
                }

                // Sync location permission
                val hasLocationPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (locationAccessEnabled && !hasLocationPerm) {
                    viewModel.updateLocationAccess(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    // State for Dialogs
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) } // NEW: Delete Dialog State

    // --- DYNAMIC SUBTITLES VARIABLES ---
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val currentLanguageSubtitle = if (currentLocale == "vi") stringResource(id = R.string.settings_language_vietnamese) else stringResource(id = R.string.settings_language_english)

    val currentThemeMode = AppCompatDelegate.getDefaultNightMode()
    val currentThemeSubtitle = when (currentThemeMode) {
        AppCompatDelegate.MODE_NIGHT_NO -> stringResource(id = R.string.settings_theme_light)
        AppCompatDelegate.MODE_NIGHT_YES -> stringResource(id = R.string.settings_theme_dark)
        else -> stringResource(id = R.string.settings_theme_system_default)
    }
    // ------------------------------------

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.settings_cd_back), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 24.dp,
                top = 24.dp,
                end = 24.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. PROFILE SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = displayUrl,
                            contentDescription = stringResource(id = R.string.settings_cd_profile_picture),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = !isUploading) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        )

                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. SAFETY SECTION
            item {
                SettingsGroup(title = stringResource(id = R.string.settings_safety_emergency)) {
                    SettingsAdvancedToggleRow(
                        icon = Icons.Default.Warning,
                        title = stringResource(id = R.string.settings_fall_detection),
                        subtitle = stringResource(id = R.string.settings_fall_detection_desc),
                        iconTint = MaterialTheme.colorScheme.error,
                        isChecked = fallDetectionEnabled,
                        onCheckedChange = { viewModel.updateFallDetection(it) },
                        onRowClick = onNavigateToFallDetection
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    SettingsActionRow(
                        icon = Icons.Default.LocalHospital,
                        title = stringResource(id = R.string.settings_medical_info),
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = onNavigateToMedicalInfo
                    )
                }
            }

            // 3. PREFERENCES SECTION
            item {
                SettingsGroup(title = stringResource(id = R.string.settings_preferences)) {
                    SettingsActionRow(
                        icon = Icons.Default.Language,
                        title = stringResource(id = R.string.settings_language),
                        subtitle = currentLanguageSubtitle,
                        onClick = { showLanguageSheet = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    SettingsActionRow(
                        icon = Icons.Default.DarkMode,
                        title = stringResource(id = R.string.settings_app_theme),
                        subtitle = currentThemeSubtitle,
                        onClick = { showThemeSheet = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(id = R.string.settings_notifications),
                        subtitle = stringResource(id = R.string.settings_notifications_desc),
                        isChecked = notificationsEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    viewModel.updateNotifications(true)
                                } else {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.updateNotifications(isChecked)
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    SettingsToggleRow(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(id = R.string.settings_location_context),
                        subtitle = stringResource(id = R.string.settings_location_context_desc),
                        isChecked = locationAccessEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    viewModel.updateLocationAccess(true)
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            } else {
                                viewModel.updateLocationAccess(false)
                            }
                        }
                    )
                }
            }

            // 4. ABOUT & SUPPORT
            item {
                SettingsGroup(title = stringResource(id = R.string.settings_support)) {
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = stringResource(id = R.string.settings_help_center),
                        onClick = { /* TODO */ }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    SettingsActionRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(id = R.string.settings_privacy_policy),
                        onClick = { /* TODO */ }
                    )
                }
            }

            // 5. ACCOUNT
            item {
                SettingsGroup(title = stringResource(id = R.string.settings_account)) {
                    SettingsActionRow(
                        icon = painterResource(id = R.drawable.logout),
                        title = stringResource(id = R.string.settings_log_out),
                        onClick = { showLogoutDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    SettingsActionRow(
                        icon = Icons.Default.DeleteForever,
                        title = stringResource(id = R.string.settings_delete_account),
                        iconTint = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteDialog = true } // UPDATED: Trigger Delete Dialog
                    )
                }
            }

            // 6. VERSION NUMBER
            item {
                Text(
                    text = stringResource(id = R.string.settings_version),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- DIALOGS & SHEETS ---

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(id = R.string.settings_log_out),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.settings_logout_confirm_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_log_out),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(
                        text = stringResource(id = android.R.string.cancel),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    // NEW: Delete Account Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(id = R.string.settings_delete_account),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.settings_delete_confirm_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        // Trigger the ViewModel deletion process, and navigate ONLY on success
                        viewModel.deleteAccount(onSuccess = onDeleteAccountClick)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_delete_forever),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = stringResource(id = android.R.string.cancel),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    if (showLanguageSheet) {
        LanguageSelectionSheet(
            onDismiss = { showLanguageSheet = false },
            onLanguageSelected = { languageTag ->
                val localeList = LocaleListCompat.forLanguageTags(languageTag)
                AppCompatDelegate.setApplicationLocales(localeList)
                showLanguageSheet = false
            }
        )
    }

    if (showThemeSheet) {
        ThemeSelectionSheet(
            onDismiss = { showThemeSheet = false },
            onThemeSelected = { selectedMode ->
                viewModel.updateThemeMode(selectedMode)
                showThemeSheet = false
            }
        )
    }
}

// --- REUSABLE SETTINGS COMPONENTS ---

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = textColor)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsActionRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = textColor)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsAdvancedToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClick() }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(id = R.string.settings_cd_more_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- BOTTOM SHEETS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSheet(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isVietnamese = currentLocale == "vi"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_language),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SheetSelectionRow(
                label = stringResource(id = R.string.settings_language_english),
                isSelected = !isVietnamese,
                onClick = { onLanguageSelected("en") }
            )

            SheetSelectionRow(
                label = stringResource(id = R.string.settings_language_vietnamese),
                isSelected = isVietnamese,
                onClick = { onLanguageSelected("vi") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionSheet(
    onDismiss: () -> Unit,
    onThemeSelected: (Int) -> Unit
) {
    val currentMode = AppCompatDelegate.getDefaultNightMode()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_app_theme),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SheetSelectionRow(
                label = stringResource(id = R.string.settings_theme_system_default),
                isSelected = currentMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM || currentMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED,
                onClick = { onThemeSelected(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
            )

            SheetSelectionRow(
                label = stringResource(id = R.string.settings_theme_light),
                isSelected = currentMode == AppCompatDelegate.MODE_NIGHT_NO,
                onClick = { onThemeSelected(AppCompatDelegate.MODE_NIGHT_NO) }
            )

            SheetSelectionRow(
                label = stringResource(id = R.string.settings_theme_dark),
                isSelected = currentMode == AppCompatDelegate.MODE_NIGHT_YES,
                onClick = { onThemeSelected(AppCompatDelegate.MODE_NIGHT_YES) }
            )
        }
    }
}

@Composable
private fun SheetSelectionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(id = R.string.settings_cd_selected),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}