package com.example.pocketplanner.ui.explore

import android.media.MediaPlayer
import androidx.compose.ui.platform.LocalContext
import com.example.pocketplanner.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveExperienceScreen(
    placeName: String,
    onNavigateBack: () -> Unit
) {
    var isFullscreen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tracks = remember(placeName, context) {
        if (placeName == "War Remnants Museum") {
            listOf(
                Track(context.getString(R.string.wrm_1_title), context.getString(R.string.wrm_1_subtitle), R.raw.wrm_1_introduction),
                Track(context.getString(R.string.wrm_2_title), context.getString(R.string.wrm_2_subtitle), R.raw.wrm_2_tigercagesystem),
                Track(context.getString(R.string.wrm_3_title), context.getString(R.string.wrm_3_subtitle), R.raw.wrm_3_guillotine),
                Track(context.getString(R.string.wrm_4_title), context.getString(R.string.wrm_4_subtitle), R.raw.wrm_4_agentorange),
                Track(context.getString(R.string.wrm_5_title), context.getString(R.string.wrm_5_subtitle), R.raw.wrm_5_sonmymassacre)
            )
        } else if (placeName == "The Independence Palace") {
            listOf(
                Track(context.getString(R.string.tip_1_title), context.getString(R.string.tip_1_subtitle), R.raw.tip_1_architecture),
                Track(context.getString(R.string.tip_2_title), context.getString(R.string.tip_2_subtitle), R.raw.tip_2_outsidearchitecture),
                Track(context.getString(R.string.tip_3_title), context.getString(R.string.tip_3_subtitle), R.raw.tip_3_insidearchitecture),
                Track(context.getString(R.string.tip_4_title), context.getString(R.string.tip_4_subtitle), R.raw.tip_4_conferencehall),
                Track(context.getString(R.string.tip_5_title), context.getString(R.string.tip_5_subtitle), R.raw.tip_5_cabinetroom),
                Track(context.getString(R.string.tip_6_title), context.getString(R.string.tip_6_subtitle), R.raw.tip_6_statebanquetinghall),
                Track(context.getString(R.string.tip_7_title), context.getString(R.string.tip_7_subtitle), R.raw.tip_7_ambassadorschamber),
                Track(context.getString(R.string.tip_8_title), context.getString(R.string.tip_8_subtitle), R.raw.tip_8_presidentialoffice),
                Track(context.getString(R.string.tip_9_title), context.getString(R.string.tip_9_subtitle), R.raw.tip_9_vicepresidentoffice),
                Track(context.getString(R.string.tip_10_title), context.getString(R.string.tip_10_subtitle), R.raw.tip_10_presidentialreceptionrooms),
                Track(context.getString(R.string.tip_11_title), context.getString(R.string.tip_11_subtitle), R.raw.tip_11_centralstaircase),
                Track(context.getString(R.string.tip_12_title), context.getString(R.string.tip_12_subtitle), R.raw.tip_12_rooftop),
                Track(context.getString(R.string.tip_13_title), context.getString(R.string.tip_13_subtitle), R.raw.tip_13_bunker)
            )
        } else {
            emptyList()
        }
    }
    
    var selectedTrackIndex by remember { mutableIntStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableIntStateOf(0) }
    var isInitialLoad by remember { mutableStateOf(true) }

    DisposableEffect(selectedTrackIndex, tracks) {
        if (tracks.isEmpty() || selectedTrackIndex !in tracks.indices) {
            return@DisposableEffect onDispose { }
        }
        
        val mp = MediaPlayer.create(context, tracks[selectedTrackIndex].resourceId)
        mediaPlayer = mp
        durationMs = mp.duration
        progress = 0f
        
        if (!isInitialLoad) {
            mp.start()
            isPlaying = true
        } else {
            isPlaying = false
            isInitialLoad = false
        }
        
        mp.setOnCompletionListener {
            isPlaying = false
            progress = 1f
        }
        onDispose {
            mp.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (durationMs > 0) {
                    progress = mp.currentPosition.toFloat() / durationMs.toFloat()
                }
            }
            delay(100)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isFullscreen) {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.explore_immersive_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.explore_immersive_back), tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = if (isFullscreen) 0.dp else 24.dp)
        ) {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 3 })
            val coroutineScope = rememberCoroutineScope()
            var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

            val staticMaps = mapOf(
                "War Remnants Museum" to "https://hiddenlandtravel.com/wp-content/uploads/2023/08/War-Remnants-Museum.jpg",
                "Cu Chi Tunnels" to "https://duaelbluiumc3.cloudfront.net/Media/Images/cu-chi-tunnels-worth-it-cu-chi-map.jpg",
                "Hoa Lo Prison" to "https://hoalo.vn/images/SoDoThamQuanL.jpg",
                "Hue Imperial City (The Citadel)" to "https://www.vietnamairlines.com/content/dam/legacy-site-assets/SEO-images/2025%20SEO/Thay%20Anh%20Traffic%20Tieng%20Anh/hue%20historic%20citadel/map-of-hue-historic-citadel.jpeg",
                "Thien Mu Pagoda" to "https://danangmotorbikeadventure.com/wp-content/uploads/2022/08/Map-of-Thien-Mu-Pagoda-in-Hue.jpeg",
                "Bach Ma National Park" to "https://culturephamtravel.com/wp-content/uploads/2025/12/Do-Quyen-Waterfall-Culture-Pham-Travel-2-1.jpg"
            )

            // Main image card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFullscreen) Modifier.weight(1f) else Modifier.height(350.dp))
                    .clip(if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp))
            ) {
                if (staticMaps.containsKey(placeName)) {
                    AsyncImage(
                        model = staticMaps[placeName],
                        contentDescription = stringResource(R.string.explore_immersive_static_map),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().background(Color.White)
                    )
                } else {
                    // Background View Pager
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = pagerState.currentPage != 0, // Disable swipe on 360 VR to allow panning
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        if (page == 0) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { context ->
                                    android.webkit.WebView(context).apply {
                                        webViewRef = this
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = object : android.webkit.WebViewClient() {
                                            private var fallbackAttempted = false
                                            override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                                if (request?.isForMainFrame == true && placeName == "The Independence Palace" && !fallbackAttempted) {
                                                    fallbackAttempted = true; view?.loadUrl("https://thamquanvr360.dinhdoclap.gov.vn/")
                                                } else super.onReceivedError(view, request, error)
                                            }
                                            override fun onReceivedHttpError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                                if (request?.isForMainFrame == true && placeName == "The Independence Palace" && !fallbackAttempted) {
                                                    fallbackAttempted = true; view?.loadUrl("https://thamquanvr360.dinhdoclap.gov.vn/")
                                                } else super.onReceivedHttpError(view, request, errorResponse)
                                            }
                                        }
                                        val url = if (placeName == "The Independence Palace") "https://vr360.com.vn/projects/dinhdoclap/" else {
                                            val query = java.net.URLEncoder.encode(placeName, "UTF-8")
                                            "https://www.google.com/maps/search/?api=1&query=$query"
                                        }
                                        loadUrl(url)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Static maps
                            val imgUrl = if (page == 1) {
                                "https://dinhdoclap.gov.vn/wp-content/uploads/2025/11/dinhdoclap738-mbws-vn-dinhdoclap-gov-vn-google-trang-t-nh-3-768x833.png"
                            } else {
                                "https://dinhdoclap.gov.vn/wp-content/uploads/2025/11/dinhdoclap738-mbws-vn-dinhdoclap-gov-vn-google-trang-t-nh-4-768x820.png"
                            }
                            
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = stringResource(R.string.explore_immersive_static_map),
                                contentScale = ContentScale.Fit, // Using Fit so the entire map is visible
                                modifier = Modifier.fillMaxSize().background(Color.White) // Added white background for transparent PNG maps
                            )
                        }
                    }

                    // Top Right Reload button (only in VR mode)
                    if (pagerState.currentPage == 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(40.dp)
                                .clickable { webViewRef?.reload() }
                        ) {
                            Icon(
                                Icons.Filled.Refresh, 
                                contentDescription = stringResource(R.string.explore_immersive_reload),
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Segmented Control
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(if (isFullscreen) Alignment.TopCenter else Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            listOf(stringResource(R.string.explore_immersive_tab_360vr), stringResource(R.string.explore_immersive_tab_map1), stringResource(R.string.explore_immersive_tab_map2)).forEachIndexed { index, title ->
                                val isSelected = pagerState.currentPage == index
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    modifier = Modifier
                                        .clickable { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom right fullscreen icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clickable { isFullscreen = !isFullscreen }
                ) {
                    Icon(
                        Icons.Filled.Fullscreen,
                        contentDescription = if (isFullscreen) stringResource(R.string.explore_immersive_exit_fullscreen) else stringResource(R.string.explore_immersive_fullscreen),
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (!isFullscreen) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.explore_immersive_audio_guide), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))

                if (tracks.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.explore_immersive_no_audio),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    // Bottom Player disabled
                    BottomPlayer(
                        progress = 0f,
                        isPlaying = false,
                        durationMs = 0,
                        onProgressChange = { },
                        onPlayPauseClick = { },
                        onPreviousClick = { },
                        onNextClick = { },
                        onRewindClick = { },
                        onFastForwardClick = { }
                    )
                } else {
                    // Track list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tracks.size) { index ->
                            TrackItem(
                                track = tracks[index],
                                isSelected = index == selectedTrackIndex,
                                onClick = { selectedTrackIndex = index }
                            )
                        }
                    }

                    // Bottom Player
                    BottomPlayer(
                        progress = progress,
                        isPlaying = isPlaying,
                        durationMs = durationMs,
                        onProgressChange = { newProgress ->
                            progress = newProgress
                            mediaPlayer?.seekTo((newProgress * durationMs).toInt())
                        },
                        onPlayPauseClick = {
                            mediaPlayer?.let { mp ->
                                if (mp.isPlaying) {
                                    mp.pause()
                                    isPlaying = false
                                } else {
                                    mp.start()
                                    isPlaying = true
                                }
                            }
                        },
                        onPreviousClick = { if (selectedTrackIndex > 0) selectedTrackIndex-- },
                        onNextClick = { if (selectedTrackIndex < tracks.size - 1) selectedTrackIndex++ },
                        onRewindClick = {
                            mediaPlayer?.let { mp ->
                                val newPos = (mp.currentPosition - 10000).coerceAtLeast(0)
                                mp.seekTo(newPos)
                                if (durationMs > 0) progress = newPos.toFloat() / durationMs.toFloat()
                            }
                        },
                        onFastForwardClick = {
                            mediaPlayer?.let { mp ->
                                val newPos = (mp.currentPosition + 10000).coerceAtMost(durationMs)
                                mp.seekTo(newPos)
                                if (durationMs > 0) progress = newPos.toFloat() / durationMs.toFloat()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomPlayer(
    progress: Float,
    isPlaying: Boolean,
    durationMs: Int,
    onProgressChange: (Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onRewindClick: () -> Unit,
    onFastForwardClick: () -> Unit
) {
    val totalSeconds = durationMs / 1000
    val currentSeconds = (progress * totalSeconds).toInt()
    
    val currentFormatted = String.format("%02d:%02d", currentSeconds / 60, currentSeconds % 60)
    val totalFormatted = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(24.dp)
            ) {
                Text(currentFormatted, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = progress,
                    onValueChange = onProgressChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(totalFormatted, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.explore_immersive_prev_track), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp).clickable { onPreviousClick() })
                Icon(Icons.Filled.Replay10, contentDescription = stringResource(R.string.explore_immersive_rewind), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp).clickable { onRewindClick() })
                
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                        .clickable { onPlayPauseClick() }
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.explore_immersive_play_pause),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Icon(Icons.Filled.Forward10, contentDescription = stringResource(R.string.explore_immersive_forward), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp).clickable { onFastForwardClick() })
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.explore_immersive_next_track), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp).clickable { onNextClick() })
            }
        }
    }
}

data class Track(val title: String, val subtitle: String, val resourceId: Int)

@Composable
fun TrackItem(track: Track, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val iconBgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val iconColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val playPauseColor = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconBgColor,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = titleColor)
                Text(track.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            // Right play/pause indicator
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = playPauseColor,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Pause, // Indicating it's active
                        contentDescription = stringResource(R.string.explore_immersive_playing),
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                 Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.explore_immersive_play),
                    tint = playPauseColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
