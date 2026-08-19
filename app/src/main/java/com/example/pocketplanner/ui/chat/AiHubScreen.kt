package com.example.pocketplanner.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AiHubScreen(
    onNavigateBack: () -> Unit
) {
    val actualPageCount = 4
    val startIndex = 20000 // A large number divisible by 4 to allow infinite looping in both directions
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { 40000 }
    )
    val coroutineScope = rememberCoroutineScope()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("PocketPlannerPrefs", android.content.Context.MODE_PRIVATE)
    }
    
    // Hint logic: hide permanently once they swipe away from the initial page
    var hasSwiped by remember { 
        mutableStateOf(sharedPreferences.getBoolean("has_swiped_ai_hub", false))
    }
    
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != startIndex && !hasSwiped) {
            hasSwiped = true
            sharedPreferences.edit().putBoolean("has_swiped_ai_hub", true).apply()
        }
    }

    val tabs = listOf("CHAT", "LIVE", "LENS", "INTERPRET")
    var tabWidths by remember { mutableStateOf(List(tabs.size) { 0.dp }) }
    var tabOffsets by remember { mutableStateOf(List(tabs.size) { 0.dp }) }
    var tabHeight by remember { mutableStateOf(32.dp) }
    val density = LocalDensity.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            var dragAccumulator by remember { mutableStateOf(0f) }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface) // Change background to white/surface
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onDragEnd = {
                                val page = pagerState.currentPage
                                if (dragAccumulator < -40f) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(page + 1) }
                                } else if (dragAccumulator > 40f) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(page - 1) }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount
                            }
                        )
                    }
                    .padding(vertical = 12.dp), // Replaced bottom-only padding with vertical to center it in the white bar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = !hasSwiped,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Swipe for more AI tools ➔",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                // Mode selector built using a Box so horizontal screen swipes seamlessly pass through
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val currentTab = pagerState.currentPage % actualPageCount
                    val pillWidth by animateDpAsState(
                        targetValue = if (tabWidths.isNotEmpty()) tabWidths[currentTab] else 0.dp,
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                        label = "pillWidth"
                    )
                    val pillOffset by animateDpAsState(
                        targetValue = if (tabOffsets.isNotEmpty()) tabOffsets[currentTab] else 0.dp,
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                        label = "pillOffset"
                    )

                    // The sliding background pill
                    if (tabWidths.isNotEmpty() && tabWidths[0] > 0.dp) {
                        Box(
                            modifier = Modifier
                                .offset(x = pillOffset)
                                .width(pillWidth)
                                .height(tabHeight) // Uses the exact measured height
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary) // App's primary color
                        )
                    }

                    // The text row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = currentTab == index
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { layoutCoordinates ->
                                        val widthDp = with(density) { layoutCoordinates.size.width.toDp() }
                                        val heightDp = with(density) { layoutCoordinates.size.height.toDp() }
                                        val offsetDp = with(density) { layoutCoordinates.positionInParent().x.toDp() }
                                        val newWidths = tabWidths.toMutableList()
                                        newWidths[index] = widthDp
                                        tabWidths = newWidths
                                        val newOffsets = tabOffsets.toMutableList()
                                        newOffsets[index] = offsetDp
                                        tabOffsets = newOffsets
                                        if (index == 0) tabHeight = heightDp
                                    }
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        coroutineScope.launch {
                                            // Calculate the closest target page that matches the clicked tab index
                                            val diff = index - currentTab
                                            pagerState.animateScrollToPage(pagerState.currentPage + diff)
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else Color.Gray, // Contrast colors
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding) // Crucial to prevent double-padding when ChatScreen processes IME insets
        ) {
            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page % actualPageCount) {
                    0 -> ChatScreen(onNavigateBack = onNavigateBack)
                    1 -> com.example.pocketplanner.ui.chat.live.LiveVoiceScreen(onNavigateBack = onNavigateBack)
                    2 -> VisualTranslatePlaceholder(onNavigateBack)
                    3 -> InterpreterPlaceholder(onNavigateBack)
                }
            }
        }
    }
}


@Composable
fun VisualTranslatePlaceholder(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C))) {
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).systemBarsPadding()) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Visual Translate", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Point your camera at foreign menus, street signs, or transit maps to instantly read them in your native language.",
                color = Color.LightGray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { /* TODO: Request Camera Permission */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Start Scanning", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
fun InterpreterPlaceholder(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF3D3D3D))) {
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).systemBarsPadding()) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Translate, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Interpreter", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Translate real-world conversations instantly. Just tap the microphone and speak—the app will speak back in the local language.",
                color = Color.LightGray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { /* TODO: Request Mic Permission */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Start Interpreting", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

fun Modifier.pillIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val currentTabWidth by animateDpAsState(
        targetValue = currentTabPosition.width,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "width"
    )
    val indicatorOffset by animateDpAsState(
        targetValue = currentTabPosition.left,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "offset"
    )
    this.fillMaxWidth()
        .wrapContentSize(Alignment.CenterStart)
        .offset(x = indicatorOffset)
        .width(currentTabWidth)
}
