package com.example.pocketplanner.ui.chat.translate

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors
import kotlin.math.max
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    onNavigateBack: () -> Unit,
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val viewModel: TranslateViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var pickingLanguageFor by remember { mutableStateOf<String?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val executor = remember { ContextCompat.getMainExecutor(context) }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, it)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            viewModel.switchMode(TranslateMode.GALLERY, bitmap)
            viewModel.processBitmap(bitmap)
        }
    }

    // Intercept hardware/gesture back press to return to Live Camera instead of exiting
    androidx.activity.compose.BackHandler(enabled = uiState.mode != TranslateMode.LIVE) {
        viewModel.switchMode(TranslateMode.LIVE)
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            viewModel.stopSpeaking()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (cameraPermissionState.status.isGranted) {
            if (uiState.mode == TranslateMode.LIVE) {
                val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

                LaunchedEffect(lifecycleOwner, isActive) {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        if (isActive) {
                            // Only unbind all if WE are the active tab taking control!
                            cameraProvider.unbindAll()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            try {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }, executor)
                }

                // LIVE CAMERA PREVIEW
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { previewView }
                )
            } else if (uiState.capturedImage != null) {
                // FROZEN OR GALLERY IMAGE
                androidx.compose.foundation.Image(
                    bitmap = uiState.capturedImage!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.translate_captured_image_cd),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }

            // TEXT OVERLAY LAYER
            if (uiState.blocks.isNotEmpty()) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val constraints = this.constraints
                        val screenWidth = constraints.maxWidth.toFloat()
                        val screenHeight = constraints.maxHeight.toFloat()
                        val densityValue = LocalContext.current.resources.displayMetrics.density

                        val scaleX = screenWidth / uiState.sourceImageWidth
                        val scaleY = screenHeight / uiState.sourceImageHeight
                        // Fit uses minOf, Crop uses maxOf
                        val scale = minOf(scaleX, scaleY)

                        val scaledImageWidth = uiState.sourceImageWidth * scale
                        val scaledImageHeight = uiState.sourceImageHeight * scale

                        val offsetX = (screenWidth - scaledImageWidth) / 2f
                        val offsetY = (screenHeight - scaledImageHeight) / 2f

                        uiState.blocks.forEachIndexed { index, block ->
                            if (block.textWidth > 0f && block.textHeight > 0f) {
                                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                val originalWidthDp = (block.textWidth * scale) / densityValue
                                val originalHeightDp = (block.textHeight * scale) / densityValue

                                val inflationDp = 3f
                                val leftDp = (((block.cornerX * scale) + offsetX) / densityValue) - inflationDp
                                val topDp = (((block.cornerY * scale) + offsetY) / densityValue) - inflationDp
                                val widthDp = originalWidthDp + (inflationDp * 2)
                                val heightDp = originalHeightDp + (inflationDp * 2)

                                // Calculate raw font size directly proportional to the ORIGINAL bounding box
                                val charRatio = block.originalText.length.toFloat() / block.translatedText.length.coerceAtLeast(1).toFloat()
                                val areaScaleFactor = kotlin.math.sqrt(charRatio.toDouble()).toFloat().coerceIn(0.5f, 1f)
                                
                                val rawFontSize = (originalHeightDp / block.lineCount) * 0.75f * areaScaleFactor
                                
                                // Lower the minimum bound significantly so tiny text remains tiny and doesn't wrap/overlap!
                                val estimatedFontSize = rawFontSize.coerceIn(2f, 24f)

                                // Highlight the currently spoken text
                                val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                                    append(block.translatedText)
                                    if (uiState.spokenBlockIndex == index && uiState.spokenLocalRange != null) {
                                        val range = uiState.spokenLocalRange!!
                                        if (range.first >= 0 && range.last < block.translatedText.length) {
                                            addStyle(
                                                style = androidx.compose.ui.text.SpanStyle(background = androidx.compose.ui.graphics.Color.Yellow),
                                                start = range.first,
                                                end = range.last + 1
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .absoluteOffset(x = leftDp.dp, y = topDp.dp)
                                        // CRITICAL: Rotate the box around its top-left corner (0,0) to perfectly align with the physical text!
                                        .graphicsLayer(
                                            rotationZ = block.rotationAngle,
                                            transformOrigin = TransformOrigin(0f, 0f)
                                        )
                                        .width(widthDp.dp)
                                        .height(heightDp.dp)
                                        .background(androidx.compose.ui.graphics.Color.White, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                        .padding(horizontal = 4.dp), // Remove vertical padding to let text use the full bubble height!
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    androidx.compose.material3.Text(
                                        text = annotatedString,
                                        color = androidx.compose.ui.graphics.Color.Black,
                                        fontSize = estimatedFontSize.sp,
                                        lineHeight = (estimatedFontSize * 1.1f).sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (uiState.mode != TranslateMode.LIVE) viewModel.switchMode(TranslateMode.LIVE)
                    else onNavigateBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.translate_back_cd), tint = Color.White)
                }

                // Language Selector Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side (Source Language)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFE8EAF6))
                                .clickable { pickingLanguageFor = "source" }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (uiState.sourceLanguage == "auto") {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1E1E1E))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            val sourceName = if (uiState.sourceLanguage == "auto") stringResource(R.string.translate_auto) 
                            else ALL_GLOBAL_LANGUAGES.find { it.first == uiState.sourceLanguage }?.second ?: stringResource(R.string.translate_unknown)
                            Text(sourceName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E1E1E), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1E1E1E))
                        Spacer(modifier = Modifier.width(4.dp))

                        // Right side (Target Language)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFE8EAF6))
                                .clickable { pickingLanguageFor = "target" }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val targetName = ALL_GLOBAL_LANGUAGES.find { it.first == uiState.targetLanguage }?.second ?: stringResource(R.string.translate_unknown)
                            Text(targetName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E1E1E), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }

                IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.translate_gallery_cd), tint = Color.White)
                }
            }

            // BOTTOM CONTROLS (Shutter)
            if (uiState.mode == TranslateMode.LIVE) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    IconButton(
                        onClick = {
                            imageCapture.takePicture(
                                executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val rawBitmap = image.toBitmap()
                                        val rotation = image.imageInfo.rotationDegrees

                                        // CameraX toBitmap() in this config ignores EXIF rotation, so we MUST manually rotate it upright
                                        val bitmap = if (rotation != 0) {
                                            val matrix = android.graphics.Matrix()
                                            matrix.postRotate(rotation.toFloat())
                                            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                        } else {
                                            rawBitmap
                                        }

                                        viewModel.switchMode(TranslateMode.CAPTURED, bitmap)
                                        viewModel.processBitmap(bitmap)
                                        image.close()
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                            .size(72.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.translate_capture_cd), tint = Color.Black)
                    }
                }
            }

            // BOTTOM PANEL (Result for Capture/Gallery)
            if (uiState.mode != TranslateMode.LIVE && !uiState.isProcessing) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        val copiedMessage = stringResource(R.string.translate_toast_copied_all)
                        
                        androidx.compose.material3.Button(
                            onClick = { 
                                val allText = viewModel.getFullTranslatedText()
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(allText))
                                android.widget.Toast.makeText(context, copiedMessage, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.95f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.translate_btn_select_all))
                        }
                        
                        androidx.compose.material3.Button(
                            onClick = { 
                                viewModel.speakAllText()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.95f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.translate_btn_listen))
                        }
                    }
                }
            }

            // Loading indicator during image processing
            if (uiState.isProcessing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

        } else {
            // PERMISSION DENIED UI
            Column(modifier = Modifier.fillMaxSize().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.translate_camera_permission_required), color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text(stringResource(R.string.translate_btn_grant_permission))
                }
            }
        }
        if (pickingLanguageFor != null) {
            val isSource = pickingLanguageFor == "source"
            val currentCode = if (isSource) uiState.sourceLanguage else uiState.targetLanguage
            
            TranslateLanguagePickerBottomSheet(
                isSource = isSource,
                currentLanguageCode = currentCode,
                onLanguageSelected = { newCode ->
                    if (isSource) {
                        viewModel.setSourceLanguage(newCode)
                    } else {
                        viewModel.setTargetLanguage(newCode)
                    }
                    pickingLanguageFor = null
                },
                onDismiss = { pickingLanguageFor = null }
            )
        }
    }
}

val ALL_GLOBAL_LANGUAGES = listOf(
    "af" to "Afrikaans", "sq" to "Albanian", "am" to "Amharic", "ar" to "Arabic",
    "hy" to "Armenian", "az" to "Azerbaijani", "eu" to "Basque", "be" to "Belarusian",
    "bn" to "Bengali", "bs" to "Bosnian", "bg" to "Bulgarian", "ca" to "Catalan",
    "ceb" to "Cebuano", "ny" to "Chichewa", "zh-cn" to "Chinese (Simplified)",
    "zh-tw" to "Chinese (Traditional)", "co" to "Corsican", "hr" to "Croatian",
    "cs" to "Czech", "da" to "Danish", "nl" to "Dutch", "en" to "English",
    "eo" to "Esperanto", "et" to "Estonian", "tl" to "Filipino", "fi" to "Finnish",
    "fr" to "French", "fy" to "Frisian", "gl" to "Galician", "ka" to "Georgian",
    "de" to "German", "el" to "Greek", "gu" to "Gujarati", "ht" to "Haitian Creole",
    "ha" to "Hausa", "haw" to "Hawaiian", "iw" to "Hebrew", "hi" to "Hindi",
    "hmn" to "Hmong", "hu" to "Hungarian", "is" to "Icelandic", "ig" to "Igbo",
    "id" to "Indonesian", "ga" to "Irish", "it" to "Italian", "ja" to "Japanese",
    "jw" to "Javanese", "kn" to "Kannada", "kk" to "Kazakh", "km" to "Khmer",
    "rw" to "Kinyarwanda", "ko" to "Korean", "ku" to "Kurdish (Kurmanji)",
    "ky" to "Kyrgyz", "lo" to "Lao", "la" to "Latin", "lv" to "Latvian",
    "lt" to "Lithuanian", "lb" to "Luxembourgish", "mk" to "Macedonian",
    "mg" to "Malagasy", "ms" to "Malay", "ml" to "Malayalam", "mt" to "Maltese",
    "mi" to "Maori", "mr" to "Marathi", "mn" to "Mongolian", "my" to "Myanmar (Burmese)",
    "ne" to "Nepali", "no" to "Norwegian", "or" to "Odia (Oriya)", "ps" to "Pashto",
    "fa" to "Persian", "pl" to "Polish", "pt" to "Portuguese", "pa" to "Punjabi",
    "ro" to "Romanian", "ru" to "Russian", "sm" to "Samoan", "gd" to "Scots Gaelic",
    "sr" to "Serbian", "st" to "Sesotho", "sn" to "Shona", "sd" to "Sindhi",
    "si" to "Sinhala", "sk" to "Slovak", "sl" to "Slovenian", "so" to "Somali",
    "es" to "Spanish", "su" to "Sundanese", "sw" to "Swahili", "sv" to "Swedish",
    "tg" to "Tajik", "ta" to "Tamil", "tt" to "Tatar", "te" to "Telugu",
    "th" to "Thai", "tr" to "Turkish", "tk" to "Turkmen", "uk" to "Ukrainian",
    "ur" to "Urdu", "ug" to "Uyghur", "uz" to "Uzbek", "vi" to "Vietnamese",
    "cy" to "Welsh", "xh" to "Xhosa", "yi" to "Yiddish", "yo" to "Yoruba",
    "zu" to "Zulu"
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TranslateLanguagePickerBottomSheet(
    isSource: Boolean,
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val baseLanguages = if (isSource) {
        listOf("auto" to stringResource(R.string.translate_auto)) + ALL_GLOBAL_LANGUAGES
    } else {
        ALL_GLOBAL_LANGUAGES
    }

    val filteredLanguages = baseLanguages.filter { 
        it.second.contains(searchQuery, ignoreCase = true) 
    }

    val title = if (isSource) stringResource(R.string.translate_from_title) else stringResource(R.string.translate_to_title)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.translate_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.translate_search_cd)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                items(filteredLanguages.size) { index ->
                    val (code, name) = filteredLanguages[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = currentLanguageCode == code,
                            onClick = { onLanguageSelected(code) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = name, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
