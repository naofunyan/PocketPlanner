package com.example.pocketplanner.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySharingScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isPicturesEnabled by viewModel.attachPicturesEnabled.collectAsState()
    val isAudioEnabled by viewModel.attachAudioEnabled.collectAsState()

    // Contact State and Picker
    val contacts by viewModel.emergencyContacts.collectAsState()
    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val contactData = resolvePhoneNumber(context, uri)
                if (contactData != null) {
                    viewModel.addEmergencyContact(contactData)
                } else {
                    Toast.makeText(context, context.getString(R.string.emergency_toast_no_phone), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- PERMISSION LAUNCHERS ---
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateAttachPictures(isGranted)
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.emergency_toast_camera_permission), Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateAttachAudio(isGranted)
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.emergency_toast_mic_permission), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.emergency_sharing_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.settings_cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.emergency_cd_more_options))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. Top Illustration Graphic
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape).padding(8.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.width(60.dp).height(4.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                            }
                        }
                        Row(modifier = Modifier.padding(start = 56.dp)) {
                            Row(
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("00:05", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.width(40.dp).height(4.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                            }
                        }
                        Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp)).padding(12.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(8.dp))
                        }
                    }
                }
            }

            // 2. Explanatory Texts
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(id = R.string.emergency_sharing_desc1),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.emergency_sharing_desc2),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3. Share With Section
            Column {
                Text(
                    text = stringResource(id = R.string.emergency_share_with_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(id = R.string.emergency_contacts_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                    contactPickerLauncher.launch(intent)
                                }
                            ) {
                                Text(stringResource(id = R.string.emergency_add_button), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (contacts.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.emergency_no_contacts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 4.dp)
                            )
                        } else {
                            contacts.forEachIndexed { index, contactData ->
                                val parts = contactData.split("|")
                                val name = parts.getOrElse(0) { stringResource(id = R.string.emergency_unknown_contact) }
                                val number = parts.getOrElse(1) { "" }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 12.dp, bottom = if (index == contacts.size - 1) 12.dp else 0.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = number,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeEmergencyContact(contactData) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(id = R.string.emergency_cd_remove_contact),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Share Surroundings Section
            Column(modifier = Modifier.padding(bottom = 120.dp)) {
                Text(
                    text = stringResource(id = R.string.emergency_share_surroundings_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Attach Pictures
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.emergency_attach_pictures),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(id = R.string.emergency_attach_pictures_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = isPicturesEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            viewModel.updateAttachPictures(true)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    } else {
                                        viewModel.updateAttachPictures(false)
                                    }
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))

                        // Attach Audio
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.emergency_attach_audio),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(id = R.string.emergency_attach_audio_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = isAudioEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            viewModel.updateAttachAudio(true)
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    } else {
                                        viewModel.updateAttachAudio(false)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("Range")
private fun resolvePhoneNumber(context: Context, uri: Uri): String? {
    var contactName = context.getString(R.string.emergency_unknown_contact)
    var contactNumber: String? = null

    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            contactName = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: context.getString(R.string.emergency_unknown_contact)
            contactNumber = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
        }
    }

    return if (contactNumber != null) "$contactName|$contactNumber" else null
}